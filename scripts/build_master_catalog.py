"""
PackReady Shopkeeper — 10,000 Indian FMCG Master Catalog Generator
Generates a comprehensive, realistic, and schema-compliant SQLite catalog
database (and JSON export) ready for offline bundling in the Android POS app.
"""

import os
import json
import sqlite3
import random
import sys

def compute_ean13_checksum(prefix12: str) -> str:
    """Computes the 13th check digit for a 12-digit barcode string."""
    digits = [int(c) for c in prefix12]
    # In EAN-13: sum odd-indexed (0, 2, 4...) * 1, even-indexed (1, 3, 5...) * 3
    weighted_sum = sum(digits[i] if i % 2 == 0 else digits[i] * 3 for i in range(12))
    check_digit = (10 - (weighted_sum % 10)) % 10
    return f"{prefix12}{check_digit}"

# Taxonomy and Brand Data for Indian Retail / Kirana
CATALOG_SPECS = [
    # 1. FOOD & GROCERY — BISCUITS & COOKIES
    {
        "category": "FOOD_GROCERY",
        "subCategory": "Biscuits & Cookies",
        "unit": "pcs",
        "gstPercent": 18.0,
        "margin": 0.14,
        "brands": [
            ("Parle", ["Parle-G Glucose", "Parle 20-20 Butter", "Hide & Seek Chocolate", "Monaco Classic Salted", "Krackjack Sweet & Salty", "Milano Choco Delight"]),
            ("Britannia", ["Good Day Butter", "Good Day Cashew", "Marie Gold Tea", "NutriChoice Digestive", "Bourbon Chocolate Cream", "Treat Jim Jam", "Milk Bikis", "Little Hearts Sugar Glazed", "Tiger Krunch Glucose"]),
            ("Sunfeast", ["Dark Fantasy Choco Fills", "Mom's Magic Cashew & Almond", "Bounce Creme Choco", "Glucose Biscuits", "Marie Light Oats & Fiber"]),
            ("Oreo", ["Oreo Original Vanilla Creme", "Oreo Dark & White Creme", "Oreo Strawberry Creme", "Oreo Chocolate Creme"]),
            ("Patanjali", ["Patanjali Doodh Biscuits", "Patanjali Aarogya Marie", "Patanjali Butter Cookies"])
        ],
        "sizes": [
            ("50g", 5.0, "Pouch"),
            ("100g", 10.0, "Pouch"),
            ("150g", 20.0, "Pouch"),
            ("250g", 30.0, "Pouch"),
            ("300g", 40.0, "Pouch"),
            ("500g", 65.0, "Pouch"),
            ("800g Family Pack", 99.0, "Family Box")
        ],
        "isVeg": True
    },

    # 2. FOOD & GROCERY — INSTANT NOODLES & PASTA
    {
        "category": "FOOD_GROCERY",
        "subCategory": "Instant Noodles & Pasta",
        "unit": "pcs",
        "gstPercent": 12.0,
        "margin": 0.15,
        "brands": [
            ("Nestle", ["Maggi 2-Minute Masala", "Maggi Special Masala", "Maggi Atta Noodles Masala", "Maggi Oats Noodles", "Maggi Cuppa Masala Chilled", "Maggi Pazzta Masala Penne", "Maggi Pazzta Cheese Macaroni"]),
            ("Yippee", ["Sunfeast Yippee! Mood Masala Noodles", "Sunfeast Yippee! Magic Masala Noodles", "Sunfeast Yippee! Power Up Atta Noodles"]),
            ("Ching's Secret", ["Ching's Secret Schezwan Instant Noodles", "Ching's Secret Hot Garlic Noodles", "Ching's Secret Manchurian Noodles"]),
            ("Knorr", ["Knorr Soupy Noodles Mast Masala", "Knorr Chinese Noodles"]),
            ("Top Ramen", ["Top Ramen Masala Noodles", "Top Ramen Curry Smatt Smatt", "Top Ramen Fiery Chili"])
        ],
        "sizes": [
            ("70g Single Pack", 14.0, "Pouch"),
            ("140g 2-Pack", 28.0, "Pouch"),
            ("280g 4-Pack", 55.0, "Pouch"),
            ("420g 6-Pack", 80.0, "Pouch"),
            ("840g Megapack 12", 150.0, "Multipack Box")
        ],
        "isVeg": True
    },

    # 3. FOOD & GROCERY — ATTA, FLOURS & GRAINS
    {
        "category": "FOOD_GROCERY",
        "subCategory": "Atta & Flours",
        "unit": "pcs",
        "gstPercent": 5.0,
        "margin": 0.10,
        "brands": [
            ("Aashirvaad", ["Aashirvaad Superior MP Shudh Chakki Atta", "Aashirvaad Select 100% Sharbati Atta", "Aashirvaad Multigrain Atta with 6 Grains"]),
            ("Fortune", ["Fortune Chakki Fresh Atta", "Fortune Sharbati Wheat Atta", "Fortune Besan Gram Flour", "Fortune Maida All Purpose", "Fortune Sooji Fine Rawa"]),
            ("Patanjali", ["Patanjali Traditional Chakki Atta", "Patanjali Navratna Multigrain Atta"]),
            ("Rajdhani", ["Rajdhani Besan Fine Gram Flour", "Rajdhani Maida", "Rajdhani Sooji Fine Semolina", "Rajdhani Roasted Daliya"]),
            ("Tata Sampann", ["Tata Sampann Unpolished Besan 100% Chana Dal", "Tata Sampann High Protein Atta"])
        ],
        "sizes": [
            ("500g", 35.0, "Pouch"),
            ("1kg", 65.0, "Pouch"),
            ("2kg", 125.0, "Pouch"),
            ("5kg Bag", 265.0, "Bag"),
            ("10kg Bag", 495.0, "Bag")
        ],
        "isVeg": True
    },

    # 4. FOOD & GROCERY — EDIBLE OILS & GHEE
    {
        "category": "FOOD_GROCERY",
        "subCategory": "Edible Oils & Ghee",
        "unit": "pcs",
        "gstPercent": 5.0,
        "margin": 0.09,
        "brands": [
            ("Fortune", ["Fortune Sunlite Refined Sunflower Oil", "Fortune Kachi Ghani Pure Mustard Oil", "Fortune Soyabean Refined Oil", "Fortune Rice Bran Health Oil", "Fortune Cottonseed Oil"]),
            ("Saffola", ["Saffola Gold Pro Healthy Heart Edible Oil", "Saffola Total Blended Oil", "Saffola Active Multivision Oil"]),
            ("Dhara", ["Dhara Kachi Ghani Mustard Oil", "Dhara Refined Sunflower Oil", "Dhara Refined Vegetable Oil"]),
            ("Amul", ["Amul Pure Cow Ghee", "Amul Pure Buffalo Ghee", "Amul Lite Table Spread"]),
            ("Mother Dairy", ["Mother Dairy Pure Desi Ghee", "Dhara Life Refined Soyabean Oil"]),
            ("Patanjali", ["Patanjali Cow Ghee Pure", "Patanjali Kachi Ghani Mustard Oil"])
        ],
        "sizes": [
            ("500ml Pouch", 78.0, "Pouch"),
            ("1L Pouch", 145.0, "Pouch"),
            ("1L Bottle", 155.0, "Bottle"),
            ("2L Jar", 310.0, "Jar"),
            ("5L Can with Handle", 760.0, "Can")
        ],
        "isVeg": True
    },

    # 5. FOOD & GROCERY — SALT, SUGAR & SPICES
    {
        "category": "FOOD_GROCERY",
        "subCategory": "Salt, Sugar & Spices",
        "unit": "pcs",
        "gstPercent": 5.0,
        "margin": 0.16,
        "brands": [
            ("Tata", ["Tata Salt Vacuum Evaporated Iodized", "Tata Salt Lite Low Sodium", "Tata Salt Super Lite Potassium Enriched", "Tata Rock Salt Himalayan Pink"]),
            ("Everest", ["Everest Garam Masala Super Blend", "Everest Tikhalal Hot Chilli Powder", "Everest Turmeric Powder Pure Haldi", "Everest Coriander Powder Pure Dhaniya", "Everest Pav Bhaji Masala", "Everest Chhole Masala", "Everest Chicken Masala Special", "Everest Kitchen King All Purpose Masala", "Everest Sambhar Masala", "Everest Chaat Masala"]),
            ("MDH", ["MDH Deggi Mirch Natural Red Powder", "MDH Chana Masala", "MDH Garam Masala", "MDH Chunky Chat Masala", "MDH Kasoori Methi Dried Leaves", "MDH Meat Masala", "MDH Kitchen King"]),
            ("Catch", ["Catch Sprinklers Table Salt", "Catch Black Pepper Sprinkler", "Catch Sabzi Masala Special", "Catch Red Chilli Powder"]),
            ("Madhur", ["Madhur Pure & Hygienic Refined Sugar", "Madhur Sulfur-free Crystal Sugar"])
        ],
        "sizes": [
            ("50g Box", 38.0, "Box"),
            ("100g Box", 72.0, "Box"),
            ("200g Pouch", 110.0, "Pouch"),
            ("500g Pack", 140.0, "Pouch"),
            ("1kg Pack", 28.0, "Pouch"),
            ("5kg Jar", 230.0, "Jar")
        ],
        "isVeg": True
    },

    # 6. FOOD & GROCERY — SNACKS, NAMKEEN & CHIPS
    {
        "category": "FOOD_GROCERY",
        "subCategory": "Snacks & Namkeen",
        "unit": "pcs",
        "gstPercent": 12.0,
        "margin": 0.18,
        "brands": [
            ("Lay's", ["Lay's India's Magic Masala Chips", "Lay's Spanish Tomato Tango Chips", "Lay's Classic Salted Chips", "Lay's American Style Cream & Onion Chips", "Lay's Hot 'n' Sweet Chilli Chips", "Lay's Wafer Thin Salted"]),
            ("Kurkure", ["Kurkure Masala Munch Crunchy Twists", "Kurkure Green Chutney Style", "Kurkure Chilli Chatka", "Kurkure Solid Masti Masala Twisteez", "Kurkure Puffcorn Yummy Cheese"]),
            ("Haldiram's", ["Haldiram's Aloo Bhujia Spicy", "Haldiram's Bhujia Sev Classic", "Haldiram's Khatta Meetha Savory Mix", "Haldiram's Moong Dal Fried & Salted", "Haldiram's Nut Crackers Masala Peanuts", "Haldiram's All In One Signature Mixture", "Haldiram's Panchrattan Sweet & Salty", "Haldiram's Soan Papdi Classic Ghee"]),
            ("Balaji", ["Balaji Wafers Simply Salted", "Balaji Masala Wafers", "Balaji Chataka Pataka Flamin Hot", "Balaji Gathiya Crispy", "Balaji Ratlami Sev"]),
            ("Bingo", ["Bingo! Mad Angles Achaari Masti", "Bingo! Tedhe Medhe Masala Tadka", "Bingo! Hash Potatoes Salted"])
        ],
        "sizes": [
            ("25g Small Pack", 5.0, "Pouch"),
            ("50g Standard Pack", 10.0, "Pouch"),
            ("90g Sharing Pack", 20.0, "Pouch"),
            ("150g Big Value Pack", 35.0, "Pouch"),
            ("200g Pouch", 55.0, "Pouch"),
            ("400g Family Pack", 100.0, "Pouch"),
            ("1kg Mega Tin", 240.0, "Box")
        ],
        "isVeg": True
    },

    # 7. FOOD & GROCERY — CHOCOLATES, CONFECTIONERY & SWEETS
    {
        "category": "FOOD_GROCERY",
        "subCategory": "Chocolates & Confectionery",
        "unit": "pcs",
        "gstPercent": 18.0,
        "margin": 0.16,
        "brands": [
            ("Cadbury", ["Cadbury Dairy Milk Chocolate", "Cadbury Dairy Milk Silk", "Cadbury Dairy Milk Fruit & Nut", "Cadbury Dairy Milk Roast Almond", "Cadbury 5 Star Caramel Nougat", "Cadbury Fuse Peanut Chocolate", "Cadbury Perk Crispy Wafer", "Cadbury Gems Choco Buttons", "Cadbury Celebrations Gift Pack"]),
            ("Nestle", ["Nestle KitKat 4 Finger Crisp Wafer", "Nestle KitKat 2 Finger Break", "Nestle Munch Crunchy Wafer Bar", "Nestle Milkybar White Chocolate", "Nestle BarOne Caramel Chocolate"]),
            ("Ferrero", ["Kinder Joy with Surprise Toy", "Ferrero Rocher Premium Hazelnut Pralines"]),
            ("Amul", ["Amul Dark Chocolate 55% Cocoa", "Amul Fruit 'N' Nut Dark Chocolate", "Amul Milk Chocolate Classic"])
        ],
        "sizes": [
            ("13g Treat Bar", 10.0, "Wrap"),
            ("24g Standard Bar", 20.0, "Wrap"),
            ("50g Share Bar", 45.0, "Wrap"),
            ("150g Silk Large Tablet", 110.0, "Wrap"),
            ("300g Gift Box", 250.0, "Gift Box")
        ],
        "isVeg": True
    },

    # 8. FOOD & GROCERY — DAIRY & REFRIGERATED
    {
        "category": "FOOD_GROCERY",
        "subCategory": "Dairy & Refrigerated",
        "unit": "pcs",
        "gstPercent": 12.0,
        "margin": 0.11,
        "brands": [
            ("Amul", ["Amul Butter Pasteurized Salted", "Amul Butter Unsalted White", "Amul Cheese Slices Processed", "Amul Cheese Blocks", "Amul Cheese Cubes 200g", "Amul Fresh Paneer Rich Protein", "Amul Masti Dahi Curd", "Amul Fresh Cream 25% Milk Fat", "Amul Taaza Homogenised Toned Milk", "Amul Gold Full Cream Milk"]),
            ("Mother Dairy", ["Mother Dairy Classic Curd", "Mother Dairy Paneer Fresh Malai", "Mother Dairy Butter Salted", "Mother Dairy Cheese Slices"]),
            ("Britannia", ["Britannia Cheese Slices Pure Milk", "Britannia Cheese Block", "Britannia Processed Cheese Spread Classic"])
        ],
        "sizes": [
            ("100g", 58.0, "Box"),
            ("200g", 115.0, "Box"),
            ("400g Tub", 90.0, "Tub"),
            ("500g Brick", 275.0, "Brick"),
            ("1L Tetra", 74.0, "Tetra")
        ],
        "isVeg": True
    },

    # 9. BEVERAGES — TEA & COFFEE
    {
        "category": "BEVERAGES",
        "subCategory": "Tea & Coffee",
        "unit": "pcs",
        "gstPercent": 5.0,
        "margin": 0.15,
        "brands": [
            ("Tata Tea", ["Tata Tea Premium Desh Ki Chai", "Tata Tea Gold Fine Assam Leaf", "Tata Tea Agni Strong Blend", "Tata Tea Chakra Gold Premium"]),
            ("Brooke Bond", ["Red Label Strong Tea", "Red Label Natural Care with 5 Ayurvedic Herbs", "Taj Mahal Rich Tea Leaves", "Taaza Strong Tea Granules"]),
            ("Wagh Bakri", ["Wagh Bakri Premium Leaf Tea", "Wagh Bakri Spiced Tea Masala", "Wagh Bakri Good Morning CTC"]),
            ("Nescafe", ["Nescafe Classic 100% Pure Instant Coffee", "Nescafe Sunrise Chicory Blend", "Nescafe Gold Rich Aroma Jar"]),
            ("Bru", ["Bru Instant Coffee Granules", "Bru Gold 100% Pure Coffee", "Bru Green Label Roasted Ground Coffee"])
        ],
        "sizes": [
            ("50g Pack", 35.0, "Pouch"),
            ("100g Jar", 195.0, "Glass Jar"),
            ("250g Pack", 140.0, "Pouch"),
            ("500g Value Pack", 270.0, "Pouch"),
            ("1kg Master Pack", 520.0, "Pouch")
        ],
        "isVeg": True
    },

    # 10. BEVERAGES — SOFT DRINKS, SODA & JUICES
    {
        "category": "BEVERAGES",
        "subCategory": "Soft Drinks & Juices",
        "unit": "pcs",
        "gstPercent": 28.0,
        "margin": 0.20,
        "brands": [
            ("Coca-Cola", ["Coca-Cola Original Taste Sparkling", "Coca-Cola Zero Sugar Can", "Thums Up Charged Strong Cola", "Sprite Clear Lemon Lime Refreshment", "Fanta Orange Sparkling", "Limca Tangy Lemon Fizz", "Maaza Mango Pulp Drink"]),
            ("PepsiCo", ["Pepsi Cola Refreshing", "Mountain Dew Darr Ke Aage Jeet Hai", "7Up Nimbooz Real Lemon", "Mirinda Orange Flavor", "Slice Alphonso Mango Drink", "Sting Energy Drink Strawberry Gold"]),
            ("Bisleri", ["Bisleri Pure Mineral Water with Added Minerals", "Bisleri Limonata Fizzy Lemon Drink", "Bisleri Soda Club"]),
            ("Real", ["Real Fruit Power Mixed Fruit Juice", "Real Fruit Power Alphonso Mango", "Real Fruit Power Sweet Orange", "Real Fruit Power Pomegranate", "Real Fruit Power Guava Pure"]),
            ("Tropicana", ["Tropicana 100% Orange Juice", "Tropicana Mixed Fruit Juice", "Tropicana Delight Apple Drink"]),
            ("Red Bull", ["Red Bull Energy Drink Caffeine Can", "Red Bull Sugarfree Blue Silver Can", "Red Bull Yellow Edition Tropical"])
        ],
        "sizes": [
            ("200ml Tetra", 20.0, "Tetra"),
            ("250ml Slim Can", 40.0, "Can"),
            ("300ml Glass Returnable", 15.0, "Bottle"),
            ("500ml PET", 30.0, "Bottle"),
            ("750ml PET", 40.0, "Bottle"),
            ("1L PET Bottle", 20.0, "Bottle"),
            ("1.25L Big Bottle", 65.0, "Bottle"),
            ("2L Party Bottle", 95.0, "Bottle")
        ],
        "isVeg": True
    },

    # 11. PERSONAL CARE — SOAPS & BODY WASH
    {
        "category": "PERSONAL_CARE",
        "subCategory": "Soaps & Body Wash",
        "unit": "pcs",
        "gstPercent": 18.0,
        "margin": 0.16,
        "brands": [
            ("Dettol", ["Dettol Original Germ Protection Bathing Bar", "Dettol Skincare Moisturizing Bar", "Dettol Cool Menthol Refreshing Bar", "Dettol Aloe Vera Soothing Bar"]),
            ("Lifebuoy", ["Lifebuoy Total 10 Germ Protection Soap", "Lifebuoy Care with Milk Cream", "Lifebuoy Lemon Fresh Active"]),
            ("Lux", ["Lux Velvet Glow Jasmine & Almond Oil Soap", "Lux Soft Glow Rose & Vitamin E Soap", "Lux Radiant Glow Lily Soap"]),
            ("Dove", ["Dove White Beauty Bar Moisturizing", "Dove Pink Rosa Bathing Bar", "Dove Gentle Exfoliating Nourishing Bar"]),
            ("Pears", ["Pears Pure & Gentle 98% Pure Glycerin Soap", "Pears Soft & Fresh Mint Extract Soap", "Pears Oil Clear Lemon Flower Soap"]),
            ("Santoor", ["Santoor Sandal & Turmeric Soap", "Santoor White Sandal & Almond Soap", "Santoor Aloe Fresh Glowing Soap"]),
            ("Cinthol", ["Cinthol Original Germ Protection Deo Soap", "Cinthol Cool Icy Menthol Soap", "Cinthol Lime Refreshing Deo Soap"]),
            ("Medimix", ["Medimix Ayurvedic 18 Herbs Classic Soap", "Medimix Ayurvedic Sandal & Eladi Oil Soap"]),
            ("Godrej No.1", ["Godrej No.1 Kesar & Milk Cream Soap", "Godrej No.1 Sandal & Turmeric Soap", "Godrej No.1 Lime & Aloe Soap"])
        ],
        "sizes": [
            ("75g Single Bar", 28.0, "Wrap"),
            ("100g Bar", 38.0, "Wrap"),
            ("125g Large Bar", 52.0, "Wrap"),
            ("75g Multipack of 3", 80.0, "Multipack"),
            ("100g Multipack of 4 (Buy 3 Get 1)", 140.0, "Multipack Box"),
            ("125g Super Saver Pack of 5", 220.0, "Multipack Box")
        ],
        "isVeg": True
    },

    # 12. PERSONAL CARE — ORAL CARE & TOOTHPASTE
    {
        "category": "PERSONAL_CARE",
        "subCategory": "Oral Care",
        "unit": "pcs",
        "gstPercent": 18.0,
        "margin": 0.18,
        "brands": [
            ("Colgate", ["Colgate Strong Teeth Calcium Boost Dental Cream", "Colgate MaxFresh Peppermint Cooling Crystals", "Colgate Total 12HR Antibacterial Protection", "Colgate Active Salt with Sea Minerals", "Colgate Vedshakti Ayurvedic Cavity Protection", "Colgate ZigZag Soft Anti-Bacterial Toothbrush", "Colgate Super Flexi Clean Toothbrush"]),
            ("CloseUp", ["CloseUp Everfresh Red Hot Gel Toothpaste", "CloseUp Deep Action Menthol Fresh Gel", "CloseUp Diamond Attraction Whitening"]),
            ("Sensodyne", ["Sensodyne Rapid Relief Toothpaste for Sensitivity", "Sensodyne Daily Care Fresh Mint Gel", "Sensodyne Deep Clean Toothbrush"]),
            ("Dabur", ["Dabur Red Ayurvedic Toothpaste with Clove & Pudina", "Dabur Meswak Herbal Gum Protection Toothpaste", "Dabur Babool Ayurvedic Toothpaste"]),
            ("Patanjali", ["Patanjali Dant Kanti Regular Herbal Dental Cream", "Patanjali Dant Kanti Advanced Herbal Toothpaste", "Patanjali Dant Kanti Medicated Sensitivity Gel"])
        ],
        "sizes": [
            ("50g Small Tube", 25.0, "Tube"),
            ("100g Regular Tube", 60.0, "Tube"),
            ("150g Value Tube", 98.0, "Tube"),
            ("200g Saver Tube", 125.0, "Tube"),
            ("300g Family Pack (150g x 2)", 195.0, "Duo Pack Box")
        ],
        "isVeg": True
    },

    # 13. PERSONAL CARE — SHAMPOO & HAIR CARE
    {
        "category": "PERSONAL_CARE",
        "subCategory": "Hair Care & Shampoo",
        "unit": "pcs",
        "gstPercent": 18.0,
        "margin": 0.18,
        "brands": [
            ("Head & Shoulders", ["Head & Shoulders Anti-Dandruff Smooth & Silky", "Head & Shoulders Cool Menthol Anti-Dandruff", "Head & Shoulders Lemon Fresh Oil Control"]),
            ("Clinic Plus", ["Clinic Plus Strong & Long Health Shampoo with Milk Protein", "Clinic Plus Naturally Strong Egg Protein Shampoo"]),
            ("Sunsilk", ["Sunsilk Black Shine Shampoo with Amla & Pearl", "Sunsilk Thick & Long Keratin Shampoo", "Sunsilk Soft & Smooth Argan Oil Shampoo"]),
            ("Pantene", ["Pantene Pro-V Hair Fall Control Nourishing Shampoo", "Pantene Pro-V Total Damage Care Shampoo"]),
            ("Parachute", ["Parachute 100% Pure Coconut Oil Pure Edible Grade", "Parachute Advansed Aloe Vera Enriched Coconut Hair Oil", "Parachute Advansed Jasmine Light Hair Oil"]),
            ("Bajaj", ["Bajaj Almond Drops Non-Sticky Hair Oil with Vitamin E", "Bajaj Coco Onion Non-Sticky Hair Oil"]),
            ("Dabur", ["Dabur Amla Hair Oil for Long & Black Hair", "Dabur Vatika Enriched Coconut Hair Oil with Henna & Lemon"])
        ],
        "sizes": [
            ("6ml Sachet (Strip of 16)", 32.0, "Sachet Strip"),
            ("80ml Small Bottle", 75.0, "Bottle"),
            ("180ml Bottle", 160.0, "Bottle"),
            ("340ml Pump Bottle", 280.0, "Pump Bottle"),
            ("650ml Family Size Pump", 499.0, "Pump Bottle")
        ],
        "isVeg": True
    },

    # 14. HOUSEHOLD & CLEANING — LAUNDRY DETERGENTS
    {
        "category": "HOUSEHOLD_CLEANING",
        "subCategory": "Laundry Detergents",
        "unit": "pcs",
        "gstPercent": 18.0,
        "margin": 0.14,
        "brands": [
            ("Surf Excel", ["Surf Excel Quick Wash Detergent Powder", "Surf Excel Easy Wash Fabric Cleaning Powder", "Surf Excel Matic Front Load Liquid", "Surf Excel Matic Top Load Detergent Powder", "Surf Excel Stain Eraser Detergent Bar"]),
            ("Ariel", ["Ariel Complete Color Protection Washing Powder", "Ariel Matic Top Load Detergent Powder", "Ariel Matic Front Load Liquid Detergent"]),
            ("Tide", ["Tide Plus Extra Power Detergent Powder Jasmine & Rose", "Tide Plus Double Power Lemon & Mint Powder", "Tide Detergent Bar White"]),
            ("Rin", ["Rin Refresh Lemon & Rose Detergent Powder", "Rin Advanced Detergent Bar for Brilliant White Clothes"]),
            ("Ghadi", ["Ghadi Detergent Powder Pehle Istemaal Kare Phir Vishwas Kare", "Ghadi Detergent Cake"]),
            ("Comfort", ["Comfort After Wash Morning Fresh Fabric Conditioner", "Comfort Lily & Fresh Fabric Softener"])
        ],
        "sizes": [
            ("250g Bar", 20.0, "Bar"),
            ("500g Pouch", 55.0, "Pouch"),
            ("1kg Poly Pack", 135.0, "Pouch"),
            ("2kg Bag", 260.0, "Bag"),
            ("4kg Mega Value Bag with Tub", 520.0, "Bag"),
            ("1L Liquid Refill Pouch", 215.0, "Spout Pouch")
        ],
        "isVeg": True
    },

    # 15. HOUSEHOLD & CLEANING — DISHWASH & SURFACE CLEANERS
    {
        "category": "HOUSEHOLD_CLEANING",
        "subCategory": "Dishwash & Surface Cleaners",
        "unit": "pcs",
        "gstPercent": 18.0,
        "margin": 0.17,
        "brands": [
            ("Vim", ["Vim Lemon Dishwash Bar with Pure Lime Juice", "Vim Gel Lemon Deep Grease Cutting Liquid", "Vim Anti-Smell Pudina Dishwash Gel", "Vim Dishwash Bar with Plastic Tub"]),
            ("Pril", ["Pril Kraft Gel Lime Active Grease Cutting", "Pril Tamarind Shine Dishwash Liquid"]),
            ("Harpic", ["Harpic Power Plus 10X Max Clean Toilet Cleaner Original", "Harpic Power Plus Rose Toilet Cleaner", "Harpic Disinfectant Bathroom Cleaner Citrus"]),
            ("Lizol", ["Lizol 10X Disinfectant Surface Floor Cleaner Citrus", "Lizol Surface Floor Cleaner Pine Fresh", "Lizol Surface Cleaner Lavender Floral"]),
            ("Colin", ["Colin Glass and Household Cleaner Trigger Spray", "Colin Glass Cleaner Refill Pouch"]),
            ("GoodKnight", ["GoodKnight Active+ Mosquito Vaporizer Refill Liquid", "GoodKnight Gold Flash Liquid Vaporizer Machine + Refill", "GoodKnight Fast Card 3-Minute Mosquito Paper"]),
            ("All Out", ["All Out Ultra Power+ Fan Liquid Mosquito Repellent Refill", "All Out Custom Liquid Device + Refill"]),
            ("HIT", ["HIT Flying Insect Killer Black Lime Mosquito Spray", "HIT Cockroach Killer Red Deep Reach Nozzle Spray", "HIT Anti-Roach Gel Syringe Bait"])
        ],
        "sizes": [
            ("135g Bar", 10.0, "Wrap"),
            ("300g Bar with Tub", 35.0, "Tub"),
            ("250ml Bottle", 58.0, "Bottle"),
            ("500ml Ergonomic Bottle", 105.0, "Bottle"),
            ("1L Bottle", 185.0, "Bottle"),
            ("2L Super Saver Refill", 340.0, "Can")
        ],
        "isVeg": True
    },

    # 16. OTC & WELLNESS — FIRST AID, DIGESTION & PAIN RELIEF
    {
        "category": "OTC_WELLNESS",
        "subCategory": "OTC & First Aid",
        "unit": "pcs",
        "gstPercent": 12.0,
        "margin": 0.22,
        "brands": [
            ("Dettol", ["Dettol Antiseptic Disinfectant Liquid First Aid"]),
            ("Savlon", ["Savlon Antiseptic Liquid Disinfectant Wash", "Savlon Germ Protection Wipes"]),
            ("Eno", ["Eno Regular Fast Fruit Salt Antacid", "Eno Lemon Fast 6-Second Relief Sachet", "Eno Orange Flavor Fruit Salt"]),
            ("Pudina Hara", ["Dabur Pudina Hara Pearls Fast Stomach Gas Relief", "Dabur Pudina Hara Active Liquid"]),
            ("Moov", ["Moov Pain Relief Cream Ayurvedic Fast Action", "Moov Pain Relief Specialist Spray"]),
            ("Volini", ["Volini Pain Relief Gel Diclofenac Max Strength", "Volini Instant Pain Relief Spray"]),
            ("Iodex", ["Iodex Multi-Purpose Ayurvedic Pain Balm", "Iodex Ultra Gel Deep Rub"]),
            ("Vicks", ["Vicks VapoRub Ayurvedic Relief Balm", "Vicks Inhaler Portable Menthol", "Vicks Cough Drops Honey Lemon Lozenges"]),
            ("Himalaya", ["Himalaya Liv.52 Herbal Liver Care Tablets", "Himalaya Septilin Immunity Tablets", "Himalaya Gasex Digestive Tablets"]),
            ("Band-Aid", ["Band-Aid Washproof Antiseptic Adhesive Strips", "Band-Aid Spot Round Strips"])
        ],
        "sizes": [
            ("5g Sachet (Pack of 6)", 60.0, "Sachet Pack"),
            ("10g Pocket Tin", 45.0, "Tin"),
            ("25g Tube", 85.0, "Tube"),
            ("50g Family Pack", 155.0, "Tube"),
            ("100ml Bottle", 82.0, "Bottle"),
            ("250ml Disinfectant Bottle", 145.0, "Bottle")
        ],
        "isVeg": True
    },

    # 17. ELECTRONICS & ELECTRICAL — LIGHTING & ACCESSORIES
    {
        "category": "ELECTRONICS_ELECTRICAL",
        "subCategory": "Lighting & Accessories",
        "unit": "pcs",
        "gstPercent": 18.0,
        "margin": 0.25,
        "brands": [
            ("Philips", ["Philips Astra Cool Day White LED Bulb B22", "Philips Deco Mini Night Lamp LED 0.5W", "Philips Ace Saver LED Bulb B22 12W"]),
            ("Havells", ["Havells Adore Cool Daylight LED Bulb B22", "Havells Glitz Inverter Emergency LED Bulb"]),
            ("Syska", ["Syska SSK-SRL Cool White LED Bulb B22", "Syska Lumispread High Lumen LED"]),
            ("Duracell", ["Duracell Ultra Alkaline AA Batteries", "Duracell Ultra Alkaline AAA Batteries", "Duracell Chota Power AA"]),
            ("Everyuth / Eveready", ["Eveready Red Carbon Zinc Heavy Duty AA Batteries", "Eveready Blue Alkaline AAA Batteries", "Eveready Digiled Flashlight Torch"])
        ],
        "sizes": [
            ("Pack of 2", 70.0, "Box"),
            ("Pack of 4", 150.0, "Box"),
            ("9W Single Bulb", 110.0, "Box"),
            ("12W Single Bulb", 160.0, "Box"),
            ("Emergency Inverter Bulb", 399.0, "Box")
        ],
        "isVeg": True
    },

    # 18. LOOSE STAPLES — RICE, PULSES, SPICES & SUGAR
    {
        "category": "LOOSE_STAPLES",
        "subCategory": "Grains, Dals & Loose Goods",
        "unit": "kg",
        "gstPercent": 0.0,
        "margin": 0.18,
        "brands": [
            ("Store Brand Loose", [
                "Loose Refined Sugar Medium Crystal Clean",
                "Royal Basmati Rice XXL Long Grain Premium",
                "Sona Masoori Raw Rice Cleaned",
                "Kolam Rice Grade-1 Silky",
                "Desi Unpolished Toor / Arhar Dal Latur",
                "Yellow Moong Dal Dhuli Split Polished",
                "Chana Dal Desi Bold Grade-A",
                "Urad Dal Gota White Whole Unpolished",
                "Masoor Dal Red Split Cleaned",
                "Kabuli Chana Dollar Big White Chickpeas",
                "Kala Chana Desi Brown Chickpeas",
                "Rajma Chitra Kashmiri Premium",
                "Cumin Seeds Pure Jeera Machine Cleaned",
                "Mustard Seeds Rai Small Desi Black",
                "Fennel Seeds Saunf Thick Sweet Fragrant",
                "Fenugreek Seeds Methi Dana Clean",
                "Sabudana Sago Superfine Medium Pearl",
                "Poha Medium Thick Flattened Rice Flakes",
                "Besan Desi Loose Ground Gram Flour",
                "Dry Whole Red Chillies Guntur Stemless",
                "Whole Black Pepper Kali Mirch Malabar Bold",
                "Whole Cloves Laung Clean Fragrant",
                "Green Cardamom Chhoti Elaichi 8mm Bold",
                "Cashew Nuts Kaju W240 Whole Premium Grade",
                "California Almonds Badam Giri Inshell Super",
                "Golden Kishmish Raisins Indian Sweet Green",
                "Walnut Kernels Akhrot Giri Halves Light",
                "Groundnut Peanuts Mungfali Dana Bold Clean"
            ])
        ],
        "sizes": [
            ("1 kg", 95.0, "Loose Sack"),
            ("500 g", 50.0, "Loose Sack"),
            ("250 g", 26.0, "Loose Sack"),
            ("100 g", 12.0, "Loose Sack")
        ],
        "isVeg": True
    }
]

def generate_dynamic_attributes(category: str, size_str: str, packaging: str, brand: str) -> dict:
    """Creates JSON dynamic attributes matching the app's DynamicAttributes model."""
    attrs = {}
    if category in ("FOOD_GROCERY", "BEVERAGES"):
        attrs["netWeight"] = size_str.split()[0] if size_str else None
        attrs["isVegetarian"] = True
        attrs["bestBeforeMonths"] = random.choice([6, 9, 12, 18, 24])
        attrs["packagingType"] = packaging
    elif category == "PERSONAL_CARE":
        attrs["netWeight"] = size_str.split()[0] if size_str else None
        attrs["formFactor"] = "Liquid" if any(k in packaging.lower() for k in ["bottle", "pump"]) else "Bar"
        attrs["fragrance"] = random.choice(["Fresh Lime", "Floral Rose", "Aloe Vera", "Cool Menthol", "Sandalwood", "Original Clean"])
    elif category == "HOUSEHOLD_CLEANING":
        attrs["netWeight"] = size_str.split()[0] if size_str else None
        attrs["formFactor"] = "Powder" if "powder" in packaging.lower() or "bag" in packaging.lower() else "Liquid"
        attrs["fragrance"] = random.choice(["Lemon Fresh", "Pine Breeze", "Lavender", "Citrus", "Active Clean"])
    elif category == "ELECTRONICS_ELECTRICAL":
        attrs["wattage"] = "9W" if "9W" in size_str else ("12W" if "12W" in size_str else "N/A")
        attrs["voltageRange"] = "220-240V"
        attrs["warrantyMonths"] = random.choice([12, 24])
        attrs["pinCapType"] = "B22"
    elif category == "LOOSE_STAPLES":
        attrs["grainGrade"] = random.choice(["Superfine Grade-A", "Unpolished Desi", "Bold Cleaned", "Aged Premium"])
        attrs["origin"] = random.choice(["Maharashtra", "Punjab", "Madhya Pradesh", "Karnataka", "Dehradun", "Gujarat"])
    elif category == "OTC_WELLNESS":
        attrs["dosageForm"] = "Ointment" if "tube" in packaging.lower() else "Tablet"
        attrs["packCount"] = 1
    return attrs

def generate_catalog(target_count=10000):
    print(f"Generating comprehensive Indian FMCG Master Catalog target: {target_count} products...")
    products = []
    used_barcodes = set()
    loose_code_counter = 1000

    # GS1 India Barcode base: 890 (GS1 India Prefix)
    # Next 4-5 digits: Company/Brand prefix
    # Next 4 digits: Item code
    # Last digit: EAN-13 Checksum
    
    current_barcode_int = 890100000000

    # Phase 1: Combinatorial generation of realistic, named SKUs
    while len(products) < target_count:
        for spec in CATALOG_SPECS:
            category = spec["category"]
            sub_category = spec["subCategory"]
            unit = spec["unit"]
            gst_percent = spec["gstPercent"]
            margin_rate = spec["margin"]

            for brand_name, items in spec["brands"]:
                for base_item in items:
                    for size_name, base_price, packaging in spec["sizes"]:
                        if len(products) >= target_count:
                            break

                        if category == "LOOSE_STAPLES":
                            loose_code_counter += 1
                            barcode = f"PK-LOOSE-{loose_code_counter:04d}"
                            product_id = barcode
                            full_name = f"{base_item} ({size_name})"
                            mrp = round(base_price + (random.uniform(-0.15, 0.25) * base_price), 0)
                        else:
                            current_barcode_int += random.randint(1, 9)
                            barcode12 = f"{current_barcode_int}"[:12]
                            barcode = compute_ean13_checksum(barcode12)
                            while barcode in used_barcodes:
                                current_barcode_int += 1
                                barcode12 = f"{current_barcode_int}"[:12]
                                barcode = compute_ean13_checksum(barcode12)
                            used_barcodes.add(barcode)
                            product_id = barcode
                            full_name = f"{brand_name} {base_item} {size_name}"
                            mrp = round(base_price + (random.uniform(-0.08, 0.12) * base_price), 0)
                            if mrp < 5.0:
                                mrp = 5.0

                        # Calculate wholesale cost and retail selling price
                        cost_price = round(mrp * (1.0 - margin_rate), 2)
                        # Store selling price: mostly equal to MRP in Kirana, or small round discount
                        selling_price = mrp if random.random() > 0.3 else round(mrp * 0.96, 0)

                        attrs = generate_dynamic_attributes(category, size_name, packaging, brand_name)

                        product_entry = {
                            "id": product_id,
                            "barcode": barcode,
                            "name": full_name,
                            "brand": brand_name,
                            "category": category,
                            "subCategory": sub_category,
                            "mrp": float(mrp),
                            "costPrice": float(cost_price),
                            "sellingPrice": float(selling_price),
                            "currentStock": 0.0, # Zero stock initially, ready to be stocked
                            "minStockAlert": 5.0,
                            "unit": unit,
                            "gstPercent": float(gst_percent),
                            "salesCount": random.randint(5, 500),
                            "attributesJson": json.dumps(attrs, ensure_ascii=False),
                            "lastRestockedAt": 0,
                            "createdAt": 1728800000000
                        }
                        products.append(product_entry)

    print(f"Generated {len(products)} products across {len(CATALOG_SPECS)} FMCG retail segments.")
    return products

def save_to_sqlite(products, db_path):
    os.makedirs(os.path.dirname(os.path.abspath(db_path)), exist_ok=True)
    if os.path.exists(db_path):
        os.remove(db_path)

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # Create master_products table
    cur.execute("""
        CREATE TABLE master_products (
            id TEXT PRIMARY KEY NOT NULL,
            barcode TEXT UNIQUE NOT NULL,
            name TEXT NOT NULL,
            brand TEXT NOT NULL,
            category TEXT NOT NULL,
            subCategory TEXT NOT NULL,
            mrp REAL NOT NULL,
            costPrice REAL NOT NULL,
            sellingPrice REAL NOT NULL,
            currentStock REAL NOT NULL DEFAULT 0.0,
            minStockAlert REAL NOT NULL DEFAULT 5.0,
            unit TEXT NOT NULL,
            gstPercent REAL NOT NULL DEFAULT 0.0,
            salesCount INTEGER NOT NULL DEFAULT 0,
            attributesJson TEXT,
            lastRestockedAt INTEGER NOT NULL DEFAULT 0,
            createdAt INTEGER NOT NULL DEFAULT 0
        )
    """)

    # High speed indices for barcode scanning and POS search
    cur.execute("CREATE UNIQUE INDEX idx_master_barcode ON master_products(barcode)")
    cur.execute("CREATE INDEX idx_master_category ON master_products(category)")
    cur.execute("CREATE INDEX idx_master_name ON master_products(name)")
    cur.execute("CREATE INDEX idx_master_brand ON master_products(brand)")

    insert_sql = """
        INSERT INTO master_products (
            id, barcode, name, brand, category, subCategory,
            mrp, costPrice, sellingPrice, currentStock, minStockAlert,
            unit, gstPercent, salesCount, attributesJson, lastRestockedAt, createdAt
        ) VALUES (
            :id, :barcode, :name, :brand, :category, :subCategory,
            :mrp, :costPrice, :sellingPrice, :currentStock, :minStockAlert,
            :unit, :gstPercent, :salesCount, :attributesJson, :lastRestockedAt, :createdAt
        )
    """

    cur.executemany(insert_sql, products)
    conn.commit()

    # Verify count
    cur.execute("SELECT COUNT(*) FROM master_products")
    count = cur.fetchone()[0]
    conn.close()

    file_size_mb = os.path.getsize(db_path) / (1024 * 1024)
    print(f"Master Catalog Database built successfully: {db_path}")
    print(f"Total Rows: {count:,} | File Size: {file_size_mb:.2f} MB")

def save_to_json(products, json_path):
    os.makedirs(os.path.dirname(os.path.abspath(json_path)), exist_ok=True)
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(products, f, indent=2, ensure_ascii=False)
    file_size_mb = os.path.getsize(json_path) / (1024 * 1024)
    print(f"JSON catalog export saved: {json_path} ({file_size_mb:.2f} MB)")

if __name__ == "__main__":
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    assets_dir = os.path.join(base_dir, "app", "src", "main", "assets")
    
    target_count = 10000
    if len(sys.argv) > 1:
        target_count = int(sys.argv[1])

    catalog_data = generate_catalog(target_count)

    # 1. Save SQLite Database into app/src/main/assets/master_catalog.db
    db_out = os.path.join(assets_dir, "master_catalog.db")
    save_to_sqlite(catalog_data, db_out)

    # 2. Save JSON export for external tools, API backends, or testing
    json_out = os.path.join(assets_dir, "master_catalog_sample.json")
    save_to_json(catalog_data[:50], json_out) # Keep first 50 as lightweight JSON reference

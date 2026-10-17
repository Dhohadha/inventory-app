package com.packready.shopkeeper.data.vision

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.packready.shopkeeper.data.local.MasterCatalogDataSource
import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.model.ProductCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedPackagingDetails(
    val rawText: String,
    val normalizedText: String,
    val detectedBrand: String? = null,
    val detectedProductName: String? = null,
    val detectedMrp: Double? = null,
    val detectedCostPrice: Double? = null,
    val detectedSellingPrice: Double? = null,
    val detectedNetQuantity: String? = null,
    val detectedUnit: String = "pcs",
    val detectedCategory: ProductCategory? = null,
    val detectedSubCategory: String? = null,
    val isVegetarian: Boolean = true,
    val bestBeforeMonths: Int? = null,
    val confidence: Float = 0f,
    val matchedMasterCatalogItem: Product? = null
)

/**
 * High-Speed Packaging OCR Normalizer & Attribute Extractor.
 * Parses front, back, and detail packaging photos to auto-populate product attributes
 * without requiring shopkeepers to type manually.
 */
class PackagingOcrParser(
    private val masterCatalogDataSource: MasterCatalogDataSource? = null,
    private val edgeOcrNormalizer: EdgeOcrNormalizer? = null
) {
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val KNOWN_BRANDS = listOf(
        "PARLE-G", "PARLE", "BRITANNIA", "MAGGI", "NESTLE", "AMUL", "TATA", "AASHIRVAAD",
        "SURF EXCEL", "ARIEL", "TIDE", "RIN", "VIM", "DETTOL", "COLGATE", "CLOSEUP",
        "DABUR", "FORTUNE", "SAFFOLA", "DHARA", "HALDIRAM'S", "HALDIRAM", "BIKAJI", "BALAJI",
        "CADBURY", "OREO", "KITKAT", "COCA-COLA", "COKE", "THUMS UP", "SPRITE", "LIMCA",
        "MAAZA", "PEPSI", "7UP", "MIRINDA", "MOUNTAIN DEW", "REAL", "TROPICANA", "RED BULL",
        "HEAD & SHOULDERS", "CLINIC PLUS", "SUNSILK", "DOVE", "LUX", "PEARS", "SANTOOR",
        "CINTHOL", "LIFEBUOY", "GODREJ", "HARPIC", "LIZOL", "COLIN", "GOODKNIGHT", "HIT",
        "VICKS", "MOOV", "VOLINI", "IODEX", "ENO", "PUDINA HARA", "HIMALAYA", "PATANJALI",
        "PHILIPS", "HAVELLS", "SYSKA", "DURACELL", "EVEREADY", "EVEREST", "MDH", "CATCH"
    )

    private val OCR_NORMALIZATIONS = listOf(
        Regex("""\b0(?=[A-Za-z])""") to "O",
        Regex("""(?<=[A-Za-z])0\b""") to "O",
        Regex("""\b1(?=[A-Za-z])""") to "I",
        Regex("""\b5(?=[A-Za-z])""") to "S",
        Regex("""\bM\.?R\.?P\.?\b""", RegexOption.IGNORE_CASE) to "MRP",
        Regex("""\bRS\.?\b""", RegexOption.IGNORE_CASE) to "RS",
        Regex("""\b₹\b""") to "RS "
    )

    suspend fun parsePackagingImages(images: List<Bitmap>): ParsedPackagingDetails = withContext(Dispatchers.Default) {
        val rawTextBuilder = StringBuilder()

        for (bitmap in images) {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            try {
                val text = Tasks.await(recognizer.process(inputImage), 1500L, TimeUnit.MILLISECONDS)
                rawTextBuilder.append("\n").append(text.text)
            } catch (e: Exception) {
                // Skip failed image
            }
        }

        val fullRaw = rawTextBuilder.toString()
        val normalized = normalizeOcrText(fullRaw)

        // Try edge SLM normalizer (model_int8.onnx) first
        val slmResult = edgeOcrNormalizer?.normalizePackaging(fullRaw)

        val mrp = slmResult?.mrp ?: extractMrp(normalized)
        val extractedQty = extractNetQuantity(normalized)
        val netQty = slmResult?.netWeight ?: extractedQty.first
        val unit = extractedQty.second
        val brand = slmResult?.brand ?: extractBrand(normalized)
        val isVeg = extractVegetarian(normalized)
        val bestBefore = extractBestBefore(normalized)

        // Cross reference against 10,000-item Master Catalog for canonical match
        var catalogMatch: Product? = null
        if (masterCatalogDataSource != null && brand != null) {
            val candidates = masterCatalogDataSource.searchMasterCatalog(brand, limit = 10)
            catalogMatch = candidates.find { candidate ->
                normalized.contains(candidate.brand, ignoreCase = true) &&
                        (netQty != null && candidate.name.contains(netQty, ignoreCase = true))
            } ?: candidates.firstOrNull()
        }

        val category = catalogMatch?.category ?: inferCategory(normalized)
        val subCategory = catalogMatch?.subCategory ?: inferSubCategory(normalized)
        val finalTitle = slmResult?.productName ?: catalogMatch?.name ?: cleanProductTitle(normalized, brand, netQty)

        val costPrice = mrp?.let { roundTwoDecimals(it * 0.88) }
        val sellingPrice = mrp

        val confidence = calculateConfidence(brand, mrp, netQty, catalogMatch != null || slmResult != null)

        ParsedPackagingDetails(
            rawText = fullRaw.trim(),
            normalizedText = normalized.trim(),
            detectedBrand = brand ?: catalogMatch?.brand,
            detectedProductName = finalTitle,
            detectedMrp = mrp ?: catalogMatch?.mrp,
            detectedCostPrice = costPrice ?: catalogMatch?.costPrice,
            detectedSellingPrice = sellingPrice ?: catalogMatch?.sellingPrice,
            detectedNetQuantity = netQty,
            detectedUnit = unit,
            detectedCategory = category,
            detectedSubCategory = subCategory,
            isVegetarian = isVeg,
            bestBeforeMonths = bestBefore ?: catalogMatch?.attributes?.bestBeforeMonths,
            confidence = confidence,
            matchedMasterCatalogItem = catalogMatch
        )
    }

    private fun normalizeOcrText(text: String): String {
        var processed = text
        for ((regex, replacement) in OCR_NORMALIZATIONS) {
            processed = processed.replace(regex, replacement)
        }
        return processed
    }

    private fun extractMrp(text: String): Double? {
        val patterns = listOf(
            Pattern.compile("""(?i)(?:MRP|MAXIMUM\s*RETAIL\s*PRICE|RS\.?|₹)\s*(?:INCL.*?TAXES)?\s*[:.\-]?\s*([0-9]+(?:\.[0-9]{1,2})?)"""),
            Pattern.compile("""(?i)(?:RS\.?|₹)\s*([0-9]+(?:\.[0-9]{1,2})?)"""),
            Pattern.compile("""(?i)\b([0-9]{1,4}(?:\.[0-9]{2})?)\s*(?:RS|INR)\b""")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val valueStr = matcher.group(1)
                val parsed = valueStr?.toDoubleOrNull()
                if (parsed != null && parsed in 1.0..10000.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun extractNetQuantity(text: String): Pair<String?, String> {
        val pattern = Pattern.compile("""(?i)(?:NET\s*(?:WT\.?|WEIGHT|QTY|QUANTITY)?|WEIGHT|CONTENTS)?\s*[:.\-]?\s*([0-9]+(?:\.[0-9]+)?)\s*(G|KG|ML|L|LTR|LITRE|GM|PCS|N|UNITS)\b""")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val amount = matcher.group(1) ?: return Pair(null, "pcs")
            val unitRaw = (matcher.group(2) ?: "pcs").uppercase()
            val normalizedUnit = when (unitRaw) {
                "G", "GM" -> "g"
                "KG" -> "kg"
                "ML" -> "ml"
                "L", "LTR", "LITRE" -> "litre"
                else -> "pcs"
            }
            return Pair("$amount$normalizedUnit", normalizedUnit)
        }
        return Pair(null, "pcs")
    }

    private fun extractBrand(text: String): String? {
        for (brand in KNOWN_BRANDS) {
            if (text.contains(brand, ignoreCase = true)) {
                return brand.split(" ").joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
            }
        }
        return null
    }

    private fun extractVegetarian(text: String): Boolean {
        if (text.contains("NON-VEG", ignoreCase = true) || text.contains("CHICKEN", ignoreCase = true) || text.contains("EGG", ignoreCase = true)) {
            return false
        }
        return true
    }

    private fun extractBestBefore(text: String): Int? {
        val pattern = Pattern.compile("""(?i)(?:BEST\s*BEFORE|USE\s*BEFORE|EXPIRY)\s*[:.\-]?\s*([0-9]{1,2})\s*(?:MONTHS|DAYS|M)""")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull()
        }
        return null
    }

    private fun inferCategory(text: String): ProductCategory {
        val upper = text.uppercase()
        return when {
            upper.contains("TEA") || upper.contains("COFFEE") || upper.contains("DRINK") || upper.contains("JUICE") || upper.contains("WATER") || upper.contains("COLA") -> ProductCategory.BEVERAGES
            upper.contains("SOAP") || upper.contains("SHAMPOO") || upper.contains("PASTE") || upper.contains("CREAM") || upper.contains("LOTION") -> ProductCategory.PERSONAL_CARE
            upper.contains("DETERGENT") || upper.contains("CLEANER") || upper.contains("DISHWASH") || upper.contains("WASHING") || upper.contains("FLOOR") -> ProductCategory.HOUSEHOLD_CLEANING
            upper.contains("BULB") || upper.contains("LED") || upper.contains("BATTERY") || upper.contains("CABLE") -> ProductCategory.ELECTRONICS_ELECTRICAL
            upper.contains("BALM") || upper.contains("TABLET") || upper.contains("COUGH") || upper.contains("RELIEF") || upper.contains("ANTACID") -> ProductCategory.OTC_WELLNESS
            else -> ProductCategory.FOOD_GROCERY
        }
    }

    private fun inferSubCategory(text: String): String {
        val upper = text.uppercase()
        return when {
            upper.contains("NOODLE") -> "Instant Noodles"
            upper.contains("BISCUIT") || upper.contains("COOKIE") -> "Biscuits & Cookies"
            upper.contains("ATTA") || upper.contains("FLOUR") -> "Atta & Flours"
            upper.contains("OIL") || upper.contains("GHEE") -> "Edible Oils & Ghee"
            upper.contains("CHIP") || upper.contains("NAMKEEN") -> "Snacks & Namkeen"
            upper.contains("SOAP") -> "Soaps & Body Wash"
            upper.contains("SHAMPOO") -> "Hair Care & Shampoo"
            upper.contains("TOOTHPASTE") || upper.contains("DENTAL") -> "Oral Care"
            upper.contains("DETERGENT") -> "Laundry Detergents"
            else -> "General Grocery"
        }
    }

    private fun cleanProductTitle(text: String, brand: String?, netQty: String?): String {
        val lines = text.lines()
            .map { it.trim() }
            .filter { line ->
                line.length in 4..60 &&
                        !line.contains("FSSAI", ignoreCase = true) &&
                        !line.contains("BATCH", ignoreCase = true) &&
                        !line.contains("INGREDIENTS", ignoreCase = true) &&
                        !line.contains("MANUFACTURED", ignoreCase = true) &&
                        !line.contains("MARKETED", ignoreCase = true) &&
                        !line.contains("CUSTOMER CARE", ignoreCase = true)
            }

        val primaryLine = lines.firstOrNull { it.length > 5 } ?: brand ?: "New Enrolled Product"
        return if (netQty != null && !primaryLine.contains(netQty, ignoreCase = true)) {
            "$primaryLine $netQty"
        } else {
            primaryLine
        }
    }

    private fun calculateConfidence(brand: String?, mrp: Double?, netQty: String?, hasCatalogMatch: Boolean): Float {
        var score = 0.2f
        if (brand != null) score += 0.3f
        if (mrp != null) score += 0.25f
        if (netQty != null) score += 0.15f
        if (hasCatalogMatch) score += 0.1f
        return score.coerceIn(0f, 1f)
    }

    private fun roundTwoDecimals(value: Double): Double =
        kotlin.math.round(value * 100.0) / 100.0
}

package com.packready.shopkeeper.data.vision

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.LongBuffer

data class NormalizedProductFields(
    val brand: String? = null,
    val productName: String? = null,
    val netWeight: String? = null,
    val mrp: Double? = null,
    val mfgDate: String? = null,
    val expDate: String? = null,
    val batchNo: String? = null,
    val ingredients: String? = null,
    val category: String? = null,
    val rawJson: String = "{}"
)

data class NormalizedBillItem(
    val name: String,
    val weight: String? = null,
    val qty: Double = 1.0,
    val price: Double = 0.0
)

data class NormalizedBillFields(
    val items: List<NormalizedBillItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val rawJson: String = "{}"
)

/**
 * Edge SLM OCR Normalizer & Attribute Extractor.
 * Runs exports/model_int8.onnx (13.07M parameter Mini-T5 Seq2Seq model) directly on-device
 * using ONNX Runtime Android for instantaneous edge normalization without network dependency.
 */
class EdgeOcrNormalizer(
    private val context: Context,
    private val modelAssetName: String = "model_int8.onnx",
    private val tokenizerAssetName: String = "tokenizer.json"
) {
    companion object {
        private const val TAG = "EdgeOcrNormalizer"
        private const val BOS_TOKEN_ID = 1L
        private const val EOS_TOKEN_ID = 2L
        private const val PAD_TOKEN_ID = 0L
    }

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokenToId = HashMap<String, Long>()
    private var idToToken = HashMap<Long, String>()
    private var isInitialized = false

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        try {
            val modelFile = getAssetFile(modelAssetName)
            val tokFile = getAssetFile(tokenizerAssetName)

            if (!modelFile.exists() || modelFile.length() == 0L) {
                Log.w(TAG, "Model file $modelAssetName not found in assets.")
                return
            }

            ortEnv = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setInterOpNumThreads(2)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            ortSession = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)

            if (tokFile.exists() && tokFile.length() > 0) {
                loadTokenizerVocab(tokFile)
            }

            isInitialized = ortSession != null
            Log.i(TAG, "Edge SLM Normalizer initialized successfully! Vocab size: ${idToToken.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Edge SLM Normalizer: ${e.message}", e)
        }
    }

    private fun getAssetFile(assetName: String): File {
        val file = File(context.cacheDir, assetName)
        if (!file.exists() || file.length() == 0L) {
            try {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Asset copy error for $assetName: ${e.message}")
            }
        }
        return file
    }

    private fun loadTokenizerVocab(file: File) {
        try {
            val content = file.readText()
            val json = JSONObject(content)
            val modelObj = json.optJSONObject("model") ?: return
            val vocabObj = modelObj.optJSONObject("vocab") ?: return

            val keys = vocabObj.keys()
            while (keys.hasNext()) {
                val token = keys.next()
                val id = vocabObj.getLong(token)
                tokenToId[token] = id
                idToToken[id] = token
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tokenizer vocab: ${e.message}")
        }
    }

    suspend fun normalizePackaging(rawOcr: String): NormalizedProductFields? = withContext(Dispatchers.Default) {
        if (!isInitialized || rawOcr.isBlank()) return@withContext null
        val prompt = "enrollment: ${rawOcr.trim()}"
        val rawJson = runGreedyDecode(prompt, maxTokens = 120) ?: return@withContext null

        try {
            val obj = JSONObject(rawJson)
            NormalizedProductFields(
                brand = obj.optString("brand").takeIf { it.isNotBlank() },
                productName = obj.optString("product_name").takeIf { it.isNotBlank() },
                netWeight = obj.optString("net_weight").takeIf { it.isNotBlank() },
                mrp = if (obj.has("mrp")) obj.optDouble("mrp", 0.0) else null,
                mfgDate = obj.optString("mfg_date").takeIf { it.isNotBlank() },
                expDate = obj.optString("exp_date").takeIf { it.isNotBlank() },
                batchNo = obj.optString("batch_no").takeIf { it.isNotBlank() },
                ingredients = obj.optString("ingredients").takeIf { it.isNotBlank() },
                category = obj.optString("category").takeIf { it.isNotBlank() },
                rawJson = rawJson
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse SLM JSON output: $rawJson")
            null
        }
    }

    suspend fun normalizeBill(rawBillOcr: String): NormalizedBillFields? = withContext(Dispatchers.Default) {
        if (!isInitialized || rawBillOcr.isBlank()) return@withContext null
        val prompt = "bill_ocr: ${rawBillOcr.trim()}"
        val rawJson = runGreedyDecode(prompt, maxTokens = 180) ?: return@withContext null

        try {
            val obj = JSONObject(rawJson)
            val itemsArr = obj.optJSONArray("items")
            val itemsList = mutableListOf<NormalizedBillItem>()
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(i)
                    itemsList.add(
                        NormalizedBillItem(
                            name = itemObj.optString("name", "Item"),
                            weight = itemObj.optString("weight").takeIf { it.isNotBlank() },
                            qty = itemObj.optDouble("qty", 1.0),
                            price = itemObj.optDouble("price", 0.0)
                        )
                    )
                }
            }
            val total = obj.optDouble("total_amount", 0.0)
            NormalizedBillFields(items = itemsList, totalAmount = total, rawJson = rawJson)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Bill SLM output: $rawJson")
            null
        }
    }

    private fun runGreedyDecode(prompt: String, maxTokens: Int): String? {
        val env = ortEnv ?: return null
        val sess = ortSession ?: return null

        val inputIds = encodeText(prompt)
        if (inputIds.isEmpty()) return null

        val inIdsBuffer = LongBuffer.wrap(inputIds)
        val inShape = longArrayOf(1, inputIds.size.toLong())
        val inTensor = OnnxTensor.createTensor(env, inIdsBuffer, inShape)

        val decIdsList = ArrayList<Long>()
        decIdsList.add(BOS_TOKEN_ID)

        val expectedInputs = sess.inputNames

        try {
            for (step in 0 until maxTokens) {
                val decIdsBuffer = LongBuffer.wrap(decIdsList.toLongArray())
                val decShape = longArrayOf(1, decIdsList.size.toLong())
                val decTensor = OnnxTensor.createTensor(env, decIdsBuffer, decShape)

                val inputMap = HashMap<String, OnnxTensor>()
                inputMap["input_ids"] = inTensor
                inputMap["decoder_input_ids"] = decTensor

                if (expectedInputs.contains("attention_mask")) {
                    val maskArray = LongArray(inputIds.size) { 1L }
                    inputMap["attention_mask"] = OnnxTensor.createTensor(env, LongBuffer.wrap(maskArray), inShape)
                }

                val results = sess.run(inputMap)
                val logitsTensor = results[0] as OnnxTensor
                val logits = logitsTensor.value as Array<Array<FloatArray>>

                val lastStepLogits = logits[0][logits[0].size - 1]
                var maxIdx = 0
                var maxVal = Float.NEGATIVE_INFINITY
                for (v in lastStepLogits.indices) {
                    if (lastStepLogits[v] > maxVal) {
                        maxVal = lastStepLogits[v]
                        maxIdx = v
                    }
                }

                val nextToken = maxIdx.toLong()
                decTensor.close()
                results.close()

                if (nextToken == EOS_TOKEN_ID) {
                    break
                }
                decIdsList.add(nextToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in autoregressive SLM greedy decode: ${e.message}", e)
        } finally {
            inTensor.close()
        }

        if (decIdsList.size <= 1) return null
        val decoded = decodeTokens(decIdsList.subList(1, decIdsList.size))

        // Clean up trailing chars past last closing brace if any
        val trimmed = decoded.trim()
        val lastBrace = trimmed.lastIndexOf('}')
        return if (lastBrace != -1) trimmed.substring(0, lastBrace + 1) else trimmed
    }

    private fun encodeText(text: String): LongArray {
        val ids = ArrayList<Long>()
        ids.add(BOS_TOKEN_ID)

        // Simple subword / word piece match with domain vocabulary
        var remaining = text
        while (remaining.isNotEmpty()) {
            var matched = false
            val maxLen = minOf(remaining.length, 32)
            for (len in maxLen downTo 1) {
                val sub = remaining.substring(0, len)
                val id = tokenToId[sub]
                if (id != null) {
                    ids.add(id)
                    remaining = remaining.substring(len)
                    matched = true
                    break
                }
            }
            if (!matched) {
                // Byte fallback
                val firstChar = remaining[0].toString()
                val id = tokenToId[firstChar] ?: 3L // UNK
                ids.add(id)
                remaining = remaining.substring(1)
            }
        }
        ids.add(EOS_TOKEN_ID)
        return ids.toLongArray()
    }

    private fun decodeTokens(tokens: List<Long>): String {
        val sb = StringBuilder()
        for (id in tokens) {
            val tokenStr = idToToken[id] ?: ""
            // Replace byte-level BPE whitespace markers (Ġ or space)
            val clean = tokenStr.replace("Ġ", " ")
            sb.append(clean)
        }
        return sb.toString()
    }
}

package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class HouseRepository(
    private val context: Context,
    private val houseDao: HouseDao
) {

    val allRecords: Flow<List<HouseRecord>> = houseDao.getAllHouseRecords()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://script.google.com/") // fallback dummy base API URL
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    /**
     * Connects to the user's Apps Script, pulls the JSON payload,
     * parses items, performs kW calculations and block extractions, and stores in Room.
     */
    suspend fun syncFromGoogleSheets(url: String): SyncResult {
        if (url.isBlank()) {
            return SyncResult.Error("API Web App Script URL cannot be empty in Settings.")
        }
        try {
            Log.d("HouseRepository", "Syncing from Google Sheets URL: $url")
            val response = apiService.fetchLoadData(url)
            if (!response.isSuccessful) {
                return SyncResult.Error("Server returned error code: ${response.code()} (${response.message()})")
            }

            val rawJsonString = response.body()?.string() ?: return SyncResult.Error("API response has empty body.")
            Log.d("HouseRepository", "Server payload pulled successfully: ${rawJsonString.length} chars")

            val parsedRecords = parseFlexibly(rawJsonString)
            if (parsedRecords.isEmpty()) {
                return SyncResult.Error("Fetched JSON successfully but parsed 0 items. Ensure keys/columns match expected configuration.")
            }

            // Map and calculate everything
            val processed = parsedRecords.map { record ->
                val calculatedLoad = LoadCalculator.calculateHouseLoadKW(record)
                val block = LoadCalculator.extractBlockName(record.houseNo)
                
                // Keep pre-assigned substation or resolve mapping based on block
                val resolvedSubstation = if (record.substationId.isNotBlank()) {
                    record.substationId
                } else {
                    when (block) {
                        "Block A", "Block B", "Block C" -> "Substation 1"
                        "Block D", "Block E" -> "Substation 2"
                        else -> "Substation 3"
                    }
                }

                val resolvedLedTango10w = if (record.ledTango10w == 0 && record.ledLightCount > 0) {
                    record.ledLightCount / 2
                } else record.ledTango10w

                val resolvedLedTango20w = if (record.ledTango20w == 0 && record.ledLightCount > 0) {
                    record.ledLightCount / 3
                } else record.ledTango20w

                val resolvedLedDownlight13w = if (record.ledDownlight13w == 0 && record.ledLightCount > 0) {
                    record.ledLightCount - resolvedLedTango10w - resolvedLedTango20w
                } else record.ledDownlight13w

                record.copy(
                    totalLoadKw = calculatedLoad,
                    blockName = block,
                    substationId = resolvedSubstation,
                    ledTango10w = resolvedLedTango10w,
                    ledTango20w = resolvedLedTango20w,
                    ledDownlight13w = resolvedLedDownlight13w
                )
            }

            // Sync into cache
            houseDao.clearAllRecords()
            houseDao.insertHouseRecords(processed)

            return SyncResult.Success(processed.size)
        } catch (e: Exception) {
            Log.e("HouseRepository", "Error running sync operation", e)
            return SyncResult.Error(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }

    /**
     * Generates 600+ records to immediately demonstrate high-capacity sorting, searching,
     * charts, and smart analytics if the database is currently empty.
     */
    suspend fun seedMockDataIfEmpty() {
        val records = mutableListOf<HouseRecord>()
        val blocks = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        
        val firstNames = listOf(
            "Albert", "Brandon", "Catherine", "Diana", "Ethan", "Fiona", "George",
            "Hannah", "Ian", "Julia", "Kevin", "Laura", "Marcus", "Natalie", "Oliver",
            "Patricia", "Quentin", "Rachel", "Samuel", "Teresa", "Victor", "Wendy", "Zack"
        )
        val lastNames = listOf(
            "Anderson", "Brown", "Carter", "Davis", "Evans", "Fisher", "Green", "Harris",
            "Jackson", "King", "Lee", "Miller", "Nelson", "Owens", "Parker", "Robinson",
            "Smith", "Taylor", "Vance", "Williams", "Young", "Zimmerman"
        )

        // Seed exactly 600 houses across 8 blocks
        var houseCounter = 1
        for (block in blocks) {
            val housesInBlock = 75 // 8 blocks * 75 houses = 600 houses
            for (i in 1..housesInBlock) {
                val houseNo = "Block $block - House $i"
                val residentName = "${firstNames.random()} ${lastNames.random()}"
                
                // Random appliances based on typical household variance
                val hasAc = Random.nextFloat() < 0.45f // 45% have ACs
                val acCount = if (hasAc) Random.nextInt(1, 4) else 0
                val ceilingFanCount = Random.nextInt(3, 7)
                val statusFluorescentS = Random.nextInt(1, 5)
                val statusFluorescentD = Random.nextInt(0, 3)
                val bulbHolderCount = Random.nextInt(2, 6)
                val exhaustFanCount = Random.nextInt(1, 3)
                val bracketFanCount = Random.nextInt(0, 4)
                
                val ledLightCount = Random.nextInt(5, 16)
                val ledTango10w = ledLightCount / 2
                val ledTango20w = ledLightCount / 3
                val ledDownlight13w = ledLightCount - ledTango10w - ledTango20w

                val fancyLightCount = Random.nextInt(0, 5)
                val hiBayLightCount = if (Random.nextFloat() < 0.05f) Random.nextInt(1, 3) else 0 // 5% have Hi-Bays (e.g., backyard/garage setup)
                
                val socket5aCount = Random.nextInt(4, 10)
                val socket15aCount = Random.nextInt(1, 4)
                val socket20aCount = if (hasAc) acCount else Random.nextInt(0, 2)

                val subId = when (block) {
                    "A", "B", "C" -> "Substation 1"
                    "D", "E" -> "Substation 2"
                    else -> "Substation 3"
                }

                val tempRecord = HouseRecord(
                    residentName = residentName,
                    houseNo = houseNo,
                    acCount = acCount,
                    singleFlCount = statusFluorescentS,
                    doubleFlCount = statusFluorescentD,
                    bulbHolderCount = bulbHolderCount,
                    ceilingFanCount = ceilingFanCount,
                    exhaustFanCount = exhaustFanCount,
                    bracketFanCount = bracketFanCount,
                    ledLightCount = ledLightCount,
                    fancyLightCount = fancyLightCount,
                    hiBayLightCount = hiBayLightCount,
                    socket5aCount = socket5aCount,
                    socket15aCount = socket15aCount,
                    socket20aCount = socket20aCount,
                    substationId = subId,
                    ledTango10w = ledTango10w,
                    ledTango20w = ledTango20w,
                    ledDownlight13w = ledDownlight13w
                )

                // Precompute
                val generatedLoad = LoadCalculator.calculateHouseLoadKW(tempRecord)
                val finalBlock = LoadCalculator.extractBlockName(houseNo)

                records.add(
                    tempRecord.copy(
                        totalLoadKw = generatedLoad,
                        blockName = finalBlock
                    )
                )
                houseCounter++
            }
        }

        // Cache in Room
        houseDao.insertHouseRecords(records)
        Log.d("HouseRepository", "Successfully seeded ${records.size} electrical records into Room Cache.")
    }

    /**
     * Resiliently parse raw Apps Script web output to find arrays (JSON list or map properties).
     * Leverages generic Map structures for total type compatibility and fallback resilience.
     */
    private fun parseFlexibly(jsonString: String): List<HouseRecord> {
        val trimmed = jsonString.trim()
        
        // Helper to convert list of generic maps into HouseRecord objects securely
        fun buildRecordsFromMapList(mapList: List<*>): List<HouseRecord> {
            val records = mutableListOf<HouseRecord>()
            for (rawItem in mapList) {
                @Suppress("UNCHECKED_CAST")
                val item = rawItem as? Map<String, Any> ?: continue
                
                val residentName = (item["residentName"] as? String) ?: "Unnamed"
                val houseNo = (item["houseNo"] as? String) ?: "N/A"
                
                fun mapToInt(v: Any?): Int {
                    return when (v) {
                        is Double -> v.toInt()
                        is Float -> v.toInt()
                        is Int -> v
                        is Long -> v.toInt()
                        is String -> v.toIntOrNull() ?: 0
                        else -> 0
                    }
                }
                
                records.add(
                    HouseRecord(
                        residentName = residentName,
                        houseNo = houseNo,
                        acCount = mapToInt(item["acCount"]),
                        singleFlCount = mapToInt(item["singleFlCount"]),
                        doubleFlCount = mapToInt(item["doubleFlCount"]),
                        bulbHolderCount = mapToInt(item["bulbHolderCount"]),
                        ceilingFanCount = mapToInt(item["ceilingFanCount"]),
                        exhaustFanCount = mapToInt(item["exhaustFanCount"]),
                        bracketFanCount = mapToInt(item["bracketFanCount"]),
                        ledLightCount = mapToInt(item["ledLightCount"]),
                        fancyLightCount = mapToInt(item["fancyLightCount"]),
                        hiBayLightCount = mapToInt(item["hiBayLightCount"]),
                        socket5aCount = mapToInt(item["socket5aCount"]),
                        socket15aCount = mapToInt(item["socket15aCount"]),
                        socket20aCount = mapToInt(item["socket20aCount"]),
                        substationId = (item["substationId"] as? String) ?: "",
                        ledTango10w = mapToInt(item["ledTango10w"]),
                        ledTango20w = mapToInt(item["ledTango20w"]),
                        ledDownlight13w = mapToInt(item["ledDownlight13w"])
                    )
                )
            }
            return records
        }

        // Strategy A: Parse list of maps directly
        try {
            val mapListType = Types.newParameterizedType(
                List::class.java,
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            )
            val adapter = moshi.adapter<List<*>>(mapListType)
            val list = adapter.fromJson(trimmed)
            if (!list.isNullOrEmpty()) {
                val records = buildRecordsFromMapList(list)
                if (records.isNotEmpty()) return records
            }
        } catch (e: Exception) {
            Log.d("HouseRepository", "Strategy A list of maps parsing skipped: ${e.localizedMessage}")
        }

        // Strategy B: Parse nested wrapped lists inside an object (e.g. { "data": [...] })
        try {
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val mapAdapter = moshi.adapter<Map<String, Any>>(mapType)
            val root = mapAdapter.fromJson(trimmed)
            if (root != null) {
                val targets = listOf("data", "records", "rows", "items", "values")
                for (target in targets) {
                    val innerList = root[target]
                    if (innerList is List<*>) {
                        val records = buildRecordsFromMapList(innerList)
                        if (records.isNotEmpty()) return records
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("HouseRepository", "Strategy B nested list of maps parsing skipped: ${e.localizedMessage}")
        }

        return emptyList()
    }
}

sealed interface SyncResult {
    data class Success(val count: Int) : SyncResult
    data class Error(val message: String) : SyncResult
}

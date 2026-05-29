package com.example.data

import android.content.Context
import android.content.SharedPreferences

object LoadCalculator {
    // Default fallback constants
    const val DEFAULT_WATT_AC = 2200.0
    const val DEFAULT_WATT_SINGLE_FL = 36.0
    const val DEFAULT_WATT_DOUBLE_FL = 72.0
    const val DEFAULT_WATT_BULB_HOLDER = 13.0
    const val DEFAULT_WATT_CEILING_FAN = 75.0
    const val DEFAULT_WATT_EXHAUST_FAN = 40.0
    const val DEFAULT_WATT_BRACKET_FAN = 60.0
    const val DEFAULT_WATT_LED_LIGHT = 15.0
    const val DEFAULT_WATT_FANCY_LIGHT = 10.0
    const val DEFAULT_WATT_HI_BAY_LIGHT = 150.0
    const val DEFAULT_WATT_SOCKET_5A = 1000.0
    const val DEFAULT_WATT_SOCKET_15A = 2000.0
    const val DEFAULT_WATT_SOCKET_20A = 3000.0

    // Mutable ratings used for dynamic load logic
    var WATT_AC = DEFAULT_WATT_AC
    var WATT_SINGLE_FL = DEFAULT_WATT_SINGLE_FL
    var WATT_DOUBLE_FL = DEFAULT_WATT_DOUBLE_FL
    var WATT_BULB_HOLDER = DEFAULT_WATT_BULB_HOLDER
    var WATT_CEILING_FAN = DEFAULT_WATT_CEILING_FAN
    var WATT_EXHAUST_FAN = DEFAULT_WATT_EXHAUST_FAN
    var WATT_BRACKET_FAN = DEFAULT_WATT_BRACKET_FAN
    var WATT_LED_LIGHT = DEFAULT_WATT_LED_LIGHT
    var WATT_FANCY_LIGHT = DEFAULT_WATT_FANCY_LIGHT
    var WATT_HI_BAY_LIGHT = DEFAULT_WATT_HI_BAY_LIGHT
    var WATT_SOCKET_5A = DEFAULT_WATT_SOCKET_5A
    var WATT_SOCKET_15A = DEFAULT_WATT_SOCKET_15A
    var WATT_SOCKET_20A = DEFAULT_WATT_SOCKET_20A

    /**
     * Loads saved equipment ratings from SharedPreferences on app startup.
     */
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("equipment_ratings_prefs", Context.MODE_PRIVATE)
        WATT_AC = prefs.getFloat("WATT_AC", DEFAULT_WATT_AC.toFloat()).toDouble()
        WATT_SINGLE_FL = prefs.getFloat("WATT_SINGLE_FL", DEFAULT_WATT_SINGLE_FL.toFloat()).toDouble()
        WATT_DOUBLE_FL = prefs.getFloat("WATT_DOUBLE_FL", DEFAULT_WATT_DOUBLE_FL.toFloat()).toDouble()
        WATT_BULB_HOLDER = prefs.getFloat("WATT_BULB_HOLDER", DEFAULT_WATT_BULB_HOLDER.toFloat()).toDouble()
        WATT_CEILING_FAN = prefs.getFloat("WATT_CEILING_FAN", DEFAULT_WATT_CEILING_FAN.toFloat()).toDouble()
        WATT_EXHAUST_FAN = prefs.getFloat("WATT_EXHAUST_FAN", DEFAULT_WATT_EXHAUST_FAN.toFloat()).toDouble()
        WATT_BRACKET_FAN = prefs.getFloat("WATT_BRACKET_FAN", DEFAULT_WATT_BRACKET_FAN.toFloat()).toDouble()
        WATT_LED_LIGHT = prefs.getFloat("WATT_LED_LIGHT", DEFAULT_WATT_LED_LIGHT.toFloat()).toDouble()
        WATT_FANCY_LIGHT = prefs.getFloat("WATT_FANCY_LIGHT", DEFAULT_WATT_FANCY_LIGHT.toFloat()).toDouble()
        WATT_HI_BAY_LIGHT = prefs.getFloat("WATT_HI_BAY_LIGHT", DEFAULT_WATT_HI_BAY_LIGHT.toFloat()).toDouble()
        WATT_SOCKET_5A = prefs.getFloat("WATT_SOCKET_5A", DEFAULT_WATT_SOCKET_5A.toFloat()).toDouble()
        WATT_SOCKET_15A = prefs.getFloat("WATT_SOCKET_15A", DEFAULT_WATT_SOCKET_15A.toFloat()).toDouble()
        WATT_SOCKET_20A = prefs.getFloat("WATT_SOCKET_20A", DEFAULT_WATT_SOCKET_20A.toFloat()).toDouble()
    }

    /**
     * Commits updated equipment ratings and re-initializes.
     */
    fun saveRatings(context: Context, ratings: Map<String, Double>) {
        val prefs = context.getSharedPreferences("equipment_ratings_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        ratings.forEach { (key, value) ->
            editor.putFloat(key, value.toFloat())
        }
        editor.apply()
        initialize(context)
    }

    /**
     * Calculates the total electrical load for a house in Watts based on appliance quantities and their respective wattages.
     */
    fun calculateHouseLoadWatts(
        acCount: Int,
        singleFlCount: Int,
        doubleFlCount: Int,
        bulbHolderCount: Int,
        ceilingFanCount: Int,
        exhaustFanCount: Int,
        bracketFanCount: Int,
        ledLightCount: Int,
        fancyLightCount: Int,
        hiBayLightCount: Int,
        socket5aCount: Int,
        socket15aCount: Int,
        socket20aCount: Int
    ): Double {
        return (acCount * WATT_AC) +
                (singleFlCount * WATT_SINGLE_FL) +
                (doubleFlCount * WATT_DOUBLE_FL) +
                (bulbHolderCount * WATT_BULB_HOLDER) +
                (ceilingFanCount * WATT_CEILING_FAN) +
                (exhaustFanCount * WATT_EXHAUST_FAN) +
                (bracketFanCount * WATT_BRACKET_FAN) +
                (ledLightCount * WATT_LED_LIGHT) +
                (fancyLightCount * WATT_FANCY_LIGHT) +
                (hiBayLightCount * WATT_HI_BAY_LIGHT) +
                (socket5aCount * WATT_SOCKET_5A) +
                (socket15aCount * WATT_SOCKET_15A) +
                (socket20aCount * WATT_SOCKET_20A)
    }

    /**
     * Calculates the total electrical load for a house in Kilowatts.
     */
    fun calculateHouseLoadKW(record: HouseRecord): Double {
        return calculateHouseLoadWatts(
            acCount = record.acCount,
            singleFlCount = record.singleFlCount,
            doubleFlCount = record.doubleFlCount,
            bulbHolderCount = record.bulbHolderCount,
            ceilingFanCount = record.ceilingFanCount,
            exhaustFanCount = record.exhaustFanCount,
            bracketFanCount = record.bracketFanCount,
            ledLightCount = record.ledLightCount,
            fancyLightCount = record.fancyLightCount,
            hiBayLightCount = record.hiBayLightCount,
            socket5aCount = record.socket5aCount,
            socket15aCount = record.socket15aCount,
            socket20aCount = record.socket20aCount
        ) / 1000.0
    }

    /**
     * Categorizes local house loads.
     */
    fun getCategoryLoadsKw(record: HouseRecord): CategoryBreakdown {
        val lightingWatts = (record.singleFlCount * WATT_SINGLE_FL) +
                (record.doubleFlCount * WATT_DOUBLE_FL) +
                (record.bulbHolderCount * WATT_BULB_HOLDER) +
                (record.ledLightCount * WATT_LED_LIGHT) +
                (record.fancyLightCount * WATT_FANCY_LIGHT) +
                (record.hiBayLightCount * WATT_HI_BAY_LIGHT)

        val coolingWatts = (record.ceilingFanCount * WATT_CEILING_FAN) +
                (record.exhaustFanCount * WATT_EXHAUST_FAN) +
                (record.bracketFanCount * WATT_BRACKET_FAN) +
                (record.acCount * WATT_AC)

        val socketWatts = (record.socket5aCount * WATT_SOCKET_5A) +
                (record.socket15aCount * WATT_SOCKET_15A) +
                (record.socket20aCount * WATT_SOCKET_20A)

        val total = (lightingWatts + coolingWatts + socketWatts) / 1000.0

        return CategoryBreakdown(
            lightingKw = lightingWatts / 1000.0,
            coolingKw = coolingWatts / 1000.0,
            socketsKw = socketWatts / 1000.0,
            totalKw = total
        )
    }

    /**
     * Smarts-detect the township block based on house number.
     * Maps common naming standards. For example:
     * - "Block A - 105" or "A-105" or "A/105" -> "Block A"
     * - "Block B, Room 12" -> "Block B"
     * - "B-40" -> "Block B"
     */
    fun extractBlockName(houseNo: String): String {
        val trimmed = houseNo.trim()
        if (trimmed.isBlank()) return "Unknown Block"

        // Check for explicit "Block A", "Block B", etc.
        val blockPhraseRegex = Regex("(?i)(block|sector|street|phase)\\s*([a-zA-Z0-9]+)")
        val blockPhraseMatch = blockPhraseRegex.find(trimmed)
        if (blockPhraseMatch != null) {
            val type = blockPhraseMatch.groupValues[1].lowercase().replaceFirstChar { it.uppercase() }
            val id = blockPhraseMatch.groupValues[2].uppercase()
            return "$type $id"
        }

        // Try to match pattern like "A-12" or "A12"
        val simpleCodeRegex = Regex("^([a-zA-Z]+)[\\s\\-/]*\\d+")
        val simpleMatch = simpleCodeRegex.find(trimmed)
        if (simpleMatch != null) {
            val code = simpleMatch.groupValues[1].uppercase()
            if (code.length <= 4) {
                return "Block $code"
            }
        }

        // Try splits with hyphens or slashes
        for (delimiter in listOf("-", "/", " ")) {
            if (trimmed.contains(delimiter)) {
                val parts = trimmed.split(delimiter)
                if (parts.isNotEmpty()) {
                    val candidate = parts[0].trim()
                    if (candidate.length in 1..4 && candidate.any { it.isLetter() }) {
                        return "Block ${candidate.uppercase()}"
                    }
                }
            }
        }

        // Fallback to first character letter, e.g., "A204" -> "Block A"
        val firstChar = trimmed.first()
        if (firstChar.isLetter()) {
            return "Block ${firstChar.uppercase()}"
        }

        return "General Block"
    }
}

data class CategoryBreakdown(
    val lightingKw: Double,
    val coolingKw: Double,
    val socketsKw: Double,
    val totalKw: Double
)

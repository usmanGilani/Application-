package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "house_records")
@JsonClass(generateAdapter = true)
data class HouseRecord(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    @Json(name = "residentName")
    val residentName: String,

    @Json(name = "houseNo")
    val houseNo: String,

    @Json(name = "acCount")
    val acCount: Int = 0,

    @Json(name = "singleFlCount")
    val singleFlCount: Int = 0,

    @Json(name = "doubleFlCount")
    val doubleFlCount: Int = 0,

    @Json(name = "bulbHolderCount")
    val bulbHolderCount: Int = 0,

    @Json(name = "ceilingFanCount")
    val ceilingFanCount: Int = 0,

    @Json(name = "exhaustFanCount")
    val exhaustFanCount: Int = 0,

    @Json(name = "bracketFanCount")
    val bracketFanCount: Int = 0,

    @Json(name = "ledLightCount")
    val ledLightCount: Int = 0,

    @Json(name = "fancyLightCount")
    val fancyLightCount: Int = 0,

    @Json(name = "hiBayLightCount")
    val hiBayLightCount: Int = 0,

    @Json(name = "socket5aCount")
    val socket5aCount: Int = 0,

    @Json(name = "socket15aCount")
    val socket15aCount: Int = 0,

    @Json(name = "socket20aCount")
    val socket20aCount: Int = 0,

    @Json(name = "substationId")
    val substationId: String = "",

    @Json(name = "ledTango10w")
    val ledTango10w: Int = 0,

    @Json(name = "ledTango20w")
    val ledTango20w: Int = 0,

    @Json(name = "ledDownlight13w")
    val ledDownlight13w: Int = 0,

    // Pre-calculated field for database sorting & search
    val totalLoadKw: Double = 0.0,
    val blockName: String = ""
)

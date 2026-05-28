package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseDao {
    @Query("SELECT * FROM house_records ORDER BY houseNo ASC")
    fun getAllHouseRecords(): Flow<List<HouseRecord>>

    @Query("SELECT * FROM house_records WHERE localId = :id LIMIT 1")
    suspend fun getHouseRecordById(id: Int): HouseRecord?

    @Query("SELECT * FROM house_records WHERE houseNo = :houseNo LIMIT 1")
    suspend fun getHouseRecordByHouseNo(houseNo: String): HouseRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouseRecords(records: List<HouseRecord>)

    @Query("DELETE FROM house_records")
    suspend fun clearAllRecords()
}

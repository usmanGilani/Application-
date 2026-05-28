package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Loading : SyncUiState
    data class Success(val message: String) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

enum class SortOrder {
    LOAD_DESC, // High -> Low Load
    LOAD_ASC,  // Low -> High Load
    HOUSE_ASC, // House No. Ascending
    NAME_ASC   // Name A-Z
}

data class BlockLoadSummary(
    val blockName: String,
    val totalLoadKw: Double,
    val averageLoadKw: Double,
    val houseCount: Int,
    val isHighLoad: Boolean,
    val loadPercentage: Float
)

enum class LoadStatus {
    SAFE, MODERATE, OVERLOADED
}

data class SubstationSummary(
    val substationId: String,
    val capacityKva: Double,
    val voltageRating: String,
    val totalConnectedLoadKw: Double,
    val houseCount: Int,
    val blockNames: List<String>,
    val loadingPercentage: Double,
    val status: LoadStatus,
    val predictedOverloadRisk: String,
    val isRiskHighKind: Boolean
)

data class TownshipAnalytics(
    val totalConnectedLoadKw: Double,
    val totalHousesCount: Int,
    val loadGaugePercentage: Float, // Utilization relative to 3000 kW (3 MW) reference capacity
    val activeSubstationKva: Double, // Substation transformer usage simulation
    val averageLoadPerHouseKw: Double
)

class LoadViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(
        "smart_township_prefs",
        Context.MODE_PRIVATE
    )

    private val houseDao = AppDatabase.getDatabase(application).houseDao()
    private val repository = HouseRepository(application, houseDao)

    // Sync Web App Script URL configuration state
    private val _webScriptUrl = MutableStateFlow(
        sharedPrefs.getString("web_script_url", "") ?: ""
    )
    val webScriptUrl: StateFlow<String> = _webScriptUrl.asStateFlow()

    // Screen state
    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    // Navigation and detail selection states
    private val _selectedHouseNo = MutableStateFlow<String?>(null)
    val selectedHouseNo: StateFlow<String?> = _selectedHouseNo.asStateFlow()

    // Filters and sorting states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBlockFilter = MutableStateFlow("All")
    val selectedBlockFilter: StateFlow<String> = _selectedBlockFilter.asStateFlow()

    private val _selectedSubstationFilter = MutableStateFlow("All")
    val selectedSubstationFilter: StateFlow<String> = _selectedSubstationFilter.asStateFlow()

    private val _houseRangeStart = MutableStateFlow("")
    val houseRangeStart: StateFlow<String> = _houseRangeStart.asStateFlow()

    private val _houseRangeEnd = MutableStateFlow("")
    val houseRangeEnd: StateFlow<String> = _houseRangeEnd.asStateFlow()

    private val _acMinFilter = MutableStateFlow<Int?>(null)
    val acMinFilter: StateFlow<Int?> = _acMinFilter.asStateFlow()

    private val _ledTango10wMinFilter = MutableStateFlow<Int?>(null)
    val ledTango10wMinFilter: StateFlow<Int?> = _ledTango10wMinFilter.asStateFlow()

    private val _ledTango20wMinFilter = MutableStateFlow<Int?>(null)
    val ledTango20wMinFilter: StateFlow<Int?> = _ledTango20wMinFilter.asStateFlow()

    private val _ledDownlight13wMinFilter = MutableStateFlow<Int?>(null)
    val ledDownlight13wMinFilter: StateFlow<Int?> = _ledDownlight13wMinFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.LOAD_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Seed mock database status track (checks once on launch)
    init {
        viewModelScope.launch {
            repository.allRecords.first().let { currentList ->
                if (currentList.isEmpty()) {
                    Log.d("LoadViewModel", "Database is empty on start. Populating mock 600 records...")
                    _syncState.value = SyncUiState.Loading
                    try {
                        withContext(Dispatchers.IO) {
                            repository.seedMockDataIfEmpty()
                        }
                        _syncState.value = SyncUiState.Success("Loaded mock database with 600 households.")
                    } catch (e: Exception) {
                        _syncState.value = SyncUiState.Error("Failed seeding mock dataset.")
                    }
                }
            }
        }
    }

    /**
     * Updates and saves the dynamic Apps Script URL.
     */
    fun updateWebScriptUrl(newUrl: String) {
        _webScriptUrl.value = newUrl.trim()
        sharedPrefs.edit().putString("web_script_url", newUrl.trim()).apply()
    }

    /**
     * Resets database to demo mock data anytime.
     */
    fun resetToDemoData() {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    houseDao.clearAllRecords()
                    repository.seedMockDataIfEmpty()
                }
                _syncState.value = SyncUiState.Success("Database reset to 600 demo fields.")
            } catch (e: Exception) {
                _syncState.value = SyncUiState.Error("Database reset failed.")
            }
        }
    }

    /**
     * Triggers active Google Sheets Google Apps Script API synchronization.
     */
    fun syncWithGoogleSheets() {
        val url = _webScriptUrl.value
        if (url.isBlank()) {
            _syncState.value = SyncUiState.Error("Please set your Apps Script URL in Settings.")
            return
        }

        viewModelScope.launch {
            _syncState.value = SyncUiState.Loading
            val result = withContext(Dispatchers.IO) {
                repository.syncFromGoogleSheets(url)
            }
            when (result) {
                is SyncResult.Success -> {
                    _syncState.value = SyncUiState.Success("Sync successful! Processed ${result.count} houses.")
                }
                is SyncResult.Error -> {
                    _syncState.value = SyncUiState.Error(result.message)
                }
            }
        }
    }

    fun dismissSyncState() {
        _syncState.value = SyncUiState.Idle
    }

    fun selectHouseNo(houseNo: String?) {
        _selectedHouseNo.value = houseNo
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateBlockFilter(block: String) {
        _selectedBlockFilter.value = block
    }

    fun updateSubstationFilter(sub: String) {
        _selectedSubstationFilter.value = sub
    }

    fun updateHouseRange(start: String, end: String) {
        _houseRangeStart.value = start.trim()
        _houseRangeEnd.value = end.trim()
    }

    fun updateAcMinFilter(min: Int?) {
        _acMinFilter.value = min
    }

    fun updateLedTango10wFilter(min: Int?) {
        _ledTango10wMinFilter.value = min
    }

    fun updateLedTango20wFilter(min: Int?) {
        _ledTango20wMinFilter.value = min
    }

    fun updateLedDownlight13wFilter(min: Int?) {
        _ledDownlight13wMinFilter.value = min
    }

    fun clearAllAdvancedFilters() {
        _selectedBlockFilter.value = "All"
        _selectedSubstationFilter.value = "All"
        _houseRangeStart.value = ""
        _houseRangeEnd.value = ""
        _acMinFilter.value = null
        _ledTango10wMinFilter.value = null
        _ledTango20wMinFilter.value = null
        _ledDownlight13wMinFilter.value = null
        _searchQuery.value = ""
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    // Exposed lists of available blocks derived dynamically from cached records
    val blockList: StateFlow<List<String>> = repository.allRecords
        .map { list ->
            list.map { it.blockName }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unified Reactive flow mapping local records under filters, search, and sorting.
    data class QuadGeo(val block: String, val sub: String, val hStart: String, val hEnd: String)
    data class QuadAppliance(val ac: Int?, val led10: Int?, val led20: Int?, val ledDown: Int?)

    private val geographicFilters = combine(
        _selectedBlockFilter,
        _selectedSubstationFilter,
        _houseRangeStart,
        _houseRangeEnd
    ) { block, sub, hStart, hEnd ->
        QuadGeo(block, sub, hStart, hEnd)
    }

    private val applianceFilters = combine(
        _acMinFilter,
        _ledTango10wMinFilter,
        _ledTango20wMinFilter,
        _ledDownlight13wMinFilter
    ) { ac, led10, led20, ledDown ->
        QuadAppliance(ac, led10, led20, ledDown)
    }

    @OptIn(FlowPreview::class)
    val filteredHouseRecords: StateFlow<List<HouseRecord>> = combine(
        repository.allRecords,
        _searchQuery.debounce(100),
        geographicFilters,
        applianceFilters,
        _sortOrder
    ) { records, query, geo, app, sorting ->
        var list = records

        // Apply Search (Matches Resident Name or House Number)
        if (query.isNotBlank()) {
            list = list.filter {
                it.residentName.contains(query, ignoreCase = true) ||
                it.houseNo.contains(query, ignoreCase = true)
            }
        }

        // Apply Geographic Filters
        if (geo.block != "All") {
            list = list.filter { it.blockName == geo.block }
        }

        if (geo.sub != "All") {
            list = list.filter { it.substationId.equals(geo.sub, ignoreCase = true) }
        }

        if (geo.hStart.isNotBlank() || geo.hEnd.isNotBlank()) {
            val startNum = geo.hStart.toIntOrNull() ?: 0
            val endNum = geo.hEnd.toIntOrNull() ?: Int.MAX_VALUE
            list = list.filter {
                val num = extractHouseNum(it.houseNo)
                num in startNum..endNum
            }
        }

        // Apply Appliance Filters
        if (app.ac != null) {
            list = list.filter { it.acCount >= app.ac }
        }

        if (app.led10 != null) {
            list = list.filter { it.ledTango10w >= app.led10 }
        }

        if (app.led20 != null) {
            list = list.filter { it.ledTango20w >= app.led20 }
        }

        if (app.ledDown != null) {
            list = list.filter { it.ledDownlight13w >= app.ledDown }
        }

        // Apply Load Sorting
        when (sorting) {
            SortOrder.LOAD_DESC -> list.sortedByDescending { it.totalLoadKw }
            SortOrder.LOAD_ASC -> list.sortedBy { it.totalLoadKw }
            SortOrder.HOUSE_ASC -> list.sortedBy { it.houseNo }
            SortOrder.NAME_ASC -> list.sortedBy { it.residentName }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun extractHouseNum(houseNo: String): Int {
        return try {
            val cleaned = houseNo.replace(Regex("[^0-9]"), "")
            if (cleaned.isNotEmpty()) cleaned.toInt() else 0
        } catch (e: Exception) {
            0
        }
    }

    // Exposes current item details dynamically
    val selectedHouseRecord: StateFlow<HouseRecord?> = combine(
        repository.allRecords,
        _selectedHouseNo
    ) { records, houseNo ->
        if (houseNo == null) null else records.firstOrNull { it.houseNo == houseNo }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Township level analytics calculations
    val townshipAnalytics: StateFlow<TownshipAnalytics> = repository.allRecords
        .map { records ->
            if (records.isEmpty()) {
                TownshipAnalytics(0.0, 0, 0f, 0.0, 0.0)
            } else {
                val totalLoad = records.sumOf { it.totalLoadKw }
                val totalCount = records.size
                val avgLoad = totalLoad / totalCount

                // Reference load limit representing 3000 kW (3 MW) for the smart township.
                // Substation rating set to 3.5 MW.
                val utilPercent = (totalLoad / 3500.0).coerceIn(0.0, 1.0).toFloat()
                
                // Active calculation for substation transformer capacity (usually slightly higher, simulated at 0.9 pf)
                val activeKva = totalLoad / 0.9

                TownshipAnalytics(
                    totalConnectedLoadKw = totalLoad,
                    totalHousesCount = totalCount,
                    loadGaugePercentage = utilPercent,
                    activeSubstationKva = activeKva,
                    averageLoadPerHouseKw = avgLoad
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TownshipAnalytics(0.0, 0, 0f, 0.0, 0.0))

    // Block-wise breakdown calculation
    val blockLoadSummaries: StateFlow<List<BlockLoadSummary>> = repository.allRecords
        .map { records ->
            if (records.isEmpty()) return@map emptyList<BlockLoadSummary>()

            val totalTownshipLoad = records.sumOf { it.totalLoadKw }
            
            records.groupBy { it.blockName }
                .map { (blockName, houseList) ->
                    val blockTotal = houseList.sumOf { it.totalLoadKw }
                    val blockCount = houseList.size
                    val blockAvg = blockTotal / blockCount
                    
                    // Highlight blocks exceeding peak safety parameters (e.g. average house load > 8 kW or block total > 400 kW)
                    val isHighLoad = blockAvg > 8.0 || blockTotal > 350.0
                    val pct = if (totalTownshipLoad > 0) (blockTotal / totalTownshipLoad).toFloat() else 0f

                    BlockLoadSummary(
                        blockName = blockName,
                        totalLoadKw = blockTotal,
                        averageLoadKw = blockAvg,
                        houseCount = blockCount,
                        isHighLoad = isHighLoad,
                        loadPercentage = pct
                    )
                }
                .sortedByDescending { it.totalLoadKw } // Highlight highest load blocks at the top
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Substation intelligence mapping calculations
    val substationSummaries: StateFlow<List<SubstationSummary>> = repository.allRecords
        .map { records ->
            val substationsConfig = listOf(
                Triple("Substation 1", 1250.0, "6kV / 415V"),
                Triple("Substation 2", 1000.0, "6kV / 415V"),
                Triple("Substation 3", 1600.0, "6kV / 415V")
            )

            substationsConfig.map { (subId, capacityKva, voltageRating) ->
                val subRecords = records.filter { it.substationId.equals(subId, ignoreCase = true) }
                val totalLoad = subRecords.sumOf { it.totalLoadKw }
                val loadingPct = if (capacityKva > 0) (totalLoad / capacityKva) * 100.0 else 0.0
                
                val status = when {
                    loadingPct > 90.0 -> LoadStatus.OVERLOADED
                    loadingPct >= 70.0 -> LoadStatus.MODERATE
                    else -> LoadStatus.SAFE
                }

                // Predict risk based on loading pct and peak trends
                val riskString = when {
                    loadingPct > 90.0 -> "CRITICAL (Immediate Upgrade Required)"
                    loadingPct > 80.0 -> "HIGH (Peak Expansion Strain)"
                    loadingPct >= 70.0 -> "MODERATE (Stable Growth)"
                    loadingPct >= 50.0 -> "LOW (Optimally Utilized)"
                    else -> "NEGLIGIBLE (Excess Overhead)"
                }

                val isRiskHighKind = loadingPct >= 80.0

                SubstationSummary(
                    substationId = subId,
                    capacityKva = capacityKva,
                    voltageRating = voltageRating,
                    totalConnectedLoadKw = totalLoad,
                    houseCount = subRecords.size,
                    blockNames = subRecords.map { it.blockName }.distinct().sorted(),
                    loadingPercentage = loadingPct,
                    status = status,
                    predictedOverloadRisk = riskString,
                    isRiskHighKind = isRiskHighKind
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

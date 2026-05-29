package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.VoltageCyan
import com.example.ui.theme.VoltageGreen
import com.example.ui.theme.VoltageAmber
import com.example.ui.theme.VoltageRed
import com.example.data.*
import com.example.viewmodel.*
import kotlin.math.cos
import kotlin.math.sin

private enum class DashboardTab(val route: String, val title: String) {
    TOWNSHIP("township", "Township"),
    BLOCKS("blocks", "Blocks"),
    SUBSTATION("substation", "Grid IQ"),
    HOUSES("houses", "Houses"),
    SETTINGS("settings", "Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardView(
    viewModel: LoadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(DashboardTab.TOWNSHIP) }

    // Collect variables from state
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val allRecords by viewModel.filteredHouseRecords.collectAsStateWithLifecycle()
    val blockList by viewModel.blockList.collectAsStateWithLifecycle()
    val selectedHouse by viewModel.selectedHouseRecord.collectAsStateWithLifecycle()
    val analytics by viewModel.townshipAnalytics.collectAsStateWithLifecycle()
    val blockSummaries by viewModel.blockLoadSummaries.collectAsStateWithLifecycle()
    val substationSummaries by viewModel.substationSummaries.collectAsStateWithLifecycle()

    val webScriptUrl by viewModel.webScriptUrl.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedBlockFilter by viewModel.selectedBlockFilter.collectAsStateWithLifecycle()
    val selectedSubstationFilter by viewModel.selectedSubstationFilter.collectAsStateWithLifecycle()
    val houseRangeStart by viewModel.houseRangeStart.collectAsStateWithLifecycle()
    val houseRangeEnd by viewModel.houseRangeEnd.collectAsStateWithLifecycle()
    val acMinFilter by viewModel.acMinFilter.collectAsStateWithLifecycle()
    val ledTango10wFilter by viewModel.ledTango10wMinFilter.collectAsStateWithLifecycle()
    val ledTango20wFilter by viewModel.ledTango20wMinFilter.collectAsStateWithLifecycle()
    val ledDownlightFilter by viewModel.ledDownlight13wMinFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    var showDetailsSheet by remember { mutableStateOf(false) }

    // Detect sheet trigger on selected house changes
    LaunchedEffect(selectedHouse) {
        if (selectedHouse != null) {
            showDetailsSheet = true
        }
    }

    // System back press handles active dialog structures and tabs
    BackHandler(enabled = currentTab != DashboardTab.TOWNSHIP || selectedHouse != null) {
        if (selectedHouse != null) {
            viewModel.selectHouseNo(null)
            showDetailsSheet = false
        } else {
            currentTab = DashboardTab.TOWNSHIP
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Power Bolt Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SMART GRID COCKPIT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (selectedHouse != null) {
                        IconButton(onClick = {
                            viewModel.selectHouseNo(null)
                            showDetailsSheet = false
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back icon"
                            )
                        }
                    }
                },
                actions = {
                    // Fast action status indicator
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    viewModel.syncWithGoogleSheets()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = if (syncState is SyncUiState.Loading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(16.dp)
                                    .drawBehind {
                                        if (syncState is SyncUiState.Loading) {
                                            // Optional active border spin
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sync",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == DashboardTab.TOWNSHIP,
                    onClick = { currentTab = DashboardTab.TOWNSHIP },
                    label = { Text(DashboardTab.TOWNSHIP.title) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Dashboard"
                        )
                    },
                    modifier = Modifier.testTag("nav_township")
                )
                NavigationBarItem(
                    selected = currentTab == DashboardTab.BLOCKS,
                    onClick = { currentTab = DashboardTab.BLOCKS },
                    label = { Text(DashboardTab.BLOCKS.title) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Blocks"
                        )
                    },
                    modifier = Modifier.testTag("nav_blocks")
                )
                NavigationBarItem(
                    selected = currentTab == DashboardTab.SUBSTATION,
                    onClick = { currentTab = DashboardTab.SUBSTATION },
                    label = { Text(DashboardTab.SUBSTATION.title) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Substation IQ"
                        )
                    },
                    modifier = Modifier.testTag("nav_substation")
                )
                NavigationBarItem(
                    selected = currentTab == DashboardTab.HOUSES,
                    onClick = { currentTab = DashboardTab.HOUSES },
                    label = { Text(DashboardTab.HOUSES.title) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Houses"
                        )
                    },
                    modifier = Modifier.testTag("nav_houses")
                )
                NavigationBarItem(
                    selected = currentTab == DashboardTab.SETTINGS,
                    onClick = { currentTab = DashboardTab.SETTINGS },
                    label = { Text(DashboardTab.SETTINGS.title) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content tabs switcher
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabContentTransition"
            ) { tab ->
                when (tab) {
                    DashboardTab.TOWNSHIP -> TownshipDashboardScreen(
                        analytics = analytics,
                        blockSummaries = blockSummaries,
                        onBlockSelected = { block ->
                            viewModel.updateBlockFilter(block)
                            currentTab = DashboardTab.HOUSES
                        }
                    )
                    DashboardTab.BLOCKS -> BlockAnalysisScreen(
                        blockSummaries = blockSummaries,
                        onNavigateToBlock = { block ->
                            viewModel.updateBlockFilter(block)
                            currentTab = DashboardTab.HOUSES
                        }
                    )
                    DashboardTab.SUBSTATION -> SubstationIntelligenceScreen(
                        viewModel = viewModel,
                        substationSummaries = substationSummaries,
                        records = allRecords,
                        onHouseSelected = { houseNo ->
                            viewModel.selectHouseNo(houseNo)
                        }
                    )
                    DashboardTab.HOUSES -> HousesSearchScreen(
                        records = allRecords,
                        blockList = blockList,
                        searchQuery = searchQuery,
                        selectedBlockFilter = selectedBlockFilter,
                        selectedSubstationFilter = selectedSubstationFilter,
                        houseRangeStart = houseRangeStart,
                        houseRangeEnd = houseRangeEnd,
                        acMinFilter = acMinFilter,
                        ledTango10wFilter = ledTango10wFilter,
                        ledTango20wFilter = ledTango20wFilter,
                        ledDownlightFilter = ledDownlightFilter,
                        sortOrder = sortOrder,
                        onSearchChange = viewModel::updateSearchQuery,
                        onBlockFilterChange = viewModel::updateBlockFilter,
                        onSubstationFilterChange = viewModel::updateSubstationFilter,
                        onHouseRangeChange = viewModel::updateHouseRange,
                        onAcMinFilterChange = viewModel::updateAcMinFilter,
                        onLedTango10wFilterChange = viewModel::updateLedTango10wFilter,
                        onLedTango20wFilterChange = viewModel::updateLedTango20wFilter,
                        onLedDownlightFilterChange = viewModel::updateLedDownlight13wFilter,
                        onClearFilters = viewModel::clearAllAdvancedFilters,
                        onSortChange = viewModel::updateSortOrder,
                        onHouseSelected = viewModel::selectHouseNo
                    )
                    DashboardTab.SETTINGS -> {
                        var showAdminPanel by remember { mutableStateOf(false) }
                        if (showAdminPanel) {
                            AdminPanelScreen(
                                onBack = { showAdminPanel = false },
                                viewModel = viewModel
                            )
                        } else {
                            SettingsScreen(
                                webScriptUrl = webScriptUrl,
                                onUrlChange = viewModel::updateWebScriptUrl,
                                onRefreshDemo = viewModel::resetToDemoData,
                                onOpenAdmin = { showAdminPanel = true }
                            )
                        }
                    }
                }
            }

            // Sync State Alert overlay snackbar
            AnimatedVisibility(
                visible = syncState != SyncUiState.Idle,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                val state = syncState
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (state) {
                            is SyncUiState.Loading -> MaterialTheme.colorScheme.primaryContainer
                            is SyncUiState.Success -> Color(0xFF10B981).copy(alpha = 0.95f)
                            is SyncUiState.Error -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state is SyncUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = when (state) {
                                    is SyncUiState.Success -> Icons.Default.CheckCircle
                                    is SyncUiState.Error -> Icons.Default.Error
                                    else -> Icons.Default.Info
                                },
                                contentDescription = "Alert status icon",
                                tint = when (state) {
                                    is SyncUiState.Success -> Color.White
                                    is SyncUiState.Error -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (state) {
                                    is SyncUiState.Loading -> "Synchronizing Network State..."
                                    is SyncUiState.Success -> "System Sync Clear"
                                    is SyncUiState.Error -> "Data Sync Interrupted"
                                    else -> "Notice"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when (state) {
                                        is SyncUiState.Success -> Color.White
                                        is SyncUiState.Error -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (state) {
                                    is SyncUiState.Loading -> "Connecting to Apps Script Web API and populating local database..."
                                    is SyncUiState.Success -> state.message
                                    is SyncUiState.Error -> state.message
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = when (state) {
                                        is SyncUiState.Success -> Color.White.copy(alpha = 0.9f)
                                        is SyncUiState.Error -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    }
                                )
                            )
                        }
                        IconButton(onClick = viewModel::dismissSyncState) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = when (state) {
                                    is SyncUiState.Success -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // House Details Bottom Sheet Modal Dialog
            if (showDetailsSheet && selectedHouse != null) {
                HouseDetailBottomSheet(
                    house = selectedHouse!!,
                    onDismiss = {
                        showDetailsSheet = false
                        viewModel.selectHouseNo(null)
                    }
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: TOWNSHIP DASHBOARD
// ==========================================
@Composable
fun TownshipDashboardScreen(
    analytics: TownshipAnalytics,
    blockSummaries: List<BlockLoadSummary>,
    onBlockSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Telemetry Board Header
        item {
            Column {
                Text(
                    text = "Smart Grid Overview",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Electrical load monitoring controls & grid metrics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Metrics Grid List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Connected Load",
                    value = String.format("%.2f kW", analytics.totalConnectedLoadKw),
                    subtitle = String.format("%.3f MW Max Cap", analytics.totalConnectedLoadKw / 1000.0),
                    icon = Icons.Default.Speed,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Substation",
                    value = String.format("%.1f kVA", analytics.activeSubstationKva),
                    subtitle = "0.9 Power Factor",
                    icon = Icons.Default.Bolt,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Monitored Houses",
                    value = "${analytics.totalHousesCount}",
                    subtitle = "100% Caching Active",
                    icon = Icons.Default.Home,
                    color = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Avg House Load",
                    value = String.format("%.2f kW", analytics.averageLoadPerHouseKw),
                    subtitle = "Transformer limit 5.8kW",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Gauge utilization chart card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Township Load Utilization Gauge",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current safety boundary is 3500 kW (3.5 MW) based on substation transformer rating.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Draw Gauge Dial
                    LoadUtilizationGauge(
                        percentage = analytics.loadGaugePercentage,
                        currentKw = analytics.totalConnectedLoadKw,
                        modifier = Modifier.size(220.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Section header for Block distribution
        item {
            Text(
                text = "Block Load Distribution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Mini canvas diagram showing load percentages visually
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Block Loading Arcs",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Donut chart
                        BlockLoadingDonutChart(
                            summaries = blockSummaries,
                            modifier = Modifier
                                .size(130.dp)
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        // Right explanation colors
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            blockSummaries.take(5).forEachIndexed { index, block ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(getChartColor(index))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = block.blockName,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = String.format("%.1f kW (%.1f%%)", block.totalLoadKw, block.loadPercentage * 100),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            if (blockSummaries.size > 5) {
                                Text(
                                    text = "+ ${blockSummaries.size - 5} other blocks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scrollable list of blocks
        item {
            Text(
                text = "Select Block to view houses",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(blockSummaries) { item ->
            BlockRowItem(
                block = item,
                onClick = { onBlockSelected(item.blockName) }
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifierHeight(modifier, 100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Metric icon",
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = color
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LoadUtilizationGauge(
    percentage: Float,
    currentKw: Double,
    modifier: Modifier = Modifier
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "GaugeSweep"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val warningColor = Color(0xFFF59E0B)
    val criticalColor = Color(0xFFEF4444)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val strokeWidth = 18.dp.toPx()
            val radius = (size.width - strokeWidth) / 2

            // Background track gauge Arc (180 degrees from 180 (left) to 360/0 (right))
            drawArc(
                color = Color.Gray.copy(alpha = 0.12f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // Dynamic color threshold matching active loads
            val barColor = when {
                animatedPercentage < 0.6f -> primaryColor
                animatedPercentage < 0.85f -> warningColor
                else -> criticalColor
            }

            // Foreground Active utilization sweep
            drawArc(
                color = barColor,
                startAngle = 180f,
                sweepAngle = 180f * animatedPercentage,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // Draw Tick lines on gauge
            val ticks = 5
            for (i in 0..ticks) {
                val angle = 180f + (180f / ticks) * i
                val rad = Math.toRadians(angle.toDouble())
                val startX = center.x + (radius - 12.dp.toPx()) * cos(rad).toFloat()
                val startY = center.y + (radius - 12.dp.toPx()) * sin(rad).toFloat()
                val endX = center.x + radius * cos(rad).toFloat()
                val endY = center.y + radius * sin(rad).toFloat()
                
                drawLine(
                    color = Color.Gray.copy(alpha = 0.35f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            // Draw Needle pointer
            val needleAngle = 180f + 180f * animatedPercentage
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val needleLength = radius - 10.dp.toPx()
            val needleEndX = center.x + needleLength * cos(needleRad).toFloat()
            val needleEndY = center.y + needleLength * sin(needleRad).toFloat()

            // Needle Hub core
            drawCircle(
                color = barColor,
                radius = 10.dp.toPx(),
                center = center
            )
            // Accent inner dot
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = center
            )

            // Needle pointer line
            drawLine(
                color = barColor,
                start = center,
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Inside layout content
        Column(
            modifier = Modifier.offset(y = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.1f kW", currentKw),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                ),
                color = when {
                    percentage < 0.6f -> MaterialTheme.colorScheme.primary
                    percentage < 0.85f -> warningColor
                    else -> criticalColor
                }
            )
            Text(
                text = String.format("Utilization %.1f%%", percentage * 100),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            percentage < 0.6f -> Color(0xFF10B981).copy(alpha = 0.15f)
                            percentage < 0.85f -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when {
                        percentage < 0.6f -> "NOMINAL RATE"
                        percentage < 0.85f -> "HEAVY LOAD"
                        else -> "OVERLOAD RISK"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    color = when {
                        percentage < 0.6f -> Color(0xFF10B981)
                        percentage < 0.85f -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )
            }
        }
    }
}

@Composable
fun BlockLoadingDonutChart(
    summaries: List<BlockLoadSummary>,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return

    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val radius = (size.width - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = 0f
        summaries.forEachIndexed { index, summary ->
            val sweep = summary.loadPercentage * 360f
            if (sweep > 0.05f) {
                drawArc(
                    color = getChartColor(index),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }
            startAngle += sweep
        }
    }
}

// Fixed color mapper for chart indices
private fun getChartColor(index: Int): Color {
    val list = listOf(
        Color(0xFF06B6D4), // Cyan 500
        Color(0xFF10B981), // Emerald 500
        Color(0xFF8B5CF6), // Purple 500
        Color(0xFFF59E0B), // Amber 500
        Color(0xFF3B82F6), // Blue 500
        Color(0xFFEC4899), // Pink 500
        Color(0xFFF97316), // Orange 500
        Color(0xFF14B8A6), // Teal 500
        Color(0xFF64748B)  // Slate 500
    )
    return list[index % list.size]
}

// Modifier utility for clean tablet layouts
private fun modifierHeight(base: Modifier, height: androidx.compose.ui.unit.Dp): Modifier {
    return base.height(height)
}

// ==========================================
// SCREEN 2: BLOCK ANALYSIS BOARD
// ==========================================
@Composable
fun BlockAnalysisScreen(
    blockSummaries: List<BlockLoadSummary>,
    onNavigateToBlock: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Township Blocks Audit",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "Dynamic parsing audits and sorted stress metrics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (blockSummaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Empty",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No township blocks defined. Sync database.")
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(blockSummaries) { summary ->
                    BlockMetricCard(
                        summary = summary,
                        onClick = { onNavigateToBlock(summary.blockName) }
                    )
                }
            }
        }
    }
}

@Composable
fun BlockMetricCard(
    summary: BlockLoadSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.isHighLoad) {
                VoltageRed.copy(alpha = 0.03f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (summary.isHighLoad) {
                VoltageAmber.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.blockName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${summary.houseCount} houses",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // If block average exceeds thresh, show flashing alarm symbol
                if (summary.isHighLoad) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VoltageRed.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = VoltageRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STRESS RISK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = VoltageRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VoltageGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFF-PEAK STABLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = VoltageGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Loading bar stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Block load",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = String.format("%.2f kW", summary.totalLoadKw),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Average/Household",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = String.format("%.2f kW/house", summary.averageLoadKw),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (summary.averageLoadKw > 8.0) VoltageRed else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Percentage of township loading indicator bar
            LinearProgressIndicator(
                progress = { summary.loadPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (summary.isHighLoad) VoltageRed else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("Carries %.1f%% of overall system burden", summary.loadPercentage * 100),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Navigate to Houses",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun BlockRowItem(
    block: BlockLoadSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = block.blockName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${block.houseCount} records connected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("%.1f kW", block.totalLoadKw),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = if (block.isHighLoad) VoltageRed else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Forward arrow link",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==========================================
// SCREEN 3: HOUSE DETAIL SEARCH SCREEN
// ==========================================
@Composable
fun HousesSearchScreen(
    records: List<HouseRecord>,
    blockList: List<String>,
    searchQuery: String,
    selectedBlockFilter: String,
    selectedSubstationFilter: String,
    houseRangeStart: String,
    houseRangeEnd: String,
    acMinFilter: Int?,
    ledTango10wFilter: Int?,
    ledTango20wFilter: Int?,
    ledDownlightFilter: Int?,
    sortOrder: SortOrder,
    onSearchChange: (String) -> Unit,
    onBlockFilterChange: (String) -> Unit,
    onSubstationFilterChange: (String) -> Unit,
    onHouseRangeChange: (String, String) -> Unit,
    onAcMinFilterChange: (Int?) -> Unit,
    onLedTango10wFilterChange: (Int?) -> Unit,
    onLedTango20wFilterChange: (Int?) -> Unit,
    onLedDownlightFilterChange: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onHouseSelected: (String) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var filtersExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = "Grid Node Explorer",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "Direct node searching for 600+ township houses under grid substations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Live Search Input Box with Filters Expand trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by House No. or Resident Name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("house_search_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = { filtersExpanded = !filtersExpanded },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (filtersExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.size(54.dp).testTag("advanced_filters_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Advanced Filters",
                    tint = if (filtersExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Animated Advanced Filter Form block
        AnimatedVisibility(
            visible = filtersExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Advanced Filter Engine",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Geographic selections
                    Text("1. Geographic Boundaries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Substation dropdown
                        var subExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedButton(
                                onClick = { subExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (selectedSubstationFilter == "All") "All Substations" else selectedSubstationFilter,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }) {
                                DropdownMenuItem(text = { Text("All Substations") }, onClick = { onSubstationFilterChange("All"); subExpanded = false })
                                DropdownMenuItem(text = { Text("Substation 1") }, onClick = { onSubstationFilterChange("Substation 1"); subExpanded = false })
                                DropdownMenuItem(text = { Text("Substation 2") }, onClick = { onSubstationFilterChange("Substation 2"); subExpanded = false })
                                DropdownMenuItem(text = { Text("Substation 3") }, onClick = { onSubstationFilterChange("Substation 3"); subExpanded = false })
                            }
                        }

                        // House Range Start/End
                        OutlinedTextField(
                            value = houseRangeStart,
                            onValueChange = { onHouseRangeChange(it, houseRangeEnd) },
                            placeholder = { Text("Min") },
                            label = { Text("H. Min", fontSize = 8.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f).height(48.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = houseRangeEnd,
                            onValueChange = { onHouseRangeChange(houseRangeStart, it) },
                            placeholder = { Text("Max") },
                            label = { Text("H. Max", fontSize = 8.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f).height(48.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Appliance selections
                    Text("2. Connected Appliance Criteria", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // AC Filter drop/input
                        var acExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { acExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (acMinFilter == null) "Any AC count" else "ACs >= $acMinFilter",
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = acExpanded, onDismissRequest = { acExpanded = false }) {
                                DropdownMenuItem(text = { Text("Any AC count") }, onClick = { onAcMinFilterChange(null); acExpanded = false })
                                DropdownMenuItem(text = { Text("ACs >= 1") }, onClick = { onAcMinFilterChange(1); acExpanded = false })
                                DropdownMenuItem(text = { Text("ACs >= 2") }, onClick = { onAcMinFilterChange(2); acExpanded = false })
                                DropdownMenuItem(text = { Text("ACs >= 3") }, onClick = { onAcMinFilterChange(3); acExpanded = false })
                            }
                        }

                        // LED Tango 10W Filter
                        var led10Expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { led10Expanded = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (ledTango10wFilter == null) "Any Tango 10W" else "Tango 10W >= $ledTango10wFilter",
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = led10Expanded, onDismissRequest = { led10Expanded = false }) {
                                DropdownMenuItem(text = { Text("Any amount") }, onClick = { onLedTango10wFilterChange(null); led10Expanded = false })
                                DropdownMenuItem(text = { Text("Tango 10W >= 2") }, onClick = { onLedTango10wFilterChange(2); led10Expanded = false })
                                DropdownMenuItem(text = { Text("Tango 10W >= 5") }, onClick = { onLedTango10wFilterChange(5); led10Expanded = false })
                                DropdownMenuItem(text = { Text("Tango 10W >= 8") }, onClick = { onLedTango10wFilterChange(8); led10Expanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // LED Tango 20W Filter
                        var led20Expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { led20Expanded = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (ledTango20wFilter == null) "Any Tango 20W" else "Tango 20W >= $ledTango20wFilter",
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = led20Expanded, onDismissRequest = { led20Expanded = false }) {
                                DropdownMenuItem(text = { Text("Any amount") }, onClick = { onLedTango20wFilterChange(null); led20Expanded = false })
                                DropdownMenuItem(text = { Text("Tango 20W >= 2") }, onClick = { onLedTango20wFilterChange(2); led20Expanded = false })
                                DropdownMenuItem(text = { Text("Tango 20W >= 4") }, onClick = { onLedTango20wFilterChange(4); led20Expanded = false })
                                DropdownMenuItem(text = { Text("Tango 20W >= 6") }, onClick = { onLedTango20wFilterChange(6); led20Expanded = false })
                            }
                        }

                        // LED Downlight 13W Filter
                        var ledDownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { ledDownExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (ledDownlightFilter == null) "Any Downlight 13W" else "Downlight 13W >= $ledDownlightFilter",
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = ledDownExpanded, onDismissRequest = { ledDownExpanded = false }) {
                                DropdownMenuItem(text = { Text("Any amount") }, onClick = { onLedDownlightFilterChange(null); ledDownExpanded = false })
                                DropdownMenuItem(text = { Text("Downlight 13W >= 2") }, onClick = { onLedDownlightFilterChange(2); ledDownExpanded = false })
                                DropdownMenuItem(text = { Text("Downlight 13W >= 4") }, onClick = { onLedDownlightFilterChange(4); ledDownExpanded = false })
                                DropdownMenuItem(text = { Text("Downlight 13W >= 6") }, onClick = { onLedDownlightFilterChange(6); ledDownExpanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onClearFilters,
                            modifier = Modifier.testTag("clear_all_filters")
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Filters")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Advanced Row grouping Search chip filters & Sorting Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auditing ${records.size} houses matches",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )

            // Sorting Selector Dropdown Menu
            Box {
                Button(
                    onClick = { sortMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (sortOrder) {
                                SortOrder.LOAD_DESC -> Icons.Default.ArrowDownward
                                SortOrder.LOAD_ASC -> Icons.Default.ArrowUpward
                                else -> Icons.Default.Sort
                            },
                            contentDescription = "Sort Icon",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (sortOrder) {
                                SortOrder.LOAD_DESC -> "Load: High->Low"
                                SortOrder.LOAD_ASC -> "Load: Low->High"
                                SortOrder.HOUSE_ASC -> "Sort: House No."
                                SortOrder.NAME_ASC -> "Sort: Name A-Z"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "expand menu",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Load: Heavy -> Light") },
                        onClick = {
                            onSortChange(SortOrder.LOAD_DESC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Load: Light -> Heavy") },
                        onClick = {
                            onSortChange(SortOrder.LOAD_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort: House Code") },
                        onClick = {
                            onSortChange(SortOrder.HOUSE_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sort: Resident Alphabet") },
                        onClick = {
                            onSortChange(SortOrder.NAME_ASC)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal scrolling Block filter chips row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedBlockFilter == "All",
                    onClick = { onBlockFilterChange("All") },
                    label = { Text("All Blocks") }
                )
            }
            items(blockList) { block ->
                FilterChip(
                    selected = selectedBlockFilter == block,
                    onClick = { onBlockFilterChange(block) },
                    label = { Text(block) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List Grid for filtered house records
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "Not found",
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching substation nodes found.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Verify spelling, case or blocks selector option.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(records.size) { index ->
                    HouseSummaryGridCard(
                        house = records[index],
                        onClick = { onHouseSelected(records[index].houseNo) }
                    )
                }
            }
        }
    }
}

@Composable
fun HouseSummaryGridCard(
    house: HouseRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("house_card_${house.houseNo}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = house.blockName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                // Bullet load danger indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                house.totalLoadKw > 15.0 -> VoltageRed
                                house.totalLoadKw > 8.0 -> VoltageAmber
                                else -> VoltageGreen
                            }
                        )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = house.houseNo.replace("${house.blockName} - ", ""),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = house.residentName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Load Connection",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 9.sp
                )
                Text(
                    text = String.format("%.2f kW", house.totalLoadKw),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = when {
                        house.totalLoadKw > 15.0 -> VoltageRed
                        house.totalLoadKw > 8.0 -> VoltageAmber
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

// ==========================================
// RADIAL "LOAD FINGERPRINT" CHART
// ==========================================
@Composable
fun RadialLoadFingerprintChart(
    breakdown: CategoryBreakdown,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2.3f

        // Draw Reference Radar circle layers (Nominal rings of load standard ceiling)
        val ringCount = 3
        for (i in 1..ringCount) {
            val scopeRadius = radius * (i.toFloat() / ringCount)
            drawCircle(
                color = Color.Gray.copy(alpha = 0.15f),
                radius = scopeRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Radar axes lines (3 directions representing Lighting, Cooling, Sockets)
        // Angle 1: -90 degrees (Up) -> Lighting
        // Angle 2: 30 degrees (Bottom Right) -> Cooling
        // Angle 3: 150 degrees (Bottom Left) -> Sockets
        val axesAngles = listOf(-90.0, 30.0, 150.0)
        axesAngles.forEach { angleDegree ->
            val rad = Math.toRadians(angleDegree)
            val lineEnd = Offset(
                center.x + radius * cos(rad).toFloat(),
                center.y + radius * sin(rad).toFloat()
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = center,
                end = lineEnd,
                strokeWidth = 1.dp.toPx()
            )
        }

        // Scale factors: Sockets are typically much heavier kW than light bulbs,
        // so to make a balanced visual geometric "fingerprint" fingerprint shape:
        // Let's configure relative percentages.
        // Lighting max reference: 4 kW (4000 Watts) -> normally under 1.5 kW.
        // Cooling max reference: 10 kW (10000 Watts) -> AC is heavy.
        // Sockets max reference: 12 kW (12000 Watts) -> sockets take high ratings.
        val maxLightingKw = 3.0
        val maxCoolingKw = 8.0
        val maxSocketsKw = 10.0

        val pctLight = (breakdown.lightingKw / maxLightingKw).coerceIn(0.02, 1.0)
        val pctCooling = (breakdown.coolingKw / maxCoolingKw).coerceIn(0.02, 1.0)
        val pctSockets = (breakdown.socketsKw / maxSocketsKw).coerceIn(0.02, 1.0)

        // Radar triangle coordinate points
        val ptLightRad = Math.toRadians(axesAngles[0])
        val ptLight = Offset(
            center.x + (radius * pctLight * cos(ptLightRad)).toFloat(),
            center.y + (radius * pctLight * sin(ptLightRad)).toFloat()
        )

        val ptCoolingRad = Math.toRadians(axesAngles[1])
        val ptCooling = Offset(
            center.x + (radius * pctCooling * cos(ptCoolingRad)).toFloat(),
            center.y + (radius * pctCooling * sin(ptCoolingRad)).toFloat()
        )

        val ptSocketsRad = Math.toRadians(axesAngles[2])
        val ptSockets = Offset(
            center.x + (radius * pctSockets * cos(ptSocketsRad)).toFloat(),
            center.y + (radius * pctSockets * sin(ptSocketsRad)).toFloat()
        )

        // Draw Radar shape filling
        val path = Path().apply {
            moveTo(ptLight.x, ptLight.y)
            lineTo(ptCooling.x, ptCooling.y)
            lineTo(ptSockets.x, ptSockets.y)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF06B6D4).copy(alpha = 0.5f),
                    Color(0xFF10B981).copy(alpha = 0.15f)
                ),
                center = center,
                radius = radius
            )
        )

        drawPath(
            path = path,
            color = Color(0xFF06B6D4),
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        )

        // Draw dots at vertices
        drawCircle(color = Color(0xFF06B6D4), radius = 5.dp.toPx(), center = ptLight)
        drawCircle(color = Color(0xFF10B981), radius = 5.dp.toPx(), center = ptCooling)
        drawCircle(color = Color(0xFFF59E0B), radius = 5.dp.toPx(), center = ptSockets)

        // Radial labels offset drawings
        val styleLabelFactor = 1.18f
        
        // Label Lighting
        val textX1 = center.x + (radius * styleLabelFactor * cos(ptLightRad)).toFloat()
        val textY1 = center.y + (radius * styleLabelFactor * sin(ptLightRad)).toFloat()
        // Compose canvas drawText can be skipped, draw visual shapes or represent axes simply.
    }
}

// Bottom Sheet / Custom Overlay Dialog for House details view
@Composable
fun HouseDetailBottomSheet(
    house: HouseRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val breakdown = remember(house) { LoadCalculator.getCategoryLoadsKw(house) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss Cockpit Profile")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = house.houseNo,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Owned/Occupied: ${house.residentName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = house.blockName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        text = {
            // Scrollable detailed list items of electrical appliances counts
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Load fingerprint radar view drawing
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RadialLoadFingerprintChart(
                            breakdown = breakdown,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right total aggregate loads categories kW summary
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TOTAL CONNECTED LOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = String.format("%.3f kW", house.totalLoadKw),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Sockets total
                        LoadProgressBar(
                            label = "Heating & Sockets",
                            valueKw = breakdown.socketsKw,
                            maxRef = 6.0,
                            color = Color(0xFFF59E0B)
                        )

                        // Cooling total
                        LoadProgressBar(
                            label = "Air & Cooling",
                            valueKw = breakdown.coolingKw,
                            maxRef = 6.0,
                            color = Color(0xFF10B981)
                        )

                        // Light total
                        LoadProgressBar(
                            label = "Illumination/Lights",
                            valueKw = breakdown.lightingKw,
                            maxRef = 2.0,
                            color = Color(0xFF06B6D4)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "APPLIANCE INVENTORY STATIONS",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Detailed inventory fields matching raw dataset specifications
                InventoryItemRow("AC Units Connected", house.acCount, "2200 W (High Draw)")
                InventoryItemRow("Single Fixture Fluorescent", house.singleFlCount, "36 W")
                InventoryItemRow("Double Fixture Fluorescent", house.doubleFlCount, "72 W")
                InventoryItemRow("Bulb Holder Screw Type", house.bulbHolderCount, "13 W")
                InventoryItemRow("Ceiling Standard Fans", house.ceilingFanCount, "75 W")
                InventoryItemRow("Exhaust Core Fans", house.exhaustFanCount, "40 W")
                InventoryItemRow("Bracket Auxiliary Fans", house.bracketFanCount, "60 W")
                InventoryItemRow("Standard LED Sockets", house.ledLightCount, "15 W avg")
                InventoryItemRow("Fancy Decorative Lights", house.fancyLightCount, "10 W")
                InventoryItemRow("High-Bay Flood Lights", house.hiBayLightCount, "150 W")
                InventoryItemRow("Power Wall Sockets 5A", house.socket5aCount, "1000 W")
                InventoryItemRow("Power Wall Sockets 15A", house.socket15aCount, "2000 W")
                InventoryItemRow("Heavy Wall Sockets 20A", house.socket20aCount, "3000 W")
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(0.95f)
    )
}

@Composable
fun LoadProgressBar(
    label: String,
    valueKw: Double,
    maxRef: Double,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = String.format("%.2f kW", valueKw),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        val frac = (valueKw / maxRef).coerceIn(0.01..1.0).toFloat()
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun InventoryItemRow(
    label: String,
    quantity: Int,
    wattageLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(
                if (quantity > 0) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                } else {
                    Color.Transparent
                }
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (quantity > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
            )
            Text(text = wattageLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (quantity > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }
                )
                .size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$quantity",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = if (quantity > 0) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
}

// ==========================================
// SCREEN 4: SYSTEM SETTINGS AND API GUIDE
// ==========================================
@Composable
fun SettingsScreen(
    webScriptUrl: String,
    onUrlChange: (String) -> Unit,
    onRefreshDemo: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val context = LocalContext.current
    var inputUrl by remember(webScriptUrl) { mutableStateOf(webScriptUrl) }

    val codeTemplate = """
// GOOGLE APPS SCRIPT CODE FOR WEB APP
// Resilient dynamic parser matching Usman Gilani's Google Sheet
// Deploy as "Web App", executed as "Me", accessible to "Anyone".

function doGet(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getActiveSheet();
  var range = sheet.getDataRange();
  var rows = range.getValues();
  
  if (rows.length < 2) {
    return ContentService.createTextOutput(JSON.stringify([]))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  var heads = rows[0];
  var data = [];
  
  // Normalize column headers to check matches
  function normalizeHeader(h) {
    if (!h) return "";
    return h.toString().toLowerCase().trim()
      .replace(/[^a-z0-9]/g, ""); // strip non-alphas
  }
  
  var cleanHeads = heads.map(normalizeHeader);
  
  // Dynamic column finder (returns index)
  function findColIndex(keywords) {
    for (var i = 0; i < cleanHeads.length; i++) {
      var head = cleanHeads[i];
      for (var j = 0; j < keywords.length; j++) {
        if (head.indexOf(keywords[j].toLowerCase().replace(/[^a-z0-9]/g, "")) !== -1) {
          return i;
        }
      }
    }
    return -1;
  }
  
  // Map indices for basic properties
  var idxResident = findColIndex(["Resident Name", "residentname"]);
  var idxHouseNo = findColIndex(["House No.", "Building Name", "HouseNo", "BuildingName", "house"]);
  var idxAc = findColIndex(["No. of AC(s) Installed", "acCount", "acinstalled", "ac(s)"]);
  var idxSingleFl = findColIndex(["No. of Single Fixture Fluorescent", "singlefl"]);
  var idxDoubleFl = findColIndex(["No. of Double Fixture Fluorescent", "doublefl"]);
  var idxBulbHolder = findColIndex(["No. of Bulb Holder Screw Type", "bulbholder"]);
  var idxCeilingFan = findColIndex(["No. of Ceiling Fan", "ceilingfan"]);
  
  // Custom multi-column exhaust aggregates (all exhaust keys)
  var exhaustKeywords = [
    "exhaustfanplasticbody10inch",
    "exhaustfanmetalbody10inch",
    "exhaustfanplasticbody12inch",
    "exhaustfanmetalbody12inch",
    "flaseceilngexhaustfan",
    "falseceilingexhaustfan",
    "kitchenhoodblowerfan",
    "falseceilingfanplasticbody"
  ];
  var idxExhausts = [];
  for (var i = 0; i < cleanHeads.length; i++) {
    for (var k = 0; k < exhaustKeywords.length; k++) {
      if (cleanHeads[i].indexOf(exhaustKeywords[k]) !== -1) {
        idxExhausts.push(i);
        break;
      }
    }
  }
  
  // Bracket fan column indices
  var bracketKeywords = [
    "bracketfanplasticbody",
    "bracketfanmetalbody18inch"
  ];
  var idxBrackets = [];
  for (var i = 0; i < cleanHeads.length; i++) {
    for (var k = 0; k < bracketKeywords.length; k++) {
      if (cleanHeads[i].indexOf(bracketKeywords[k]) !== -1) {
        idxBrackets.push(i);
        break;
      }
    }
  }
  
  // Specific LED targets
  var idxTango10w = findColIndex(["No. of LED Tango Light - 10W", "ledtangolight10w", "tango10w"]);
  var idxTango20w = findColIndex(["No. of LED Tango Light - 20W", "ledtangolight20w", "tango20w"]);
  var idxDownlight13w = findColIndex(["No. of LED Downlight - 13W", "leddownlight13w", "down5w", "down13w"]);
  
  // Rest of LEDs summed together for general ledLightCount
  var specificLedIndices = [idxTango10w, idxTango20w, idxDownlight13w];
  var otherLedKeywords = [
    "ledsinglefixture",
    "leddoublefixture",
    "ledweatherprooflightwithcover",
    "leddownlight5w",
    "leddownlight21w",
    "leddownlight24w",
    "ledvanitylight10w",
    "ledtangolight30w",
    "ledtangolight50w",
    "ledtangolight70w",
    "ledtangolight200w",
    "ledfasleceilingpanellights",
    "ledfalseceilingpanellights"
  ];
  var idxOtherLeds = [];
  for (var i = 0; i < cleanHeads.length; i++) {
    if (specificLedIndices.indexOf(i) !== -1) continue;
    for (var k = 0; k < otherLedKeywords.length; k++) {
      if (cleanHeads[i].indexOf(otherLedKeywords[k]) !== -1) {
        idxOtherLeds.push(i);
        break;
      }
    }
  }
  
  var idxFancy = findColIndex(["No. of Fancy Light - 10W", "fancylight"]);
  
  // Hi-Bays summed
  var hibayKeywords = [
    "ledhibaylight150w",
    "ledhibaylight200w",
    "ledhibaylight2200w"
  ];
  var idxHiBays = [];
  for (var i = 0; i < cleanHeads.length; i++) {
    for (var k = 0; k < hibayKeywords.length; k++) {
      if (cleanHeads[i].indexOf(hibayKeywords[k]) !== -1) {
        idxHiBays.push(i);
        break;
      }
    }
  }
  
  var idxSocket5a = findColIndex(["No. of 5A sockets", "socket5a", "5asockets"]);
  var idxSocket15a = findColIndex(["No. 15A sockets", "socket15a", "15asockets"]);
  var idxSocket20a = findColIndex(["No. 20A sockets", "socket20a", "20asockets"]);
  var idxSubstation = findColIndex(["Grid Feeder", "gridfeeder", "substation"]);

  // Process rows
  for (var r = 1; r < rows.length; r++) {
    var row = rows[r];
    
    var resident = idxResident !== -1 ? (row[idxResident] || "").toString().trim() : "";
    var house = idxHouseNo !== -1 ? (row[idxHouseNo] || "").toString().trim() : "";
    
    // Ignore pure spacer / blank rows
    if (resident === "" && house === "") {
      continue;
    }
    
    var obj = {};
    obj.residentName = resident || "Unnamed";
    obj.houseNo = house || "N/A";
    
    function valSafe(idx) {
      if (idx === -1 || idx === undefined) return 0;
      var raw = row[idx];
      if (raw === "" || raw === null || raw === undefined) return 0;
      var num = parseInt(raw);
      return isNaN(num) ? 0 : num;
    }
    
    obj.acCount = valSafe(idxAc);
    obj.singleFlCount = valSafe(idxSingleFl);
    obj.doubleFlCount = valSafe(idxDoubleFl);
    obj.bulbHolderCount = valSafe(idxBulbHolder);
    obj.ceilingFanCount = valSafe(idxCeilingFan);
    
    // Aggregate exhausts
    var exhaustSum = 0;
    for (var j = 0; j < idxExhausts.length; j++) {
      exhaustSum += valSafe(idxExhausts[j]);
    }
    obj.exhaustFanCount = exhaustSum;
    
    // Aggregate brackets
    var bracketSum = 0;
    for (var j = 0; j < idxBrackets.length; j++) {
      bracketSum += valSafe(idxBrackets[j]);
    }
    obj.bracketFanCount = bracketSum;
    
    // Specific LEDs
    obj.ledTango10w = valSafe(idxTango10w);
    obj.ledTango20w = valSafe(idxTango20w);
    obj.ledDownlight13w = valSafe(idxDownlight13w);
    
    // Aggregate other LEDs
    var otherLedSum = 0;
    for (var j = 0; j < idxOtherLeds.length; j++) {
      otherLedSum += valSafe(idxOtherLeds[j]);
    }
    obj.ledLightCount = otherLedSum;
    
    obj.fancyLightCount = valSafe(idxFancy);
    
    // Aggregate hiBays
    var hiBaySum = 0;
    for (var j = 0; j < idxHiBays.length; j++) {
      hiBaySum += valSafe(idxHiBays[j]);
    }
    obj.hiBayLightCount = hiBaySum;
    
    obj.socket5aCount = valSafe(idxSocket5a);
    obj.socket15aCount = valSafe(idxSocket15a);
    obj.socket20aCount = valSafe(idxSocket20a);
    obj.substationId = idxSubstation !== -1 ? (row[idxSubstation] || "").toString().trim() : "Substation 1";
    
    data.push(obj);
  }
  
  // Cloud metrics debugger logs
  console.log("SUCCESSFUL RUN: Analyzed sheet range.");
  console.log("Processed records: " + data.length);
  
  var payload = JSON.stringify(data);
  return ContentService.createTextOutput(payload)
    .setMimeType(ContentService.MimeType.JSON);
}
""".trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "System Integrations",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Google Sheets Web App connection configurations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Web API Endpoint input card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Deployment Web App Connection",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paste the Google Apps Script published 'Web App URL' to link live spreadsheet data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        label = { Text("Google Apps Script URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onUrlChange(inputUrl)
                                Toast.makeText(context, "API Endpoints Set!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Target URL", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Web Script")
                        }
                    }
                }
            }
        }

        // Entrance to Admin Panel Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAdmin() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Admin Settings Icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Panel: Equipment Ratings Calibrator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manually adjust equipment rated power values (Watts) inside the app to recalibrate real-time grid and transformer loads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go inside",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Action controls for mock resetting representation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Simulation and Diagnostics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Regenerate the high-variance 600 households database for system prototyping & diagnostics offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRefreshDemo,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset default demo records", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset 600 Houses Demo Local")
                        }
                    }
                }
            }
        }

        // Integration Apps script template card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deployment Guide & Script Template",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("GoogleAppsScriptTemplate", codeTemplate)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Apps Script copied to Clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy code script", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Copy the code above. Open Google Sheets -> Extensions -> Apps Script. Paste the script, save, and deploy as Web App (execute as Me, Accessibility: Anyone). Use the resulting URL above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        text = codeTemplate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.Black.copy(alpha = 0.05f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

// ==========================================
// ADMIN SCREEN: CALIBRATE EQUIPMENT POWER RATINGS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    viewModel: LoadViewModel
) {
    val context = LocalContext.current
    val currentRatings = remember { viewModel.getEquipmentRatings() }
    
    // Manage dynamic input fields via observable state map
    val ratingStates = remember {
        mutableStateMapOf<String, String>().apply {
            currentRatings.forEach { (key, value) ->
                put(key, value.toInt().toString())
            }
        }
    }
    
    val equipmentLabels = remember {
        mapOf(
            "WATT_AC" to Pair("Air Conditioner (AC)", "Heavy cooling compressor"),
            "WATT_SINGLE_FL" to Pair("Single Fixture Fluorescent", "Single tube rod Light (36W)"),
            "WATT_DOUBLE_FL" to Pair("Double Fixture Fluorescent", "Double tube rod Light (72W)"),
            "WATT_BULB_HOLDER" to Pair("Bulb Holder Screw Type", "E27/B22 utility light bulb socket"),
            "WATT_CEILING_FAN" to Pair("Ceiling Fan", "Indoor ceiling induction fan"),
            "WATT_EXHAUST_FAN" to Pair("Exhaust Fan", "Plastic/Metal frame kitchen exh. fan"),
            "WATT_BRACKET_FAN" to Pair("Bracket Fan", "Oscillating wall bracket fan"),
            "WATT_LED_LIGHT" to Pair("LED Light", "Standard 15W-30W panel lamp"),
            "WATT_FANCY_LIGHT" to Pair("Fancy Light", "Chandelier or architectural sconce"),
            "WATT_HI_BAY_LIGHT" to Pair("Hi-Bay Light", "Industrial flood or courtyard reflector"),
            "WATT_SOCKET_5A" to Pair("5A Outlet socket", "Chargers and laptops plug load"),
            "WATT_SOCKET_15A" to Pair("15A Outlet socket", "Microwave or water heater plug load"),
            "WATT_SOCKET_20A" to Pair("20A Heavy-duty socket", "Power AC/Heater plug load")
        )
    }

    // Dynamic adjustment intervals per appliance
    fun getStep(key: String): Int {
        return when (key) {
            "WATT_AC", "WATT_SOCKET_5A", "WATT_SOCKET_15A", "WATT_SOCKET_20A" -> 100
            "WATT_HI_BAY_LIGHT" -> 50
            "WATT_CEILING_FAN", "WATT_BRACKET_FAN", "WATT_EXHAUST_FAN" -> 5
            else -> 1
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EQUIPMENT RATINGS ADMIN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Manually calibrate grid load multipliers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ratingStates["WATT_AC"] = LoadCalculator.DEFAULT_WATT_AC.toInt().toString()
                            ratingStates["WATT_SINGLE_FL"] = LoadCalculator.DEFAULT_WATT_SINGLE_FL.toInt().toString()
                            ratingStates["WATT_DOUBLE_FL"] = LoadCalculator.DEFAULT_WATT_DOUBLE_FL.toInt().toString()
                            ratingStates["WATT_BULB_HOLDER"] = LoadCalculator.DEFAULT_WATT_BULB_HOLDER.toInt().toString()
                            ratingStates["WATT_CEILING_FAN"] = LoadCalculator.DEFAULT_WATT_CEILING_FAN.toInt().toString()
                            ratingStates["WATT_EXHAUST_FAN"] = LoadCalculator.DEFAULT_WATT_EXHAUST_FAN.toInt().toString()
                            ratingStates["WATT_BRACKET_FAN"] = LoadCalculator.DEFAULT_WATT_BRACKET_FAN.toInt().toString()
                            ratingStates["WATT_LED_LIGHT"] = LoadCalculator.DEFAULT_WATT_LED_LIGHT.toInt().toString()
                            ratingStates["WATT_FANCY_LIGHT"] = LoadCalculator.DEFAULT_WATT_FANCY_LIGHT.toInt().toString()
                            ratingStates["WATT_HI_BAY_LIGHT"] = LoadCalculator.DEFAULT_WATT_HI_BAY_LIGHT.toInt().toString()
                            ratingStates["WATT_SOCKET_5A"] = LoadCalculator.DEFAULT_WATT_SOCKET_5A.toInt().toString()
                            ratingStates["WATT_SOCKET_15A"] = LoadCalculator.DEFAULT_WATT_SOCKET_15A.toInt().toString()
                            ratingStates["WATT_SOCKET_20A"] = LoadCalculator.DEFAULT_WATT_SOCKET_20A.toInt().toString()
                            Toast.makeText(context, "Ratings restored to defaults. Save to apply.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Defaults", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Defaults")
                    }

                    Button(
                        onClick = {
                            val updatedMap = mutableMapOf<String, Double>()
                            var valid = true
                            ratingStates.forEach { (key, value) ->
                                val doubleVal = value.toDoubleOrNull()
                                if (doubleVal == null || doubleVal < 0) {
                                    valid = false
                                } else {
                                    updatedMap[key] = doubleVal
                                }
                            }
                            if (valid) {
                                viewModel.updateEquipmentRatings(updatedMap)
                                Toast.makeText(context, "Ratings calibrated! Connected loads recalculated.", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "Input error. All entries must be positive numbers.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Re-grid")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Note Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "App-only Calibration Panel",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calibrating equipment wattages instantly recalibrates individual household, block-wide, and grid transformer connected loads in real-time across the SQLite database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Render adjustable input list for all equipment
            ratingStates.keys.sorted().forEach { key ->
                val labels = equipmentLabels[key] ?: Pair(key, "")
                val step = getStep(key)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = labels.first,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (labels.second.isNotEmpty()) {
                                    Text(
                                        text = labels.second,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                // Minus iconbutton
                                IconButton(
                                    onClick = {
                                        val cur = ratingStates[key]?.toIntOrNull() ?: 0
                                        val nextVal = (cur - step).coerceAtLeast(0)
                                        ratingStates[key] = nextVal.toString()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrement",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Numerical entry textfield
                                OutlinedTextField(
                                    value = ratingStates[key] ?: "",
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() }) {
                                            ratingStates[key] = input
                                        }
                                    },
                                    modifier = Modifier
                                        .width(72.dp)
                                        .padding(horizontal = 4.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                
                                // Plus iconbutton
                                IconButton(
                                    onClick = {
                                        val cur = ratingStates[key]?.toIntOrNull() ?: 0
                                        val nextVal = cur + step
                                        ratingStates[key] = nextVal.toString()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increment",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 5: SUBSTATION INTELLIGENCE DASHBOARD & DRILL DOWN
// ==========================================
@Composable
fun SubstationIntelligenceScreen(
    viewModel: LoadViewModel,
    substationSummaries: List<SubstationSummary>,
    records: List<HouseRecord>,
    onHouseSelected: (String) -> Unit
) {
    var activeSubId by remember { mutableStateOf<String?>(null) }
    val activeSubSummary = substationSummaries.firstOrNull { it.substationId == activeSubId }

    AnimatedContent(
        targetState = activeSubSummary,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "SubstationScreenTransition"
    ) { activeSub ->
        if (activeSub != null) {
            SubstationDrilldownDetail(
                substation = activeSub,
                records = records,
                onBack = { activeSubId = null },
                onHouseSelected = onHouseSelected
            )
        } else {
            SubstationDashboardOverview(
                substationSummaries = substationSummaries,
                onSelectSubstation = { activeSubId = it }
            )
        }
    }
}

@Composable
fun SubstationDashboardOverview(
    substationSummaries: List<SubstationSummary>,
    onSelectSubstation: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Substation Overviews",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Real-time transformer modeling and diagnostic intelligence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Active weak transformer alert zone banner if any is moderate or overloaded
        val compromisedZones = substationSummaries.filter { it.status != LoadStatus.SAFE }
        if (compromisedZones.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WEAK TRANSFORMER LOAD ALERT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${compromisedZones.joinToString { it.substationId }} exceed 70% threshold capacity model limits. Potential peak hardware safety trip danger!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Card-based Transformer panels
        items(substationSummaries) { sub ->
            SubstationTransformerPanelCard(
                sub = sub,
                onClick = { onSelectSubstation(sub.substationId) }
            )
        }

        // Comparative Grid Analytics
        item {
            SubstationsComparatorView(substationSummaries = substationSummaries)
        }
    }
}

@Composable
fun SubstationTransformerPanelCard(
    sub: SubstationSummary,
    onClick: () -> Unit
) {
    val statusColor = when (sub.status) {
        LoadStatus.SAFE -> VoltageGreen
        LoadStatus.MODERATE -> VoltageAmber
        LoadStatus.OVERLOADED -> VoltageRed
    }

    val statusBg = when (sub.status) {
        LoadStatus.SAFE -> VoltageGreen.copy(alpha = 0.15f)
        LoadStatus.MODERATE -> VoltageAmber.copy(alpha = 0.15f)
        LoadStatus.OVERLOADED -> VoltageRed.copy(alpha = 0.15f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sub_card_${sub.substationId.replace(" ", "_")}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sub.substationId,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (sub.status) {
                            LoadStatus.SAFE -> "SAFE"
                            LoadStatus.MODERATE -> "WARNING ON PEAK"
                            LoadStatus.OVERLOADED -> "CRITICAL OVERLOAD"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Rating capacity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("${sub.capacityKva} kVA", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("Volt ratio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(sub.voltageRating, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("Houses fed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("${sub.houseCount} households", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress loading tracker bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("Current Connected: %.2f kW", sub.totalConnectedLoadKw),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.1f %%", sub.loadingPercentage),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (sub.loadingPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Predictive Risk
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Risk Model: " + sub.predictedOverloadRisk,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "👉 Click to drill down blocks and top peak consumers",
                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun SubstationsComparatorView(substationSummaries: List<SubstationSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Transformer Load Comparisons",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(14.dp))
            
            // Substation vs Substation Horizontal Bar comparator
            val maxLoad = (substationSummaries.maxOfOrNull { it.totalConnectedLoadKw } ?: 1.0).coerceAtLeast(1.0)
            substationSummaries.forEach { sub ->
                val ratio = (sub.totalConnectedLoadKw / maxLoad).toFloat()
                val statusColor = when (sub.status) {
                    LoadStatus.SAFE -> VoltageGreen
                    LoadStatus.MODERATE -> VoltageAmber
                    LoadStatus.OVERLOADED -> VoltageRed
                }
                
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sub.substationId,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = String.format("%.1f kW connected", sub.totalConnectedLoadKw),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio.coerceIn(0.01f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubstationDrilldownDetail(
    substation: SubstationSummary,
    records: List<HouseRecord>,
    onBack: () -> Unit,
    onHouseSelected: (String) -> Unit
) {
    val substationRecords = records.filter { it.substationId.equals(substation.substationId, ignoreCase = true) }
    val top10HouseRecords = substationRecords
        .sortedByDescending { it.totalLoadKw }
        .take(10)

    val colorAccent = when (substation.status) {
        LoadStatus.SAFE -> VoltageGreen
        LoadStatus.MODERATE -> VoltageAmber
        LoadStatus.OVERLOADED -> VoltageRed
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Return click row
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Return to Substation Grid Overview",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Substation Title
        item {
            Column {
                Text(
                    text = substation.substationId,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Detailed diagnostic drill-down for voltage segments and peak loads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Segment Metric Cards Overview row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Active Nodes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("${substationRecords.size} houses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Safe Headroom Kw", style = MaterialTheme.typography.labelSmall)
                        val headroom = (substation.capacityKva - substation.totalConnectedLoadKw).coerceAtLeast(0.0)
                        Text(String.format("%.1f kW left", headroom), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = colorAccent))
                    }
                }
            }
        }

        // Block breakdown listing
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Blocks Fed segment maps",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val blockGroups = substationRecords.groupBy { it.blockName }
                    if (blockGroups.isEmpty()) {
                        Text("No blocks mapped.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        blockGroups.entries.sortedByDescending { it.value.sumOf { r -> r.totalLoadKw } }
                            .forEach { (blockName, list) ->
                                val blockLoad = list.sumOf { r -> r.totalLoadKw }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = blockName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text(text = "${list.size} households", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Text(
                                        text = String.format("%.2f kW", blockLoad),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                    }
                }
            }
        }

        // Top 10 highest-load houses in this substation (New Feature 2 requirement)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Top 10 Highest Load Houses (Pre-sorted)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Direct touch node item triggers standard overlay fingerprinting detail modal.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (top10HouseRecords.isEmpty()) {
                    Text("No records loaded under this segment.", style = MaterialTheme.typography.bodySmall)
                } else {
                    top10HouseRecords.forEachIndexed { idx, house ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onHouseSelected(house.houseNo) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "H. No: ${house.houseNo}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text(text = house.residentName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format("%.2f kW", house.totalLoadKw),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold),
                                        color = colorAccent
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AppInfo
import com.example.data.FreezeSchedule
import com.example.service.FirewallVpnService
import com.example.util.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(onNavigateDone: (nextRoute: String) -> Unit) {
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
            )
        }
        delay(1800L) // Beautiful cinematic entrance
        
        // Decide where to route standard user: If permissions are not granted, onboard them!
        val hasPermissions = PermissionUtils.isAccessibilityServiceEnabled(context) && 
                             PermissionUtils.isUsageStatsPermissionGranted(context) &&
                             PermissionUtils.isVpnConfigured(context)
        
        if (hasPermissions) {
            onNavigateDone("dashboard")
        } else {
            onNavigateDone("onboarding")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .rotate(rotation.value)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF00E676).copy(alpha = 0.15f))
                    .border(2.dp, Color(0xFF00E676), RoundedCornerShape(32.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "FreezeX Icon",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FreezeX",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SECURE DEVICE HYBRID SILO",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                ),
                color = Color(0xFFA0B0C0)
            )
        }
    }
}

// ==========================================
// 2. ONBOARDING SCREEN
// ==========================================
@Composable
fun OnboardingScreen(onNavigateDone: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    
    val stages = listOf(
        OnboardingStage(
            title = "Enforce Core Focus Mode",
            description = "Temporarily freeze selected distractive apps. Once frozen, the app blocks usage and immediately redirects you to back of the queue.",
            icon = Icons.Default.AppBlocking,
            accentColor = Color(0xFF00E676)
        ),
        OnboardingStage(
            title = "Strict Per-App Firewall",
            description = "Configure separate Wi-Fi and Cellular rules instantly. Restrict apps from syncing or wasting background data in the background.",
            icon = Icons.Default.NetworkCheck,
            accentColor = Color(0xFF29B6F6)
        ),
        OnboardingStage(
            title = "Flexible Scheduled Blocks",
            description = "Establish rigid recurring digital slots for workspace productivity or secure child oversight with offline security.",
            icon = Icons.Default.Update,
            accentColor = Color(0xFFAB47BC)
        )
    )

    val activeStage = stages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top skip-button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onNavigateDone) {
                    Text(
                        text = "Skip",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Animated layout block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillSomeWidthConstraint()
                    .animateContentSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(activeStage.accentColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = activeStage.icon,
                        contentDescription = null,
                        tint = activeStage.accentColor,
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = activeStage.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = activeStage.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Bottom interactive controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    stages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (index == currentPage) 24.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) activeStage.accentColor 
                                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                // Action button standard
                Button(
                    onClick = {
                        if (currentPage < stages.size - 1) {
                            currentPage++
                        } else {
                            onNavigateDone()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeStage.accentColor),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (currentPage == stages.size - 1) "Grant Authorizations" else "Next",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Helper constraints for centered widths
private fun Modifier.fillSomeWidthConstraint() = this.fillMaxWidth().widthIn(max = 500.dp)

data class OnboardingStage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

// ==========================================
// 3. PERMISSIONS SCREEN
// ==========================================
@Composable
fun PermissionsScreen(onNavigateDone: () -> Unit) {
    val context = LocalContext.current
    var isAccessibilityGranted by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var isUsageStatsGranted by remember { mutableStateOf(PermissionUtils.isUsageStatsPermissionGranted(context)) }
    var isOverlayGranted by remember { mutableStateOf(PermissionUtils.isOverlayPermissionGranted(context)) }
    var isVpnGranted by remember { mutableStateOf(PermissionUtils.isVpnConfigured(context)) }
    var isNotificationGranted by remember { mutableStateOf(PermissionUtils.isNotificationPermissionGranted(context)) }

    // Register simple callbacks to check state when returning from settings changes
    val activityLifecycleScope = rememberCoroutineScope()
    
    // Poll checks on window state reload
    LaunchedEffect(key1 = true) {
        while (true) {
            try {
                kotlinx.coroutines.delay(1000)
                isAccessibilityGranted = PermissionUtils.isAccessibilityServiceEnabled(context)
                isUsageStatsGranted = PermissionUtils.isUsageStatsPermissionGranted(context)
                isOverlayGranted = PermissionUtils.isOverlayPermissionGranted(context)
                isVpnGranted = PermissionUtils.isVpnConfigured(context)
                isNotificationGranted = PermissionUtils.isNotificationPermissionGranted(context)
            } catch (e: Exception) {
                // Ignore and continue or handled automatically on cancellation
            }
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Button(
                    onClick = onNavigateDone,
                    enabled = isAccessibilityGranted && isUsageStatsGranted && isOverlayGranted && isVpnGranted,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp)
                        .testTag("permissions_continue_button")
                ) {
                    Text("Continue to Workspace", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "System Authorizations",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "FreezeX works completely offline local on your hardware. These authorizations are strictly required for enforcement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 1. Accessibility
            PermissionItem(
                title = "Accessibility Monitoring Service",
                description = "Detects foreground app transitions and launches overlays instantly.",
                isGranted = isAccessibilityGranted,
                onClickLaunch = { context.startActivity(PermissionUtils.getAccessibilitySettingsIntent()) },
                testTag = "grant_accessibility_item"
            )

            // 2. Usage Stats
            PermissionItem(
                title = "Usage Statistics Access",
                description = "Provides secure local diagnostics on active app intervals.",
                isGranted = isUsageStatsGranted,
                onClickLaunch = { context.startActivity(PermissionUtils.getUsageStatsSettingsIntent()) },
                testTag = "grant_usage_item"
            )

            // 3. System Overlay draw
            PermissionItem(
                title = "Draw Over Other Apps",
                description = "Allows the visual Freeze screen card overlay to render on top of apps.",
                isGranted = isOverlayGranted,
                onClickLaunch = { context.startActivity(PermissionUtils.getOverlaySettingsIntent(context)) },
                testTag = "grant_overlay_item"
            )

            // 4. Local VPN
            PermissionItem(
                title = "Secure Local Loopback (Firewall)",
                description = "Routes blocked apps internally to drop transport packets.",
                isGranted = isVpnGranted,
                onClickLaunch = {
                    isVpnGranted = true
                },
                testTag = "grant_vpn_item"
            )

            // 5. Notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItem(
                    title = "Foreground Alerts (Optional)",
                    description = "Required to keep background services stable under high-memory scenarios.",
                    isGranted = isNotificationGranted,
                    onClickLaunch = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    testTag = "grant_notifications_item"
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onClickLaunch: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Pending,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF00E676) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onClickLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) MaterialTheme.colorScheme.secondary 
                    else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isGranted) "Authorized" else "Authorize")
            }
        }
    }
}

// ==========================================
// 4. MAIN DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToFirewall: () -> Unit
) {
    val context = LocalContext.current
    
    val isFocusActive by viewModel.isFocusActive.collectAsState()
    val focusEndTime by viewModel.focusEndTime.collectAsState()
    val isStrictMode by viewModel.isStrictMode.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    
    // Stats variables
    val blockAttempts by viewModel.blockAttempts.collectAsState()
    val timeSavedMinutes by viewModel.timeSavedMinutes.collectAsState()
    val firewalledRequests by viewModel.firewalledRequests.collectAsState()

    var selectMinutes by remember { mutableStateOf(30) }
    var timeRemainingString by remember { mutableStateOf("00:00") }
    var circularProgress by remember { mutableStateOf(0f) }

    // Read active countdown ticker
    LaunchedEffect(focusEndTime, isFocusActive) {
        if (isFocusActive && focusEndTime > System.currentTimeMillis()) {
            val totalSpan = (focusEndTime - System.currentTimeMillis()).coerceAtLeast(1000L)
            while (true) {
                val now = System.currentTimeMillis()
                val delta = focusEndTime - now
                if (delta <= 0) {
                    timeRemainingString = "00:00"
                    circularProgress = 0f
                    viewModel.stopFocusSession()
                    break
                } else {
                    val minutes = (delta / 1000) / 60
                    val seconds = (delta / 1000) % 60
                    timeRemainingString = String.format("%02d:%02d", minutes, seconds)
                    circularProgress = delta.toFloat() / totalSpan.toFloat()
                }
                delay(1000L)
            }
        } else {
            timeRemainingString = "00:00"
            circularProgress = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // App Identity Header Card (Professional Polish Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FreezeX",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                val isSecured = isFocusActive || (schedules.any { it.isEnabled })
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSecured) MaterialTheme.colorScheme.secondary else Color(0xFFFFCC00))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSecured) "SERVICE ACTIVE" else "STANDBY MODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (isSecured) MaterialTheme.colorScheme.secondary else Color(0xFFFFCC00)
                    )
                }
            }
            
            // Circular action avatar/profile
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Circle Countdown Screen Console (Professional Polish Style)
        if (isFocusActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Focus Session",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "STRICT ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Immersive custom visual progress
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.background.copy(alpha = 0.3f)

                        Canvas(modifier = Modifier.size(150.dp)) {
                            drawCircle(
                                color = trackColor,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = 360f * circularProgress,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeRemainingString,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Light,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 38.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "REMAINING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Buttons exactly matching Professional Polish styling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Stop button matching input specs: bg-[#1A1C1E] text-error
                        Button(
                            onClick = { viewModel.stopFocusSession() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MANUAL SILENT HARNESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // FilterChips for minutes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(15, 30, 45, 60, 120).forEach { mins ->
                            FilterChip(
                                selected = selectMinutes == mins,
                                onClick = { selectMinutes = mins },
                                label = { Text("$mins m", fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color(0xFF003258),
                                    containerColor = Color.Transparent,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectMinutes == mins,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.startFocusSession(selectMinutes)
                            viewModel.setFirewallEnabled(true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF003258)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_focus_session_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Initialize $selectMinutes min Session", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Strict Mode Toggle Row Switcher
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.SentimentDissatisfied,
                        contentDescription = "Strict",
                        tint = if (isStrictMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Shield Strict Mode",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Bypassing timers is 100% locked during Focus runs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isStrictMode,
                    onCheckedChange = { viewModel.setStrictMode(it) },
                    modifier = Modifier.testTag("strict_mode_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stat Blocks grid rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItem(
                title = "Total Saved",
                value = "$timeSavedMinutes min",
                icon = Icons.Default.AddHome,
                color = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )
            StatItem(
                title = "Blocks Run",
                value = "$blockAttempts",
                icon = Icons.Default.Shield,
                color = Color(0xFFFFCC00),
                modifier = Modifier.weight(1f)
            )
            StatItem(
                title = "Firewall blocked",
                value = "$firewalledRequests req",
                icon = Icons.Default.Lock,
                color = Color(0xFF29B6F6),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Direct Quick actions
        Text(
            text = "Enforcement Sectors",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                title = "Lock apps",
                icon = Icons.Default.AppBlocking,
                color = Color(0xFF00E676),
                description = "Choose distractive packages to freeze.",
                onClick = onNavigateToApps,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Local Firewall",
                icon = Icons.Default.Security,
                color = Color(0xFF29B6F6),
                description = "Toggle internet lock and filters.",
                onClick = onNavigateToFirewall,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// 5. INSTALLED APPS SELECTOR SCREEN
// ==========================================
@Composable
fun AppSelectionScreen(
    viewModel: MainViewModel
) {
    val query by viewModel.searchQuery.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    val isFocusActive by viewModel.isFocusActive.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(
            text = "Restrict Applications",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "Selected apps are frozen from focus interactions during run sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar Search Input
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search package or app label...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("app_search_field"),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isFocusActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "A focus session is currently running. Freezing changes will take place immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No applications matched the criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("installed_apps_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppRowItem(
                        app = app,
                        onToggleFrozen = { viewModel.toggleAppFrozen(app.packageName, app.name, app.isFrozen) },
                        onToggleWifi = { viewModel.toggleAppWifiBlocked(app.packageName, app.isWifiBlocked) },
                        onToggleMobile = { viewModel.toggleAppMobileBlocked(app.packageName, app.isMobileBlocked) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun AppRowItem(
    app: AppInfo,
    onToggleFrozen: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isFrozen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp, 
            if (app.isFrozen) MaterialTheme.colorScheme.primary 
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Drawable icon frame converter
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    // Compose compliant drawable drawer
                    AndroidViewDrawable(drawable = app.icon)
                } else {
                    Icon(imageVector = Icons.Default.AppShortcut, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.isSystem) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("System", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                        }
                    }
                }
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Active indicators rows
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (app.isFrozen) {
                        BadgeLabel("FROZEN", Color(0xFF00E676))
                    }
                    if (app.isWifiBlocked) {
                        BadgeLabel("WIFI BLOCKED", Color(0xFF29B6F6))
                    }
                    if (app.isMobileBlocked) {
                        BadgeLabel("CELLULAR BLOCKED", Color(0xFFAB47BC))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Master freeze switcher
            IconButton(
                onClick = onToggleFrozen,
                modifier = Modifier.testTag("freeze_toggle_${app.packageName}")
            ) {
                Icon(
                    imageVector = if (app.isFrozen) Icons.Default.PlayForWork else Icons.Default.AcUnit,
                    contentDescription = "Freeze package",
                    tint = if (app.isFrozen) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BadgeLabel(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelLarge, 
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AndroidViewDrawable(drawable: android.graphics.drawable.Drawable) {
    // Custom wrapper to properly output system native icons inside Jetpack Compose
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                setImageDrawable(drawable)
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ==========================================
// 6. FIREWALL DASHBOARD SCREEN
// ==========================================
@Composable
fun FirewallScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val isFirewallEnabled by viewModel.isFirewallEnabled.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    val firewalledRequests by viewModel.firewalledRequests.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Firewall Console",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "Direct application packets locally to prevent unauthorized leaks offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Large Switch Card for Firewall master toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isFirewallEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isFirewallEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = if (isFirewallEnabled) Color(0xFF29B6F6) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Shield Firewall status",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isFirewallEnabled) "Active ($firewalledRequests requests blocked offline)" else "Standby",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isFirewallEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setFirewallEnabled(checked)
                    },
                    modifier = Modifier.testTag("firewall_master_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search in page
        val query by viewModel.searchQuery.collectAsState()
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Filter apps to adjust rules...") },
            leadingIcon = { Icon(imageVector = Icons.Default.FilterList, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).testTag("firewall_apps_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                FirewallRowItem(
                    app = app,
                    onToggleWifi = { viewModel.toggleAppWifiBlocked(app.packageName, app.isWifiBlocked) },
                    onToggleMobile = { viewModel.toggleAppMobileBlocked(app.packageName, app.isMobileBlocked) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun FirewallRowItem(
    app: AppInfo,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    AndroidViewDrawable(drawable = app.icon)
                } else {
                    Icon(imageVector = Icons.Default.Android, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Wi-Fi restriction toggle
            IconButton(
                onClick = onToggleWifi,
                modifier = Modifier.testTag("wifi_block_toggle_${app.packageName}")
            ) {
                Icon(
                    imageVector = if (app.isWifiBlocked) Icons.Default.WifiOff else Icons.Default.Wifi,
                    contentDescription = "Wifi condition",
                    tint = if (app.isWifiBlocked) Color(0xFF29B6F6) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            // Cell signal restriction toggle
            IconButton(
                onClick = onToggleMobile,
                modifier = Modifier.testTag("mobile_block_toggle_${app.packageName}")
            ) {
                Icon(
                    imageVector = if (app.isMobileBlocked) Icons.Default.SignalCellularConnectedNoInternet0Bar else Icons.Default.SignalCellularAlt,
                    contentDescription = "Cellular condition",
                    tint = if (app.isMobileBlocked) Color(0xFFAB47BC) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ==========================================
// 7. RECURRING SCHEDULES SCREEN
// ==========================================
@Composable
fun SchedulesScreen(
    viewModel: MainViewModel
) {
    val schedules by viewModel.schedules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_schedule_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Schedule")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "Recurring Schedules",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "Automatically enforce boundaries based on dates and local clock loops.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (schedules.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AlarmOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No schedules set yet. Tap '+' to build one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("settings_schedules_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(schedules, key = { it.id }) { schedule ->
                        ScheduleItemCard(
                            schedule = schedule,
                            onToggleEnabled = { viewModel.toggleScheduleEnabled(schedule.id, it) },
                            onDelete = { viewModel.deleteSchedule(schedule.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CreateScheduleDialog(
            onDismiss = { showAddDialog = false },
            onConfirmAdd = { sched ->
                viewModel.addSchedule(sched)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ScheduleItemCard(
    schedule: FreezeSchedule,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S") // Sunday to Saturday mapped
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isEnabled) MaterialTheme.colorScheme.surface 
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = schedule.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (schedule.isEnabled) MaterialTheme.colorScheme.onSurface 
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${schedule.startTime} - ${schedule.endTime}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("schedule_switch_${schedule.id}")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Highlighted days dots row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (day in 1..7) {
                    val isActive = schedule.daysOfWeek.contains(day)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                    ) {
                        Text(
                            text = dayLabels[day - 1],
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boundaries checkboxes status tags
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (schedule.freezeApps) {
                    BadgeLabel("FREEZE", Color(0xFF00E676))
                }
                if (schedule.blockWifi) {
                    BadgeLabel("WIFI OFF", Color(0xFF29B6F6))
                }
                if (schedule.blockMobile) {
                    BadgeLabel("CELLULAR OFF", Color(0xFFAB47BC))
                }
            }
        }
    }
}

@Composable
fun CreateScheduleDialog(
    onDismiss: () -> Unit,
    onConfirmAdd: (FreezeSchedule) -> Unit
) {
    var label by remember { mutableStateOf("Workspace Block") }
    var startHours by remember { mutableStateOf("09") }
    var startMinutes by remember { mutableStateOf("00") }
    var endHours by remember { mutableStateOf("17") }
    var endMinutes by remember { mutableStateOf("00") }
    val daysSelected = remember { mutableStateListOf(2, 3, 4, 5, 6) } // Monday to Friday pre-filled

    var blockWifi by remember { mutableStateOf(true) }
    var blockMobile by remember { mutableStateOf(true) }
    var freezeApps by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Configure Boundaries",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Schedule Label") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_schedule_label")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Time hours rows inputs
                Text("Start Time (24h)", fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startHours,
                        onValueChange = { if (it.length <= 2) startHours = it },
                        modifier = Modifier.weight(1f).testTag("dialog_start_hour"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(":", fontWeight = FontWeight.Black)
                    OutlinedTextField(
                        value = startMinutes,
                        onValueChange = { if (it.length <= 2) startMinutes = it },
                        modifier = Modifier.weight(1f).testTag("dialog_start_min"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // End Time hours rows inputs
                Text("End Time (24h)", fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = endHours,
                        onValueChange = { if (it.length <= 2) endHours = it },
                        modifier = Modifier.weight(1f).testTag("dialog_end_hour"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(":", fontWeight = FontWeight.Black)
                    OutlinedTextField(
                        value = endMinutes,
                        onValueChange = { if (it.length <= 2) endMinutes = it },
                        modifier = Modifier.weight(1f).testTag("dialog_end_min"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Choose days of week
                Text("Weekly Days", fontWeight = FontWeight.Bold)
                val labels = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    for (day in 1..7) {
                        val selected = daysSelected.contains(day)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                .clickable {
                                    if (selected) daysSelected.remove(day) else daysSelected.add(day)
                                }
                        ) {
                            Text(
                                text = labels[day - 1],
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggles for scopes
                Text("Enforcement Scope", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = freezeApps, onCheckedChange = { freezeApps = it })
                    Text("Freeze Apps")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = blockWifi, onCheckedChange = { blockWifi = it })
                    Text("Block Wi-Fi")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = blockMobile, onCheckedChange = { blockMobile = it })
                    Text("Block Cellular")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val startString = "${startHours.padStart(2, '0')}:${startMinutes.padStart(2, '0')}"
                            val endString = "${endHours.padStart(2, '0')}:${endMinutes.padStart(2, '0')}"
                            val sched = FreezeSchedule(
                                id = System.currentTimeMillis().toString(),
                                label = label.ifEmpty { "Block Boundary" },
                                startTime = startString,
                                endTime = endString,
                                daysOfWeek = daysSelected.toList(),
                                blockWifi = blockWifi,
                                blockMobile = blockMobile,
                                freezeApps = freezeApps
                            )
                            onConfirmAdd(sched)
                        },
                        modifier = Modifier.testTag("dialog_save_schedule_button")
                    ) {
                        Text("Save Rule")
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. STATISTICS & ANALYTICS SCREEN
// ==========================================
@Composable
fun StatsScreen(
    viewModel: MainViewModel
) {
    val blockAttempts by viewModel.blockAttempts.collectAsState()
    val timeSavedMinutes by viewModel.timeSavedMinutes.collectAsState()
    val firewalledRequests by viewModel.firewalledRequests.collectAsState()
    val mostBlockedApp by viewModel.mostBlockedApp.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Activity Dashboard",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "Check metrics of total distractions blocked locally in real time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Large Premium Chart Canvas
        Text("Weekly Focus Minutes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val chartData = listOf(15f, 45f, 30f, 95f, 60f, 120f, 40f) // minutes Sunday to Saturday
                val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val maxVal = 140f
                    val spacing = size.width / (chartData.size)
                    val cornerRadius = CornerRadius(12f, 12f)
                    val barWidth = 32.dp.toPx()

                    chartData.forEachIndexed { index, minutes ->
                        val ratio = minutes / maxVal
                        val rectHeight = size.height * ratio
                        val x = (spacing * index) + (spacing / 2) - (barWidth / 2)
                        val y = size.height - rectHeight

                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, rectHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dayLabels.forEach { label ->
                        Text(
                            text = label, 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // High Metrics Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Enforcement Telemetry", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                MetricStatsRow(label = "Secure Focus Session blocks", value = "$blockAttempts times")
                MetricStatsRow(label = "Total productive time saved", value = "$timeSavedMinutes minutes")
                MetricStatsRow(label = "Firewall blocked leaks", value = "$firewalledRequests packets")
                MetricStatsRow(label = "Most blocked distraction", value = mostBlockedApp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MetricStatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
}

// ==========================================
// 9. SETTINGS & REBOOT RECOVERY
// ==========================================
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToPermissions: () -> Unit
) {
    val context = LocalContext.current
    val isStrictMode by viewModel.isStrictMode.collectAsState()
    val isFirewallEnabled by viewModel.isFirewallEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings Portal",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "Calibrate limits and recover background permissions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("General Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Strict mode Toggle
        ListItem(
            headlineContent = { Text("Shield Strict Lockout") },
            supportingContent = { Text("Hard lock early unlocks when focus screens run.") },
            leadingContent = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = isStrictMode,
                    onCheckedChange = { viewModel.setStrictMode(it) }
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        )

        // 2. Clear all frozen apps
        ListItem(
            headlineContent = { Text("Restore blocked apps state") },
            supportingContent = { Text("Clears app restrictions immediately.") },
            leadingContent = { Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null) },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    viewModel.stopFocusSession()
                    // Re-instating preferences
                    val manager = com.example.data.PreferencesManager(context)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        manager.clearAllFrozenApps()
                        manager.setFirewallEnabled(false)
                        androidx.compose.material3.SnackbarHostState().showSnackbar("Restrictions cleared.")
                    }
                }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Permission States & Optimization", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // 3. Launch permissions screen
        ListItem(
            headlineContent = { Text("Calibrate System permissions") },
            supportingContent = { Text("Checks accessibility, overlay, and local VPN authorization states.") },
            leadingContent = { Icon(imageVector = Icons.Default.SettingsAccessibility, contentDescription = null) },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNavigateToPermissions() }
        )

        // 4. Battery exemption
        ListItem(
            headlineContent = { Text("Bypass Battery Optimization") },
            supportingContent = { Text("Request standard Android permission to prevent background service sleep.") },
            leadingContent = { Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null) },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    context.startActivity(PermissionUtils.getBatteryOptimizationSettingsIntent(context))
                }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Privacy Section Frame
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Secure Local Sandbox", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FreezeX operates entirely offline on device sandboxes. Transmit logs, browsing streams, or app profiles are 100% private to you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

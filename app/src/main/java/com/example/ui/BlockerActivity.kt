package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlockerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
    }

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        preferencesManager = PreferencesManager(applicationContext)
        val packageBlocked = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "Unknown"
        val labelBlocked = intent.getStringExtra(EXTRA_APP_LABEL) ?: "Target Application"

        setContent {
            MyApplicationTheme {
                BlockerScreen(
                    appLabel = labelBlocked,
                    packageName = packageBlocked,
                    preferencesManager = preferencesManager,
                    onExitToHome = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun BlockerScreen(
    appLabel: String,
    packageName: String,
    preferencesManager: PreferencesManager,
    onExitToHome: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isFocusActive by preferencesManager.focusModeActiveFlow.collectAsState(initial = true)
    val focusEndTime by preferencesManager.focusModeEndTimeFlow.collectAsState(initial = 0L)
    val isStrictMode by preferencesManager.strictModeFlow.collectAsState(initial = false)

    var timeRemainingString by remember { mutableStateOf("00:00") }
    var progressFraction by remember { mutableStateOf(1f) }
    var totalSessionDuration by remember { mutableStateOf(1L) }

    // List of inspiring motivational quotes to encourage productivity
    val quotes = listOf(
        "Focus on being productive instead of busy.",
        "Your focus determines your reality.",
        "Deep work is the superpower of the 21st century.",
        "Action is the foundational key to all success.",
        "Starve your distractions, feed your focus.",
        "Disconnect to reconnect with what matters.",
        "Control your devices, don't let them control you."
    )
    
    val displayQuote = remember(packageName) {
        quotes[packageName.hashCode().coerceAtLeast(0) % quotes.size]
    }

    LaunchedEffect(focusEndTime, isFocusActive) {
        val now = System.currentTimeMillis()
        if (focusEndTime > now && isFocusActive) {
            totalSessionDuration = (focusEndTime - now).coerceAtLeast(1000L)
            while (true) {
                val current = System.currentTimeMillis()
                val remaining = focusEndTime - current
                if (remaining <= 0) {
                    timeRemainingString = "00:00"
                    progressFraction = 0f
                    // Focus complete, stop the blocker activity automatically
                    preferencesManager.stopFocusSession()
                    onExitToHome()
                    break
                } else {
                    val minutes = (remaining / 1000) / 60
                    val seconds = (remaining / 1000) % 60
                    timeRemainingString = String.format("%02d:%02d", minutes, seconds)
                    progressFraction = remaining.toFloat() / totalSessionDuration.toFloat()
                }
                delay(1000L)
            }
        } else {
            timeRemainingString = "00:00"
            progressFraction = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(24.dp)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon Badge with Glowing Border Effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Frozen",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Block Notification Labels
            Text(
                text = appLabel,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "is currently Frozen",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Cool circular/linear timer indicator progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Timer",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeRemainingString,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Card container for motivational focus quote
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Quote",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "\"$displayQuote\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Actions: Return to home or bypass checks
            Button(
                onClick = onExitToHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("exit_to_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Workspace",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (isFocusActive && !isStrictMode) {
                Spacer(modifier = Modifier.height(16.dp))
                // Unlock optional action if strict mode is disabled
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.stopFocusSession()
                            onExitToHome()
                        }
                    },
                    modifier = Modifier.testTag("unlock_session_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pause Session (Disable limits)",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            } else if (isFocusActive && isStrictMode) {
                Spacer(modifier = Modifier.height(24.dp))
                // Indicate strict mode constraint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Strict Locked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Strict Mode active: Early bypass is locked",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

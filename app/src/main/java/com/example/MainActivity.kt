package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AsistenDatabase
import com.example.notification.BriefingWorker
import com.example.ui.MainViewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DataImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read theme preference and apply to AppCompatDelegate
        val appPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val mainPrefs = getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)
        val setupDone = appPrefs.getBoolean("setup_done", false) || mainPrefs.getBoolean("setup_done", false)

        // FIX 6: Clear semua data lama jika setup belum selesai
        if (!setupDone) {
            val onboardingCompleted = mainPrefs.getBoolean("onboarding_completed", false) || appPrefs.getBoolean("onboarding_completed", false)
            appPrefs.edit().clear().apply()
            mainPrefs.edit().clear().apply()
            getSharedPreferences("premium", Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("premium_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("chat_limit", Context.MODE_PRIVATE).edit().clear().apply()
            if (onboardingCompleted) {
                mainPrefs.edit().putBoolean("onboarding_completed", true).apply()
                appPrefs.edit().putBoolean("onboarding_completed", true).apply()
            }
        }

        val themePref = mainPrefs.getString("theme_mode", "auto") ?: "auto"
        AppCompatDelegate.setDefaultNightMode(
            when (themePref) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )

        // 1. Pindahkan inisialisasi database & import data ke background thread
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                // Init database & import in background thread
                val db = AsistenDatabase.getDatabase(applicationContext)
                if (!setupDone) {
                    db.configDao().deleteAll()
                    db.jadwalDao().deleteAll()
                }
                DataImporter.importDataIfNeeded(applicationContext, db)
                BriefingWorker.scheduleDailyBriefing(applicationContext, 20, 0)
            }
            // Minimal splash 500ms agar transisi halus dan bebas lag
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 500) {
                delay(500 - elapsed)
            }
            isLoading = false
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                // Request notification permission on Android 13+ (API 33+) once loading is complete
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle permission response gracefully */ }

                LaunchedEffect(isLoading) {
                    if (!isLoading && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasNotificationPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = isLoading, label = "splash_transition") { loading ->
                        if (loading) {
                            SplashScreen()
                        } else {
                            AppNavigation(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Asisten Mengajar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Memuat data aplikasi...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

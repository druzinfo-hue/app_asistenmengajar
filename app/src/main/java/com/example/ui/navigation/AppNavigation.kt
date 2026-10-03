package com.example.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.ui.MainViewModel
import com.example.ui.main.MainScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.setup.SetupAwalScreen

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val context = LocalContext.current
    val mainPrefs = remember { context.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE) }
    val appPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    val userName by viewModel.userName.collectAsState()

    var isOnboardingCompleted by remember {
        mutableStateOf(mainPrefs.getBoolean("onboarding_completed", false) || appPrefs.getBoolean("onboarding_completed", false))
    }
    var isSetupCompleted by remember {
        val done = mainPrefs.getBoolean("setup_done", false) || appPrefs.getBoolean("setup_done", false)
        mutableStateOf(done)
    }

    // FIX 1 & FIX 3: Setup Awal WAJIB (tidak bisa di-skip). Jika nama masih kosong, tetap di Setup Awal.
    val effectiveSetupDone = isSetupCompleted && (userName?.isNotBlank() == true)

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            onFinish = {
                mainPrefs.edit().putBoolean("onboarding_completed", true).apply()
                appPrefs.edit().putBoolean("onboarding_completed", true).apply()
                isOnboardingCompleted = true
            }
        )
    } else if (!effectiveSetupDone) {
        SetupAwalScreen(
            viewModel = viewModel,
            onSetupComplete = {
                mainPrefs.edit().putBoolean("setup_done", true).apply()
                appPrefs.edit().putBoolean("setup_done", true).apply()
                isSetupCompleted = true
            }
        )
    } else {
        MainScreen(
            viewModel = viewModel,
            onNavigateToSetup = {
                isSetupCompleted = false
            }
        )
    }
}



package com.example.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.generator.GeneratorScreen
import com.example.ui.jadwal.JadwalScreen
import com.example.ui.kalender.KalenderScreen
import com.example.ui.materi.MateriScreen
import com.example.ui.navigation.Screen
import com.example.ui.progress.ProgressScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.GreenPrimary
import com.example.util.WhatsAppShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val teacherSchool by viewModel.userSchool.collectAsState()
    val kelasUtama by viewModel.userKelasUtama.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val isSpeaking by viewModel.voiceOutputManager.isSpeaking.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }

    fun navigateTo(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        scope.launch { drawerState.close() }
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Tentang Asisten Mengajar",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Asisten Mengajar Guru SD",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = GreenPrimary
                    )
                    Text(
                        text = "Versi: 1.2.0 • Kurikulum Merdeka",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider()
                    Text(
                        text = "Aplikasi asisten cerdas untuk guru sekolah dasar Indonesia. Membantu pengelolaan jadwal mengajar, kalender pendidikan, bank materi ajar, tanya jawab kurikulum dengan asisten AI, generator soal & RPP, pengumuman WhatsApp wali murid, dan pemindai teks OCR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    Text(
                        text = "Pengembang: DRUZ.ID\nHak Cipta © 2026 DRUZ.ID",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Clear Chat Dialog from top bar action
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Hapus Riwayat Chat?") },
            text = { Text("Semua percakapan dengan asisten AI akan dihapus permanen dari memori aplikasi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearChatDialog = false
                        Toast.makeText(context, "Riwayat chat dibersihkan", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DrawerHeader(viewModel = viewModel)
                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(8.dp))

                    // Menu Utama Drawer (7 Menu)
                    DrawerMenuItem(
                        icon = Icons.Default.Home,
                        label = "Beranda",
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = { navigateTo(Screen.Dashboard.route) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.CalendarToday,
                        label = "Jadwal",
                        selected = currentRoute == Screen.Jadwal.route,
                        onClick = { navigateTo(Screen.Jadwal.route) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.CalendarMonth,
                        label = "Kalender",
                        selected = currentRoute == Screen.Kalender.route,
                        onClick = { navigateTo(Screen.Kalender.route) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Chat,
                        label = "Tanya AI",
                        selected = currentRoute == Screen.Chat.route,
                        onClick = { navigateTo(Screen.Chat.route) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.BarChart,
                        label = "Progress",
                        selected = currentRoute == Screen.Progress.route,
                        onClick = { navigateTo(Screen.Progress.route) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.MenuBook,
                        label = "Bank Materi",
                        selected = currentRoute == Screen.Materi.route,
                        onClick = { navigateTo(Screen.Materi.route) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.AutoAwesome,
                        label = "Generator",
                        selected = currentRoute == Screen.Generator.route,
                        onClick = { navigateTo(Screen.Generator.route) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Footer Drawer
                    DrawerMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Pengaturan",
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navigateTo(Screen.Settings.route) }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Info,
                        label = "Tentang Aplikasi",
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAboutDialog = true
                        }
                    )

                    DrawerMenuItem(
                        icon = Icons.Default.Help,
                        label = "Bantuan / Support",
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            WhatsAppShareHelper.openWhatsAppSupport(context)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                val screenTitle = when (currentRoute) {
                    Screen.Dashboard.route -> "Asisten Mengajar"
                    Screen.Jadwal.route -> "Jadwal Mengajar"
                    Screen.Kalender.route -> "Kalender Pendidikan"
                    Screen.Chat.route -> "Tanya AI"
                    Screen.Progress.route -> "Progress Materi"
                    Screen.Materi.route -> "Bank Materi"
                    Screen.Generator.route -> "Generator & Alat Guru"
                    Screen.Settings.route -> "Pengaturan"
                    else -> "Asisten Mengajar"
                }

                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = screenTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!teacherSchool.isNullOrBlank()) {
                                val kelasText = if (!kelasUtama.isNullOrBlank()) " - Kelas $kelasUtama" else ""
                                Text(
                                    text = "$teacherSchool$kelasText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Belum di-setup",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("btn_open_drawer")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Drawer")
                        }
                    },
                    actions = {
                        if (currentRoute == Screen.Chat.route) {
                            // TTS Audio Speaker toggle
                            IconButton(
                                onClick = {
                                    if (isSpeaking) {
                                        viewModel.voiceOutputManager.stop()
                                    } else {
                                        viewModel.setTtsEnabled(ttsEnabled != "true")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (ttsEnabled == "true") {
                                        if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp
                                    } else Icons.Default.VolumeOff,
                                    contentDescription = "Toggle Suara AI",
                                    tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            // Clear chat
                            IconButton(
                                onClick = { showClearChatDialog = true },
                                modifier = Modifier.testTag("clear_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Riwayat Chat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "🔔 Tidak ada notifikasi baru", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("btn_notification")
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifikasi")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = GreenPrimary
                    ),
                    modifier = Modifier.testTag("main_top_app_bar")
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    // 1. Beranda
                    val isDashboardSelected = currentRoute == Screen.Dashboard.route
                    NavigationBarItem(
                        selected = isDashboardSelected,
                        onClick = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isDashboardSelected) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Beranda"
                            )
                        },
                        label = {
                            Text(
                                text = "Beranda",
                                fontWeight = if (isDashboardSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GreenPrimary,
                            selectedTextColor = GreenPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )

                    // 2. Jadwal
                    val isJadwalSelected = currentRoute == Screen.Jadwal.route
                    NavigationBarItem(
                        selected = isJadwalSelected,
                        onClick = {
                            navController.navigate(Screen.Jadwal.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isJadwalSelected) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
                                contentDescription = "Jadwal"
                            )
                        },
                        label = {
                            Text(
                                text = "Jadwal",
                                fontWeight = if (isJadwalSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GreenPrimary,
                            selectedTextColor = GreenPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_jadwal")
                    )

                    // 3. Tanya AI
                    val isChatSelected = currentRoute == Screen.Chat.route
                    NavigationBarItem(
                        selected = isChatSelected,
                        onClick = {
                            navController.navigate(Screen.Chat.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isChatSelected) Icons.Filled.Chat else Icons.Outlined.Chat,
                                contentDescription = "Tanya AI"
                            )
                        },
                        label = {
                            Text(
                                text = "Tanya AI",
                                fontWeight = if (isChatSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GreenPrimary,
                            selectedTextColor = GreenPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_chat")
                    )

                    // 4. Bank Materi
                    val isMateriSelected = currentRoute == Screen.Materi.route
                    NavigationBarItem(
                        selected = isMateriSelected,
                        onClick = {
                            navController.navigate(Screen.Materi.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isMateriSelected) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                contentDescription = "Bank Materi"
                            )
                        },
                        label = {
                            Text(
                                text = "Bank Materi",
                                fontWeight = if (isMateriSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GreenPrimary,
                            selectedTextColor = GreenPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_materi")
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { navigateTo(Screen.Chat.route) },
                        onNavigateToProgress = { navigateTo(Screen.Progress.route) },
                        onNavigateToMateri = { navigateTo(Screen.Materi.route) },
                        onNavigateToSetup = {
                            onNavigateToSetup()
                            navController.navigate("setup_awal") {
                                popUpTo(0)
                            }
                        },
                        showTopBar = false
                    )
                }

                composable("setup_awal") {
                    com.example.ui.setup.SetupAwalScreen(
                        viewModel = viewModel,
                        onSetupComplete = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Jadwal.route) {
                    JadwalScreen(viewModel = viewModel, showTopBar = false)
                }

                composable(Screen.Kalender.route) {
                    KalenderScreen(viewModel = viewModel, showTopBar = false)
                }

                composable(Screen.Chat.route) {
                    ChatScreen(viewModel = viewModel, showTopBar = false)
                }

                composable(Screen.Progress.route) {
                    ProgressScreen(viewModel = viewModel, showTopBar = false)
                }

                composable(Screen.Materi.route) {
                    MateriScreen(viewModel = viewModel, showTopBar = false)
                }

                composable(Screen.Generator.route) {
                    GeneratorScreen(
                        viewModel = viewModel,
                        showTopBar = false,
                        onNavigateToDonasi = {
                            navController.navigate("donasi")
                        },
                        onDismissOrBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("donasi") {
                    com.example.ui.donasi.DonasiScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        showTopBar = false,
                        onNavigateToDashboard = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerHeader(viewModel: MainViewModel) {
    val teacherName by viewModel.userName.collectAsState()
    val teacherSchool by viewModel.userSchool.collectAsState()
    val teacherRole by viewModel.userRole.collectAsState()
    val kelasUtama by viewModel.userKelasUtama.collectAsState()

    val nama = teacherName.orEmpty().ifBlank { "Guru" }
    val school = teacherSchool.orEmpty()
    val role = teacherRole.orEmpty().ifBlank { "Guru SD" }
    val kelas = kelasUtama.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(GreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (nama.firstOrNull() ?: 'G').toString().uppercase(),
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = nama,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        if (school.isNotBlank()) {
            Text(
                text = "$role - $school",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        } else {
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }

        if (kelas.isNotBlank()) {
            Text(
                text = "Kelas $kelas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) GreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) GreenPrimary else MaterialTheme.colorScheme.onSurface
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("drawer_item_${label.lowercase().replace(" ", "_")}")
    )
}

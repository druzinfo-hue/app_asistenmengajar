package com.example.ui.onboarding

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Top Bar: Lewati button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < 2) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = "Lewati",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Slides
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("onboarding_pager")
            ) { page ->
                when (page) {
                    0 -> OnboardingPage1()
                    1 -> OnboardingPage2()
                    2 -> OnboardingPage3()
                }
            }

            // Indicator dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("onboarding_dots")
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            // Action Button: "Lanjut" on slide 1-2, "Mulai" on slide 3
            Button(
                onClick = {
                    if (pagerState.currentPage == 2) {
                        // Slide terakhir → selesai
                        onFinish()
                    } else {
                        // Slide lain → lanjut
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.currentPage + 1
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("onboarding_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary
                )
            ) {
                Text(
                    text = if (pagerState.currentPage == 2) "Mulai" else "Lanjut",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun OnboardingPageItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(110.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OnboardingPage1() {
    OnboardingPageItem(
        icon = Icons.Default.School,
        iconTint = GreenPrimary,
        title = "Asisten Mengajar Cerdas",
        description = "Kelola jadwal mengajar harian, materi Kurikulum Merdeka (Kelas 1-6 lengkap), dan kalender pendidikan langsung dari smartphone Anda.",
        modifier = Modifier.testTag("onboarding_page_1")
    )
}

@Composable
fun OnboardingPage2() {
    OnboardingPageItem(
        icon = Icons.Default.AutoAwesome,
        iconTint = GreenPrimary,
        title = "Cici AI & Generator RAG",
        description = "Susun lembar Bahan Ajar, Modul Ajar / RPP, dan Soal Latihan otomatis yang mengutip materi asli Buku Guru dan Buku Siswa Kemendikbud.",
        modifier = Modifier.testTag("onboarding_page_2")
    )
}

@Composable
fun OnboardingPage3() {
    OnboardingPageItem(
        icon = Icons.Default.CalendarMonth,
        iconTint = GreenPrimary,
        title = "Siap Mengajar Lebih Nyaman",
        description = "Pantau progres pembelajaran per pertemuan, buat pengumuman WhatsApp wali murid, dan jalani aktivitas belajar-mengajar dengan terorganisir.",
        modifier = Modifier.testTag("onboarding_page_3")
    )
}

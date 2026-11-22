package com.equalizer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.equalizer.app.ui.theme.Tokens
import kotlinx.coroutines.launch

data class OnboardingStep(val title: String, val body: String)

val ONBOARDING_STEPS = listOf(
    OnboardingStep(
        "Bienvenido a Equalizer",
        "Mejora el sonido de lo que ya escuchas: Spotify, YouTube en el navegador, podcasts y audiolibros. " +
            "No necesitas cambiar de app: Equalizer trabaja por detrás.",
    ),
    OnboardingStep(
        "Enciéndelo y elige un preset",
        "Toca el botón de encendido de arriba a la derecha. Luego elige un preset según lo que escuches: " +
            "Voces o Podcast para hablar, Bass Boost o Rock para música. " +
            "Los que tienen ★ son los recomendados para el aparato que estás usando.",
    ),
    OnboardingStep(
        "Cada aparato suena distinto",
        "Equalizer detecta si el sonido sale por el parlante del celular, por cable o por Bluetooth, " +
            "y ajusta la base para cada uno. Con audífonos Bluetooth, entra a la pestaña «Dispositivo» " +
            "y dile qué son (buds, over-ear, clips…) para que suenen mejor.",
    ),
    OnboardingStep(
        "Ajústalo a tu gusto",
        "Mueve las bandas hacia arriba para reforzar y hacia abajo para bajar. Si te gusta cómo queda, " +
            "guárdalo con «+ Guardar como…». Si en alguna app no notas el cambio, en la pestaña «Dispositivo» " +
            "puedes probar otro método. Esta guía siempre está allí, en «Ver guía de uso».",
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState(pageCount = { ONBOARDING_STEPS.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == ONBOARDING_STEPS.lastIndex
    Column(
        Modifier.fillMaxSize().background(Tokens.BgScreen).statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("Saltar", color = Tokens.TextSecondary) }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            val step = ONBOARDING_STEPS[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(Tokens.BrandPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${page + 1}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Tokens.OnBrand)
                }
                Spacer(Modifier.height(32.dp))
                Text(step.title, style = MaterialTheme.typography.headlineMedium, color = Tokens.TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(step.body, style = MaterialTheme.typography.bodyLarge, color = Tokens.TextSecondary, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(ONBOARDING_STEPS.size) { i ->
                Box(
                    Modifier.padding(4.dp).size(8.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) Tokens.BrandPrimary else Tokens.ControlOff),
                )
            }
        }
        Button(
            onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 24.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Tokens.BrandPrimary, contentColor = Tokens.OnBrand),
        ) { Text(if (last) "Empezar" else "Siguiente", style = MaterialTheme.typography.titleMedium) }
    }
}

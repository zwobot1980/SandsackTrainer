package ch.sandsackcoach.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme


class MainActivity : ComponentActivity() {

    private lateinit var tts: TextToSpeech
    private var ttsReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val languageResult = tts.setLanguage(Locale.US)
                ttsReady =
                    languageResult != TextToSpeech.LANG_MISSING_DATA &&
                            languageResult != TextToSpeech.LANG_NOT_SUPPORTED

                tts.setSpeechRate(1.0f)
            }
        }

        setContent {
            val pantone3035 = Color(0xFF003E51)

            val colorScheme = darkColorScheme(
                primary = pantone3035,
                onPrimary = Color.White,
                secondary = Color(0xFF1A6B7A),
                onSecondary = Color.White,
                tertiary = Color(0xFF004D61),
                background = Color(0xFF0A1A1F),
                surface = Color(0xFF12252B),
                onBackground = Color(0xFFE0F0F3),
                onSurface = Color(0xFFE0F0F3),
                surfaceVariant = Color(0xFF1A333A),
                onSurfaceVariant = Color(0xFFB0C8CF)
            )

            MaterialTheme(
                colorScheme = colorScheme
            ) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    CoachScreen(
                        ttsReady = ttsReady,
                        speak = { word ->
                            if (ttsReady) {
                                tts.speak(
                                    word,
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "technique"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1A1F)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.sandsack_splash
            ),
            contentDescription = "Sandsack Coach",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun CoachScreen(
    ttsReady: Boolean,
    speak: (String) -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var roundMinutes by remember { mutableIntStateOf(3) }
    var rounds by remember { mutableIntStateOf(5) }
    var restSeconds by remember { mutableIntStateOf(60) }
    var intervalSeconds by remember { mutableFloatStateOf(2f) }
    var category by remember { mutableStateOf("Alle") }

    var status by remember { mutableStateOf("Bereit für dein Training") }
    var timeLeft by remember { mutableIntStateOf(0) }
    var currentRound by remember { mutableIntStateOf(0) }

    val techniques = when (category) {
        "Faust" -> listOf(
            "Jab", "Cross", "Overhand Punch", "Lead Hook",
            "Rear Hook", "Uppercut", "Body Hook", "Elbow"
        )
        "Beine" -> listOf(
            "Front Kick", "Body Kick",
            "Low Kick", "Knee"
        )
        else -> listOf(
            "Jab", "Cross", "Overhand Punch", "Lead Hook",
            "Rear Hook", "Uppercut", "Body Hook",
            "Front Kick", "Body Kick",
            "Low Kick", "Knee", "Elbow"
        )
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect

        status = "Bereit machen!"

        for (seconds in 10 downTo 1) {
            timeLeft = seconds
            delay(1000)
        }

        for (roundNumber in 1..rounds) {
            if (!isRunning) return@LaunchedEffect

            currentRound = roundNumber
            status = "Runde $roundNumber von $rounds"

            val durationTenths = roundMinutes * 600
            val intervalTenths =
                (intervalSeconds * 10).toInt().coerceAtLeast(5)

            var elapsedTenths = 0
            var nextCommandTenths = 0
            var previousTechnique = ""

            while (elapsedTenths < durationTenths) {
                if (!isRunning) return@LaunchedEffect

                timeLeft =
                    ((durationTenths - elapsedTenths) + 9) / 10

                if (elapsedTenths >= nextCommandTenths) {
                    val available = techniques.filter {
                        it != previousTechnique
                    }.ifEmpty { techniques }

                    val technique = available.random()
                    previousTechnique = technique

                    status = "Runde $roundNumber: $technique"
                    speak(technique)

                    nextCommandTenths += intervalTenths
                }

                delay(100)
                elapsedTenths++
            }

            speak("Stop")

            if (roundNumber < rounds) {
                status = "Pause"
                timeLeft = restSeconds

                repeat(restSeconds) {
                    if (!isRunning) return@LaunchedEffect
                    delay(1000)
                    timeLeft--
                }
            }
        }

        status = "Training beendet!"
        timeLeft = 0
        speak("Training finished")
        isRunning = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Sandsack Coach",
            fontSize = 30.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Trainingskonfiguration")
        HorizontalDivider()

        Text("Technikgruppe")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Alle", "Faust", "Beine").forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    enabled = !isRunning,
                    label = { Text(option) }
                )
            }
        }

        HorizontalDivider()

        Text("Rundendauer: $roundMinutes Minuten")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = !isRunning,
                onClick = {
                    roundMinutes = (roundMinutes - 1).coerceAtLeast(1)
                }
            ) { Text("−") }

            Text(
                "$roundMinutes min",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 20.sp
            )

            Button(
                enabled = !isRunning,
                onClick = {
                    roundMinutes = (roundMinutes + 1).coerceAtMost(10)
                }
            ) { Text("+") }
        }

        Text("Anzahl Runden: $rounds")
        Slider(
            value = rounds.toFloat(),
            onValueChange = { rounds = it.toInt() },
            enabled = !isRunning,
            valueRange = 1f..12f,
            steps = 10
        )

        Text("Pause: $restSeconds Sekunden")
        Slider(
            value = restSeconds.toFloat(),
            onValueChange = { restSeconds = it.toInt() },
            enabled = !isRunning,
            valueRange = 15f..120f,
            steps = 6
        )

        Text(
            "Abstand zwischen Kommandos: " +
                    String.format(Locale.US, "%.1f", intervalSeconds) +
                    " Sekunden"
        )
        Slider(
            value = intervalSeconds,
            onValueChange = { intervalSeconds = it },
            enabled = !isRunning,
            valueRange = 0.5f..3f
        )

        HorizontalDivider()

        Text(
            status,
            style = MaterialTheme.typography.titleMedium
        )

        if (isRunning) {
            Text(
                if (currentRound == 0) {
                    "Start in $timeLeft Sekunden"
                } else {
                    "Runde $currentRound / $rounds " +
                            "%02d:%02d".format(
                                timeLeft / 60,
                                timeLeft % 60
                            )
                },
                fontSize = 26.sp
            )
        }

        Button(
            onClick = {
                if (isRunning) {
                    isRunning = false
                    speak("Stop")
                } else {
                    currentRound = 0
                    isRunning = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            enabled = ttsReady || isRunning
        ) {
            Text(
                if (isRunning) "STOP" else "START",
                fontSize = 22.sp
            )
        }

        if (!ttsReady) {
            Text(
                "Sprachsynthese wird geladen oder ist nicht verfügbar.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
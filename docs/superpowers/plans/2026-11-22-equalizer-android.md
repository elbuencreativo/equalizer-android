# Equalizer Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App Android personal que ecualiza el audio de otras apps (Spotify, Pocket Casts, audiolibros, YouTube, VLC), con corrección base por dispositivo de salida y preset de contenido encima.

**Architecture:** Un servicio en primer plano detecta la salida activa (`AudioDeviceCallback`) y escucha los avisos de sesión de audio de los reproductores; por cada sesión crea un `DynamicsProcessing` con la curva calculada (perfil base + preset, en dB por banda). La lógica de curvas, perfiles y recomendaciones es Kotlin puro y testeable en JVM; la capa Android se prueba en el teléfono. Primero se construye un prototipo (Tareas 1-4) y se detiene en una compuerta de verificación con las apps reales antes de seguir.

**Tech Stack:** Kotlin, Android SDK (minSdk 28), Jetpack Compose, DataStore, JUnit 4, GitHub Releases para actualizaciones.

**Spec:** `docs/superpowers/specs/2026-11-22-equalizer-android-design.md`

## Global Constraints

- Plataforma: Android nativo en Kotlin; `minSdk 28` (requerido por `DynamicsProcessing`); dispositivo objetivo Samsung Galaxy A57 (Android 16/17, versión instalada por confirmar).
- No usar EQ sobre la sesión 0 (mezcla global): obsoleto.
- Paquete: `com.equalizer.app`.
- Bandas fijas: 10, en Hz `31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000`.
- Combinación: `ganancia_final[i] = perfil_base[i] + preset[i]`; cada banda limitada a `[-12, +12]` dB; preamp automático = `-max(0, mayor ganancia final)`.
- Tipos de dispositivo: `OVER_EAR`, `BUDS`, `CLIPS`, `IN_EAR_WIRED`, `PHONE_SPEAKER`, `TABLET_SPEAKER`, `BT_SPEAKER`.
- Presets: `FLAT`, `BASS_BOOST`, `VOCAL`, `PODCAST`, `POP`, `ROCK`, `ELECTRONIC`, `JAZZ_CLASSICAL`, `LOUDNESS`, `NIGHT`.
- Tipos de contenido: `POP`, `ROCK`, `ELECTRONIC`, `JAZZ_CLASSICAL`, `VOCAL`, `PODCAST`, `AUDIOBOOK`.
- Valores de partida de perfiles y presets son puntos de partida a validar de oído; los de clips y parlantes son hipótesis sin fuente.
- Persistencia: perfil base cambia por dispositivo; el preset de contenido es global.
- Bluetooth: clave de dispositivo = dirección, con nombre como respaldo; sin tipo asignado = perfil plano.
- UI: tema oscuro (fondo `#0E0E0E`, superficie `#2D2D2D`, principal cian `#00B2FF`, texto `#FFFFFF`, secundario `#B8B8B8`, borde `#464646`), tipografía Inter, espaciado base 4 pt. Texto/fondo con contraste ≥ 4.5:1. No copiar marca ni pantallas de la app de referencia.
- Distribución: repositorio público de GitHub con APK por release; APK firmado siempre con la misma llave guardada fuera del repositorio; el usuario confirma cada actualización. No crear el repositorio remoto sin aprobación de Leo.
- Interfaz y textos de la app en español.

## Review Focus

1. Bluetooth se desconecta con audio sonando: la salida vuelve al parlante, el perfil cambia y la app no se cae.
2. Un reproductor cierra su sesión de audio (o anuncia dos veces la misma): el efecto se libera sin fugas y no se duplica.
3. Botón apagado durante la reproducción: el audio queda sin procesar y no queda ningún efecto activo.
4. Dispositivo Bluetooth nuevo sin tipo asignado, o dos salidas activas: perfil plano y la UI pide asignar tipo, sin elegir uno al azar.
5. Descarga de actualización interrumpida o APK con otra firma: no se instala y se muestra un error claro.

---

### Task 1: Proyecto y modelo de curvas (núcleo puro)

**Files:**
- Create: Gradle/Android project en la raíz (`settings.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml`), `.gitignore` (incluye `*.jks`, `local.properties`)
- Create: `app/src/main/kotlin/com/equalizer/app/core/EqCurve.kt`
- Test: `app/src/test/kotlin/com/equalizer/app/core/EqCurveTest.kt`

**Interfaces:**
- Produces: `val BAND_FREQS_HZ: List<Int>` (las 10 de Global Constraints); `data class EqCurve(val gainsDb: List<Float>)` (siempre 10 valores); `data class Combined(val bands: List<Float>, val preampDb: Float)`; `fun combine(base: EqCurve, preset: EqCurve): Combined`.

- [ ] **Step 1: `git init`, crear el proyecto Android vacío con Compose que compile (`./gradlew :app:assembleDebug`).**
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Escribir tests en `EqCurveTest`**
`combine_sumsPerBand` (base +2 y preset +3 en banda 0 → 5.0), `combine_clampsEachBandTo12` (base +10 y preset +10 → 12.0), `combine_preampIsMinusHighestPositive` (mayor ganancia final 6.0 → preampDb −6.0), `combine_allNegative_preampIsZero`, `curve_requiresTenBands` (9 valores → `IllegalArgumentException`).

- [ ] **Step 3: Correr y confirmar que fallan.**
Run: `./gradlew :app:testDebugUnitTest --tests "*EqCurveTest*"`  Expected: FAIL (clases no definidas).

- [ ] **Step 4: Implementar `EqCurve`, `Combined` y `combine`** según Global Constraints (validación de 10 bandas en `init`).

- [ ] **Step 5: Correr los tests.** Expected: PASS.

- [ ] **Step 6: Commit** `feat: project scaffold and EQ curve combination`.

---

### Task 2: Perfiles, presets y recomendaciones

**Files:**
- Create: `core/DeviceType.kt`, `core/Preset.kt`, `core/ContentType.kt`, `core/Profiles.kt`, `core/Recommendations.kt`
- Test: `core/ProfilesTest.kt`, `core/RecommendationsTest.kt`

**Interfaces:**
- Consumes: `EqCurve`, `BAND_FREQS_HZ`.
- Produces: enums `DeviceType`, `Preset`, `ContentType` (valores de Global Constraints) con `displayName: String` en español; `fun baseProfile(type: DeviceType?): EqCurve` (`null` = plano); `fun presetCurve(preset: Preset): EqCurve`; `fun recommend(device: DeviceType?, content: ContentType): Preset`; `fun recommendedFor(device: DeviceType?): Map<ContentType, Preset>`.

- [ ] **Step 1: Escribir tests de perfiles** (índices: 62 Hz = 1, 125 Hz = 2, 2000 Hz = 6):
`flat_isAllZero`; `nullDevice_isFlat`; `inEar_bassAtLeast6dbAboveOverEar_at62hz`; `clips_bassBoostAtLeast4db_at62hz`; `phoneSpeaker_cutsLowEnd` (banda 31 y 62 ≤ −6.0); `podcast_rollsOffBelow100AndLiftsPresence` (banda 62 ≤ −6.0 y banda 2000 en 2.0..3.0); `bassBoost_liftsLowBands` (banda 62 ≥ 4.0, banda 125 > 0).

- [ ] **Step 2: Escribir tests de recomendaciones:**
`phoneSpeaker_podcast_recommendsPodcast`; `anyDevice_audiobook_recommendsPodcast`; `overEar_jazzClassical_recommendsJazzClassical`; `clips_pop_recommendsBassBoost`; `nullDevice_vocal_recommendsVocal`; `recommendedFor_returnsEntryForEveryContentType`.

- [ ] **Step 3: Correr y confirmar que fallan.**
Run: `./gradlew :app:testDebugUnitTest --tests "*ProfilesTest*" --tests "*RecommendationsTest*"`

- [ ] **Step 4: Implementar** con los valores de partida del diseño: over-ear = Harman Over-Ear 2019 aproximada a 10 bandas; buds/in-ear = Harman In-Ear 2019 (≈ +10 dB bajo 125 Hz respecto a over-ear); clips = refuerzo fuerte de graves; parlantes de teléfono/tableta = corte bajo ~150 Hz y leve subida 1-4 kHz; parlante BT = casi plano. Presets según la tabla del diseño. Recomendaciones como tabla editable `Map<Pair<DeviceType?, ContentType>, Preset>` con valor por defecto sensato.

- [ ] **Step 5: Correr los tests.** Expected: PASS.

- [ ] **Step 6: Commit** `feat: device profiles, presets and recommendations`.

---

### Task 3: Detección de salida y registro de dispositivos

**Files:**
- Create: `device/ActiveOutput.kt`, `device/OutputDetector.kt`, `device/DeviceRegistry.kt`
- Test: `device/OutputMappingTest.kt` (JVM), `device/DeviceRegistryTest.kt` (JVM con DataStore en archivo temporal)

**Interfaces:**
- Consumes: `DeviceType`.
- Produces:
  - `enum class OutputKind { BUILT_IN_SPEAKER, WIRED, BLUETOOTH, UNKNOWN }`
  - `enum class TypeSource { ASSIGNED, DETECTED, UNASSIGNED }`
  - `data class ActiveOutput(val key: String, val displayName: String, val kind: OutputKind, val deviceType: DeviceType?, val typeSource: TypeSource)`
  - `fun mapKind(androidDeviceType: Int): OutputKind` (puro, recibe constantes de `AudioDeviceInfo`)
  - `fun defaultTypeFor(kind: OutputKind, isTablet: Boolean): DeviceType?` (`BUILT_IN_SPEAKER` → `PHONE_SPEAKER`/`TABLET_SPEAKER`; `WIRED` → `IN_EAR_WIRED`; `BLUETOOTH`/`UNKNOWN` → `null`)
  - `class DeviceRegistry(store)`: `suspend fun assign(key: String, type: DeviceType)`, `suspend fun typeFor(key: String, name: String): DeviceType?`
  - `class OutputDetector(context)`: `val active: StateFlow<ActiveOutput>`; `fun start()`, `fun stop()`.

- [ ] **Step 1: Tests de mapeo y registro:** `mapKind_a2dp_isBluetooth`, `mapKind_wiredHeadset_isWired`, `mapKind_builtinSpeaker_isBuiltIn`, `defaultType_bluetooth_isNull`, `registry_assignThenLookupByKey`, `registry_fallsBackToNameWhenKeyChanges`, `registry_unknownDevice_returnsNull`.

- [ ] **Step 2: Correr y confirmar que fallan.** Run: `./gradlew :app:testDebugUnitTest --tests "*OutputMappingTest*" --tests "*DeviceRegistryTest*"`

- [ ] **Step 3: Implementar** `mapKind`, `defaultTypeFor` y `DeviceRegistry` (DataStore Preferences, mapas clave→tipo y nombre→tipo).

- [ ] **Step 4: Implementar `OutputDetector`** con `AudioManager.registerAudioDeviceCallback` y `getDevices(GET_DEVICES_OUTPUTS)`: elige la salida que Android reporte como activa; si hay varias, Bluetooth manda sobre cable y cable sobre parlante interno. Calcula `typeSource` (`ASSIGNED` si el registro tiene tipo, `DETECTED` si salió de `defaultTypeFor`, `UNASSIGNED` si es `null`).

- [ ] **Step 5: Correr los tests.** Expected: PASS.

- [ ] **Step 6: Test instrumentado en el teléfono** `OutputDetectorInstrumentedTest.emitsBuiltInSpeakerWithoutHeadphones`. Run: `./gradlew :app:connectedDebugAndroidTest`  Expected: PASS con el teléfono por USB.

- [ ] **Step 7: Commit** `feat: audio output detection and device registry`.

---

### Task 4: Motor de EQ y escucha de sesiones (prototipo)

**Files:**
- Create: `engine/EqEngine.kt`, `engine/SessionReceiver.kt`, `engine/EqService.kt`
- Modify: `AndroidManifest.xml` (servicio en primer plano tipo `mediaPlayback`, receiver exportado para `android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION` y `...CLOSE_AUDIO_EFFECT_CONTROL_SESSION`, permisos de notificación y Bluetooth)
- Test: `engine/EqEngineTest.kt` (con fábrica de efectos falsa)

**Interfaces:**
- Consumes: `Combined`, `OutputDetector.active`.
- Produces:
  - `interface EffectHandle { fun apply(c: Combined); fun release() }`
  - `class EqEngine(factory: (sessionId: Int) -> EffectHandle)`: `fun onSessionOpened(id: Int, pkg: String)`, `fun onSessionClosed(id: Int)`, `fun setCurve(c: Combined)`, `fun setEnabled(on: Boolean)`, `val sessions: StateFlow<Map<Int, String>>` (id → paquete)
  - `class DynamicsEffectHandle(sessionId: Int) : EffectHandle` (usa `DynamicsProcessing` con 10 bandas de pre-EQ y `inputGain = preampDb`)

- [ ] **Step 1: Tests de `EqEngine`:** `sessionOpened_createsAndAppliesCurve`, `sameSessionOpenedTwice_createsOneHandle`, `sessionClosed_releasesHandle`, `setEnabled_false_releasesAllHandles`, `setEnabled_true_recreatesForKnownSessions`, `setCurve_reappliesToAllLiveSessions`, `factoryThrows_isCaughtAndSessionMarkedFailed`.

- [ ] **Step 2: Correr y confirmar que fallan.** Run: `./gradlew :app:testDebugUnitTest --tests "*EqEngineTest*"`

- [ ] **Step 3: Implementar `EqEngine` y `DynamicsEffectHandle`;** el receiver llama a `onSessionOpened/Closed` con `EXTRA_AUDIO_SESSION` y `EXTRA_PACKAGE_NAME`.

- [ ] **Step 4: Implementar `EqService`:** servicio en primer plano con notificación; combina `OutputDetector.active` + preset global desde DataStore y llama `setCurve` al cambiar cualquiera; reanuda el estado (encendido/apagado, preset) al reiniciarse.

- [ ] **Step 5: UI mínima de prueba** (una pantalla Compose): nombre de la salida activa, lista de apps con sesión detectada, botón encender/apagar y un preset de prueba `BASS_BOOST`.

- [ ] **Step 6: Correr los tests.** Expected: PASS. Luego `./gradlew :app:assembleDebug`.

- [ ] **Step 7: Commit** `feat: EQ engine, session receiver and prototype screen`.

---

### Task 5: Compuerta del prototipo en el Samsung A57 (manual, sin código)

**Files:**
- Create: `docs/superpowers/results/2026-prototype-matrix.md`

- [ ] **Step 1: Instalar por USB.** Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`  Expected: `Success`.

- [ ] **Step 2: Anotar versión de Android y One UI** del teléfono en el archivo de resultados.

- [ ] **Step 3: Probar una por una** Spotify, Pocket Casts, la app de audiolibros de Leo (obligatorias), YouTube, VLC, un servicio de video en streaming (deseables). Para cada una anotar: ¿aparece la sesión en la pantalla de prueba?, ¿se oye el cambio con `BASS_BOOST` encendido y se quita al apagar?. Probar con el parlante del teléfono y con audífonos Bluetooth.

- [ ] **Step 4: Probar si el EQ/Dolby Atmos de Samsung interfiere** (con y sin Dolby Atmos activo) y anotarlo.

- [ ] **Step 5: Decidir con Leo la compuerta:** si Spotify, Pocket Casts y audiolibros responden → seguir con la Tarea 6. Si falla alguna obligatoria → conceder `DUMP` (`adb shell pm grant com.equalizer.app android.permission.DUMP`), agregar descubrimiento de sesiones por `dumpsys audio` y repetir; si aún falla → replantear con Leo (plan C, reproductor propio). **No seguir sin esta decisión.**

---

### Task 6: Sistema de diseño y pantalla principal

**Files:**
- Create: `ui/theme/Tokens.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt` (Inter), `ui/components/PresetChip.kt`, `ui/components/BandSlider.kt`, `ui/components/PowerButton.kt`, `ui/components/DeviceCard.kt`, `ui/MainScreen.kt`, `ui/MainViewModel.kt`
- Test: `ui/theme/ContrastTest.kt` (JVM), `ui/MainViewModelTest.kt` (JVM), `ui/MainScreenTest.kt` (Compose instrumentado)

**Interfaces:**
- Consumes: `ActiveOutput`, `DeviceRegistry`, `recommendedFor`, `combine`, `EqEngine`.
- Produces: `object Tokens` con los colores de Global Constraints; `fun contrastRatio(fg: Color, bg: Color): Double`; `class MainViewModel` con `uiState: StateFlow<MainUiState>`, `fun onTogglePower()`, `fun onSelectPreset(p: Preset)`, `fun onAssignType(t: DeviceType)`, `fun onBandChanged(index: Int, db: Float)`; `data class MainUiState(output: ActiveOutput, enabled: Boolean, preset: Preset, recommended: Map<ContentType, Preset>, bands: List<Float>)`.

- [ ] **Step 1: Tests:** `contrast_textOnScreen_atLeast4_5`, `contrast_textOnSurface_atLeast4_5`, `contrast_onPrimaryOverPrimary_atLeast4_5`; `viewModel_unassignedBluetooth_exposesUnassignedSource`; `viewModel_assignType_updatesOutputAndCurve`; `viewModel_selectPreset_changesCombinedBands`; `viewModel_togglePower_disablesEngine`; `viewModel_recommendedMatchesDeviceType`; `viewModel_bandChanged_clampsTo12`.

- [ ] **Step 2: Correr y confirmar que fallan.** Run: `./gradlew :app:testDebugUnitTest --tests "*ContrastTest*" --tests "*MainViewModelTest*"`

- [ ] **Step 3: Implementar tokens, tema, tipografía y `MainViewModel`.**

- [ ] **Step 4: Implementar la pantalla principal** con, de arriba abajo: botón de encendido/apagado; tarjeta de dispositivo con nombre de la salida, tipo y cómo se identificó (texto según `TypeSource`: «Asignado por ti», «Detectado por la salida», «Sin asignar — elige el tipo»), con un toque para cambiar el tipo (lista de los 7 tipos); sección «Recomendado para este dispositivo» con una fila por tipo de contenido y su preset sugerido (tocar aplica el preset); fila de chips de preset; 10 bandas verticales con etiqueta de frecuencia y valor en dB; estado de las apps detectadas con aviso si una app conocida no responde.

- [ ] **Step 5: Test Compose** `MainScreenTest.showsDeviceNamePowerButtonAndRecommendations`: dado un `MainUiState` fijo, la pantalla muestra el nombre del dispositivo, el botón de encendido y la fila de recomendaciones. Run: `./gradlew :app:connectedDebugAndroidTest --tests "*MainScreenTest*"`

- [ ] **Step 6: Correr todos los tests.** Expected: PASS.

- [ ] **Step 7: Commit** `feat: design system and main screen`.

---

### Task 7: Presets propios

**Files:**
- Create: `core/CustomPresets.kt`
- Modify: `ui/MainViewModel.kt`, `ui/MainScreen.kt`
- Test: `core/CustomPresetsTest.kt`

**Interfaces:**
- Produces: `data class CustomPreset(val name: String, val curve: EqCurve)`; `class CustomPresets(store)`: `suspend fun save(p: CustomPreset)`, `suspend fun delete(name: String)`, `val all: Flow<List<CustomPreset>>`.

- [ ] **Step 1: Tests:** `save_thenListed`, `save_sameNameOverwrites`, `delete_removes`, `save_blankName_isRejected`.
- [ ] **Step 2: Correr y confirmar que fallan.** Run: `./gradlew :app:testDebugUnitTest --tests "*CustomPresetsTest*"`
- [ ] **Step 3: Implementar** con DataStore y exponerlo en la UI (botón «Guardar como preset», chips de presets propios).
- [ ] **Step 4: Correr tests.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: custom presets`.

---

### Task 8: Actualización por GitHub

**Files:**
- Create: `update/Version.kt`, `update/UpdateChecker.kt`, `update/Installer.kt`
- Modify: `AndroidManifest.xml` (`REQUEST_INSTALL_PACKAGES`, `FileProvider`), `app/build.gradle.kts` (constantes `GITHUB_OWNER`, `GITHUB_REPO`)
- Test: `update/VersionTest.kt`, `update/UpdateCheckerTest.kt`

**Interfaces:**
- Produces: `fun isNewer(current: String, latest: String): Boolean` (versiones `x.y.z`, ignora prefijo `v`); `data class ReleaseInfo(val version: String, val apkUrl: String, val sha256: String?)`; `class UpdateChecker(http)`: `suspend fun latest(): ReleaseInfo?`; `class Installer(context)`: `suspend fun downloadAndInstall(r: ReleaseInfo): InstallResult`; `sealed class InstallResult { Started, Failed(val reason: String) }`.

- [ ] **Step 1: Tests:** `isNewer_patchBump_true`, `isNewer_sameVersion_false`, `isNewer_vPrefixIgnored`, `isNewer_invalidString_false`; `latest_parsesReleaseJson` (JSON de ejemplo de la API de releases), `latest_noApkAsset_returnsNull`, `latest_httpError_returnsNull`.
- [ ] **Step 2: Correr y confirmar que fallan.** Run: `./gradlew :app:testDebugUnitTest --tests "*VersionTest*" --tests "*UpdateCheckerTest*"`
- [ ] **Step 3: Implementar** consultando `https://api.github.com/repos/{GITHUB_OWNER}/{GITHUB_REPO}/releases/latest` (repositorio público, sin token).
- [ ] **Step 4: Implementar `Installer`:** descarga a caché, verifica `sha256` si viene en la release, lanza el instalador del sistema vía `FileProvider`; descarga interrumpida o hash distinto → `Failed` con motivo y sin instalar. Android rechaza APK con firma distinta; mostrar el error del sistema.
- [ ] **Step 5: Pantalla de ajustes:** versión actual, botón «Buscar actualización», aviso de nueva versión y confirmación del usuario antes de instalar.
- [ ] **Step 6: Correr tests.** Expected: PASS.
- [ ] **Step 7: Commit** `feat: GitHub release updater`.

---

### Task 9: Firma, release y publicación (requiere aprobación de Leo para el remoto)

**Files:**
- Modify: `app/build.gradle.kts` (firma de release desde `keystore.properties`, fuera del repositorio)
- Create: `README.md`, `scripts/release.sh`, `.github/workflows/` no (publicación manual)

- [ ] **Step 1: Generar la llave de firma** fuera del repositorio y guardar su ruta en `keystore.properties` (ignorado por git). Hacer copia de seguridad: sin la misma llave no hay actualizaciones.
- [ ] **Step 2: Compilar release.** Run: `./gradlew :app:assembleRelease`  Expected: APK firmado; `apksigner verify --print-certs app-release.apk` muestra el certificado esperado.
- [ ] **Step 3: Instalar por USB** (`adb install -r`) y repetir la matriz de la Tarea 5 con el APK de release.
- [ ] **Step 4: Preguntar a Leo** si crea el repositorio público de GitHub (nombre y cuenta) antes de ejecutar cualquier `gh repo create` o `git push`. Tras su sí: crear el repositorio, fijar `GITHUB_OWNER`/`GITHUB_REPO`, subir el código.
- [ ] **Step 5: Publicar la release v0.1.0** con el APK y su `sha256`. Instalar una v0.1.1 de prueba desde la app para verificar el flujo de actualización.
- [ ] **Step 6: Commit** `chore: release signing and README`.

---

## Autorevisión

- **Cobertura del diseño:** detección y visualización del dispositivo (T3, T6); cómo se identificó el tipo y cambiarlo (T3, T6); recomendaciones por dispositivo y contenido (T2, T6); botón de encendido/apagado (T4, T6); presets y perfiles (T2); preset global y perfil por dispositivo (T4, T6); presets propios y bandas editables (T6, T7); prototipo y compuerta con apps obligatorias (T4, T5); respaldo `DUMP` y plan C (T5); actualización por GitHub y firma (T8, T9); instalación por USB (T5, T9); sistema de diseño (T6); aviso de choque con Samsung (T5 lo prueba; T6 muestra aviso de apps que no responden).
- **Fuera del alcance** (según el diseño): importar/exportar perfiles AutoEq, temas adicionales, acceso rápido por notificación/tile (opcional más adelante).
- **Consistencia de tipos:** `EqCurve`, `Combined`, `ActiveOutput`, `EqEngine` y sus firmas se usan igual en todas las tareas.
- **Pendientes del constructor:** frecuencias exactas de los valores de partida, nombres de paquete de las apps de audiolibros de Leo, ritmo de reintentos del detector.

# Matriz del prototipo — Samsung Galaxy A57 (SM-A576B)

Dispositivo: Android 17, One UI 9.0 (2026-11-22). Salida probada: parlante del teléfono.
Instaladas: Spotify, Pocket Casts, Audible, Voice (de.ph1b.audiobook), YouTube, Netflix. VLC no está instalado.

| App | Obligatoria | Anuncia sesión | Efecto procesa (prueba -24 dB) | Notas |
|---|---|---|---|---|
| Spotify | sí | sí (sesión 98529, 4 avisos seguidos) | sí, volumen cae de forma obvia | DynamicsProcessing visible en audio_flinger |
| Pocket Casts | sí | no (ni con reproducción nueva) | sí vía descubrimiento por `dumpsys audio` (DUMP), volumen cae de forma obvia | Usa su propio Loudness Enhancer; conviven |
| Voice (de.ph1b.audiobook) | sí | no | sí vía `dumpsys audio` (DUMP), volumen cae de forma obvia | App de audiolibros de Leo |
| Audible | no | no probada | no probada | |
| YouTube | no | pendiente | pendiente | |
| Netflix | no | pendiente | pendiente | |

## Hallazgos
- **Respaldo DUMP activo:** `adb shell pm grant com.equalizer.app android.permission.DUMP` + lectura de `dumpsys audio` descubre sesiones de apps que no anuncian la suya. Resuelve también las sesiones abiertas antes de arrancar el servicio.
- Nombre de la app no resoluble con `getPackagesForUid` (visibilidad de paquetes); falta `<queries>` o `QUERY_ALL_PACKAGES`.
- Las sesiones abiertas antes de que arranque el servicio se pierden (el aviso se envía una vez). Hay que descubrirlas al iniciar (p. ej. `AudioManager.getActivePlaybackConfigurations()`) o pedir a la app que reanude.
- Los presets se notan poco en el parlante del teléfono porque su perfil base ya recorta graves. Con audífonos el cambio es mayor. Valorar presets más marcados.
- El receptor dinámico dentro del servicio recibe los avisos de Spotify en One UI 9.
- Samsung Dolby Atmos / EQ: pendiente de probar.

## Veredicto de la compuerta (2026-11-22)
Spotify, Pocket Casts y Voice responden en el A57 (con respaldo DUMP). Compuerta superada: se sigue con la interfaz. YouTube, Netflix, Dolby Atmos y la prueba con audífonos Bluetooth quedan para la matriz de la release (Tarea 9).

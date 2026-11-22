# Equalizer (Android)

Ecualizador personal para Android que aplica una corrección por tipo de dispositivo de salida
(over-ear, buds, clips, in-ear de cable, parlantes) más un preset de contenido (voces, podcast, bass, etc.)
sobre el audio de otras apps (Spotify, Pocket Casts, Voice, …).

Diseño: `docs/superpowers/specs/2026-11-22-equalizer-android-design.md`
Plan: `docs/superpowers/plans/2026-11-22-equalizer-android.md`
Resultados en el A57: `docs/superpowers/results/2026-prototype-matrix.md`

## Instalar en otro teléfono o tableta (sin computador)

1. Abre la página de [Releases](https://github.com/elbuencreativo/equalizer-android/releases/latest) y descarga el archivo `equalizer-<versión>.apk`.
2. Ábrelo. Android pedirá permitir instalar apps desde esa fuente (el navegador o el gestor de archivos): acéptalo solo para esta instalación.
3. Abre Equalizer y acepta los permisos de notificaciones y Bluetooth.

Funciona en Android 9 o superior. El método «Global» (el predeterminado) no necesita más permisos.
Para ver qué canción suena en la tarjeta «Ahora suena», la app te lleva a darle acceso a notificaciones (solo lee las sesiones de medios).
Sin ADB, el modo «Por app» no descubre apps que no avisan su sesión; por eso se recomienda «Global».
Hay tres métodos (Configuración): «Global» (predeterminado, 10 bandas y balance), «Clásico» (ecualizador antiguo de Android, para teléfonos viejos que rechazan «Global»; menos bandas y sin balance) y «Por app».
Si un método falla, la app pasa sola al siguiente (Global → Clásico → Por app) y lo explica en pantalla.

## Instalar por USB (primera vez)

1. Activa la depuración USB en el teléfono y conéctalo.
2. `adb install -r app-release.apk`
3. Concede el permiso que permite descubrir apps que no anuncian su sesión de audio (una sola vez):

```bash
adb shell pm grant com.equalizer.app android.permission.DUMP
```

4. Abre la app y acepta los permisos de notificaciones y Bluetooth.

## Actualizaciones

La app consulta la última release del repositorio de GitHub y pide tu confirmación antes de instalar.
El APK debe estar firmado siempre con la misma llave: la copia de respaldo está en `~/.equalizer-signing/`
(guárdala fuera de este equipo; sin ella no hay actualizaciones, solo reinstalación).

## Compilar

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest      # pruebas
./scripts/release.sh                  # APK firmado en dist/
```

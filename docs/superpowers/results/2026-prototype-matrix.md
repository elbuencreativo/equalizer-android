# Matriz del prototipo — Samsung Galaxy A57 (SM-A576B)

Dispositivo: Android 17, One UI 9.0 (2026-11-22). Salida probada: parlante del teléfono.
Instaladas: Spotify, Pocket Casts, Audible, Voice (de.ph1b.audiobook), YouTube, Netflix. VLC no está instalado.

| App | Obligatoria | Anuncia sesión | Efecto procesa (prueba -24 dB) | Notas |
|---|---|---|---|---|
| Spotify | sí | sí (sesión 98529, 4 avisos seguidos) | sí, volumen cae de forma obvia | DynamicsProcessing visible en audio_flinger |
| Pocket Casts | sí | pendiente | pendiente | |
| Audible / Voice | sí (una) | pendiente | pendiente | |
| YouTube | no | pendiente | pendiente | |
| Netflix | no | pendiente | pendiente | |

## Hallazgos
- Las sesiones abiertas antes de que arranque el servicio se pierden (el aviso se envía una vez). Hay que descubrirlas al iniciar (p. ej. `AudioManager.getActivePlaybackConfigurations()`) o pedir a la app que reanude.
- Los presets se notan poco en el parlante del teléfono porque su perfil base ya recorta graves. Con audífonos el cambio es mayor. Valorar presets más marcados.
- El receptor dinámico dentro del servicio recibe los avisos de Spotify en One UI 9.
- Samsung Dolby Atmos / EQ: pendiente de probar.

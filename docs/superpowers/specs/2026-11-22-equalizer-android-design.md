# Equalizer Android: diseño

Fecha: 2026-11-22. Estado: borrador para aprobación.

## Intención y por qué

Leo escucha en muchos dispositivos: over-ear, buds, clips, in-ear de cable, parlante del celular y de la tableta, y parlantes Bluetooth externos. Cada uno suena distinto, y quiere cambiar entre presets comunes (voces, podcast, bass, géneros) sin reajustar todo cada vez. La app aplica una **corrección base según el dispositivo conectado** y, encima, un **preset según el contenido**.

Escucha casi todo en Spotify, YouTube, plataformas de streaming, VLC, apps de podcasts y audiolibros. Por eso la app debe afectar a esas apps, no ser un reproductor propio.

## Objetivos

- Detectar la salida de audio activa (parlante del teléfono, cable, Bluetooth) y cambiar de perfil sola.
- Perfil base por tipo de dispositivo: over-ear, buds, clips, in-ear de cable, parlante de celular, parlante de tableta, parlante Bluetooth.
- Presets de contenido: Plano, Bass Boost, Voces, Podcast/Spoken word, Pop, Rock, Electrónica/Hip-hop, Jazz/Clásica, Loudness, Night.
- Resultado = perfil base + preset. Bandas editables y presets propios guardables.
- Funcionar sobre Spotify, Pocket Casts y una app de audiolibros (obligatorias), y sobre YouTube, VLC y video en streaming (deseables).
- Actualizarse por internet desde las releases de GitHub.
- **Mostrar el dispositivo de audio conectado** en la pantalla principal: nombre y salida (parlante del teléfono, cable, Bluetooth).
- **Mostrar cómo se identificó el tipo de dispositivo** (asignado por Leo, detectado por la salida o aún sin asignar) y permitir cambiarlo con un toque. Para Bluetooth sin tipo asignado, ofrecer la lista de tipos al conectar.
- **Presets recomendados por tipo de contenido para ese dispositivo:** la pantalla principal sugiere, para el dispositivo activo, el preset más adecuado según lo que se escucha (música por género, voces, podcast, audiolibro). Ejemplo: parlante del teléfono + podcast sugiere Podcast con el corte de graves. Las recomendaciones salen de una tabla dispositivo × contenido, editable, que se valida de oído.
- **Botón de encendido/apagado** del ecualizador, visible en la pantalla principal. Apagado = audio sin procesar y sesiones liberadas.

Aprobado por Leo el 2026-11-22, incluido el repositorio público.
- Interfaz sencilla: elegir preset en una pantalla.

## No objetivos y antiobjetivos

- No es un reproductor de música ni gestiona archivos.
- No analiza el audio ni sube datos a internet.
- No es multiplataforma: solo Android. iOS no permite EQ sobre otras apps.
- Antiobjetivo: una app que parece funcionar pero deja fuera en silencio a las apps que Leo usa. Si una app no responde, la UI debe decirlo.

## Restricciones

- Dispositivo objetivo: Samsung Galaxy A57. Salió con Android 16 y One UI 8.5 ([Samsung](https://news.samsung.com/us/samsung-unveils-galaxy-a57-5g-and-galaxy-a37-5g-packing-pro-level-features-at-awesome-price/)) y se actualiza a Android 17 / One UI 9 ([Android Authority](https://androidauthority.com/samsung-galaxy-a57-stable-one-ui-9-update-3719924)). Falta confirmar la versión instalada.
- Android: no usar EQ sobre la sesión 0 (mezcla global), está obsoleto ([docs Android](https://developer.android.com/reference/android/media/audiofx/Equalizer)).
- Se necesita Android 9 o superior para `DynamicsProcessing`.
- Samsung trae su propio ecualizador y Dolby Atmos. Informes de usuarios sugieren que One UI 7/8 cambió o quitó controles. No encontré fuente sobre conflicto con apps de terceros, así que es un **riesgo sin verificar**.
- Los reproductores deben avisar su sesión de audio con `ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION`. No hay lista confirmada de apps que lo hagan.

## Enfoques considerados

- **A. EQ que se engancha a otras apps (elegido).** Se aplica un efecto de audio a las sesiones que anuncian los reproductores. Ventaja: Leo sigue usando sus apps. Riesgo: no todas anuncian su sesión, y Samsung puede interferir.
- **B. Reproductor propio.** Funciona siempre, pero deja fuera Spotify y el streaming. Descartado como base porque va contra el motivo de la app.
- **C. A con B de respaldo.** Se considera solo si el prototipo muestra que A falla con apps clave.

Leo eligió A tras saber que no escucha música local.

## Decisiones tomadas por Claude (a corregir si no gustan)

- Android nativo en Kotlin.
- Efecto principal: `DynamicsProcessing` (API 28+); `Equalizer` clásico solo como respaldo si hace falta.
- Un servicio en primer plano que detecta salidas con `AudioManager`/`AudioDeviceCallback` y escucha los avisos de sesión.
- Bluetooth: Android no distingue over-ear, buds o clips. La primera vez que se conecta un dispositivo, el usuario le asigna el tipo y la app lo recuerda por nombre/dirección.
- Cobertura de sesiones sin aviso: opcionalmente, la técnica que usa Wavelet (leer sesiones con el permiso `DUMP`, concedido una vez por ADB). Se decide después del prototipo, según qué apps no avisen.
- Primero un prototipo de una pantalla, sin perfiles finales, para probar con las apps reales de Leo. Resultado esperado: tabla de qué apps responden en el A57.

## Decisiones adicionales de Claude tras la revisión (a corregir si no gustan)

- **Perfil base + preset:** se suman en dB por banda (el preset es un delta sobre el perfil base). La app aplica un preamp automático igual al pico de ganancia positiva para evitar clipping.
- **Persistencia:** el perfil base cambia solo por dispositivo. El preset de contenido es global (si eliges Podcast, sigue activo al cambiar de buds al parlante). Opcional después: un acceso rápido (notificación o tile) para cambiar preset sin abrir la app.
- **Conflicto Samsung (EQ y Dolby Atmos):** no es bloqueante. La app lo detecta si puede, avisa en pantalla y le dice a Leo qué desactivar. Se prueba en la versión de Android/One UI que tenga instalada.
- **Bluetooth:** la clave del dispositivo es la dirección; si cambia, se cae al nombre. Mientras no se asigne tipo, se usa un perfil plano. Con varias salidas activas manda la que Android reporte como activa.
- **Importar o exportar perfiles (formato AutoEq, respaldo):** fuera del alcance de la primera versión.

## Datos de partida para los perfiles (a validar de oído)

Fuentes y números son puntos de partida, no estándares.

- Over-ear: curva Harman Over-Ear 2019 como base.
- Buds e in-ear de cable: Harman In-Ear 2019, con unos 10 dB más de graves bajo 125 Hz ([Spinorama](https://www.spinorama.org/headphone_scores.html)).
- Clips (open-ear): hipótesis, refuerzo fuerte de graves. Sin fuente.
- Parlante de celular/tableta: hipótesis, corte bajo ~150 Hz y leve subida 1-4 kHz. Sin fuente.
- Parlante Bluetooth: perfil editable, depende del modelo.
- Podcast: filtro bajo 80-100 Hz, bajar 200-500 Hz, subir 1-4 kHz unos 2-3 dB.
- Voces: subir 1-5 kHz. Bass: subir 50-100 Hz y 120-250 Hz. Rock: 100-250 Hz y 2-4 kHz. Jazz/Clásica: casi plano.

## Decisiones de Leo (confirmadas el 2026-11-22)

- **Criterio de éxito del prototipo:** Spotify, Pocket Casts y una app de audiolibros son obligatorias. YouTube, VLC y video en streaming son deseables. Si alguna obligatoria no responde, se activa el respaldo: permiso `DUMP` por ADB (técnica Wavelet) y, si tampoco alcanza, el reproductor propio (plan C).
- **Distribución:** app de uso personal, fuera de Play Store. Se publica en un repositorio de GitHub con el APK en cada release, y la app se actualiza por internet desde ahí. La primera instalación es por USB (`adb install`), lo que además permite conceder `DUMP` en el mismo paso.
- **Implicaciones (decisiones de Claude):** el APK se firma siempre con la misma llave, guardada fuera del repositorio. La app pide el permiso de instalar paquetes y consulta la última release de GitHub; el usuario confirma cada actualización. Se asume un repositorio público para no guardar un token dentro de la app. Falta que Leo confirme si el repositorio será público o privado, y no se crea ninguno hasta que él lo apruebe.

## Sistema de diseño (referentes de Leo)

Leo compartió capturas de un sistema de diseño de otra app de ecualizador ("UBooster") como referente de estilo. Se toma como inspiración de estructura y sensación, no se copia su marca ni sus pantallas.

- **Capas:** colores base, tokens semánticos (oscuro y claro), temas, componentes y pantallas. La app parte con un solo tema oscuro, con la estructura lista para sumar temas después.
- **Tokens base (oscuro):** fondo de pantalla `#0E0E0E`, superficie `#2D2D2D`, seleccionado `#0B3A50`, color principal cian `#00B2FF`, texto sobre el principal `#001820`, texto primario `#FFFFFF`, secundario `#B8B8B8`, terciario `#9E9E9E`, borde `#464646`, control apagado `#5A5A5A`. Equivalentes claros en las capturas.
- **Gradientes:** de marca (cian a ámbar), de sliders y volumen (cian a amarillo) y de chips (gris).
- **Accesibilidad:** todos los pares de texto y fondo cumplen WCAG 2.1 AA (≥ 4.5:1).
- **Tipografía:** Inter (Regular, Medium, Bold, Black), base de 4 pt, números grandes y en negrita para valores como ganancia en dB. Estilos de ejemplo: Display 48, Headline 26, Title 20/16, Body 16/14, Caption 12, Label 10.
- **Espaciado:** 2, 4, 8, 12, 16, 20, 24, 32, 40. **Radios:** 4, 8, 12, 20, 28, completo.
- **Componentes vistos:** pestañas con subrayado, chips de preset (uno activo en cian), botones primario y secundario, interruptores, botón de encendido, slider con gradiente, perillas con indicador ámbar, medidores de nivel azul a ámbar, tarjetas de superficie con borde de 1 px.
- **Pantalla de ecualizador de referencia:** fila de chips de preset arriba, bandas verticales con etiquetas de frecuencia, dos perillas (Bass y Virtualizer) abajo.
- **Para Equalizer:** las pestañas serían Dispositivo y Ecualizador. La pantalla principal muestra la salida activa, la fila de presets, las bandas y la ganancia general. Sin pestaña de música, porque no es un reproductor.

## Dejado al constructor

Número y frecuencias de bandas, diseño visual, almacenamiento de perfiles, nombres exactos de presets, límite de ganancia para evitar clipping, manejo del servicio y la batería.

## Pendiente de verificar en el teléfono

- Qué apps responden: Spotify, YouTube Music, YouTube, VLC, apps de podcasts y audiolibros, y servicios de streaming de video.
- Si el EQ y Dolby Atmos de Samsung chocan con la app.
- Versión de Android y One UI instalada.

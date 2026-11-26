<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="Icono de QuizApp">
</p>

<h1 align="center">QuizApp</h1>

<p align="center">
  Estudia con tus propios cuestionarios, sin conexión y sin cuentas.
</p>

QuizApp es una aplicación Android para crear exámenes a partir de tus apuntes.
Importa las preguntas desde un archivo JSON y practica cuando quieras. Todo se
guarda en el propio dispositivo: la app no pide permisos, no incluye anuncios y
no envía datos a ningún servidor.

**Versión 1.0.9** · Android 7.0 o superior.

## Qué puedes hacer

- Importar preguntas desde un archivo `.json` o pegando el texto.
- Importar **varios archivos a la vez**, con resumen de lo importado.
- Organizar los conjuntos en **grupos** y cambiarles el nombre.
- Hacer un examen **completo** o solo con las preguntas **falladas**.
- Elegir orden **aleatorio** o **secuencial**.
- Navegar libremente entre preguntas y usar el **mapa de estado**.
- **Salir y continuar** un examen donde lo dejaste; el progreso aparece en Inicio.
- Responder preguntas de **una o varias opciones** (se detecta solo).
- Comprobar cada respuesta y leer su **explicación**.
- Ver la nota, los aciertos, los fallos y las preguntas sin contestar.
- Consultar el **historial** y la **evolución de aciertos por grupo y por examen**, con el número de intentos.
- Repasar las **preguntas falladas** agrupadas por grupo y examen, con el número de fallos de cada una, eliminarlas o exportarlas a TXT/JSON.
- Cambiar entre tema **oscuro y claro**.

## Instalar

### Google Play

La publicación en Google Play está en preparación. Los materiales de la ficha
no forman parte de este repositorio.

### APK

Puedes descargar la última versión desde la sección
[**Releases**](../../releases). Si prefieres compilarla tú mismo, sigue las
instrucciones de abajo.

## Formato del JSON

El archivo debe ser un array de preguntas:

```json
[
  {
    "id": 1,
    "question": "¿Cuál es el animal terrestre más rápido?",
    "options": ["León", "Guepardo", "Tigre", "Canguro"],
    "correct_answer": "Guepardo",
    "explanation": "El guepardo alcanza hasta 120 km/h en distancias cortas."
  },
  {
    "id": 2,
    "question": "¿Cuáles de estos son lenguajes de programación? (marca 2)",
    "options": ["Python", "HTML", "Java", "CSS"],
    "correct_answer": "A, C",
    "explanation": "Python y Java son lenguajes de programación; HTML y CSS no."
  }
]
```

- `id`, `question`, `options` y `correct_answer` son obligatorios.
- `explanation` es opcional.
- `correct_answer` puede ser el texto exacto de una opción, una letra (`"A"`) o
  varias letras separadas (`"A, C"` o `"A and C"`).

## Privacidad

La app funciona completamente offline. No recopila datos personales, no usa
analítica, no muestra anuncios y no tiene permiso de internet. Los conjuntos, las
preguntas falladas y el historial se guardan en una base de datos local y se
borran al desinstalar la app.

Lee la [política de privacidad completa](PRIVACY.md).

## Compilar

Requisitos:

- JDK 17
- Android Studio o el Android SDK con la plataforma 36

```bash
./gradlew assembleDebug
```

El APK de debug queda en:

```
app/build/outputs/apk/debug/app-debug.apk
```

Para ejecutar los tests:

```bash
./gradlew test
```

## Firmar para Google Play

El repositorio no incluye ningún keystore ni contraseña. Para generar un AAB
firmado, crea un archivo `keystore.properties` en la raíz (está ignorado por git):

```properties
storeFile=app/release.keystore
storePassword=TU_CONTRASEÑA
keyAlias=release
keyPassword=TU_CONTRASEÑA
```

Después ejecuta:

```bash
./gradlew bundleRelease
```

El archivo queda en `app/build/outputs/bundle/release/app-release.aab`.

> Guarda el keystore y sus contraseñas en un lugar seguro. Sin ellos no podrás
> publicar actualizaciones de la app en Google Play.

## Estructura

```
app/src/main/java/com/digarivas/quizapp/
├── MainActivity.kt
├── data/            Room: entidades, DAOs, base de datos y repositorio
├── domain/          Modelos y lector de JSON
├── settings/        Preferencias de tema
└── ui/              Componentes, navegación, pantallas, tema y ViewModels

app/src/test/        Tests unitarios
app/src/androidTest/ Tests de instrumentación
```

La app está escrita en Kotlin con Jetpack Compose y Room.

## Créditos

Tipografías [Geist](https://github.com/vercel/geist-font) y
[Space Grotesk](https://github.com/floriankarsten/space-grotesk), incluidas en
la app bajo licencia SIL Open Font License. Los textos de las licencias están en
`app/src/main/assets/licenses/`.

## Licencia

El código es público para consulta y uso personal. **No se permite redistribuir
la aplicación ni publicar versiones derivadas** sin permiso. Consulta
[LICENSE](LICENSE).

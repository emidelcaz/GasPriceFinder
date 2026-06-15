# ⛽ GasPriceFinder

**Consulta precios de combustible oficiales en tiempo real, directamente desde tu móvil.**

GasPriceFinder es una aplicación nativa para Android que nace de la idea de transformar los datos públicos del MITECO en una herramienta útil y directa para el conductor. En lugar de consultar tablas estáticas en una web, la app te sitúa en el mapa, te muestra las estaciones de servicio más cercanas y te permite comparar precios, guardar favoritos y consultar la evolución histórica de cada gasolinera.

La aplicación fue desarrollada como Trabajo Fin de Ciclo del Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM), buscando siempre el equilibrio entre una experiencia de usuario sencilla y una arquitectura técnica sólida.

---

## ✨ ¿Qué puedes hacer?

- **Localizar estaciones cercanas** mediante geolocalización, con visualización en mapa interactivo y listado ordenado por distancia.
- **Consultar precios oficiales** de todos los carburantes disponibles, actualizados desde la API del MITECO.
- **Guardar favoritos** para tener siempre a mano las estaciones de tu zona o de tu ruta habitual.
- **Revisar el historial de precios** y detectar tendencias de subida o bajada.
- **Calcular el ahorro estimado** respecto a la media de tu zona para cada repostaje.
- **Usar perfiles locales** para distintos usuarios en el mismo dispositivo (por ejemplo, uso personal y profesional).
- **Funcionar sin conexión** gracias a la sincronización en segundo plano que mantiene la base de datos local actualizada.

---

## 🏗️ Arquitectura y tecnologías

El proyecto está construido con **Kotlin** y sigue los principios de **Clean Architecture**, separando claramente la capa de presentación, la lógica de negocio y el acceso a datos. La interfaz se ha desarrollado integramente con **Jetpack Compose** y **Material 3**, lo que permite una experiencia visual coherente y adaptable.

Para la gestión de dependencias se utiliza **Hilt**, y la persistencia local corre a cargo de **Room**, trabajando sobre SQLite. La sincronización con el servicio externo se resuelve mediante **WorkManager**, de modo que la app puede actualizar sus datos incluso cuando no está en primer plano. El consumo de la API REST del MITECO se realiza con **Retrofit** y **OkHttp**, y la geolocalización junto con el mapa interactivo se sirven de **Google Play Services** y **Maps Compose**.

El resultado es una aplicación modular, testeable y preparada para escalar sin complicar el mantenimiento.

---

## 🚀 Cómo probarla

### Requisitos
- Android Studio Ladybug o superior.
- SDK de Android 26 (Android 8.0) como mínimo.
- Una API key de Google Maps (se configura en `local.properties`).

### Configuración
1. Clona el repositorio.
2. Crea un archivo `local.properties` en la raíz del proyecto si no existe.
   > `local.properties` debe estar ignorado por Git.
3. Añade tu clave de Google Maps:
   ```properties
   sdk.dir=/ruta/a/tu/Android/Sdk
   MAPS_API_KEY=tu_clave_aqui
   ```
4. Sincroniza el proyecto con Gradle y ejecútalo en un emulador o dispositivo físico.

### Sobre la seguridad de la API key
- La clave se inyecta en el `AndroidManifest.xml` en tiempo de compilación gracias al plugin
  `secrets-gradle-plugin`, que lee `local.properties`.
- Eso **evita que la clave aparezca en el repositorio**, pero **no la oculta completamente del APK**:
  el SDK de Google Maps la necesita en el manifiesto, por lo que cualquier persona con el APK
  podría extraerla.
- Para reducir riesgos:
  - Restringe la clave en la [Google Cloud Console](https://console.cloud.google.com/) a la
    huella SHA-1 de tu keystore de firma y al nombre de paquete `com.gas_price_finder`.
  - Si la clave ha estado expuesta, **rótala** y actualiza `local.properties`.

---

## 🔮 Próximos pasos

El proyecto tiene líneas de evolución claras que se irán abordando poco a poco:

- Integración de un **modo en ruta**, para calcular la gasolinera óptima durante un trayecto largo.
- Sistema de **alertas de bajada de precio** en las estaciones marcadas como favoritas.
- Adaptación a **Android Auto**, para ofrecer la información al conductor sin distracciones.

---

## 📝 Nota

Este proyecto ha sido una oportunidad excelente para aplicar de forma integral todo lo aprendido durante el ciclo: desde el diseño de una arquitectura escalable hasta la gestión de permisos, estados de carga y la experiencia offline. Si tienes alguna sugerencia, encuentras un comportamiento extraño o simplemente quieres conversar sobre la implementación, estaré encantado de leerte.

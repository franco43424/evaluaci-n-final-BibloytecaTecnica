# 🛠️ Sistema de Registro y Creación de Informes Técnicos (BIBLOTECATECNICA)

[![GitHub license](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Plataforma](https://img.shields.io/badge/Plataforma-Android%20Studio-blue)](https://developer.android.com/studio)
[![SDK Mínimo](https://img.shields.io/badge/SDK%20M%C3%ADnimo-21-orange)](https://developer.android.com/about/dashboards)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-green)](https://developer.android.com/about/dashboards)



## 💡 Resumen del Proyecto

**BIBLOTECATECNICA** es una aplicación móvil diseñada para la **gestión y documentación de informes de procesos de desarme y evaluación** en empresas de manufactura. Su objetivo principal es centralizar el conocimiento técnico, permitiendo a los técnicos crear un registro detallado que sirva como referencia histórica para futuros trabajos con componentes similares.

---

## ✨ Características Principales

El proyecto está dividido en dos grandes roles: el Técnico y el Administrador.

### 👷 Técnico (Usuario)
* ✅ **Registro Fotográfico Integral:** Permite al técnico capturar y adjuntar un historial fotográfico del proceso de desarme/evaluación que está realizando.
* 📝 **Documentación Detallada:** Creación de informes de proceso paso a paso para el registro de acciones.
* 💾 **Base de Conocimiento:** Acceso a un registro de informes previos de componentes similares.

### 👤 Administrador
* 🔑 **Gestión de Cuentas:** Capacidad para crear y administrar las cuentas de acceso para cada técnico.
* 🗂️ **Generación de Informes PDF:** Exportación de informes de proceso en formato PDF para archivo o impresión.

---

## 💻 Stack Tecnológico

| Componente | Tecnología | Notas |
| :--- | :--- | :--- |
| **Lenguaje Principal** | `Java` | Lenguaje de desarrollo principal de la lógica de la aplicación. |
| **Plataforma/IDE** | `Android Studio` | Entorno de desarrollo oficial. |
| **Base de Datos** | `SQLite` | Persistencia local y robusta de los datos e informes. |
| **Generación PDF** | `android.graphics.pdf.PdfDocument` | Uso de la API nativa de Android (API 19+) para la creación de documentos PDF. |
| **Target Device** | `Medium Phone` | Optimizado para dispositivos móviles estándar. |

---

## ⚙️ Estructura y Módulos

El proyecto sigue una estructura modular para separar las responsabilidades:

1.  **Módulo de Autenticación:** Manejo de la creación de cuentas por el Administrador y el login de los Técnicos.
2.  **Módulo de Datos:** Clases `SQLite` para la gestión de la base de datos local y el manejo de los modelos de `Informe`.
3.  **Módulo de Cámara:** Implementación para la captura de fotos y su almacenamiento.
4.  **Módulo de Impresión/PDF:** Lógica para dibujar el contenido del informe en el `Canvas` de `PdfDocument`.

---

## 🚀 Guía de Instalación Rápida

1.  **Clonar el Repositorio:**
    ```bash
    git clone https://github.com/franco43424/evaluaci-n-final-BibloytecaTecnica.git
    ```
2.  Abrir la carpeta del proyecto en **Android Studio**.
3.  Esperar a que Gradle complete la sincronización de dependencias.
4.  Ejecutar el proyecto en un emulador o dispositivo físico con **Android 5.0 (API 21)** o superior.

---


# Onboarding Clientes API - Spring Boot

API REST empresarial para el proceso de onboarding, registro y administración de clientes personas físicas, desarrollada con Spring Boot, Spring Data JPA, arquitectura limpia por capas, validación Bean Validation y manejo centralizado de excepciones con códigos estandarizados.

---

## 🔐 Variables de Entorno (Environment Variables)

Para garantizar la seguridad de las credenciales y evitar datos sensibles quemados en el código fuente, la aplicación utiliza las siguientes variables de entorno:

| Variable de Entorno | Descripción | Valor por Defecto | Obligatoria |
|---|---|---|---|
| `DB_PASSWORD` | Contraseña de autenticación para la base de datos PostgreSQL | *(Sin valor por defecto)* | **Sí** (en perfil producción/local) |
| `DB_URL` | URL de conexión JDBC a la base de datos PostgreSQL | `jdbc:postgresql://localhost:5433/onboarding` | No |
| `DB_USER` | Usuario de conexión a la base de datos PostgreSQL | `postgres` | No |
| `PORT` | Puerto HTTP donde se ejecuta la aplicación Spring Boot | `8081` | No |
| `app.seguridad.inactividad-segundos` | Tiempo límite en segundos para expiración de sesión por inactividad | `5` | No |

### Ejemplo de Configuración en Terminal (PowerShell / Windows)

```powershell
$env:DB_PASSWORD="mi_contraseña_segura"
$env:DB_USER="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5433/onboarding"
$env:PORT="8081"
.\gradlew bootRun
```

### Ejemplo en Linux / Bash

```bash
export DB_PASSWORD="mi_contraseña_segura"
export DB_USER="postgres"
export DB_URL="jdbc:postgresql://localhost:5433/onboarding"
export PORT="8081"
./gradlew bootRun
```

---

## 🏛️ Arquitectura del Proyecto

El proyecto sigue una arquitectura en capas estricta y desacoplada:

- **`com.kapm.onboarding_clientes.controller`**: Capa web que expone los endpoints REST. Cero `@PathVariable`, cero `@RequestParam`, cero lógica de negocio y cero bloques `try-catch`. 100% de operaciones mediante `@RequestBody` con `@Valid`.
- **`com.kapm.onboarding_clientes.service`**: Interfaces de servicio e implementaciones (`ClienteServiceImpl`, `CuentaServiceImpl`, `SeguridadBiometriaServiceImpl`, `CatalogoServiceImpl`).
- **`com.kapm.onboarding_clientes.mapper`**: Componente `@Component ClienteMapper` dedicado para transformaciones DTO <-> Entity desacopladas.
- **`com.kapm.onboarding_clientes.repository`**: Repositorios Spring Data JPA y especificaciones dinámicas (`ClienteSpecification`).
- **`com.kapm.onboarding_clientes.entity`**: Entidades JPA de base de datos (`Cliente`, `Cuenta`, `Saldo`, `Domicilio`, `DatosSeguridadBiometria`) y catálogos en `entity.catalogo`.
- **`com.kapm.onboarding_clientes.dto`**: Objetos de transferencia de datos Request y Response (`PageResponse`, `ClienteResponse`, etc.).
- **`com.kapm.onboarding_clientes.exception`**: Manejo global de excepciones (`GlobalExceptionHandler`), catálogo enum `ErrorMessage` y excepciones de dominio personalizadas.
- **`com.kapm.onboarding_clientes.validation`**: Validadores Bean Validation personalizados y utilerías de validación oficial (RENAPO/SAT).

---

## 🛡️ Inmutabilidad de Datos de Identidad en Actualización

Bajo la estricta política de seguridad y consistencia bancaria, los siguientes **7 campos de identidad no se pueden modificar bajo ninguna circunstancia**:

1. `nombre`
2. `segundoNombre`
3. `apellido` (`apellidoPaterno` y `apellidoMaterno`)
4. `fechaNacimiento`
5. `curp` *(solo utilizable como identificador de búsqueda en el body, nunca se sobrescribe)*
6. `rfc` *(solo utilizable como identificador de búsqueda en el body, nunca se sobrescribe)*
7. `nacionalidad`

### Estrategia de Defensa en Profundidad

- **1. DTOs de Actualización (`ClienteUpdateRequest`, `ActualizarPorCurpRequest`, `ActualizarPorRfcRequest`)**:
  - Excluyen totalmente los campos de identidad no modificables. Solo reciben atributos editables (contacto, estado civil, sexo, domicilio, datos laborales).
  - Configurados con `@JsonIgnoreProperties(ignoreUnknown = false)` y `spring.jackson.deserialization.fail-on-unknown-properties=true`. Si un cliente intenta enviar campos inmutables o desconocidos, la petición es rechazada de inmediato.
- **2. Manejo Centralizado de Excepciones (`GlobalExceptionHandler`)**:
  - Intercepta `HttpMessageNotReadableException` y detecta propiedades no reconocidas o inmutables.
  - Retorna HTTP `400 Bad Request` en formato estandarizado estricto de dos campos:
    ```json
    {
      "codigo": 2,
      "mensaje": "La solicitud contiene campos que no se pueden modificar o no están permitidos"
    }
    ```
- **3. Capa de Servicio (`ClienteServiceImpl`)**:
  - Cero invocaciones a setters de los campos inmutables.
  - Validación de unicidad de correo electrónico excluyendo al propio cliente (`existsByCorreoAndIdNot`) con retorno HTTP 409 (`ErrorMessage.CLIENTE_CORREO_DUPLICADO`).
- **4. Capa de Entidad (`Cliente.java`)**:
  - Todas las columnas de identidad cuentan con `@Column(..., updatable = false)` y `@JoinColumn(..., updatable = false)`. Hibernate garantiza que jamás sean incluidas en sentencias SQL `UPDATE`.

---

## 💳 Pagos de Servicios y Recargas (Gestopago / PuntoRed)

El flujo completo de pago (autenticación y 2FA, validación contra catálogo, reserva de saldo con bloqueo
pesimista, integración con `sendTx.do`, idempotencia, verificación con `confirmTx` y conciliación) y la
justificación de cada decisión de diseño están documentados en
[`docs/INTEGRACION_GESTOPAGO.md`](docs/INTEGRACION_GESTOPAGO.md).

---

## 🧪 Ejecución de Pruebas

Para compilar el proyecto y correr la suite de pruebas unitarias y de integración automatizadas (con base de datos H2 en memoria):

```powershell
.\gradlew clean test
```

Para generar el empaquetado de producción:

```powershell
.\gradlew clean build -x test
```


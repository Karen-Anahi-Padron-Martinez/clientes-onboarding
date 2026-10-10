# Documento Técnico de la Solución: Sistema de Onboarding de Clientes y Cuentas Bancarias

## 1. Resumen Ejecutivo
El **Sistema de Onboarding de Clientes Personas Físicas** es una aplicación enterprise desarrollada en **Java 21** con **Spring Boot 3.4.3** para instituciones financieras. Su objetivo principal es permitir a los ejecutivos bancarios capturar información personal, de contacto, domicilio, laboral y de seguridad de clientes personas físicas, aplicando validaciones estrictas de negocio en tiempo real, registrando sus credenciales de manera encriptada junto con plantillas biométricas, gestionando saldos contables y disponibles en una **tabla dedicada de saldos**, y controlando la sesión activa mediante la bandera **`loggeado` (true/false)** con un **temporizador automático de inactividad de 5 segundos**.

---

## 2. Arquitectura del Sistema

La solución adopta una **Arquitectura en Capas Decoplada (Layered Clean Architecture)**:

```
+-----------------------------------------------------------------------+
|                    CAPA DE PRESENTACIÓN / API REST                    |
|    ClienteController   |   CuentaController   |   AuthController      |
|    (Spring MVC REST + OpenAPI / Swagger UI + Glassmorphic Web UI)     |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                         CAPA DE SERVICIOS                             |
|  ClienteServiceImpl   |   CuentaServiceImpl   |  SeguridadBiometria   |
| (Reglas de Negocio, Cifrado BCrypt/SHA-256, Inactividad de 5s)        |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                        CAPA DE PERSISTENCIA                           |
|  ClienteRepo | CuentaRepo | SaldoRepo | DatosSeguridadBiometriaRepo   |
|                  (Spring Data JPA / Hibernate)                        |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                         BASE DE DATOS                                 |
|      PostgreSQL / MySQL / H2 In-Memory (Tipos de datos TEXT)          |
+-----------------------------------------------------------------------+
```

---

## 3. Diseño de Base de Datos y Optimización de Memoria

### 3.1 Criterios de Diseño Obligatorios
1. **Requisito "Sin uso de VARCHAR"**: Todas las columnas alfanuméricas de la base de datos se definen explícitamente como tipo **`TEXT`** (o `CHARACTER VARYING` omitido), garantizando flexibilidad y óptima administración de memoria dinámica en PostgreSQL.
2. **Tabla Dedicada de Saldos (`saldos`)**: Se creó una entidad y tabla separada para el manejo financiero con `saldo_disponible`, `saldo_contable` y `fecha_ultima_actualizacion`.
3. **Control de Sesión de Usuario (`loggeado` e Inactividad)**: La tabla de login incluye la bandera **`loggeado` (`BOOLEAN` true/false)** y el timestamp `ultima_actividad` para expirar la sesión tras **5 segundos** de inactividad.

### 3.2 Diagrama Entidad-Relación (ERD)

```mermaid
erDiagram
    CAT_SEXOS {
        bigint id PK
        string codigo "TEXT UNIQUE"
        string descripcion "TEXT"
    }

    CAT_ESTADOS_CIVILES {
        bigint id PK
        string codigo "TEXT UNIQUE"
        string descripcion "TEXT"
    }

    CAT_NACIONALIDADES {
        bigint id PK
        string codigo "TEXT UNIQUE"
        string descripcion "TEXT"
    }

    CAT_OCUPACIONES {
        bigint id PK
        string codigo "TEXT UNIQUE"
        string descripcion "TEXT"
    }

    CAT_ESTADOS_REPUBLICA {
        bigint id PK
        string codigo "TEXT UNIQUE"
        string descripcion "TEXT"
    }

    CAT_PAISES {
        bigint id PK
        string codigo "TEXT UNIQUE"
        string descripcion "TEXT"
    }

    CLIENTES {
        bigint id PK
        string nombre "TEXT"
        string segundo_nombre "TEXT"
        string apellido_paterno "TEXT"
        string apellido_materno "TEXT"
        date fecha_nacimiento "DATE"
        string curp "TEXT UNIQUE"
        string rfc "TEXT UNIQUE"
        bigint sexo_id FK
        bigint nacionalidad_id FK
        bigint estado_civil_id FK
        string correo "TEXT UNIQUE"
        string telefono_movil "TEXT"
        string telefono_alternativo "TEXT"
        bigint ocupacion_id FK
        string empresa "TEXT"
        decimal ingreso_mensual "NUMERIC(15,2)"
        boolean activo "BOOLEAN"
        timestamp fecha_registro "TIMESTAMP"
        bigint domicilio_id FK, UNIQUE
        bigint cuenta_id FK, UNIQUE
        bigint seguridad_id FK, UNIQUE
    }

    DOMICILIOS {
        bigint id PK
        string calle "TEXT"
        string numero_exterior "TEXT"
        string numero_interior "TEXT"
        string colonia "TEXT"
        string municipio "TEXT"
        bigint estado_id FK
        string codigo_postal "TEXT"
        bigint pais_id FK
    }

    CUENTAS {
        bigint id PK
        string numero_cuenta "TEXT UNIQUE"
        decimal saldo "NUMERIC(15,2)"
        string estatus "TEXT"
        timestamp fecha_creacion "TIMESTAMP"
    }

    SALDOS {
        bigint id PK
        bigint cuenta_id FK, UNIQUE
        decimal saldo_disponible "NUMERIC(15,2)"
        decimal saldo_contable "NUMERIC(15,2)"
        timestamp fecha_ultima_actualizacion "TIMESTAMP"
    }

    DATOS_SEGURIDAD_BIOMETRIA {
        bigint id PK
        string username "TEXT UNIQUE"
        string password_hash "TEXT"
        string tipo_biometria "TEXT"
        string hash_biometrico "TEXT"
        boolean loggeado "BOOLEAN"
        timestamp ultima_actividad "TIMESTAMP"
        timestamp fecha_registro_biometrico "TIMESTAMP"
    }

    CLIENTES }o--|| CAT_SEXOS : "clasificado_en"
    CLIENTES }o--|| CAT_NACIONALIDADES : "pertenece_a"
    CLIENTES }o--|| CAT_ESTADOS_CIVILES : "registra"
    CLIENTES }o--|| CAT_OCUPACIONES : "desempeña"
    DOMICILIOS }o--|| CAT_ESTADOS_REPUBLICA : "ubicado_en"
    DOMICILIOS }o--|| CAT_PAISES : "país_de_residencia"
    CLIENTES ||--|| DOMICILIOS : "posee 1:1"
    CLIENTES ||--|| CUENTAS : "posee 1:1"
    CLIENTES ||--|| DATOS_SEGURIDAD_BIOMETRIA : "asocia 1:1"
    CUENTAS ||--|| SALDOS : "mantiene 1:1"
```

---

## 4. Script DDL de Creación de Base de Datos (`schema.sql`)

```sql
-- =============================================================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS FINANCIAL ONBOARDING SYSTEM
-- NOTA: Todos los campos de texto utilizan tipo 'TEXT' (Sin uso de VARCHAR)
-- =============================================================================

DROP TABLE IF EXISTS saldos CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS domicilios CASCADE;
DROP TABLE IF EXISTS cuentas CASCADE;
DROP TABLE IF EXISTS datos_seguridad_biometria CASCADE;
DROP TABLE IF EXISTS cat_sexos CASCADE;
DROP TABLE IF EXISTS cat_estados_civiles CASCADE;
DROP TABLE IF EXISTS cat_nacionalidades CASCADE;
DROP TABLE IF EXISTS cat_ocupaciones CASCADE;
DROP TABLE IF EXISTS cat_estados_republica CASCADE;
DROP TABLE IF EXISTS cat_paises CASCADE;

-- 1. TABLAS DE CATÁLOGOS NORMALIZADOS
CREATE TABLE cat_sexos (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

CREATE TABLE cat_estados_civiles (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

CREATE TABLE cat_nacionalidades (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

CREATE TABLE cat_ocupaciones (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

CREATE TABLE cat_estados_republica (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

CREATE TABLE cat_paises (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

-- 2. TABLA DOMICILIOS
CREATE TABLE domicilios (
    id BIGSERIAL PRIMARY KEY,
    calle TEXT NOT NULL,
    numero_exterior TEXT NOT NULL,
    numero_interior TEXT,
    colonia TEXT NOT NULL,
    municipio TEXT NOT NULL,
    estado_id BIGINT NOT NULL,
    codigo_postal TEXT NOT NULL,
    pais_id BIGINT NOT NULL,
    CONSTRAINT chk_codigo_postal CHECK (codigo_postal ~ '^\d{5}$'),
    CONSTRAINT fk_domicilio_estado FOREIGN KEY (estado_id) REFERENCES cat_estados_republica(id),
    CONSTRAINT fk_domicilio_pais FOREIGN KEY (pais_id) REFERENCES cat_paises(id)
);

-- 3. TABLA CUENTAS
CREATE TABLE cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta TEXT NOT NULL UNIQUE,
    saldo DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    estatus TEXT NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_saldo_no_negativo CHECK (saldo >= 0),
    CONSTRAINT chk_estatus_valido CHECK (estatus IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA'))
);

-- 4. TABLA SALDOS
CREATE TABLE saldos (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL UNIQUE,
    saldo_disponible DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    saldo_contable DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    fecha_ultima_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saldos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE
);

-- 5. TABLA DATOS_SEGURIDAD_BIOMETRIA
CREATE TABLE datos_seguridad_biometria (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    tipo_biometria TEXT NOT NULL DEFAULT 'HUELLA_DACTILAR',
    hash_biometrico TEXT NOT NULL,
    loggeado BOOLEAN NOT NULL DEFAULT FALSE,
    ultima_actividad TIMESTAMP,
    fecha_registro_biometrico TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tipo_biometria CHECK (tipo_biometria IN ('HUELLA_DACTILAR', 'RECONOCIMIENTO_FACIAL', 'IRIS', 'PATRON_VASCULAR'))
);

-- 6. TABLA CLIENTES
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    segundo_nombre TEXT,
    apellido_paterno TEXT NOT NULL,
    apellido_materno TEXT NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    curp TEXT NOT NULL UNIQUE,
    rfc TEXT NOT NULL UNIQUE,
    sexo_id BIGINT NOT NULL,
    nacionalidad_id BIGINT NOT NULL,
    estado_civil_id BIGINT NOT NULL,
    correo TEXT NOT NULL UNIQUE,
    telefono_movil TEXT NOT NULL,
    telefono_alternativo TEXT,
    ocupacion_id BIGINT NOT NULL,
    empresa TEXT NOT NULL,
    ingreso_mensual DECIMAL(15, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    domicilio_id BIGINT NOT NULL UNIQUE,
    cuenta_id BIGINT NOT NULL UNIQUE,
    seguridad_id BIGINT UNIQUE,
    
    CONSTRAINT fk_cliente_sexo FOREIGN KEY (sexo_id) REFERENCES cat_sexos(id),
    CONSTRAINT fk_cliente_nacionalidad FOREIGN KEY (nacionalidad_id) REFERENCES cat_nacionalidades(id),
    CONSTRAINT fk_cliente_estado_civil FOREIGN KEY (estado_civil_id) REFERENCES cat_estados_civiles(id),
    CONSTRAINT fk_cliente_ocupacion FOREIGN KEY (ocupacion_id) REFERENCES cat_ocupaciones(id),
    CONSTRAINT fk_cliente_domicilio FOREIGN KEY (domicilio_id) REFERENCES domicilios(id) ON DELETE CASCADE,
    CONSTRAINT fk_cliente_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE,
    CONSTRAINT fk_cliente_seguridad FOREIGN KEY (seguridad_id) REFERENCES datos_seguridad_biometria(id) ON DELETE CASCADE,
    
    CONSTRAINT chk_curp_formato CHECK (curp ~ '^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$'),
    CONSTRAINT chk_rfc_formato CHECK (rfc ~ '^[A-Z&Ñ]{3,4}\d{6}[A-Z0-9]{3}$'),
    CONSTRAINT chk_telefono_movil CHECK (telefono_movil ~ '^\d{10}$'),
    CONSTRAINT chk_ingreso_positivo CHECK (ingreso_mensual > 0)
);

CREATE UNIQUE INDEX idx_clientes_curp ON clientes(curp);
CREATE UNIQUE INDEX idx_clientes_rfc ON clientes(rfc);
CREATE UNIQUE INDEX idx_clientes_correo ON clientes(correo);
CREATE UNIQUE INDEX idx_cuentas_numero ON cuentas(numero_cuenta);
```

---

## 5. Cifrado Biométrico y Temporizador de Inactividad (5 Segundos)

1. **Encriptación de Login (BCrypt)**: Las contraseñas se encriptan con algoritmos de hashing BCrypt adaptativos con salting automático.
2. **Cifrado Biométrico (SHA-256)**: Las plantillas de huella dactilar o rostro se encriptan mediante SHA-256.
3. **Bandera `loggeado` (true/false)**: Cuando un usuario realiza login exitoso (`POST /api/auth/login`), la bandera `loggeado` pasa a `true` y se guarda la hora de última actividad.
4. **Temporizador de Inactividad de 5 Segundos**: En cada consulta a los endpoints únicos del usuario (`POST /api/clientes/cuenta`, `POST /api/cuentas/saldo`, `POST /api/auth/estado`), se evalúa el tiempo transcurrido desde la última actividad. Si transcurren **más de 5 segundos**, la sesión cambia automáticamente a **`loggeado: false`** y el campo `estadoSesion` indica **EXPIRADA POR INACTIVIDAD (5s)**.

---

## 6. Endpoints API REST (100% Request Body - Cero PathVariables)

Todos los endpoints que reciben parámetros o identificadores consumen los datos exclusivamente mediante **`@RequestBody` (JSON)**. No se utilizan variables de ruta (`{curp}`, `{rfc}`, `{id}`, `{numeroCuenta}`, `{username}`) ni parámetros en la URL (`@PathVariable` / `@RequestParam`), permitiendo una interfaz homogénea y segura en Swagger/OpenAPI y clientes REST.

| Método | Endpoint | Request Body | Descripción | Estado HTTP |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/clientes` | `ClienteRegistrationRequest` | Registrar cliente persona física (no expone `id` en la respuesta) | `201 CREATED` |
| `GET` | `/api/clientes` | *(Ninguno)* | Consultar el listado completo de clientes | `200 OK` |
| `GET` | `/api/clientes/activos` | *(Ninguno)* | Consultar únicamente clientes en estatus activo | `200 OK` |
| `PUT` | `/api/clientes` | `ClienteUpdateRequest` | Actualizar cliente identificándolo por `curp` o `rfc` en el body | `200 OK` |
| `POST` | `/api/clientes/buscar` | `BuscarClienteRequest` | Buscar cliente por `curp`, `rfc`, `correo`, `numeroCuenta` o `id` | `200 OK` |
| `POST` | `/api/clientes/desactivar` | `ClienteIdentificadorRequest` | Baja lógica de cliente y cuenta por `curp` o `rfc` | `200 OK` |
| `DELETE` | `/api/clientes` | `ClienteIdentificadorRequest` | Baja lógica de cliente por `curp` o `rfc` en el body | `200 OK` |
| `POST` | `/api/clientes/reactivar` | `ClienteIdentificadorRequest` | Reactivar cliente y su cuenta bancaria por `curp` o `rfc` | `200 OK` |
| `POST` | `/api/clientes/validar` | `ValidarIdentificadorRequest` | Pre-validar sintaxis oficial y disponibilidad de `curp` o `rfc` | `200 OK` |
| `PATCH` | `/api/clientes/contacto` | `ActualizarContactoRequest` | Actualizar datos de contacto por `curp` o `rfc` (correo y teléfonos) | `200 OK` |
| `POST` | `/api/clientes/cuenta` | `ClienteIdentificadorRequest` | Consultar cuenta bancaria y saldo del titular por `curp` o `rfc` | `200 OK` |
| `POST` | `/api/clientes/rango-fechas` | `RangoFechasRequest` | Filtrar clientes por rango de fechas de registro en el body | `200 OK` |
| `GET` | `/api/cuentas/activas` | *(Ninguno)* | Consultar listado de cuentas bancarias activas | `200 OK` |
| `POST` | `/api/cuentas/detalle` | `CuentaConsultaRequest` | Consultar cuenta bancaria por número único en el body | `200 OK` |
| `POST` | `/api/cuentas/saldo` | `CuentaConsultaRequest` | Consultar saldos contable y disponible por número de cuenta en body | `200 OK` |
| `POST` | `/api/auth/login` | `LoginRequest` | Login seguro con contraseña BCrypt o biometría SHA-256 | `200 OK` |
| `POST` | `/api/auth/estado` | `UsuarioSesionRequest` | Consultar estado `loggeado: true/false` y temporizador de 5s por body | `200 OK` |
| `POST` | `/api/auth/logout` | `UsuarioSesionRequest` | Cerrar sesión manualmente (`loggeado: false`) por username en body | `200 OK` |
| `GET` | `/api/catalogos` | *(Ninguno)* | Consultar todos los catálogos estandarizados del sistema | `200 OK` |
| `GET` | `/api/catalogos/sexos` | *(Ninguno)* | Catálogo de Sexos / Géneros (MASCULINO, FEMENINO, OTRO) | `200 OK` |
| `GET` | `/api/catalogos/estados-civiles` | *(Ninguno)* | Catálogo de Estados Civiles (SOLTERO, CASADO, DIVORCIADO, etc.) | `200 OK` |
| `GET` | `/api/catalogos/nacionalidades` | *(Ninguno)* | Catálogo de Nacionalidades estandarizadas | `200 OK` |
| `GET` | `/api/catalogos/ocupaciones` | *(Ninguno)* | Catálogo de Actividades Laborales estandarizadas | `200 OK` |
| `GET` | `/api/catalogos/estados-republica` | *(Ninguno)* | Catálogo de las 32 Entidades Federativas de México | `200 OK` |
| `GET` | `/api/catalogos/paises` | *(Ninguno)* | Catálogo de Países para Domicilio | `200 OK` |
| `GET` | `/api/catalogos/tipos-biometria` | *(Ninguno)* | Catálogo de Modalidades Biométricas | `200 OK` |
| `GET` | `/api/catalogos/estatus-cuenta` | *(Ninguno)* | Catálogo de Estados de Cuenta Bancaria | `200 OK` |

---

## 7. Catálogos Estandarizados (Control de Variación de Datos)

Para evitar la disparidad y variabilidad arbitraria en la captura manual de datos (como diferencias ortográficas, mayúsculas/minúsculas, abreviaturas no oficiales o errores tipográficos), se incorporó una capa de **Catálogos Estandarizados**:

1. **Sexo / Género (`SexoCatalogo`)**:
   - `MASCULINO`, `FEMENINO`, `OTRO`.
   - Garantiza consistencia directa con el cálculo de CURP en México (`H` / `M`).

2. **Estado Civil (`EstadoCivilCatalogo`)**:
   - `SOLTERO`, `CASADO`, `DIVORCIADO`, `VIUDO`, `UNION_LIBRE`.

3. **Nacionalidad (`NacionalidadCatalogo`)**:
   - Estandariza la nacionalidad evitando variaciones como `mexicana`, `MEX`, `mexicano` o `México`.

4. **Entidades Federativas de México (`EstadoRepublicaCatalogo`)**:
   - Comprende los 32 estados oficiales de la República Mexicana (Aguascalientes hasta Zacatecas), previniendo abreviaturas conflictivas como `CDMX`, `D.F.`, `Edomex`, `NL`, etc.

5. **Países (`PaisCatalogo`)**:
   - Estandarización de país de residencia fiscal / domicilio con valor por defecto `México`.

6. **Ocupación / Giro Laboral (`OcupacionCatalogo`)**:
   - Homologa las ocupaciones (`Empleado Sector Privado`, `Servidor Público`, `Profesionista Independiente`, `Desarrollador TI`, `Empresario`, `Comerciante`, `Estudiante`, `Jubilado/Pensionado`, `Hogar`, `Otro`).

7. **Sincronización Dinámica Frontend-Backend**:
   - La interfaz Web consume dinámicamente `/api/catalogos` poblando los elementos `<select>` en el registro y en la edición.
   - La capa de servicio (`CatalogoService`) normaliza y valida los valores recibidos garantizando integridad referencial.

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
    CLIENTES {
        bigint id PK
        string nombre "TEXT"
        string segundo_nombre "TEXT"
        string apellido_paterno "TEXT"
        string apellido_materno "TEXT"
        date fecha_nacimiento "DATE"
        string curp "TEXT UNIQUE"
        string rfc "TEXT UNIQUE"
        string sexo "TEXT"
        string nacionalidad "TEXT"
        string estado_civil "TEXT"
        string correo "TEXT UNIQUE"
        string telefono_movil "TEXT"
        string telefono_alternativo "TEXT"
        string ocupacion "TEXT"
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
        string estado "TEXT"
        string codigo_postal "TEXT"
        string pais "TEXT"
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

-- 1. TABLA DOMICILIOS (Tipo TEXT)
CREATE TABLE domicilios (
    id BIGSERIAL PRIMARY KEY,
    calle TEXT NOT NULL,
    numero_exterior TEXT NOT NULL,
    numero_interior TEXT,
    colonia TEXT NOT NULL,
    municipio TEXT NOT NULL,
    estado TEXT NOT NULL,
    codigo_postal TEXT NOT NULL,
    pais TEXT NOT NULL DEFAULT 'México',
    CONSTRAINT chk_codigo_postal CHECK (codigo_postal ~ '^\d{5}$')
);

-- 2. TABLA CUENTAS (Tipo TEXT)
CREATE TABLE cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta TEXT NOT NULL UNIQUE,
    saldo DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    estatus TEXT NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_saldo_no_negativo CHECK (saldo >= 0),
    CONSTRAINT chk_estatus_valido CHECK (estatus IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA'))
);

-- 3. TABLA SALDOS (Dedicada)
CREATE TABLE saldos (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL UNIQUE,
    saldo_disponible DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    saldo_contable DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    fecha_ultima_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saldos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE
);

-- 4. TABLA DATOS_SEGURIDAD_BIOMETRIA (Login Cifrado, Biometría, loggeado TRUE/FALSE)
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

-- 5. TABLA CLIENTES (Tipo TEXT)
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre TEXT NOT NULL,
    segundo_nombre TEXT,
    apellido_paterno TEXT NOT NULL,
    apellido_materno TEXT NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    curp TEXT NOT NULL UNIQUE,
    rfc TEXT NOT NULL UNIQUE,
    sexo TEXT NOT NULL,
    nacionalidad TEXT NOT NULL DEFAULT 'Mexicana',
    estado_civil TEXT NOT NULL,
    correo TEXT NOT NULL UNIQUE,
    telefono_movil TEXT NOT NULL,
    telefono_alternativo TEXT,
    ocupacion TEXT NOT NULL,
    empresa TEXT NOT NULL,
    ingreso_mensual DECIMAL(15, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    domicilio_id BIGINT NOT NULL UNIQUE,
    cuenta_id BIGINT NOT NULL UNIQUE,
    seguridad_id BIGINT UNIQUE,
    
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
4. **Temporizador de Inactividad de 5 Segundos**: En cada consulta a los endpoints únicos del usuario (`GET /api/clientes/{id}`, `GET /api/cuentas/{numeroCuenta}/saldo`, `GET /api/auth/estado/{username}`), se evalúa el tiempo transcurrido desde la última actividad. Si transcurren **más de 5 segundos**, la sesión cambia automáticamente a **`loggeado: false`** y el campo `estadoSesion` indica **EXPIRADA POR INACTIVIDAD (5s)**.

---

## 6. Endpoints API REST

| Método | Endpoint | Descripción | Estado HTTP |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/clientes` | Registrar cliente con datos biométricos, tabla de saldos y bandera `loggeado` | `201 CREATED` |
| `GET` | `/api/clientes` | Consultar el listado completo de clientes | `200 OK` |
| `GET` | `/api/clientes/{id}` | Consultar cliente por ID (incluye estado de sesión `loggeado: true/false`) | `200 OK` |
| `GET` | `/api/cuentas/{numeroCuenta}/saldo`| Consultar saldos contable y disponible de la tabla `saldos` | `200 OK` |
| `POST` | `/api/auth/login` | Login con contraseña BCrypt o biometría SHA-256 (`loggeado: true`) | `200 OK` |
| `GET` | `/api/auth/estado/{username}` | Consultar estado `loggeado: true/false` y temporizador de 5s | `200 OK` |
| `POST` | `/api/auth/logout/{username}` | Cerrar sesión manualmente (`loggeado: false`) | `200 OK` |

-- =============================================================================
-- SCRIPT DE CREACIÓN DE BASE DE DATOS FINANCIAL ONBOARDING SYSTEM
-- =============================================================================

DROP TABLE IF EXISTS saldos CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS domicilios CASCADE;
DROP TABLE IF EXISTS cuentas CASCADE;
DROP TABLE IF EXISTS datos_seguridad_biometria CASCADE;

-- 1. TABLA DOMICILIOS 
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

-- 2. TABLA CUENTAS 
CREATE TABLE cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta TEXT NOT NULL UNIQUE,
    saldo DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    estatus TEXT NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_saldo_no_negativo CHECK (saldo >= 0),
    CONSTRAINT chk_estatus_valido CHECK (estatus IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA'))
);

-- 3. TABLA SALDOS 
CREATE TABLE saldos (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL UNIQUE,
    saldo_disponible DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    saldo_contable DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    fecha_ultima_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saldos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE
);

-- 4. TABLA DATOS_SEGURIDAD_BIOMETRIA 
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

-- 5. TABLA CLIENTES 
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
    
    -- Relaciones (Foreign Keys)
    CONSTRAINT fk_cliente_domicilio FOREIGN KEY (domicilio_id) REFERENCES domicilios(id) ON DELETE CASCADE,
    CONSTRAINT fk_cliente_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE,
    CONSTRAINT fk_cliente_seguridad FOREIGN KEY (seguridad_id) REFERENCES datos_seguridad_biometria(id) ON DELETE CASCADE,
    
    -- Restricciones de Negocio
    CONSTRAINT chk_curp_formato CHECK (curp ~ '^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$'),
    CONSTRAINT chk_rfc_formato CHECK (rfc ~ '^[A-Z&Ñ]{3,4}\d{6}[A-Z0-9]{3}$'),
    CONSTRAINT chk_telefono_movil CHECK (telefono_movil ~ '^\d{10}$'),
    CONSTRAINT chk_ingreso_positivo CHECK (ingreso_mensual > 0)
);

-- ÍNDICES DE ALTO RENDIMIENTO
CREATE UNIQUE INDEX idx_clientes_curp ON clientes(curp);
CREATE UNIQUE INDEX idx_clientes_rfc ON clientes(rfc);
CREATE UNIQUE INDEX idx_clientes_correo ON clientes(correo);
CREATE UNIQUE INDEX idx_cuentas_numero ON cuentas(numero_cuenta);
CREATE INDEX idx_clientes_activo ON clientes(activo);
CREATE INDEX idx_cuentas_estatus ON cuentas(estatus);
CREATE INDEX idx_clientes_fecha_registro ON clientes(fecha_registro);

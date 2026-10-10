-- =============================================================================
-- SCRIPT DE BASE DE DATOS: FINANCIAL ONBOARDING SYSTEM (NORMALIZADO)
-- PostgreSQL 15+ / 16+ / 17+ / 18+
-- REGLAS ARQUITECTURALES:
--  - Sin tipos VARCHAR, uso exclusivo de TEXT.
--  - Tablas de catálogo normalizadas con llaves foráneas íntegras.
--  - Datos semilla precargados para catálogos oficiales.
-- =============================================================================

-- Descomentar si se ejecuta desde cero para crear la base:
-- CREATE DATABASE onboarding;
-- \c onboarding;

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

-- =============================================================================
-- 1. TABLAS DE CATÁLOGOS NORMALIZADOS
-- =============================================================================

-- Catálogo de Sexos
CREATE TABLE cat_sexos (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

-- Catálogo de Estados Civiles
CREATE TABLE cat_estados_civiles (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

-- Catálogo de Nacionalidades
CREATE TABLE cat_nacionalidades (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

-- Catálogo de Ocupaciones
CREATE TABLE cat_ocupaciones (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

-- Catálogo de Estados de la República Mexicana
CREATE TABLE cat_estados_republica (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

-- Catálogo de Países
CREATE TABLE cat_paises (
    id BIGSERIAL PRIMARY KEY,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL
);

-- =============================================================================
-- 2. TABLAS PRINCIPALES DEL SISTEMA
-- =============================================================================

-- 1. TABLA DOMICILIOS
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

    -- Relaciones (Foreign Keys)
    CONSTRAINT fk_cliente_sexo FOREIGN KEY (sexo_id) REFERENCES cat_sexos(id),
    CONSTRAINT fk_cliente_nacionalidad FOREIGN KEY (nacionalidad_id) REFERENCES cat_nacionalidades(id),
    CONSTRAINT fk_cliente_estado_civil FOREIGN KEY (estado_civil_id) REFERENCES cat_estados_civiles(id),
    CONSTRAINT fk_cliente_ocupacion FOREIGN KEY (ocupacion_id) REFERENCES cat_ocupaciones(id),
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
CREATE INDEX idx_clientes_sexo ON clientes(sexo_id);
CREATE INDEX idx_clientes_nacionalidad ON clientes(nacionalidad_id);
CREATE INDEX idx_clientes_estado_civil ON clientes(estado_civil_id);
CREATE INDEX idx_clientes_ocupacion ON clientes(ocupacion_id);
CREATE INDEX idx_domicilios_estado ON domicilios(estado_id);
CREATE INDEX idx_domicilios_pais ON domicilios(pais_id);

-- =============================================================================
-- 3. INSERTS SEMILLA PARA CATÁLOGOS NORMALIZADOS
-- =============================================================================

-- Catálogo de Sexos
INSERT INTO cat_sexos (codigo, descripcion) VALUES
('MASCULINO', 'Masculino'),
('FEMENINO', 'Femenino'),
('OTRO', 'Otro / No binario')
ON CONFLICT (codigo) DO NOTHING;

-- Catálogo de Estados Civiles
INSERT INTO cat_estados_civiles (codigo, descripcion) VALUES
('SOLTERO', 'Soltero / a'),
('CASADO', 'Casado / a'),
('DIVORCIADO', 'Divorciado / a'),
('VIUDO', 'Viudo / a'),
('UNION_LIBRE', 'Unión Libre')
ON CONFLICT (codigo) DO NOTHING;

-- Catálogo de Nacionalidades
INSERT INTO cat_nacionalidades (codigo, descripcion) VALUES
('MEXICANA', 'Mexicana'),
('ESTADOUNIDENSE', 'Estadounidense'),
('CANADIENSE', 'Canadiense'),
('ESPANYOLA', 'Española'),
('COLOMBIANA', 'Colombiana'),
('ARGENTINA', 'Argentina'),
('VENEZOLANA', 'Venezolana'),
('PERUANA', 'Peruana'),
('CHILENA', 'Chilena'),
('OTRA', 'Otra nacionalidad')
ON CONFLICT (codigo) DO NOTHING;

-- Catálogo de Ocupaciones
INSERT INTO cat_ocupaciones (codigo, descripcion) VALUES
('EMPLEADO_SECTOR_PRIVADO', 'Empleado Sector Privado'),
('EMPLEADO_SECTOR_PUBLICO', 'Servidor Público / Sector Público'),
('PROFESIONISTA_INDEPENDIENTE', 'Profesionista Independiente / Honorarios'),
('DESARROLLADOR_TI', 'Desarrollador / Tecnologías de la Información'),
('EMPRESARIO', 'Empresario / Dueño de Negocio'),
('COMERCIANTE', 'Comerciante'),
('ESTUDIANTE', 'Estudiante'),
('JUBILADO_PENSIONADO', 'Jubilado / Pensionado'),
('DEDICADO_AL_HOGAR', 'Dedicado(a) al Hogar'),
('OTRO', 'Otro oficio o profesión')
ON CONFLICT (codigo) DO NOTHING;

-- Catálogo de Estados de la República Mexicana
INSERT INTO cat_estados_republica (codigo, descripcion) VALUES
('AGUASCALIENTES', 'Aguascalientes'),
('BAJA_CALIFORNIA', 'Baja California'),
('BAJA_CALIFORNIA_SUR', 'Baja California Sur'),
('CAMPECHE', 'Campeche'),
('CHIAPAS', 'Chiapas'),
('CHIHUAHUA', 'Chihuahua'),
('CIUDAD_DE_MEXICO', 'Ciudad de México'),
('COAHUILA', 'Coahuila'),
('COLIMA', 'Colima'),
('DURANGO', 'Durango'),
('ESTADO_DE_MEXICO', 'Estado de México'),
('GUANAJUATO', 'Guanajuato'),
('GUERRERO', 'Guerrero'),
('HIDALGO', 'Hidalgo'),
('JALISCO', 'Jalisco'),
('MICHOACAN', 'Michoacán'),
('MORELOS', 'Morelos'),
('NAYARIT', 'Nayarit'),
('NUEVO_LEON', 'Nuevo León'),
('OAXACA', 'Oaxaca'),
('PUEBLA', 'Puebla'),
('QUERETARO', 'Querétaro'),
('QUINTANA_ROO', 'Quintana Roo'),
('SAN_LUIS_POTOSI', 'San Luis Potosí'),
('SINALOA', 'Sinaloa'),
('SONORA', 'Sonora'),
('TABASCO', 'Tabasco'),
('TAMAULIPAS', 'Tamaulipas'),
('TLAXCALA', 'Tlaxcala'),
('VERACRUZ', 'Veracruz'),
('YUCATAN', 'Yucatán'),
('ZACATECAS', 'Zacatecas')
ON CONFLICT (codigo) DO NOTHING;

-- Catálogo de Países
INSERT INTO cat_paises (codigo, descripcion) VALUES
('MEXICO', 'México'),
('ESTADOS_UNIDOS', 'Estados Unidos'),
('CANADA', 'Canadá'),
('ESPANYA', 'España'),
('COLOMBIA', 'Colombia'),
('ARGENTINA', 'Argentina'),
('OTRO', 'Otro país')
ON CONFLICT (codigo) DO NOTHING;

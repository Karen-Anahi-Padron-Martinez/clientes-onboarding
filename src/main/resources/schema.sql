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


-- 1. TABLA DOMICILIOS (Relacionada con cat_estados_republica y cat_paises)
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

-- 5. TABLA CLIENTES (Relacionada con catálogos y entidades satélite)
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

-- 6. TABLA CATALOGO_PRODUCTOS_PUNTORED
CREATE TABLE IF NOT EXISTS catalogo_productos_puntored (
    id BIGSERIAL PRIMARY KEY,
    id_producto INT NOT NULL UNIQUE,
    id_servicio INT NOT NULL,
    nombre_producto TEXT NOT NULL,
    nombre_servicio TEXT NOT NULL,
    id_cat_tipo_servicio INT,
    tipo_front INT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_cat_prod_pr_id_prod ON catalogo_productos_puntored(id_producto);

-- 7. TABLA PAGOS_SERVICIOS (Aislamiento de identificadores internos bancarios vs PuntoRed)
CREATE TABLE IF NOT EXISTS pagos_servicios (
    id BIGSERIAL PRIMARY KEY,
    upc TEXT NOT NULL UNIQUE,
    idempotency_key TEXT UNIQUE,
    curp TEXT,
    rfc TEXT,
    numero_cuenta TEXT,
    id_user TEXT,
    id_producto INT NOT NULL,
    id_servicio INT NOT NULL,
    telefono TEXT NOT NULL,
    referencia TEXT,
    monto DECIMAL(15, 2) NOT NULL,
    comision DECIMAL(15, 2),
    hora_local TEXT NOT NULL,
    unidad TEXT NOT NULL,
    estatus TEXT NOT NULL DEFAULT 'PENDIENTE',
    id_tx_proveedor TEXT,
    num_autorizacion TEXT,
    codigo_respuesta TEXT,
    texto_respuesta TEXT,
    intentos_verificacion INT DEFAULT 0,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP
);
ALTER TABLE pagos_servicios ADD COLUMN IF NOT EXISTS intentos_verificacion INT DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS idx_pagos_servicios_upc ON pagos_servicios(upc);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pagos_servicios_idempotency ON pagos_servicios(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_pagos_servicios_curp ON pagos_servicios(curp);
CREATE INDEX IF NOT EXISTS idx_pagos_servicios_cuenta ON pagos_servicios(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_pagos_servicios_estatus ON pagos_servicios(estatus);

-- 8. TABLA CODIGOS_VERIFICACION_PAGO (2FA para operaciones de pago)
CREATE TABLE IF NOT EXISTS codigos_verificacion_pago (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT REFERENCES clientes(id) ON DELETE CASCADE,
    numero_cuenta TEXT NOT NULL,
    curp TEXT,
    hash_codigo TEXT NOT NULL,
    hash_operacion TEXT NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_codigos_verif_cuenta ON codigos_verificacion_pago(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_codigos_verif_hash_op ON codigos_verificacion_pago(hash_operacion);
CREATE INDEX IF NOT EXISTS idx_codigos_verif_creado ON codigos_verificacion_pago(creado_en);
CREATE INDEX IF NOT EXISTS idx_codigos_verif_usado ON codigos_verificacion_pago(usado);



-- 9. TABLA CONSULTAS_PROVEEDOR_DIARIAS (cupo diario de getProductList.do: 3 llamadas por día)
CREATE TABLE IF NOT EXISTS consultas_proveedor_diarias (
    id BIGSERIAL PRIMARY KEY,
    operacion TEXT NOT NULL,
    fecha DATE NOT NULL,
    consultas INT NOT NULL DEFAULT 0,
    ultima_exitosa TIMESTAMP,
    CONSTRAINT uk_consulta_proveedor_operacion_fecha UNIQUE (operacion, fecha)
);

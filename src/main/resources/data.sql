-- =============================================================================
-- INSERTS SEMILLA PARA CATÁLOGOS NORMALIZADOS
-- =============================================================================

-- 1. Catálogo de Sexos
INSERT INTO cat_sexos (codigo, descripcion) VALUES
('MASCULINO', 'Masculino'),
('FEMENINO', 'Femenino'),
('OTRO', 'Otro / No binario')
ON CONFLICT (codigo) DO NOTHING;

-- 2. Catálogo de Estados Civiles
INSERT INTO cat_estados_civiles (codigo, descripcion) VALUES
('SOLTERO', 'Soltero / a'),
('CASADO', 'Casado / a'),
('DIVORCIADO', 'Divorciado / a'),
('VIUDO', 'Viudo / a'),
('UNION_LIBRE', 'Unión Libre')
ON CONFLICT (codigo) DO NOTHING;

-- 3. Catálogo de Nacionalidades
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

-- 4. Catálogo de Ocupaciones
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

-- 5. Catálogo de Estados de la República Mexicana
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

-- 6. Catálogo de Países
INSERT INTO cat_paises (codigo, descripcion) VALUES
('MEXICO', 'México'),
('ESTADOS_UNIDOS', 'Estados Unidos'),
('CANADA', 'Canadá'),
('ESPANYA', 'España'),
('COLOMBIA', 'Colombia'),
('ARGENTINA', 'Argentina'),
('OTRO', 'Otro país')
ON CONFLICT (codigo) DO NOTHING;

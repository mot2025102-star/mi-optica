-- ═══════════════════════════════════════════════════════════════
--  Mi Óptica — Base de Datos para Spring Boot
--  MySQL 8.0+ / XAMPP
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS mi_optica
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE mi_optica;

-- ─── ROLES ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id_rol  INT          AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(50)  NOT NULL UNIQUE
);

INSERT IGNORE INTO roles (id_rol, nombre) VALUES
(1, 'Administrador'),
(2, 'Vendedor'),
(3, 'Bodeguero'),
(4, 'Optometrista'),
(5, 'Contador');

-- ─── SUCURSALES ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sucursales (
    id_sucursal INT          AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    direccion   VARCHAR(200),
    telefono    VARCHAR(20),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT IGNORE INTO sucursales (id_sucursal, nombre, direccion) VALUES
(1, 'Zona 1 Central', '6a Avenida 12-34, Zona 1, Guatemala'),
(2, 'Zona 18',        '15 Calle 25-10, Zona 18, Guatemala');

-- ─── USUARIOS ─────────────────────────────────────────────────────
-- password_hash es BCrypt de "admin123" — cámbialo en producción
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario      INT           AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100)  NOT NULL,
    username        VARCHAR(50)   NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    id_rol          INT           NOT NULL,
    id_sucursal     INT,
    puesto          VARCHAR(100),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    FOREIGN KEY (id_rol)      REFERENCES roles(id_rol),
    FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal)
);

-- Usuario admin por defecto: admin / admin123
INSERT IGNORE INTO usuarios (nombre_completo, username, password_hash, id_rol, id_sucursal, puesto) VALUES
('Administrador Sistema', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8RIZe.S1Gk7tNgCgeC', 1, 1, 'Administrador General');

-- ─── CATEGORÍAS ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categorias (
    id_categoria INT         AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(80) NOT NULL UNIQUE
);

INSERT IGNORE INTO categorias (nombre) VALUES
('Lentes oftálmicos'),
('Armazones'),
('Lentes de contacto'),
('Soluciones y accesorios'),
('Lentes de sol'),
('Insumos');

-- ─── MARCAS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS marcas (
    id_marca INT         AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(80) NOT NULL UNIQUE
);

INSERT IGNORE INTO marcas (nombre) VALUES
('Essilor'), ('Hoya'), ('Zeiss'), ('Transitions'),
('Ray-Ban'), ('Oakley'), ('Acuvue'), ('Bausch+Lomb'),
('Alcon'), ('Sin marca');

-- ─── PRODUCTOS ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS productos (
    id_producto   INT          AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(50)  NOT NULL UNIQUE,
    detalle       VARCHAR(200) NOT NULL,
    id_categoria  INT,
    id_marca      INT,
    descripcion   TEXT,
    unidad_medida VARCHAR(30)  DEFAULT 'Unidad',
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria),
    FOREIGN KEY (id_marca)     REFERENCES marcas(id_marca)
);



-- ─── INVENTARIO ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventario (
    id_inventario INT            AUTO_INCREMENT PRIMARY KEY,
    id_producto   INT            NOT NULL,
    id_sucursal   INT            NOT NULL,
    existencia    DECIMAL(10,2)  NOT NULL DEFAULT 0,
    costo         DECIMAL(10,2)  DEFAULT 0,
    precio_venta  DECIMAL(10,2)  DEFAULT 0,
    UNIQUE KEY uq_prod_suc (id_producto, id_sucursal),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal)
);

-- ─── KARDEX ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS kardex (
    id_kardex          INT           AUTO_INCREMENT PRIMARY KEY,
    id_producto        INT           NOT NULL,
    id_sucursal        INT           NOT NULL,
    tipo_movimiento    VARCHAR(30)   NOT NULL COMMENT 'Entrada|Salida|Traslado_Entrada|Traslado_Salida|Ajuste',
    referencia         VARCHAR(50),
    fecha              DATE          NOT NULL,
    cantidad           DECIMAL(10,2) NOT NULL,
    precio_unitario    DECIMAL(10,2) DEFAULT 0,
    existencia_anterior DECIMAL(10,2) DEFAULT 0,
    existencia_nueva    DECIMAL(10,2) DEFAULT 0,
    observacion        VARCHAR(200),
    id_usuario         INT,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario)
);

-- ─── CLIENTES ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS clientes (
    id_cliente       INT           AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100)  NOT NULL,
    dpi              VARCHAR(20)   UNIQUE,
    nit              VARCHAR(20)   DEFAULT 'CF',
    telefono         VARCHAR(20),
    telefono2        VARCHAR(20),
    correo           VARCHAR(100),
    fecha_nacimiento DATE,
    sexo             VARCHAR(20),
    ocupacion        VARCHAR(100),
    direccion        VARCHAR(200),
    zona             VARCHAR(10),
    municipio        VARCHAR(80),
    notas            TEXT,
    activo           BOOLEAN       NOT NULL DEFAULT TRUE
);

-- ─── FICHAS CLÍNICAS ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS fichas_clinicas (
    id_ficha          INT            AUTO_INCREMENT PRIMARY KEY,
    id_cliente        INT            NOT NULL,
    id_sucursal       INT            NOT NULL,
    id_optometrista   INT            NOT NULL,
    fecha             DATE           NOT NULL,
    motivo_consulta   TEXT,
    -- Graduación
    od_esfera         DECIMAL(5,2), od_cilindro DECIMAL(5,2), od_eje INT, od_adicion DECIMAL(5,2),
    oi_esfera         DECIMAL(5,2), oi_cilindro DECIMAL(5,2), oi_eje INT, oi_adicion DECIMAL(5,2),
    av_od_sc         VARCHAR(20),  av_oi_sc  VARCHAR(20),
    av_od_cc         VARCHAR(20),  av_oi_cc  VARCHAR(20),
    -- Pedido
    armazon           VARCHAR(200),
    detalle_lentes    TEXT,
    total             DECIMAL(10,2) DEFAULT 0,
    abono             DECIMAL(10,2) DEFAULT 0,
    saldo             DECIMAL(10,2) DEFAULT 0,
    fecha_entrega     DATE,
    fecha_entrega_real DATE,
    estado_entrega    VARCHAR(30)   DEFAULT 'Pendiente',
    observaciones     TEXT,
    FOREIGN KEY (id_cliente)      REFERENCES clientes(id_cliente),
    FOREIGN KEY (id_sucursal)     REFERENCES sucursales(id_sucursal),
    FOREIGN KEY (id_optometrista) REFERENCES usuarios(id_usuario)
);

-- ─── PROVEEDORES ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS proveedores (
    id_proveedor     INT          AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    empresa          VARCHAR(100),
    contacto         VARCHAR(100),
    telefono         VARCHAR(20),
    telefono2        VARCHAR(20),
    correo           VARCHAR(100),
    nit              VARCHAR(20),
    pais             VARCHAR(50)  DEFAULT 'Guatemala',
    direccion        VARCHAR(200),
    condicion_pago   VARCHAR(30)  DEFAULT 'Contado',
    dias_credito     INT          DEFAULT 0,
    notas            TEXT,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ─── VENTAS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ventas (
    id_venta        INT            AUTO_INCREMENT PRIMARY KEY,
    id_sucursal     INT            NOT NULL,
    id_usuario      INT            NOT NULL,
    id_cliente      INT,
    numero_factura  VARCHAR(20)    UNIQUE,
    fecha           DATE           NOT NULL,
    subtotal        DECIMAL(10,2)  DEFAULT 0,
    descuento       DECIMAL(10,2)  DEFAULT 0,
    total           DECIMAL(10,2)  DEFAULT 0,
    estado          VARCHAR(20)    DEFAULT 'Pagada',
    observacion     TEXT,
    FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_cliente)  REFERENCES clientes(id_cliente)
);

CREATE TABLE IF NOT EXISTS detalle_ventas (
    id              INT            AUTO_INCREMENT PRIMARY KEY,
    id_venta        INT            NOT NULL,
    id_producto     INT            NOT NULL,
    cantidad        DECIMAL(10,2)  NOT NULL,
    precio_unitario DECIMAL(10,2)  NOT NULL,
    descuento       DECIMAL(10,2)  DEFAULT 0,
    subtotal        DECIMAL(10,2)  NOT NULL,
    FOREIGN KEY (id_venta)    REFERENCES ventas(id_venta),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- ─── RECIBOS DE CAJA ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS recibos_caja (
    id_recibo      INT            AUTO_INCREMENT PRIMARY KEY,
    id_sucursal    INT            NOT NULL,
    id_usuario     INT            NOT NULL,
    id_cliente     INT,
    numero_recibo  VARCHAR(20)    UNIQUE,
    fecha          DATE           NOT NULL,
    monto          DECIMAL(10,2)  NOT NULL,
    forma_pago     VARCHAR(30)    DEFAULT 'Contado',
    concepto       VARCHAR(200),
    id_venta       INT,
    FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_cliente)  REFERENCES clientes(id_cliente),
    FOREIGN KEY (id_venta)    REFERENCES ventas(id_venta)
);

-- ─── TRASLADOS ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS traslados (
    id_traslado      INT         AUTO_INCREMENT PRIMARY KEY,
    numero_registro  VARCHAR(20) UNIQUE,
    id_sucursal_orig INT         NOT NULL,
    id_sucursal_dest INT         NOT NULL,
    id_usuario       INT         NOT NULL,
    fecha            DATE        NOT NULL,
    nota             TEXT,
    estado           VARCHAR(20) DEFAULT 'Pendiente',
    FOREIGN KEY (id_sucursal_orig) REFERENCES sucursales(id_sucursal),
    FOREIGN KEY (id_sucursal_dest) REFERENCES sucursales(id_sucursal),
    FOREIGN KEY (id_usuario)       REFERENCES usuarios(id_usuario)
);

CREATE TABLE IF NOT EXISTS detalle_traslados (
    id          INT            AUTO_INCREMENT PRIMARY KEY,
    id_traslado INT            NOT NULL,
    id_producto INT            NOT NULL,
    cantidad    DECIMAL(10,2)  NOT NULL,
    FOREIGN KEY (id_traslado) REFERENCES traslados(id_traslado),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- ─── INGRESOS DE MERCADERÍA ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS ingresos_mercaderia (
    id_ingreso         INT            AUTO_INCREMENT PRIMARY KEY,
    id_sucursal        INT            NOT NULL,
    id_usuario         INT            NOT NULL,
    id_proveedor       INT,
    numero_ingreso     VARCHAR(20)    UNIQUE,
    factura_proveedor  VARCHAR(50),
    fecha              DATE           NOT NULL,
    total              DECIMAL(10,2)  DEFAULT 0,
    nota               TEXT,
    FOREIGN KEY (id_sucursal)  REFERENCES sucursales(id_sucursal),
    FOREIGN KEY (id_usuario)   REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor)
);

CREATE TABLE IF NOT EXISTS detalle_ingresos (
    id           INT            AUTO_INCREMENT PRIMARY KEY,
    id_ingreso   INT            NOT NULL,
    id_producto  INT            NOT NULL,
    cantidad     DECIMAL(10,2)  NOT NULL,
    costo_unitario DECIMAL(10,2) NOT NULL,
    precio_venta   DECIMAL(10,2) DEFAULT 0,
    subtotal       DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_ingreso)  REFERENCES ingresos_mercaderia(id_ingreso),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- ─── COTIZACIONES ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cotizaciones (
    id_cotizacion      INT            AUTO_INCREMENT PRIMARY KEY,
    id_sucursal        INT            NOT NULL,
    id_usuario         INT            NOT NULL,
    id_cliente         INT,
    numero_cotizacion  VARCHAR(20)    UNIQUE,
    fecha              DATE           NOT NULL,
    fecha_vencimiento  DATE,
    subtotal           DECIMAL(10,2)  DEFAULT 0,
    descuento          DECIMAL(10,2)  DEFAULT 0,
    total              DECIMAL(10,2)  DEFAULT 0,
    nota               TEXT,
    estado             VARCHAR(20)    DEFAULT 'Pendiente',
    FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    FOREIGN KEY (id_usuario)  REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_cliente)  REFERENCES clientes(id_cliente)
);

CREATE TABLE IF NOT EXISTS detalle_cotizaciones (
    id              INT            AUTO_INCREMENT PRIMARY KEY,
    id_cotizacion   INT            NOT NULL,
    id_producto     INT            NOT NULL,
    cantidad        DECIMAL(10,2)  NOT NULL,
    precio_unitario DECIMAL(10,2)  NOT NULL,
    descuento       DECIMAL(10,2)  DEFAULT 0,
    subtotal        DECIMAL(10,2)  NOT NULL,
    FOREIGN KEY (id_cotizacion) REFERENCES cotizaciones(id_cotizacion),
    FOREIGN KEY (id_producto)   REFERENCES productos(id_producto)
);

-- ─── CORRELATIVOS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS correlativos (
    id            INT         AUTO_INCREMENT PRIMARY KEY,
    id_sucursal   INT         NOT NULL,
    tipo          VARCHAR(30) NOT NULL COMMENT 'Factura|Recibo|Ingreso|Traslado|Cotizacion',
    valor_actual  INT         NOT NULL DEFAULT 0,
    UNIQUE KEY uq_suc_tipo (id_sucursal, tipo),
    FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal)
);
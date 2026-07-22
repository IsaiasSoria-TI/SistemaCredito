CREATE DATABASE IF NOT EXISTS sistemagarantia
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE sistemagarantia;

CREATE TABLE tb_persona (
    id_persona INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(70) NOT NULL,
    apellido_paterno VARCHAR(50) NULL,
    apellido_materno VARCHAR(50) NULL,
    dni VARCHAR(8) NULL,
    telefono VARCHAR(9) NULL,
    PRIMARY KEY (id_persona)
);

CREATE TABLE tb_rol (
    id_rol INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    es_administrador TINYINT NOT NULL DEFAULT 0,
    flgactivo TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id_rol),
    UNIQUE KEY uq_rol_nombre (nombre)
);

CREATE TABLE tb_usuario (
    id_usuario INT NOT NULL AUTO_INCREMENT,
    id_persona INT NOT NULL,
    id_rol INT NOT NULL,
    usuario VARCHAR(50) NOT NULL,
    contrasena VARCHAR(100) NOT NULL,
    color_perfil VARCHAR(7) NOT NULL DEFAULT '#116A72',
    flgactivo TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uq_usuario_nombre (usuario),
    KEY idx_usuario_persona (id_persona),
    KEY idx_usuario_rol (id_rol),
    CONSTRAINT fk_usuario_persona
        FOREIGN KEY (id_persona) REFERENCES tb_persona (id_persona),
    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol) REFERENCES tb_rol (id_rol)
);

CREATE TABLE tb_cliente (
    id_cliente INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    apellido_paterno VARCHAR(50) NULL,
    apellido_materno VARCHAR(50) NULL,
    telefono VARCHAR(9) NULL,
    flgactivo TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id_cliente)
);

CREATE TABLE tb_producto (
    id_producto INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    flgactivo TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id_producto)
);

CREATE TABLE tb_garantia (
    id_garantia INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_producto INT NOT NULL,
    id_cliente INT NOT NULL,
    fecha_inicio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cantidad_envases INT NOT NULL,
    cantidad_devuelta INT NOT NULL DEFAULT 0,
    monto_garantia_unitario DECIMAL(8,2) NOT NULL,
    fecha_devolucion DATETIME NULL,
    monto_garantia_total DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    monto_devuelto DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    estado_envase ENUM('PENDIENTE', 'PARCIAL', 'DEVUELTO') NOT NULL DEFAULT 'PENDIENTE',
    estado_deposito ENUM('RETENIDO', 'PARCIAL', 'DEVUELTO') NOT NULL DEFAULT 'RETENIDO',
    estado_garantia ENUM('ABIERTA', 'CERRADA', 'ANULADA') NOT NULL DEFAULT 'ABIERTA',
    observacion VARCHAR(255) NULL,
    PRIMARY KEY (id_garantia),
    KEY idx_garantia_usuario (id_usuario),
    KEY idx_garantia_producto (id_producto),
    KEY idx_garantia_cliente (id_cliente),
    CONSTRAINT fk_garantia_usuario
        FOREIGN KEY (id_usuario) REFERENCES tb_usuario (id_usuario),
    CONSTRAINT fk_garantia_producto
        FOREIGN KEY (id_producto) REFERENCES tb_producto (id_producto)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_garantia_cliente
        FOREIGN KEY (id_cliente) REFERENCES tb_cliente (id_cliente),
    CONSTRAINT chk_garantia_cantidad_envases CHECK (cantidad_envases > 0),
    CONSTRAINT chk_garantia_cantidad_devuelta
        CHECK (cantidad_devuelta >= 0 AND cantidad_devuelta <= cantidad_envases),
    CONSTRAINT chk_garantia_monto_unitario CHECK (monto_garantia_unitario >= 0),
    CONSTRAINT chk_garantia_monto_devuelto
        CHECK (monto_devuelto >= 0 AND monto_devuelto <= monto_garantia_total)
);

CREATE TABLE tb_devolucion_garantia (
    id_devolucion_garantia INT NOT NULL AUTO_INCREMENT,
    id_garantia INT NOT NULL,
    id_usuario INT NOT NULL,
    cantidad_devuelta INT NOT NULL,
    monto_devuelto DECIMAL(8,2) NOT NULL,
    fecha_devolucion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_devolucion_garantia),
    KEY idx_devolucion_garantia (id_garantia),
    KEY idx_devolucion_usuario (id_usuario),
    CONSTRAINT fk_devolucion_garantia
        FOREIGN KEY (id_garantia) REFERENCES tb_garantia (id_garantia),
    CONSTRAINT fk_devolucion_usuario
        FOREIGN KEY (id_usuario) REFERENCES tb_usuario (id_usuario),
    CONSTRAINT chk_devolucion_cantidad CHECK (cantidad_devuelta >= 0),
    CONSTRAINT chk_devolucion_monto CHECK (monto_devuelto >= 0),
    CONSTRAINT chk_devolucion_contenido
        CHECK (cantidad_devuelta > 0 OR monto_devuelto > 0)
);

INSERT INTO tb_rol (nombre, es_administrador, flgactivo)
VALUES
    ('Administrador', 1, 1),
    ('Empleado', 0, 1);

INSERT INTO tb_persona (nombre)
VALUES ('Administrador');

INSERT INTO tb_usuario (
    id_persona,
    id_rol,
    usuario,
    contrasena,
    color_perfil,
    flgactivo
)
VALUES (
    (SELECT id_persona FROM tb_persona WHERE nombre = 'Administrador' LIMIT 1),
    (SELECT id_rol FROM tb_rol WHERE es_administrador = 1 LIMIT 1),
    'admin',
    'pbkdf2_sha256$120000$tKlSLd1bIY318WOeFU79XQ$HfW3n/nwMI4Cx0fbzBANHnHPAg295Y1W1wGIuu74jNE',
    '#116A72',
    1
);

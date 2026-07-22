CREATE TABLE tb_rol (
    id_rol INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    flgactivo TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id_rol),
    UNIQUE KEY uq_rol_nombre (nombre)
);

INSERT INTO tb_rol (nombre, flgactivo)
VALUES ('Administrador', 1);

ALTER TABLE tb_usuario
    ADD COLUMN id_rol INT NULL AFTER id_persona;

UPDATE tb_usuario
SET id_rol = (SELECT id_rol FROM tb_rol WHERE nombre = 'Administrador');

ALTER TABLE tb_usuario
    MODIFY COLUMN id_rol INT NOT NULL,
    ADD CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol) REFERENCES tb_rol (id_rol),
    ADD UNIQUE KEY uq_usuario_nombre (usuario);

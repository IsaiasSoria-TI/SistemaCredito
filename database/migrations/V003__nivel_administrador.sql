ALTER TABLE tb_rol
    ADD COLUMN es_administrador TINYINT NOT NULL DEFAULT 0
    AFTER nombre;

UPDATE tb_rol
SET es_administrador = 1
WHERE nombre = 'Administrador';

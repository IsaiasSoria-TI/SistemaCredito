ALTER TABLE tb_usuario
    ADD COLUMN color_perfil VARCHAR(7) NOT NULL DEFAULT '#116A72'
    AFTER contrasena;

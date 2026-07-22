package com.sistemagarantia.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtils {

    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int ITERACIONES = 120_000;
    private static final int TAMANO_SAL = 16;
    private static final int TAMANO_HASH = 256;

    private PasswordUtils() {
    }

    public static String proteger(String contrasena) {
        byte[] sal = new byte[TAMANO_SAL];
        new SecureRandom().nextBytes(sal);
        byte[] hash = derivar(contrasena.toCharArray(), sal, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(sal) + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(hash);
    }

    public static boolean verificar(String contrasena, String valorGuardado) {
        if (contrasena == null || valorGuardado == null) {
            return false;
        }
        if (!valorGuardado.startsWith(PREFIJO + "$")) {
            return MessageDigest.isEqual(
                    contrasena.getBytes(StandardCharsets.UTF_8),
                    valorGuardado.getBytes(StandardCharsets.UTF_8)
            );
        }

        try {
            String[] partes = valorGuardado.split("\\$", 4);
            if (partes.length != 4) {
                return false;
            }
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] obtenido = derivar(contrasena.toCharArray(), sal, iteraciones);
            return MessageDigest.isEqual(esperado, obtenido);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean requiereActualizacion(String valorGuardado) {
        if (valorGuardado == null || !valorGuardado.startsWith(PREFIJO + "$")) {
            return true;
        }
        try {
            String[] partes = valorGuardado.split("\\$", 3);
            return partes.length < 2 || Integer.parseInt(partes[1]) < ITERACIONES;
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    private static byte[] derivar(char[] contrasena, byte[] sal, int iteraciones) {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, sal, iteraciones, TAMANO_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacion)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo proteger la contraseña.", exception);
        } finally {
            especificacion.clearPassword();
            java.util.Arrays.fill(contrasena, '\0');
        }
    }
}

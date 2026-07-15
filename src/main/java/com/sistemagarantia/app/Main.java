package com.sistemagarantia.app;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.sistemagarantia.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configurarLookAndFeel();
            new LoginFrame().setVisible(true);
        });
    }

    private static void configurarLookAndFeel() {
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.showButtons", true);
        UIManager.put(FlatClientProperties.USE_WINDOW_DECORATIONS, true);
    }
}

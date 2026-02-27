package com.example.cliente;

import javax.swing.SwingUtilities;

public class ClienteMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BibliotecaGUI().setVisible(true));
    }
}

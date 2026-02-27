package com.example.cliente;

import library.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class BibliotecaGUI extends JFrame {

    // ── Colores ──────────────────────────────────────────────────────────────
    private static final Color PRIMARY      = new Color(37,  99, 235);   // azul
    private static final Color PRIMARY_DARK = new Color(29,  78, 216);
    private static final Color SUCCESS      = new Color(22, 163,  74);
    private static final Color DANGER       = new Color(220,  38,  38);
    private static final Color BG           = new Color(248, 250, 252);
    private static final Color CARD         = Color.WHITE;
    private static final Color TEXT_MAIN    = new Color( 15,  23,  42);
    private static final Color TEXT_SUB     = new Color(100, 116, 139);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color ROW_ALT      = new Color(241, 245, 249);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  20);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);

    // ── ISBN fijos ────────────────────────────────────────────────────────────
    private static final String[] ISBNS = {
            "9780307474278",
            "9788437604947",
            "9788466333978",
            "9780060883287",
            "9789500721507"
    };

    // ── Estado ────────────────────────────────────────────────────────────────
    private BibliotecaServiceGrpc.BibliotecaServiceBlockingStub stub;
    private ManagedChannel channel;

    // ── Componentes principales ───────────────────────────────────────────────
    private JTable tablaLibros;
    private DefaultTableModel modeloTabla;
    private JLabel lblEstado;
    private JTextArea areaResultado;
    private JPanel panelConexion;
    private JPanel panelApp;
    private CardLayout cardLayout;
    private JPanel cardPanel;

    // ── Conexión ──────────────────────────────────────────────────────────────
    private JTextField txtHost;
    private JTextField txtPuerto;

    public BibliotecaGUI() {
        setTitle("📚 Biblioteca — Sistema de Gestión");
        setSize(900, 640);
        setMinimumSize(new Dimension(800, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(BG);

        cardPanel.add(buildPanelConexion(), "conexion");
        cardPanel.add(buildPanelApp(),      "app");

        add(cardPanel);
        cardLayout.show(cardPanel, "conexion");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PANTALLA DE CONEXIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    private JPanel buildPanelConexion() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(40, 50, 40, 50)));
        card.setMaximumSize(new Dimension(420, 999));

        // Ícono + título
        JLabel ico   = new JLabel("📚", SwingConstants.CENTER);
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        ico.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Biblioteca", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Conecte al servidor gRPC para continuar", SwingConstants.CENTER);
        sub.setFont(FONT_SMALL);
        sub.setForeground(TEXT_SUB);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Campos
        txtHost   = styledField("localhost");
        txtPuerto = styledField("50051");

        JButton btnConectar = bigButton("Conectar", PRIMARY);
        btnConectar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConectar.addActionListener(e -> conectar());

        card.add(ico);
        card.add(Box.createVerticalStrut(8));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(28));
        card.add(fieldBlock("Servidor (IP / hostname)", txtHost));
        card.add(Box.createVerticalStrut(12));
        card.add(fieldBlock("Puerto", txtPuerto));
        card.add(Box.createVerticalStrut(24));
        card.add(btnConectar);

        outer.add(card);
        return outer;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PANTALLA PRINCIPAL
    // ═══════════════════════════════════════════════════════════════════════════
    private JPanel buildPanelApp() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildCenter(),    BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(PRIMARY);
        h.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel lbl = new JLabel("📚  Biblioteca");
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(Color.WHITE);

        JButton btnRefresh = ghostBtn("⟳ Actualizar");
        btnRefresh.addActionListener(e -> actualizarTabla());

        JButton btnDesconectar = ghostBtn("⏻ Desconectar");
        btnDesconectar.addActionListener(e -> desconectar());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnRefresh);
        right.add(btnDesconectar);

        h.add(lbl,   BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    // ── Centro (tabla + panel acciones) ──────────────────────────────────────
    private JSplitPane buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildTablePanel(), buildActionsPanel());
        split.setDividerLocation(520);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(BG);
        return split;
    }

    // ── Tabla ─────────────────────────────────────────────────────────────────
    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 0, 8));

        JLabel lbl = new JLabel("Catálogo");
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MAIN);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));

        String[] cols = {"ISBN", "Título", "Disponibles"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tablaLibros = new JTable(modeloTabla);
        tablaLibros.setFont(FONT_NORMAL);
        tablaLibros.setRowHeight(32);
        tablaLibros.setShowGrid(false);
        tablaLibros.setIntercellSpacing(new Dimension(0, 0));
        tablaLibros.setSelectionBackground(new Color(219, 234, 254));
        tablaLibros.setSelectionForeground(TEXT_MAIN);
        tablaLibros.setBackground(CARD);
        tablaLibros.setForeground(TEXT_MAIN);
        tablaLibros.getTableHeader().setFont(FONT_LABEL);
        tablaLibros.getTableHeader().setBackground(new Color(241, 245, 249));
        tablaLibros.getTableHeader().setForeground(TEXT_SUB);
        tablaLibros.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));
        tablaLibros.getColumnModel().getColumn(0).setPreferredWidth(130);
        tablaLibros.getColumnModel().getColumn(1).setPreferredWidth(240);
        tablaLibros.getColumnModel().getColumn(2).setPreferredWidth(80);

        // Renderer alternado + badge disponibles
        tablaLibros.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (!sel) setBackground(row % 2 == 0 ? CARD : ROW_ALT);
                if (col == 2 && v != null) {
                    int n = 0;
                    try { n = Integer.parseInt(v.toString()); } catch (Exception ignored) {}
                    setForeground(n > 0 ? SUCCESS : DANGER);
                    setFont(FONT_LABEL);
                } else {
                    setForeground(TEXT_MAIN);
                    setFont(col == 0 ? new Font("Monospaced", Font.PLAIN, 12) : FONT_NORMAL);
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(tablaLibros);
        scroll.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        scroll.getViewport().setBackground(CARD);

        p.add(lbl,    BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── Panel acciones ────────────────────────────────────────────────────────
    private JPanel buildActionsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 8, 0, 16));

        JLabel lbl = new JLabel("Acciones");
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MAIN);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));

        // Botones de acción
        JButton btnConsultar    = actionBtn("🔍  Consultar por ISBN",    PRIMARY);
        JButton btnPrestarIsbn  = actionBtn("📤  Prestar por ISBN",      new Color(124, 58, 237));
        JButton btnPrestarTitulo= actionBtn("📤  Prestar por Título",    new Color(124, 58, 237));
        JButton btnDevolver     = actionBtn("📥  Devolver libro",        SUCCESS);

        btnConsultar    .addActionListener(e -> dialogConsultar());
        btnPrestarIsbn  .addActionListener(e -> dialogPrestarIsbn());
        btnPrestarTitulo.addActionListener(e -> dialogPrestarTitulo());
        btnDevolver     .addActionListener(e -> dialogDevolver());

        JPanel btns = new JPanel();
        btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));
        btns.setBackground(BG);
        btns.add(btnConsultar);
        btns.add(Box.createVerticalStrut(10));
        btns.add(btnPrestarIsbn);
        btns.add(Box.createVerticalStrut(10));
        btns.add(btnPrestarTitulo);
        btns.add(Box.createVerticalStrut(10));
        btns.add(btnDevolver);

        // Área de resultado
        JLabel lblRes = new JLabel("Resultado");
        lblRes.setFont(FONT_LABEL);
        lblRes.setForeground(TEXT_MAIN);
        lblRes.setBorder(new EmptyBorder(16, 0, 6, 0));

        areaResultado = new JTextArea(6, 20);
        areaResultado.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaResultado.setEditable(false);
        areaResultado.setBackground(new Color(15, 23, 42));
        areaResultado.setForeground(new Color(134, 239, 172));
        areaResultado.setCaretColor(Color.WHITE);
        areaResultado.setBorder(new EmptyBorder(10, 12, 10, 12));
        areaResultado.setText("Esperando operación...");

        JScrollPane scrollRes = new JScrollPane(areaResultado);
        scrollRes.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BG);
        south.add(lblRes,     BorderLayout.NORTH);
        south.add(scrollRes,  BorderLayout.CENTER);

        p.add(lbl,   BorderLayout.NORTH);
        p.add(btns,  BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        bar.setBackground(new Color(241, 245, 249));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JLabel dot = new JLabel("●");
        dot.setForeground(SUCCESS);
        dot.setFont(FONT_SMALL);

        lblEstado = new JLabel("Conectado");
        lblEstado.setFont(FONT_SMALL);
        lblEstado.setForeground(TEXT_SUB);

        bar.add(dot);
        bar.add(lblEstado);
        return bar;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  LÓGICA gRPC
    // ═══════════════════════════════════════════════════════════════════════════
    private void conectar() {
        String host  = txtHost.getText().trim();
        String pStr  = txtPuerto.getText().trim();
        if (host.isEmpty() || pStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completa host y puerto.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int port = Integer.parseInt(pStr);
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
            stub    = BibliotecaServiceGrpc.newBlockingStub(channel);

            lblEstado.setText("Conectado a " + host + ":" + port);
            cardLayout.show(cardPanel, "app");
            actualizarTabla();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar:\n" + ex.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void desconectar() {
        if (channel != null) channel.shutdown();
        cardLayout.show(cardPanel, "conexion");
    }

    private void actualizarTabla() {
        modeloTabla.setRowCount(0);
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            protected Void doInBackground() {
                for (String isbn : ISBNS) {
                    try {
                        ConsultarRes r = stub.consultar(ConsultarReq.newBuilder().setIsbn(isbn).build());
                        publish(new Object[]{isbn, r.getTitulo(), r.getEjemplaresDisponibles()});
                    } catch (Exception ex) {
                        publish(new Object[]{isbn, "Error", "—"});
                    }
                }
                return null;
            }
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) modeloTabla.addRow(row);
            }
        };
        worker.execute();
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────
    private void dialogConsultar() {
        String isbn = JOptionPane.showInputDialog(this, "ISBN:", "Consultar libro", JOptionPane.PLAIN_MESSAGE);
        if (isbn == null || isbn.isBlank()) return;
        try {
            ConsultarRes r = stub.consultar(ConsultarReq.newBuilder().setIsbn(isbn.trim()).build());
            mostrarResultado("📖 CONSULTA\n" +
                    "Título:      " + r.getTitulo() + "\n" +
                    "Disponibles: " + r.getEjemplaresDisponibles() + "\n" +
                    "Mensaje:     " + r.getMensaje());
        } catch (Exception ex) { mostrarError(ex); }
    }

    private void dialogPrestarIsbn() {
        JTextField fIsbn = styledField(""); JTextField fNombre = styledField("");
        Object[] form = {"ISBN:", fIsbn, "Prestatario:", fNombre};
        int ok = JOptionPane.showConfirmDialog(this, form, "Prestar por ISBN", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            PrestarRes r = stub.prestarPorIsbn(PrestarPorIsbnReq.newBuilder()
                    .setIsbn(fIsbn.getText().trim()).setPrestatario(fNombre.getText().trim()).build());
            mostrarResultado("📤 PRÉSTAMO\nMensaje: " + r.getMensaje() +
                    (r.getOk() ? "\nDevolución: " + r.getFechaDevolucionIso() : ""));
            actualizarTabla();
        } catch (Exception ex) { mostrarError(ex); }
    }

    private void dialogPrestarTitulo() {
        JTextField fTitulo = styledField(""); JTextField fNombre = styledField("");
        Object[] form = {"Título exacto:", fTitulo, "Prestatario:", fNombre};
        int ok = JOptionPane.showConfirmDialog(this, form, "Prestar por Título", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            PrestarRes r = stub.prestarPorTitulo(PrestarPorTituloReq.newBuilder()
                    .setTitulo(fTitulo.getText().trim()).setPrestatario(fNombre.getText().trim()).build());
            mostrarResultado("📤 PRÉSTAMO\nMensaje: " + r.getMensaje() +
                    (r.getOk() ? "\nDevolución: " + r.getFechaDevolucionIso() : ""));
            actualizarTabla();
        } catch (Exception ex) { mostrarError(ex); }
    }

    private void dialogDevolver() {
        JTextField fIsbn = styledField(""); JTextField fNombre = styledField("");
        Object[] form = {"ISBN:", fIsbn, "Prestatario:", fNombre};
        int ok = JOptionPane.showConfirmDialog(this, form, "Devolver libro", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            DevolverRes r = stub.devolver(DevolverReq.newBuilder()
                    .setIsbn(fIsbn.getText().trim()).setPrestatario(fNombre.getText().trim()).build());
            mostrarResultado("📥 DEVOLUCIÓN\nMensaje: " + r.getMensaje());
            actualizarTabla();
        } catch (Exception ex) { mostrarError(ex); }
    }

    private void mostrarResultado(String texto) {
        areaResultado.setForeground(new Color(134, 239, 172));
        areaResultado.setText(texto);
    }

    private void mostrarError(Exception ex) {
        areaResultado.setForeground(new Color(252, 165, 165));
        areaResultado.setText("❌ Error:\n" + ex.getMessage());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ═══════════════════════════════════════════════════════════════════════════
    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setFont(FONT_NORMAL);
        f.setForeground(TEXT_MAIN);
        f.setBackground(Color.WHITE);
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        f.setPreferredSize(new Dimension(280, 36));
        return f;
    }

    private JPanel fieldBlock(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MAIN);
        p.add(lbl,   BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JButton bigButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(FONT_BTN);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(280, 42));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(PRIMARY_DARK); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private JButton actionBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(FONT_BTN);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setPreferredSize(new Dimension(320, 40));
        Color darker = bg.darker();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(darker); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private JButton ghostBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(255, 255, 255, 30));
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(255, 255, 255, 80), 1, true),
                new EmptyBorder(4, 12, 4, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        return b;
    }
}

package com.pix.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.MaskFormatter;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.pix.client.PixClient;

/**
 * Interface gráfica moderna para login e cadastro de usuários (estilo BigTech).
 */
public class LoginGUI extends JFrame {

    private PixClient client;
    private JFormattedTextField cpfField;
    private JPasswordField senhaField;
    private JButton loginButton;
    private JButton conectarButton;
    private JTextField hostField;
    private JTextField portField;
    private JLabel statusLabel;
    private JButton abrirCadastroButton;
    private JToggleButton darkModeToggle;

    private boolean darkMode = false;

    public LoginGUI() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        applyTheme();
    }

    private void initializeComponents() {
        setTitle("💸 PixFácil - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        // Campos de conexão
        hostField = new JTextField("localhost", 15);
        portField = new JTextField("25444", 8);
        conectarButton = createPrimaryButton("Conectar");

        // Campo CPF com máscara
        try {
            MaskFormatter cpfMask = new MaskFormatter("###.###.###-##");
            cpfMask.setPlaceholderCharacter('_');
            cpfField = new JFormattedTextField(cpfMask);
            cpfField.setColumns(14);
        } catch (ParseException e) {
            cpfField = new JFormattedTextField();
        }

        senhaField = new JPasswordField(20);

        loginButton = createPrimaryButton("Entrar");
        abrirCadastroButton = createSecondaryButton("Cadastrar");

        statusLabel = new JLabel("Desconectado", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));

        darkModeToggle = new JToggleButton("🌙 Modo Escuro");

        // Desabilitar campos até conectar
        setFieldsEnabled(false);
    }

    private void setupLayout() {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        // Painel de título
        JLabel titleLabel = new JLabel("PixFácil");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(titleLabel, BorderLayout.NORTH);

        // Painel central
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Conexão
        JPanel conexaoPanel = createCardPanel("Conexão com Servidor");
        addLabeledField(conexaoPanel, "Host:", hostField);
        addLabeledField(conexaoPanel, "Porta:", portField);
        conexaoPanel.add(conectarButton);
        conexaoPanel.add(statusLabel);

        // Login
        JPanel loginPanel = createCardPanel("Acesso ao Sistema");
        addLabeledField(loginPanel, "CPF:", cpfField);
        addLabeledField(loginPanel, "Senha:", senhaField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(loginButton);
        buttonPanel.add(abrirCadastroButton);
        loginPanel.add(buttonPanel);

        // Adicionar ao centro
        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(conexaoPanel, gbc);
        gbc.gridy = 1;
        centerPanel.add(loginPanel, gbc);

        root.add(centerPanel, BorderLayout.CENTER);

        // Rodapé
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        JTextArea infoArea = new JTextArea(
                "💡 Como usar:\n" +
                        "1. Conecte-se ao servidor\n" +
                        "2. Faça login ou cadastre-se\n" +
                        "3. Realize transações PIX com segurança"
        );
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        bottomPanel.add(infoArea, BorderLayout.CENTER);
        bottomPanel.add(darkModeToggle, BorderLayout.SOUTH);

        root.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        conectarButton.addActionListener(e -> conectarServidor());
        loginButton.addActionListener(e -> realizarLogin());
        abrirCadastroButton.addActionListener(e -> abrirCadastro());

        darkModeToggle.addActionListener(e -> {
            darkMode = darkModeToggle.isSelected();
            darkModeToggle.setText(darkMode ? "☀️ Modo Claro" : "🌙 Modo Escuro");
            applyTheme();
        });
    }

    private void applyTheme() {
        try {
            if (darkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void conectarServidor() {
        try {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());

            client = new PixClient(host, port);

            if (client.connect()) {
                statusLabel.setText("✅ Conectado");
                statusLabel.setForeground(new Color(0, 180, 0));
                conectarButton.setText("Desconectar");
                setFieldsEnabled(true);

                for (ActionListener al : conectarButton.getActionListeners()) {
                    conectarButton.removeActionListener(al);
                }
                conectarButton.addActionListener(e -> desconectarServidor());
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao conectar ao servidor!",
                        "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Porta inválida!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void desconectarServidor() {
        if (client != null) {
            client.disconnect();
        }
        statusLabel.setText("Desconectado");
        statusLabel.setForeground(Color.RED);
        conectarButton.setText("Conectar");
        setFieldsEnabled(false);

        for (ActionListener al : conectarButton.getActionListeners()) {
            conectarButton.removeActionListener(al);
        }
        conectarButton.addActionListener(e -> conectarServidor());
    }

    private void realizarLogin() {
        String cpfDigits = cpfField.getText();
        String senha = new String(senhaField.getPassword());

        if (cpfDigits.isEmpty() || senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "CPF e senha são obrigatórios!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (cpfDigits.length() != 14) {
            JOptionPane.showMessageDialog(this, "CPF inválido: informe 11 dígitos.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PixClient.LoginResult result = client.login(cpfDigits, senha);

        if (result.isSuccess()) {
            MainGUI mainGUI = new MainGUI(client, result.getToken());
            mainGUI.setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(),
                    "Erro de Login", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        cpfField.setEnabled(enabled);
        senhaField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        abrirCadastroButton.setEnabled(enabled);

        hostField.setEnabled(!enabled);
        portField.setEnabled(!enabled);
    }

    private void abrirCadastro() {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Conecte-se ao servidor antes de cadastrar usuários.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        CadastroGUI cadastro = new CadastroGUI(this, client);
        cadastro.setVisible(true);
    }

    // ==== UI Helper Methods ====
    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    private void addLabeledField(JPanel panel, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 8));
        row.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(80, 25));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(row);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginGUI().setVisible(true));
    }
}

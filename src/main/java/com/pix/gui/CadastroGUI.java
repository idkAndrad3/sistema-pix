package com.pix.gui;

import com.pix.client.PixClient;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.ParseException;

/**
 * Janela moderna de cadastro de usuário (estilo BigTech).
 */
public class CadastroGUI extends JDialog {

    private final PixClient client;
    private JFormattedTextField cpfField;
    private JTextField nomeField;
    private JPasswordField senhaField;
    private JButton cadastrarButton;
    private JButton cancelarButton;

    private boolean darkMode = false;

    public CadastroGUI(JFrame owner, PixClient client) {
        super(owner, "✨ Cadastrar Usuário", true);
        this.client = client;
        initializeComponents();
        setupLayout();
        setupEventHandlers();

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initializeComponents() {
        // CPF com máscara
        try {
            MaskFormatter cpfMask = new MaskFormatter("###.###.###-##");
            cpfMask.setPlaceholderCharacter('_');
            cpfField = new JFormattedTextField(cpfMask);
            cpfField.setColumns(14);
        } catch (ParseException e) {
            e.printStackTrace();
            cpfField = new JFormattedTextField();
            cpfField.setColumns(14);
        }

        nomeField = new JTextField(20);
        senhaField = new JPasswordField(20);

        cadastrarButton = createPrimaryButton("Cadastrar");
        cancelarButton = createSecondaryButton("Cancelar");
    }

    private void setupLayout() {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        // Título
        JLabel titleLabel = new JLabel("Cadastro de Usuário");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(titleLabel, BorderLayout.NORTH);

        // Formulário
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addLabeledField(formPanel, gbc, 0, "CPF:", cpfField);
        addLabeledField(formPanel, gbc, 1, "Nome:", nomeField);
        addLabeledField(formPanel, gbc, 2, "Senha:", senhaField);

        // Botões
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cadastrarButton);
        buttonPanel.add(cancelarButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);

        // Informações
        JTextArea info = new JTextArea(
                "📋 Regras:\n" +
                        "• Nome: 6-120 caracteres\n" +
                        "• Senha: 6-120 caracteres\n" +
                        "• CPF válido (apenas dígitos)"
        );
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        info.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        gbc.gridy = 4;
        formPanel.add(info, gbc);

        root.add(formPanel, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        cadastrarButton.addActionListener(e -> realizarCadastro());
        cancelarButton.addActionListener(e -> dispose());
    }

    private void realizarCadastro() {
        if (client == null) {
            JOptionPane.showMessageDialog(this,
                    "Conexão não disponível. Volte à tela de login e conecte-se ao servidor.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String cpfDigits = cpfField.getText();
        String nome = nomeField.getText().trim();
        String senha = new String(senhaField.getPassword());

        // Validações
        if (cpfDigits.isEmpty() || nome.isEmpty() || senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos os campos são obrigatórios.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (cpfDigits.length() != 14) {
            JOptionPane.showMessageDialog(this, "CPF inválido: informe 11 dígitos.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (nome.length() < 6 || nome.length() > 120) {
            JOptionPane.showMessageDialog(this, "Nome deve ter entre 6 e 120 caracteres.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (senha.length() < 6 || senha.length() > 120) {
            JOptionPane.showMessageDialog(this, "Senha deve ter entre 6 e 120 caracteres.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            PixClient.OperationResult result = client.criarUsuario(cpfDigits, nome, senha);
            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        "Usuário cadastrado com sucesso! Faça login para continuar.",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                cpfField.setValue(null);
                nomeField.setText("");
                senhaField.setText("");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage(),
                        "Erro no cadastro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao cadastrar: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==== UI Helpers ====
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

    private void addLabeledField(JPanel panel, GridBagConstraints gbc, int y,
                                 String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(80, 25));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    // ==== FlatLaf Tema Claro/Escuro ====
    public void applyTheme(boolean dark) {
        try {
            if (dark) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

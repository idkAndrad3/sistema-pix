package com.pix.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.MaskFormatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pix.client.PixClient;

/**
 * Interface gráfica principal do cliente NewPix.
 */
public class MainGUI extends JFrame {
    
    private PixClient client;
    private String token;
    private String nomeUsuario;
    private String cpfUsuario;
    private double saldoUsuario;
    
    // Componentes da interface
    private JLabel nomeLabel;
    private JLabel cpfLabel;
    private JLabel saldoLabel;
    private JFormattedTextField valorPixField; // campo de moeda (com DocumentFilter)
    private JFormattedTextField cpfDestinoField; // campo CPF com máscara
    private JButton pixButton;
    private JButton atualizarButton;
    private JButton extratoButton;
    private JButton logoutButton;
    private JTable extratoTable;
    private DefaultTableModel tableModel;
    private JTextField novoNomeField;
    private JPasswordField novaSenhaField;
    
    // Novos componentes para depósito
    private JFormattedTextField valorDepositoField;
    private JButton depositoButton;
    
    private DecimalFormat currencyFormat = new DecimalFormat("R$ #,##0.00");
    
    public MainGUI(PixClient client, String token) {
        this.client = client;
        this.token = token;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        carregarDadosUsuario();
    }
    
    private void initializeComponents() {
        setTitle("NewPix - Sistema Bancário");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        
        // Labels de informação do usuário
        nomeLabel = new JLabel("Nome: Carregando...");
        cpfLabel = new JLabel("CPF: Carregando...");
        saldoLabel = new JLabel("Saldo: Carregando...");
        saldoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        saldoLabel.setForeground(new Color(0, 150, 0));
        
        // Campos para PIX
        // Valor: campo texto com DocumentFilter que formata moeda enquanto digita
        valorPixField = new  JFormattedTextField();
        ((AbstractDocument) valorPixField.getDocument()).setDocumentFilter(new CurrencyDocumentFilter());
        valorPixField.setColumns(15);
        // inicializar com 0,00 (opcional)
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        valorPixField.setText(nf.format(0.0));
        
        // CPF destino: JFormattedTextField com máscara
        try {
            MaskFormatter cpfMask = new MaskFormatter("###.###.###-##");
            cpfMask.setPlaceholderCharacter('_');
            cpfDestinoField = new JFormattedTextField(cpfMask);
            cpfDestinoField.setColumns(14);
        } catch (ParseException e) {
            e.printStackTrace();
            cpfDestinoField = new JFormattedTextField();
            cpfDestinoField.setColumns(14);
        }
        
        pixButton = new JButton("Enviar PIX");
        pixButton.setBackground(new Color(50, 150, 250));
        pixButton.setForeground(Color.WHITE);
        
        // Campos para atualização de dados
        novoNomeField = new JTextField(20);
        novaSenhaField = new JPasswordField(20);
        atualizarButton = new JButton("Atualizar Dados");
        
        // Botões de ação
        extratoButton = new JButton("Atualizar Extrato");
        logoutButton = new JButton("Logout");
        
        // Tabela de extrato
        String[] columns = {"Data", "Tipo", "Valor", "Origem/Destino"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        extratoTable = new JTable(tableModel);
        extratoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Novos componentes para depósito
        valorDepositoField = new JFormattedTextField();
        ((AbstractDocument) valorDepositoField.getDocument()).setDocumentFilter(new CurrencyDocumentFilter());
        valorDepositoField.setColumns(15);
        NumberFormat nfDeposito = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        valorDepositoField.setText(nfDeposito.format(0.0));
        
        depositoButton = new JButton("Realizar Depósito");
        depositoButton.setBackground(new Color(50, 200, 100));
        depositoButton.setForeground(Color.WHITE);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Panel superior - informações do usuário
        JPanel userPanel = new JPanel(new GridBagLayout());
        userPanel.setBorder(BorderFactory.createTitledBorder("Dados do Usuário"));
        userPanel.setBackground(new Color(240, 248, 255));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        userPanel.add(nomeLabel, gbc);
        gbc.gridx = 1;
        userPanel.add(cpfLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        userPanel.add(saldoLabel, gbc);
        
        // Panel central - operações
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Aba PIX
        JPanel pixPanel = createPixPanel();
        tabbedPane.addTab("Enviar PIX", pixPanel);
        
        // Aba Depósito
        JPanel depositoPanel = createDepositoPanel();
        tabbedPane.addTab("Depósito", depositoPanel);
        
        // Aba Extrato
        JPanel extratoPanel = createExtratoPanel();
        tabbedPane.addTab("Extrato", extratoPanel);
        
        // Aba Configurações
        JPanel configPanel = createConfigPanel();
        tabbedPane.addTab("Configurações", configPanel);
        
        // Panel inferior - botões de ação
        JPanel actionPanel = new JPanel(new FlowLayout());
        actionPanel.add(extratoButton);
        actionPanel.add(logoutButton);
        
        add(userPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createPixPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Valor (R$):"), gbc);
        gbc.gridx = 1;
        panel.add(valorPixField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("CPF Destino:"), gbc);
        gbc.gridx = 1;
        panel.add(cpfDestinoField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(pixButton, gbc);
        
        // Área de instruções
        gbc.gridy = 3;
        JTextArea instructions = new JTextArea(
            "Instruções:\n" +
            "1. Digite o valor a ser enviado\n" +
            "2. Informe o CPF do destinatário\n" +
            "3. Clique em 'Enviar PIX'\n\n" 
            
        );
        instructions.setEditable(false);
        instructions.setBackground(panel.getBackground());
        instructions.setBorder(BorderFactory.createTitledBorder("Instruções"));
        panel.add(instructions, gbc);
        
        return panel;
    }
    
    private JPanel createDepositoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Valor do Depósito (R$):"), gbc);
        gbc.gridx = 1;
        panel.add(valorDepositoField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(depositoButton, gbc);
        
        // Área de instruções
        gbc.gridy = 2;
        JTextArea instructions = new JTextArea(
            "Instruções:\n" +
            "1. Digite o valor a ser depositado\n" +
            "2. Clique em 'Realizar Depósito'\n" +
            "3. Seu saldo será atualizado automaticamente"
        );
        instructions.setEditable(false);
        instructions.setBackground(panel.getBackground());
        instructions.setBorder(BorderFactory.createTitledBorder("Instruções"));
        panel.add(instructions, gbc);
        
        return panel;
    }
    
    private JPanel createExtratoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JScrollPane scrollPane = new JScrollPane(extratoTable);
        scrollPane.setPreferredSize(new Dimension(800, 300));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Atualizar");
        refreshButton.addActionListener(e -> carregarExtrato());
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Novo Nome:"), gbc);
        gbc.gridx = 1;
        panel.add(novoNomeField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Nova Senha:"), gbc);
        gbc.gridx = 1;
        panel.add(novaSenhaField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(atualizarButton, gbc);
        
        // Aviso
        gbc.gridy = 3;
        JTextArea warning = new JTextArea(
            "Atenção:\n" +
            "• Deixe em branco os campos que não deseja alterar\n" +
            "• A senha deve ter entre 6 e 120 caracteres\n" +
            "• O nome deve ter entre 6 e 120 caracteres"
        );
        warning.setEditable(false);
        warning.setBackground(panel.getBackground());
        warning.setBorder(BorderFactory.createTitledBorder("Aviso"));
        panel.add(warning, gbc);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        pixButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarPix();
            }
        });
        
        depositoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarDeposito();
            }
        });
        
        atualizarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                atualizarDados();
            }
        });
        
        extratoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                carregarDadosUsuario();
                carregarExtrato();
            }
        });
        
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarLogout();
            }
        });
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                realizarLogout();
            }
        });
    }
    
    private void carregarDadosUsuario() {
        PixClient.UserDataResult result = client.lerUsuario(token);
        
        if (result.isSuccess()) {
            nomeUsuario = result.getNome();
            cpfUsuario = result.getCpf();
            saldoUsuario = result.getSaldo();
            
            nomeLabel.setText("Nome: " + nomeUsuario);
            cpfLabel.setText("CPF: " + cpfUsuario);
            saldoLabel.setText("Saldo: " + currencyFormat.format(saldoUsuario));
        } else {
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados: " + result.getMessage(),
                                        "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void enviarPix() {
        try {
            double valor = parseValorFromField(valorPixField);

            // Remove caracteres não numéricos mas mantém a formatação para envio
            String cpfDestino = cpfDestinoField.getText().trim();

            if (valor <= 0 || cpfDestino.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos os campos são obrigatórios e o valor deve ser positivo!",
                                              "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (cpfDestino.length() != 14) {
                JOptionPane.showMessageDialog(this, "CPF inválido: informe 11 dígitos.",
                                              "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }


            // Envia o CPF formatado (igual ao login)
            PixClient.OperationResult result = client.criarTransacao(token, valor, cpfDestino);

            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this, "PIX enviado com sucesso para " + cpfDestino,
                                              "Sucesso", JOptionPane.INFORMATION_MESSAGE);

                // Limpa os campos
                NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
                valorPixField.setText(nf.format(0.0));
                cpfDestinoField.setValue(null);
                carregarDadosUsuario();
                carregarExtrato();
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage(),
                                              "Erro", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido!",
                                          "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao enviar PIX: " + e.getMessage(),
                                          "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void realizarDeposito() {
        try {
            double valor = parseValorFromField(valorDepositoField);

            if (valor <= 0) {
                JOptionPane.showMessageDialog(this, "O valor do depósito deve ser positivo!",
                                            "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PixClient.OperationResult result = client.depositar(token, valor);

            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Depósito realizado com sucesso!",
                                            "Sucesso", JOptionPane.INFORMATION_MESSAGE);

                // Limpa o campo e atualiza os dados
                NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
                valorDepositoField.setText(nf.format(0.0));
                carregarDadosUsuario();
                carregarExtrato();
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage(),
                                            "Erro", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido!",
                                        "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao realizar depósito: " + e.getMessage(),
                                        "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void atualizarDados() {
        String novoNome = novoNomeField.getText().trim();
        String novaSenha = new String(novaSenhaField.getPassword());
        
        if (novoNome.isEmpty() && novaSenha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe pelo menos um campo para atualizar!",
                                        "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        PixClient.OperationResult result = client.atualizarUsuario(token, 
                                                                     novoNome.isEmpty() ? null : novoNome,
                                                                     novaSenha.isEmpty() ? null : novaSenha);
        
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, "Dados atualizados com sucesso!",
                                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            novoNomeField.setText("");
            novaSenhaField.setText("");
            carregarDadosUsuario(); // Atualizar dados exibidos
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(),
                                        "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void carregarExtrato() {
    }
    
    private void realizarLogout() {
        int option = JOptionPane.showConfirmDialog(this, "Deseja realmente sair?", 
                                                  "Confirmar Logout", JOptionPane.YES_NO_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            client.logout(token);
            client.disconnect();
            
            // Voltar para tela de login
            LoginGUI loginGUI = new LoginGUI();
            loginGUI.setVisible(true);
            this.dispose();
        }
    }

    // ------------------ utilitários para campo de moeda ------------------

   
    private static class CurrencyDocumentFilter extends DocumentFilter {
        private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        // retorna apenas dígitos da string
        private String onlyDigits(String s) {
            return s == null ? "" : s.replaceAll("\\D", "");
        }

        private void applyFormat(FilterBypass fb, String digits) throws BadLocationException {
            // remover zeros à esquerda, mas manter ao menos um zero
            digits = digits.replaceFirst("^0+(?!$)", "");
            if (digits.isEmpty()) {
                fb.replace(0, fb.getDocument().getLength(), "", null);
                return;
            }

            long cents = Long.parseLong(digits);
            double value = cents / 100.0;
            String formatted = currencyFormat.format(value);
            fb.replace(0, fb.getDocument().getLength(), formatted, null);
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String currentDigits = onlyDigits(current);
            String newDigits = currentDigits + onlyDigits(string); // append no final
            applyFormat(fb, newDigits);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String currentDigits = onlyDigits(current);

            // comportamento simples: se houve substituição, retiramos 'length' dígitos do final e então anexamos os novos
            int removeCount = Math.min(length, currentDigits.length());
            String base = currentDigits;
            if (removeCount > 0) {
                base = currentDigits.substring(0, currentDigits.length() - removeCount);
            }
            String newDigits = base + onlyDigits(text);
            if (newDigits.isEmpty()) {
                fb.replace(0, fb.getDocument().getLength(), "", null);
            } else {
                applyFormat(fb, newDigits);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String currentDigits = onlyDigits(current);
            // remover 'length' dígitos do final (fallback)
            int newLen = Math.max(0, currentDigits.length() - length);
            String newDigits = currentDigits.substring(0, newLen);
            if (newDigits.isEmpty()) {
                fb.replace(0, fb.getDocument().getLength(), "", null);
            } else {
                applyFormat(fb, newDigits);
            }
        }
    }

    /**
     * Extrai o valor em double do campo formatado (ex: "R$ 10,00" -> 10.0).
     */
    private double parseValorFromField(JFormattedTextField field) throws ParseException {
        String text = field.getText().trim();
        if (text.isEmpty() || text.equalsIgnoreCase("R$")) {
            return 0.0;
        }

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        Number number = nf.parse(text);
        return number.doubleValue();
    }
}
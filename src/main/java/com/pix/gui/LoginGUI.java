package com.pix.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;

import com.pix.client.PixClient;

/**
 * Interface gráfica para login e cadastro de usuários.
 */
public class LoginGUI extends JFrame {
    
    private PixClient client;
    private JFormattedTextField cpfField;
    private JPasswordField senhaField;
    private JButton loginButton;
    private JButton cadastroButton;
    private JButton conectarButton;
    private JTextField hostField;
    private JTextField portField;
    private JLabel statusLabel;
    private JButton abrirCadastroButton;
    
    public LoginGUI() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        setTitle("NewPix - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Campos de conexão
        hostField = new JTextField("localhost", 15);
        portField = new JTextField("7777", 8);
        conectarButton = new JButton("Conectar");
        
        // Campo CPF com máscara
        try {
            MaskFormatter cpfMask = new MaskFormatter("###.###.###-##");
            cpfMask.setPlaceholderCharacter('_');
            cpfField = new JFormattedTextField(cpfMask);
            cpfField.setColumns(14);
        } catch (ParseException e) {
            e.printStackTrace();
            cpfField = new JFormattedTextField();
        }

        senhaField = new JPasswordField(20);
        
        loginButton = new JButton("Login");
        abrirCadastroButton = new JButton("Cadastrar");
        
        statusLabel = new JLabel("Desconectado");
        statusLabel.setForeground(Color.RED);
        
        // Desabilitar campos até conectar
        setFieldsEnabled(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Panel de conexão
        JPanel conexaoPanel = new JPanel(new GridBagLayout());
        conexaoPanel.setBorder(BorderFactory.createTitledBorder("Conexão com Servidor"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        conexaoPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        conexaoPanel.add(hostField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        conexaoPanel.add(new JLabel("Porta:"), gbc);
        gbc.gridx = 1;
        conexaoPanel.add(portField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        conexaoPanel.add(conectarButton, gbc);
        
        gbc.gridy = 3;
        conexaoPanel.add(statusLabel, gbc);
        
        // Panel principal
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder("Sistema NewPix"));
        
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("CPF:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(cpfField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(senhaField, gbc);
        
        // Panel de botões
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(loginButton);
        buttonPanel.add(abrirCadastroButton);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(buttonPanel, gbc);
        
        // Adicionar painéis ao frame
        add(conexaoPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        
        // Panel de informações
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Informações"));
        
        JTextArea infoArea = new JTextArea(
            "NewPix - Sistema Bancário Distribuído\n" +
            "1. Conecte-se ao servidor\n" +
            "2. Faça login ou cadastre-se\n" +
            "3. Realize transações PIX"
        );
        infoArea.setEditable(false);
        infoArea.setBackground(getBackground());
        infoPanel.add(infoArea);
        
        add(infoPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        conectarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                conectarServidor();
            }
        });
        
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarLogin();
            }
        });
        
        abrirCadastroButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirCadastro();
            }
        });
    }
    
    private void conectarServidor() {
        try {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            
            client = new PixClient(host, port);
            
            if (client.connect()) {
                statusLabel.setText("Conectado");
                statusLabel.setForeground(Color.GREEN);
                conectarButton.setText("Desconectar");
                setFieldsEnabled(true);
                
                // Atualizar action do botão conectar
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
        
        // Atualizar action do botão conectar
        for (ActionListener al : conectarButton.getActionListeners()) {
            conectarButton.removeActionListener(al);
        }
        conectarButton.addActionListener(e -> conectarServidor());
    }
    
    private void realizarLogin() {
        // pega apenas os dígitos do campo
        String cpfDigits = cpfField.getText();
        String senha = new String(senhaField.getPassword());

        if (cpfDigits.isEmpty() || senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "CPF e senha são obrigatórios!",
                                          "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // valida CPF (11 dígitos)
        if (cpfDigits.length() != 14) {
            JOptionPane.showMessageDialog(this, "CPF inválido: informe 11 dígitos.",
                                          "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }


        // envia cpf formatado
        PixClient.LoginResult result = client.login(cpfDigits, senha);

        if (result.isSuccess()) {
            // abrir tela principal
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
            JOptionPane.showMessageDialog(this, "Conecte-se ao servidor antes de cadastrar usuários.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // abrir janela de cadastro (passando o PixClient conectado)
        CadastroGUI cadastro = new CadastroGUI(this, client);
        cadastro.setVisible(true);
    }

    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginGUI().setVisible(true);
        });
    }
}

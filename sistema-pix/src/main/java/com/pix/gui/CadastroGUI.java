package com.pix.gui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.MaskFormatter;

import com.pix.client.PixClient;

/**
 * Janela de cadastro de usuário. Recebe um PixClient já conectado (reutiliza a conexão).
 * É criada como modal a partir do LoginGUI.
 */
public class CadastroGUI extends JDialog {

    private PixClient client;
    private JFormattedTextField cpfField;
    private JTextField nomeField;
    private JPasswordField senhaField;
    private JButton cadastrarButton;
    private JButton cancelarButton;

    /**
     * @param owner frame proprietário (p.ex. LoginGUI)
     * @param client PixClient conectado (não nulo)
     */
    public CadastroGUI(JFrame owner, PixClient client) {
        super(owner, "Cadastrar Usuário", true);
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

        cadastrarButton = new JButton("Cadastrar");
        cancelarButton = new JButton("Cancelar");
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("CPF:"), gbc);
        gbc.gridx = 1;
        add(cpfField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1;
        add(nomeField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1;
        add(senhaField, gbc);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(cadastrarButton);
        buttons.add(cancelarButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(buttons, gbc);

        // Informação útil
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JTextArea info = new JTextArea("Regras:\n• Nome: 6-120 caracteres\n• Senha: 6-120 caracteres\n• CPF válido (apenas dígitos)");
        info.setEditable(false);
        info.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(info, gbc);
    }

    private void setupEventHandlers() {
        cadastrarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarCadastro();
            }
        });

        cancelarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CadastroGUI.this.dispose();
            }
        });
    }

    	private void realizarCadastro() {
    	    if (client == null) {
    	        JOptionPane.showMessageDialog(this, "Conexão não disponível. Volte à tela de login e conecte-se ao servidor.", "Erro", JOptionPane.ERROR_MESSAGE);
    	        return;
    	    }

    	    String cpfDigits = cpfField.getText();
    	    String nome = nomeField.getText().trim();
    	    String senha = new String(senhaField.getPassword());

    	    // Valida campos obrigatórios
    	    if (cpfDigits.isEmpty() || nome.isEmpty() || senha.isEmpty()) {
    	        JOptionPane.showMessageDialog(this, "Todos os campos são obrigatórios.", "Erro", JOptionPane.ERROR_MESSAGE);
    	        return;
    	    }

    	    // Valida CPF (11 dígitos)
    	    if (cpfDigits.length() != 14) {
    	        JOptionPane.showMessageDialog(this, "CPF inválido: informe 11 dígitos.", "Erro", JOptionPane.ERROR_MESSAGE);
    	        return;
    	    }


    	    // Validações adicionais
    	    if (nome.length() < 6 || nome.length() > 120) {
    	        JOptionPane.showMessageDialog(this, "Nome deve ter entre 6 e 120 caracteres.", "Erro", JOptionPane.ERROR_MESSAGE);
    	        return;
    	    }

    	    if (senha.length() < 6 || senha.length() > 120) {
    	        JOptionPane.showMessageDialog(this, "Senha deve ter entre 6 e 120 caracteres.", "Erro", JOptionPane.ERROR_MESSAGE);
    	        return;
    	    }

    	    try {
    	        // envia o CPF já formatado
    	        PixClient.OperationResult result = client.criarUsuario(cpfDigits, nome, senha);
    	        if (result.isSuccess()) {
    	            JOptionPane.showMessageDialog(this, "Usuário cadastrado com sucesso! Faça login para continuar.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    	            // limpar campos e fechar
    	            cpfField.setValue(null);
    	            nomeField.setText("");
    	            senhaField.setText("");
    	            this.dispose();
    	        } else {
    	            JOptionPane.showMessageDialog(this, result.getMessage(), "Erro no cadastro", JOptionPane.ERROR_MESSAGE);
    	        }
    	    } catch (Exception ex) {
    	        ex.printStackTrace();
    	        JOptionPane.showMessageDialog(this, "Erro ao cadastrar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    	    }
    	}

}

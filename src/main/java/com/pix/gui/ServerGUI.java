package com.pix.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.pix.server.PixServer;

/**
 * Interface gráfica para o servidor PIX integrado com banco de dados.
 */
public class ServerGUI extends JFrame {
    private PixServer server;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea logArea;
    private JLabel statusLabel;
    private boolean serverRunning = false;

    public ServerGUI() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        setTitle("Servidor PIX com Banco de Dados");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        startButton = new JButton("Iniciar Servidor");
        stopButton = new JButton("Parar Servidor");
        stopButton.setEnabled(false);
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        statusLabel = new JLabel("Status: Parado");
        statusLabel.setForeground(Color.RED);
        
        server = new PixServer(8080);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Panel superior com botões e status
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(startButton);
        topPanel.add(stopButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(statusLabel);
        
        // Área de log
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log do Servidor"));
        
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior com informações
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Porta: 8080 | Banco: sistema_pix | Host: localhost:3306"));
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });
    }

    private void startServer() {
        try {
            server.start();
            serverRunning = true;
            
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("Status: Rodando");
            statusLabel.setForeground(Color.GREEN);
            
            appendLog("Servidor iniciado na porta 8080");
            appendLog("Conectado ao banco de dados MySQL");
            appendLog("Aguardando conexões de clientes...");
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao iniciar servidor: " + ex.getMessage(), 
                "Erro", 
                JOptionPane.ERROR_MESSAGE);
            appendLog("ERRO: " + ex.getMessage());
        }
    }

    private void stopServer() {
        try {
            server.stop();
            serverRunning = false;
            
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Status: Parado");
            statusLabel.setForeground(Color.RED);
            
            appendLog("Servidor parado");
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao parar servidor: " + ex.getMessage(), 
                "Erro", 
                JOptionPane.ERROR_MESSAGE);
            appendLog("ERRO ao parar: " + ex.getMessage());
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) {
                // Usar look and feel padrão
            }
            
            new ServerGUI().setVisible(true);
        });
    }
}


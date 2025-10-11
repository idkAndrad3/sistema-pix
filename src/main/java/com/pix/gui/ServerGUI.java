package com.pix.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

// ---- FlatLaf ----
import com.formdev.flatlaf.FlatLightLaf;
import com.pix.dao.TransacaoDAO;
import com.pix.dao.UsuarioDAO;
import com.pix.model.Sessao;
import com.pix.model.Transacao;
import com.pix.model.Usuario;
import com.pix.server.PixServer;

/**
 * Interface gráfica para o servidor PIX integrado com banco de dados.
 * Abas: Log, Usuários, Transações, Clientes
 */
public class ServerGUI extends JFrame {
    private PixServer server;
    private JButton startButton;
    private JButton stopButton;
    private JButton saveLogButton;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JTextField portField;
    private JLabel connLabel;
    private boolean serverRunning = false;

    // Abas e tabelas
    private JTabbedPane tabbedPane;
    private JTable usuariosTable;
    private JTable transacoesTable;
    private JTable clientesTable;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ServerGUI() {
        // ---- aplica FlatLaf antes de inicializar ----
        try {
            FlatLightLaf.setup();
        } catch (Exception ex) {
            System.err.println("Falha ao aplicar FlatLaf: " + ex.getMessage());
        }

        initializeComponents();
        setupLayout();
        setupEventHandlers();

        setTitle("Servidor PIX com Banco de Dados");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
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

        saveLogButton = new JButton("Salvar Log");
        connLabel = new JLabel("Conexões: 0");

        portField = new JTextField(String.valueOf(25444), 6);

        // Tabelas
        usuariosTable = new JTable();
        transacoesTable = new JTable();
        clientesTable = new JTable();
    }

 // Local do arquivo: src/main/java/com/pix/gui/ServerGUI.java

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Panel superior com botões e status
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(startButton);
        topPanel.add(stopButton);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(new JLabel("Porta:"));
        topPanel.add(portField);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(connLabel);
        topPanel.add(Box.createHorizontalStrut(10));
        topPanel.add(saveLogButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);

        // Abas
        tabbedPane = new JTabbedPane();

        // --- Log tab ---
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));
        tabbedPane.addTab("Log", logScroll);

        // --- Usuários tab ---
        DefaultTableModel userModel = new DefaultTableModel(new Object[] {
            "CPF", "Nome", "Saldo", "Criado Em", "Atualizado Em"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        usuariosTable.setModel(userModel);
        JScrollPane up = new JScrollPane(usuariosTable);
        JPanel upPanel = new JPanel(new BorderLayout());
        upPanel.add(up, BorderLayout.CENTER);
        JButton refreshUsers = new JButton("Atualizar Usuários");
        refreshUsers.addActionListener(e -> refreshUsuarios());
        upPanel.add(refreshUsers, BorderLayout.SOUTH);
        tabbedPane.addTab("Usuários", upPanel);

        // --- Transações tab ---
        DefaultTableModel txModel = new DefaultTableModel(new Object[] {
            "ID", "Valor", "CPF Enviador", "CPF Recebedor", "Data"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        transacoesTable.setModel(txModel);
        JScrollPane tp = new JScrollPane(transacoesTable);
        JPanel tpPanel = new JPanel(new BorderLayout());
        tpPanel.add(tp, BorderLayout.CENTER);
        JButton refreshTx = new JButton("Atualizar Transações");
        refreshTx.addActionListener(e -> refreshTransacoes());
        tpPanel.add(refreshTx, BorderLayout.SOUTH);
        tabbedPane.addTab("Transações", tpPanel);

        // --- Clientes tab ---
        DefaultTableModel clientsModel = new DefaultTableModel(new Object[] {
            "ID Cliente", "IP", "Porta", "Hostname", "Status", "Conectado Em"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        clientesTable.setModel(clientsModel);
        JScrollPane cp = new JScrollPane(clientesTable);
        JPanel cpPanel = new JPanel(new BorderLayout());
        cpPanel.add(cp, BorderLayout.CENTER);
        JButton refreshClients = new JButton("Atualizar Clientes");
        refreshClients.addActionListener(e -> refreshClientes());
        cpPanel.add(refreshClients, BorderLayout.SOUTH);
        tabbedPane.addTab("Clientes", cpPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // ---- INÍCIO DA MODIFICAÇÃO ----
        // Panel inferior fixo com IP do servidor
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String serverIp = "IP não encontrado";
        try {
            // Tenta obter o IP da máquina local
            serverIp = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace(); // Imprime o erro no console
        }
        bottomPanel.add(new JLabel("IP do Servidor: " + serverIp));
        bottomPanel.add(new JLabel(" | Banco: sistema_pix | Host: localhost:3306"));
        add(bottomPanel, BorderLayout.SOUTH);
        // ---- FIM DA MODIFICAÇÃO ----
    }

    private void setupEventHandlers() {
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
                // start periodic client count update
                startClientUpdater();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });

        saveLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try (PrintWriter out = new PrintWriter(new FileWriter("server-log.txt", true))) {
                    out.println("---- Log salvo em " + java.time.LocalDateTime.now());
                    out.println(logArea.getText());
                    JOptionPane.showMessageDialog(ServerGUI.this, "Log salvo em server-log.txt");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(ServerGUI.this, "Erro ao salvar log: " + ex.getMessage());
                }
            }
        });
    }

    private Timer clientTimer;

    private void startClientUpdater() {
        if (clientTimer != null) clientTimer.stop();
        clientTimer = new Timer(1000, e -> {
            if (server != null) {
                connLabel.setText("Conexões: " + server.getActiveConnections());
                // se a aba Clientes estiver ativa, atualiza automaticamente
                if (tabbedPane.getSelectedIndex() == 3) {
                    refreshClientes();
                }
            }
        });
        clientTimer.start();
    }

    private void startServer() {
        try {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Porta inválida");
                return;
            }
            server = new com.pix.server.PixServer(port);
            Thread t = new Thread(() -> {
                try {
                    server.start();
                } catch (IOException e) {
                    appendLog("ERRO: " + e.getMessage());
            }
            }, "PixServer-Thread");
            t.setDaemon(true);
            t.start();

            serverRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            statusLabel.setText("Status: Rodando");
            statusLabel.setForeground(Color.GREEN);

            appendLog("Servidor iniciado na porta " + port);
            appendLog("Conectado ao banco de dados MySQL");
            appendLog("Aguardando conexões de clientes...");
            // atualiza tabelas iniciais
            refreshUsuarios();
            refreshTransacoes();
            refreshClientes();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao iniciar servidor: " + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE);
            appendLog("ERRO: " + ex.getMessage());
        }
    }

    private void stopServer() {
        try {
            if (server != null) {
                server.stop();
            }
            serverRunning = false;

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            statusLabel.setText("Status: Parado");
            statusLabel.setForeground(Color.RED);

            appendLog("Servidor parado");
            connLabel.setText("Conexões: 0");
            if (clientTimer != null) clientTimer.stop();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao parar servidor: " + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE);
            appendLog("ERRO ao parar: " + ex.getMessage());
        }
    }

    private void refreshUsuarios() {
        try {
            UsuarioDAO dao = new UsuarioDAO();
            List<Usuario> usuarios = dao.listarTodos();
            DefaultTableModel model = (DefaultTableModel) usuariosTable.getModel();
            model.setRowCount(0);
            for (Usuario u : usuarios) {
                String cpf = u.getCpf();
                String[] stamps = dao.getTimestamps(cpf);
                String criado = stamps != null && stamps[0] != null ? stamps[0].replace('T',' ') : "";
                String atualizado = stamps != null && stamps[1] != null ? stamps[1].replace('T',' ') : "";
                model.addRow(new Object[] { cpf, u.getNome(), u.getSaldo(), criado, atualizado });
            }
            appendLog("Usuários atualizados: " + usuarios.size());
        } catch (Exception e) {
            appendLog("Erro ao atualizar usuários: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshTransacoes() {
        try {
            TransacaoDAO dao = new TransacaoDAO();
            java.util.List<Transacao> txs = dao.listarTodas();
            DefaultTableModel model = (DefaultTableModel) transacoesTable.getModel();
            model.setRowCount(0);
            for (Transacao t : txs) {
                String data = t.getCriadoEm() != null ? t.getCriadoEm().format(dtf) : "";
                model.addRow(new Object[] { t.getId(), t.getValor(), t.getOrigem(), t.getDestino(), data });
            }
            appendLog("Transações atualizadas: " + txs.size());
        } catch (Exception e) {
            appendLog("Erro ao atualizar transações: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshClientes() {
        try {
            DefaultTableModel model = (DefaultTableModel) clientesTable.getModel();
            model.setRowCount(0);
            if (server == null) return;
            java.util.List<Sessao> lista = server.getClientes();
            for (Sessao s : lista) {
                String conectado = s.getConectadoEm() != null ? s.getConectadoEm().format(dtf) : "";
                model.addRow(new Object[] { s.getId(), s.getIp(), s.getPorta(), s.getHostname(), s.getStatus(), conectado });
            }
            appendLog("Clientes atualizados: " + lista.size());
        } catch (Exception e) {
            appendLog("Erro ao atualizar clientes: " + e.getMessage());
            e.printStackTrace();
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
            new ServerGUI().setVisible(true);
        });
    }
}

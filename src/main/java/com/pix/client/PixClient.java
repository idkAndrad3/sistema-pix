package com.pix.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class PixClient {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ObjectMapper mapper = new ObjectMapper();

    public PixClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao conectar: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao desconectar: " + e.getMessage());
        }
    }

    private JsonNode sendRequest(ObjectNode request) throws IOException {
        out.println(mapper.writeValueAsString(request));
        String responseStr = in.readLine();
        if (responseStr == null) {
            throw new IOException("Conexão encerrada pelo servidor.");
        }
        return mapper.readTree(responseStr);
    }

    // --- Classes de Resultado --- //

    public static class OperationResult {
        private boolean success;
        private String message;

        public OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class LoginResult extends OperationResult {
        private String token;

        public LoginResult(boolean success, String message, String token) {
            super(success, message);
            this.token = token;
        }

        public String getToken() { return token; }
    }

    public static class UserDataResult extends OperationResult {
        private String nome;
        private String cpf;
        private double saldo;

        public UserDataResult(boolean success, String message, String nome, String cpf, double saldo) {
            super(success, message);
            this.nome = nome;
            this.cpf = cpf;
            this.saldo = saldo;
        }

        public String getNome() { return nome; }
        public String getCpf() { return cpf; }
        public double getSaldo() { return saldo; }
    }

    public static class TransactionResult extends OperationResult {
        private String transacoesJson; // Manter como string para evitar complexidade de parsing aqui

        public TransactionResult(boolean success, String message, String transacoesJson) {
            super(success, message);
            this.transacoesJson = transacoesJson;
        }

        public String getTransacoes() { return transacoesJson; }
    }

    // --- Métodos de Operação --- //

    public OperationResult criarUsuario(String cpf, String nome, String senha) {
        try {
            ObjectNode req = mapper.createObjectNode();
            req.put("operacao", "usuario_criar");
            req.put("nome", nome);
            req.put("cpf", cpf);
            req.put("senha", senha);
            JsonNode resp = sendRequest(req);
            return new OperationResult(resp.path("status").asBoolean(), resp.path("info").asText());
        } catch (IOException e) {
            return new OperationResult(false, "Erro de comunicação: " + e.getMessage());
        }
    }

    public LoginResult login(String cpf, String senha) {
        try {
            ObjectNode req = mapper.createObjectNode();
            req.put("operacao", "usuario_login");
            req.put("cpf", cpf);
            req.put("senha", senha);
            JsonNode resp = sendRequest(req);
            boolean status = resp.path("status").asBoolean();
            String info = resp.path("info").asText();
            String token = status ? resp.path("dados").path("token").asText() : null;
            return new LoginResult(status, info, token);
        } catch (IOException e) {
            return new LoginResult(false, "Erro de comunicação: " + e.getMessage(), null);
        }
    }

    public OperationResult logout(String token) {
        try {
            ObjectNode req = mapper.createObjectNode();
            req.put("operacao", "usuario_logout");
            req.put("token", token);
            JsonNode resp = sendRequest(req);
            return new OperationResult(resp.path("status").asBoolean(), resp.path("info").asText());
        } catch (IOException e) {
            return new OperationResult(false, "Erro de comunicação: " + e.getMessage());
        }
    }

    public UserDataResult lerUsuario(String token) {
        try {
            ObjectNode req = mapper.createObjectNode();
            req.put("operacao", "usuario_ler");
            req.put("token", token);

            JsonNode resp = sendRequest(req);
            boolean status = resp.path("status").asBoolean(false);
            String info = resp.path("info").asText("");

            if (status) {
                JsonNode dadosNode = resp.path("dados");
                // aceita duas estruturas: { dados: { usuario: { ... } } } ou { dados: { ... } }
                JsonNode usuarioNode = dadosNode.has("usuario") ? dadosNode.path("usuario") : dadosNode;

                String nome = usuarioNode.path("nome").asText("");
                String cpf = usuarioNode.path("cpf").asText("");
                double saldo = usuarioNode.path("saldo").asDouble(0.0);

                return new UserDataResult(true, info, nome, cpf, saldo);
            } else {
                return new UserDataResult(false, info, "", "", 0.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new UserDataResult(false, "Erro: " + e.getMessage(), "", "", 0.0);
        }
    }

    public OperationResult atualizarUsuario(String token, String novoNome, String novaSenha) {
        try {
            ObjectNode req = mapper.createObjectNode();
            req.put("operacao", "usuario_atualizar");
            req.put("token", token);
            ObjectNode usuarioData = mapper.createObjectNode();
            if (novoNome != null && !novoNome.isEmpty()) {
                usuarioData.put("nome", novoNome);
            }
            if (novaSenha != null && !novaSenha.isEmpty()) {
                usuarioData.put("senha", novaSenha);
            }
            req.set("usuario", usuarioData);
            JsonNode resp = sendRequest(req);
            return new OperationResult(resp.path("status").asBoolean(), resp.path("info").asText());
        } catch (IOException e) {
            return new OperationResult(false, "Erro de comunicação: " + e.getMessage());
        }
    }

    public OperationResult criarTransacao(String token, double valor, String cpfDestino) {
        try {
            ObjectNode req = mapper.createObjectNode();
            req.put("operacao", "transacao_criar");
            req.put("token", token);
            req.put("valor", valor);
            req.put("cpf_destino", cpfDestino);
            JsonNode resp = sendRequest(req);
            return new OperationResult(resp.path("status").asBoolean(), resp.path("info").asText());
        } catch (IOException e) {
            return new OperationResult(false, "Erro de comunicação: " + e.getMessage());
        }
    }

    public OperationResult depositar(String token, double valor) {
        try {
            ObjectNode req = mapper.createObjectNode();
            req.put("operacao", "depositar");
            req.put("token", token);
            req.put("valor_enviado", valor);
            JsonNode resp = sendRequest(req);
            return new OperationResult(resp.path("status").asBoolean(), resp.path("info").asText());
        } catch (IOException e) {
            return new OperationResult(false, "Erro de comunicação: " + e.getMessage());
        }
    }

    public TransactionResult lerTransacoes(String token, String dataInicial, String dataFinal) {
        try {
            ObjectNode req = mapper.createObjectNode();
            req.put("operacao", "transacao_ler");
            req.put("token", token);
            req.put("data_inicial", dataInicial);
            req.put("data_final", dataFinal);
            JsonNode resp = sendRequest(req);
            boolean status = resp.path("status").asBoolean();
            String info = resp.path("info").asText();
            String transacoesJson = status ? resp.path("dados").path("transacoes").toString() : null;
            return new TransactionResult(status, info, transacoesJson);
        } catch (IOException e) {
            return new TransactionResult(false, "Erro de comunicação: " + e.getMessage(), null);
        }
    }
}


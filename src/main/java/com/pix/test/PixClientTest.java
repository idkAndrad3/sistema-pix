package com.pix.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.Socket;

public class PixClientTest {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println(">>> Rodando testes do sistema PIX...\n");

        try (
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
        ) {
            System.out.println("[OK] Conectado ao servidor Pix\n");

            // Usuário A

            System.out.println("### Teste 1: Cadastro de usuário ###");
            String tokenJoao = criarUsuario(reader, writer, "Paulo Plinio", "123.456.789-00", "senha123");

            // Usuário B
            System.out.println("\n### Teste 2: Cadastro de outro usuário ###");
            String tokenMaria = criarUsuario(reader, writer, "Maicou Jequison", "987.654.321-00", "senha456");

            // Login João
            System.out.println("\n### Teste 3: Login do usuário ###");
            tokenJoao = autenticar(reader, writer, "123.456.789-00", "senha123");

            // Consultar usuário
            System.out.println("\n### Teste 4: Consultar perfil ###");
            consultarUsuario(reader, writer, tokenJoao);

            // Depositar
            System.out.println("\n### Teste 5: Realizar depósito ###");
            depositar(reader, writer, tokenJoao, 1500.0);

            // Transferência
            System.out.println("\n### Teste 6: Efetuar transferência ###");
            transferir(reader, writer, tokenJoao, "987.654.321-00", 300.0);

            // Extrato
            System.out.println("\n### Teste 7: Consultar extrato ###");
            consultarExtrato(reader, writer, tokenJoao);

            // Atualizar dados
            System.out.println("\n### Teste 8: Alterar dados cadastrais ###");
            atualizarUsuario(reader, writer, tokenJoao, "Maicou Jordao", "novaSenha123");

            // Logout
            System.out.println("\n### Teste 9: Encerrar sessão ###");
            logout(reader, writer, tokenJoao);

            // Login João
            System.out.println("\n### Teste 3: Login do usuário ###");
            tokenJoao = autenticar(reader, writer, "123.456.789-00", "senha123");
            
        } catch (IOException e) {
            System.err.println("Erro durante os testes: " + e.getMessage());
        }
    }

    private static String criarUsuario(BufferedReader in, PrintWriter out, String nome, String cpf, String senha) throws IOException {
        ObjectNode req = JSON.createObjectNode();
        req.put("operacao", "usuario_criar");
        req.put("nome", nome);
        req.put("cpf", cpf);
        req.put("senha", senha);

        out.println(JSON.writeValueAsString(req));
        JsonNode resp = JSON.readTree(in.readLine());

        exibirResposta(resp);
        return resp.path("status").asBoolean() ? "ok" : null;
    }

    private static String autenticar(BufferedReader in, PrintWriter out, String cpf, String senha) throws IOException {
        ObjectNode req = JSON.createObjectNode();
        req.put("operacao", "usuario_login");
        req.put("cpf", cpf);
        req.put("senha", senha);

        out.println(JSON.writeValueAsString(req));
        JsonNode resp = JSON.readTree(in.readLine());

        exibirResposta(resp);
        String token = resp.path("dados").path("token").asText();
        if (!token.isEmpty()) {
            System.out.println("Token recebido: " + token.substring(0, Math.min(6, token.length())) + "...");
        }
        return resp.path("status").asBoolean() ? token : null;
    }

    private static void consultarUsuario(BufferedReader in, PrintWriter out, String token) throws IOException {
        ObjectNode req = JSON.createObjectNode();
        req.put("operacao", "usuario_ler");
        req.put("token", token);

        out.println(JSON.writeValueAsString(req));
        JsonNode resp = JSON.readTree(in.readLine());

        exibirResposta(resp);
        if (resp.path("status").asBoolean()) {
            JsonNode user = resp.path("dados").path("usuario");
            System.out.printf("Usuário: %s | CPF: %s | Saldo: R$ %.2f\n",
                    user.path("nome").asText(),
                    user.path("cpf").asText(),
                    user.path("saldo").asDouble());
        }
    }

    private static void depositar(BufferedReader in, PrintWriter out, String token, double valor) throws IOException {
        ObjectNode req = JSON.createObjectNode();
        req.put("operacao", "depositar");
        req.put("token", token);
        req.put("valor_enviado", valor);

        out.println(JSON.writeValueAsString(req));
        JsonNode resp = JSON.readTree(in.readLine());

        exibirResposta(resp);
        System.out.printf("Depósito de R$ %.2f concluído\n", valor);
    }

    private static void transferir(BufferedReader in, PrintWriter out, String token, String cpfDestino, double valor) throws IOException {
        ObjectNode req = JSON.createObjectNode();
        req.put("operacao", "transacao_criar");
        req.put("token", token);
        req.put("cpf_destino", cpfDestino);
        req.put("valor", valor);

        out.println(JSON.writeValueAsString(req));
        JsonNode resp = JSON.readTree(in.readLine());

        exibirResposta(resp);
        System.out.printf("Transferência de R$ %.2f para CPF %s\n", valor, cpfDestino);
    }

    private static void consultarExtrato(BufferedReader in, PrintWriter out, String token) throws IOException {
        ObjectNode req = JSON.createObjectNode();
        req.put("operacao", "transacao_ler");
        req.put("token", token);
        req.put("data_inicial", "2025-09-01T00:00:00Z");
        req.put("data_final", "2025-09-30T23:59:59Z");

        out.println(JSON.writeValueAsString(req));
        JsonNode resp = JSON.readTree(in.readLine());

        exibirResposta(resp);
        if (resp.path("status").asBoolean()) {
            for (JsonNode tx : resp.path("dados").path("transacoes")) {
                System.out.printf("ID %d | Valor: R$ %.2f | Data: %s\n",
                        tx.path("id").asLong(),
                        tx.path("valor").asDouble(),
                        tx.path("criado_em").asText());
            }
        }
    }

    private static void atualizarUsuario(BufferedReader in, PrintWriter out, String token, String novoNome, String novaSenha) throws IOException {
        ObjectNode req = JSON.createObjectNode();
        req.put("operacao", "usuario_atualizar");
        req.put("token", token);

        ObjectNode usuario = JSON.createObjectNode();
        usuario.put("nome", novoNome);
        usuario.put("senha", novaSenha);
        req.set("usuario", usuario);

        out.println(JSON.writeValueAsString(req));
        JsonNode resp = JSON.readTree(in.readLine());

        exibirResposta(resp);
        System.out.println("Novo nome salvo: " + novoNome);
    }

    private static void logout(BufferedReader in, PrintWriter out, String token) throws IOException {
        ObjectNode req = JSON.createObjectNode();
        req.put("operacao", "usuario_logout");
        req.put("token", token);

        out.println(JSON.writeValueAsString(req));
        exibirResposta(JSON.readTree(in.readLine()));
    }

    private static void exibirResposta(JsonNode resp) {
        boolean status = resp.path("status").asBoolean();
        System.out.println("Status: " + (status ? "OK" : "FALHA"));
        System.out.println("Mensagem: " + resp.path("info").asText());
    }
}

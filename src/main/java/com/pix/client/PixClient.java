package com.pix.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pix.dao.TransacaoDAO;
import com.pix.dao.UsuarioDAO;
import com.pix.model.RespostaBase;
import com.pix.model.Transacao;
import com.pix.model.Usuario;
import com.pix.service.TokenManager;

public class PixClient {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ObjectMapper mapper = new ObjectMapper();
	private static final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final UsuarioDAO usuarioDAO = new UsuarioDAO();
	private static final TransacaoDAO transacaoDAO = new TransacaoDAO();

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
            String transacoesJson = null;
            if (status && resp.has("transacoes")) {
                transacoesJson = resp.get("transacoes").toString();
            }
            
            return new TransactionResult(status, info, transacoesJson);
        } catch (IOException e) {
            return new TransactionResult(false, "Erro de comunicação: " + e.getMessage(), null);
        }
    
    }
	private static String validateToken(JsonNode req) {
		String token = req.path("token").asText("");
		return TokenManager.validateToken(token);
	}

	// Operações

	public static RespostaBase opUsuarioCriar(JsonNode req) {
		String nome = req.path("nome").asText("").trim();
		String cpf = req.path("cpf").asText("").trim();
		String senha = req.path("senha").asText("").trim();

		if (nome.isEmpty() || cpf.isEmpty() || senha.isEmpty()) {
			return new RespostaBase("usuario_criar", false, "Nome, CPF e senha são obrigatórios");
		}

		if (nome.length() < 6 || nome.length() > 120) {
			return new RespostaBase("usuario_criar", false, "Nome deve ter entre 6 e 120 caracteres");
		}

		if (senha.length() < 6 || senha.length() > 120) {
			return new RespostaBase("usuario_criar", false, "Senha deve ter entre 6 e 120 caracteres");
		}

		// Verificar se usuário já existe no banco
		Usuario existente = usuarioDAO.buscarPorCpf(cpf);
		if (existente != null) {
			return new RespostaBase("usuario_criar", false, "Usuário já existente");
		}

		// Criar e salvar novo usuário
		Usuario u = new Usuario(nome, cpf, senha);
		usuarioDAO.salvar(u);
		
		return new RespostaBase("usuario_criar", true, "Usuário criado com sucesso");
	}

	public static RespostaBase opUsuarioLogin(JsonNode req) {
		String cpf = req.path("cpf").asText("").trim();
		String senha = req.path("senha").asText("").trim();

		if (cpf.isEmpty() || senha.isEmpty()) {
			return new RespostaBase("usuario_login", false, "CPF e senha são obrigatórios");
		}

		Usuario u = usuarioDAO.buscarPorCpf(cpf);
		if (u == null) {
			return new RespostaBase("usuario_login", false, "Usuário inexistente");
		}

		if (!u.getSenha().equals(senha)) {
			return new RespostaBase("usuario_login", false, "Senha inválida");
		}

		String token = TokenManager.generateToken(cpf);
		RespostaBase r = new RespostaBase("usuario_login", true, "Login bem-sucedido");
		r.setToken(token);
		r.getDados().put("token", token);
		return r;
	}

	public static RespostaBase opUsuarioLogout(JsonNode req) {
		String token = req.path("token").asText("");

		if (token.isEmpty()) {
			return new RespostaBase("usuario_logout", false, "Token é obrigatório");
		}

		boolean removed = TokenManager.removeToken(token);

		
		if (removed) {
			return new RespostaBase("usuario_logout", true, "Logout realizado com sucesso");
		} else {
			return new RespostaBase("usuario_logout", false, "Token inválido ou expirado");
		}
	}

	public static RespostaBase opUsuarioLer(JsonNode req) {
		String cpf = validateToken(req);
		if (cpf == null) {
			return new RespostaBase("usuario_ler", false, "Token inválido ou expirado");
		}

		// Buscar usuário atualizado no banco
		Usuario u = usuarioDAO.buscarPorCpf(cpf);
		if (u == null) {
			return new RespostaBase("usuario_ler", false, "Usuário não encontrado");
		}

		RespostaBase r = new RespostaBase("usuario_ler", true, "Dados do usuário");
		java.util.Map<String, Object> usuarioMap = new java.util.HashMap<>();
		usuarioMap.put("nome", u.getNome());
		usuarioMap.put("cpf", u.getCpf());
		usuarioMap.put("saldo", u.getSaldo());
		r.setUsuario(usuarioMap);
		r.getDados().put("usuario", usuarioMap);
		return r;

	}

	public static RespostaBase opTransacaoCriar(JsonNode req) {
		String cpf = validateToken(req);
		if (cpf == null) {
			return new RespostaBase("transacao_criar", false, "Token inválido ou expirado");
		}

		String cpfDestino = req.path("cpf_destino").asText("").trim();
		double valor = req.path("valor").asDouble(0);

		if (cpfDestino.isEmpty()) {
			return new RespostaBase("transacao_criar", false, "CPF de destino é obrigatório");
		}

		if (valor <= 0) {
			return new RespostaBase("transacao_criar", false, "Valor deve ser positivo");
		}

		if (cpf.equals(cpfDestino)) {
			return new RespostaBase("transacao_criar", false, "Não é possível transferir para si mesmo");
		}

		// Verificar se usuário de origem existe e tem saldo
		Usuario origem = usuarioDAO.buscarPorCpf(cpf);
		if (origem == null) {
			return new RespostaBase("transacao_criar", false, "Usuário de origem não encontrado");
		}

		if (origem.getSaldo() < valor) {
			return new RespostaBase("transacao_criar", false, "Saldo insuficiente");
		}

		// Verificar se usuário de destino existe
		Usuario destino = usuarioDAO.buscarPorCpf(cpfDestino);
		if (destino == null) {
			return new RespostaBase("transacao_criar", false, "Usuário de destino não encontrado");
		}

		// Criar e salvar transação (o DAO já atualiza os saldos)
		Transacao t = new Transacao(cpf, cpfDestino, valor);
		transacaoDAO.salvar(t);

		RespostaBase r = new RespostaBase("transacao_criar", true, "Transação realizada com sucesso");
		r.getDados().put("id", t.getId());
		r.getDados().put("valor", valor);
		r.getDados().put("data_hora", t.getCriadoEm().format(dtf));
		return r;
	}

	public static RespostaBase opTransacaoLer(JsonNode req) {
	    String cpf = validateToken(req);
	    if (cpf == null) {
	        return new RespostaBase("transacao_ler", false, "Token inválido ou expirado");
	    }

	    // Extrair e validar datas
	    String dataInicialStr = req.path("data_inicial").asText("").trim();
	    String dataFinalStr = req.path("data_final").asText("").trim();

	    if (dataInicialStr.isEmpty() || dataFinalStr.isEmpty()) {
	        return new RespostaBase("transacao_ler", false, "Datas inicial e final são obrigatórias");
	    }

	    LocalDateTime dataInicial;
	    LocalDateTime dataFinal;
	    try {
	        // Usar formatter que aceita formato UTC com 'Z'
	        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
	        dataInicial = LocalDateTime.ofInstant(Instant.from(formatter.parse(dataInicialStr)), ZoneOffset.UTC);
	        dataFinal = LocalDateTime.ofInstant(Instant.from(formatter.parse(dataFinalStr)), ZoneOffset.UTC);
	    } catch (Exception e) {
	        return new RespostaBase("transacao_ler", false, "Formato de data inválido. Use o formato ISO 8601 UTC (ex: 2024-05-01T00:00:00Z)");
	    }

	    // Buscar transações do usuário no intervalo
	    List<Transacao> transacoes = transacaoDAO.listarPorCpfEData(cpf, dataInicial, dataFinal);

	    // Preparar a resposta
	    RespostaBase r = new RespostaBase("transacao_ler", true, "Transações listadas com sucesso");
	    List<Map<String, Object>> transacoesList = new ArrayList<>();

	    for (Transacao t : transacoes) {
	        Map<String, Object> transacaoMap = new HashMap<>();

	        transacaoMap.put("id", t.getId());
	        transacaoMap.put("valor", t.getValor());

	        // Buscar usuário enviador
	        Usuario enviador = usuarioDAO.buscarPorCpf(t.getCpfOrigem());
	        Map<String, String> enviadorMap = new HashMap<>();
	        enviadorMap.put("cpf", enviador.getCpf());
	        enviadorMap.put("nome", enviador.getNome());
	        transacaoMap.put("usuario_enviador", enviadorMap);

	        // Buscar usuário recebedor
	        Usuario recebedor = usuarioDAO.buscarPorCpf(t.getCpfDestino());
	        Map<String, String> recebedorMap = new HashMap<>();
	        recebedorMap.put("cpf", recebedor.getCpf());
	        recebedorMap.put("nome", recebedor.getNome());
	        transacaoMap.put("usuario_recebedor", recebedorMap);

	        // Formatar datas no formato UTC com 'Z'
	        transacaoMap.put("criado_em", t.getCriadoEm().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
	        transacaoMap.put("atualizado_em", t.getAtualizadoEm().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

	        transacoesList.add(transacaoMap);
	    }

	    r.setTransacoes(transacoesList);
	    return r;
	}

	public static RespostaBase opDepositar(JsonNode req) {
		String cpf = validateToken(req);
		if (cpf == null) {
			return new RespostaBase("depositar", false, "Token inválido ou expirado");
		}

		double valor = req.path("valor_enviado").asDouble(0);

		if (valor <= 0) {
			return new RespostaBase("depositar", false, "Valor deve ser positivo");
		}

		// Buscar usuário no banco
		Usuario u = usuarioDAO.buscarPorCpf(cpf);
		if (u == null) {
			return new RespostaBase("depositar", false, "Usuário não encontrado");
		}

		// Atualizar saldo
		u.addSaldo(valor);
		usuarioDAO.atualizar(u);

		RespostaBase r = new RespostaBase("depositar", true, "Depósito realizado com sucesso");
		r.getDados().put("novo_saldo", u.getSaldo());
		return r;
	}

	public static RespostaBase opUsuarioAtualizar(JsonNode req) {
		// Valida token e obtém o CPF do usuário dono do token
		String cpf = validateToken(req);
		if (cpf == null) {
			return new RespostaBase("usuario_atualizar", false, "Token inválido ou expirado");
		}

		// aceitar duas formas: { usuario: { nome, senha } } ou { nome, senha }
		// diretamente
		JsonNode usuarioNode = req.has("usuario") ? req.path("usuario") : req;

		String novoNome = usuarioNode.path("nome").asText(null);
		String novaSenha = usuarioNode.path("senha").asText(null);

		// Se nenhum campo para atualizar, retorna erro
		if ((novoNome == null || novoNome.trim().isEmpty()) && (novaSenha == null || novaSenha.trim().isEmpty())) {
			return new RespostaBase("usuario_atualizar", false, "Nenhum campo para atualizar");
		}

		// Buscar o usuário atual no banco
		Usuario u = usuarioDAO.buscarPorCpf(cpf);
		if (u == null) {
			return new RespostaBase("usuario_atualizar", false, "Usuário não encontrado");
		}

		// Atualizar campos (preservar saldo e CPF)
		if (novoNome != null && !novoNome.trim().isEmpty()) {
			// validações básicas (opcional)
			if (novoNome.length() < 6 || novoNome.length() > 120) {
				return new RespostaBase("usuario_atualizar", false, "Nome deve ter entre 6 e 120 caracteres");
			}
			u.setNome(novoNome.trim());
		}
		if (novaSenha != null && !novaSenha.trim().isEmpty()) {
			if (novaSenha.length() < 6 || novaSenha.length() > 120) {
				return new RespostaBase("usuario_atualizar", false, "Senha deve ter entre 6 e 120 caracteres");
			}
			u.setSenha(novaSenha.trim());
		}

		// Persistir alteração no banco
		usuarioDAO.atualizar(u);

		// Responder sucesso
		return new RespostaBase("usuario_atualizar", true, "Dados atualizados com sucesso");
	}
}


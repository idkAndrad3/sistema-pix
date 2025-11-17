package com.pix.client;

import java.io.BufferedReader;
import validador.Validator;
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

	public OperationResult verificarConexaoServidor() {
		try {
			ObjectNode req = mapper.createObjectNode();
			req.put("operacao", "conectar");
			JsonNode resp = sendRequest(req);
			return new OperationResult(resp.path("status").asBoolean(), resp.path("info").asText());
		} catch (IOException e) {
			return new OperationResult(false, "Erro de comunicação: " + e.getMessage());
		}
	}

	private JsonNode sendRequest(ObjectNode request) throws IOException {

		String operacaoOriginal = request.path("operacao").asText("desconhecida");

		try {

			if (out == null) {
				throw new IOException("Cliente não está conectado (PrintWriter nulo).");
			}
			
			String jsonRequest = mapper.writeValueAsString(request);
			System.out.println("\n>> [CLIENTE ENVIANDO] " + jsonRequest);
			out.println(jsonRequest);
			if (in == null) {
				throw new IOException("Cliente não está conectado (BufferedReader nulo).");
			}
			String responseStr = in.readLine();
			System.out.println("<< [CLIENTE RECEBENDO] " + responseStr);
			if (responseStr == null) {
				disconnect();
				throw new IOException("Conexão encerrada pelo servidor.");
			}

			JsonNode responseNode;
			try {
				responseNode = mapper.readTree(responseStr);
			} catch (Exception e) {

				String infoErro = "Erro de sintaxe. A resposta do servidor não é um JSON válido: " + responseStr;
				reportarErroServidor(operacaoOriginal, infoErro);
				disconnect();
				throw new IOException(infoErro, e);

			}

			try {
				Validator.validateServer(responseStr);
			} catch (Exception e) {
				String infoErro = "Resposta inválida do servidor (não conforme ao protocolo): " + e.getMessage();
				reportarErroServidor(operacaoOriginal, infoErro);
				disconnect();
				throw new IOException(infoErro + " Conexão encerrada.", e);

			}

			return responseNode;

		} catch (IOException e) {
			disconnect();

			throw new IOException("Erro crítico de comunicação: " + e.getMessage(), e);
		}
	}

	private void reportarErroServidor(String operacaoOriginal, String info) {
		try {
			if (out == null) {

				System.err.println("[CLIENTE] Quis reportar erro, mas já estava desconectado: " + info);
				return;
			}

			ObjectNode erroReq = mapper.createObjectNode();
			erroReq.put("operacao", "erro_servidor");
			erroReq.put("operacao_enviada", operacaoOriginal);
			erroReq.put("info", info);

			String jsonErro = mapper.writeValueAsString(erroReq);
			System.err.println("[CLIENTE] Reportando erro ao servidor: " + jsonErro);

			out.println(jsonErro);

		} catch (Exception e) {

			System.err.println("Falha crítica ao tentar reportar erro ao servidor: " + e.getMessage());
		}
	}

	public static class OperationResult {
		private boolean success;
		private String message;

		public OperationResult(boolean success, String message) {
			this.success = success;
			this.message = message;
		}

		public boolean isSuccess() {
			return success;
		}

		public String getMessage() {
			return message;
		}
	}

	public static class LoginResult extends OperationResult {
		private String token;

		public LoginResult(boolean success, String message, String token) {
			super(success, message);
			this.token = token;
		}

		public String getToken() {
			return token;
		}
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

		public String getNome() {
			return nome;
		}

		public String getCpf() {
			return cpf;
		}

		public double getSaldo() {
			return saldo;
		}
	}

	public static class TransactionResult extends OperationResult {
		private String transacoesJson;

		public TransactionResult(boolean success, String message, String transacoesJson) {
			super(success, message);
			this.transacoesJson = transacoesJson;
		}

		public String getTransacoes() {
			return transacoesJson;
		}
	}

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
			String token = null;
			if (status) {

				if (resp.has("token") && !resp.path("token").asText().isEmpty()) {
					token = resp.path("token").asText();
				} else {
					token = null;
				}
			}

			System.out.println(
					"[DEBUG] login() -> status=" + status + " token='" + (token == null ? "null" : token) + "'");
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

				JsonNode usuarioNode = resp.path("usuario");

				if (usuarioNode.isNull() || usuarioNode.isMissingNode()) {

					reportarErroServidor("usuario_ler", "O campo 'usuario' veio nulo ou ausente na resposta.");

					return new UserDataResult(false,
							"Falha de protocolo: O servidor enviou uma resposta incompleta (usuário nulo).", "", "",
							0.0);
				}

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

	public OperationResult deletarUsuario(String token) {
		try {
			ObjectNode req = mapper.createObjectNode();
			req.put("operacao", "usuario_deletar");
			req.put("token", token);
			JsonNode resp = sendRequest(req);
			return new OperationResult(resp.path("status").asBoolean(), resp.path("info").asText());
		} catch (IOException e) {
			return new OperationResult(false, "Erro de comunicação: " + e.getMessage());
		}
	}

	public static RespostaBase opUsuarioDeletar(JsonNode req) {
		String cpf = validateToken(req);
		if (cpf == null) {
			return new RespostaBase("usuario_deletar", false, "Token inválido ou expirado");
		}

		boolean deletado = usuarioDAO.deletar(cpf);

		if (deletado) {
			TokenManager.removeToken(req.path("token").asText(""));
			return new RespostaBase("usuario_deletar", true, "Usuário deletado com sucesso.");
		} else {
			return new RespostaBase("usuario_deletar", false, "Erro ao deletar usuário.");
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

		Usuario existente = usuarioDAO.buscarPorCpf(cpf);
		if (existente != null) {
			return new RespostaBase("usuario_criar", false, "Usuário já existente");
		}

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

		Usuario origem = usuarioDAO.buscarPorCpf(cpf);
		if (origem == null) {
			return new RespostaBase("transacao_criar", false, "Usuário de origem não encontrado");
		}

		if (origem.getSaldo() < valor) {
			return new RespostaBase("transacao_criar", false, "Saldo insuficiente");
		}

		Usuario destino = usuarioDAO.buscarPorCpf(cpfDestino);
		if (destino == null) {
			return new RespostaBase("transacao_criar", false, "Usuário de destino não encontrado");
		}

		Transacao t = new Transacao(cpf, cpfDestino, valor);
		transacaoDAO.salvar(t);

		RespostaBase r = new RespostaBase("transacao_criar", true, "Transação realizada com sucesso");
		return r;
	}

	public static RespostaBase opTransacaoLer(JsonNode req) {
		String cpf = validateToken(req);
		if (cpf == null) {
			return new RespostaBase("transacao_ler", false, "Token inválido ou expirado");
		}

		String dataInicialStr = req.path("data_inicial").asText("").trim();
		String dataFinalStr = req.path("data_final").asText("").trim();

		if (dataInicialStr.isEmpty() || dataFinalStr.isEmpty()) {
			return new RespostaBase("transacao_ler", false, "Datas inicial e final são obrigatórias");
		}

		LocalDateTime dataInicial;
		LocalDateTime dataFinal;
		try {

			DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
			dataInicial = LocalDateTime.ofInstant(Instant.from(formatter.parse(dataInicialStr)), ZoneOffset.UTC);
			dataFinal = LocalDateTime.ofInstant(Instant.from(formatter.parse(dataFinalStr)), ZoneOffset.UTC);
		} catch (Exception e) {
			return new RespostaBase("transacao_ler", false,
					"Formato de data inválido. Use o formato ISO 8601 UTC (ex: 2024-05-01T00:00:00Z)");
		}
		if (dataInicial.plusDays(31).isBefore(dataFinal)) {
			return new RespostaBase("transacao_ler", false, "O período de consulta não pode exceder 31 dias.");
		}

		List<Transacao> transacoes = transacaoDAO.listarPorCpfEData(cpf, dataInicial, dataFinal);

		RespostaBase r = new RespostaBase("transacao_ler", true, "Transações listadas com sucesso");
		List<Map<String, Object>> transacoesList = new ArrayList<>();

		for (Transacao t : transacoes) {
			Map<String, Object> transacaoMap = new HashMap<>();

			transacaoMap.put("id", t.getId());
			transacaoMap.put("valor_enviado", t.getValor());

			Usuario enviador = usuarioDAO.buscarPorCpf(t.getCpfOrigem());
			Map<String, String> enviadorMap = new HashMap<>();
			if (enviador != null) {
				enviadorMap.put("cpf", enviador.getCpf());
				enviadorMap.put("nome", enviador.getNome());
			} else {
				enviadorMap.put("cpf", t.getCpfOrigem());
				enviadorMap.put("nome", "Usuário Deletado");
			}
			transacaoMap.put("usuario_enviador", enviadorMap);

			Usuario recebedor = usuarioDAO.buscarPorCpf(t.getCpfDestino());
			Map<String, String> recebedorMap = new HashMap<>();
			if (recebedor != null) {
				recebedorMap.put("cpf", recebedor.getCpf());
				recebedorMap.put("nome", recebedor.getNome());
			} else {
				recebedorMap.put("cpf", t.getCpfDestino());
				recebedorMap.put("nome", "Usuário Deletado");
			}
			transacaoMap.put("usuario_recebedor", recebedorMap);

			transacaoMap.put("criado_em",
					t.getCriadoEm().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
			transacaoMap.put("atualizado_em",
					t.getAtualizadoEm().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));

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

		Usuario u = usuarioDAO.buscarPorCpf(cpf);
		if (u == null) {
			return new RespostaBase("depositar", false, "Usuário não encontrado");
		}
		Transacao t = new Transacao(cpf, cpf, valor);
		transacaoDAO.salvar(t);

		u.addSaldo(valor);
		usuarioDAO.atualizar(u);

		RespostaBase r = new RespostaBase("depositar", true, "Depósito realizado com sucesso");
		return r;
	}

	public static RespostaBase opUsuarioAtualizar(JsonNode req) {

		String cpf = validateToken(req);
		if (cpf == null) {
			return new RespostaBase("usuario_atualizar", false, "Token inválido ou expirado");
		}

		JsonNode usuarioNode = req.has("usuario") ? req.path("usuario") : req;

		String novoNome = usuarioNode.path("nome").asText(null);
		String novaSenha = usuarioNode.path("senha").asText(null);

		if ((novoNome == null || novoNome.trim().isEmpty()) && (novaSenha == null || novaSenha.trim().isEmpty())) {
			return new RespostaBase("usuario_atualizar", false, "Nenhum campo para atualizar");
		}

		Usuario u = usuarioDAO.buscarPorCpf(cpf);
		if (u == null) {
			return new RespostaBase("usuario_atualizar", false, "Usuário não encontrado");
		}

		if (novoNome != null && !novoNome.trim().isEmpty()) {

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

		usuarioDAO.atualizar(u);

		return new RespostaBase("usuario_atualizar", true, "Dados atualizados com sucesso");
	}
}

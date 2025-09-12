package com.pix.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pix.dao.TransacaoDAO;
import com.pix.dao.UsuarioDAO;
import com.pix.model.RespostaBase;
import com.pix.model.Transacao;
import com.pix.model.Usuario;
import com.pix.service.TokenManager;
import validador.Validator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servidor PIX integrado com banco de dados MySQL. Esta versão substitui o
 * armazenamento em memória por persistência no banco.
 */
public class PixServer {
	private final int port;
	private ServerSocket serverSocket;
	private boolean running = false;
	private Thread serverThread;

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final UsuarioDAO usuarioDAO = new UsuarioDAO();
	private static final TransacaoDAO transacaoDAO = new TransacaoDAO();

	public PixServer(int port) {
		this.port = port;
	}
	
	

	public void start() throws IOException {
		if (running)
			return;

		serverSocket = new ServerSocket(port);
		running = true;

		serverThread = new Thread(() -> {
			System.out.println("Servidor Pix com BD rodando na porta " + port);
			while (running) {
				try {
					Socket clientSocket = serverSocket.accept();
					new Thread(() -> handleClient(clientSocket)).start();
				} catch (IOException e) {
					if (running) {
						System.err.println("Erro no servidor: " + e.getMessage());
					}
				}
			}
		});
		serverThread.start();
	}

	public void stop() throws IOException {
		running = false;
		if (serverSocket != null) {
			serverSocket.close();
		}
		if (serverThread != null) {
			serverThread.interrupt();
		}
	}

	private void handleClient(Socket socket) {
		String remote = socket.getRemoteSocketAddress().toString();
		System.out.println("Cliente conectado: " + remote);

		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				try {
					Validator.validateClient(inputLine);
					JsonNode req = mapper.readTree(inputLine);
					String operacao = req.path("operacao").asText("");

					RespostaBase resp;
					switch (operacao) {
					case "usuario_criar":
						resp = opUsuarioCriar(req);
						break;
					case "usuario_login":
						resp = opUsuarioLogin(req);
						break;
					case "usuario_logout":
						resp = opUsuarioLogout(req);
						break;
					case "usuario_ler":
						resp = opUsuarioLer(req);
						break;
					case "transacao_criar":
						resp = opTransacaoCriar(req);
						break;
					case "transacao_ler":
						resp = opTransacaoLer(req);
						break;
					case "depositar":
						resp = opDepositar(req);
						break;
					case "usuario_atualizar":
						resp = opUsuarioAtualizar(req);
						break;
					default:
						resp = new RespostaBase(operacao, false, "Operação desconhecida");
					}
					String jsonResp = mapper.writeValueAsString(resp);
			        Validator.validateServer(jsonResp);

			        out.println(jsonResp);
					
				} catch (Exception e) {
					RespostaBase erro = new RespostaBase("erro", false, "Erro no processamento: " + e.getMessage());
					out.println(mapper.writeValueAsString(erro));
				}
			}
		} catch (IOException e) {
			System.out.println("Cliente desconectado: " + remote);
		} finally {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}

	private static String validateToken(JsonNode req) {
		String token = req.path("token").asText("");
		return TokenManager.validateToken(token);
	}

	// Operações

	private static RespostaBase opUsuarioCriar(JsonNode req) {
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

	private static RespostaBase opUsuarioLogin(JsonNode req) {
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

	private static RespostaBase opUsuarioLogout(JsonNode req) {
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

	private static RespostaBase opUsuarioLer(JsonNode req) {
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

	private static RespostaBase opTransacaoCriar(JsonNode req) {
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

	private static RespostaBase opTransacaoLer(JsonNode req) {
		String cpf = validateToken(req);
		if (cpf == null) {
			return new RespostaBase("transacao_ler", false, "Token inválido ou expirado");
		}

		// Buscar todas as transações do banco
		List<Transacao> todasTransacoes = transacaoDAO.listarTodas();
		List<Map<String, Object>> transacoesUsuario = new ArrayList<>();

		for (Transacao t : todasTransacoes) {
			if (t.getCpfOrigem().equals(cpf) || t.getCpfDestino().equals(cpf)) {
				Map<String, Object> transacao = new HashMap<>();
				transacao.put("id", t.getId());
				transacao.put("valor", t.getValor());
				// criar objetos de usuario_enviador / usuario_recebedor (nome + cpf)
				Usuario enviador = usuarioDAO.buscarPorCpf(t.getCpfOrigem());
				Usuario recebedor = usuarioDAO.buscarPorCpf(t.getCpfDestino());
				Map<String, Object> usuarioEnviador = new HashMap<>();
				usuarioEnviador.put("nome", enviador != null ? enviador.getNome() : "");
				usuarioEnviador.put("cpf", t.getCpfOrigem());
				Map<String, Object> usuarioRecebedor = new HashMap<>();
				usuarioRecebedor.put("nome", recebedor != null ? recebedor.getNome() : "");
				usuarioRecebedor.put("cpf", t.getCpfDestino());
				transacao.put("usuario_enviador", usuarioEnviador);
				transacao.put("usuario_recebedor", usuarioRecebedor);
				// datas em ISO8601 UTC
				String criado = t.getCriadoEm().atZone(java.time.ZoneId.systemDefault())
						.withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime().toString() + "Z";
				transacao.put("criado_em", criado);
				transacao.put("atualizado_em", criado); // se não houver atualizado, repete criado
				transacoesUsuario.add(transacao);
			}
		}

		RespostaBase r = new RespostaBase("transacao_ler", true, "Transações do usuário");
		r.setTransacoes(transacoesUsuario);
		r.getDados().put("transacoes", transacoesUsuario); 
		return r;
	}

	private static RespostaBase opDepositar(JsonNode req) {
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

	private static RespostaBase opUsuarioAtualizar(JsonNode req) {
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

	public static void main(String[] args) {
		try {
			PixServer server = new PixServer(8080);
			server.start();

			// Manter o servidor rodando
			Scanner scanner = new Scanner(System.in);
			System.out.println("Pressione Enter para parar o servidor...");
			scanner.nextLine();

			server.stop();
			System.out.println("Servidor parado.");
		} catch (IOException e) {
			System.err.println("Erro ao iniciar servidor: " + e.getMessage());
		}
	}
}

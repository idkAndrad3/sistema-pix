package com.pix.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.pix.model.RespostaBase;
import com.pix.model.Sessao;

import com.pix.client.PixClient;
import validador.Validator;

/**
 * Servidor PIX integrado com banco de dados MySQL. Esta versão substitui o
 * armazenamento em memória por persistência no banco.
 */
public class PixServer {
	
	private static int port;
	private ServerSocket serverSocket;
	private boolean running = false;
	private Thread serverThread;

	private static final ObjectMapper mapper = new ObjectMapper();

	/* tracking de conexões/sessões para a GUI */
	private final AtomicInteger activeConnections = new AtomicInteger(0);
	private final AtomicLong sessionIdCounter = new AtomicLong(1);
	private final ConcurrentHashMap<Long, Sessao> sessions = new ConcurrentHashMap<>();
	private final Consumer<String> logger;

	public PixServer(int port, Consumer<String> logger) {
		PixServer.port = port;
		this.logger = logger; // Armazena o logger recebido
	}

	public static int getPort() {
		return port;
	}

	/** Retorna número atual de conexões ativas */
	public int getActiveConnections() {
		return activeConnections.get();
	}

	/** Retorna snapshot da lista de sessões/clients */
	public List<Sessao> getClientes() {
		return new ArrayList<>(sessions.values());
	}

	public void start() throws IOException {
		if (running)
			return;

		serverSocket = new ServerSocket(port);
		running = true;

		serverThread = new Thread(() -> {
			logger.accept("Servidor Pix com BD rodando na porta " + port);
			while (running) {
				try {
					Socket clientSocket = serverSocket.accept();

					// registra sessão
					long sessionId = sessionIdCounter.getAndIncrement();
					activeConnections.incrementAndGet();
					Sessao sess = new Sessao(sessionId,
							clientSocket.getInetAddress().getHostAddress(),
							clientSocket.getPort(),
							clientSocket.getInetAddress().getHostName(),
							"Conectado",
							LocalDateTime.now());
					sessions.put(sessionId, sess);

					new Thread(() -> {
						try {
							// mantém assinatura original do handleClient
							handleClient(clientSocket);
						} finally {
							// ao finalizar a conexão, atualiza contadores e sessão
							activeConnections.decrementAndGet();
							Sessao s = sessions.get(sessionId);
							if (s != null) {
								s.setStatus("Desconectado");
								s.setDesconectadoEm(LocalDateTime.now());
							}
							try {
								clientSocket.close();
							} catch (IOException ignored) {}
						}
					}, "PixClient-" + sessionId).start();

				} catch (IOException e) {
					if (running) {
						logger.accept("Erro no servidor: " + e.getMessage());
					}
				}
			}
		}, "PixServer-Main");
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
		logger.accept("Cliente conectado: " + remote);

		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				 logger.accept("[RECEBIDO] de " + remote + ": " + inputLine);
				try {
					JsonNode req = mapper.readTree(inputLine);
					String operacao = req.path("operacao").asText("");

					if ("conectar".equals(operacao)) {
	                    RespostaBase resp = new RespostaBase("conectar", true, "Servidor conectado com sucesso.");
	                    String jsonResp = mapper.writeValueAsString(resp);
	                    logger.accept("[ENVIANDO] para " + remote + ": " + jsonResp);
	                    out.println(jsonResp);
	                    continue;
	                }
					Validator.validateClient(inputLine);

					RespostaBase resp;
					switch (operacao) {
					case "conectar":
						resp = new RespostaBase("conectar", true, "Servidor conectado com sucesso");
						System.out.println(req);
						break;
					case "usuario_criar":
						resp = PixClient.opUsuarioCriar(req);
						System.out.println(req);
						break;
					case "usuario_login":
						resp = PixClient.opUsuarioLogin(req);
						System.out.println(req);
						break;
					case "usuario_logout":
						resp = PixClient.opUsuarioLogout(req);
						System.out.println(req);
						break;
					case "usuario_ler":
						resp = PixClient.opUsuarioLer(req);
						System.out.println(req);
						break;
					case "transacao_criar":
						resp = PixClient.opTransacaoCriar(req);
						System.out.println(req);
						break;
					case "transacao_ler":
						resp = PixClient.opTransacaoLer(req);
						System.out.println(req);
						break;
					case "depositar":
						resp = PixClient.opDepositar(req);
						System.out.println(req);
						break;
					case "usuario_atualizar":
						resp = PixClient.opUsuarioAtualizar(req);
						System.out.println(req);
						break;
                    case "usuario_deletar":
                        resp = PixClient.opUsuarioDeletar(req);
                        System.out.println(req);
                        break;
					default:
						resp = new RespostaBase(operacao, false, "Operação desconhecida");
					}
					String jsonResp = mapper.writeValueAsString(resp);
					logger.accept("[ENVIANDO] para " + remote + ": " + jsonResp);
					Validator.validateServer(jsonResp);

					out.println(jsonResp);

				} catch (Exception e) {
					RespostaBase erro = new RespostaBase("erro", false, "Erro no processamento: " + e.getMessage());
					try {
						
						String jsonErro = mapper.writeValueAsString(erro);
                        // Log do JSON de erro enviado
                        logger.accept("[ENVIANDO ERRO] para " + remote + ": " + jsonErro);
						
					} catch (Exception ignore) {}
				}
			}
		} catch (IOException e) {
			logger.accept("Cliente desconectado: " + remote);
		} finally {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}

	public static void main(String[] args) {
	}
}

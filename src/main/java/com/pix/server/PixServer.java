package com.pix.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.pix.model.RespostaBase;

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


	public PixServer(int port) {
		this.port = port;
	}

	public static int getPort() {
		return port;
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
						resp = PixClient.opUsuarioCriar(req);
						break;
					case "usuario_login":
						resp = PixClient.opUsuarioLogin(req);
						break;
					case "usuario_logout":
						resp = PixClient.opUsuarioLogout(req);
						break;
					case "usuario_ler":
						resp = PixClient.opUsuarioLer(req);
						break;
					case "transacao_criar":
						resp = PixClient.opTransacaoCriar(req);
						break;
					case "transacao_ler":
						resp = PixClient.opTransacaoLer(req);
						break;
					case "depositar":
						resp = PixClient.opDepositar(req);
						break;
					case "usuario_atualizar":
						resp = PixClient.opUsuarioAtualizar(req);
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


//Posso excluir, porém deixei para fazer os testes em PixClientTest.java
	public static void main(String[] args) {
		try {
			PixServer server = new PixServer(25444);
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

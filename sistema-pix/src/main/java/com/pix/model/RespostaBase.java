package com.pix.model;

import java.util.HashMap;
import java.util.Map;

public class RespostaBase {
	private String operacao;
	private boolean status;
	private String info;
	private Map<String, Object> dados = new HashMap<>();
	private Object usuario;
	private java.util.List<java.util.Map<String, Object>> transacoes;
	private String token;

	public RespostaBase() {
	}

	public RespostaBase(String operacao, boolean status, String info) {
		this.operacao = operacao;
		this.status = status;
		this.info = info;
	}

	public Object getUsuario() {
		return usuario;
	}

	public void setUsuario(Object usuario) {
		this.usuario = usuario;
	}

	public java.util.List<java.util.Map<String, Object>> getTransacoes() {
		return transacoes;
	}

	public void setTransacoes(java.util.List<java.util.Map<String, Object>> transacoes) {
		this.transacoes = transacoes;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getOperacao() {
		return operacao;
	}

	public void setOperacao(String operacao) {
		this.operacao = operacao;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Map<String, Object> getDados() {
		return dados;
	}

	public void setDados(Map<String, Object> dados) {
		this.dados = dados;
	}

	// Métodos de compatibilidade com código existente
	public boolean isSucesso() {
		return status;
	}

	public void setSucesso(boolean sucesso) {
		this.status = sucesso;
	}

	public String getMensagem() {
		return info;
	}

	public void setMensagem(String mensagem) {
		this.info = mensagem;
	}
}

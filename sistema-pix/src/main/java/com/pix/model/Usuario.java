package com.pix.model;

import java.util.ArrayList;
import java.util.List;

public class Usuario {
    private String nome;
    private String cpf;
    private String senha;
    private double saldo;
    private final List<Transacao> transacoes = new ArrayList<>();

    public Usuario(String nome, String cpf, String senha) {
        this.nome = nome;
        this.cpf = cpf;
        this.senha = senha;
        this.saldo = 0.0;
    }

    public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getCpf() { return cpf; }
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public synchronized double getSaldo() { return saldo; }
    public synchronized void addSaldo(double valor) { this.saldo += valor; }
    public synchronized void subSaldo(double valor) { this.saldo -= valor; }

    public List<Transacao> getTransacoes() { return transacoes; }

    public synchronized void addTransacao(Transacao t) { this.transacoes.add(t); }
}

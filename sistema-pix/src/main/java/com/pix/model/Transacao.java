package com.pix.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class Transacao {
    private static final AtomicLong idCounter = new AtomicLong(1);
    
    private long id;
    private String cpfOrigem;
    private String cpfDestino;
    private double valor;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Transacao(String cpfOrigem, String cpfDestino, double valor) {
        this.id = idCounter.getAndIncrement();
        this.cpfOrigem = cpfOrigem;
        this.cpfDestino = cpfDestino;
        this.valor = valor;
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    public long getId() { return id; }
    public String getCpfOrigem() { return cpfOrigem; }
    public String getCpfDestino() { return cpfDestino; }
    public double getValor() { return valor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    
    // Métodos de compatibilidade com código existente
    public String getOrigem() { return cpfOrigem; }
    public String getDestino() { return cpfDestino; }
    public LocalDateTime getDataHora() { return criadoEm; }
}

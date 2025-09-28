package com.pix.model;

import java.time.LocalDateTime;

public class Sessao {
    private long id;
    private String ip;
    private int porta;
    private String hostname;
    private String status;
    private LocalDateTime conectadoEm;
    private LocalDateTime desconectadoEm;

    public Sessao(long id, String ip, int porta, String hostname, String status, LocalDateTime conectadoEm) {
        this.id = id;
        this.ip = ip;
        this.porta = porta;
        this.hostname = hostname;
        this.status = status;
        this.conectadoEm = conectadoEm;
    }

    public long getId() { return id; }
    public String getIp() { return ip; }
    public int getPorta() { return porta; }
    public String getHostname() { return hostname; }
    public String getStatus() { return status; }
    public LocalDateTime getConectadoEm() { return conectadoEm; }
    public LocalDateTime getDesconectadoEm() { return desconectadoEm; }

    public void setStatus(String status) { this.status = status; }
    public void setConectadoEm(LocalDateTime dt) { this.conectadoEm = dt; }
    public void setDesconectadoEm(LocalDateTime dt) { this.desconectadoEm = dt; }
}

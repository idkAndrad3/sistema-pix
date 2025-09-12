package com.pix.model;

public class TransferRequest {
	 private String origem;
	    private String destino;
	    private double valor;

	    public String getOrigem() { return origem; }
	    public void setOrigem(String origem) { this.origem = origem; }

	    public String getDestino() { return destino; }
	    public void setDestino(String destino) { this.destino = destino; }

	    public double getValor() { return valor; }
	    public void setValor(double valor) { this.valor = valor; }
}

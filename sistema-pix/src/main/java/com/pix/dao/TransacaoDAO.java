package com.pix.dao;

import com.pix.model.Transacao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para persistir transações. Ao salvar, também atualiza os saldos dos usuários envolvidos.
 */
public class TransacaoDAO {

    public void salvar(Transacao t) {
        String insertTx = "INSERT INTO transacoes (cpf_origem, cpf_destino, valor, criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?)";
        String debita = "UPDATE usuarios SET saldo = saldo - ? WHERE cpf = ?";
        String credita = "UPDATE usuarios SET saldo = saldo + ? WHERE cpf = ?";

        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ptx = conn.prepareStatement(insertTx, Statement.RETURN_GENERATED_KEYS)) {
                ptx.setString(1, t.getCpfOrigem());
                ptx.setString(2, t.getCpfDestino());
                ptx.setDouble(3, t.getValor());
                ptx.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ptx.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ptx.executeUpdate();
            }

            try (PreparedStatement pst = conn.prepareStatement(debita)) {
                pst.setDouble(1, t.getValor());
                pst.setString(2, t.getCpfOrigem());
                pst.executeUpdate();
            }

            try (PreparedStatement pst = conn.prepareStatement(credita)) {
                pst.setDouble(1, t.getValor());
                pst.setString(2, t.getCpfDestino());
                pst.executeUpdate();
            }

            conn.commit();
            System.out.println("[TransacaoDAO] Transação salva.");

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public List<Transacao> listarTodas() {
        List<Transacao> lista = new ArrayList<>();
        String sql = "SELECT id, cpf_origem, cpf_destino, valor, criado_em FROM transacoes ORDER BY criado_em DESC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String origem = rs.getString("cpf_origem");
                String destino = rs.getString("cpf_destino");
                double valor = rs.getDouble("valor");
                Transacao t = new Transacao(origem, destino, valor);
                lista.add(t);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}

package com.pix.dao;

import com.pix.model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para persistência de Usuario.
 * Observação: o campo 'cpf' é tratado como chave primária natural.
 */
public class UsuarioDAO {

    public void salvar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (cpf, nome, senha, saldo) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getCpf());
            stmt.setString(2, usuario.getNome());
            stmt.setString(3, usuario.getSenha());
            stmt.setDouble(4, usuario.getSaldo());
            stmt.executeUpdate();

            System.out.println("[UsuarioDAO] Usuário salvo: " + usuario.getCpf());

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // duplicate entry
                System.out.println("[UsuarioDAO] CPF já existe, tentando atualizar...");
                atualizar(usuario);
            } else {
                e.printStackTrace();
            }
        }
    }

    public void atualizar(Usuario usuario) {
        String sql = "UPDATE usuarios SET nome = ?, senha = ?, saldo = ? WHERE cpf = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getSenha());
            stmt.setDouble(3, usuario.getSaldo());
            stmt.setString(4, usuario.getCpf());
            stmt.executeUpdate();

            System.out.println("[UsuarioDAO] Usuário atualizado: " + usuario.getCpf());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Usuario buscarPorCpf(String cpf) {
        String sql = "SELECT cpf, nome, senha, saldo FROM usuarios WHERE cpf = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String nome = rs.getString("nome");
                    String senha = rs.getString("senha");
                    double saldo = rs.getDouble("saldo");
                    Usuario u = new Usuario(nome, cpf, senha);
                    double diff = saldo - u.getSaldo();
                    if (diff > 0) u.addSaldo(diff);
                    else if (diff < 0) u.subSaldo(-diff);
                    return u;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT cpf, nome, senha, saldo FROM usuarios";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String cpf = rs.getString("cpf");
                String nome = rs.getString("nome");
                String senha = rs.getString("senha");
                double saldo = rs.getDouble("saldo");
                Usuario u = new Usuario(nome, cpf, senha);
                double diff = saldo - u.getSaldo();
                if (diff > 0) u.addSaldo(diff);
                else if (diff < 0) u.subSaldo(-diff);
                usuarios.add(u);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return usuarios;
    }
}

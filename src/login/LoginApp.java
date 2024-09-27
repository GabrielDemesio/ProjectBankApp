package login;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class LoginApp extends JFrame {

    private int usuarioId = -1; // ID do usuário logado

    public LoginApp() {
        setTitle("Login");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2));

        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");
        JTextField emailField = new JTextField();
        JPasswordField senhaField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        panel.add(usernameLabel);
        panel.add(emailField);
        panel.add(passwordLabel);
        panel.add(senhaField);
        panel.add(loginButton);
        panel.add(registerButton);

        add(panel);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = emailField.getText();
                String senha = new String(senhaField.getPassword());

                if (authenticate(email, senha)) {
                    JOptionPane.showMessageDialog(LoginApp.this, "Login successful!");
                    new banco.BancoApp(usuarioId).setVisible(true);
                    dispose(); // Fecha a janela de login
                } else {
                    JOptionPane.showMessageDialog(LoginApp.this, "Login failed!");
                }
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = emailField.getText();
                String senha = new String(senhaField.getPassword());

                if (register(email, senha)) {
                    JOptionPane.showMessageDialog(LoginApp.this, "Registration successful!");
                } else {
                    JOptionPane.showMessageDialog(LoginApp.this, "Registration failed!");
                }
            }
        });
    }

    private boolean authenticate(String email, String senha) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/banco_nubank", "root", "tmvohs91")) {
            String query = "SELECT usuario_id FROM usuarios WHERE email = ? AND senha = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            stmt.setString(2, senha);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                usuarioId = rs.getInt("usuario_id"); // Alterado para usuario_id
                return true; // Usuário autenticado
            }
            return false; // Falha na autenticação

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean register(String email, String senha) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/banco_nubank", "root", "tmvohs91")) {
            String query = "INSERT INTO usuarios (email, senha) VALUES (?, ?)"; // Alterado para a tabela usuarios
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            stmt.setString(2, senha);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // Sucesso se pelo menos uma linha foi inserida
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginApp().setVisible(true));
    }
}

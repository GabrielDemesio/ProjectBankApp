package banco;

import login.LoginApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BancoApp extends JFrame {

    private double saldo = 0.0;
    private JLabel saldoLabel;
    private int usuarioId;

    public BancoApp(int usuarioId) {
        this.usuarioId = usuarioId;

        // Carregar o saldo do usuário da tabela 'contas'
        carregarSaldo();

        setTitle("Banco Digital - Tela Principal");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        saldoLabel = new JLabel("Saldo: R$ " + String.format("%.2f", saldo));
        saldoLabel.setFont(new Font("Arial", Font.BOLD, 24));
        saldoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton depositarButton = new JButton("Depositar");
        JButton investirButton = new JButton("Investir");
        JButton registrarFaturaButton = new JButton("Registrar Fatura");
        JButton verFaturasButton = new JButton("Ver Faturas");
        JButton verInvestimentosButton = new JButton("Ver Investimentos");
        JButton voltarButton = new JButton("Voltar para Login");

        depositarButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        investirButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registrarFaturaButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        verFaturasButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        verInvestimentosButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        voltarButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(saldoLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(depositarButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(investirButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(registrarFaturaButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(verFaturasButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(verInvestimentosButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(voltarButton);

        add(panel);

        depositarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarDeposito();
            }
        });

        investirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                realizarInvestimento();
            }
        });

        registrarFaturaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registrarFatura();
            }
        });

        verFaturasButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                verFaturas();
            }
        });

        verInvestimentosButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                verInvestimentos();
            }
        });

        // Ação para o botão de voltar
        voltarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                voltarParaLogin();
            }
        });
    }

    private void realizarDeposito() {
        String valorStr = JOptionPane.showInputDialog(this, "Digite o valor do depósito:");

        try {
            double valor = Double.parseDouble(valorStr);

            // Verifica se o usuário existe antes de tentar realizar o depósito
            String checkQuery = "SELECT * FROM usuarios WHERE usuario_id = ?";  // Corrigido para buscar pelo id

            try (Connection conn = getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

                checkStmt.setInt(1, usuarioId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "Usuário não encontrado na tabela 'usuarios'.");
                        return; // Sai do método se o usuário não existe
                    }
                }

                // Verifica se o usuário existe na tabela 'contas'
                String checkQueryContas = "SELECT * FROM contas WHERE usuario_id = ?";
                try (PreparedStatement checkStmtContas = conn.prepareStatement(checkQueryContas)) {
                    checkStmtContas.setInt(1, usuarioId);
                    try (ResultSet rsContas = checkStmtContas.executeQuery()) {
                        if (!rsContas.next()) {
                            // Se não existir na tabela 'contas', insira um novo registro
                            String insertQuery = "INSERT INTO contas (usuario_id, saldo) VALUES (?, ?)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                                insertStmt.setInt(1, usuarioId);
                                insertStmt.setDouble(2, valor);
                                insertStmt.executeUpdate();
                                saldo = valor;  // Atualiza o saldo localmente
                                atualizarSaldo();
                                salvarTransacao("depósito", valor);
                                JOptionPane.showMessageDialog(this, "Novo registro criado e depósito realizado com sucesso!");
                                return; // Sai do método após inserir o novo registro
                            }
                        }
                    }
                }

                // Realiza o depósito se o usuário for encontrado na tabela 'contas'
                String updateQuery = "UPDATE contas SET saldo = saldo + ? WHERE usuario_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                    stmt.setDouble(1, valor);
                    stmt.setInt(2, usuarioId);

                    // Executa a atualização do saldo
                    int rowsUpdated = stmt.executeUpdate();

                    if (rowsUpdated > 0) {
                        // Atualiza o saldo localmente
                        saldo += valor;
                        atualizarSaldo();
                        salvarTransacao("depósito", valor);
                        JOptionPane.showMessageDialog(this, "Depósito realizado com sucesso!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Erro ao atualizar saldo no banco de dados.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco de dados: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido. Tente novamente.");
        }
    }

    private void realizarInvestimento() {
        String valorStr = JOptionPane.showInputDialog(this, "Digite o valor a ser investido:");
        try {
            double valor = Double.parseDouble(valorStr);
            if (valor > saldo) {
                JOptionPane.showMessageDialog(this, "Saldo insuficiente.");
            } else {
                saldo -= valor;
                atualizarSaldo();
                salvarTransacao("investimento", valor);
                salvarSaldo();  // Salva o saldo atualizado na tabela 'contas'
                JOptionPane.showMessageDialog(this, "Investimento de R$ " + String.format("%.2f", valor) + " realizado com sucesso!");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido. Tente novamente.");
        }
    }

    private void registrarFatura() {
        String nomeProduto = JOptionPane.showInputDialog(this, "Nome do Produto:");
        String parcelasStr = JOptionPane.showInputDialog(this, "Quantidade de Parcelas:");
        String valorStr = JOptionPane.showInputDialog(this, "Valor Total:");

        try {
            int parcelas = Integer.parseInt(parcelasStr);
            double valor = Double.parseDouble(valorStr);

            Connection conn = getConnection();
            String query = "INSERT INTO faturas (produto, parcelas, valor, usuario_id) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, nomeProduto);
            stmt.setInt(2, parcelas);
            stmt.setDouble(3, valor);
            stmt.setInt(4, usuarioId);
            stmt.executeUpdate();

            stmt.close();
            conn.close();

            JOptionPane.showMessageDialog(this, "Fatura registrada com sucesso!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Dados inválidos. Tente novamente.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao registrar a fatura: " + ex.getMessage());
        }
    }

    private void verFaturas() {
        try {
            Connection conn = getConnection();
            String query = "SELECT * FROM faturas WHERE usuario_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder faturas = new StringBuilder();
            while (rs.next()) {
                faturas.append("Produto: ").append(rs.getString("produto")).append("\n");
                faturas.append("Parcelas: ").append(rs.getInt("parcelas")).append("\n");
                faturas.append("Valor: R$ ").append(String.format("%.2f", rs.getDouble("valor"))).append("\n\n");
            }

            if (faturas.length() == 0) {
                faturas.append("Nenhuma fatura encontrada.");
            }

            JOptionPane.showMessageDialog(this, faturas.toString());

            stmt.close();
            rs.close();
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao carregar as faturas: " + ex.getMessage());
        }
    }

    private void verInvestimentos() {
        String query = "SELECT * FROM transacoes WHERE usuario_id = ?";
        StringBuilder investimentos = new StringBuilder();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    investimentos.append("Descrição: ").append(rs.getString("tipo")).append("\n");
                    investimentos.append("Valor: R$ ").append(String.format("%.2f", rs.getDouble("valor"))).append("\n");

                    Date data = rs.getDate("data");
                    if (data != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        investimentos.append("Data: ").append(sdf.format(data)).append("\n\n");
                    } else {
                        investimentos.append("Data: Não disponível\n\n");
                    }
                }

                if (investimentos.length() == 0) {
                    investimentos.append("Nenhum investimento encontrado.");
                }
                abrirJanelaInvestimentos(investimentos.toString());
            }
        } catch (SQLException ex) {
            handleSQLException(ex, "Erro ao carregar os investimentos.");
        }
    }

    private void abrirJanelaInvestimentos(String investimentos) {
        JFrame frame = new JFrame("Meus Investimentos");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JTextArea textArea = new JTextArea(investimentos);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane);

        JButton fecharButton = new JButton("Fechar");
        fecharButton.addActionListener(e -> frame.dispose());
        frame.add(fecharButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void voltarParaLogin() {
        this.dispose(); // Fecha a tela atual
        new LoginApp(); // Supondo que você tenha uma classe LoginScreen que cria a tela de login
    }

    private void carregarSaldo() {
        String query = "SELECT saldo FROM contas WHERE usuario_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    saldo = rs.getDouble("saldo");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void atualizarSaldo() {
        saldoLabel.setText("Saldo: R$ " + String.format("%.2f", saldo));
    }

    private void salvarTransacao(String tipo, double valor) {
        String query = "INSERT INTO transacoes (tipo, valor, usuario_id) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tipo);
            stmt.setDouble(2, valor);
            stmt.setInt(3, usuarioId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void salvarSaldo() {
        String query = "UPDATE contas SET saldo = ? WHERE usuario_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDouble(1, saldo);
            stmt.setInt(2, usuarioId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/banco_nubank", "root", "tmvohs91");
    }

    private void handleSQLException(SQLException ex, String message) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, message + ": " + ex.getMessage());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BancoApp(1).setVisible(true)); // Substitua '1' pelo id do usuário correto
    }
}

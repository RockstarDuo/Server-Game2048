import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 9999;
    private static final Map<ClientHandler, GamePanel> clientPanels = new ConcurrentHashMap<>();
    private static JFrame frame;
    private static JTextArea rankingArea = new JTextArea();
    private static List<ClientHandler> finishedClients = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        frame = new JFrame("128 Game Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(600, 800);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));
        JScrollPane scrollPane = new JScrollPane(panel);

        rankingArea.setEditable(false);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(rankingArea, BorderLayout.SOUTH);

        frame.setVisible(true);

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Game server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            GamePanel gamePanel = new GamePanel();
            clientPanels.put(clientHandler, gamePanel);

            panel.add(gamePanel);
            panel.revalidate();
            panel.repaint();

            new Thread(clientHandler).start();
        }
    }

    public static synchronized void clientFinished(ClientHandler clientHandler) {
        finishedClients.add(clientHandler);
        if (finishedClients.size() == clientPanels.size()) {
            updateRankings();
        }
    }

    private static void updateRankings() {
        List<ClientHandler> sortedClients = new ArrayList<>(finishedClients);
        sortedClients.sort(Comparator.comparingInt(ClientHandler::getMoves));

        StringBuilder sb = new StringBuilder();
        sb.append("Ranking:\n");
        for (int i = 0; i < sortedClients.size(); i++) {
            ClientHandler client = sortedClients.get(i);
            sb.append(String.format("%d. Client %d: %s (Moves: %d)\n",
                    i + 1, client.getId(), client.hasWon() ? "Won" : "Lost", client.getMoves()));
        }

        rankingArea.setText(sb.toString());
    }

    public static class ClientHandler implements Runnable {
        private static int clientCount = 0;
        private Socket socket;
        private BufferedReader in;
        private int id;
        private int moves;
        private boolean won;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.id = ++clientCount;
            this.moves = 0;
            this.won = false;
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("WIN")) {
                        won = true;
                        GamePanel gamePanel = clientPanels.get(this);
                        if (gamePanel != null) {
                            gamePanel.showWinMessage();
                        }
                        GameServer.clientFinished(this);
                        break;
                    } else {
                        moves++;
                        int[][] board = parseBoard(message);
                        GamePanel gamePanel = clientPanels.get(this);
                        if (gamePanel != null) {
                            gamePanel.updateBoard(board);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public int getId() {
            return id;
        }

        public int getMoves() {
            return moves;
        }

        public boolean hasWon() {
            return won;
        }

        private int[][] parseBoard(String message) {
            int[][] board = new int[4][4];
            String[] rows = message.split(";");
            for (int i = 0; i < 4; i++) {
                String[] values = rows[i].split(",");
                for (int j = 0; j < 4; j++) {
                    board[i][j] = Integer.parseInt(values[j]);
                }
            }
            return board;
        }
    }

    public static class GamePanel extends JPanel {
        private int[][] board;

        public GamePanel() {
            this.board = new int[4][4];
            setPreferredSize(new Dimension(400, 400));
        }

        public void updateBoard(int[][] board) {
            this.board = board;
            repaint();
        }

        public void showWinMessage() {
            JOptionPane.showMessageDialog(this, "Client won!");
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    drawTile(g, board[i][j], i, j);
                }
            }
        }

        private void drawTile(Graphics g, int value, int row, int col) {
            int x = col * 100;
            int y = row * 100;

            g.setColor(getTileColor(value));
            g.fillRect(x, y, 100, 100);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, 100, 100);

            if (value != 0) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.BOLD, 24));
                String text = String.valueOf(value);
                FontMetrics fm = getFontMetrics(g.getFont());
                int textX = x + (100 - fm.stringWidth(text)) / 2;
                int textY = y + (100 - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(text, textX, textY);
            }
        }

        private Color getTileColor(int value) {
            switch (value) {
                case 2: return Color.LIGHT_GRAY;
                case 4: return Color.GRAY;
                case 8: return Color.ORANGE;
                case 16: return Color.RED;
                case 32: return Color.PINK;
                case 64: return Color.YELLOW;
                case 128: return Color.GREEN;
                default: return Color.WHITE;
            }
        }
    }
}

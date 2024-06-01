/*
import javax.swing.*;
import javax.swing.border.LineBorder;
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
    private static JPanel streamingPanel;
    private static List<JPanel> panels = new ArrayList<>();     // grid cell 들의 Arraylist
    private static final JTextArea rankingArea = new JTextArea();
    private static final LineBorder lineBorder = new LineBorder(Color.BLACK, 1, false);
    private static List<ClientHandler> finishedClients = new CopyOnWriteArrayList<>();
    private static List<ClientHandler> retiredClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        setupGUI();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Game server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                GamePanel gamePanel = new GamePanel();
                clientPanels.put(clientHandler, gamePanel);

                addGamePanelToGUI(gamePanel);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setupGUI() {
        frame = new JFrame("2048 Game Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(1200, 800);
        frame.setMinimumSize(new Dimension(1100 , 800));    // Label 에 있는 text가 다 보이게하기 위해 frame 최소 크기 제한

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(Color.ORANGE);
        JLabel topLabel = new JLabel("2048 Game Server Screen");
        topLabel.setFont(topLabel.getFont().deriveFont(Font.BOLD, 30));
        topLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topLabel.setVerticalAlignment(SwingConstants.CENTER);
        topLabel.setBorder(lineBorder);
        topPanel.add(topLabel, BorderLayout.CENTER);

        // Color Array
        Color[] colors = {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.ORANGE, Color.CYAN, Color.MAGENTA, Color.PINK
        };

        // Streaming Panel
        streamingPanel = new JPanel();
        streamingPanel.setLayout(new GridLayout(2, 4,3,3));

        for (int i = 0; i < 8; i++) {
            JPanel panel = new JPanel(new BorderLayout());
            JPanel topPanelInner = new JPanel(new BorderLayout());
            panel.add(topPanelInner, BorderLayout.NORTH);

//            topPanelInner.setBackground(colors[i]);
            JLabel playerLabel = new JLabel("Player " + (i + 1));
            playerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            playerLabel.setBorder(lineBorder);
            topPanelInner.add(playerLabel, BorderLayout.WEST);

            JLabel highScoreLabel = new JLabel("High Score: 128");
            highScoreLabel.setFont(highScoreLabel.getFont().deriveFont(Font.BOLD));
            highScoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
            highScoreLabel.setVerticalAlignment(SwingConstants.CENTER);
            highScoreLabel.setBorder(lineBorder);
            topPanelInner.add(highScoreLabel, BorderLayout.CENTER);
            JLabel playerMovesLabel = new JLabel("Moves: ???");
            playerMovesLabel.setBorder(lineBorder);
            topPanelInner.add(playerMovesLabel, BorderLayout.EAST);

            panels.add(panel);
        }


        for (JPanel panel : panels) {
            streamingPanel.add(panel);
        }

        for (JPanel panel : panels) {
            JLabel cellLabel = new JLabel("Waiting Players To Join Game...");
            cellLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cellLabel.setVerticalAlignment(SwingConstants.CENTER);
            cellLabel.setBorder(lineBorder);
            panel.add(cellLabel, BorderLayout.CENTER);
        }


        // Bottom Panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        JLabel bottomLabel = new JLabel("Additional Fearture will be added here.");
        bottomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottomLabel.setVerticalAlignment(SwingConstants.CENTER);
        bottomLabel.setBorder(lineBorder);
        bottomPanel.add(bottomLabel, BorderLayout.CENTER);

//        rankingArea.setEditable(false);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(streamingPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
//        frame.add(rankingArea, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void addGamePanelToGUI(GamePanel gamePanel) {
        JPanel panel = panels.get(clientPanels.size() - 1);
        panel.remove(1);
        panel.add(gamePanel, BorderLayout.CENTER);
        streamingPanel.revalidate();
        streamingPanel.repaint();
    }


    public static synchronized void clientFinished(ClientHandler clientHandler) {
        finishedClients.add(clientHandler);
        if (finishedClients.size() + retiredClients.size() == clientPanels.size()) {
            updateRankings();
        }
    }

    public static synchronized void clientRetired(ClientHandler clientHandler) {
        retiredClients.add(clientHandler);
        if (finishedClients.size() + retiredClients.size() == clientPanels.size()) {
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
        sb.append("\nRetired Clients:\n");
        for (ClientHandler client : retiredClients) {
            sb.append(String.format("Client %d: Retired\n", client.getId()));
        }

        rankingArea.setText(sb.toString());
    }

    public static class ClientHandler implements Runnable {
        private static int clientCount = 0;
        private final Socket socket;
        private final BufferedReader in;
        private final int id;
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
                    } else if (message.equals("RETIRE")) {
                        GameServer.clientRetired(this);
                        break;
                    } else {
                        int[][] board = parseBoard(message);
                        GamePanel gamePanel = clientPanels.get(this);
                        if (gamePanel != null) {
                            gamePanel.updateBoard(board);
                            gamePanel.updateMoves(moves);
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
            String[] parts = message.split(";", 2);
            moves = Integer.parseInt(parts[0]);
            int[][] board = new int[4][4];
            String[] rows = parts[1].split(";");
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
        private JLabel movesLabel;

        public GamePanel() {
            this.board = new int[4][4];
            this.movesLabel = new JLabel("Moves: 0");
            setPreferredSize(new Dimension(400, 450));
            setLayout(new BorderLayout());
            add(movesLabel, BorderLayout.SOUTH);
        }

        public void updateBoard(int[][] board) {
            this.board = board;
            repaint();
        }

        public void updateMoves(int moves) {
            movesLabel.setText("Moves: " + moves);
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
*/

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 9999;
    private static final Map<ClientHandler, JPanel> clientPanels = new ConcurrentHashMap<>();
    private static JFrame frame;
    private static JPanel streamingPanel;
    private static List<JPanel> panels = new ArrayList<>();
    private static final JTextArea rankingArea = new JTextArea();
    private static final LineBorder lineBorder = new LineBorder(Color.BLACK, 1, false);
    private static List<ClientHandler> finishedClients = new CopyOnWriteArrayList<>();
    private static List<ClientHandler> retiredClients = new CopyOnWriteArrayList<>();
    private static int clientCount  = 0;

    public static void main(String[] args) {
        setupGUI();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Game server started on port " + PORT);

            while (true) {
                if (clientCount >= 8){
                    System.out.println("FULL Cannot accept new Client.");
                    Socket ClientSocket = serverSocket.accept();
                    continue;
                }
                Socket clientSocket = serverSocket.accept();
                clientCount++;
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                JPanel gamePanelContainer = createGamePanelContainer(clientHandler);
                clientPanels.put(clientHandler, gamePanelContainer);

                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setupGUI() {
        /*frame = new JFrame("2048 Game Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(1250, 800);
        frame.setResizable(false);

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(Color.ORANGE);
        JLabel topLabel = new JLabel("2048 Game Server Screen");
        topLabel.setFont(topLabel.getFont().deriveFont(Font.BOLD, 30));
        topLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topLabel.setVerticalAlignment(SwingConstants.CENTER);
        topLabel.setBorder(lineBorder);
        topPanel.add(topLabel, BorderLayout.CENTER);

        // Streaming Panel
        streamingPanel = new JPanel();
        streamingPanel.setLayout(new GridLayout(2, 4, 10, 10));

        for (int i = 0; i < 8; i++) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(300, 400));
            JLabel playerLabel = new JLabel("Player " + (i + 1));
            playerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            playerLabel.setBorder(lineBorder);
            panel.add(playerLabel, BorderLayout.NORTH);

            JLabel cellLabel = new JLabel("Waiting Players To Join Game...");
            cellLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cellLabel.setVerticalAlignment(SwingConstants.CENTER);
            cellLabel.setBorder(lineBorder);
            panel.add(cellLabel, BorderLayout.CENTER);

            panels.add(panel);
        }

        for (JPanel panel : panels) {
            streamingPanel.add(panel);
        }

        // Bottom Panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        JLabel bottomLabel = new JLabel("Additional Features will be added here.");
        bottomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottomLabel.setVerticalAlignment(SwingConstants.CENTER);
        bottomLabel.setBorder(lineBorder);
        bottomPanel.add(bottomLabel, BorderLayout.CENTER);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(streamingPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);*/

        frame = new JFrame("2048 Game Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(1250, 800);
        frame.setResizable(false);

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(Color.ORANGE);
        JLabel topLabel = new JLabel("2048 Game Server Screen");
        topLabel.setFont(topLabel.getFont().deriveFont(Font.BOLD, 30));
        topLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topLabel.setVerticalAlignment(SwingConstants.CENTER);
        topLabel.setBorder(lineBorder);
        topPanel.add(topLabel, BorderLayout.CENTER);

        // Color Array
        Color[] colors = {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.ORANGE, Color.CYAN, Color.MAGENTA, Color.PINK
        };

        // Streaming Panel
        streamingPanel = new JPanel();
        streamingPanel.setLayout(new GridLayout(2, 4,3,3));

        // streamingPanel -> panels -> topPanelInner -> playerLabel

        for (int i = 0; i < 8; i++) {
            JPanel panel = new JPanel(new BorderLayout());
            JPanel topPanelInner = new JPanel(new BorderLayout());
            panel.add(topPanelInner, BorderLayout.NORTH);

//            topPanelInner.setBackground(colors[i]);
            JLabel playerLabel = new JLabel("Player " + (i + 1));
            playerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            playerLabel.setBorder(lineBorder);
            topPanelInner.add(playerLabel, BorderLayout.WEST);

            JLabel highScoreLabel = new JLabel("High Score: NULL");
            highScoreLabel.setFont(highScoreLabel.getFont().deriveFont(Font.BOLD));
            highScoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
            highScoreLabel.setVerticalAlignment(SwingConstants.CENTER);
            highScoreLabel.setBorder(lineBorder);
            topPanelInner.add(highScoreLabel, BorderLayout.CENTER);
            JLabel playerMovesLabel = new JLabel("Moves: NULL");
            playerMovesLabel.setBorder(lineBorder);
            topPanelInner.add(playerMovesLabel, BorderLayout.EAST);
            panels.add(panel);

        }


        for (JPanel panel : panels) {
            streamingPanel.add(panel);
        }

        for (JPanel panel : panels) {
            JLabel cellLabel = new JLabel("Waiting Players To Join Game...");
            cellLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cellLabel.setVerticalAlignment(SwingConstants.CENTER);
            cellLabel.setBorder(lineBorder);
            panel.add(cellLabel, BorderLayout.CENTER);
        }

        // Bottom Panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        JLabel bottomLabel = new JLabel("Additional Fearture will be added here.");
        bottomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottomLabel.setVerticalAlignment(SwingConstants.CENTER);
        bottomLabel.setBorder(lineBorder);
        bottomPanel.add(bottomLabel, BorderLayout.CENTER);

//        rankingArea.setEditable(false);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(streamingPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
//        frame.add(rankingArea, BorderLayout.SOUTH);



        frame.setVisible(true);
    }

    private static JPanel createGamePanelContainer(ClientHandler clientHandler) {
        JPanel gamePanelContainer = panels.get(clientPanels.size());
        gamePanelContainer.remove(1); // Remove the cellLabel

        GamePanel gamePanel = new GamePanel();
        gamePanelContainer.add(gamePanel, BorderLayout.CENTER);
        streamingPanel.revalidate();
        streamingPanel.repaint();
        return gamePanelContainer;
    }

    public static synchronized void clientFinished(ClientHandler clientHandler) {
        finishedClients.add(clientHandler);
        if (finishedClients.size() + retiredClients.size() == clientPanels.size()) {
            updateRankings();
        }
    }

    public static synchronized void clientRetired(ClientHandler clientHandler) {
        retiredClients.add(clientHandler);
        if (finishedClients.size() + retiredClients.size() == clientPanels.size()) {
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
        sb.append("\nRetired Clients:\n");
        for (ClientHandler client : retiredClients) {
            sb.append(String.format("Client %d: Retired\n", client.getId()));
        }

        rankingArea.setText(sb.toString());
    }

    public static class ClientHandler implements Runnable {
        private static int clientCount = 0;
        private final Socket socket;
        private final BufferedReader in;
        private final int id;
        private int moves;
        private boolean won;
        private int clientIdNum;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.id = ++clientCount;
            this.moves = 0;
            this.won = false;
            this.clientIdNum = clientCount-1;
        }

        public void InitiatePanel(String playerName) {
            JPanel firstPanel = panels.get(clientIdNum);  // 해당 클라이언트의 패널 가져오기
            Component[] components = firstPanel.getComponents();
            for (Component component : components) {
                if (component instanceof JPanel) {
                    JPanel innerPanel = (JPanel) component;
                    Component[] innerComponents = innerPanel.getComponents();
                    for (Component innerComponent : innerComponents) {
                        if (innerComponent instanceof JLabel) {
                            JLabel label = (JLabel) innerComponent;
                            if (label.getText().startsWith("Player")) {
                                label.setText(playerName);  // 플레이어 이름 설정
                            }
                        }
                    }
                }
            }
            firstPanel.revalidate();
            firstPanel.repaint();
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.indexOf("^") == 0) {
                        String playerName = message.substring(1);
                        InitiatePanel(playerName);
                    }

                    else if (message.equals("WIN")) {
                        won = true;
                        GamePanel gamePanel = getGamePanel();
                        if (gamePanel != null) {
                            gamePanel.showWinMessage();
                        }
                        GameServer.clientFinished(this);
                        break;
                    } else if (message.equals("RETIRE")) {
                        GameServer.clientRetired(this);
                        break;
                    } else {
//                        System.out.println(message);
                        int[][] board = parseBoard(message);
                        GamePanel gamePanel = getGamePanel();
                        if (gamePanel != null) {
                            gamePanel.updateBoard(board);
                            gamePanel.updateMoves(moves);
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

        private GamePanel getGamePanel() {
            JPanel panel = clientPanels.get(this);
            return (GamePanel) panel.getComponent(1);
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
            String[] parts = message.split(";", 2);
            moves = Integer.parseInt(parts[0]);
            int[][] board = new int[4][4];
            String[] rows = parts[1].split(";");
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
        private JLabel movesLabel;

        public GamePanel() {
            this.board = new int[4][4];
            this.movesLabel = new JLabel("Moves: 0");
            setPreferredSize(new Dimension(300, 400));
            setLayout(new BorderLayout());
            add(movesLabel, BorderLayout.SOUTH);
        }

        public void updateBoard(int[][] board) {
            this.board = board;
            repaint();
        }

        public void updateMoves(int moves) {
            movesLabel.setText("Moves: " + moves);
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
            int x = col * 75;
            int y = row * 75;

            g.setColor(getTileColor(value));
            g.fillRect(x, y, 75, 75);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, 75, 75);

            if (value != 0) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.BOLD, 24));
                String text = String.valueOf(value);
                FontMetrics fm = getFontMetrics(g.getFont());
                int textX = x + (75 - fm.stringWidth(text)) / 2;
                int textY = y + (75 - fm.getHeight()) / 2 + fm.getAscent();
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

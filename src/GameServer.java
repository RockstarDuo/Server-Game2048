import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.Comparator;
import java.util.Collections;

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
    private static List<ClientHandler> exitedClients = new CopyOnWriteArrayList<>();
    private static List<ClientHandler> gameoverClients = new CopyOnWriteArrayList<>();
    private static List<ClientHandler> easyModeClients = new CopyOnWriteArrayList<>();
    private static List<ClientHandler> normalModeClients = new CopyOnWriteArrayList<>();
    private static List<ClientHandler> hardModeClients = new CopyOnWriteArrayList<>();
    private static int clientCount  = 0;

    public static void main(String[] args) {
        setupGUI();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Game server started on port " + PORT);

            while (true) {
                /*if (clientCount >= 8){
                    System.out.println("Game Room is FULL. Cannot accept new Client.");
                    serverSocket.accept();
                    continue;
                }*/
                if (clientPanels.size() >= 8) {
                    System.out.println("Game Room is FULL. Cannot accept new Client.");
                    serverSocket.accept();
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
        frame = new JFrame("2048 Game Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(1250, 800);
        frame.setResizable(false);

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        JLabel topLabel = new JLabel("2048 Game Server Screen");
        topLabel.setForeground(new Color(50, 150, 200));
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
            topPanelInner.setBackground(new Color(50, 150, 200));
            panel.add(topPanelInner, BorderLayout.NORTH);

//            topPanelInner.setBackground(colors[i]);
            JLabel playerLabel = new JLabel("Player " + (i + 1));
            playerLabel.setForeground(Color.WHITE);
            playerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            playerLabel.setBorder(lineBorder);
            topPanelInner.add(playerLabel, BorderLayout.CENTER);

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
            cellLabel.setForeground(new Color(50, 150, 200));
            panel.setBackground(Color.WHITE);
            panel.add(cellLabel, BorderLayout.CENTER);
        }

        // Bottom Panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        JLabel bottomLabel = new JLabel("2048 Game made by  @ _se__hyeon  &  @ jaehunshin_");
        bottomLabel.setForeground(new Color(50, 150, 200));
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
        /*System.out.println("---------------------------------------------");
        System.out.println("finished client: " + finishedClients.size());
        System.out.println("retired client: " + retiredClients.size());
        System.out.println("exited client: " + exitedClients.size());
        System.out.println("clientPanels: " + clientPanels.size());
        System.out.println("---------------------------------------------");*/

        if (finishedClients.size() + retiredClients.size() + exitedClients.size() + gameoverClients.size() == clientPanels.size()) {
            gameAllFinished();
            System.out.println("All Players Finished the Game.");
        }
    }

    public static synchronized void clientRetired(ClientHandler clientHandler) {
        retiredClients.add(clientHandler);
        /*System.out.println("---------------------------------------------");
        System.out.println("finished client: " + finishedClients.size());
        System.out.println("retired client: " + retiredClients.size());
        System.out.println("exited client: " + exitedClients.size());
        System.out.println("clientPanels: " + clientPanels.size());
        System.out.println("---------------------------------------------");*/
        if (finishedClients.size() + retiredClients.size() + exitedClients.size() + gameoverClients.size() == clientPanels.size()) {
            gameAllFinished();
            System.out.println("All Players Finished the Game.");
        }
    }

    public static synchronized void clientExit(ClientHandler clientHandler) {
        exitedClients.add(clientHandler);
        /*System.out.println("---------------------------------------------");
        System.out.println("finished client: " + finishedClients.size());
        System.out.println("retired client: " + retiredClients.size());
        System.out.println("exited client: " + exitedClients.size());
        System.out.println("clientPanels: " + clientPanels.size());
        System.out.println("---------------------------------------------");*/
        if (finishedClients.size() + retiredClients.size() + exitedClients.size() + gameoverClients.size() == clientPanels.size()) {
            gameAllFinished();
            System.out.println("All Players Finished the Game.");
        }
    }

    public static synchronized void clientGameover(ClientHandler clientHandler) {
        gameoverClients.add(clientHandler);
        if (finishedClients.size() + retiredClients.size() + exitedClients.size() + gameoverClients.size() == clientPanels.size()) {
            gameAllFinished();
            System.out.println("All Players Finished the Game.");
        }
    }

    private static String makeNameMoveString(List<ClientHandler> clientList) {
        StringBuilder stringBuilder = new StringBuilder();

        for (ClientHandler client : clientList) {
            stringBuilder.append(client.name + " ");
            stringBuilder.append(client.moves + ";");
        }
        String finalStr = stringBuilder.toString();
//        System.out.println(finalStr);
        return finalStr;
    }

    private static String makeNameString(List<ClientHandler> clientList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (ClientHandler client : clientList) {
            stringBuilder.append(client.name + ";");
        }

        String finalStr = stringBuilder.toString();
//        System.out.println(finalStr);
        return finalStr;
    }

    // Method to resize icons
    private static ImageIcon resizeIcon(ImageIcon icon, int width, int height) {
        Image image = icon.getImage();
        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }

    private static void gameAllFinished(){
        JOptionPane.showMessageDialog(null, "All Players Finished the Game.", "Message", JOptionPane.INFORMATION_MESSAGE);
        sendStatistics();
        JFrame statisticsFrame = new JFrame("Game Statistics");
        statisticsFrame.setSize(800, 850);
        statisticsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        statisticsFrame.setLayout(new BorderLayout());

        ImageIcon easy = new ImageIcon("easy.png");
        ImageIcon normal = new ImageIcon("normal.png");
        ImageIcon hard = new ImageIcon("hard.png");

        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(50, 150, 200));
        topPanel.setBorder(lineBorder);
        JLabel topLabel = new JLabel("Game result statistics");
        topLabel.setForeground(Color.white);
        topLabel.setFont(new Font(null, Font.BOLD, 20));
        topLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        topLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        topPanel.add(topLabel);

        JPanel mainPanel = new JPanel(new GridLayout(5, 3));
        mainPanel.setBorder(lineBorder);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(lineBorder);
        bottomPanel.setBackground(new Color(50, 150, 200));
        JLabel bottomLabel = new JLabel("2048 Game made by  @ _se__hyeon  &  @ jaehunshin_");
        bottomLabel.setForeground(Color.white);
        bottomLabel.setFont(new Font(null, Font.BOLD, 12));
        bottomLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        bottomLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        bottomPanel.add(bottomLabel);

        // Calculate the size of the icons based on panel size
        int iconWidth = 100; // Adjust the size as needed
        int iconHeight = 100; // Adjust the size as needed

        JPanel easyPanel = new JPanel(new BorderLayout());
        easyPanel.setBorder(lineBorder);
        easyPanel.setBackground(Color.WHITE); // Set background color to white
        JLabel easyLabel = new JLabel(resizeIcon(easy, iconWidth, iconHeight));
        easyPanel.add(easyLabel, BorderLayout.CENTER);

        JLabel easyWinLabel = new JLabel("easyWin");
        easyWinLabel.setForeground(new Color(50, 150, 200));
        easyWinLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        easyWinLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel easyWin = new JPanel(new BorderLayout());
        easyWin.setBorder(lineBorder);
        easyWin.setBackground(Color.WHITE); // Set background color to white
        easyWin.add(easyWinLabel, BorderLayout.NORTH);

        JLabel easyLoseLabel = new JLabel("easyLose");
        easyLoseLabel.setForeground(new Color(50, 150, 200));
        easyLoseLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        easyLoseLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel easyLose = new JPanel(new BorderLayout());
        easyLose.setBorder(lineBorder);
        easyLose.setBackground(Color.WHITE); // Set background color to white
        easyLose.add(easyLoseLabel, BorderLayout.NORTH);

        JLabel easyDisconnectedLabel = new JLabel("easyDisconnected");
        easyDisconnectedLabel.setForeground(new Color(50, 150, 200));
        easyDisconnectedLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        easyDisconnectedLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel easyDisconnected = new JPanel(new BorderLayout());
        easyDisconnected.setBorder(lineBorder);
        easyDisconnected.setBackground(Color.WHITE); // Set background color to white
        easyDisconnected.add(easyDisconnectedLabel, BorderLayout.NORTH);

        JLabel easyRetireLabel = new JLabel("easyRetire");
        easyRetireLabel.setForeground(new Color(50, 150, 200));
        easyRetireLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        easyRetireLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel easyRetire = new JPanel(new BorderLayout());
        easyRetire.setBorder(lineBorder);
        easyRetire.setBackground(Color.WHITE); // Set background color to white
        easyRetire.add(easyRetireLabel, BorderLayout.NORTH);

        JPanel normalPanel = new JPanel(new BorderLayout());
        normalPanel.setBorder(lineBorder);
        normalPanel.setBackground(Color.WHITE); // Set background color to white
        JLabel normalLabel = new JLabel(resizeIcon(normal, iconWidth, iconHeight));
        normalPanel.add(normalLabel, BorderLayout.CENTER);

        JLabel normalWinLabel = new JLabel("normalWin");
        normalWinLabel.setForeground(new Color(50, 150, 200));
        normalWinLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        normalWinLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel normalWin = new JPanel(new BorderLayout());
        normalWin.setBorder(lineBorder);
        normalWin.setBackground(Color.WHITE); // Set background color to white
        normalWin.add(normalWinLabel, BorderLayout.NORTH);

        JLabel normalLoseLabel = new JLabel("normalLose");
        normalLoseLabel.setForeground(new Color(50, 150, 200));
        normalLoseLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        normalLoseLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel normalLose = new JPanel(new BorderLayout());
        normalLose.setBorder(lineBorder);
        normalLose.setBackground(Color.WHITE); // Set background color to white
        normalLose.add(normalLoseLabel, BorderLayout.NORTH);

        JLabel normalDisconnectedLabel = new JLabel("normalDisconnected");
        normalDisconnectedLabel.setForeground(new Color(50, 150, 200));
        normalDisconnectedLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        normalDisconnectedLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel normalDisconnected = new JPanel(new BorderLayout());
        normalDisconnected.setBorder(lineBorder);
        normalDisconnected.setBackground(Color.WHITE); // Set background color to white
        normalDisconnected.add(normalDisconnectedLabel, BorderLayout.NORTH);

        JLabel normalRetireLabel = new JLabel("normalRetire");
        normalRetireLabel.setForeground(new Color(50, 150, 200));
        normalRetireLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        normalRetireLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel normalRetire = new JPanel(new BorderLayout());
        normalRetire.setBorder(lineBorder);
        normalRetire.setBackground(Color.WHITE); // Set background color to white
        normalRetire.add(normalRetireLabel, BorderLayout.NORTH);

        JPanel hardPanel = new JPanel(new BorderLayout());
        hardPanel.setBorder(lineBorder);
        hardPanel.setBackground(Color.WHITE); // Set background color to white
        JLabel hardLabel = new JLabel(resizeIcon(hard, iconWidth, iconHeight));
        hardPanel.add(hardLabel, BorderLayout.CENTER);

        JLabel hardWinLabel = new JLabel("hardWin");
        hardWinLabel.setForeground(new Color(50, 150, 200));
        hardWinLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        hardWinLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel hardWin = new JPanel(new BorderLayout());
        hardWin.setBorder(lineBorder);
        hardWin.setBackground(Color.WHITE); // Set background color to white
        hardWin.add(hardWinLabel, BorderLayout.NORTH);

        JLabel hardLoseLabel = new JLabel("hardLose");
        hardLoseLabel.setForeground(new Color(50, 150, 200));
        hardLoseLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        hardLoseLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel hardLose = new JPanel(new BorderLayout());
        hardLose.setBorder(lineBorder);
        hardLose.setBackground(Color.WHITE); // Set background color to white
        hardLose.add(hardLoseLabel, BorderLayout.NORTH);

        JLabel hardDisconnectedLabel = new JLabel("hardDisconnected");
        hardDisconnectedLabel.setForeground(new Color(50, 150, 200));
        hardDisconnectedLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        hardDisconnectedLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel hardDisconnected = new JPanel(new BorderLayout());
        hardDisconnected.setBorder(lineBorder);
        hardDisconnected.setBackground(Color.WHITE); // Set background color to white
        hardDisconnected.add(hardDisconnectedLabel, BorderLayout.NORTH);

        JLabel hardRetireLabel = new JLabel("hardRetire");
        hardRetireLabel.setForeground(new Color(50, 150, 200));
        hardRetireLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        hardRetireLabel.setVerticalAlignment(SwingConstants.CENTER); // 중앙 정렬
        JPanel hardRetire = new JPanel(new BorderLayout());
        hardRetire.setBorder(lineBorder);
        hardRetire.setBackground(Color.WHITE); // Set background color to white
        hardRetire.add(hardRetireLabel, BorderLayout.NORTH);

        mainPanel.add(easyPanel);
        mainPanel.add(normalPanel);
        mainPanel.add(hardPanel);

        mainPanel.add(easyWin);
        mainPanel.add(normalWin);
        mainPanel.add(hardWin);

        mainPanel.add(easyLose);
        mainPanel.add(normalLose);
        mainPanel.add(hardLose);

        mainPanel.add(easyRetire);
        mainPanel.add(normalRetire);
        mainPanel.add(hardRetire);

        mainPanel.add(easyDisconnected);
        mainPanel.add(normalDisconnected);
        mainPanel.add(hardDisconnected);

        statisticsFrame.add(topPanel, BorderLayout.NORTH);
        statisticsFrame.add(mainPanel, BorderLayout.CENTER);
        statisticsFrame.add(bottomPanel, BorderLayout.SOUTH);
        statisticsFrame.setVisible(true);

    }

    private static void sendStatistics() {
        // finished -> gameover -> retire -> exit 순으로 보냄
        for (ClientHandler client : clientPanels.keySet()) {
            client.out.println(makeNameMoveString(sortFinishedClientList(easyModeClients)));
            client.out.println(makeNameString(sortGameoverClientList(easyModeClients)));
            client.out.println(makeNameString(sortRetireClientList(easyModeClients)));
            client.out.println(makeNameString(sortExitedClientList(easyModeClients)));

            client.out.println(makeNameMoveString(sortFinishedClientList(normalModeClients)));
            client.out.println(makeNameString(sortGameoverClientList(normalModeClients)));
            client.out.println(makeNameString(sortRetireClientList(normalModeClients)));
            client.out.println(makeNameString(sortExitedClientList(normalModeClients)));

            client.out.println(makeNameMoveString(sortFinishedClientList(hardModeClients)));
            client.out.println(makeNameString(sortGameoverClientList(hardModeClients)));
            client.out.println(makeNameString(sortRetireClientList(hardModeClients)));
            client.out.println(makeNameString(sortExitedClientList(hardModeClients)));
        }
    }


    // 정상적으로 게임을 모두 완료한 클라이언트들을 moves의 오름차순으로 정렬한 새로운 리스트에 add 한다.
    private static List<ClientHandler> sortFinishedClientList(List<ClientHandler> clientList) {
        List<ClientHandler> filteredClients = new ArrayList<>();

        // Filter out clients who have gameOvered, retired, or exited
        for (ClientHandler client : clientList) {
            if (!client.gameOvered && !client.retired && !client.exited) {
                filteredClients.add(client);
            }
        }

        // Sort the filtered clients based on the number of moves
        filteredClients.sort(Comparator.comparing(ClientHandler::getMoves));

        // moves 수 오름차순으로 sort 한거 확인출력
        for (ClientHandler client : filteredClients) {
            System.out.println(client.getMoves());
        }

        // Return the sorted list of clients
        return filteredClients;
    }

    private static List<ClientHandler> sortRetireClientList(List<ClientHandler> clientList) {
        List<ClientHandler> sortedRetireClients = new ArrayList<>();
        for (ClientHandler client : clientList) {
            if (client.retired) {
                sortedRetireClients.add(client);
            }
        }
        return sortedRetireClients;
    }

    private static List<ClientHandler> sortExitedClientList(List<ClientHandler> clientList) {
        List<ClientHandler> sortedExitedClients = new ArrayList<>();
        for (ClientHandler client : clientList) {
            if (client.exited) {
                sortedExitedClients.add(client);
            }
        }
        return sortedExitedClients;
    }

    private static List<ClientHandler> sortGameoverClientList(List<ClientHandler> clientList) {
        List<ClientHandler> sortedGameoverClients = new ArrayList<>();
        for (ClientHandler client : clientList) {
            if (client.gameOvered) {
                sortedGameoverClients.add(client);
            }
        }
        return sortedGameoverClients;
    }


    public static class ClientHandler implements Runnable {
        private static int clientCount = 0;
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private final int id;
        private int moves;
        private boolean won;
        private int clientIdNum;
        private boolean retired = false;
        private boolean gameOvered = false;
        private boolean exited = false;
        private String name = null;


        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.id = ++clientCount;
            this.moves = 0;
            this.won = false;
            this.clientIdNum = clientCount-1;

        }

        public void InitiatePanel(String playerName) {
            this.name = playerName;
            JPanel firstPanel = panels.get(clientIdNum);  // 해당 클라이언트의 패널 가져오기
            Component[] components = firstPanel.getComponents();
            for (Component component : components) {
                if (component instanceof JPanel) {
                    JPanel innerPanel = (JPanel) component;
                    Component[] innerComponents = innerPanel.getComponents();
                    for (Component innerComponent : innerComponents) {
                        if (innerComponent instanceof JLabel) {
                            JLabel label = (JLabel) innerComponent;
                            label.setForeground(Color.WHITE);
                            if (label.getText().startsWith("Player")) {
                                label.setText("< " + playerName + " >");  // 플레이어 이름 설정
                                label.getFont().deriveFont(Font.BOLD, 30);
                            }
                        }
                    }
                }
            }
            firstPanel.revalidate();
            firstPanel.repaint();
        }

        public void playerExit(ClientHandler clientHandler) {
            GamePanel gamePanel = getGamePanel();

            // Load the original ImageIcon
            ImageIcon retireImageIcon = new ImageIcon("disconnectedimage.png");
            Image originalImage = retireImageIcon.getImage();

            // Get the size of the panel
            Dimension panelSize = gamePanel.getSize();

            // Scale the image to fit the panel size
            Image scaledImage = originalImage.getScaledInstance(panelSize.width, panelSize.height, Image.SCALE_SMOOTH);

            // Create a new ImageIcon with the scaled image
            ImageIcon scaledImageIcon = new ImageIcon(scaledImage);

            // Create and set up the label with the scaled image
            gamePanel.removeAll();
            JLabel retireLabel = new JLabel(scaledImageIcon);

            // Create and set up the exit panel
            JPanel exitPanel = new JPanel(new BorderLayout());
            exitPanel.add(retireLabel, BorderLayout.CENTER);

            // Add the exit panel to the game panel
            gamePanel.add(exitPanel, BorderLayout.CENTER);
            gamePanel.setBorder(lineBorder);

            // Revalidate and repaint the game panel
            gamePanel.revalidate();
            gamePanel.repaint();

            this.exited = true;
        }

        public void playerRetire(ClientHandler clientHandler) {
            GamePanel gamePanel = getGamePanel();

            // Load the original ImageIcon
            ImageIcon retireImageIcon = new ImageIcon("gameOverimage.png");
            Image originalImage = retireImageIcon.getImage();

            // Get the size of the panel
            Dimension panelSize = gamePanel.getSize();

            // Scale the image to fit the panel size
            Image scaledImage = originalImage.getScaledInstance(panelSize.width, panelSize.height, Image.SCALE_SMOOTH);

            // Create a new ImageIcon with the scaled image
            ImageIcon scaledImageIcon = new ImageIcon(scaledImage);

            // Create and set up the label with the scaled image
            gamePanel.removeAll();
            JLabel retireLabel = new JLabel(scaledImageIcon);

            // Create and set up the exit panel
            JPanel exitPanel = new JPanel(new BorderLayout());
            exitPanel.add(retireLabel, BorderLayout.CENTER);

            // Add the exit panel to the game panel
            gamePanel.add(exitPanel, BorderLayout.CENTER);
            gamePanel.setBorder(lineBorder);

            // Revalidate and repaint the game panel
            gamePanel.revalidate();
            gamePanel.repaint();
            this.retired = true;

        }

        public void playerWin(ClientHandler clientHandler) {
            GamePanel gamePanel = getGamePanel();

            // Load the original ImageIcon
            ImageIcon retireImageIcon = new ImageIcon("win.png");
            Image originalImage = retireImageIcon.getImage();

            // Get the size of the panel
            Dimension panelSize = gamePanel.getSize();

            // Scale the image to fit the panel size
            Image scaledImage = originalImage.getScaledInstance(panelSize.width, panelSize.height, Image.SCALE_SMOOTH);

            // Create a new ImageIcon with the scaled image
            ImageIcon scaledImageIcon = new ImageIcon(scaledImage);

            // Create and set up the label with the scaled image
            gamePanel.removeAll();
            JLabel retireLabel = new JLabel(scaledImageIcon);

            // Create and set up the exit panel
            JPanel exitPanel = new JPanel(new BorderLayout());
            exitPanel.add(retireLabel, BorderLayout.CENTER);

            // Add the exit panel to the game panel
            gamePanel.add(exitPanel, BorderLayout.CENTER);
            gamePanel.setBorder(lineBorder);

            // Revalidate and repaint the game panel
            gamePanel.revalidate();
            gamePanel.repaint();
            won = true;

        }

        public void playerGameOver(ClientHandler clientHandler) {
            GamePanel gamePanel = getGamePanel();

            // Load the original ImageIcon
            ImageIcon retireImageIcon = new ImageIcon("gameOverimage.png");
            Image originalImage = retireImageIcon.getImage();

            // Get the size of the panel
            Dimension panelSize = gamePanel.getSize();

            // Scale the image to fit the panel size
            Image scaledImage = originalImage.getScaledInstance(panelSize.width, panelSize.height, Image.SCALE_SMOOTH);

            // Create a new ImageIcon with the scaled image
            ImageIcon scaledImageIcon = new ImageIcon(scaledImage);

            // Create and set up the label with the scaled image
            gamePanel.removeAll();
            JLabel retireLabel = new JLabel(scaledImageIcon);

            // Create and set up the exit panel
            JPanel exitPanel = new JPanel(new BorderLayout());
            exitPanel.add(retireLabel, BorderLayout.CENTER);

            // Add the exit panel to the game panel
            gamePanel.add(exitPanel, BorderLayout.CENTER);
            gamePanel.setBorder(lineBorder);

            // Revalidate and repaint the game panel
            gamePanel.revalidate();
            gamePanel.repaint();
            this.gameOvered = true;
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.indexOf("^") == 0) {
                        String playerName = message.substring(1);
                        InitiatePanel(playerName);
                    } else if (message.indexOf("_") == 0) {
                        String difficulty = message.substring(1);

                        if (difficulty.equals("Easy")) {
                            System.out.println("Difficulty: Easy");
                            easyModeClients.add(this);
                        }else if (difficulty.equals("Normal")) {
                            System.out.println("Difficulty: Normal");
                            normalModeClients.add(this);
                        }else if(difficulty.equals("Hard")) {
                            System.out.println("Difficulty: Hard");
                            hardModeClients.add(this);
                        }
                        /*System.out.println("----------------------------------");
                        System.out.println("Easy Mode Clients: " + easyModeClients.size());
                        System.out.println("Normal Mode Clients: " + normalModeClients.size());
                        System.out.println("Hard Mode Clients: " + hardModeClients.size());
                        System.out.println("----------------------------------");*/

                    } else if (message.equals("WIN")) {
                        playerWin(this);
                        GameServer.clientFinished(this);
                        break;
                    } else if (message.equals("RETIRE")) {
                        playerRetire(this);
                        GameServer.clientRetired(this);
                        break;
                    } else if (message.equals("GAMEOVER")) {
                        playerGameOver(this);
                        GameServer.clientGameover(this);

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
                    if (!retired & !won & !gameOvered){
                        playerExit(this);
                        clientExit(this);
                    }
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
            JPanel bottomMoves = new JPanel();
            bottomMoves.setLayout(new BorderLayout());
            bottomMoves.add(movesLabel,BorderLayout.CENTER);
            add(bottomMoves, BorderLayout.SOUTH);
        }

        public void updateBoard(int[][] board) {
            this.board = board;
            repaint();
        }

        public void updateMoves(int moves) {
            movesLabel.setText("Moves: " + moves);
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
                case 2: return new Color(255, 182, 193);
                case 4: return new Color(173, 216, 230);
                case 8: return new Color(152, 251, 152);
                case 16: return new Color(253, 253, 150);
                case 32: return new Color(216, 191, 216);
                case 64: return new Color(255, 204, 153);
                case 128: return new Color(189, 252, 201);
                case 256: return new Color(218, 185, 255); // Light Pastel Purple
                case 512: return new Color(255, 255, 204); // Light Pastel Yellow
                default: return Color.white;
            }
        }
    }
}

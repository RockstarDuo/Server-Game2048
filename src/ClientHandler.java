import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private static int clientCount = 0;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final int id;
    private int moves;
    private boolean won;
    private int clientIdNum;
    boolean retired = false;
    boolean gameOvered = false;
    boolean exited = false;
    public String name = null;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.id = ++clientCount;
        this.moves = 0;
        this.won = false;
        this.clientIdNum = clientCount - 1;
    }

    public void InitiatePanel(String playerName) {
        this.name = playerName;
        JPanel firstPanel = GameServer.panels.get(clientIdNum);
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
                            label.setText("< " + playerName + " >");
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
        updateGamePanel("disconnectedimage.png");
        this.exited = true;
    }

    public void playerRetire(ClientHandler clientHandler) {
        updateGamePanel("gameOverimage.png");
        this.retired = true;
    }

    public void playerWin(ClientHandler clientHandler) {
        updateGamePanel("win.png");
        won = true;
    }

    public void playerGameOver(ClientHandler clientHandler) {
        updateGamePanel("gameOverimage.png");
        this.gameOvered = true;
    }

    private void updateGamePanel(String imagePath) {
        GamePanel gamePanel = getGamePanel();

        ImageIcon icon = new ImageIcon(imagePath);
        Image originalImage = icon.getImage();
        Dimension panelSize = gamePanel.getSize();
        Image scaledImage = originalImage.getScaledInstance(panelSize.width, panelSize.height, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        gamePanel.removeAll();
        JLabel label = new JLabel(scaledIcon);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.CENTER);

        gamePanel.add(panel, BorderLayout.CENTER);
        gamePanel.setBorder(GameServer.lineBorder);
        gamePanel.revalidate();
        gamePanel.repaint();
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("^")) {
                    String playerName = message.substring(1);
                    InitiatePanel(playerName);
                } else if (message.startsWith("_")) {
                    String difficulty = message.substring(1);

                    if (difficulty.equals("Easy")) {
                        GameServer.easyModeClients.add(this);
                    } else if (difficulty.equals("Normal")) {
                        GameServer.normalModeClients.add(this);
                    } else if (difficulty.equals("Hard")) {
                        GameServer.hardModeClients.add(this);
                    }

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
                if (!retired & !won & !gameOvered) {
                    playerExit(this);
                    GameServer.clientExit(this);
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private GamePanel getGamePanel() {
        JPanel panel = GameServer.clientPanels.get(this);
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

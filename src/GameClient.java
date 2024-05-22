import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Random;
//add retire
public class GameClient extends JFrame {
    private final Game128 game;
    private final PrintWriter out;
    private boolean gameWon = false;
    private JLabel movesLabel;
    private int moveCount = 0;

    public GameClient(String serverAddress) throws IOException {
        game = new Game128();

        Socket socket = new Socket(serverAddress, 9999);
        out = new PrintWriter(socket.getOutputStream(), true);

        GameBoard board = new GameBoard(game);
        add(board, BorderLayout.CENTER);

        movesLabel = new JLabel("Moves: 0");
        add(movesLabel, BorderLayout.SOUTH);

        pack();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameWon) return;  // Prevent further actions if the game is won

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        game.moveLeft();
                        break;
                    case KeyEvent.VK_RIGHT:
                        game.moveRight();
                        break;
                    case KeyEvent.VK_UP:
                        game.moveUp();
                        break;
                    case KeyEvent.VK_DOWN:
                        game.moveDown();
                        break;
                }
                board.repaint();
                sendBoardToServer(game.getBoard());
                moveCount++;
                movesLabel.setText("Moves: " + moveCount);
                checkGameState();
            }
        });

        // Add a window listener to close resources properly
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    out.close();
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    private void sendBoardToServer(int[][] board) {
        StringBuilder sb = new StringBuilder();
        sb.append(moveCount).append(";");
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sb.append(board[i][j]);
                if (j < 3) {
                    sb.append(",");
                }
            }
            if (i < 3) {
                sb.append(";");
            }
        }
        out.println(sb.toString());
    }

    private void checkGameState() {
        if (game.isWin()) {
            gameWon = true;
            out.println("WIN");
            JOptionPane.showMessageDialog(null, "You Win!");
        } else if (game.isGameOver()) {
            JOptionPane.showMessageDialog(null, "Game Over");
        }
    }

    public static void main(String[] args) throws IOException {
        String serverAddress = args.length > 0 ? args[0] : "192.168.0.3";
        JFrame frame = new GameClient(serverAddress);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 450);
        frame.setVisible(true);
    }
}

class Game128 {
    private int[][] board;
    private final Random random;

    public Game128() {
        board = new int[4][4];
        random = new Random();
        initializeBoard();
    }

    private void initializeBoard() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                board[i][j] = 0;
            }
        }
        addRandomTile();
        addRandomTile();
    }

    private void addRandomTile() {
        int row, col;
        do {
            row = random.nextInt(4);
            col = random.nextInt(4);
        } while (board[row][col] != 0);
        board[row][col] = random.nextDouble() < 0.9 ? 2 : 4;
    }

    public int[][] getBoard() {
        return board;
    }

    public void moveLeft() {
        if (moveAndMerge()) {
            addRandomTile();
        }
    }

    public void moveRight() {
        rotateBoard();
        rotateBoard();
        moveLeft();
        rotateBoard();
        rotateBoard();
    }

    public void moveUp() {
        rotateBoard();
        rotateBoard();
        rotateBoard();
        moveLeft();
        rotateBoard();
    }

    public void moveDown() {
        rotateBoard();
        moveLeft();
        rotateBoard();
        rotateBoard();
        rotateBoard();
    }

    private boolean moveAndMerge() {
        boolean moved = false;
        for (int i = 0; i < 4; i++) {
            int[] row = board[i];
            int[] newRow = new int[4];
            int newIndex = 0;
            boolean merged = false;

            for (int j = 0; j < 4; j++) {
                if (row[j] != 0) {
                    if (newIndex > 0 && newRow[newIndex - 1] == row[j] && !merged) {
                        newRow[newIndex - 1] *= 2;
                        merged = true;
                        moved = true;
                    } else {
                        if (newRow[newIndex] != row[j]) {
                            moved = true;
                        }
                        newRow[newIndex++] = row[j];
                        merged = false;
                    }
                }
            }

            board[i] = newRow;
        }
        return moved;
    }

    private void rotateBoard() {
        int[][] newBoard = new int[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                newBoard[i][j] = board[4 - j - 1][i];
            }
        }
        board = newBoard;
    }

    public boolean isWin() {
        for (int[] row : board) {
            for (int value : row) {
                if (value == 128) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isGameOver() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (board[i][j] == 0) {
                    return false;
                }
                if (i < 3 && board[i][j] == board[i + 1][j]) {
                    return false;
                }
                if (j < 3 && board[i][j] == board[i][j + 1]) {
                    return false;
                }
            }
        }
        return true;
    }
}

class GameBoard extends JPanel {
    private final Game128 game;

    public GameBoard(Game128 game) {
        this.game = game;
        setPreferredSize(new Dimension(400, 400));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int[][] board = game.getBoard();
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

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private int[][] board;
    private JLabel movesLabel;

    public GamePanel() {
        this.board = new int[4][4];
        this.movesLabel = new JLabel("Moves: 0");
        setPreferredSize(new Dimension(300, 400));
        setLayout(new BorderLayout());
        JPanel bottomMoves = new JPanel();
        bottomMoves.setLayout(new BorderLayout());
        bottomMoves.add(movesLabel, BorderLayout.CENTER);
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
            case 2:
                return new Color(255, 182, 193);
            case 4:
                return new Color(173, 216, 230);
            case 8:
                return new Color(152, 251, 152);
            case 16:
                return new Color(253, 253, 150);
            case 32:
                return new Color(216, 191, 216);
            case 64:
                return new Color(255, 204, 153);
            case 128:
                return new Color(189, 252, 201);
            case 256:
                return new Color(218, 185, 255);
            case 512:
                return new Color(255, 255, 204);
            default:
                return Color.white;
        }
    }
}

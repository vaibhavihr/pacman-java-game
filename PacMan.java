import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            switch (this.direction) {
                case 'U': this.velocityX = 0; this.velocityY = -tileSize / 4; break;
                case 'D': this.velocityX = 0; this.velocityY = tileSize / 4; break;
                case 'L': this.velocityX = -tileSize / 4; this.velocityY = 0; break;
                case 'R': this.velocityX = tileSize / 4; this.velocityY = 0; break;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21, columnCount = 19, tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage, blueGhostImage, orangeGhostImage, pinkGhostImage, redGhostImage;
    private Image pacmanUpImage, pacmanDownImage, pacmanLeftImage, pacmanRightImage;

    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O       bpo       O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX"
    };

    HashSet<Block> walls, foods, ghosts;
    Block pacman;
    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'};
    Random random = new Random();
    int score = 0, lives = 3;
    boolean gameOver = false;

    DatabaseManager db;
    String playerName;

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load images
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        loadMap();
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }

        // Start the game loop
        gameLoop = new Timer(50, this); // 20fps (1000/50)
        gameLoop.start();

        // Database initialization
        try {
            db = new DatabaseManager();
            playerName = JOptionPane.showInputDialog("Enter your name:");
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "Unknown";
            }
            db.addPlayer(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        ghosts = new HashSet<>();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                char ch = tileMap[r].charAt(c);
                int x = c * tileSize, y = r * tileSize;

                switch (ch) {
                    case 'X': walls.add(new Block(wallImage, x, y, tileSize, tileSize)); break;
                    case 'b': ghosts.add(new Block(blueGhostImage, x, y, tileSize, tileSize)); break;
                    case 'o': ghosts.add(new Block(orangeGhostImage, x, y, tileSize, tileSize)); break;
                    case 'p': ghosts.add(new Block(pinkGhostImage, x, y, tileSize, tileSize)); break;
                    case 'r': ghosts.add(new Block(redGhostImage, x, y, tileSize, tileSize)); break;
                    case 'P': pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize); break;
                    case ' ': foods.add(new Block(null, x + 14, y + 14, 4, 4)); break;
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);
        for (Block ghost : ghosts) g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        for (Block wall : walls) g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);

        g.setColor(Color.WHITE);
        for (Block food : foods) g.fillRect(food.x, food.y, food.width, food.height);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver)
            g.drawString("Game Over: " + score, tileSize / 2, tileSize / 2);
        else
            g.drawString("x" + lives + " Score: " + score, tileSize / 2, tileSize / 2);
    }

    public void move() {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Check for collisions with walls
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        // Check for collisions with ghosts
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                lives--;
                if (lives == 0) {
                    gameOver = true;
                    try {
                        db.updateHighScore(playerName, score);
                        db.addToLeaderboard(playerName, score);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                resetPositions();
            }

            if (ghost.y == tileSize * 9 && ghost.direction != 'U' && ghost.direction != 'D')
                ghost.updateDirection('U');

            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;

            // Check for collisions with walls
            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    ghost.updateDirection(directions[random.nextInt(4)]);
                }
            }
        }

        // Check for food collisions
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
            }
        }
        foods.remove(foodEaten);

        // If all food is eaten, reload the map and reset
        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }
    }

    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x &&
               a.y < b.y + b.height && a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        for (Block ghost : ghosts) {
            ghost.reset();
            ghost.updateDirection(directions[random.nextInt(4)]);
        }
    }

    public void showLeaderboard() {
        try {
            // Always create a fresh connection
            DatabaseManager freshDb = new DatabaseManager();
            ResultSet rs = freshDb.getTopPlayers(5);

            rs.last();
            int rowCount = rs.getRow();
            rs.beforeFirst();

            if (rowCount == 0) {
                JOptionPane.showMessageDialog(this, "No scores found in the leaderboard.");
                return;
            }

            String[] columns = {"Player", "Score"};
            String[][] data = new String[rowCount][2];

            int i = 0;
            while (rs.next()) {
                data[i][0] = rs.getString("name");
                data[i][1] = String.valueOf(rs.getInt("score"));
                i++;
            }

            JTable table = new JTable(data, columns);
            JScrollPane scrollPane = new JScrollPane(table);
            JOptionPane.showMessageDialog(this, scrollPane, "Leaderboard", JOptionPane.PLAIN_MESSAGE);
            freshDb.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading leaderboard:\n" + e.getMessage());
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyPressed(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {
        if (gameOver) {
            int option = JOptionPane.showConfirmDialog(this, "Game Over! Your Score: " + score +
                "\nDo you want to view the leaderboard?", "Game Over", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                showLeaderboard();
            }

            int restart = JOptionPane.showConfirmDialog(this, "Do you want to play again?", "Restart?", JOptionPane.YES_NO_OPTION);
            if (restart == JOptionPane.YES_OPTION) {
                loadMap();
                resetPositions();
                lives = 3;
                score = 0;
                gameOver = false;
                gameLoop.start();
            } else {
                System.exit(0);
            }
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP: pacman.updateDirection('U'); pacman.image = pacmanUpImage; break;
            case KeyEvent.VK_DOWN: pacman.updateDirection('D'); pacman.image = pacmanDownImage; break;
            case KeyEvent.VK_LEFT: pacman.updateDirection('L'); pacman.image = pacmanLeftImage; break;
            case KeyEvent.VK_RIGHT: pacman.updateDirection('R'); pacman.image = pacmanRightImage; break;
        }
    }
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class PinballGameEnhanced2 extends JPanel implements ActionListener, KeyListener {

    // Panel dimensions.
    private final int PANEL_WIDTH = 500;
    private final int PANEL_HEIGHT = 600;
    
    // Game timer.
    private Timer timer;
    
    // Ball properties.
    private int ballX = 250, ballY = 300, ballDiameter = 15;
    private int ballDX = 3, ballDY = 4;
    
    // Score and lives.
    private int score = 0;
    private int lives = 3;
    private boolean gameOver = false;
    
    // Bumpers.
    private ArrayList<Bumper> bumpers;
    
    // Flippers.
    private Flipper leftFlipper;
    private Flipper rightFlipper;
    
    // Random instance for introducing variability.
    private Random rand = new Random();
    
    public PinballGameEnhanced2() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        
        // Game timer update every 15 ms.
        timer = new Timer(15, this);
        timer.start();
        
        // Initialize bumpers.
        bumpers = new ArrayList<>();
        // Bumper(centerX, centerY, radius, points)
        bumpers.add(new Bumper(150, 200, 20, 50));
        bumpers.add(new Bumper(350, 200, 20, 50));
        bumpers.add(new Bumper(250, 350, 25, 100));
        
        // Initialize flippers.
        // For the left flipper, pivot is at the left end; resting angle 0° and active angle -30°.
        leftFlipper = new Flipper(100, 500, 100, 15, 0, -30, true);
        // For the right flipper, pivot is at the right end; resting angle 0° and active angle 30°.
        rightFlipper = new Flipper(400, 500, 100, 15, 0, 30, false);
        
        // Randomize the ball's initial velocity.
        resetBall();
    }
    
    // Inner class for bumpers.
    class Bumper {
        int centerX, centerY, radius, points;
        public Bumper(int centerX, int centerY, int radius, int points) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            this.points = points;
        }
        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
        }
    }
    
    // Inner class for flippers.
    // The flipper rotates about a pivot point between two angles.
    class Flipper {
        int pivotX, pivotY;
        int length, thickness;
        double angle;      // current angle in degrees
        double restAngle;  // resting (default) angle
        double activeAngle;// active (flipped) angle
        boolean isLeft;    // true for left flipper, false for right
        
        public Flipper(int pivotX, int pivotY, int length, int thickness, double restAngle, double activeAngle, boolean isLeft) {
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.length = length;
            this.thickness = thickness;
            this.restAngle = restAngle;
            this.activeAngle = activeAngle;
            this.angle = restAngle;
            this.isLeft = isLeft;
        }
        
        // Set flipper to active or resting angle.
        public void setActive(boolean active) {
            angle = active ? activeAngle : restAngle;
        }
        
        // Draw flipper using rotation.
        public void draw(Graphics2D g2d) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(pivotX, pivotY);
            g2d.rotate(Math.toRadians(angle));
            g2d.setColor(Color.RED);
            // For left flipper, rectangle from pivot going right; for right, from pivot going left.
            if (isLeft) {
                g2d.fillRect(0, -thickness / 2, length, thickness);
            } else {
                g2d.fillRect(-length, -thickness / 2, length, thickness);
            }
            g2d.setTransform(old);
        }
        
        // Return the line that approximates the flipper's collision edge.
        public Line2D getEdgeLine() {
            double rad = Math.toRadians(angle);
            double x1 = pivotX, y1 = pivotY;
            double x2, y2;
            if (isLeft) {
                x2 = pivotX + length * Math.cos(rad);
                y2 = pivotY + length * Math.sin(rad);
            } else {
                x2 = pivotX - length * Math.cos(rad);
                y2 = pivotY - length * Math.sin(rad);
            }
            return new Line2D.Double(x1, y1, x2, y2);
        }
        
        public int getThickness() {
            return thickness;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Use Graphics2D for better rendering control.
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw the ball.
        g2d.setColor(Color.WHITE);
        g2d.fillOval(ballX, ballY, ballDiameter, ballDiameter);
        
        // Draw the flippers.
        leftFlipper.draw(g2d);
        rightFlipper.draw(g2d);
        
        // Draw the bumpers.
        for (Bumper b : bumpers) {
            b.draw(g);
        }
        
        // Draw the score and lives.
        g2d.setColor(Color.GREEN);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("Score: " + score, 10, 20);
        g2d.drawString("Lives: " + lives, PANEL_WIDTH - 80, 20);
        
        // Game over message.
        if (gameOver) {
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.setColor(Color.RED);
            g2d.drawString("GAME OVER", PANEL_WIDTH / 2 - 100, PANEL_HEIGHT / 2);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.drawString("Press 'R' to restart", PANEL_WIDTH / 2 - 80, PANEL_HEIGHT / 2 + 30);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            moveBall();
            checkCollisions();
        }
        repaint();
    }
    
    // Update ball position and handle wall bounces.
    private void moveBall() {
        ballX += ballDX;
        ballY += ballDY;
        
        // Bounce off left/right walls.
        if (ballX <= 0 || ballX + ballDiameter >= PANEL_WIDTH) {
            ballDX = -ballDX;
        }
        // Bounce off the top.
        if (ballY <= 0) {
            ballDY = -ballDY;
        }
        // Lose a life if the ball falls below the panel.
        if (ballY + ballDiameter >= PANEL_HEIGHT) {
            lives--;
            if (lives <= 0) {
                gameOver = true;
                timer.stop();
            } else {
                resetBall();
            }
        }
    }
    
    // Reset the ball to the center with random velocity.
    private void resetBall() {
        ballX = PANEL_WIDTH / 2;
        ballY = PANEL_HEIGHT / 2;
        // Random horizontal velocity between 2 and 4, with random direction.
        ballDX = (rand.nextBoolean() ? 1 : -1) * (2 + rand.nextInt(3));
        // Random vertical velocity between 3 and 5; negative to go upward.
        ballDY = -(3 + rand.nextInt(3));
    }
    
    // Check collisions with flippers and bumpers.
    private void checkCollisions() {
        // Get ball center.
        int ballCenterX = ballX + ballDiameter / 2;
        int ballCenterY = ballY + ballDiameter / 2;
        
        // Check collision with left flipper.
        if (checkFlipperCollision(leftFlipper)) {
            ballDY = -Math.abs(ballDY);
            ballDX = adjustHorizontalVelocity(leftFlipper);
        }
        // Check collision with right flipper.
        if (checkFlipperCollision(rightFlipper)) {
            ballDY = -Math.abs(ballDY);
            ballDX = adjustHorizontalVelocity(rightFlipper);
        }
        
        // Check collisions with bumpers.
        for (Bumper b : bumpers) {
            int dx = ballCenterX - b.centerX;
            int dy = ballCenterY - b.centerY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= b.radius + ballDiameter / 2) {
                // Invert vertical velocity and add a small random impulse.
                ballDY = -ballDY + (rand.nextInt(3) - 1);
                ballDX = ballDX + (rand.nextInt(3) - 1);
                score += b.points;
                // Advance the ball to help avoid repeated collisions.
                ballX += ballDX;
                ballY += ballDY;
            }
        }
    }
    
    // Adjust horizontal velocity based on ball's position relative to the flipper.
    private int adjustHorizontalVelocity(Flipper flipper) {
        int flipperMid = flipper.pivotX;  // Using pivot as reference.
        int ballCenter = ballX + ballDiameter / 2;
        if (flipper.isLeft) {
            return (ballCenter < flipperMid) ? -Math.abs(ballDX) : Math.abs(ballDX);
        } else {
            return (ballCenter > flipperMid) ? Math.abs(ballDX) : -Math.abs(ballDX);
        }
    }
    
    // Check collision between the ball and a flipper based on the flipper's active edge.
    private boolean checkFlipperCollision(Flipper flipper) {
        Line2D line = flipper.getEdgeLine();
        double distance = line.ptSegDist(ballX + ballDiameter / 2, ballY + ballDiameter / 2);
        return distance <= ballDiameter / 2 + flipper.getThickness() / 2;
    }
    
    // KeyListener methods.
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        // Activate flippers.
        if (key == KeyEvent.VK_LEFT) {
            leftFlipper.setActive(true);
        } else if (key == KeyEvent.VK_RIGHT) {
            rightFlipper.setActive(true);
        } else if (key == KeyEvent.VK_R && gameOver) {
            // Restart the game.
            gameOver = false;
            lives = 3;
            score = 0;
            resetBall();
            timer.start();
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            leftFlipper.setActive(false);
        } else if (key == KeyEvent.VK_RIGHT) {
            rightFlipper.setActive(false);
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    // Main method to launch the game.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Enhanced Pinball Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            PinballGameEnhanced2 game = new PinballGameEnhanced2();
            frame.add(game);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
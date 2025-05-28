import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PacManGame extends JFrame {

    public PacManGame() {
        initUI();
    }

    private void initUI() {
        GameBoard board = new GameBoard();
        add(board);

        setTitle("Pac-Man");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(380, 420); // Ajustado para o tamanho do tabuleiro
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            PacManGame ex = new PacManGame();
            ex.setVisible(true);
        });
    }
}

class GameBoard extends JPanel implements ActionListener {

    private final int TILE_SIZE = 20; // Tamanho de cada célula do grid
    private final int N_ROWS = 17;    // Número de linhas
    private final int N_COLS = 17;    // Número de colunas
    private final int SCREEN_WIDTH = N_COLS * TILE_SIZE;
    private final int SCREEN_HEIGHT = N_ROWS * TILE_SIZE;

    private Image pacmanImage, ghostImage, frightenedGhostImage, powerPelletImage;
    private Image pacmanUp, pacmanDown, pacmanLeft, pacmanRight;

    private int pacmanX, pacmanY, pacmanDx, pacmanDy;
    private int reqDx, reqDy; // Direção requisitada pelo jogador

    private final int PACMAN_SPEED = TILE_SIZE; // Pac-Man se move uma célula por vez

    private List<Ghost> ghosts;
    private boolean frightenedMode = false;
    private int frightenedTimer = 0;
    private final int FRIGHTENED_DURATION = 200; // Cerca de 10 segundos (200 * 50ms)

    private int lives;
    private int score;
    private int pelletsRemaining;

    private Timer timer;
    private final int DELAY = 150; // Aumentado para melhor jogabilidade (movimento mais lento)

    // 0 = Vazio, 1 = Parede, 2 = Pastilha, 3 = Pastilha de Poder
    private int[][] maze = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,1},
        {1,3,1,1,2,1,1,2,1,2,1,1,2,1,1,3,1},
        {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
        {1,2,1,1,2,1,2,1,1,1,2,1,2,1,1,2,1},
        {1,2,2,2,2,1,2,2,1,2,2,1,2,2,2,2,1},
        {1,1,1,1,2,1,1,0,1,0,1,1,2,1,1,1,1}, // 0 para espaço da casa dos fantasmas
        {0,0,0,1,2,1,0,0,0,0,0,1,2,1,0,0,0}, // Túnel e casa dos fantasmas
        {1,1,1,1,2,1,0,1,1,1,0,1,2,1,1,1,1}, // 0 para espaço da casa dos fantasmas
        {1,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,1},
        {1,2,1,1,2,1,1,2,1,2,1,1,2,1,1,2,1},
        {1,3,2,2,2,2,2,2,2,2,2,2,2,2,2,3,1},
        {1,2,1,1,2,1,2,1,1,1,2,1,2,1,1,2,1},
        {1,2,2,2,2,1,2,2,1,2,2,1,2,2,2,2,1},
        {1,2,2,1,1,1,1,2,1,2,1,1,1,1,2,2,1},
        {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    private boolean inGame = false;
    private boolean dying = false;

    private final int[] validSpeeds = {1, 2, 4, 5, 10, 20}; // Divisores de TILE_SIZE
    private int currentSpeedIndex = 2; // PACMAN_SPEED = TILE_SIZE / validSpeeds[currentSpeedIndex]

    public GameBoard() {
        loadImages();
        initVariables();
        addKeyListener(new TAdapter());
        setFocusable(true);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT + 40)); // Espaço para score/vidas
        initGame();
    }

    private void loadImages() {
        // Usar primitivas de desenho para Pac-Man e fantasmas para simplicidade
        // Se quiser usar imagens:
        // pacmanImage = new ImageIcon("path/to/pacman.png").getImage();
        // ghostImage = new ImageIcon("path/to/ghost.png").getImage();
        // powerPelletImage = new ImageIcon("path/to/power_pellet.png").getImage();
        // ... carregar pacmanUp, pacmanDown, etc.
    }
    
    private void initVariables() {
        pelletsRemaining = 0;
        for (int i = 0; i < N_ROWS; i++) {
            for (int j = 0; j < N_COLS; j++) {
                if (maze[i][j] == 2 || maze[i][j] == 3) {
                    pelletsRemaining++;
                }
            }
        }
        score = 0;
        lives = 3;
        ghosts = new ArrayList<>();
    }

    private void initGame() {
        inGame = true;
        dying = false;
        frightenedMode = false;
        frightenedTimer = 0;

        pacmanX = 8 * TILE_SIZE; // Posição inicial do Pac-Man
        pacmanY = 11 * TILE_SIZE;
        pacmanDx = 0;
        pacmanDy = 0;
        reqDx = 0;
        reqDy = 0;

        ghosts.clear();
        // Posições iniciais dos fantasmas (ajuste conforme necessário)
        ghosts.add(new Ghost(7 * TILE_SIZE, 7 * TILE_SIZE, Color.RED));
        ghosts.add(new Ghost(8 * TILE_SIZE, 7 * TILE_SIZE, Color.PINK));
        ghosts.add(new Ghost(9 * TILE_SIZE, 7 * TILE_SIZE, Color.CYAN));
        ghosts.add(new Ghost(8 * TILE_SIZE, 6 * TILE_SIZE, Color.ORANGE));


        if (timer == null) {
            timer = new Timer(DELAY, this);
            timer.start();
        } else {
            timer.restart();
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, d.width, d.height);

        drawMaze(g2d);
        drawScore(g2d);
        drawLives(g2d);

        if (inGame) {
            drawPacman(g2d);
            drawGhosts(g2d);
        } else {
            showIntroScreen(g2d);
        }

        Toolkit.getDefaultToolkit().sync();
        g2d.dispose();
    }
    
    private void drawMaze(Graphics2D g2d) {
        for (int i = 0; i < N_ROWS; i++) {
            for (int j = 0; j < N_COLS; j++) {
                int cellX = j * TILE_SIZE;
                int cellY = i * TILE_SIZE;

                if (maze[i][j] == 1) { // Parede
                    g2d.setColor(Color.BLUE);
                    g2d.fillRect(cellX, cellY, TILE_SIZE, TILE_SIZE);
                } else if (maze[i][j] == 2) { // Pastilha
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(cellX + TILE_SIZE / 2 - 2, cellY + TILE_SIZE / 2 - 2, 4, 4);
                } else if (maze[i][j] == 3) { // Pastilha de Poder
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(cellX + TILE_SIZE / 3, cellY + TILE_SIZE / 3, TILE_SIZE / 2, TILE_SIZE / 2);
                }
            }
        }
    }

    private int pacmanAnimPos = 0;
    private int pacmanAnimDir = 1;
    private final int PACMAN_ANIM_DELAY = 2;
    private int pacmanAnimCount = PACMAN_ANIM_DELAY;

    private void drawPacman(Graphics2D g2d) {
        g2d.setColor(Color.YELLOW);
        int angleStart = 0;
        int angleExtent = 360;

        // Animação da boca
        if (pacmanDx != 0 || pacmanDy != 0) { // Só anima se estiver se movendo
             if (pacmanAnimCount < PACMAN_ANIM_DELAY) {
                pacmanAnimCount++;
            } else {
                pacmanAnimCount = 0;
                pacmanAnimPos = pacmanAnimPos + pacmanAnimDir;
                if (pacmanAnimPos == 0 || pacmanAnimPos == 3) { // Limites da animação (0 a 3)
                    pacmanAnimDir = -pacmanAnimDir;
                }
            }
        } else { // Boca fechada se parado
            pacmanAnimPos = 0;
        }


        int mouthAngle = pacmanAnimPos * 15; // Abre até 45 graus (3 * 15)

        if (reqDx == -1 || (reqDx == 0 && pacmanDx == -1)) { // Esquerda
            angleStart = 180 + mouthAngle / 2;
            angleExtent = 360 - mouthAngle;
        } else if (reqDx == 1 || (reqDx == 0 && pacmanDx == 1)) { // Direita
            angleStart = mouthAngle / 2;
            angleExtent = 360 - mouthAngle;
        } else if (reqDy == -1 || (reqDy == 0 && pacmanDy == -1)) { // Cima
            angleStart = 90 + mouthAngle / 2;
            angleExtent = 360 - mouthAngle;
        } else if (reqDy == 1 || (reqDy == 0 && pacmanDy == 1)) { // Baixo
            angleStart = 270 + mouthAngle / 2;
            angleExtent = 360 - mouthAngle;
        } else { // Parado, olhando para a direita por padrão
             angleStart = mouthAngle / 2;
             angleExtent = 360 - mouthAngle;
        }


        g2d.fillArc(pacmanX, pacmanY, TILE_SIZE, TILE_SIZE, angleStart, angleExtent);
    }

    private void drawGhosts(Graphics2D g2d) {
        for (Ghost ghost : ghosts) {
            ghost.draw(g2d, TILE_SIZE, frightenedMode);
        }
    }

    private void drawScore(Graphics2D g) {
        String s;
        g.setFont(new Font("Helvetica", Font.BOLD, 14));
        g.setColor(new Color(96, 128, 255));
        s = "Score: " + score;
        g.drawString(s, SCREEN_WIDTH / 2 - 40, SCREEN_HEIGHT + 15); // Posição ajustada
    }
    
    private void drawLives(Graphics2D g) {
        g.setColor(Color.YELLOW);
        for (int i = 0; i < lives; i++) {
            g.fillOval(TILE_SIZE + i * (TILE_SIZE + 5), SCREEN_HEIGHT + 5, TILE_SIZE-5, TILE_SIZE-5);
        }
    }

    private void showIntroScreen(Graphics2D g2d) {
        String startMsg = "Pressione ENTER para iniciar";
        if (dying) {
            startMsg = "Fim de Jogo! Score: " + score + ". ENTER para reiniciar.";
        } else if (pelletsRemaining == 0) {
            startMsg = "Você Venceu! Score: " + score + ". ENTER para reiniciar.";
        }
        
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Helvetica", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();
        int msgWidth = fm.stringWidth(startMsg);
        g2d.drawString(startMsg, (SCREEN_WIDTH - msgWidth) / 2, SCREEN_HEIGHT / 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (inGame) {
            checkCollisions();
            movePacman();
            moveGhosts();
            checkMaze();
            updateFrightenedMode();
        }
        repaint();
    }

    private void updateFrightenedMode() {
        if (frightenedMode) {
            frightenedTimer--;
            if (frightenedTimer <= 0) {
                frightenedMode = false;
                // Restaurar estado normal dos fantasmas (se necessário, por exemplo, velocidade)
                for (Ghost ghost : ghosts) {
                    ghost.isEaten = false; // Se foi comido, deve estar voltando para casa
                }
            }
        }
    }

    private void movePacman() {
        int newPacmanX = pacmanX;
        int newPacmanY = pacmanY;

        // Tentar mover na direção requisitada
        if (reqDx != 0 || reqDy != 0) {
            newPacmanX = pacmanX + reqDx * PACMAN_SPEED;
            newPacmanY = pacmanY + reqDy * PACMAN_SPEED;

            // Verificar túnel
            if (newPacmanX < 0) newPacmanX = (N_COLS - 1) * TILE_SIZE;
            if (newPacmanX >= N_COLS * TILE_SIZE) newPacmanX = 0;
            if (newPacmanY < 0) newPacmanY = (N_ROWS - 1) * TILE_SIZE;
            if (newPacmanY >= N_ROWS * TILE_SIZE) newPacmanY = 0;


            int gridX = newPacmanX / TILE_SIZE;
            int gridY = newPacmanY / TILE_SIZE;

            if (maze[gridY][gridX] != 1) { // Se não for parede na direção requisitada
                pacmanDx = reqDx;
                pacmanDy = reqDy;
                pacmanX = newPacmanX;
                pacmanY = newPacmanY;
            } else { // Tentar continuar na direção atual se a requisitada for bloqueada
                newPacmanX = pacmanX + pacmanDx * PACMAN_SPEED;
                newPacmanY = pacmanY + pacmanDy * PACMAN_SPEED;

                // Verificar túnel
                if (newPacmanX < 0) newPacmanX = (N_COLS - 1) * TILE_SIZE;
                if (newPacmanX >= N_COLS * TILE_SIZE) newPacmanX = 0;
                if (newPacmanY < 0) newPacmanY = (N_ROWS - 1) * TILE_SIZE;
                if (newPacmanY >= N_ROWS * TILE_SIZE) newPacmanY = 0;

                gridX = newPacmanX / TILE_SIZE;
                gridY = newPacmanY / TILE_SIZE;
                if (maze[gridY][gridX] != 1) {
                    pacmanX = newPacmanX;
                    pacmanY = newPacmanY;
                } else { // Parar se a direção atual também for bloqueada
                    pacmanDx = 0;
                    pacmanDy = 0;
                }
            }
        } else { // Se não houver requisição, continuar na direção atual
            newPacmanX = pacmanX + pacmanDx * PACMAN_SPEED;
            newPacmanY = pacmanY + pacmanDy * PACMAN_SPEED;
            
            // Verificar túnel
            if (newPacmanX < 0) newPacmanX = (N_COLS - 1) * TILE_SIZE;
            if (newPacmanX >= N_COLS * TILE_SIZE) newPacmanX = 0;
            if (newPacmanY < 0) newPacmanY = (N_ROWS - 1) * TILE_SIZE;
            if (newPacmanY >= N_ROWS * TILE_SIZE) newPacmanY = 0;

            int gridX = newPacmanX / TILE_SIZE;
            int gridY = newPacmanY / TILE_SIZE;
            if (maze[gridY][gridX] != 1) {
                pacmanX = newPacmanX;
                pacmanY = newPacmanY;
            } else { // Parar se bloqueado
                pacmanDx = 0;
                pacmanDy = 0;
            }
        }

        // Comer pastilhas
        int currentGridX = pacmanX / TILE_SIZE;
        int currentGridY = pacmanY / TILE_SIZE;

        if (maze[currentGridY][currentGridX] == 2) { // Pastilha normal
            maze[currentGridY][currentGridX] = 0; // Remove pastilha
            score += 10;
            pelletsRemaining--;
        } else if (maze[currentGridY][currentGridX] == 3) { // Pastilha de poder
            maze[currentGridY][currentGridX] = 0;
            score += 50;
            pelletsRemaining--;
            frightenedMode = true;
            frightenedTimer = FRIGHTENED_DURATION;
            for(Ghost ghost : ghosts) {
                ghost.isEaten = false; // Reseta estado de "comido" ao pegar nova pastilha
            }
        }
    }

    private void moveGhosts() {
        for (Ghost ghost : ghosts) {
            ghost.move(maze, TILE_SIZE, N_COLS, N_ROWS, pacmanX, pacmanY, frightenedMode);
        }
    }

    private void checkCollisions() {
        Rectangle pacmanBounds = new Rectangle(pacmanX, pacmanY, TILE_SIZE, TILE_SIZE);

        for (Ghost ghost : ghosts) {
            Rectangle ghostBounds = new Rectangle(ghost.x, ghost.y, TILE_SIZE, TILE_SIZE);
            if (pacmanBounds.intersects(ghostBounds)) {
                if (frightenedMode && !ghost.isEaten) {
                    score += 200; // Pontos por comer fantasma
                    ghost.isEaten = true;
                    // O fantasma deve agora retornar à sua "casa"
                    // Simplificação: teletransporta para a casa ou implementa pathfinding
                    ghost.x = 8 * TILE_SIZE; 
                    ghost.y = 7 * TILE_SIZE;
                } else if (!frightenedMode && !ghost.isEaten) {
                    // Pac-Man é pego
                    lives--;
                    dying = true; // Usar para lógica de animação de morte ou reinício de nível
                    if (lives <= 0) {
                        inGame = false; // Game Over
                    } else {
                        // Reiniciar posições para continuar o nível
                        continueLevel();
                    }
                    return; // Sai da checagem de colisão após uma morte
                }
            }
        }
    }
    
    private void continueLevel() {
        // Reinicia posições do Pac-Man e fantasmas
        pacmanX = 8 * TILE_SIZE;
        pacmanY = 11 * TILE_SIZE;
        pacmanDx = 0;
        pacmanDy = 0;
        reqDx = 0;
        reqDy = 0;

        // Posições iniciais dos fantasmas (ajuste conforme necessário)
        ghosts.get(0).x = 7 * TILE_SIZE; ghosts.get(0).y = 7 * TILE_SIZE; ghosts.get(0).isEaten = false;
        ghosts.get(1).x = 8 * TILE_SIZE; ghosts.get(1).y = 7 * TILE_SIZE; ghosts.get(1).isEaten = false;
        ghosts.get(2).x = 9 * TILE_SIZE; ghosts.get(2).y = 7 * TILE_SIZE; ghosts.get(2).isEaten = false;
        ghosts.get(3).x = 8 * TILE_SIZE; ghosts.get(3).y = 6 * TILE_SIZE; ghosts.get(3).isEaten = false;
        
        frightenedMode = false;
        frightenedTimer = 0;
    }

    private void checkMaze() {
        if (pelletsRemaining == 0 && inGame) {
            inGame = false; // Vitória!
            // Poderia adicionar lógica para próximo nível aqui
        }
    }

    class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (inGame) {
                if (key == KeyEvent.VK_LEFT) {
                    reqDx = -1;
                    reqDy = 0;
                } else if (key == KeyEvent.VK_RIGHT) {
                    reqDx = 1;
                    reqDy = 0;
                } else if (key == KeyEvent.VK_UP) {
                    reqDx = 0;
                    reqDy = -1;
                } else if (key == KeyEvent.VK_DOWN) {
                    reqDx = 0;
                    reqDy = 1;
                } else if (key == KeyEvent.VK_ESCAPE && timer.isRunning()) {
                    inGame = false; // Pausa simples, poderia ser mais elaborada
                }
            } else {
                if (key == KeyEvent.VK_ENTER) {
                    // Reiniciar o jogo
                    initVariables();
                    initGame();
                }
            }
        }
    }
}

class Ghost {
    int x, y, dx, dy;
    Color color;
    boolean isEaten = false;
    private Random random = new Random();
    private final int GHOST_SPEED;

    // Coordenadas da "casa" dos fantasmas para onde retornar quando comidos
    private final int GHOST_HOME_X; 
    private final int GHOST_HOME_Y;

    public Ghost(int startX, int startY, Color c) {
        this.x = startX;
        this.y = startY;
        this.color = c;
        this.GHOST_SPEED = 20; // Mesma que TILE_SIZE

        // Definindo uma casa padrão, idealmente seria configurável
        this.GHOST_HOME_X = 8 * GHOST_SPEED; // 8 é um exemplo de coluna
        this.GHOST_HOME_Y = 7 * GHOST_SPEED; // 7 é um exemplo de linha
    }

    public void draw(Graphics2D g2d, int tileSize, boolean frightenedMode) {
        if (frightenedMode && !isEaten) {
            g2d.setColor(Color.BLUE.darker()); // Cor quando assustado
            // Poderia adicionar olhos piscando ou algo assim
        } else if (isEaten) {
            g2d.setColor(Color.WHITE); // Cor quando "comido" (apenas olhos voltando)
            // Desenhar apenas olhos
            g2d.fillOval(x + tileSize / 4, y + tileSize / 4, tileSize / 4, tileSize / 4);
            g2d.fillOval(x + tileSize / 2, y + tileSize / 4, tileSize / 4, tileSize / 4);
            return; // Não desenha o corpo do fantasma
        }
        else {
            g2d.setColor(color);
        }
        
        // Corpo do fantasma
        g2d.fillArc(x, y, tileSize, tileSize, 0, 180);
        g2d.fillRect(x, y + tileSize / 2, tileSize, tileSize / 2);

        // Base ondulada
        int waveSize = tileSize / 4;
        for (int i = 0; i < 4; i++) {
            g2d.fillArc(x + i * waveSize, y + tileSize - waveSize -2, waveSize, waveSize, 0, (i%2 == 0) ? 180 : -180);
        }

        // Olhos
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + tileSize / 5, y + tileSize / 4, tileSize / 3, tileSize / 3);
        g2d.fillOval(x + tileSize / 2, y + tileSize / 4, tileSize / 3, tileSize / 3);

        g2d.setColor(Color.BLACK); // Pupilas
        int pupilXOffset = 0;
        int pupilYOffset = 0;
        if (dx == -1) pupilXOffset = -tileSize/12;
        if (dx == 1) pupilXOffset = tileSize/12;
        if (dy == -1) pupilYOffset = -tileSize/12;
        if (dy == 1) pupilYOffset = tileSize/12;

        g2d.fillOval(x + tileSize / 5 + tileSize/10 + pupilXOffset, y + tileSize / 4 + tileSize/10 + pupilYOffset, tileSize / 6, tileSize / 6);
        g2d.fillOval(x + tileSize / 2 + tileSize/10 + pupilXOffset, y + tileSize / 4 + tileSize/10 + pupilYOffset, tileSize / 6, tileSize / 6);
    }

    public void move(int[][] maze, int tileSize, int numCols, int numRows, int pacmanX, int pacmanY, boolean isFrightened) {
        if (x % tileSize != 0 || y % tileSize != 0) { // Só decide nova direção se estiver alinhado com o grid
            x += dx * (GHOST_SPEED/ (isFrightened && !isEaten ? 2 : 1) ); // Mais lento se assustado
            y += dy * (GHOST_SPEED/ (isFrightened && !isEaten ? 2 : 1) );
            return;
        }

        int currentGridX = x / tileSize;
        int currentGridY = y / tileSize;

        // Lógica de pathfinding para voltar para casa se comido
        if (isEaten) {
            if (x == GHOST_HOME_X && y == GHOST_HOME_Y) {
                isEaten = false; // Chegou em casa
                // Poderia ter um timer antes de sair da casa de novo
            } else {
                // IA simples para voltar para casa: move em direção a GHOST_HOME
                int targetX = GHOST_HOME_X;
                int targetY = GHOST_HOME_Y;
                
                // Priorizar movimento horizontal ou vertical para simplificar
                if (x < targetX && maze[currentGridY][currentGridX+1] !=1) dx = 1;
                else if (x > targetX && maze[currentGridY][currentGridX-1] !=1) dx = -1;
                else dx = 0;

                if (dx == 0) { // Se não moveu horizontalmente ou está bloqueado, tenta vertical
                    if (y < targetY && maze[currentGridY+1][currentGridX] !=1) dy = 1;
                    else if (y > targetY && maze[currentGridY-1][currentGridX] !=1) dy = -1;
                    else dy = 0;
                } else {
                    dy = 0; // Se moveu horizontalmente, não move verticalmente no mesmo passo
                }
                 // Se ainda assim não conseguiu se mover (ex: preso num canto ao tentar voltar)
                if (dx == 0 && dy == 0) {
                     // Tenta uma direção aleatória válida para sair do bloqueio
                    List<Point> validMoves = getValidMoves(maze, tileSize, numCols, numRows, -dx, -dy); // Evita voltar
                    if (!validMoves.isEmpty()) {
                        Point move = validMoves.get(random.nextInt(validMoves.size()));
                        dx = move.x;
                        dy = move.y;
                    } else { // Preso mesmo, tenta qualquer direção
                        validMoves = getValidMoves(maze, tileSize, numCols, numRows, 10, 10); // 10,10 para não excluir nenhuma direção
                        if(!validMoves.isEmpty()){
                            Point move = validMoves.get(random.nextInt(validMoves.size()));
                            dx = move.x;
                            dy = move.y;
                        }
                    }
                }

                x += dx * GHOST_SPEED;
                y += dy * GHOST_SPEED;
                return;
            }
        }


        List<Point> validMoves = getValidMoves(maze, tileSize, numCols, numRows, dx, dy);

        if (validMoves.isEmpty()) { // Preso
            dx = -dx; // Tenta inverter direção
            dy = -dy;
            // Se ainda preso, não faz nada neste turno, ou tenta uma lógica mais complexa
             x += dx * GHOST_SPEED / (isFrightened ? 2 : 1); // Mais lento se assustado
             y += dy * GHOST_SPEED / (isFrightened ? 2 : 1);
            return;
        }

        // IA Simples:
        // Se assustado, move aleatoriamente para uma direção válida (não para trás, se possível)
        // Se não assustado, tenta se aproximar do Pac-Man (muito simples)
        if (isFrightened) {
            // Tenta não voltar, a menos que seja a única opção
            List<Point> nonReversingMoves = new ArrayList<>();
            for(Point move : validMoves) {
                if (!(move.x == -dx && move.y == -dy)) {
                    nonReversingMoves.add(move);
                }
            }
            if (!nonReversingMoves.isEmpty()) {
                 Point move = nonReversingMoves.get(random.nextInt(nonReversingMoves.size()));
                 dx = move.x;
                 dy = move.y;
            } else { // Única opção é voltar
                 Point move = validMoves.get(random.nextInt(validMoves.size()));
                 dx = move.x;
                 dy = move.y;
            }

        } else { // Comportamento normal (tentativa de perseguir)
            Point bestMove = null;
            double minDistance = Double.MAX_VALUE;

            for (Point move : validMoves) {
                // Evitar voltar, a menos que seja a única opção ou num beco sem saída
                if (validMoves.size() > 1 && move.x == -dx && move.y == -dy) {
                    continue;
                }

                int nextX = x + move.x * tileSize;
                int nextY = y + move.y * tileSize;
                double distanceToPacman = Math.sqrt(Math.pow(nextX - pacmanX, 2) + Math.pow(nextY - pacmanY, 2));

                if (distanceToPacman < minDistance) {
                    minDistance = distanceToPacman;
                    bestMove = move;
                }
            }
            if (bestMove != null) {
                dx = bestMove.x;
                dy = bestMove.y;
            } else if (!validMoves.isEmpty()) { // Se todos os movimentos foram "voltar" e foram ignorados
                Point move = validMoves.get(random.nextInt(validMoves.size())); // Escolhe um aleatório dos válidos
                dx = move.x;
                dy = move.y;
            }
        }
        
        x += dx * GHOST_SPEED / (isFrightened && !isEaten ? 2 : 1);
        y += dy * GHOST_SPEED / (isFrightened && !isEaten ? 2 : 1);

        // Túnel para fantasmas
        if (x < 0) x = (numCols - 1) * tileSize;
        if (x >= numCols * tileSize) x = 0;
    }

    private List<Point> getValidMoves(int[][] maze, int tileSize, int numCols, int numRows, int currentDx, int currentDy) {
        List<Point> moves = new ArrayList<>();
        int gridX = x / tileSize;
        int gridY = y / tileSize;

        int[] tryDx = {0, 0, 1, -1}; // cima, baixo, direita, esquerda
        int[] tryDy = {-1, 1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int nextGridX = gridX + tryDx[i];
            int nextGridY = gridY + tryDy[i];

            // Checar limites do labirinto
            if (nextGridX >= 0 && nextGridX < numCols && nextGridY >= 0 && nextGridY < numRows) {
                // Checar se não é parede
                // Fantasmas podem passar por 0 (espaço vazio), 2 (pastilha), 3 (power pellet)
                if (maze[nextGridY][nextGridX] != 1) { 
                    // Não permite inversão de direção imediata, a menos que seja um beco sem saída
                    // (essa lógica é parcialmente tratada no chamador da função)
                    moves.add(new Point(tryDx[i], tryDy[i]));
                }
            } else if ((nextGridX < 0 && gridX == 0) || (nextGridX >= numCols && gridX == numCols - 1) ) {
                 // Permite movimento para túnel (se o labirinto tiver)
                 // Este código trata túneis no movePacman e moveGhosts diretamente.
                 // Aqui, apenas certificamos que o fantasma pode se mover para a borda.
                 if (maze[gridY][gridX == 0 ? numCols-1 : 0] != 1) { // Verifica a célula do outro lado do túnel
                     moves.add(new Point(tryDx[i], tryDy[i]));
                 }
            }
        }
        return moves;
    }
}

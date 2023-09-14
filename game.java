import javax.swing.*;
import java.awt.*;
import java.awt.event.*;                                    //import all libraries
import java.util.*;

public class game {    

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        int grid = 0;

        while (grid < 10 || grid > 18) {                              
            System.out.println("Please enter a grid size between 10 and 18");       //take in value until value grid size is given
            grid = scan.nextInt();
        }
        scan.close();

        Board board = new Board();
        board.setup(board, grid);                               // sets up board and grid
    }
}

class Board extends JPanel implements MouseListener {       //use JPanel for making the window and MouseListener for reading mouse inputs
    private static final long serialVersionUID = 1L;
    Scanner scan = new Scanner(System.in);

    int grid = 0;                                           //size of grid
    int cells[][];                                          //stores value of each cell in grid
    int x, y;                                               //stores last location of mouse click       
    int dimension;                                          //used for measurement of size of window
    int cellsRemaining = 0;                                 //keeps track of how many cells are left
    int timer = 0;                                          //timer
    boolean first = true;                                   //checks to see if the first click, used for safety net
    boolean flood = true;                                   //checks to see if should flood open more boxes after opening empty cell
    boolean finish = false;                                 //boolean to check if game finished 
    Color[] color = {Color.BLUE, Color.GREEN, Color.RED, Color.MAGENTA, Color.BLACK, Color.GRAY, Color.RED.darker(), Color.CYAN};     //array for color of each number

    public void setup(Board f, int grid) {                  //sets up the window and variables
        this.grid = grid;                                   
        dimension = 800 / grid;                             //size of window is roughly 800
        addMouseListener(this);
        cellsRemaining = (grid*grid)-grid;                  //updates to maximum possible number of cells

        cells = new int[grid][grid];                        //gives array length
        for (int i = 0; i < grid; i++) {
            for (int j = 1; j < grid; j++) {
                if (Math.random() <= 0.175) {               //17.5% chance of bomb
                    cells[i][j] = 9;                        //9 means a bomb is in the cell
                }
            }
        }

        JFrame window = new JFrame();
        window.setTitle("MineSweeper");                     //create window
        window.add(f);
        window.setSize(((800 / grid) * grid) + 17, ((800 / grid) * grid) + 40); // Additions account for size of border
        window.setVisible(true);
        window.setResizable(false);                         //stop from resizing window
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);                  //quit program if window is closed
        window.setBackground(Color.LIGHT_GRAY);

        try {
            timer();                                        //make timer work
        }
        catch (InterruptedException e) {
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);        
        setBackground(Color.LIGHT_GRAY);

        g.setColor(Color.BLACK);
        for (int i = 0; i <= grid; i++) {
            g.drawLine(dimension * i, dimension, dimension * i, dimension * grid);  //set lines
            g.drawLine(0, dimension * i, dimension * grid, dimension * i);
        }
        g.setColor(Color.GRAY);
        g.fillRect(0,0,dimension*grid,dimension);                                   //color tiles gray
    }

    public void timer() throws InterruptedException {           //method for timer 
        Graphics g = getGraphics();
        Font font = new Font("Arial Rounded MT Bold",Font.PLAIN,30+(100/grid));   
        g.setFont(font);
        while (!finish) {                           //update timer
            g.setColor(Color.GRAY);
            g.clearRect(600,10,200,30);
            g.fillRect(600,10,200,30);
            g.setColor(Color.BLACK);
            g.drawString(timer + "",600,40);
            Thread.sleep(990);                      //wait a second
            timer+=1;
        }
    }

    public void update(int x, int y) {              //method for updating cells when clicked
        if (cells[x][y] >700) {                     //if already updates
            return;
        }
        Graphics g = getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(x * dimension + 1, y * dimension + 1, dimension - 1, dimension - 1);
        cellsRemaining-=1;                          //update cell count
        if (first) {                                //if its the first click
            first = !first;
            cells[x][y] = 0;                        //make it not a bomb
            safetyNet(x, y);                        //make all neighbours not a bomb
            for (int i = 0; i < grid; i++) {
                for (int j = 1; j < grid; j++) {
                    if (cells[i][j]%10 != 9) {
                        checkNeighbours(i, j);  	//check if bombs are neighbours
                    }  
                    else {
                        cellsRemaining-=1;          //if bomb, thats one less cell to find
                    } 
                }
            }
            cells[x][y] += 500;                     //mark cell as updated
        }
        if (cells[x][y] % 10 == 9) {                //if its a bomb (ends with 9)
            g.setColor(Color.RED);
            g.fillRect(x * dimension + 1, y * dimension + 1, dimension - 1, dimension - 1);
            g.setColor(Color.BLACK);
            g.fillOval(x * dimension + (dimension / 4), y * dimension + (dimension / 4), dimension / 2, dimension / 2);         //draw bomb
            try {
                if (!finish)                        //stops recursion
                loseGame();                         //the game is lost
            } 
            catch (InterruptedException e) {}
        }

        else if (cells[x][y]%10 != 0 && !finish) {              //if the cell has a bomb neighbour
            g.setColor(color[cells[x][y]%10-1]);                  //get the color for the number
            Font font = new Font("Arial Rounded MT Bold",Font.PLAIN,45-grid);   
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics();
            String temp = (cells[x][y]%10) + "";

            g.drawString(temp,(x*dimension + dimension/2 - (metrics.stringWidth(temp)/2)),((y*dimension + dimension/2) - (metrics.getAscent() + metrics.getDescent())/2) + metrics.getAscent());    //draw number on cell
        }

        else if (flood) { 
            flood = !flood;                             //flood if the cell has no bomb neighbours
            floodFill();
        }
        if (cellsRemaining==0) {                        //if all cells are clicked
            try {
                if(!finish)                             //stops recursion
                winGame();                              //the game is won
            }
            catch (InterruptedException e) {}
        }
    }

    public void flag(int a,int b) {             //method for drawing flags
        Graphics2D g2 = (Graphics2D) getGraphics();
        if (cells[a][b]<100) {                  //if there isnt a flag
            g2.setColor(Color.RED);
            g2.drawLine(x*dimension+(dimension/4),y*dimension+(dimension/8),x*dimension+(dimension/4),(y+1)*dimension-(dimension/8));
            g2.fillPolygon(new int[] {x*dimension+(dimension/4),x*dimension+(dimension/4),(x+1)*dimension-(dimension/8)}, new int[] {y*dimension+(dimension/8),y*dimension+(dimension/2),y*dimension+(dimension/4)},3);     //draw a flag
            cells[a][b]+=100;                   //add 100 to show that the cell has a flag
        }

        else if (cells[a][b]>=100 && !finish) {                 //if there is a flag
            setBackground(Color.LIGHT_GRAY);    
            cells[a][b]-=100;
            g2.clearRect(x*dimension+1,y*dimension+1,dimension-1,dimension-1);      //remove flag
        }

        else if (cells[a][b]%10!=9 && finish) {                 //if the cell was flagged and wasnt a bomb
            g2.setColor(Color.RED.darker());    
            g2.setStroke(new BasicStroke(3));
            g2.drawLine((dimension*a)+(dimension/8),(dimension*b)+(dimension/8),(dimension*(a+1)-(dimension/8)),(dimension*(b+1)-(dimension/8)));       //draw an X
            g2.drawLine((dimension*(a+1)-(dimension/8)),(dimension*b)+(dimension/8),(dimension*a)+(dimension/8),(dimension*(b+1)-(dimension/8)));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {            //method for checking mouse clicks

        if (finish) {                                   //doesnt work after game is ended
            return;
        }
        else {
            x = (e.getX()/dimension);
            y = (e.getY()/dimension);                   //get position of mouse
            if (cells[x][y]<500 && y>0) {
                if (e.getButton() == MouseEvent.BUTTON1) {          //if left click
                    flood=true;                                     //allow flood
                    cells[x][y]+=500;                               //add 500 to mark as revealed
                    update(x,y);                                    //update cell
                }

                else if (e.getButton() == MouseEvent.BUTTON3) {     //if right click
                    flag(x,y);                                      //call flag method
                }
            }
        }
    }

    /**This method checks all neighbours 
     * of a tile to see if there are any bombs */

    public void checkNeighbours(int x,int y) {                      //method for checking if bombs are neighbours 
        int rowS = Math.max(x-1,0);
        int rowF = Math.min(x+1,cells.length-1);                    //stops out of bounds errors
        int colS = Math.max(y-1,0);
        int colF = Math.min(y+1,cells.length-1);

        for (int curRow = rowS; curRow<=rowF;curRow++) {
            for (int curCol = colS; curCol<=colF;curCol++) {
                if (cells[curRow][curCol]%10==9) {                  //if neighbour is a bomb
                    cells[x][y] +=1;                                //add 1 to the cell
                }
            }
        }
    }

    /**This method ensures the user does not click a bomb 
     * on the first click by converting
     * all neighbours to non-bombs */

    public void safetyNet(int x,int y) {                            //safety net method to stop first click from being a bomb
        int rowS = Math.max(x-1,0);
        int rowF = Math.min(x+1,cells.length-1);
        int colS = Math.max(y-1,0);
        int colF = Math.min(y+1,cells.length-1);

        for (int curRow = rowS; curRow<=rowF;curRow++) {
            for (int curCol = colS; curCol<=colF;curCol++) {
                cells[curRow][curCol] = 0;                      //make neighbours not bombs
            }
        }
    }

    public void floodFillUtil(int a, int b)                     //floodfill algorithm for opening no-bomb-neighbour cells
{
    // Base cases
    if (a < 0 || a >= grid || b < 1 || b >= grid || cells[a][b]>500 && cells[a][b] != 600 || cells[a][b]%10 == 9) {     //if out of range or revealed or a bomb, don't do anything
        return;
    }

    if (cells[a][b]%10 > 0) {                                   //if has a bomb-neighbour
        cells[a][b] += 500;                                     //mark as revealed
        update(a,b);
        return;
    }

    else {
        cells[a][b] += 500;
        update(a,b);
        floodFillUtil(a+1, b);                                  //check each neighbour to reveal
        floodFillUtil(a-1, b);
        floodFillUtil(a, b+1);
        floodFillUtil(a, b-1);
        floodFillUtil(a+1, b+1);
        floodFillUtil(a-1, b-1);
        floodFillUtil(a-1, b+1);
        floodFillUtil(a+1, b-1);
    }
}

public void floodFill() {
    floodFillUtil(x, y);
}

public void loseGame() throws InterruptedException {                //method if player loses game
    Graphics g = getGraphics();
    finish = true;                                                  //mark as finished
    for (int i=0;i<grid;i++) {
        for(int j=0;j<grid;j++) {
            if (cells[j][i] == 9) {                                 //reveal each bomb
                Thread.sleep(150);
                cells[j][i]+=500;
                update(j,i);
            }
            else if (cells[j][i] >100 && cells[j][i] <400) {       // if cell was marked wrongly
                Thread.sleep(150);
                flag(j,i);
            }
        }
    }

    for (int i=1;i<256;i=i*2) {
        Color end = new Color(0,0,0,i);
        g.setColor(end);
        g.fillRect(0,0,dimension*grid,dimension*grid);              //fade in screen
        Thread.sleep(200);
    }
    
    
    g.setColor(Color.WHITE);
    Font font = new Font("Arial Rounded MT Bold",Font.PLAIN,45+(grid*2));   
    g.setFont(font);
    g.drawString("You lose",400-(grid*15),(dimension*grid)/3);          
    g.drawString("Time:  " + (timer-1),400-(grid*15),(dimension*grid)/2);

    Thread.sleep(4000);                                             //wait 4 seconds
    System.exit(0);                                                 //exit program
}

public void winGame() throws InterruptedException{                  //method if player wins game
    Graphics g = getGraphics();
    finish = true;                                                  //mark as finished

    for (int i=1;i<256;i=i*2) {
        Color end = new Color(255,255,255,i);
        g.setColor(end);
        g.fillRect(0,0,dimension*grid,dimension*grid);              //fade in screen
        Thread.sleep(200);
    }
    
    g.setColor(Color.RED);
    Font font = new Font("Arial Rounded MT Bold",Font.PLAIN,45+(grid));   
    g.setFont(font);
    g.drawString("Congratulations",400-(grid*15),(dimension*grid)/3);
    g.drawString("Time:  " + (timer-1),400-(grid*15),(dimension*grid)/2);

    Thread.sleep(4000);                                             //wait 4 seconds
    System.exit(0);                                                 //exit program
}

    @Override
    public void mouseClicked(MouseEvent e) {}                      //need these so mouseListener works, don't need them
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
}



//found or unfound
//flagged or unflagged

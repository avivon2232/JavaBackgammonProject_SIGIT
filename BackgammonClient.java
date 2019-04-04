// Fig. 24.15: TicTacToeClient.java
// Client that let a user play backgammon with another across a network.
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import javax.swing.*;
import java.net.SocketAddress;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.lang.Math;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.Thread;

public class BackgammonClient extends JFrame implements Runnable, ActionListener {
    private JTextField idField; // textfield to display player's mark
    private JTextField eatenPiecesField; // textfield to display eaten pieces
    private JTextArea dicesAndEatenField; //textfield to display the dicesAndEatenField
    private JTextArea displayArea; // JTextArea to display output
    private JPanel textPanel; //text panel
    private JPanel boardHolderPanel;//panel that holds the whole board
    private JPanel firstBoardRowPanel; // panel for number of pieces in first board row
    private JPanel lastBoardRowPanel; // panel for number of pieces in last board row
    private JPanel boardPanel; // panel for backgammon board
    private JPanel movesButtonsPannel; //panel for the moves undo and reset
    private JButton undoMoveBtn;
    private JButton resetMovesBtn;
    private JButton finishTurnBtn;
    private JPanel panel2; // panel to hold board
    private JPanel boardAndEatenPanel; // panel to hold board
    private Square board[][]; // backgammon board
    private Square firstRowBoard[]; // backgammon board first row numbers
    private Square lastRowBoard[]; // backgammon board last row numbers
    private Square currentSquare; // current square
    private Socket connection; // connection to server
    private Scanner input; // input from server
    private Formatter output; // output to server
    private String ticTacToeHost; // host name for server
    private String myMark; // this client's mark
    private boolean myTurn; // determines which client's turn it is
    private final String Y_MARK = "Y"; // mark for first client
    private final String B_MARK = "B"; // mark for second client
    private Dice dices; //the dices
    private int[] moves; //the moves for this turn array
    private int[] gameCols = new int[26]; //all the game cols array
    private int diceCalcOne;
    private int diceCalcTwo;
    private int diceCalcThree;
    private int diceCalcFour;
    private int eatenPieces = 0;
    private int subEatenPieces = 0;
    private boolean checkDeadLock = false;
    private boolean checkStartToWin = false;
    private boolean checkOnePieceLeft = false;
    private boolean checkTwoPieceLeft = false;
    private boolean checkThreePieceLeft = false;
    // set up user-interface and board
    public BackgammonClient( String host, int port )
    {

        boardAndEatenPanel = new JPanel();
        boardAndEatenPanel.setLayout(new BorderLayout());
        //ticTacToeHost = host; // set name of server
        displayArea = new JTextArea( 4, 30 ); // set up JTextArea
        displayArea.setEditable( false );
        add( new JScrollPane( displayArea ), BorderLayout.SOUTH );

        eatenPiecesField = new JTextField(3); // set up textfield
        eatenPiecesField.setEditable( false );
        boardAndEatenPanel.add(eatenPiecesField, BorderLayout.CENTER);
        boardAndEatenPanel.add(new JScrollPane(displayArea), BorderLayout.SOUTH);

        add(boardAndEatenPanel, BorderLayout.SOUTH);
        boardPanel = new JPanel(); // set up panel for squares in board
        boardPanel.setLayout( new GridLayout( 12, 12, 0, 0 ) );


        firstBoardRowPanel = new JPanel();
        firstBoardRowPanel.setLayout( new GridLayout( 1, 12, 0, 0 ) );

        lastBoardRowPanel = new JPanel();
        lastBoardRowPanel.setLayout( new GridLayout( 1, 12, 0, 0 ) );

        board = new Square[ 12 ][ 13 ]; // create board
        firstRowBoard = new Square[12];
        lastRowBoard = new Square[12];

        // loop over the rows in the board
        for ( int row = 0; row < board.length; row++ )
        {
            // loop over the columns in the board
            for ( int column = 0; column < board[ row ].length - 1; column++ )
            {
                // create square & fill it
                if((column == 0 && (row !=5 && row != 6))  || (column == 4 && (row < 3 || row > 8)) || (column == 6 &&
                        (row !=5 && row != 6)) || (column == 11 && (row < 2 || row > 9)))
                {
                    //yellow fill
                    if((column == 0 && row < 5) || (column == 4 && row > 8) || (column == 6 && row > 6) ||
                            (column == 11 && row < 2))
                    {
                        board[ row ][ column ] = new Square( "Y", row * 12 + column );
                    }
                    else
                    {
                        board[ row ][ column ] = new Square( "B", row * 12 + column );
                    }
                }
                else {
                    board[row][column] = new Square(" ", row * 12 + column);
                    if(row == 5 || row == 6){
                        board[row][column].mark = (char)9617 + "" ;
                    }
                }
                boardPanel.add( board[ row ][ column ] ); // add square
            } // end inner for
        } // end outer for

        for(int i = 0; i < gameCols.length; i++)
        {
            if( i == 0 || i == 4 || i == 6 || i == 11 || i == 12 || i == 17 || i == 19 | i == 23)
            {
                if (i == 0 || i == 6 || i == 17 || i == 23)
                    gameCols[i] = 5;
                else if(i == 4 || i == 19)
                    gameCols[i] =3;
                else
                    gameCols[i] = 2;
            }
            else
            {
                gameCols[i] = 0;
            }
        }
        /*Fast Win Board
        for( int i = 0; i< 12; i++)
        {
            for( int j = 0; j < 12; j++)
            {
                board[i][j] = new Square(" ", i * 12 + j);
                if(i== 5 || i == 6){
                    board[i][j].mark =(char)9617 + "" ;
                }
                boardPanel.add( board[ i ][ j ] ); // add square
            }
        }
        board[ 0 ][ 6 ].mark = "B";
        board[ 1 ][ 6 ].mark = "B";
        board[ 2 ][ 6 ].mark = "B";
        board[ 3 ][ 6 ].mark = "B";
        board[ 4 ][ 6 ].mark = "B";

        board[ 11 ][ 6 ].mark = "Y";
        board[ 10 ][ 6 ].mark = "Y";
        board[ 9 ][ 6 ].mark = "Y";
        board[ 8 ][ 6 ].mark = "Y";
        board[ 7 ][ 6 ].mark = "Y";

        gameCols[6] = 15;
        gameCols[23 - 6] = 15;

        */

        gameCols[24] = 100;
        gameCols[25] = 100;

        for(int i= 0; i<12;i++){
            firstRowBoard[i] = new Square(String.valueOf(gameCols[i]), 65);
            firstBoardRowPanel.add(firstRowBoard[i]);
            lastRowBoard[i] = new Square(String.valueOf(gameCols[23-i]), 65);
            lastBoardRowPanel.add(lastRowBoard[i]);
        }
        idField = new JTextField(4); // set up textfield
        idField.setEditable( false );

        dicesAndEatenField = new JTextArea();
        dices = new Dice(); //dices
        dicesAndEatenField.setEditable( false );

        moves = new int[9]; //moves array

        textPanel = new JPanel();
        textPanel.setLayout(new BorderLayout());
        movesButtonsPannel = new JPanel();

        //buttons
        finishTurnBtn = new JButton("submit turn");
        finishTurnBtn.addActionListener(this);

        undoMoveBtn = new JButton("UndoMove");
        undoMoveBtn.addActionListener(this);

        resetMovesBtn = new JButton("ResetMoves");
        resetMovesBtn.addActionListener(this);

        movesButtonsPannel.add(undoMoveBtn);
        movesButtonsPannel.add(resetMovesBtn);
        movesButtonsPannel.add(finishTurnBtn);

        textPanel.setLayout(new BorderLayout());
        textPanel.add(idField, BorderLayout.NORTH);
        textPanel.add(dicesAndEatenField, BorderLayout.CENTER);
        textPanel.add(movesButtonsPannel, BorderLayout.SOUTH);
        add( textPanel, BorderLayout.NORTH );

        panel2 = new JPanel(); // set up panel to contain boardPanel
        panel2.setLayout(new BorderLayout());

        panel2.add(firstBoardRowPanel, BorderLayout.NORTH);
        panel2.add( boardPanel, BorderLayout.CENTER ); // add board panel
        panel2.add(lastBoardRowPanel, BorderLayout.SOUTH);

        boardHolderPanel = new JPanel();

        boardHolderPanel.add(panel2, BorderLayout.CENTER);


        add( boardHolderPanel, BorderLayout.CENTER ); // add container panel

        setSize( 500, 800 ); // set size of window
        setVisible( true ); // show window

        startClient(host, port);
    } // end TicTacToeClient constructor
    // start the client thread
    public void startClient(String host, int port)
    {
        try // connect to server, get streams and start outputThread
        {
            // make connection to server
            connection = new Socket();
            try {
                connection.connect(new InetSocketAddress(host, port));
            }
            catch (IOException connection){
                System.exit(1);
            }

            // get streams for input and output
            //dices.throwTwoDices();
            dices.setDices(6,6);
            //dices.setDices(6, 6);
            dicesAndEatenField.setText("Dices: " + dices.getDices()[0] + ", " + dices.getDices()[1] +
                    "\nMove No." + (1+moves[0]/2)+
                    "\nMove 1:" +
                    "\nMove 2:" +
                    "\nMove 3:" +
                    "\nMove 4:");
            eatenPiecesField.setText("Eaten Pieces: " + eatenPieces );
            input = new Scanner( connection.getInputStream() );
            output = new Formatter( connection.getOutputStream() );
        } // end try
        catch ( IOException ioException )
        {
            ioException.printStackTrace();
        } // end catch

        // create and start worker thread for this client
        ExecutorService worker = Executors.newFixedThreadPool( 1 );
        worker.execute( this ); // execute client
    } // end method startClient

    // control thread that allows continuous update of displayArea
    public void run()
    {
        myMark = input.nextLine(); // get player's mark (Y or B)

        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        // display player's mark
                        idField.setText( "You are player \"" + myMark + "\"" );
                    } // end method run
                } // end anonymous inner class
        ); // end call to SwingUtilities.invokeLater

        myTurn = ( myMark.equals(Y_MARK) ); // determine if client's turn

        Thread input_thread = new Thread(){
            public void run(){
                while ( true ) {
                    //check if there's a win
                    if(isGameOver() == 0) {
                        if (!(eatenPieces - subEatenPieces > 0) && checkStartToWin == false) {
                            if (isItTheStartOfAW()) {
                                checkStartToWin = true;
                            }
                        }
                        else if(eatenPieces - subEatenPieces > 0)
                            checkStartToWin = false;
                    }
                    else{
                        if(eatenPieces==0) {
                            if (isGameOver() == 1) {

                                displayMessage("B won!\nExiting in 3 seconds.");
                            } else {
                                displayMessage("Y won!\nExiting in 3 seconds.");
                            }
                            try//to prevent a loss of piece placement
                            {
                                Thread.sleep(3000);
                            }
                            catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt(); // restore interrupted status
                            }
                            System.exit(0);
                        }
                    }

                    //check if there are less than 4 pieces remaining
                    if(numberOfPieces(myMark)==1){
                        checkOnePieceLeft = true;
                        displayMessage("One Piece Left!");
                    }
                    else{
                        if(numberOfPieces(myMark) == 2){
                            checkTwoPieceLeft = true;
                            displayMessage("Two Pieces Left!");
                        }
                        else{
                            if(numberOfPieces(myMark) == 3){
                                checkThreePieceLeft = true;
                                displayMessage("Three Pieces Left!");
                            }
                        }
                    }
                    //checks if there's any received data
                    if (input.hasNextLine())
                        processMessage(input.nextLine());
                }
            }
        };
        input_thread.start();
    } // end method run

    public int numberOfPieces(String mark){
        int count = 0;
        for(int i = 0; i < 24; i++){
            if(gameCols[i] > 0){
                if(i < 12){
                    if(board[0][i].mark.equals(mark))
                        count += gameCols[i];
                }
                else{
                    if(board[11][23-i].mark.equals(mark))
                        count+=gameCols[i];
                }
            }
        }
        return count;
    }

    public int isGameOver(){
        // return -1 for Y win, 0 if game isn't over and 1 if B won
        int BCheck = 0, YCheck = 0;
        for(int i = 0; i < 12; i++){
            if(gameCols[i] > 0){
                if(board[0][i].mark.equals("Y")){
                    YCheck = 1;
                }
                else{
                    BCheck = 1;
                }
            }
            if(gameCols[23 - i] > 0){
                if(board[11][i].mark.equals("Y")){
                    YCheck = 1;
                }
                else{
                    BCheck = 1;
                }
            }
            if(BCheck == 1 && YCheck == 1){
                return 0;
            }
        }
        if(BCheck == 1 && YCheck == 0)
        {
            return -1;//Y won
        }
        else if(YCheck == 1 && BCheck == 0) {
            return 1;//B won
        }
        return 0;
    }
    public boolean isItTheStartOfAW(){
        //check if one side is gonna win
        int sum = 0;
        if(myMark.equals("Y")){
            for(int col = 0 ; col < 12; col++){
                if(col < 6){
                    if(board[11][col]!= null)
                        if(board[11][col].mark.equals(myMark))
                            return false;
                }
                if(col > 5){
                    if(board[11][col]!= null)
                        if(board[11][col].mark.equals(myMark))
                            sum += gameCols[23 - col];
                }
                if(board[0][col]!= null)
                    if(board[0][col].mark.equals(myMark))
                        return false;
            }
            if(sum==15)
                return true;
        }
        else{
            for(int col = 0 ; col < 12; col++){
                if(col < 6){
                    if(board[0][col]!= null)
                        if(board[0][col].mark.equals(myMark))
                            return false;
                }
                if(col > 5){
                    if(board[0][col]!= null)
                        if(board[0][col].mark.equals(myMark))
                            sum += gameCols[col];
                }
                if(board[11][col]!= null)
                    if(board[11][col].mark.equals(myMark))
                        return false;
            }
            if(sum==15)
                return true;
        }
        return false;
    }
    public boolean isItGonnaGetEaten(int destinationLocation)
    {//to check if the piece is hella hungry
        int row = destinationLocation / 12;
        int col = destinationLocation % 12;
        if(row < 5){
            if(gameCols[ col ] == 1)
            {
                if(!board[0][col].mark.equals(myMark)&&!board[0][col].mark.equals(" "))
                    return true;
            }
        }
        else{
            if(gameCols[ 23-col ] == 1)
            {
                if(!board[ 11 ][ col ].mark.equals(myMark)&&!board[11][col].mark.equals(" "))
                    return true;
            }
        }
        return false;
    }
    public boolean opponnentIsItGonnaGetEaten(int destinationLocation)
    {//to check if the piece is hella hungry
        int row = destinationLocation / 12;
        int col = destinationLocation % 12;
        if(row < 5){
            if(gameCols[ col ] == 1)
            {
                if((board[0][col].mark.equals(myMark)))
                    return true;
            }
        }
        else{
            if(gameCols[ 23-col ] == 1)
            {
                if((board[ 11 ][ col ].mark.equals(myMark)))
                    return true;
            }
        }
        return false;
    }


    // process messages received by client
    private void processMessage( String message )
    {
        // valid move occurred
        if( message.equals("DeadLock!") ){
            displayMessage("DeadLock!");
            myTurn = true;
        }
        if ( message.equals( "Valid move." ) )
        {
            boolean checkDeadlock = false;//nowhere to move
            displayMessage( "Valid move, please wait.");
            if(moves[1] == 170 && moves[2] == 170 && moves[3] == 170 && moves[4] == 170) {
                displayMessage("DeadLock!, please wait.");
                checkDeadlock = true;
            }

            if(!checkDeadlock) {
                if(moves[1]==moves[2]){
                    if(myMark.equals("Y")){
                        if(moves[1] / 12 > 6){
                            if(gameCols[23 - moves[1]%12] > 0){
                                if(gameCols[23 - moves[1]%12] < 6){
                                    setMark(board[12 - gameCols[23 - moves[1]%12]][moves[1]% 12], " ");
                                }
                                gameCols[23 - moves[1]%12]--;
                            }
                        }
                    }
                    else{
                        if(moves[1] / 12 < 5){
                            if(gameCols[moves[1]%12] > 0){
                                if(gameCols[moves[1]%12] < 6){
                                    setMark(board[gameCols[moves[1]%12] - 1][moves[1]% 12], " ");
                                }
                                gameCols[moves[1]%12]--;
                            }
                        }
                    }
                }
                else if (moves[2] != 144 && moves[2] != 145 && isItGonnaGetEaten(moves[2])) {
                    if (moves[2] / 12 < 5)
                        setMark(board[0][moves[2] % 12], myMark);
                    else if (moves[2] / 12 > 6)
                        setMark(board[11][moves[2] % 12], myMark);
                    displayMessage(eatenPieces + "");
                } else {
                    if (moves[2] == 144 || moves[2] == 145) {
                        if(isItGonnaGetEaten(moves[1])){
                            if(moves[1]/12 < 5){
                                setMark(board[0][moves[1]% 12], myMark);
                            }
                            else{
                                setMark(board[11][moves[1] % 12], myMark);
                            }
                        }
                        else {
                            if (moves[1] / 12 < 5) {
                                if (gameCols[moves[1] % 12]++ < 5)
                                    setMark(board[gameCols[moves[1] % 12] - 1][moves[1] % 12],myMark);
                            } else {
                                if (gameCols[23 - moves[1] % 12]++ < 5)
                                    setMark(board[11 - gameCols[23 - moves[1] % 12] + 1][moves[1] % 12],myMark);
                            }
                        }
                    } else {
                        if (moves[2] / 12 < 5) {
                            if (gameCols[moves[2]  % 12]++ < 5)
                                setMark(board[gameCols[moves[2]  % 12] - 1][moves[2]  % 12],myMark);
                        } else {
                            if (gameCols[23 - moves[2]  % 12]++ < 5)
                                setMark(board[11 - gameCols[23 - moves[2]  % 12] + 1][moves[2]  % 12],myMark);
                        }
                    }
                }
            }

            try//to prevent a loss of piece placement
            {
                Thread.sleep(5);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt(); // restore interrupted status
            }
            if(!checkDeadlock && moves[3]!=200) {
                if(moves[3]==moves[4] && moves[3]!=200){
                    if(myMark.equals("Y")){
                        if(moves[3] / 12 > 6){
                            if(gameCols[23 - moves[3]%12] > 0){
                                if(gameCols[23 - moves[3]%12] < 6){
                                    setMark(board[12 - gameCols[23 - moves[3]%12]][moves[3]% 12], " ");
                                }
                                gameCols[23 - moves[3]%12]--;
                            }
                        }
                    }
                    else{
                        if(moves[3] / 12 < 5){
                            if(gameCols[moves[3]%12] > 0){
                                if(gameCols[moves[3]%12] < 6){
                                    setMark(board[gameCols[moves[3]%12] - 1][moves[3]% 12], " ");
                                }
                                gameCols[moves[3]%12]--;
                            }
                        }
                    }
                }
                else if (moves[4] != 144 && moves[4] != 145 && isItGonnaGetEaten(moves[4])) {
                    if (moves[4] / 12 < 5)
                        setMark(board[0][moves[4] % 12],myMark);
                    else if (moves[4] / 12 > 6)
                        setMark(board[11][moves[4] % 12],myMark);
                    displayMessage(eatenPieces + "");
                } else {
                    if(moves[3]==moves[4] && moves[3] == 200){

                    }
                    else if (moves[4] == 144 || moves[4] == 145) {
                        if(isItGonnaGetEaten(moves[3])){
                            if(moves[3]/12 < 5){
                                setMark(board[0][moves[3]% 12],myMark);
                            }
                            else{
                                setMark(board[11][moves[3] % 12],myMark);
                            }
                        }
                        else {
                            if (moves[3] / 12 < 5) {
                                if (gameCols[moves[3] % 12]++ < 5)
                                    setMark(board[gameCols[moves[3] % 12] - 1][moves[3] % 12],myMark);
                            } else {
                                if (gameCols[23 - moves[3] % 12]++ < 5)
                                    setMark(board[11 - gameCols[23 - moves[3] % 12] + 1][moves[3] % 12],myMark);
                            }
                        }
                    } else {
                        if (moves[4] / 12 < 5) {
                            if (gameCols[moves[4]  % 12]++ < 5)
                                setMark(board[gameCols[moves[4]  % 12] - 1][moves[4]  % 12],myMark);
                        } else {
                            if (gameCols[23 - moves[4]  % 12]++ < 5)
                                setMark(board[11 - gameCols[23 - moves[4]  % 12] + 1][moves[4]  % 12],myMark);
                        }
                    }
                }
            }
            try//to prevent a loss of piece placement
            {
                Thread.sleep(5);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt(); // restore interrupted status
            }
            if(!checkDeadlock&&moves[5] !=0 && moves[6] != 0) {
                if (moves[5] != 200) {
                    if (moves[5] == moves[6] && moves[5] != 200) {
                        if (myMark.equals("Y")) {
                            if (moves[5] / 12 > 6) {
                                if (gameCols[23 - moves[5] % 12] > 0) {
                                    if (gameCols[23 - moves[5] % 12] < 6) {
                                        setMark(board[12 - gameCols[23 - moves[5] % 12]][moves[5] % 12], " ");
                                    }
                                    gameCols[23 - moves[5] % 12]--;
                                }
                            }
                        } else {
                            if (moves[5] / 12 < 5) {
                                if (gameCols[moves[5] % 12] > 0) {
                                    if (gameCols[moves[5] % 12] < 6) {
                                        setMark(board[gameCols[moves[5] % 12] - 1][moves[5] % 12], " ");
                                    }
                                    gameCols[moves[5] % 12]--;
                                }
                            }
                        }
                    } else if (moves[6] != 144 && moves[6] != 145 && isItGonnaGetEaten(moves[6])) {
                        if (moves[6] / 12 < 5)
                            setMark(board[0][moves[6] % 12], myMark);
                        else if (moves[6] / 12 > 6)
                            setMark(board[11][moves[6] % 12], myMark);
                        displayMessage(eatenPieces + "");
                    } else {
                        if (moves[5] == moves[6] && moves[5] == 200) {

                        } else if (moves[6] == 144 || moves[6] == 145) {
                            if (isItGonnaGetEaten(moves[5])) {
                                if (moves[5] / 12 < 5) {
                                    setMark(board[0][moves[5] % 12], myMark);
                                } else {
                                    setMark(board[11][moves[5] % 12], myMark);
                                }
                            } else {
                                if (moves[5] / 12 < 5) {
                                    if (gameCols[moves[5] % 12]++ < 5)
                                        setMark(board[gameCols[moves[5] % 12] - 1][moves[5] % 12], myMark);
                                } else {
                                    if (gameCols[23 - moves[5] % 12]++ < 5)
                                        setMark(board[11 - gameCols[23 - moves[5] % 12] + 1][moves[5] % 12], myMark);
                                }
                            }
                        } else {
                            if (moves[6] / 12 < 5) {
                                if (gameCols[moves[6] % 12]++ < 5)
                                    setMark(board[gameCols[moves[6] % 12] - 1][moves[6] % 12], myMark);
                            } else {
                                if (gameCols[23 - moves[6] % 12]++ < 5)
                                    setMark(board[11 - gameCols[23 - moves[6] % 12] + 1][moves[6] % 12], myMark);
                            }
                        }
                    }
                }
                try//to prevent a loss of piece placement
                {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // restore interrupted status
                }
                if (moves[7] != 200) {
                    if (moves[7] == moves[8] && moves[7] != 200) {
                        if (myMark.equals("Y")) {
                            if (moves[7] / 12 > 6) {
                                if (gameCols[23 - moves[7] % 12] > 0) {
                                    if (gameCols[23 - moves[7] % 12] < 6) {
                                        setMark(board[12 - gameCols[23 - moves[7] % 12]][moves[7] % 12], " ");
                                    }
                                    gameCols[23 - moves[7] % 12]--;
                                }
                            }
                        } else {
                            if (moves[7] / 12 < 5) {
                                if (gameCols[moves[7] % 12] > 0) {
                                    if (gameCols[moves[7] % 12] < 6) {
                                        setMark(board[gameCols[moves[5] % 12] - 1][moves[7] % 12], " ");
                                    }
                                    gameCols[moves[7] % 12]--;
                                }
                            }
                        }
                    } else if (moves[8] != 144 && moves[8] != 145 && isItGonnaGetEaten(moves[8])) {
                        if (moves[8] / 12 < 5)
                            setMark(board[0][moves[8] % 12], myMark);
                        else if (moves[8] / 12 > 6)
                            setMark(board[11][moves[8] % 12], myMark);
                        displayMessage(eatenPieces + "");
                    } else {
                        if (moves[7] == moves[8] && moves[7] == 200) {

                        } else if (moves[8] == 144 || moves[8] == 145) {
                            if (isItGonnaGetEaten(moves[7])) {
                                if (moves[7] / 12 < 5) {
                                    setMark(board[0][moves[7] % 12], myMark);
                                } else {
                                    setMark(board[11][moves[7] % 12], myMark);
                                }
                            } else {
                                if (moves[7] / 12 < 5) {
                                    if (gameCols[moves[7] % 12]++ < 5)
                                        setMark(board[gameCols[moves[7] % 12] - 1][moves[7] % 12], myMark);
                                } else {
                                    if (gameCols[23 - moves[7] % 12]++ < 5)
                                        setMark(board[11 - gameCols[23 - moves[7] % 12] + 1][moves[7] % 12], myMark);
                                }
                            }
                        } else {
                            if (moves[8] / 12 < 5) {
                                if (gameCols[moves[8] % 12]++ < 5)
                                    setMark(board[gameCols[moves[8] % 12] - 1][moves[8] % 12], myMark);
                            } else {
                                if (gameCols[23 - moves[8] % 12]++ < 5)
                                    setMark(board[11 - gameCols[23 - moves[8] % 12] + 1][moves[8] % 12], myMark);
                            }
                        }
                    }
                }
            }
            try//to prevent a loss of piece placement
            {
                Thread.sleep(5);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt(); // restore interrupted status
            }
            if(!checkDeadlock) {
                if (moves[2]!=moves[1] && (moves[2] != 144 && moves[2] != 145)) {
                    if (moves[1] / 12 < 5) {
                        if(gameCols[moves[1] % 12] > 0 && gameCols[moves[1] % 12] < 6)
                            setMark(board[--gameCols[moves[1] % 12]][moves[1] % 12], " ");
                        else{
                            --gameCols[moves[1] % 12];
                        }
                    } else {
                        if(gameCols[23 - moves[1] % 12] > 0 && gameCols[23 - moves[1] % 12] < 6)
                            setMark(board[11 - (--gameCols[23 - moves[1] % 12])][moves[1] % 12], " ");
                        else{
                            --gameCols[23 - moves[1] % 12];
                        }
                    }
                }
                try//to prevent a loss of piece placement
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                }
                if (moves[3]!=moves[4] && (moves[4] != 144 && moves[4] != 145)) {
                    if (moves[3] / 12 < 5) {
                        if(gameCols[moves[3] % 12] > 0 && gameCols[moves[3] % 12] < 6)
                            setMark(board[--gameCols[moves[3] % 12]][moves[3] % 12], " ");
                        else{
                            --gameCols[moves[3] % 12];
                        }
                    } else {
                        if(gameCols[23 - moves[3] % 12] > 0 && gameCols[23 - moves[3] % 12] < 6)
                            setMark(board[11 - (--gameCols[23 - moves[3] % 12])][moves[3] % 12], " ");
                        else{
                            --gameCols[23 - moves[3] % 12];
                        }
                    }
                }
            }
            try//to prevent a loss of piece placement
            {
                Thread.sleep(5);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt(); // restore interrupted status
            }
            if(moves[5] !=0 && moves[6] != 0 && moves[7] != 0 && moves[8] != 0) {
                if (moves[5]!=moves[6] && (moves[6] != 144 && moves[6] != 145)) {
                    if (moves[5] / 12 < 5) {
                        if(gameCols[moves[5] % 12] > 0 && gameCols[moves[5] % 12] < 6)
                            setMark(board[--gameCols[moves[5] % 12]][moves[5] % 12], " ");
                        else{
                            --gameCols[moves[5] % 12];
                        }
                    } else {
                        if(gameCols[23 - moves[5] % 12] > 0 && gameCols[23 - moves[5] % 12] < 6)
                            setMark(board[11 - (--gameCols[23 - moves[5] % 12])][moves[5] % 12], " ");
                        else{
                            --gameCols[23 - moves[5] % 12];
                        }
                    }
                }
                try//to prevent a loss of piece placement
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                }
                if (moves[7]!=moves[8] && (moves[8] != 144 && moves[8] != 145)) {
                    if (moves[7] / 12 < 5) {
                        if(gameCols[moves[7] % 12] > 0 && gameCols[moves[7] % 12] < 6)
                            setMark(board[--gameCols[moves[7] % 12]][moves[7] % 12], " ");
                        else{
                            --gameCols[moves[7] % 12];
                        }
                    } else {
                        if(gameCols[23 - moves[7] % 12] > 0 && gameCols[23 - moves[7] % 12] < 6)
                            setMark(board[11 - (--gameCols[23 - moves[7] % 12])][moves[7] % 12], " ");
                        else{
                            --gameCols[23 - moves[7] % 12];
                        }
                    }
                }
            }
            eatenPieces-=subEatenPieces;
            subEatenPieces = 0;

            resetFunc();
            dicesAndEatenField.setText("Dices: " + dices.getDices()[0] + ", " + dices.getDices()[1] +
                    "\nMove No." + (1+moves[0]/2)+
                    "\nMove 1:" +
                    "\nMove 2:" +
                    "\nMove 3:" +
                    "\nMove 4:");
            eatenPiecesField.setText("Eaten Pieces: " + eatenPieces );

        } // end if
        else if ( message.equals( "Invalid move, try again" ) )
        {
            displayMessage( message ); // display invalid move
            resetFunc();
            dicesAndEatenField.setText("Dices: " + dices.getDices()[0] + ", " + dices.getDices()[1] +
                    "\nMove No." + (1+moves[0]/2)+
                    "\nMove 1:" +
                    "\nMove 2:" +
                    "\nMove 3:" +
                    "\nMove 4:");
            eatenPiecesField.setText("Eaten Pieces: " + eatenPieces );
            subEatenPieces = 0;
            myTurn = true; // still this client's turn
        } // end else if
        else if ( message.equals( "Opponent moved" ) )
        {
            String[] loc;
            String hisMark = myMark.equals(Y_MARK) ? B_MARK : Y_MARK;
            boolean checkDeadlock = false;
            loc = input.next().split(",", -1);
            displayMessage( "Valid move, please wait.");
            if(loc.length == 4|| loc.length == 8)
            {
                if(Integer.parseInt(loc[0])==170 && Integer.parseInt(loc[1]) == 170 && Integer.parseInt(loc[2]) == 170 && Integer.parseInt(loc[3]) == 170)
                    checkDeadlock = true;
            }

            if(!checkDeadlock) {
                if(Integer.parseInt(loc[0])==Integer.parseInt(loc[1])){
                    if(myMark.equals("B")){
                        if(Integer.parseInt(loc[0]) / 12 > 6){
                            if(gameCols[23 - Integer.parseInt(loc[0])%12] > 0){
                                if(gameCols[23 - Integer.parseInt(loc[0])%12] < 6){
                                    setMark(board[12 - gameCols[23 - Integer.parseInt(loc[0])%12]][Integer.parseInt(loc[0])% 12], " ");
                                }
                                gameCols[23 - Integer.parseInt(loc[0])%12]--;
                            }
                        }
                    }
                    else{
                        if(Integer.parseInt(loc[0]) / 12 < 5){
                            if(gameCols[Integer.parseInt(loc[0])%12] > 0){
                                if(gameCols[Integer.parseInt(loc[0])%12] < 6){
                                    setMark(board[gameCols[Integer.parseInt(loc[0])%12] - 1][Integer.parseInt(loc[0])% 12], " ");
                                }
                                gameCols[Integer.parseInt(loc[0])%12]--;
                            }
                        }
                    }
                }
                else if (Integer.parseInt(loc[1]) != 144 && Integer.parseInt(loc[1]) != 145 && opponnentIsItGonnaGetEaten(Integer.parseInt(loc[1]))) {
                    if (Integer.parseInt(loc[1]) / 12 < 5)
                        setMark(board[0][Integer.parseInt(loc[1]) % 12], hisMark);
                    else if (Integer.parseInt(loc[1]) / 12 > 6)
                        setMark(board[11][Integer.parseInt(loc[1]) % 12], hisMark);
                    eatenPieces++;
                    displayMessage(eatenPieces + "");
                } else {
                    if (Integer.parseInt(loc[1]) == 144 || Integer.parseInt(loc[1]) == 145) {
                        if(opponnentIsItGonnaGetEaten(Integer.parseInt(loc[0]))){
                            if(Integer.parseInt(loc[0])/12 < 5){
                                setMark(board[0][Integer.parseInt(loc[0])% 12], hisMark);
                            }
                            else{
                                setMark(board[11][Integer.parseInt(loc[0]) % 12], hisMark);
                            }
                        }
                        else {
                            if (Integer.parseInt(loc[0])/ 12 < 5) {
                                if (gameCols[Integer.parseInt(loc[0]) % 12]++ < 5)
                                    setMark(board[gameCols[Integer.parseInt(loc[0])% 12] - 1][Integer.parseInt(loc[0]) % 12],hisMark);
                            } else {
                                if (gameCols[23 - Integer.parseInt(loc[0]) % 12]++ < 5)
                                    setMark(board[11 - gameCols[23 - Integer.parseInt(loc[0]) % 12] + 1][Integer.parseInt(loc[0]) % 12],hisMark);
                            }
                        }
                    } else {
                        if (Integer.parseInt(loc[1]) / 12 < 5) {
                            if (gameCols[Integer.parseInt(loc[1]) % 12]++ < 5)
                                setMark(board[gameCols[Integer.parseInt(loc[1])  % 12] - 1][Integer.parseInt(loc[1]) % 12],hisMark);
                        } else {
                            if (gameCols[23 - Integer.parseInt(loc[1])  % 12]++ < 5)
                                setMark(board[11 - gameCols[23 - Integer.parseInt(loc[1])  % 12] + 1][Integer.parseInt(loc[1])  % 12],hisMark);
                        }
                    }
                }
            }

            try//to prevent a loss of piece placement
            {
                Thread.sleep(5);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt(); // restore interrupted status
            }
            if(!checkDeadlock && Integer.parseInt(loc[2]) != 200) {
                if(Integer.parseInt(loc[2])==Integer.parseInt(loc[3]) && Integer.parseInt(loc[2]) != 200){
                    if(myMark.equals("B")){
                        if(Integer.parseInt(loc[2])/ 12 > 6){
                            if(gameCols[23 - Integer.parseInt(loc[2])%12] > 0){
                                if(gameCols[23 - Integer.parseInt(loc[2])%12] < 6){
                                    setMark(board[12 - gameCols[23 - Integer.parseInt(loc[2])%12]][Integer.parseInt(loc[2])% 12], " ");
                                }
                                gameCols[23 - Integer.parseInt(loc[2])%12]--;
                            }
                        }
                    }
                    else{
                        if(Integer.parseInt(loc[2]) / 12 < 5){
                            if(gameCols[Integer.parseInt(loc[2])%12] > 0){
                                if(gameCols[Integer.parseInt(loc[2])%12] < 6){
                                    setMark(board[gameCols[Integer.parseInt(loc[2])%12] - 1][Integer.parseInt(loc[2])% 12], " ");
                                }
                                gameCols[Integer.parseInt(loc[2])%12]--;
                            }
                        }
                    }
                }
                else if (Integer.parseInt(loc[3]) != 144 && Integer.parseInt(loc[3]) != 145 && opponnentIsItGonnaGetEaten(Integer.parseInt(loc[3]))) {
                    if (Integer.parseInt(loc[3]) / 12 < 5)
                        setMark(board[0][Integer.parseInt(loc[3]) % 12], hisMark);
                    else if (Integer.parseInt(loc[3]) / 12 > 6)
                        setMark(board[11][Integer.parseInt(loc[3]) % 12], hisMark);
                    eatenPieces++;
                    displayMessage(eatenPieces + "");
                } else {
                    if (Integer.parseInt(loc[3]) == 144 || Integer.parseInt(loc[3]) == 145) {
                        if(opponnentIsItGonnaGetEaten(Integer.parseInt(loc[2]))){
                            if(Integer.parseInt(loc[2])/12 < 5){
                                setMark(board[0][Integer.parseInt(loc[2])% 12], hisMark);
                            }
                            else{
                                setMark(board[11][Integer.parseInt(loc[2]) % 12], hisMark);
                            }
                        }
                        else {
                            if (Integer.parseInt(loc[2]) / 12 < 5) {
                                if (gameCols[Integer.parseInt(loc[2]) % 12]++ < 5)
                                    setMark(board[gameCols[Integer.parseInt(loc[2]) % 12] - 1][Integer.parseInt(loc[2]) % 12],hisMark);
                            } else {
                                if (gameCols[23 - Integer.parseInt(loc[2]) % 12]++ < 5)
                                    setMark(board[11 - gameCols[23 - Integer.parseInt(loc[2]) % 12] + 1][Integer.parseInt(loc[2]) % 12],hisMark);
                            }
                        }
                    } else {
                        if (Integer.parseInt(loc[3]) / 12 < 5) {
                            if (gameCols[Integer.parseInt(loc[3])  % 12]++ < 5)
                                setMark(board[gameCols[Integer.parseInt(loc[3])  % 12] - 1][Integer.parseInt(loc[3])  % 12],hisMark);
                        } else {
                            if (gameCols[23 - Integer.parseInt(loc[3])  % 12]++ < 5)
                                setMark(board[11 - gameCols[23 - Integer.parseInt(loc[3])  % 12] + 1][Integer.parseInt(loc[3])  % 12],hisMark);
                        }
                    }
                }
            }
            try//to prevent a loss of piece placement
            {
                Thread.sleep(5);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt(); // restore interrupted status
            }
            if(loc.length == 8){
                if(!checkDeadlock && Integer.parseInt(loc[4]) != 200) {
                    if(Integer.parseInt(loc[4])==Integer.parseInt(loc[5]) && Integer.parseInt(loc[4]) != 200){
                        if(myMark.equals("B")){
                            if(Integer.parseInt(loc[4])/ 12 > 6){
                                if(gameCols[23 - Integer.parseInt(loc[4])%12] > 0){
                                    if(gameCols[23 - Integer.parseInt(loc[4])%12] < 6){
                                        setMark(board[12 - gameCols[23 - Integer.parseInt(loc[4])%12]][Integer.parseInt(loc[4])% 12], " ");
                                    }
                                    gameCols[23 - Integer.parseInt(loc[4])%12]--;
                                }
                            }
                        }
                        else{
                            if(Integer.parseInt(loc[4]) / 12 < 5){
                                if(gameCols[Integer.parseInt(loc[4])%12] > 0){
                                    if(gameCols[Integer.parseInt(loc[4])%12] < 6){
                                        setMark(board[gameCols[Integer.parseInt(loc[4])%12] - 1][Integer.parseInt(loc[4])% 12], " ");
                                    }
                                    gameCols[Integer.parseInt(loc[4])%12]--;
                                }
                            }
                        }
                    }
                    else if (Integer.parseInt(loc[5]) != 144 && Integer.parseInt(loc[5]) != 145 && opponnentIsItGonnaGetEaten(Integer.parseInt(loc[5]))) {
                        if (Integer.parseInt(loc[5]) / 12 < 5)
                            setMark(board[0][Integer.parseInt(loc[5]) % 12], hisMark);
                        else if (Integer.parseInt(loc[5]) / 12 > 6)
                            setMark(board[11][Integer.parseInt(loc[5]) % 12], hisMark);
                        eatenPieces++;
                        displayMessage(eatenPieces + "");
                    } else {
                        if (Integer.parseInt(loc[5]) == 144 || Integer.parseInt(loc[5]) == 145) {
                            if(opponnentIsItGonnaGetEaten(Integer.parseInt(loc[4]))){
                                if(Integer.parseInt(loc[4])/12 < 5){
                                    setMark(board[0][Integer.parseInt(loc[4])% 12], hisMark);
                                }
                                else{
                                    setMark(board[11][Integer.parseInt(loc[4]) % 12], hisMark);
                                }
                            }
                            else {
                                if (Integer.parseInt(loc[4]) / 12 < 5) {
                                    if (gameCols[Integer.parseInt(loc[4]) % 12]++ < 5)
                                        setMark(board[gameCols[Integer.parseInt(loc[4]) % 12] - 1][Integer.parseInt(loc[4]) % 12],hisMark);
                                } else {
                                    if (gameCols[23 - Integer.parseInt(loc[4]) % 12]++ < 5)
                                        setMark(board[11 - gameCols[23 - Integer.parseInt(loc[4]) % 12] + 1][Integer.parseInt(loc[4]) % 12],hisMark);
                                }
                            }
                        } else {
                            if (Integer.parseInt(loc[5]) / 12 < 5) {
                                if (gameCols[Integer.parseInt(loc[5])  % 12]++ < 5)
                                    setMark(board[gameCols[Integer.parseInt(loc[5])  % 12] - 1][Integer.parseInt(loc[5])  % 12],hisMark);
                            } else {
                                if (gameCols[23 - Integer.parseInt(loc[5])  % 12]++ < 5)
                                    setMark(board[11 - gameCols[23 - Integer.parseInt(loc[5])  % 12] + 1][Integer.parseInt(loc[5])  % 12],hisMark);
                            }
                        }
                    }
                }
                try//to prevent a loss of piece placement
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                }
                if(!checkDeadlock && Integer.parseInt(loc[6]) != 200) {
                    if(Integer.parseInt(loc[6])==Integer.parseInt(loc[7])){
                        if(myMark.equals("B")){
                            if(Integer.parseInt(loc[6])/ 12 > 6){
                                if(gameCols[23 - Integer.parseInt(loc[6])%12] > 0){
                                    if(gameCols[23 - Integer.parseInt(loc[6])%12] < 6){
                                        setMark(board[12 - gameCols[23 - Integer.parseInt(loc[6])%12]][Integer.parseInt(loc[6])% 12], " ");
                                    }
                                    gameCols[23 - Integer.parseInt(loc[6])%12]--;
                                }
                            }
                        }
                        else{
                            if(Integer.parseInt(loc[6]) / 12 < 5){
                                if(gameCols[Integer.parseInt(loc[6])%12] > 0){
                                    if(gameCols[Integer.parseInt(loc[6])%12] < 6){
                                        setMark(board[gameCols[Integer.parseInt(loc[6])%12] - 1][Integer.parseInt(loc[6])% 12], " ");
                                    }
                                    gameCols[Integer.parseInt(loc[6])%12]--;
                                }
                            }
                        }
                    }
                    else if (Integer.parseInt(loc[7]) != 144 && Integer.parseInt(loc[7]) != 145 && opponnentIsItGonnaGetEaten(Integer.parseInt(loc[7]))) {
                        if (Integer.parseInt(loc[7]) / 12 < 5)
                            setMark(board[0][Integer.parseInt(loc[7]) % 12], hisMark);
                        else if (Integer.parseInt(loc[7]) / 12 > 6)
                            setMark(board[11][Integer.parseInt(loc[7]) % 12], hisMark);
                        eatenPieces++;
                    } else {
                        if (Integer.parseInt(loc[7]) == 144 || Integer.parseInt(loc[7]) == 145) {
                            if(opponnentIsItGonnaGetEaten(Integer.parseInt(loc[6]))){
                                if(Integer.parseInt(loc[6])/12 < 5){
                                    setMark(board[0][Integer.parseInt(loc[6])% 12], hisMark);
                                }
                                else{
                                    setMark(board[11][Integer.parseInt(loc[6]) % 12], hisMark);
                                }
                            }
                            else {
                                if (Integer.parseInt(loc[6]) / 12 < 5) {
                                    if (gameCols[Integer.parseInt(loc[6]) % 12]++ < 5)
                                        setMark(board[gameCols[Integer.parseInt(loc[6]) % 12] - 1][Integer.parseInt(loc[6]) % 12],hisMark);
                                } else {
                                    if (gameCols[23 - Integer.parseInt(loc[6]) % 12]++ < 5)
                                        setMark(board[11 - gameCols[23 - Integer.parseInt(loc[6]) % 12] + 1][Integer.parseInt(loc[6]) % 12],hisMark);
                                }
                            }
                        } else {
                            if (Integer.parseInt(loc[7]) / 12 < 5) {
                                if (gameCols[Integer.parseInt(loc[7])  % 12]++ < 5)
                                    setMark(board[gameCols[Integer.parseInt(loc[7])  % 12] - 1][Integer.parseInt(loc[7])  % 12],hisMark);
                            } else {
                                if (gameCols[23 - Integer.parseInt(loc[7])  % 12]++ < 5)
                                    setMark(board[11 - gameCols[23 - Integer.parseInt(loc[7])  % 12] + 1][Integer.parseInt(loc[7])  % 12],hisMark);
                            }
                        }
                    }
                }
            }
            if(!checkDeadlock) {
                try//to prevent a loss of piece placement
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                }
                if ((Integer.parseInt(loc[1]) != 144 && Integer.parseInt(loc[1]) != 145) && Integer.parseInt(loc[0])!= Integer.parseInt(loc[1])) {
                    if (Integer.parseInt(loc[0]) / 12 < 5) {
                        if(gameCols[Integer.parseInt(loc[0])% 12] > 0 && gameCols[Integer.parseInt(loc[0])% 12] < 6)
                            setMark(board[--gameCols[Integer.parseInt(loc[0]) % 12]][Integer.parseInt(loc[0]) % 12], " ");
                        else{
                            --gameCols[Integer.parseInt(loc[0]) % 12];
                        }
                    } else {
                        if(gameCols[23 - Integer.parseInt(loc[0]) % 12] > 0 && gameCols[23 - Integer.parseInt(loc[0]) % 12] < 6)
                            setMark(board[11 - (--gameCols[23 - Integer.parseInt(loc[0])% 12])][Integer.parseInt(loc[0]) % 12], " ");
                        else{
                            --gameCols[23 - Integer.parseInt(loc[0]) % 12];
                        }
                    }
                }
                try//to prevent a loss of piece placement
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                }
                if ((Integer.parseInt(loc[3]) != 144 && Integer.parseInt(loc[2]) != 145) && Integer.parseInt(loc[2])!= Integer.parseInt(loc[3])) {
                    if (Integer.parseInt(loc[2]) / 12 < 5) {
                        if(gameCols[Integer.parseInt(loc[2]) % 12] > 0 && gameCols[Integer.parseInt(loc[2]) % 12] < 6)
                            setMark(board[--gameCols[Integer.parseInt(loc[2]) % 12]][Integer.parseInt(loc[2]) % 12], " ");
                        else{
                            --gameCols[Integer.parseInt(loc[2]) % 12];
                        }
                    } else {
                        if(gameCols[23 - Integer.parseInt(loc[2]) % 12] > 0 && gameCols[23 - Integer.parseInt(loc[2]) % 12] < 6)
                            setMark(board[11 - (--gameCols[23 - Integer.parseInt(loc[2]) % 12])][Integer.parseInt(loc[2]) % 12], " ");
                        else{
                            --gameCols[23 - Integer.parseInt(loc[2]) % 12];
                        }
                    }
                }
            }

            if(!checkDeadlock && loc.length == 8) {
                try//to prevent a loss of piece placement
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                }
                if ((Integer.parseInt(loc[5]) != 144 && Integer.parseInt(loc[5]) != 145) && Integer.parseInt(loc[4])!= Integer.parseInt(loc[5])) {
                    if (Integer.parseInt(loc[4]) / 12 < 5) {
                        if(gameCols[Integer.parseInt(loc[4]) % 12] > 0 && gameCols[Integer.parseInt(loc[4]) % 12] < 6)
                            setMark(board[--gameCols[Integer.parseInt(loc[4])  % 12]][Integer.parseInt(loc[4])  % 12], " ");
                        else{
                            --gameCols[Integer.parseInt(loc[4])  % 12];
                        }
                    } else {
                        if(gameCols[23 - Integer.parseInt(loc[4])  % 12] > 0 && gameCols[23 - Integer.parseInt(loc[4])  % 12] < 6)
                            setMark(board[11 - (--gameCols[23 - Integer.parseInt(loc[4]) % 12])][Integer.parseInt(loc[4])  % 12], " ");
                        else{
                            --gameCols[23 - Integer.parseInt(loc[4]) % 12];
                        }
                    }
                }
                try//to prevent a loss of piece placement
                {
                    Thread.sleep(5);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt(); // restore interrupted status
                }
                if ((Integer.parseInt(loc[7]) != 144 && Integer.parseInt(loc[7]) != 145) && Integer.parseInt(loc[6])!= Integer.parseInt(loc[7])) {
                    if (Integer.parseInt(loc[6]) / 12 < 5) {
                        if(gameCols[Integer.parseInt(loc[6]) % 12] > 0 && gameCols[Integer.parseInt(loc[6]) % 12] < 6)
                            setMark(board[--gameCols[Integer.parseInt(loc[6])  % 12]][Integer.parseInt(loc[6])  % 12], " ");
                        else{
                            --gameCols[Integer.parseInt(loc[6])  % 12];
                        }
                    } else {
                        if(gameCols[23 - Integer.parseInt(loc[6])  % 12] > 0){
                            if(gameCols[23 - Integer.parseInt(loc[6]) % 12] < 6){
                                setMark(board[11 - (--gameCols[23 - Integer.parseInt(loc[6]) % 12])][Integer.parseInt(loc[6]) % 12], " ");
                            }
                            else{
                                --gameCols[23 - Integer.parseInt(loc[6]) % 12];
                            }
                        }

                    }
                }

            }

            //int location = input.nextInt(); // get move location
            //input.nextLine(); // skip newline after int location
            //int row = location / 12; // calculate row
            //int column = location % 12; // calculate column

            //setMark(  board[ row ][ column ],
            //        ( myMark.equals(Y_MARK) ? B_MARK : Y_MARK) ); // mark move
            displayMessage( "Opponent moved. Your turn." );
            //dices.throwTwoDices();

            if(myMark.equals("B")) {
                dices.setDices(1, 1);
                //dices.throwTwoDices();
            }
            else {
                dices.throwTwoDices();
                dices.setDices(6,6);
            }

            myTurn = true; // now this client's turn
            subEatenPieces = 0;

            resetFunc();
            dicesAndEatenField.setText("Dices: " + dices.getDices()[0] + ", " + dices.getDices()[1] +
                    "\nMove No." + (1+moves[0]/2)+
                    "\nMove 1:" +
                    "\nMove 2:" +
                    "\nMove 3:" +
                    "\nMove 4:");
            eatenPiecesField.setText("Eaten Pieces: " + eatenPieces );

        } // end else if
        else
            displayMessage( message ); // display the message

        //change the display numbers of every col
        for(int i= 0; i<12;i++){
            firstRowBoard[i].setMark(gameCols[i]+"");
            lastRowBoard[i].setMark(gameCols[23-i]+"");
        }

    } // end method processMessage

    // manipulate outputArea in event-dispatch thread
    private void displayMessage( final String messageToDisplay )
    {
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        displayArea.append( messageToDisplay + "\n" ); // updates output
                    } // end method run
                }  // end inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method displayMessage

    // utility method to set mark on board in event-dispatch thread
    private void setMark( final Square squareToMark, final String mark )
    {
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        squareToMark.setMark( mark ); // set mark in square
                    } // end method run
                } // end anonymous inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method setMark

    // send message to server indicating clicked square
    public void sendClickedSquare()
    {
        // if it is my turn
        if ( myTurn )
        {
            if(checkDeadLock){
                output.format( "%d,%d,%d,%d\n", 170, 170, 170, 170 );
            }
            else {
                if (moves[0] == 4)
                    output.format("%d,%d,%d,%d\n", moves[1], moves[2], moves[3], moves[4]); // send location to server
                else {
                    output.format("%d,%d,%d,%d,%d,%d,%d,%d\n", moves[1], moves[2], moves[3], moves[4], moves[5], moves[6], moves[7], moves[8]);
                }
            }
            output.flush();
            myTurn = false; // not my turn anymore
        } // end if
    } // end method sendClickedSquare

    // set current Square
    public void setCurrentSquare( Square square )
    {
        currentSquare = square; // set current square to argument
    } // end method setCurrentSquare
    public void resetFunc(){
        if(myTurn && moves[0] > 0) {
            for (int i = 0; i < moves.length; i++)
                moves[i] = 0;
            dicesAndEatenField.setText("Dices: " + dices.getDices()[0] + ", " + dices.getDices()[1] +
                    "\nMove No." + (1+moves[0]/2)+
                    "\nMove 1:" +
                    "\nMove 2:" +
                    "\nMove 3:" +
                    "\nMove 4:");
            eatenPiecesField.setText("Eaten Pieces: " + eatenPieces );
        }
    }
    public void actionPerformed(ActionEvent e) {
        if(undoMoveBtn == e.getSource())
        {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if(myTurn && moves[0] > 0) {
                        if(moves[0] == 1 || moves[0] == 2){
                            if (moves[2] != 0) {
                                moves[2] = 0;
                                moves[0]--;
                            }
                            moves[1] = 0;
                            moves[0]--;
                            displayMessage("undo the first move");
                        }
                        if (moves[0] == 3 || moves[0] == 4) {
                            if (moves[4] != 0) {
                                moves[4] = 0;
                                moves[0]--;
                            }
                            moves[3] = 0;
                            moves[0]--;
                            displayMessage("undo the second move");
                        }
                        if (moves[0] == 5 || moves[0] == 6) {
                            if (moves[6] != 0) {
                                moves[6] = 0;
                                moves[0]--;
                            }
                            moves[5] = 0;
                            moves[0]--;
                            displayMessage("undo the third move");
                        }
                        if (moves[0] == 7 || moves[0] == 8) {
                            if (moves[8] != 0) {
                                moves[8] = 0;
                                moves[0]--;
                            }
                            moves[7] = 0;
                            moves[0]--;
                            displayMessage("undo the fourth move");
                        }
                    }
                }
            });
            dicesAndEatenField.setText("Dices: " + dices.getDices()[0] + ", " + dices.getDices()[1] +
                    "\nMove No." + (1+moves[0]/2)+
                    "\nMove 1:" +
                    "\nMove 2:" +
                    "\nMove 3:" +
                    "\nMove 4:");
            eatenPiecesField.setText("Eaten Pieces: " + eatenPieces );
        }
        if(finishTurnBtn == e.getSource())
        {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if(myTurn && moves[0] > 0) {
                        if ( moves[0] == 4 && dices.getDices()[0] != dices.getDices()[1]
                                || moves[0] == 8 && dices.getDices()[0] == dices.getDices()[1] || (checkStartToWin && checkOnePieceLeft) || (checkStartToWin && checkTwoPieceLeft) || (checkStartToWin && checkThreePieceLeft)) {
                            // send location of this square
                            boolean checkSmallerRemoval = false;
                            if (checkStartToWin == true) {
                                if(moves[0] == 4 || moves[0] == 8) {
                                    if (moves[3] == moves[4] && moves[1] != moves[2]) {
                                        int temp1 = moves[2], temp2 = moves[3];
                                        moves[2] = moves[4];
                                        moves[4] = temp1;
                                        moves[3] = moves[1];
                                        moves[1] = temp2;
                                    }
                                }
                            }
                            diceCalcOne = Math.abs(moves[1] % 12 - moves[2] % 12);
                            diceCalcTwo = Math.abs(moves[3] % 12 - moves[4] % 12);
                            diceCalcThree = Math.abs(moves[5] % 12 - moves[6] % 12);
                            diceCalcFour = Math.abs(moves[7] % 12 - moves[8] % 12);

                            if (moves[1] / 12 < 5 && moves[2] / 12 > 6 || moves[1] / 12 > 6 && moves[2] / 12 < 5)
                                diceCalcOne = moves[1]%12 + moves[2]%12 + 1;
                            if (moves[3] / 12 < 5 && moves[4] / 12 > 6 || moves[3] / 12 > 6 && moves[4] / 12 < 5)
                                diceCalcTwo = moves[3]%12 + moves[4]%12 + 1;
                            if (diceCalcThree!=0&&moves[5] / 12 < 5 && moves[6] / 12 > 6 || moves[5] / 12 > 6 && moves[6] / 12 < 5)
                                diceCalcThree = moves[5]%12 + moves[6]%12 + 1;
                            if (diceCalcFour!=0&&moves[7] / 12 < 5 && moves[8] / 12 > 6 || moves[7] / 12 > 6 && moves[8] / 12 < 5)
                                diceCalcFour = moves[7]%12 + moves[8]%12 + 1;
                            if((moves[2]==144||moves[2]==145) || ((moves[2]==180)))
                            {
                                if(moves[4]==144 || moves[4] == 145 || (moves[4] == moves[3]))
                                {
                                    if(((12 - moves[1] % 12 == dices.getDices()[0] && 12 - moves[3] % 12 == dices.getDices()[1])
                                            || ((12 - moves[1] % 12 == dices.getDices()[1]) && 12 - moves[3] % 12 == dices.getDices()[0]))) {
                                        sendClickedSquare();
                                    }
                                    else{
                                        displayMessage("Please move by your dices! try again new moves...");
                                        subEatenPieces = 0;
                                        resetFunc();
                                    }
                                }
                                else if (((12 - moves[1] % 12 == dices.getDices()[0] && diceCalcTwo == dices.getDices()[1])
                                        || ((12 - moves[1] % 12 == dices.getDices()[1]) && diceCalcTwo == dices.getDices()[0]))) {
                                    sendClickedSquare();
                                }
                                else {
                                    displayMessage("Please move by your dices! try again new moves...");
                                    subEatenPieces = 0;
                                    resetFunc();
                                }
                            }
                            else {
                                if(!(moves[5]!= 0 && moves[6]!=0)) {
                                    if (checkStartToWin == true) {
                                        checkSmallerRemoval = true;
                                        if (moves[1] == moves[2]) {
                                            if (moves[3] == moves[4]) {
                                                if (((12 - moves[2] % 12) <= dices.getDices()[0] && (12 - moves[4] % 12) <= dices.getDices()[1])
                                                        || ((12 - moves[2] % 12) <= dices.getDices()[1] && (12 - moves[4] % 12) <= dices.getDices()[0])) {
                                                    for (int i = 7; i < moves[2] % 12; i++) {
                                                        if (myMark.equals("Y")) {
                                                            if (gameCols[23 - i] != 0)
                                                                checkSmallerRemoval = false;
                                                        } else {
                                                            if (gameCols[i] != 0)
                                                                checkSmallerRemoval = false;
                                                        }
                                                    }
                                                    for (int i = 7; i < moves[4] % 12; i++) {
                                                        if (myMark.equals("Y")) {
                                                            if (gameCols[23 - i] != 0 && !(i == 6 && (gameCols[23-i] == 1) && moves[2] == 138))
                                                                checkSmallerRemoval = false;
                                                        } else {
                                                            if (gameCols[i] != 0 && !(i == 6 && (gameCols[i] == 1) && moves[2] == 6))
                                                                checkSmallerRemoval = false;
                                                        }
                                                    }
                                                }
                                                else{
                                                    checkSmallerRemoval = false;
                                                }
                                            }
                                            else {
                                                if (((12 - moves[2] % 12) <= dices.getDices()[0] && diceCalcTwo == dices.getDices()[1])
                                                        || ((12 - moves[2] % 12) <= dices.getDices()[1] && diceCalcTwo == dices.getDices()[0])) {
                                                    for (int i = 7; i <  moves[2] % 12; i++) {
                                                        if (myMark.equals("Y")) {
                                                            if (gameCols[23 - i] != 0)
                                                                checkSmallerRemoval = false;
                                                        } else {
                                                            if (gameCols[i] != 0)
                                                                checkSmallerRemoval = false;
                                                        }
                                                    }
                                                }
                                                else{
                                                    checkSmallerRemoval = false;
                                                }
                                            }
                                        }
                                        else{
                                            checkSmallerRemoval=false;
                                        }
                                    }
                                }
                                if(moves[5]!= 0 && moves[6]!=0){
                                    if (checkStartToWin == true) {
                                        checkSmallerRemoval = true;
                                        if (moves[1] == moves[2]) {
                                            if (moves[3] == moves[4]) {
                                                if (moves[5] == moves[6]) {
                                                    if (moves[7] == moves[8]) {
                                                        if ((((12 - moves[2] % 12) <= dices.getDices()[0])
                                                                && ((12 - moves[4] % 12) <= dices.getDices()[0])
                                                                && ((12 - moves[6] % 12) <= dices.getDices()[0])
                                                                && ((12 - moves[8] % 12) <= dices.getDices()[0]))) {
                                                            for (int i = 7; i < moves[2] % 12; i++) {
                                                                if (myMark.equals("Y")) {
                                                                    if (gameCols[23 - i] != 0)
                                                                        checkSmallerRemoval = false;
                                                                } else {
                                                                    if (gameCols[i] != 0)
                                                                        checkSmallerRemoval = false;
                                                                }
                                                            }
                                                            for (int i = 7; i < moves[4] % 12; i++) {
                                                                if (myMark.equals("Y")) {
                                                                    if (gameCols[23 - i] != 0 && !(i == 6 && (gameCols[23 - i] == 1) && moves[2] == 138))
                                                                        checkSmallerRemoval = false;
                                                                } else {
                                                                    if (gameCols[i] != 0 && !(i == 6 && (gameCols[i] == 1) && moves[2] == 6))
                                                                        checkSmallerRemoval = false;
                                                                }
                                                            }
                                                            for (int i = 7; i < moves[6] % 12; i++) {
                                                                if (myMark.equals("Y")) {
                                                                    if (gameCols[23 - i] != 0 && (!(i == 6 && (gameCols[23 - i] == 1) && moves[2] == 138)
                                                                            ||!(i == 6 && (gameCols[23 - i] == 2) && moves[2] == 138 && moves[4] == 138)))
                                                                        checkSmallerRemoval = false;
                                                                } else {
                                                                    if (gameCols[i] != 0 && (!(i == 6 && (gameCols[i] == 1) && moves[2] == 6)
                                                                            ||!(i == 6 && (gameCols[i] == 2) && moves[2] == 6 && moves[4] == 6)))
                                                                        checkSmallerRemoval = false;
                                                                }
                                                            }
                                                            for (int i = 7; i < moves[6] % 12; i++) {
                                                                if (myMark.equals("Y")) {
                                                                    if (gameCols[23 - i] != 0 && (!(i == 6 && (gameCols[23 - i] == 1) && moves[2] == 138)
                                                                            ||!(i == 6 && (gameCols[23 - i] == 2) && moves[2] == 138 && moves[4] == 138)
                                                                            ||!(i == 6 && (gameCols[23 - i] == 2) && moves[2] == 138 && moves[4] == 138 && moves[6] == 138)))
                                                                        checkSmallerRemoval = false;
                                                                } else {
                                                                    if (gameCols[i] != 0 && (!(i == 6 && (gameCols[i] == 1) && moves[2] == 6)
                                                                            ||!(i == 6 && (gameCols[i] == 2) && moves[2] == 6 && moves[4] == 6)
                                                                            ||!(i == 6 && (gameCols[i] == 3) && moves[2] == 6 && moves[4] == 6 && moves[6] == 6)))
                                                                        checkSmallerRemoval = false;
                                                                }
                                                            }
                                                        } else {
                                                            checkSmallerRemoval = false;
                                                        }
                                                    }
                                                    else {
                                                        if ((((12 - moves[2] % 12) <= dices.getDices()[0])
                                                                && ((12 - moves[4] % 12) <= dices.getDices()[0])
                                                                && ((12 - moves[6] % 12) <= dices.getDices()[0])
                                                                && ((12 - moves[8] % 12) <= dices.getDices()[0]))) {
                                                            for (int i = 7; i < moves[2] % 12; i++) {
                                                                if (myMark.equals("Y")) {
                                                                    if (gameCols[23 - i] != 0)
                                                                        checkSmallerRemoval = false;
                                                                } else {
                                                                    if (gameCols[i] != 0)
                                                                        checkSmallerRemoval = false;
                                                                }
                                                            }
                                                            for (int i = 7; i < moves[4] % 12; i++) {
                                                                if (myMark.equals("Y")) {
                                                                    if (gameCols[23 - i] != 0 && !(i == 6 && (gameCols[23 - i] == 1) && moves[2] == 138))
                                                                        checkSmallerRemoval = false;
                                                                } else {
                                                                    if (gameCols[i] != 0)
                                                                        checkSmallerRemoval = false;
                                                                }
                                                            }
                                                            for (int i = 7; i < moves[6] % 12; i++) {
                                                                if (myMark.equals("Y")) {
                                                                    if (gameCols[23 - i] != 0 && (!(i == 6 && (gameCols[23 - i] == 1) && moves[2] == 138)
                                                                            ||!(i == 6 && (gameCols[23 - i] == 2) && moves[2] == 138 && moves[4] == 138)))
                                                                        checkSmallerRemoval = false;
                                                                } else {
                                                                    if (gameCols[i] != 0)
                                                                        checkSmallerRemoval = false;
                                                                }
                                                            }
                                                        } else {
                                                            checkSmallerRemoval = false;
                                                        }
                                                    }
                                                }
                                                else {
                                                    if ((((12 - moves[2] % 12) <= dices.getDices()[0])
                                                            && ((12 - moves[4] % 12) <= dices.getDices()[0])
                                                            && ((12 - moves[6] % 12) <= dices.getDices()[0])
                                                            && ((12 - moves[8] % 12) <= dices.getDices()[0]))) {
                                                        for (int i = 7; i < moves[2] % 12; i++) {
                                                            if (myMark.equals("Y")) {
                                                                if (gameCols[23 - i] != 0)
                                                                    checkSmallerRemoval = false;
                                                            } else {
                                                                if (gameCols[i] != 0)
                                                                    checkSmallerRemoval = false;
                                                            }
                                                        }
                                                        for (int i = 7; i < moves[4] % 12; i++) {
                                                            if (myMark.equals("Y")) {
                                                                if (gameCols[23 - i] != 0 && !(i == 6 && (gameCols[23 - i] == 1) && moves[2] == 138))
                                                                    checkSmallerRemoval = false;
                                                            } else {
                                                                if (gameCols[i] != 0)
                                                                    checkSmallerRemoval = false;
                                                            }
                                                        }
                                                    } else {
                                                        checkSmallerRemoval = false;
                                                    }
                                                }
                                            }
                                            else {
                                                if (((12 - moves[2] % 12) <= dices.getDices()[0]
                                                        && diceCalcTwo == dices.getDices()[0]
                                                        && diceCalcThree == dices.getDices()[0]
                                                        && diceCalcFour == dices.getDices()[0])) {
                                                    for (int i = moves[2] % 12 - 1; i > 5; i--) {
                                                        if (myMark.equals("Y")) {
                                                            if (gameCols[23 - i] != 0)
                                                                checkSmallerRemoval = false;
                                                        } else {
                                                            if (gameCols[i] != 0)
                                                                checkSmallerRemoval = false;
                                                        }
                                                    }
                                                }
                                                else{
                                                    checkSmallerRemoval = false;
                                                }
                                            }
                                        }
                                        else{
                                            checkSmallerRemoval=false;
                                        }
                                    }
                                    if((moves[2]==144||moves[2]==145)|| (moves[2] == 180))
                                    {
                                        if(moves[4]==144 || moves[4] == 145|| (moves[4] == 180))
                                        {
                                            if(moves[6] == 144 || moves[6] == 145|| (moves[6] == 180))
                                            {
                                                if(moves[8] == 144 || moves[8] == 145|| (moves[8] == 180)) {
                                                    if (diceCalcThree != 0 && diceCalcFour != 0 && ((12 - moves[1] % 12 == dices.getDices()[0] && 12 - moves[3] % 12 == dices.getDices()[0])
                                                            && 12 - moves[5] % 12 == dices.getDices()[0] && 12 - moves[7] % 12 == dices.getDices()[0])) {
                                                        sendClickedSquare();
                                                    }
                                                    else{
                                                        displayMessage("Please move by your dices! try again new moves...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                else if (diceCalcThree != 0 && diceCalcFour != 0 && ((12 - moves[1] % 12 == dices.getDices()[0] && 12 - moves[3] % 12 == dices.getDices()[0])
                                                        && 12 - moves[5] % 12 == dices.getDices()[0] && diceCalcFour == dices.getDices()[0])) {
                                                    sendClickedSquare();
                                                }
                                                else{
                                                    displayMessage("Please move by your dices! try again new moves...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            }
                                            else if (diceCalcThree != 0 && diceCalcFour != 0 && ((12 - moves[1] % 12 == dices.getDices()[0] && 12 - moves[3] % 12 == dices.getDices()[0])
                                                    && diceCalcThree == dices.getDices()[0] && diceCalcFour == dices.getDices()[0])) {
                                                sendClickedSquare();
                                            }
                                            else{
                                                displayMessage("Please move by your dices! try again new moves...");
                                                subEatenPieces = 0;
                                                resetFunc();
                                            }
                                        }
                                        else if (diceCalcThree != 0 && diceCalcFour != 0 && ((12 - moves[1] % 12 == dices.getDices()[0] && diceCalcTwo == dices.getDices()[0])
                                                && diceCalcThree == dices.getDices()[0] && diceCalcFour == dices.getDices()[0])) {
                                            sendClickedSquare();
                                        }
                                        else {
                                            displayMessage("Please move by your dices! try again new moves...");
                                            subEatenPieces = 0;
                                            resetFunc();
                                        }
                                    }
                                    // a piece removal or a regular double
                                    else if ((checkThreePieceLeft&&checkStartToWin)||(checkTwoPieceLeft&&checkStartToWin)||(diceCalcThree != 0 && diceCalcFour != 0 && checkSmallerRemoval ||
                                            ((((moves[1] == moves[2] && (12 - moves[1]%12) == dices.getDices()[0])||diceCalcOne == dices.getDices()[0])
                                                    &&((moves[3] == moves[4] && (12 - moves[3]%12) == dices.getDices()[0]) || diceCalcTwo == dices.getDices()[0])
                                                    &&((moves[5] == moves[6] && (12 - moves[5]%12) == dices.getDices()[0]) || diceCalcThree == dices.getDices()[0])
                                                    &&((moves[3] == moves[4] && (12 - moves[3]%12) == dices.getDices()[0]) || diceCalcFour == dices.getDices()[0]))))) {
                                        if(checkOnePieceLeft && checkStartToWin)
                                        {
                                            for(int i=6; i< 12; i++){
                                                if(myMark.equals("Y")) {
                                                    if (gameCols[23 - i] == 1 && (dices.getDices()[0] + dices.getDices()[1] >= (12 - i))) {
                                                        if (!(moves[3] == moves[4] && (moves[4] == 144 || moves[4] == 145)) && !(moves[3] != moves[4])) {
                                                            moves[3] = 200;
                                                            moves[4] = 200;
                                                            moves[0] = 4;
                                                        }
                                                    }
                                                }
                                                else{
                                                    if (gameCols[i] == 1 && (dices.getDices()[0] + dices.getDices()[1] >= (12 - i))) {
                                                        if (!(moves[3] == moves[4] && (moves[4] == 144 || moves[4] == 145)) && !(moves[3] != moves[4])) {
                                                            moves[3] = 200;
                                                            moves[4] = 200;
                                                            moves[0] = 4;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if(checkTwoPieceLeft && checkStartToWin) {
                                            int countTwo = 0, countMoves = 0;
                                            /*
                                            for(int i=6; i< 12; i++){
                                                if(myMark.equals("Y")) {
                                                    if (gameCols[23 - i] > 0) {
                                                        countTwo+=gameCols[23 - i];
                                                    }
                                                }
                                                else{
                                                    if (gameCols[i] > 0) {
                                                        countTwo+=gameCols[i];
                                                    }
                                                }
                                            }
                                            */
                                            if ((moves[2] == moves[1] && moves[1] != 0) && (moves[4] == moves[3] && moves[3] != 0)) {
                                                moves[0] = 4;
                                            } else if ((moves[4] == moves[3] && moves[3] != 0) && (moves[6] == moves[5] && moves[5] != 0)){
                                                moves[0] = 8;
                                                moves[7] = 200;
                                                moves[8] = 200;
                                            }
                                        }
                                        if(checkThreePieceLeft && checkStartToWin) {
                                            /*
                                            if (!(moves[3] == moves[4] && (moves[4] == 144 || moves[4] == 145)) && !(moves[3] != moves[4])) {
                                                moves[7] = 200;
                                                moves[8] = 200;
                                                moves[0] = 8;
                                                displayMessage(moves[1] + " " + moves[2] + " " + moves[3] + " " + moves[4] + "     WINNINGGGGGG");
                                            }
                                            */if ((moves[2] == moves[1] && moves[1] != 0) && (moves[4] == moves[3] && moves[3] != 0) && (moves[6] == moves[5] && moves[5] != 0)){
                                                moves[0] = 8;
                                                moves[7] = 200;
                                                moves[8] = 200;
                                            }
                                        }
                                        sendClickedSquare();
                                    }
                                    else {
                                        displayMessage("Please move by your dices! try again new moves...");
                                        subEatenPieces = 0;
                                        resetFunc();
                                    }
                                }
                                // a piece removal or a regular turn
                                else if ((checkOnePieceLeft&&checkStartToWin)||(checkSmallerRemoval || ((((moves[1] == moves[2] && (12 - moves[1]%12) == dices.getDices()[0])||diceCalcOne == dices.getDices()[0]) && ((moves[3] == moves[4] && (12 - moves[3]%12) == dices.getDices()[1]) || diceCalcTwo == dices.getDices()[1]))
                                        || ((moves[1] == moves[2] && (12 - moves[1]%12) == dices.getDices()[1])||diceCalcOne == dices.getDices()[1]) && ((moves[3] == moves[4] && (12 - moves[3]%12) == dices.getDices()[0]) || diceCalcTwo == dices.getDices()[0])))) {
                                    if(checkOnePieceLeft && checkStartToWin)
                                    {
                                        for(int i=6; i< 12; i++){
                                            if(myMark.equals("Y")) {
                                                if (gameCols[23 - i] == 1 && (dices.getDices()[0] + dices.getDices()[1] >= (12 - i))) {
                                                    if (!(moves[3] == moves[4] && (moves[4] == 144 || moves[4] == 145)) && !(moves[3] != moves[4])) {
                                                        moves[3] = 200;
                                                        moves[4] = 200;
                                                        moves[0] = 4;
                                                        displayMessage(moves[1] + " " + moves[2] + " " + moves[3] + " " + moves[4] + "     WINNINGGGGGG");
                                                    }
                                                }
                                            }
                                            else{
                                                if (gameCols[i] == 1 && (dices.getDices()[0] + dices.getDices()[1] >= (12 - i))) {
                                                    if (!(moves[3] == moves[4] && (moves[4] == 144 || moves[4] == 145)) && !(moves[3] != moves[4])) {
                                                        moves[3] = 200;
                                                        moves[4] = 200;
                                                        moves[0] = 4;
                                                        displayMessage(moves[1] + " " + moves[2] + " " + moves[3] + " " + moves[4] + "     WINNINGGGGGG");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    sendClickedSquare();
                                } else {
                                    displayMessage("Please move by your dices! try again new moves...");
                                    subEatenPieces = 0;
                                    resetFunc();
                                }
                            }
                        } else {
                            displayMessage("Please finish your turn before submitting k?");
                        }
                        dicesAndEatenField.setText("Dices: " + dices.getDices()[0] + ", " + dices.getDices()[1] +
                                "\nMove No." + (1+moves[0]/2)+
                                "\nMove 1:" +
                                "\nMove 2:" +
                                "\nMove 3:" +
                                "\nMove 4:");
                        eatenPiecesField.setText("Eaten Pieces: " + eatenPieces );
                    }
                }
            });
        }
        if(resetMovesBtn == e.getSource())
        {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if(myTurn && moves[0] > 0) {
                        resetFunc();
                        subEatenPieces = 0;
                        displayMessage("The moves Reset has finished.");
                    }
                }
            });
        }
    }

    // private inner class for the squares on the board
    private class Square extends JPanel
    {
        private String mark; // mark to be drawn in this square
        private int location; // location of square
        public boolean isOccupied( int location )
        {
            int row = location / 12;
            int col = location % 12;
            if ( !board[ row ][ col ].mark.equals( myMark) && !board[ row ][ col ].mark.equals(" ")) {
                if(row < 5)
                {
                    if(gameCols[col] > 1)
                    {
                        return true;
                    }
                    return false;
                }
                else if(row >6)
                {
                    if(gameCols[23 - col] > 1)
                    {
                        return true;
                    }
                    return false;
                }
            }
            return false; // location is not occupied
        } // end method isOccupied
        public Square( String squareMark, int squareLocation )
        {
            mark = squareMark; // set mark for this square
            location = squareLocation; // set location of this square
            addMouseListener(
                    new MouseAdapter() {
                        public void mouseReleased( MouseEvent e )
                        {
                            if(myTurn) {
                                setCurrentSquare(Square.this); // set current square
                                int row = getSquareLocation() / 12;
                                if(eatenPieces - subEatenPieces > 0) {
                                    checkStartToWin = false;
                                    if(myMark.equals("Y")){
                                        if(isOccupied(12-dices.getDices()[0])&&isOccupied(12-dices.getDices()[1])){

                                            checkDeadLock = true;
                                            sendClickedSquare();
                                        }
                                        else{
                                            if (getSquareLocation()/12 >= 0 && getSquareLocation()/12 < 5 && getSquareLocation()%12 > 6 && getSquareLocation()%12 < 12 &&
                                                    (moves[0] < 4 && dices.getDices()[0] != dices.getDices()[1]
                                                            || moves[0] < 8 && dices.getDices()[0] == dices.getDices()[1])) {
                                                moves[1 + moves[0]++] = getSquareLocation();
                                                moves[1 + moves[0]++] = 144;
                                                subEatenPieces++;
                                            }
                                        }
                                    }
                                    else{
                                        if(isOccupied(12*11 + 12 - dices.getDices()[0])&&isOccupied(12*11 + 12 - dices.getDices()[1])){

                                            checkDeadLock = true;
                                            sendClickedSquare();
                                        }
                                        else{
                                            if (getSquareLocation()/12 > 6 && getSquareLocation()/12 < 12 && getSquareLocation()%12 > 6 && getSquareLocation()%12 < 12 &&
                                                    (moves[0] < 4 && dices.getDices()[0] != dices.getDices()[1]
                                                            || moves[0] < 8 && dices.getDices()[0] == dices.getDices()[1])) {
                                                moves[1 + moves[0]++] = getSquareLocation();
                                                moves[1 + moves[0]++] = 145;
                                                subEatenPieces++;
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    if (row < 5 || row > 6) {
                                        if (moves[0] < 4 && dices.getDices()[0] != dices.getDices()[1] || moves[0] < 8 && dices.getDices()[0] == dices.getDices()[1])
                                            moves[1 + moves[0]++] = getSquareLocation();
                                        if (moves[0] == 2) {
                                            if (myMark.equals("Y")) {
                                                if (moves[1] / 12 < 5 && moves[2] / 12 < 5) {
                                                    if (moves[1] % 12 - moves[2] % 12 <= 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[1] / 12 > 6 && moves[2] / 12 > 6) {
                                                    if (moves[1] % 12 - moves[2] % 12 > 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[1] / 12 > 6 && moves[2] / 12 < 5) {
                                                    displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            } else {
                                                if (moves[1] / 12 < 5 && moves[2] / 12 < 5) {
                                                    if (moves[1] % 12 - moves[2] % 12 > 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[1] / 12 > 6 && moves[2] / 12 > 6) {
                                                    if (moves[1] % 12 - moves[2] % 12 <= 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[1] / 12 < 5 && moves[2] / 12 > 6) {
                                                    displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            }
                                        }
                                        if (moves[0] == 4) {
                                            if (myMark.equals("Y")) {
                                                if (moves[3] / 12 < 5 && moves[4] / 12 < 5) {
                                                    if (moves[3] % 12 - moves[4] % 12 <= 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[3] / 12 > 6 && moves[4] / 12 > 6) {
                                                    if (moves[3] % 12 - moves[4] % 12 > 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[3] / 12 > 6 && moves[4] / 12 < 5) {
                                                    displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            } else {
                                                if (moves[3] / 12 < 5 && moves[4] / 12 < 5) {
                                                    if (moves[3] % 12 - moves[4] % 12 > 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[3] / 12 > 6 && moves[4] / 12 > 6) {
                                                    if (moves[3] % 12 - moves[4] % 12 <= 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[3] / 12 < 5 && moves[4] / 12 > 6) {
                                                    displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            }
                                        }
                                        if (moves[0] == 6) {
                                            if (myMark.equals("Y")) {
                                                if (moves[5] / 12 < 5 && moves[6] / 12 < 5) {
                                                    if (moves[5] % 12 - moves[6] % 12 <= 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[5] / 12 > 6 && moves[6] / 12 > 6) {
                                                    if (moves[5] % 12 - moves[6] % 12 > 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[5] / 12 > 6 && moves[6] / 12 < 5) {
                                                    displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            } else {
                                                if (moves[5] / 12 < 5 && moves[6] / 12 < 5) {
                                                    if (moves[5] % 12 - moves[6] % 12 > 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[5] / 12 > 6 && moves[6] / 12 > 6) {
                                                    if (moves[5] % 12 - moves[6] % 12 <= 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[5] / 12 < 5 && moves[6] / 12 > 6) {
                                                    displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            }
                                        }
                                        if (moves[0] == 8) {
                                            if (myMark.equals("Y")) {
                                                if (moves[7] / 12 < 5 && moves[8] / 12 < 5) {
                                                    if (moves[7] % 12 - moves[8] % 12 <= 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[7] / 12 > 6 && moves[8] / 12 > 6) {
                                                    if (moves[7] % 12 - moves[8] % 12 > 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[7] / 12 > 6 && moves[8] / 12 < 5) {
                                                    displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            } else {
                                                if (moves[7] / 12 < 5 && moves[8] / 12 < 5) {
                                                    if (moves[7] % 12 - moves[8] % 12 > 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[7] / 12 > 6 && moves[8] / 12 > 6) {
                                                    if (moves[7] % 12 - moves[8] % 12 <= 0 && checkStartToWin ==false) {
                                                        displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                        subEatenPieces = 0;
                                                        resetFunc();
                                                    }
                                                }
                                                if (moves[7] / 12 < 5 && moves[8] / 12 > 6) {
                                                    displayMessage("Please move by your dices and by the rules!\nPlease try again...");
                                                    subEatenPieces = 0;
                                                    resetFunc();
                                                }
                                            }

                                        }
                                    }
                                }
                                dicesAndEatenField.setText("Dices: " + dices.getDices()[0] + ", " + dices.getDices()[1] +
                                        "\nMove No." + (1+moves[0]/2) +
                                        "\nMove 1: From (Row:" + moves[1] / 12 + ", Col:" + moves[1] % 12 + ") To (Row:"
                                        + moves[2] / 12 + ", Col:" + moves[2] % 12 + ")" +
                                        "\nMove 2: From (Row:" + moves[3] / 12 + ", Col:" + moves[3] % 12 + ") To (Row:"
                                        + moves[4] / 12 + ", Col:" + moves[4] % 12 + ")" +
                                        "\nMove 3: From (Row:" + moves[5] / 12 + ", Col:" + moves[5] % 12 + ") To (Row:"
                                        + moves[6] / 12 + ", Col:" + moves[6] % 12 + ")" +
                                        "\nMove 4: From (Row:" + moves[7] / 12 + ", Col:" + moves[7] % 12 + ") To (Row:"
                                        + moves[8] / 12 + ", Col:" + moves[8] % 12 + ")");
                                eatenPiecesField.setText("Eaten Pieces: " + eatenPieces );
                            }
                        } // end method mouseReleased
                    } // end anonymous inner class
            ); // end call to addMouseListener
        } // end Square constructor

        // return preferred size of Square
        public Dimension getPreferredSize()
        {
            return new Dimension( 30, 30 ); // return preferred size
        } // end method getPreferredSize

        // return minimum size of Square
        public Dimension getMinimumSize()
        {
            return getPreferredSize(); // return preferred size
        } // end method getMinimumSize

        // set mark for Square
        public void setMark( String newMark )
        {
            mark = newMark; // set mark of square
            repaint(); // repaint square
        } // end method setMark

        // return Square location
        public int getSquareLocation()
        {
            return location; // return location of square
        } // end method getSquareLocation

        // draw Square
        public void paintComponent( Graphics g )
        {
            super.paintComponent( g );

            g.drawRect( 0, 0, 29, 29 ); // draw square
            g.drawString( mark, 11, 20 ); // draw mark
        } // end method paintComponent
    } // end inner-class Square
} // end class TicTacToeClient

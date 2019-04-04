// Fig. 24.13: BackgammonServer.java
// This class maintains a game of Tic-Tac-Toe for two clients.
import com.sun.security.ntlm.Server;

import java.awt.BorderLayout;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import javax.swing.*;

public class BackgammonServer extends JFrame
{
    private int[] gameCols = new int[26];
    private String[] board = new String[ 144 ]; // tic-tac-toe board
    private JTextArea outputArea; // for outputting moves
    private Player[] players; // array of Players
    private ServerSocket server; // server socket to connect with clients
    private int currentPlayer; // keeps track of player with current move
    private final static int PLAYER_Y = 0; // constant for first player
    private final static int PLAYER_B = 1; // constant for second player
    private final static String[] MARKS = { "Y", "B" }; // array of marks
    private ExecutorService runGame; // will run players
    private Lock gameLock; // to lock game for synchronization
    private Condition otherPlayerConnected; // to wait for other player
    private Condition otherPlayerTurn; // to wait for other player's turn
    private int[] eatenPieces = new int[2];
    private int[] subFromEatenPieces = new int[2];
    private boolean[] checkEaten = new boolean[4];
    // set up tic-tac-toe server and GUI that displays messages
    public BackgammonServer(String servIP, int servPort)
    {
        super( "Backgammon Server" ); // set title of window

        // create ExecutorService with a thread for each player
        runGame = Executors.newFixedThreadPool( 2 );
        gameLock = new ReentrantLock(); // create lock for game

        // condition variable for both players being connected
        otherPlayerConnected = gameLock.newCondition();

        // condition variable for the other player's turn
        otherPlayerTurn = gameLock.newCondition();


        for ( int row = 0; row < 12; row++ )
        {
            // loop over the columns in the board
            for ( int column = 0; column < 12; column++ )
            {
                // create square & fill it
                if((column == 0 && (row !=5 && row != 6))  || (column == 4 && (row < 3 || row > 8)) || (column == 6 &&
                        (row !=5 && row != 6)) || (column == 11 && (row < 2 || row > 9)))
                {
                    //yellow fill
                    if((column == 0 && row < 5) || (column == 4 && row > 8) || (column == 6 && row > 6) ||
                            (column == 11 && row < 2))
                    {
                        board[ row*12 + column ] = "Y";
                    }
                    else
                    {
                        board[ row*12 + column ] = "B";
                    }
                }
                else {
                    board[row*12 + column] = " ";
                }
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

        /* Fast Win Board

        for( int i = 0; i< 12; i++)
        {
            for( int j = 0; j < 12; j++)
            {
                board[i*12 + j] = new String(" ");
            }
        }
        board[ 0 * 12 + 6 ] = "B";
        board[ 1 * 12 + 6 ] = "B";
        board[ 2 * 12 + 6 ] = "B";
        board[ 3 * 12 + 6 ] = "B";
        board[ 4 * 12 + 6 ] = "B";

        board[ 11 * 12 + 6 ] = "Y";
        board[ 10 * 12 + 6 ] = "Y";
        board[ 9 * 12 + 6 ] = "Y";
        board[ 8 * 12 + 6 ] = "Y";
        board[ 7 * 12 + 6 ] = "Y";

        gameCols[6] = 15;
        gameCols[23 - 6] = 15;
        */

        gameCols[24] = 100;
        gameCols[25] = 100;

        players = new Player[ 2 ]; // create array of players
        currentPlayer = PLAYER_Y; // set current player to first player

        try
        {
            server = new ServerSocket(); // set up ServerSocket
            server.bind(new InetSocketAddress(servIP, servPort), 2 );
        } // end try
        catch ( IOException ioException )
        {
            ioException.printStackTrace();
            System.exit( 1 );
        } // end catch

        outputArea = new JTextArea(); // create JTextArea for output
        add( new JScrollPane(outputArea), BorderLayout.CENTER );
        outputArea.setText( "Server awaiting connections\n" );

        setSize( 500, 500 ); // set size of window
        setVisible( true ); // show window
    } // end BackgammonServer constructor

    // wait for two connections so game can be played
    public void execute()
    {
        // wait for each client to connect
        for ( int i = 0; i < players.length; i++ )
        {
            try // wait for connection, create Player, start runnable
            {
                players[ i ] = new Player( server.accept(), i );
                runGame.execute( players[ i ] ); // execute player runnable
            } // end try
            catch ( IOException ioException )
            {
                ioException.printStackTrace();
                System.exit( 1 );
            } // end catch
        } // end for

        gameLock.lock(); // lock game to signal player Y's thread

        try
        {
            players[PLAYER_Y].setSuspended( false ); // resume player Y
            otherPlayerConnected.signal(); // wake up player Y's thread
        } // end try
        finally
        {
            gameLock.unlock(); // unlock game after signalling player Y
        } // end finally
    } // end method execute

    // display message in outputArea
    private void displayMessage( final String messageToDisplay )
    {
        // display message from event-dispatch thread of execution
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run() // updates outputArea
                    {
                        outputArea.append( messageToDisplay ); // add message
                    } // end  method run
                } // end inner class
        ); // end call to SwingUtilities.invokeLater
    } // end method displayMessage

    // determine if move is valid
    public boolean isItGonnaGetEaten(int destinationLocation)
    {//to check if the piece is hella hungry
        int row = destinationLocation / 12;
        int col = destinationLocation % 12;
        if(destinationLocation == 145 || destinationLocation == 144)
            return false;
        if(row < 5){
            if(gameCols[ col ] == 1)
            {
                if((!board[col].equals(MARKS[ currentPlayer ])&&!board[col].equals(" ")))
                    return true;
            }
        }
        else{
            if(gameCols[ 23-col ] == 1)
            {
                if((!board[ 11*12 + col ].equals(MARKS[ currentPlayer ])&&!board[ 11*12 + col ].equals(" ")))
                    return true;
            }
        }
        return false;
    }
    public boolean isFilledBySameColor(int sourceLocation)
    {
        int row = sourceLocation / 12;
        int col = sourceLocation % 12;
        if(row < 5){
            if(gameCols[ col ] > 0)
            {
                if(board[col].equals(MARKS[ currentPlayer ]) || board[col].equals(" "))
                    return true;
            }
        }
        else{
            if(gameCols[ 23-col ] > 0)
            {
                if(board[ 11*12 + col ].equals(MARKS[ currentPlayer ]) || board[ 11*12 + col].equals(" "))
                    return true;
            }
        }
        return false;
    }
    public boolean validateAndMoveDouble( int firstSourceLocation, int firstDestinationLocation,
                                          int secondSourceLocation, int secondDestinationLocation,
                                          int thirdSourceLocation, int thirdDestinationLocation,
                                          int fourthSourceLocation, int fourthDestinationLocation, int player)
    {
        // while not current player, must wait for turn
        while ( player != currentPlayer )
        {
            gameLock.lock(); // lock game to wait for other player to go

            try
            {
                otherPlayerTurn.await(); // wait for player's turn
            } // end try
            catch ( InterruptedException exception )
            {
                exception.printStackTrace();
            } // end catch
            finally
            {
                gameLock.unlock(); // unlock game after waiting
            } // end finally
        } // end while

        int[] backupGameCols = new int[24];

        for( int i = 0; i < backupGameCols.length; i++)
            backupGameCols[i] = gameCols[i];

        // if location not occupied, make move
        if(firstDestinationLocation == firstSourceLocation) {
            if (MARKS[currentPlayer].equals("Y")) {
                if (gameCols[23 - firstDestinationLocation % 12] > 0)
                {
                    if(gameCols[23 - firstDestinationLocation % 12] < 6) {
                        board[(11 - (gameCols[23 - firstSourceLocation % 12] - 1)) * 12 + firstSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[23 - firstSourceLocation % 12]--;
            } else {
                if (gameCols[firstDestinationLocation % 12] > 0) {
                    if (gameCols[firstDestinationLocation % 12] < 6) {
                        board[(gameCols[firstSourceLocation % 12] - 1) * 12 + firstSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[firstSourceLocation % 12]--;
            }
        }
        else {
            if ((MARKS[currentPlayer].equals("Y") && eatenPieces[0] - subFromEatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] - subFromEatenPieces[1] > 0)) {
                if ((firstDestinationLocation == 144 || firstDestinationLocation == 145) && isItGonnaGetEaten(firstSourceLocation)) {
                    if (firstSourceLocation / 12 < 5) {
                        board[firstSourceLocation % 12] = MARKS[currentPlayer];
                    }
                    if (firstSourceLocation > 6) {
                        board[11 * 12 + firstSourceLocation % 12] = MARKS[currentPlayer];
                    }
                    checkEaten[0] = true;
                } else if (isItGonnaGetEaten(firstDestinationLocation)) {
                    if (firstDestinationLocation / 12 < 5) {
                        board[firstDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if (firstDestinationLocation > 6) {
                        board[11 * 12 + firstDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    checkEaten[0] = true;
                } else {
                    if (MARKS[currentPlayer].equals("B")) {
                        board[(11 - gameCols[23 - firstSourceLocation % 12]++) * 12 + firstSourceLocation % 12] = MARKS[currentPlayer];
                        subFromEatenPieces[0]++;
                    } else {
                        board[gameCols[firstSourceLocation % 12]++ * 12 + firstSourceLocation % 12] = MARKS[currentPlayer];
                        subFromEatenPieces[1]++;
                    }
                }
            } else if (isItGonnaGetEaten(firstDestinationLocation)) {
                if (firstDestinationLocation / 12 < 5) {
                    board[firstDestinationLocation % 12] = MARKS[currentPlayer];
                    if(firstSourceLocation/12 < 5) {
                        gameCols[firstSourceLocation % 12]--;
                    }
                    else{
                        gameCols[23 - firstSourceLocation % 12]--;
                    }
                }
                if (firstDestinationLocation > 6) {
                    board[11 * 12 + firstDestinationLocation % 12] = MARKS[currentPlayer];
                }
                checkEaten[0] = true;
            }
        }

        if(secondDestinationLocation == secondSourceLocation && secondDestinationLocation != 200) {
            if (MARKS[currentPlayer].equals("Y")) {
                if (gameCols[23 - secondDestinationLocation % 12] > 0)
                {
                    if(gameCols[23 - secondDestinationLocation % 12] < 6) {
                        board[(11 - (gameCols[23 - secondSourceLocation % 12] - 1)) * 12 + secondSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[23 - secondSourceLocation % 12]--;
            } else {
                if (gameCols[secondDestinationLocation % 12] > 0) {
                    if (gameCols[secondDestinationLocation % 12] < 6) {
                        board[(gameCols[secondSourceLocation % 12] - 1) * 12 + secondSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[secondSourceLocation % 12]--;
            }
        }
        else {
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if ((MARKS[currentPlayer].equals("Y") && eatenPieces[0] - subFromEatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] - subFromEatenPieces[1] > 1)) {
                    if ((secondDestinationLocation == 145 || secondDestinationLocation == 144) && isItGonnaGetEaten(secondSourceLocation)) {
                        if (secondSourceLocation / 12 < 5) {
                            board[secondSourceLocation % 12] = MARKS[currentPlayer];
                        }
                        if (secondSourceLocation > 6) {
                            board[11 * 12 + secondSourceLocation % 12] = MARKS[currentPlayer];
                        }
                        checkEaten[1] = true;
                    } else if (isItGonnaGetEaten(secondDestinationLocation)) {
                        if (secondDestinationLocation / 12 < 5) {
                            board[secondDestinationLocation % 12] = MARKS[currentPlayer];
                        }
                        if (secondDestinationLocation > 6) {
                            board[11 * 12 + secondDestinationLocation % 12] = MARKS[currentPlayer];
                        }
                        checkEaten[1] = true;
                    } else {
                        if (MARKS[currentPlayer].equals("B")) {
                            board[(11 - gameCols[23 - secondSourceLocation % 12]++) * 12 + secondSourceLocation % 12] = MARKS[currentPlayer];
                            subFromEatenPieces[0]++;
                        } else {
                            board[gameCols[secondSourceLocation % 12]++ * 12 + secondSourceLocation % 12] = MARKS[currentPlayer];
                            subFromEatenPieces[1]++;
                        }
                    }
                } else if (isItGonnaGetEaten(secondDestinationLocation)) {
                    if (secondDestinationLocation / 12 < 5) {
                        board[secondDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if (secondDestinationLocation > 6) {
                        board[11 * 12 + secondDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if(secondSourceLocation/12 < 5) {
                        gameCols[secondSourceLocation % 12]--;
                    }
                    else{
                        gameCols[23 - secondSourceLocation % 12]--;
                    }
                    checkEaten[1] = true;
                }
            }
        }

        if(thirdDestinationLocation == thirdSourceLocation && thirdDestinationLocation != 200) {
            if (MARKS[currentPlayer].equals("Y")) {
                if (gameCols[23 - thirdDestinationLocation % 12] > 0)
                {
                    if(gameCols[23 - thirdDestinationLocation % 12] < 6) {
                        board[(11 - (gameCols[23 - thirdSourceLocation % 12] - 1)) * 12 + thirdSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[23 - thirdSourceLocation % 12]--;
            } else {
                if (gameCols[thirdDestinationLocation % 12] > 0) {
                    if (gameCols[thirdDestinationLocation % 12] < 6) {
                        board[(gameCols[thirdSourceLocation % 12] - 1) * 12 + thirdSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[thirdSourceLocation % 12]--;
            }
        }
        else {
            if (thirdDestinationLocation != thirdSourceLocation && thirdDestinationLocation != 200) {
                if ((MARKS[currentPlayer].equals("Y") && eatenPieces[0] - subFromEatenPieces[0] > 2) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] - subFromEatenPieces[1] > 2)) {
                    if ((thirdDestinationLocation == 145 || thirdDestinationLocation == 144) && isItGonnaGetEaten(thirdSourceLocation)) {
                        if (thirdSourceLocation / 12 < 5) {
                            board[thirdSourceLocation % 12] = MARKS[currentPlayer];
                        }
                        if (thirdSourceLocation > 6) {
                            board[11 * 12 + thirdSourceLocation % 12] = MARKS[currentPlayer];
                        }
                        checkEaten[1] = true;
                    } else if (isItGonnaGetEaten(thirdDestinationLocation)) {
                        if (thirdDestinationLocation / 12 < 5) {
                            board[thirdDestinationLocation % 12] = MARKS[currentPlayer];
                        }
                        if (thirdDestinationLocation > 6) {
                            board[11 * 12 + thirdDestinationLocation % 12] = MARKS[currentPlayer];
                        }
                        checkEaten[1] = true;
                    } else {
                        if (MARKS[currentPlayer].equals("B")) {
                            board[(11 - gameCols[23 - thirdSourceLocation % 12]++) * 12 + thirdSourceLocation % 12] = MARKS[currentPlayer];
                            subFromEatenPieces[0]++;
                        } else {
                            board[gameCols[thirdSourceLocation % 12]++ * 12 + thirdSourceLocation % 12] = MARKS[currentPlayer];
                            subFromEatenPieces[1]++;
                        }
                    }
                } else if (isItGonnaGetEaten(thirdDestinationLocation)) {
                    if (thirdDestinationLocation / 12 < 5) {
                        board[thirdDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if (thirdDestinationLocation > 6) {
                        board[11 * 12 + thirdDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if(thirdSourceLocation/12 < 5) {
                        gameCols[thirdSourceLocation % 12]--;
                    }
                    else{
                        gameCols[23 - thirdSourceLocation % 12]--;
                    }
                    checkEaten[2] = true;
                }
            }
        }

        if(fourthDestinationLocation == fourthSourceLocation && fourthDestinationLocation != 200) {
            if (MARKS[currentPlayer].equals("Y")) {
                if (gameCols[23 - fourthDestinationLocation % 12] > 0)
                {
                    if(gameCols[23 - fourthDestinationLocation % 12] < 6) {
                        board[(11 - (gameCols[23 - fourthSourceLocation % 12] - 1)) * 12 + fourthSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[23 - fourthSourceLocation % 12]--;
            } else {
                if (gameCols[fourthDestinationLocation % 12] > 0) {
                    if (gameCols[fourthDestinationLocation % 12] < 6) {
                        board[(gameCols[fourthSourceLocation % 12] - 1) * 12 + fourthSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[fourthSourceLocation % 12]--;
            }
        }
        else {
            if (fourthDestinationLocation != fourthSourceLocation && fourthDestinationLocation != 200) {
                if ((MARKS[currentPlayer].equals("Y") && eatenPieces[0] - subFromEatenPieces[0] > 3) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] - subFromEatenPieces[1] > 3)) {
                    if ((fourthDestinationLocation == 145 || fourthDestinationLocation == 144) && isItGonnaGetEaten(fourthSourceLocation)) {
                        if (fourthSourceLocation / 12 < 5) {
                            board[fourthSourceLocation % 12] = MARKS[currentPlayer];
                        }
                        if (fourthSourceLocation > 6) {
                            board[11 * 12 + fourthSourceLocation % 12] = MARKS[currentPlayer];
                        }
                        checkEaten[1] = true;
                    } else if (isItGonnaGetEaten(fourthDestinationLocation)) {
                        if (fourthDestinationLocation / 12 < 5) {
                            board[fourthDestinationLocation % 12] = MARKS[currentPlayer];
                        }
                        if (fourthDestinationLocation > 6) {
                            board[11 * 12 + fourthDestinationLocation % 12] = MARKS[currentPlayer];
                        }
                        checkEaten[1] = true;
                    } else {
                        if (MARKS[currentPlayer].equals("B")) {
                            board[(11 - gameCols[23 - fourthSourceLocation % 12]++) * 12 + fourthSourceLocation % 12] = MARKS[currentPlayer];
                            subFromEatenPieces[0]++;
                        } else {
                            board[gameCols[fourthSourceLocation % 12]++ * 12 + fourthSourceLocation % 12] = MARKS[currentPlayer];
                            subFromEatenPieces[1]++;
                        }
                    }
                } else if (isItGonnaGetEaten(fourthDestinationLocation)) {
                    if (fourthDestinationLocation / 12 < 5) {
                        board[fourthDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if (fourthDestinationLocation > 6) {
                        board[11 * 12 + fourthDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if(fourthSourceLocation/12 < 5) {
                        gameCols[fourthSourceLocation % 12]--;
                    }
                    else{
                        gameCols[23 - fourthSourceLocation % 12]--;
                    }
                    checkEaten[2] = true;
                }
            }
        }
        /*
        if(firstDestinationLocation == 144 || firstDestinationLocation == 145)
        {

        }
        if(isItGonnaGetEaten(firstDestinationLocation))
        {
            if(firstDestinationLocation/12 < 5)
            {
                board[firstDestinationLocation%12] = MARKS[currentPlayer];
            }
            if(firstDestinationLocation > 6)
            {
                board[11*12 + firstDestinationLocation%12] = MARKS[currentPlayer];
            }
            checkEaten[0] = true;
        }
        if(isItGonnaGetEaten(secondDestinationLocation))
        {
            if(secondDestinationLocation/12 < 5)
            {
                board[secondDestinationLocation%12] = MARKS[currentPlayer];
            }
            if(secondDestinationLocation > 6)
            {
                board[11*12 + secondDestinationLocation%12] = MARKS[currentPlayer];
            }
            checkEaten[1] = true;
        }
        if(isItGonnaGetEaten(thirdDestinationLocation))
        {
            if(thirdDestinationLocation/12 < 5)
            {
                board[thirdDestinationLocation%12] = MARKS[currentPlayer];
            }
            if(thirdDestinationLocation > 6)
            {
                board[11*12 + thirdDestinationLocation%12] = MARKS[currentPlayer];
            }
            checkEaten[2] = true;
        }
        if(isItGonnaGetEaten(fourthDestinationLocation))
        {
            if(fourthDestinationLocation/12 < 5)
            {
                board[fourthDestinationLocation%12] = MARKS[currentPlayer];
            }
            if(fourthDestinationLocation > 6)
            {
                board[11*12 + fourthDestinationLocation%12] = MARKS[currentPlayer];
            }
            checkEaten[3] = true;
        }
        */
        if (!(firstSourceLocation == firstDestinationLocation && secondDestinationLocation == secondSourceLocation && thirdSourceLocation == thirdDestinationLocation && fourthSourceLocation == fourthDestinationLocation) && checkEaten[0] || (!isOccupied( firstDestinationLocation ) && isFilledBySameColor( firstSourceLocation )))
        {
            int firstRow = firstDestinationLocation / 12;
            int firstCol = firstDestinationLocation % 12;
            int secondRow = secondDestinationLocation / 12;
            int secondCol = secondDestinationLocation % 12;
            int thirdRow = thirdDestinationLocation / 12;
            int thirdCol = thirdDestinationLocation % 12;
            int fourthRow = fourthDestinationLocation / 12;
            int fourthCol = fourthDestinationLocation % 12;

            //firstMove
            if (firstSourceLocation!=firstDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 0)) && !checkEaten[0])) {
                if (firstRow < 5) {
                    if (gameCols[firstCol] < 5) {
                        firstDestinationLocation = (gameCols[firstCol]) * 12 + firstCol;
                    }
                    gameCols[firstCol]++;
                } else if (firstRow > 6) {
                    if (gameCols[23 - firstCol] < 5) {
                        firstDestinationLocation = (11 - gameCols[23 - firstCol]) * 12 + firstCol;
                    }
                    gameCols[23 - firstCol]++;
                }
            }
            //secondMove
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if (!(firstSourceLocation == firstDestinationLocation && secondDestinationLocation == secondSourceLocation) && (MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 1) || checkEaten[1] || (!isOccupied(secondDestinationLocation) && isFilledBySameColor(secondSourceLocation))) {
                    if (secondDestinationLocation != secondSourceLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 1)) && !checkEaten[1])) {
                        if (secondRow < 5) {
                            if (gameCols[secondCol] < 5) {
                                secondDestinationLocation = (gameCols[secondCol] * 12 + secondCol);
                            }
                            if (!isItGonnaGetEaten(secondDestinationLocation))
                                gameCols[secondCol]++;
                        } else if (secondRow > 6) {
                            if (gameCols[23 - secondCol] < 5) {
                                secondDestinationLocation = (11 - gameCols[23 - secondCol]) * 12 + secondCol;
                            }
                            if (!isItGonnaGetEaten(secondDestinationLocation))
                                gameCols[23 - secondCol]++;
                        }
                    }
                } else {
                    for (int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
            }

            //thirdMove
            if (thirdDestinationLocation != thirdSourceLocation && thirdDestinationLocation != 200) {
                if (!(firstSourceLocation == firstDestinationLocation && thirdDestinationLocation == thirdSourceLocation && thirdSourceLocation == thirdDestinationLocation && fourthSourceLocation == fourthDestinationLocation) && (MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 2) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 2) || checkEaten[1] || (!isOccupied(thirdDestinationLocation) && isFilledBySameColor(thirdSourceLocation))) {
                    if (thirdDestinationLocation != thirdSourceLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 2) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 2)) && !checkEaten[1])) {
                        if (thirdRow < 5) {
                            if (gameCols[thirdCol] < 5) {
                                thirdDestinationLocation = (gameCols[thirdCol] * 12 + thirdCol);
                            }
                            if (!isItGonnaGetEaten(thirdDestinationLocation))
                                gameCols[thirdCol]++;
                        } else if (thirdRow > 6) {
                            if (gameCols[23 - thirdCol] < 5) {
                                thirdDestinationLocation = (11 - gameCols[23 - thirdCol]) * 12 + thirdCol;
                            }
                            if (!isItGonnaGetEaten(thirdDestinationLocation))
                                gameCols[23 - thirdCol]++;
                        }
                    }
                } else {
                    for (int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
            }
            if (fourthDestinationLocation != fourthSourceLocation && fourthDestinationLocation != 200) {
                if (!(firstSourceLocation == firstDestinationLocation && fourthDestinationLocation == fourthSourceLocation && fourthSourceLocation == fourthDestinationLocation && fourthSourceLocation == fourthDestinationLocation) && (MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 2) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 2) || checkEaten[1] || (!isOccupied(fourthDestinationLocation) && isFilledBySameColor(fourthSourceLocation))) {
                    if (fourthDestinationLocation != fourthSourceLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 2) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 2)) && !checkEaten[1])) {
                        if (fourthRow < 5) {
                            if (gameCols[fourthCol] < 5) {
                                fourthDestinationLocation = (gameCols[fourthCol] * 12 + fourthCol);
                            }
                            if (!isItGonnaGetEaten(fourthDestinationLocation))
                                gameCols[fourthCol]++;
                        } else if (fourthRow > 6) {
                            if (gameCols[23 - fourthCol] < 5) {
                                fourthDestinationLocation = (11 - gameCols[23 - fourthCol]) * 12 + fourthCol;
                            }
                            if (!isItGonnaGetEaten(fourthDestinationLocation))
                                gameCols[23 - fourthCol]++;
                        }
                    }
                } else {
                    for (int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
            }

            // set moves on board
            if(firstSourceLocation!=firstDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 0)) && !checkEaten[0])) {
                if (firstSourceLocation / 12 < 5) {
                    if(gameCols[firstSourceLocation % 12] > 0) {
                        if (gameCols[firstSourceLocation % 12] < 6)
                            board[gameCols[firstSourceLocation % 12] * 12 + firstSourceLocation % 12 - 12] = " ";
                        gameCols[firstSourceLocation % 12]--;
                    }
                    else{
                        for( int i = 0; i < backupGameCols.length; i++)
                            gameCols[i] = backupGameCols[i];
                        return false;                    }
                } else {
                    if (gameCols[23 - firstSourceLocation % 12] > 0) {
                        if (gameCols[23 - firstSourceLocation % 12] < 6)
                            board[(11 - gameCols[23 - firstSourceLocation % 12]) * 12 + firstSourceLocation % 12 + 12] = " ";
                        gameCols[23 - firstSourceLocation % 12]--;
                    } else {
                        for (int i = 0; i < backupGameCols.length; i++)
                            gameCols[i] = backupGameCols[i];
                        return false;
                    }
                }
            }
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if (secondDestinationLocation != secondSourceLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 1)) && !checkEaten[1])) {
                    if (secondSourceLocation / 12 < 5) {
                        if (gameCols[secondSourceLocation % 12] > 0) {
                            if (gameCols[secondSourceLocation % 12] < 6)
                                board[gameCols[secondSourceLocation % 12] * 12 + secondSourceLocation % 12 - 12] = " ";
                            gameCols[secondSourceLocation % 12]--;
                        } else {
                            for (int i = 0; i < backupGameCols.length; i++)
                                gameCols[i] = backupGameCols[i];
                            return false;
                        }
                    } else {
                        if (gameCols[23 - secondSourceLocation % 12] > 0) {
                            if (gameCols[23 - secondSourceLocation % 12] < 6)
                                board[(11 - gameCols[23 - secondSourceLocation % 12]) * 12 + secondSourceLocation % 12 + 12] = " ";
                            gameCols[23 - secondSourceLocation % 12]--;
                        } else {
                            for (int i = 0; i < backupGameCols.length; i++)
                                gameCols[i] = backupGameCols[i];
                            return false;
                        }
                    }
                }
            }
            if (thirdDestinationLocation != thirdSourceLocation && thirdDestinationLocation != 200) {
                if (thirdDestinationLocation != thirdSourceLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 2) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 2)) && !checkEaten[2])) {
                    if (thirdSourceLocation / 12 < 5) {
                        if (gameCols[thirdSourceLocation % 12] > 0) {
                            if (gameCols[thirdSourceLocation % 12] < 6)
                                board[gameCols[thirdSourceLocation % 12] * 12 + thirdSourceLocation % 12 - 12] = " ";
                            gameCols[thirdSourceLocation % 12]--;
                        } else {
                            for (int i = 0; i < backupGameCols.length; i++)
                                gameCols[i] = backupGameCols[i];
                            return false;
                        }
                    } else {
                        if (gameCols[23 - thirdSourceLocation % 12] > 0) {
                            if (gameCols[23 - thirdSourceLocation % 12] < 6)
                                board[(11 - gameCols[23 - thirdSourceLocation % 12]) * 12 + thirdSourceLocation % 12 + 12] = " ";
                            gameCols[23 - thirdSourceLocation % 12]--;
                        } else {
                            for (int i = 0; i < backupGameCols.length; i++)
                                gameCols[i] = backupGameCols[i];
                            return false;
                        }
                    }
                }
            }

            if (fourthDestinationLocation != fourthSourceLocation && fourthDestinationLocation != 200) {
                if (fourthDestinationLocation != fourthSourceLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 3) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 3)) && !checkEaten[3])) {
                    if (fourthSourceLocation / 12 < 5) {
                        if (gameCols[fourthSourceLocation % 12] > 0) {
                            if (gameCols[fourthSourceLocation % 12] < 6)
                                board[gameCols[fourthSourceLocation % 12] * 12 + fourthSourceLocation % 12 - 12] = " ";
                            gameCols[fourthSourceLocation % 12]--;
                        } else {
                            for (int i = 0; i < backupGameCols.length; i++)
                                gameCols[i] = backupGameCols[i];
                            return false;
                        }
                    } else {
                        if (gameCols[23 - fourthSourceLocation % 12] > 0) {
                            if (gameCols[23 - fourthSourceLocation % 12] < 6)
                                board[(11 - gameCols[23 - fourthSourceLocation % 12]) * 12 + fourthSourceLocation % 12 + 12] = " ";
                            gameCols[23 - fourthSourceLocation % 12]--;
                        } else {
                            for (int i = 0; i < backupGameCols.length; i++)
                                gameCols[i] = backupGameCols[i];
                            return false;
                        }
                    }
                }
            }
            if(firstSourceLocation!=firstDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 0)) && !checkEaten[0])) {
                if(firstDestinationLocation/12 < 5) {
                    if (gameCols[firstDestinationLocation%12] > 0)
                        board[firstDestinationLocation] = MARKS[currentPlayer];
                }
                else{
                    if (gameCols[23 - firstDestinationLocation%12] > 0)
                        board[firstDestinationLocation] = MARKS[currentPlayer];
                }
            }
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if (secondSourceLocation != secondDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 1)) && !checkEaten[1])) {
                    if (secondDestinationLocation / 12 < 5) {
                        if (gameCols[secondDestinationLocation % 12] > 0)
                            board[secondDestinationLocation] = MARKS[currentPlayer];
                    } else {
                        if (gameCols[23 - secondDestinationLocation % 12] > 0)
                            board[secondDestinationLocation] = MARKS[currentPlayer];
                    }
                }
            }
            if (thirdDestinationLocation != thirdSourceLocation && thirdDestinationLocation != 200) {
                if (thirdSourceLocation != thirdDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 2) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 2)) && !checkEaten[1])) {
                    if (thirdDestinationLocation / 12 < 5) {
                        if (gameCols[thirdDestinationLocation % 12] > 0)
                            board[thirdDestinationLocation] = MARKS[currentPlayer];
                    } else {
                        if (gameCols[23 - thirdDestinationLocation % 12] > 0)
                            board[thirdDestinationLocation] = MARKS[currentPlayer];
                    }
                }
            }

            if (fourthDestinationLocation != fourthSourceLocation && fourthDestinationLocation != 200) {
                if (fourthSourceLocation != fourthDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 3) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 3)) && !checkEaten[1])) {
                    if (fourthDestinationLocation / 12 < 5) {
                        if (gameCols[fourthDestinationLocation % 12] > 0)
                            board[fourthDestinationLocation] = MARKS[currentPlayer];
                    } else {
                        if (gameCols[23 - fourthDestinationLocation % 12] > 0)
                            board[fourthDestinationLocation] = MARKS[currentPlayer];
                    }
                }
            }

            if(firstSourceLocation!=firstDestinationLocation && checkEaten[0])
            {
                if(MARKS[currentPlayer].equals("Y"))
                    eatenPieces[1]++;
                else
                    eatenPieces[0]++;
                if(firstDestinationLocation/12 < 5)
                    firstDestinationLocation %= 12;
                else if(firstDestinationLocation/12 > 6) {
                    firstDestinationLocation %= 12;
                    firstDestinationLocation += 132;
                }
            }
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if (secondSourceLocation != secondDestinationLocation && checkEaten[1]) {
                    if (MARKS[currentPlayer].equals("Y"))
                        eatenPieces[1]++;
                    else
                        eatenPieces[0]++;
                    if (secondDestinationLocation / 12 < 5)
                        secondDestinationLocation %= 12;
                    else if (secondDestinationLocation / 12 > 6) {
                        secondDestinationLocation %= 12;
                        secondDestinationLocation += 132;
                    }
                }
            }
            if (thirdSourceLocation != thirdDestinationLocation && thirdDestinationLocation != 200) {
                if (thirdSourceLocation != thirdDestinationLocation && checkEaten[2]) {
                    if (MARKS[currentPlayer].equals("Y"))
                        eatenPieces[1]++;
                    else
                        eatenPieces[0]++;
                    if (thirdDestinationLocation / 12 < 5)
                        thirdDestinationLocation %= 12;
                    else if (thirdDestinationLocation / 12 > 6) {
                        thirdDestinationLocation %= 12;
                        thirdDestinationLocation += 132;
                    }
                }
            }

            if (fourthSourceLocation != fourthDestinationLocation && fourthDestinationLocation != 200) {
                if (fourthSourceLocation != fourthDestinationLocation && checkEaten[3]) {
                    if (MARKS[currentPlayer].equals("Y"))
                        eatenPieces[1]++;
                    else
                        eatenPieces[0]++;
                    if (fourthDestinationLocation / 12 < 5)
                        fourthDestinationLocation %= 12;
                    else if (fourthDestinationLocation / 12 > 6) {
                        fourthDestinationLocation %= 12;
                        fourthDestinationLocation += 132;
                    }
                }
            }

            eatenPieces[0] -= subFromEatenPieces[0];
            eatenPieces[1] -= subFromEatenPieces[1];
            subFromEatenPieces[0] = subFromEatenPieces[1] = 0;

            currentPlayer = ( currentPlayer + 1 ) % 2; // change player
            // let new current player know that move occurred


            players[ currentPlayer ].otherPlayerMovedDouble( firstSourceLocation, firstDestinationLocation,
                    secondSourceLocation, secondDestinationLocation, thirdSourceLocation,
                    thirdDestinationLocation, fourthSourceLocation, fourthDestinationLocation);

            checkEaten[0] = checkEaten[1] = checkEaten[2] = checkEaten[3] = false;

            gameLock.lock(); // lock game to signal other player to go

            try
            {
                otherPlayerTurn.signal(); // signal other player to continue
            } // end try
            finally
            {
                gameLock.unlock(); // unlock game after signaling
            } // end finally

            return true; // notify player that move was valid
        } // end if
        else // move was not valid
            if(firstSourceLocation == firstDestinationLocation && secondDestinationLocation == secondSourceLocation && thirdSourceLocation == thirdDestinationLocation && fourthSourceLocation == fourthDestinationLocation)
            {
                currentPlayer = ( currentPlayer + 1 ) % 2; // change player
                // let new current player know that move occurred
                players[ currentPlayer ].otherPlayerMovedDouble( firstSourceLocation, firstDestinationLocation,
                        secondSourceLocation, secondDestinationLocation, thirdSourceLocation,
                        thirdDestinationLocation, fourthSourceLocation, fourthDestinationLocation);

                checkEaten[0] = checkEaten[1] = checkEaten[2] = checkEaten[3] = false;

                gameLock.lock(); // lock game to signal other player to go

                try
                {
                    otherPlayerTurn.signal(); // signal other player to continue
                } // end try
                finally
                {
                    gameLock.unlock(); // unlock game after signaling
                } // end finally

                return true; // notify player that move was valid
            }
            else {
                for (int i = 0; i < backupGameCols.length; i++)
                    gameCols[i] = backupGameCols[i];
                return false; // notify player that move was invalid
            }
    } //end validateandmovedouble func
    public boolean validateAndMove( int firstSourceLocation, int firstDestinationLocation,
                                    int secondSourceLocation, int secondDestinationLocation, int player )
    {
        // while not current player, must wait for turn
        while ( player != currentPlayer )
        {
            gameLock.lock(); // lock game to wait for other player to go

            try
            {
                otherPlayerTurn.await(); // wait for player's turn
            } // end try
            catch ( InterruptedException exception )
            {
                exception.printStackTrace();
            } // end catch
            finally
            {
                gameLock.unlock(); // unlock game after waiting
            } // end finally
        } // end while

        if(firstSourceLocation == firstDestinationLocation && firstSourceLocation == 170 && secondDestinationLocation == secondSourceLocation && secondSourceLocation == 170){

            currentPlayer = ( currentPlayer + 1 ) % 2; // change player
            // let new current player know that move occurred

            players[ currentPlayer ].otherPlayerMoved( firstSourceLocation, firstDestinationLocation,
                    secondSourceLocation, secondDestinationLocation );

            gameLock.lock(); // lock game to signal other player to go

            try
            {
                otherPlayerTurn.signal(); // signal other player to continue
            } // end try
            finally
            {
                gameLock.unlock(); // unlock game after signaling
            } // end finally
        }
        int[] backupGameCols = new int[24];

        for( int i = 0; i < backupGameCols.length; i++)
            backupGameCols[i] = gameCols[i];

        // if location not occupied, make move
        if(firstDestinationLocation == firstSourceLocation) {
            if (MARKS[currentPlayer].equals("Y")) {
                if (gameCols[23 - firstDestinationLocation % 12] > 0)
                {
                    if(gameCols[23 - firstDestinationLocation % 12] < 6) {
                        board[(11 - (gameCols[23 - firstSourceLocation % 12] - 1)) * 12 + firstSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[23 - firstSourceLocation % 12]--;
            } else {
                if (gameCols[firstDestinationLocation % 12] > 0) {
                    if (gameCols[firstDestinationLocation % 12] < 6) {
                        board[(gameCols[firstSourceLocation % 12] - 1) * 12 + firstSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[firstSourceLocation % 12]--;
            }
        }
        else {
            if ((MARKS[currentPlayer].equals("Y") && eatenPieces[0] - subFromEatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] - subFromEatenPieces[1] > 0)) {
                if ((firstDestinationLocation == 144 || firstDestinationLocation == 145) && isItGonnaGetEaten(firstSourceLocation)) {
                    if (firstSourceLocation / 12 < 5) {
                        board[firstSourceLocation % 12] = MARKS[currentPlayer];
                    }
                    if (firstSourceLocation > 6) {
                        board[11 * 12 + firstSourceLocation % 12] = MARKS[currentPlayer];
                    }
                    checkEaten[0] = true;
                } else if (isItGonnaGetEaten(firstDestinationLocation)) {
                    if (firstDestinationLocation / 12 < 5) {
                        board[firstDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if (firstDestinationLocation > 6) {
                        board[11 * 12 + firstDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    checkEaten[0] = true;
                } else {
                    if (MARKS[currentPlayer].equals("B")) {
                        board[(11 - gameCols[23 - firstSourceLocation % 12]++) * 12 + firstSourceLocation % 12] = MARKS[currentPlayer];
                        subFromEatenPieces[0]++;
                    } else {
                        board[gameCols[firstSourceLocation % 12]++ * 12 + firstSourceLocation % 12] = MARKS[currentPlayer];
                        subFromEatenPieces[1]++;
                    }
                }
            } else if (isItGonnaGetEaten(firstDestinationLocation)) {
                if (firstDestinationLocation / 12 < 5) {
                    board[firstDestinationLocation % 12] = MARKS[currentPlayer];
                }
                if (firstDestinationLocation > 6) {
                    board[11 * 12 + firstDestinationLocation % 12] = MARKS[currentPlayer];
                }
                if(firstSourceLocation/12 < 5) {
                    gameCols[firstSourceLocation % 12]--;
                }
                else{
                    gameCols[23 - firstSourceLocation % 12]--;
                }
                checkEaten[0] = true;
            }
        }

        if(secondDestinationLocation == secondSourceLocation && secondDestinationLocation != 200) {
            if (MARKS[currentPlayer].equals("Y")) {
                if (gameCols[23 - secondDestinationLocation % 12] > 0)
                {
                    if(gameCols[23 - secondDestinationLocation % 12] < 6) {
                        board[(11 - (gameCols[23 - secondSourceLocation % 12] - 1)) * 12 + secondSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[23 - secondSourceLocation % 12]--;
            } else {
                if (gameCols[secondDestinationLocation % 12] > 0) {
                    if (gameCols[secondDestinationLocation % 12] < 6) {
                        board[(gameCols[secondSourceLocation % 12] - 1) * 12 + secondSourceLocation % 12] = " ";
                    }
                }
                else
                {
                    for( int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
                gameCols[secondSourceLocation % 12]--;
            }
        }
        else {
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if ((MARKS[currentPlayer].equals("Y") && eatenPieces[0] - subFromEatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] - subFromEatenPieces[1] > 0)) {
                    if ((secondDestinationLocation == 145 || secondDestinationLocation == 144) && isItGonnaGetEaten(secondSourceLocation)) {
                        if (secondSourceLocation / 12 < 5) {
                            board[secondSourceLocation % 12] = MARKS[currentPlayer];
                        }
                        if (secondSourceLocation > 6) {
                            board[11 * 12 + secondSourceLocation % 12] = MARKS[currentPlayer];
                        }
                        checkEaten[1] = true;
                    } else if (isItGonnaGetEaten(secondDestinationLocation)) {
                        if (secondDestinationLocation / 12 < 5) {
                            board[secondDestinationLocation % 12] = MARKS[currentPlayer];
                        }
                        if (secondDestinationLocation > 6) {
                            board[11 * 12 + secondDestinationLocation % 12] = MARKS[currentPlayer];
                        }
                        checkEaten[1] = true;
                    } else {
                        if (MARKS[currentPlayer].equals("B")) {
                            board[(11 - gameCols[23 - firstSourceLocation % 12]++) * 12 + firstSourceLocation % 12] = MARKS[currentPlayer];
                            subFromEatenPieces[0]++;
                        } else {
                            board[gameCols[firstSourceLocation % 12]++ * 12 + firstSourceLocation % 12] = MARKS[currentPlayer];
                            subFromEatenPieces[1]++;
                        }
                    }
                } else if (isItGonnaGetEaten(secondDestinationLocation)) {
                    if (secondDestinationLocation / 12 < 5) {
                        board[secondDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if (secondDestinationLocation > 6) {
                        board[11 * 12 + secondDestinationLocation % 12] = MARKS[currentPlayer];
                    }
                    if(secondSourceLocation/12 < 5) {
                        gameCols[secondSourceLocation % 12]--;
                    }
                    else{
                        gameCols[23 - secondSourceLocation % 12]--;
                    }
                    checkEaten[1] = true;
                }
            }
        }
        /*
        if(firstDestinationLocation == 144 || firstDestinationLocation == 145)
        {

        }
        if(isItGonnaGetEaten(firstDestinationLocation))
        {
            if(firstDestinationLocation/12 < 5)
            {
                board[firstDestinationLocation%12] = MARKS[currentPlayer];
            }
            if(firstDestinationLocation > 6)
            {
                board[11*12 + firstDestinationLocation%12] = MARKS[currentPlayer];
            }
            checkEaten[0] = true;
        }
        if(isItGonnaGetEaten(secondDestinationLocation))
        {
            if(secondDestinationLocation/12 < 5)
            {
                board[secondDestinationLocation%12] = MARKS[currentPlayer];
            }
            if(secondDestinationLocation > 6)
            {
                board[11*12 + secondDestinationLocation%12] = MARKS[currentPlayer];
            }
            checkEaten[1] = true;
        }
        if(isItGonnaGetEaten(thirdDestinationLocation))
        {
            if(thirdDestinationLocation/12 < 5)
            {
                board[thirdDestinationLocation%12] = MARKS[currentPlayer];
            }
            if(thirdDestinationLocation > 6)
            {
                board[11*12 + thirdDestinationLocation%12] = MARKS[currentPlayer];
            }
            checkEaten[2] = true;
        }
        if(isItGonnaGetEaten(fourthDestinationLocation))
        {
            if(fourthDestinationLocation/12 < 5)
            {
                board[fourthDestinationLocation%12] = MARKS[currentPlayer];
            }
            if(fourthDestinationLocation > 6)
            {
                board[11*12 + fourthDestinationLocation%12] = MARKS[currentPlayer];
            }
            checkEaten[3] = true;
        }
        */
        if (!(firstSourceLocation == firstDestinationLocation && secondDestinationLocation == secondSourceLocation) && checkEaten[0] || (!isOccupied( firstDestinationLocation ) && isFilledBySameColor( firstSourceLocation )))
        {
            int firstRow = firstDestinationLocation / 12;
            int firstCol = firstDestinationLocation % 12;
            int secondRow = secondDestinationLocation / 12;
            int secondCol = secondDestinationLocation % 12;

            //firstMove
            if (firstSourceLocation!=firstDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 0)) && !checkEaten[0])) {
                if (firstRow < 5) {
                    if (gameCols[firstCol] < 5) {
                        firstDestinationLocation = (gameCols[firstCol]) * 12 + firstCol;
                    }
                    gameCols[firstCol]++;
                } else if (firstRow > 6) {
                    if (gameCols[23 - firstCol] < 5) {
                        firstDestinationLocation = (11 - gameCols[23 - firstCol]) * 12 + firstCol;
                    }
                    gameCols[23 - firstCol]++;
                }
            }
            //secondMove
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if (!(firstSourceLocation == firstDestinationLocation && secondDestinationLocation == secondSourceLocation) && (MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 1) || checkEaten[1] || (!isOccupied(secondDestinationLocation) && isFilledBySameColor(secondSourceLocation))) {
                    if (secondDestinationLocation != secondSourceLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 1)) && !checkEaten[1])) {
                        if (secondRow < 5) {
                            if (gameCols[secondCol] < 5) {
                                secondDestinationLocation = (gameCols[secondCol] * 12 + secondCol);
                            }
                            if (!isItGonnaGetEaten(secondDestinationLocation))
                                gameCols[secondCol]++;
                        } else if (secondRow > 6) {
                            if (gameCols[23 - secondCol] < 5) {
                                secondDestinationLocation = (11 - gameCols[23 - secondCol]) * 12 + secondCol;
                            }
                            if (!isItGonnaGetEaten(secondDestinationLocation))
                                gameCols[23 - secondCol]++;
                        }
                    }
                } else {
                    for (int i = 0; i < backupGameCols.length; i++)
                        gameCols[i] = backupGameCols[i];
                    return false;
                }
            }

            // set moves on board
            if(firstSourceLocation!=firstDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 0)) && !checkEaten[0])) {
                if (firstSourceLocation / 12 < 5) {
                    if(gameCols[firstSourceLocation % 12] > 0) {
                        if (gameCols[firstSourceLocation % 12] < 6)
                            board[gameCols[firstSourceLocation % 12] * 12 + firstSourceLocation % 12 - 12] = " ";
                        gameCols[firstSourceLocation % 12]--;
                    }
                    else{
                        for( int i = 0; i < backupGameCols.length; i++)
                            gameCols[i] = backupGameCols[i];
                        return false;                    }
                } else {
                    if (gameCols[23 - firstSourceLocation % 12] > 0) {
                        if (gameCols[23 - firstSourceLocation % 12] < 6)
                            board[(11 - gameCols[23 - firstSourceLocation % 12]) * 12 + firstSourceLocation % 12 + 12] = " ";
                        gameCols[23 - firstSourceLocation % 12]--;
                    } else {
                        for (int i = 0; i < backupGameCols.length; i++)
                            gameCols[i] = backupGameCols[i];
                        return false;
                    }
                }
            }
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if (secondDestinationLocation != secondSourceLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 1)) && !checkEaten[1])) {
                    if (secondSourceLocation / 12 < 5) {
                        if (gameCols[secondSourceLocation % 12] > 0) {
                            if (gameCols[secondSourceLocation % 12] < 6)
                                board[gameCols[secondSourceLocation % 12] * 12 + secondSourceLocation % 12 - 12] = " ";
                            gameCols[secondSourceLocation % 12]--;
                        } else {
                            for (int i = 0; i < backupGameCols.length; i++)
                                gameCols[i] = backupGameCols[i];
                            return false;
                        }
                    } else {
                        if (gameCols[23 - secondSourceLocation % 12] > 0) {
                            if (gameCols[23 - secondSourceLocation % 12] < 6)
                                board[(11 - gameCols[23 - secondSourceLocation % 12]) * 12 + secondSourceLocation % 12 + 12] = " ";
                            gameCols[23 - secondSourceLocation % 12]--;
                        } else {
                            for (int i = 0; i < backupGameCols.length; i++)
                                gameCols[i] = backupGameCols[i];
                            return false;
                        }
                    }
                }
            }

            if(firstSourceLocation!=firstDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 0) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 0)) && !checkEaten[0])) {
                if(firstDestinationLocation/12 < 5) {
                    if (gameCols[firstDestinationLocation%12] > 0)
                        board[firstDestinationLocation] = MARKS[currentPlayer];
                }
                else{
                    if (gameCols[23 - firstDestinationLocation%12] > 0)
                        board[firstDestinationLocation] = MARKS[currentPlayer];
                }
            }
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if (secondSourceLocation != secondDestinationLocation && (!((MARKS[currentPlayer].equals("Y") && eatenPieces[0] > 1) || (MARKS[currentPlayer].equals("B") && eatenPieces[1] > 1)) && !checkEaten[1])) {
                    if (secondDestinationLocation / 12 < 5) {
                        if (gameCols[secondDestinationLocation % 12] > 0)
                            board[secondDestinationLocation] = MARKS[currentPlayer];
                    } else {
                        if (gameCols[23 - secondDestinationLocation % 12] > 0)
                            board[secondDestinationLocation] = MARKS[currentPlayer];
                    }
                }
            }

            if(firstSourceLocation!=firstDestinationLocation && checkEaten[0])
            {
                if(MARKS[currentPlayer].equals("Y"))
                    eatenPieces[1]++;
                else
                    eatenPieces[0]++;
                if(firstDestinationLocation/12 < 5)
                    firstDestinationLocation %= 12;
                else if(firstDestinationLocation/12 > 6) {
                    firstDestinationLocation %= 12;
                    firstDestinationLocation += 132;
                }
            }
            if (secondDestinationLocation != secondSourceLocation && secondDestinationLocation != 200) {
                if (secondSourceLocation != secondDestinationLocation && checkEaten[1]) {
                    if (MARKS[currentPlayer].equals("Y"))
                        eatenPieces[1]++;
                    else
                        eatenPieces[0]++;
                    if (secondDestinationLocation / 12 < 5)
                        secondDestinationLocation %= 12;
                    else if (secondDestinationLocation / 12 > 6) {
                        secondDestinationLocation %= 12;
                        secondDestinationLocation += 132;
                    }
                }
            }


            eatenPieces[0] -= subFromEatenPieces[0];
            eatenPieces[1] -= subFromEatenPieces[1];
            subFromEatenPieces[0] = subFromEatenPieces[1] = 0;

            currentPlayer = ( currentPlayer + 1 ) % 2; // change player
            // let new current player know that move occurred


            players[ currentPlayer ].otherPlayerMoved( firstSourceLocation, firstDestinationLocation,
                    secondSourceLocation, secondDestinationLocation );

            checkEaten[0] = checkEaten[1];

            gameLock.lock(); // lock game to signal other player to go

            try
            {
                otherPlayerTurn.signal(); // signal other player to continue
            } // end try
            finally
            {
                gameLock.unlock(); // unlock game after signaling
            } // end finally

            return true; // notify player that move was valid
        } // end if
        else // move was not valid
            if(firstSourceLocation == firstDestinationLocation && secondDestinationLocation == secondSourceLocation )
            {
                currentPlayer = ( currentPlayer + 1 ) % 2; // change player
                // let new current player know that move occurred
                players[ currentPlayer ].otherPlayerMoved( firstSourceLocation, firstDestinationLocation,
                        secondSourceLocation, secondDestinationLocation);

                checkEaten[0] = checkEaten[1] = false;

                gameLock.lock(); // lock game to signal other player to go

                try
                {
                    otherPlayerTurn.signal(); // signal other player to continue
                } // end try
                finally
                {
                    gameLock.unlock(); // unlock game after signaling
                } // end finally

                return true; // notify player that move was valid
            }
            else {
                for (int i = 0; i < backupGameCols.length; i++)
                    gameCols[i] = backupGameCols[i];
                return false; // notify player that move was invalid
            }
    } // end method validateAndMove

    // determine whether location is occupied
    public boolean isOccupied( int location )
    {
        int row = location / 12; // 8
        int col = location % 12; // 4
        if(location == 145 || location == 144)
            return false;
        if ( !board[ location ].equals( MARKS[currentPlayer]) && !board[ location ].equals(" ")) {
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
    // place code in this method to determine whether game over
    public int isGameOver(){
        // return -1 for Y win, 0 if game isn't over and 1 if B won
        int BCheck = 0, YCheck = 0;
        for(int i = 0; i < 12; i++){
            if(gameCols[i] > 0){
                if(board[i].equals("Y")){
                    YCheck = 1;
                }
                else{
                    BCheck = 1;
                }
            }
            if(gameCols[23 - i] > 0){
                if(board[11*12 + i].equals("Y")){
                    YCheck = 1;
                }
                else{
                    BCheck = 1;
                }
            }
            if(BCheck == 1 && YCheck == 1){//game not over
                return 0;
            }
        }
        if(BCheck == 1 && YCheck == 0)
        {
            if(eatenPieces[0]==0)
                return -1;//Y won
            else{
                if(eatenPieces[1]==0 && YCheck == 1 && BCheck == 0)
                    return 1;//B won
            }
        }
        else if(YCheck == 1 && BCheck == 0) {
            if(eatenPieces[1]==0)
                return 1;//B won
            else {
                if (eatenPieces[0]==0&&BCheck == 1 && YCheck == 0)
                    return -1;//Y won
            }
        }
        return 0;
    }

    // private inner class Player manages each Player as a runnable
    private class Player implements Runnable
    {
        private Socket connection; // connection to client
        private Scanner input; // input from client
        private Formatter output; // output to client
        private int playerNumber; // tracks which player this is
        private String mark; // mark for this player
        private boolean suspended = true; // whether thread is suspended

        // set up Player thread
        public Player( Socket socket, int number )
        {
            playerNumber = number; // store this player's number
            mark = MARKS[ playerNumber ]; // specify player's mark
            connection = socket; // store socket for client

            try // obtain streams from Socket
            {
                input = new Scanner( connection.getInputStream() );
                output = new Formatter( connection.getOutputStream() );
            } // end try
            catch ( IOException ioException )
            {
                ioException.printStackTrace();
                System.exit( 1 );
            } // end catch
        } // end Player constructor

        public void deadlockNextPlayer(){
            output.format( "Opponent moved\n" );
            output.format( "%d,%d,%d,%d\n", 170, 170, 170, 170 ); // send location of move
            output.flush();
            try
            {
                otherPlayerTurn.signal(); // signal other player to continue
            } // end try
            finally
            {
                gameLock.unlock(); // unlock game after signaling
            } // end finally
        }
        // send message that other player moved
        public void otherPlayerMoved( int firstSrc, int firstDst, int secondSrc, int secondDst)
        {
            output.format( "Opponent moved\n" );
            output.format( "%d,%d,%d,%d\n", firstSrc, firstDst, secondSrc, secondDst ); // send location of move
            output.flush(); // flush output
        } // end method otherPlayerMoved
        public void otherPlayerMovedDouble( int firstSrc, int firstDst, int secondSrc, int secondDst,
                                            int thirdSrc, int thirdDst, int fourthSrc, int fourthDst)
        {
            output.format( "Opponent moved\n" );
            output.format( "%d,%d,%d,%d,%d,%d,%d,%d\n", firstSrc, firstDst, secondSrc, secondDst,
                    thirdSrc, thirdDst, fourthSrc, fourthDst); // send location of move
            output.flush(); // flush output
        } // end method otherPlayerMoved

        // control thread's execution
        public void run()
        {
            // send client its mark (Y or B), process messages from client
            try
            {
                displayMessage( "Player " + mark + " connected\n" );
                output.format( "%s\n", mark ); // send player's mark
                output.flush(); // flush output

                // if player Y, wait for another player to arrive
                if ( playerNumber == PLAYER_Y)
                {
                    output.format( "%s\n%s", "Player Y connected",
                            "Waiting for another player\n" );
                    output.flush(); // flush output

                    gameLock.lock(); // lock game to  wait for second player

                    try
                    {
                        while( suspended )
                        {
                            otherPlayerConnected.await(); // wait for player B
                        } // end while
                    } // end try
                    catch ( InterruptedException exception )
                    {
                        exception.printStackTrace();
                    } // end catch
                    finally
                    {
                        gameLock.unlock(); // unlock game after second player
                    } // end finally

                    // send message that other player connected
                    output.format( "Other player connected. Your move.\n" );
                    output.flush(); // flush output
                } // end if
                else
                {
                    output.format( "Player B connected, please wait\n" );
                    output.flush(); // flush output
                } // end else



                // while game not over
                while ( isGameOver() == 0 )
                {
                    int location = 0; // initialize move location
                    String[] loc;
                    String inp;
                    if ( input.hasNext() ) {
                        inp = input.next();
                        if(inp.equals("170,170,170,170")) {
                            validateAndMove(170, 170, 170, 170, playerNumber);
                            output.format("Valid move.\n"); // notify client
                            output.flush(); // flush output
                        }
                        else {
                            String brd = "\n";
                            for (int i = 0; i < 12; i++){
                                for (int j = 0; j < 12; j++){
                                    if(board[i*12 + j]!=" ")
                                        brd += board[i*12 + j];
                                    else{
                                        brd += "_";
                                    }
                                }
                                brd += "\n";
                            }
                            displayMessage(brd);
                            loc = inp.split(",", -1); // get move location
                            // check for valid move
                            if (loc.length == 4) {
                                if (validateAndMove(Integer.parseInt(loc[0]), Integer.parseInt(loc[1]),
                                        Integer.parseInt(loc[2]), Integer.parseInt(loc[3]), playerNumber)) {

                                    output.format("Valid move.\n"); // notify client
                                    output.flush(); // flush output
                                } // end if
                                else // move was invalid
                                {
                                    subFromEatenPieces[0] = subFromEatenPieces[1] = 0;
                                    checkEaten[0] = checkEaten[1] = checkEaten[2] = checkEaten[3] = false;
                                    output.format("Invalid move, try again\n");
                                    output.flush(); // flush output
                                } // end else
                            } else if (loc.length == 8) {
                                if (validateAndMoveDouble(Integer.parseInt(loc[0]), Integer.parseInt(loc[1]),
                                        Integer.parseInt(loc[2]), Integer.parseInt(loc[3]),
                                        Integer.parseInt(loc[4]), Integer.parseInt(loc[5]),
                                        Integer.parseInt(loc[6]), Integer.parseInt(loc[7]), playerNumber)) {

                                    output.format("Valid move.\n"); // notify client
                                    output.flush(); // flush output
                                } // end if
                                else // move was invalid
                                {
                                    subFromEatenPieces[0] = subFromEatenPieces[1] = 0;
                                    checkEaten[0] = checkEaten[1] = checkEaten[2] = checkEaten[3] = false;
                                    output.format("Invalid move, try again\n");
                                    output.flush(); // flush output
                                } // end else
                            } else // move was invalid
                            {
                                subFromEatenPieces[0] = subFromEatenPieces[1] = 0;
                                checkEaten[0] = checkEaten[1] = checkEaten[2] = checkEaten[3] = false;
                                output.format("Invalid move, try again\n");
                                output.flush(); // flush output
                            } // end else
                        }
                    }
                } // end while
            } // end try
            finally
            {
                displayMessage("GameWon!");
                try
                {
                    connection.close(); // close connection to client
                } // end try
                catch ( IOException ioException )
                {
                    ioException.printStackTrace();
                    System.exit( 1 );
                } // end catch
                System.exit(0);
            } // end finally
        } // end method run

        // set whether or not thread is suspended
        public void setSuspended( boolean status )
        {
            suspended = status; // set value of suspended
        } // end method setSuspended
    } // end class Player
} // end class BackgammonServer



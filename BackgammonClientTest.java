//Fig. 24.16: BackgammonClientTest.java
//Tests the BackgammonClient class.
import javax.swing.JFrame;
        import java.util.Scanner;

public class BackgammonClientTest
{
    public static void main( String args[] )
    {
        Scanner sc = new Scanner(System.in);  // Create a Scanner object
        Thread executionThread = new Thread(){
            public void run(){
                BackgammonClient application;
                System.out.println("Enter port number: ");
                application = new BackgammonClient("127.0.0.1", sc.nextInt()); // localhost
                application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            }
        };
        executionThread.start();

    } // end main
} // end class BackgammonClientTest

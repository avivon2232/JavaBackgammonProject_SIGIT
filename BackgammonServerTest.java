//Fig. 24.14: BackgammonServerTest.java
//Tests the BackgammonServer.
import javax.swing.JFrame;
import java.util.*;
public class BackgammonServerTest
{
    private static int count = 0;//helps to run the threads
    public static List<BackgammonServer> servers = new ArrayList<BackgammonServer>();
    public static Thread[] threads;
    public static void main( String args[] )
    {
        int serverCount = 2;//the amount of servers you're intending to start up
        threads = new Thread[serverCount];
        for (int i = 0; i < serverCount; i++) {
            servers.add(new BackgammonServer("127.0.0.1", (12345 + i)));
            servers.get(i).setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
            System.out.println(i);
        }
        for(int i = 0; i < serverCount; i++){
            threads[i] = new Thread(){public void run(){
                servers.get(count++).execute();
            }};
            threads[i].start();
        }
    } // end main
} // end class BackgammonServerTest


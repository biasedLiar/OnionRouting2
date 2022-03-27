import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.util.*;

public class OnionClient  extends OnionEndPoint{
    private Scanner in;
    private String serverSocketString;

    public OnionClient(int port, String serverSocketString, ArrayList<String> nodePorts) throws SocketException, UnknownHostException {
        super(port, nodePorts);
        this.serverSocketString = serverSocketString;
        in = new Scanner(System.in);
    }




    public void run() {
        try {
            //Gets keys from nodes
            keyEchange();
            System.out.println("Client finished sharing keys\n\nSending test message to echo server.");

            //Sets settings for test message
            msg = "Success!";
            boolean running = true;
            targetSocketString = serverSocketString;
            while (running){
                //Encrypts message
                wrapMessage();
                //Sends message
                sendMessage();
                //recieves response
                recieveMessageUpdatePort();
                System.out.println("Response: \n" + new String(msgBytes));

                //Lets user choose what to do
                System.out.println("\nSelect option\n1: Echo server\n2: Web server\n3: Terminate");
                msg = in.nextLine();
                if (msg.equals("1")){
                    //Echo server
                    System.out.println("Echo server chosen.\nEnter input then press Enter.");
                    msg = in.nextLine();
                    targetSocketString = serverSocketString;
                    mode = MessageMode.FORWARD_ON_NETWORK;
                } else if (msg.equals("2")){
                    //Web server
                    System.out.println("Web server chosen.\nEnter url then press Enter.");
                    msg = in.nextLine();
                    targetSocketString = serverSocketString;
                    mode = MessageMode.FORWARD_TO_WEB;
                } else {
                    //Exits client
                    System.out.println("Exiting...");
                    running = false;
                }
            }

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //closes socket
        close();
    }

    public void close() {
        socket.close();
    }
}

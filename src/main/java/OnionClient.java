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
            keyEchange();
            System.out.println("Client finished sharing keys\n\nSending test message to echo server.");

            msg = "Success!";
            boolean running = true;
            targetSocketString = serverSocketString;
            while (running){
                wrapMessage();
                sendMessage();

                recieveMessageUpdatePort();
                System.out.println("Response: \n" + new String(msgBytes));

                System.out.println("\nSelect option\n1: Echo server\n2: Web server\n3: Terminate");
                msg = in.nextLine();
                if (msg.equals("1")){
                    //
                    System.out.println("Echo server chosen.\nEnter input then press Enter.");
                    msg = in.nextLine();
                    targetSocketString = serverSocketString;
                    mode = MessageMode.FORWARD_ON_NETWORK;
                } else if (msg.equals("2")){
                    System.out.println("Web server chosen.\nEnter url then press Enter.");
                    msg = in.nextLine();
                    targetSocketString = serverSocketString;
                    mode = MessageMode.FORWARD_TO_WEB;
                } else {
                    System.out.println("Exiting...");
                    running = false;
                }
            }

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        socket.close();
    }
}

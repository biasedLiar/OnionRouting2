import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class OnionServer extends OnionEndPoint{
    private boolean running;

    public OnionServer(int port, ArrayList<String> nodePorts) throws SocketException {
        super(port, nodePorts);
    }

    public void add_x_of_y_toFront(int x, int y){
        //Sets message part number and total number of messages parts to the front of the message
        //used when message is split into mulitple smaller messages
        byte[] myBytes= new byte[2];
        myBytes[0] = (byte) x;
        myBytes[1] = (byte) y;
        addToFrontOfMessage(myBytes);
    }

    public void run(){
        try {
            //Exchanges keys with nodes
            keyEchange();
            System.out.println("Server finished sharing keys");
            running = true;

            while (running){
                //Recieves message from client
                recieveMessageUpdatePort();
                System.out.println("Server recieved message, sending answer.");
                //Checks if message is to be sent to the web.
                if (mode == MessageMode.FORWARD_TO_WEB){
                    //SEnds the request
                    URL url = new URL(msg);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    con.disconnect();
                    //Sets the response to message
                    msg = content.toString();
                    msgBytes = msg.getBytes();
                    //if the message is to long, it must be split
                    if (msgBytes.length >1000){
                        //Figures out how many pieces the response is to be divided into
                        int numMessages = msgBytes.length/1000 + 1;
                        byte[] msgStorage = msgBytes;
                        //We must save the what socket the messages are to be sent to
                        String ogSocketString = targetSocketString;

                        for (int i = 0; i < numMessages - 1; i++) {
                            //Sets what part of the response is to be included in this message
                            msgBytes = Arrays.copyOfRange(msgStorage, i*1000, (i+1)*1000);
                            //Sets the mode to splitResponse
                            mode = MessageMode.SPLIT_RESPONSE;
                            //Adds message number and totalNumMessages to the message
                            add_x_of_y_toFront(i, numMessages);
                            msg = new String(msgBytes);
                            //Encrypts the message
                            wrapMessage();
                            //sends the message
                            sendMessage();
                            //Ensures that the next message is sent to the client correctly
                            targetSocketString = ogSocketString;
                        }
                        //Sets the final bytes of the response to the message
                        msgBytes = Arrays.copyOfRange(msgStorage, (numMessages - 1)*1000, msgStorage.length);
                        //Sets mode
                        mode = MessageMode.SPLIT_RESPONSE;
                        //Adds message number and totalNumMessages to the message
                        add_x_of_y_toFront(numMessages-1, numMessages);
                        msg = new String(msgBytes);
                        //Encrypts the message
                        wrapMessage();
                        //sends the message
                        sendMessage();
                    } else {
                        //If the message is short enough, it can be sent normally
                        //Encrypts the message
                        wrapMessage();
                        //sends the message
                        sendMessage();
                    }
                } else {
                    //Otherwise, the server simply sends the message back to the client
                    //Encrypts the message
                    wrapMessage();
                    //sends the message
                    sendMessage();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


        socket.close();
    }
}

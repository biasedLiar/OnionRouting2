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
        byte[] myBytes= new byte[2];
        myBytes[0] = (byte) x;
        myBytes[1] = (byte) y;
        addToFrontOfMessage(myBytes);
    }

    public void run(){
        try {
            keyEchange();
            System.out.println("Server finished sharing keys");
            running = true;

            while (running){
                recieveMessageUpdatePort();
                //System.out.println("Server her, message recieved: \n" + msg);
                System.out.println("Server recieved message, sending answer.");
                if (mode == MessageMode.FORWARD_TO_WEB){

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

                    msg = content.toString();
                    msgBytes = msg.getBytes();
                    //msgBytes = Arrays.copyOfRange(msgBytes, 0, 1200);
                    if (msgBytes.length >1000){
                        int numMessages = msgBytes.length/1000 + 1;
                        byte[]msgStorage = msgBytes;

                        String ogSocketString = targetSocketString;

                        for (int i = 0; i < numMessages - 1; i++) {
                            msgBytes = Arrays.copyOfRange(msgStorage, i*1000, (i+1)*1000);
                            mode = MessageMode.SPLIT_RESPONSE;
                            add_x_of_y_toFront(i, numMessages);
                            msg = new String(msgBytes);
                            //addToFrontOfMessage(new byte[]{(byte) i, (byte) numMessages});
                            wrapMessage();
                            sendMessage();
                            targetSocketString = ogSocketString;
                        }
                        msgBytes = Arrays.copyOfRange(msgStorage, (numMessages - 1)*1000, msgStorage.length);
                        mode = MessageMode.SPLIT_RESPONSE;
                        add_x_of_y_toFront(numMessages-1, numMessages);
                        msg = new String(msgBytes);
                        //addToFrontOfMessage(new byte[]{(byte) (numMessages - 1), (byte) numMessages});
                        wrapMessage();
                        sendMessage();
                    } else {
                        wrapMessage();
                        sendMessage();
                    }
                    msg = new String(msgBytes);
                    /*
                    String s1 = "";
                    for (String s :
                            msg.split("")) {
                        s1 += s;
                    }
                    msg = s1;
                    mode = MessageMode.FORWARD_ON_NETWORK;

                     */
                } else {
                    wrapMessage();
                    sendMessage();
                }
                //addModeToMessage(MessageMode.FORWARD_ON_NETWORK);
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

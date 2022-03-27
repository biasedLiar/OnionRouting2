import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public abstract class OnionParent extends Thread{
    protected DatagramSocket socket;
    protected InetAddress address;
    protected InetAddress myAddress;
    protected String msg;
    protected byte[] msgBytes;
    protected Cipher rsaCipher;
    protected Cipher aesCipher;
    protected byte[] buf;
    protected byte[] buf2;
    protected int port;
    protected int myPort;
    protected String targetSocketString;
    protected MessageMode mode;

    public OnionParent(int port){
        mode = MessageMode.FORWARD_ON_NETWORK;
        buf = new byte[2048];
        buf2 = new byte[2048];
        msgBytes = new byte[244];
        this.port = port;
        myPort = port;
        targetSocketString = "1250 127.0.0.1";
        try {
            myAddress = InetAddress.getByName("127.0.0.1");
            socket = new DatagramSocket(port);
            address = InetAddress.getByName("127.0.0.1");
            rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//https://howtodoinjava.com/java/java-security/java-aes-encryption-example/
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void recieveMessage() throws IOException {
        DatagramPacket packet = new DatagramPacket(buf2, buf2.length);
        socket.receive(packet);
        msgBytes = packet.getData();

        msgBytes = Arrays.copyOfRange(msgBytes, 0, packet.getLength());
        mode = MessageMode.valueOf(msgBytes[0]);
        msgBytes = Arrays.copyOfRange(msgBytes, 1, msgBytes.length);
        msg = new String(msgBytes);
        //unless specified otherwise, the response will be sent back;

        address = packet.getAddress();
        port = packet.getPort();
        setSocketString();

        //System.out.println("Node recieved message");
    }

    public void sendMessage() throws IOException {
        setTargetFromSocketString();
        buf = msgBytes;
        System.out.println("Message sent from " + myPort + " to " + port);
        //System.out.println("MEssage is " + msgBytes.length + " at clientside.");
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);

    }

    public int getPort(String socketString){
        return Integer.parseInt(socketString.split(" ")[0]);
    }

    public String getAddress(String socketString){
        return socketString.split(" ")[1];
    }
    public void setTargetFromSocketString(){
        port = getPort(targetSocketString);
        try {
            address = InetAddress.getByName(getAddress(targetSocketString));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void setSocketString(){
        targetSocketString = port + " " + address.getHostAddress();
    }

    public int byteToInt(byte b){
        int n = b;
        if (n<0){
            n+=256;
        }
        return n;
    }

    public void addModeToMessage(MessageMode mode){
        byte[] modeByte = {mode.getValue()};
        addToFrontOfMessage(modeByte);
    }

    public void addToFrontOfMessage(byte[] newBytes){
        byte[] tempBytes = new byte[newBytes.length + msgBytes.length];
        for (int i = 0; i < newBytes.length; i++) {
            tempBytes[i] = newBytes[i];
        }
        for (int i = 0; i < msgBytes.length; i++) {
            tempBytes[i + newBytes.length] = msgBytes[i];
        }
        msgBytes = tempBytes;
    }


    public void calculatePort(){
        port = byteToInt(msgBytes[0])*256 + byteToInt(msgBytes[1]);
        String addressString = "";
        addressString += byteToInt(msgBytes[2]);
        for (int i = 0; i < 3; i++) {
            addressString += "." +  byteToInt(msgBytes[3+i]);
        }
        try {
            address = InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        setSocketString();
        msgBytes = Arrays.copyOfRange(msgBytes, 6, msgBytes.length);

        msg = new String(msgBytes);
    }



}

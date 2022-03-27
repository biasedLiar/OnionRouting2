import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

public class OnionNode extends OnionParent{
    private KeyPair pair;

    public OnionNode(int port) throws SocketException {
        super(port);
        createKeys();
    }

    public void createKeys(){
        //Source: https://www.tutorialspoint.com/java_cryptography/java_cryptography_quick_guide.htm

        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyPairGen.initialize(2048);
        pair = keyPairGen.generateKeyPair();
    }

    public void handleData() throws UnknownHostException, NoSuchAlgorithmException {



        //System.out.println("encrypted message: " + encryptedMsg + "\nEnd encrypted");
        if (mode == MessageMode.KEY_EXCHANGE){
            //Exchange keys
            PublicKey publicKey = pair.getPublic();
            String modulus = String.valueOf(((RSAPublicKey) publicKey).getModulus());
            String exponent = String.valueOf(((RSAPublicKey) publicKey).getPublicExponent());

            msg = modulus + "\n" + exponent;
            msgBytes = msg.getBytes();
            addModeToMessage(MessageMode.KEY_EXCHANGE);

            //System.out.println("client:\nModulus: " +  String.valueOf(modulus) + "\nExponent: " +  String.valueOf(exponent));



        } else if (mode == MessageMode.FORWARD_ON_NETWORK){
            //Forward message
            //address = InetAddress.getByName(splitMessage[1]);
            //System.out.println("Starting decrypting");
            //msgBytes = Arrays.copyOfRange(msgBytes, 1, msgBytes.length);
            //System.out.println(msgBytes.length);
            decryptData(msgBytes);
            //System.out.println("The message is: \n" + new String(msgBytes) + "\nEnd off message.");
        } else{

        }

    }



    public void decryptData(byte[] encryptedBytes){
        try {
            byte[] rsaEncodedAesKey = Arrays.copyOfRange(msgBytes, 0, 256);
            msgBytes = Arrays.copyOfRange(msgBytes, 256, msgBytes.length);

            rsaCipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
            byte[] aesBytes = rsaCipher.doFinal(rsaEncodedAesKey);

            //https://www.baeldung.com/java-secret-key-to-string
            SecretKey secretKey = new SecretKeySpec(aesBytes, 0, aesBytes.length, "AES");
            aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
            msgBytes = aesCipher.doFinal(msgBytes);
            if (mode == MessageMode.FORWARD_ON_NETWORK){

                calculatePort();
            } else if(mode == MessageMode.FORWARD_TO_WEB){
                System.out.println("TODO: implement this function");
            }
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

    }

    public void run(){
        try {

            while (true){
                recieveMessage();

                handleData();
                //System.out.println("Now sending message from node");
                sendMessage();


            }

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }



        socket.close();
    }
}

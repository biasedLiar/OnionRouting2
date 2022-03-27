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
        //Create keypair upon creation
        createKeys();
    }

    public void createKeys(){
        //Creates keypair
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
        //Decides how and to who the response should be
        if (mode == MessageMode.KEY_EXCHANGE){
            //Exchange keys
            PublicKey publicKey = pair.getPublic();
            //Turns the public key into a byte[]
            String modulus = String.valueOf(((RSAPublicKey) publicKey).getModulus());
            String exponent = String.valueOf(((RSAPublicKey) publicKey).getPublicExponent());
            msg = modulus + "\n" + exponent;
            msgBytes = msg.getBytes();
            //Selects flag
            addModeToMessage(MessageMode.KEY_EXCHANGE);

        } else{
            //Otherwise, the node decodes and forwards the message
            decryptData(msgBytes);
        }

    }



    public void decryptData(byte[] encryptedBytes){
        //decodes message that is to be forwarded
        try {
            //Uses private RSA key to decode AES key
            byte[] rsaEncodedAesKey = Arrays.copyOfRange(msgBytes, 0, 256);
            msgBytes = Arrays.copyOfRange(msgBytes, 256, msgBytes.length);
            rsaCipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
            byte[] aesBytes = rsaCipher.doFinal(rsaEncodedAesKey);
            //https://www.baeldung.com/java-secret-key-to-string
            SecretKey secretKey = new SecretKeySpec(aesBytes, 0, aesBytes.length, "AES");
            //Uses AES key to decode rest of message
            aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
            msgBytes = aesCipher.doFinal(msgBytes);
            //Calculates where to forward the message to
            calculateSocketString();

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

    }

    public void run(){
        //Eventloop of program
        try {

            while (true){
                //recieves the message
                recieveMessage();
                //handles the message
                handleData();
                //forwards/replies to the message
                sendMessage();
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        socket.close();
    }
}

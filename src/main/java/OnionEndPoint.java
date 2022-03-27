import javax.crypto.*;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public abstract class OnionEndPoint extends OnionParent{
    protected HashMap<String, PublicKey> keys;
    private ArrayList<String> socketStrings;
    protected ArrayList<byte[]> waitingMessages;
    protected HashMap<Integer, byte[]> splitMsgBytes;
    public static int ONION_LAYERS = 3;



    public OnionEndPoint(int port, ArrayList<String> originalSocketStrings) {
        super(port);
        socketStrings = new ArrayList<>();
        //Creates a copy of the arraylist
        //Not creating a copy would result in problems if
        //the client randomizes the list while the server is iterating through it.
        for (String socketString :
                originalSocketStrings) {
            socketStrings.add(socketString);
        }
        keys = new HashMap<>();
        splitMsgBytes = new HashMap<>();
        waitingMessages = new ArrayList<>();
    }



    public void recieveEncryption() throws IOException {
        //Creates request for public key
        msgBytes = new byte[1];
        msgBytes[0] = MessageMode.KEY_EXCHANGE.getValue();
        //Sends request
        sendMessage();
        boolean waiting = true;
        //Waits in loop for the response to the request.
        //This prevents the server from recieving a request from server as a response from the node.
        while (waiting){
            //recieves message
            recieveMessage();
            if (mode != MessageMode.KEY_EXCHANGE){
                //If the message is not the message where waiting for, we put it in a queue
                waitingMessages.add(msgBytes);
            } else {
                //Otherwise, we stop waiting
                waiting = false;
                msg = new String(msgBytes);
            }
        }
    }

    public void recieveMessageUpdatePort() throws IOException {
        //We first check that there are no requests in the queue
        if (waitingMessages.size() == 0){
            //If not, we prepare to recieve request.
            splitMsgBytes = new HashMap<>();
            //Do while loop lets normal request pass through while split messages wait for all parts to arrive
            do {
                recieveMessage();
                calculateSocketString();
                if (mode == MessageMode.SPLIT_RESPONSE){
                    splitMsgBytes.put(byteToInt(msgBytes[0]), Arrays.copyOfRange(msgBytes, 2, msgBytes.length));
                    if (splitMsgBytes.size()== byteToInt(msgBytes[1])){
                        //If all parts have arrived, we combine the messages and exit loop
                        combineSplitMessage();
                    }
                }
            }while (mode == MessageMode.SPLIT_RESPONSE);
        } else {
            //If there are messages in the queue, we load one of them as if we just recieved it.
            msgBytes = waitingMessages.remove(0);
            msg = new String(msgBytes);
        }

    }
    public void combineSplitMessage(){
        //All the parts of the message has arrived, so we put it together in order.
        msgBytes = new byte[0];
        //Since we already have an function for adding to the front of array,
        //we start loading from the back and move forward
        for (int i = splitMsgBytes.size() - 1; i >= 0; i--) {
            addToFrontOfMessage(splitMsgBytes.get(i));
        }
        msg = new String(msgBytes);
        //Setting the mode ensures we exit loop.
        mode = MessageMode.FORWARD_ON_NETWORK;
    }
    
    public void singleKeyExchange(String socketString) throws IOException, NoSuchAlgorithmException {
        //Recieves and copies public key with a single node
        //Selects and requests public key from a node
        targetSocketString = socketString;
        recieveEncryption();

        //Extracts the info needed to make the key from message
        //https://attacomsian.com/blog/java-split-string-new-line
        String[] splitMessage = msg.split("\n");
        BigInteger modulus = new BigInteger(splitMessage[0]);
        BigInteger exponent = new BigInteger(splitMessage[1]);

        //Uses the information to create copy of public key.
        //source: https://stackoverflow.com/questions/2023549/creating-rsa-keys-from-known-parameters-in-java
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        try {
            PublicKey pub = factory.generatePublic(spec);
            keys.put(targetSocketString, pub);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public void keyEchange() throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //For each node, we ask for and recieve public key.
        for (String socketString :
                socketStrings) {
            singleKeyExchange(socketString);
        }
    }

    public ArrayList<String> getRandomSockets(int numSockets){
        //Randomizes and returns the nodes that will be routed through.
        Collections.shuffle(socketStrings);
        if (socketStrings.size() < numSockets){
            return socketStrings;
        } else {
            return new ArrayList<>(socketStrings.subList(0, numSockets));
        }
    }



    public void addTargetSocketToMessage(){
        //Adds the current target socket to front of message
        addSocketToMessage(targetSocketString);
    }

    public void addSourceSocketToMessage(){
        //Adds our socket to front of message
        addSocketToMessage(myPort + " " + myAddress.getHostAddress());
    }

    public void addSocketToMessage(String socketString){
        //Adds a socket to front of message
        byte[] socketBytes = new byte[6];
        socketBytes[0] = (byte) Math.floor(getPort(socketString)/256);
        socketBytes[1] = (byte) (getPort(socketString) % 256);
        String[] myStrings = getAddress(socketString).split("\\.");
        for (int i = 0; i < myStrings.length; i++) {
            socketBytes[2+i] = Byte.valueOf(myStrings[i]);
        }
        addToFrontOfMessage(socketBytes);
    }



    public void encryptMessage(String nodeSocket, MessageMode mode){
        //Adds a layer of encryption for a given node
        try {
            //This is the address the node will send the message on to.
            addTargetSocketToMessage();

            //Creates an AES symmetric key that will be used to encrypt the payload.
            //https://stackoverflow.com/questions/18228579/how-to-create-a-secure-random-aes-key-in-java
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();

            //Encrypts the payload with AES key
            encryptWithAES(secretKey);
            //Encrypts the AES KEY with the node's public key
            encryptAesKeyWithRsa(secretKey, nodeSocket);
            //Adds the given flag to front of message
            addModeToMessage(mode);


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void encryptWithAES(SecretKey secretKey){
        //Encrypts the message with AES
        //https://howtodoinjava.com/java/java-security/java-aes-encryption-example/
        try {
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            msgBytes = aesCipher.doFinal(msgBytes);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

    }



    public void encryptAesKeyWithRsa(SecretKey secretKey, String socketString){
        //Encrypts the AES key with the node's RSA public key.
        byte[] aesBytes = secretKey.getEncoded();

        try {
            //encrypts the key
            rsaCipher.init(Cipher.ENCRYPT_MODE, keys.get(socketString));
            rsaCipher.update(aesBytes);
            aesBytes = rsaCipher.doFinal();

            //adds encrypted key to front of message
            addToFrontOfMessage(aesBytes);

        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
    }

    public void wrapMessage(){
        //Selects route, then adds a layer of encryption for node on the route.
        //Makes sure the correct message is loaded
        msgBytes = msg.getBytes();
        //Lets the recipient know what address to send the response to.
        addSourceSocketToMessage();
        //Sets the flag that the recipient will see
        addModeToMessage(mode);

        //Randomly selects nodes to route through
        ArrayList<String> onionSockets = getRandomSockets(ONION_LAYERS);
        for (int i = 0; i < onionSockets.size(); i++) {
            //for each node, we encrypt the message
            encryptMessage(onionSockets.get(i), MessageMode.FORWARD_ON_NETWORK);
            //We can now act as if this message is going to that node
            targetSocketString = onionSockets.get(i);
        }
    }








}

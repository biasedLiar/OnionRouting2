import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class OnionMain {
    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        try {
            ArrayList<String> nodePorts = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                new OnionNode(1251 + i).start();
                nodePorts.add(1251+i + " 127.0.0.1");
            }
            new OnionServer(1250, nodePorts).start();
            new OnionClient(8081, 1250 + " 127.0.0.1", nodePorts).start();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}

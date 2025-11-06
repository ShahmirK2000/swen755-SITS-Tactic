import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CentralController extends Thread {
    private final int port;
    private final Map<String, Long> heartbeats = new HashMap<>();

    public CentralController(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Central controller started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try (Scanner scanner = new Scanner(socket.getInputStream())) {
            String address = socket.getInetAddress().toString();
            System.out.println("Connection made: " + address);

            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();

                if(message.startsWith("PULSE:")) {
                    String heartbeatId = message.split(":")[1];
                    heartbeats.put(heartbeatId, System.currentTimeMillis());
                    System.out.println("Pulse received from " + heartbeatId);
                }
                checkTimeouts();
            }
        } catch (IOException e) {
            System.out.println("Service disconnected: " + e.getMessage());
        }
    }

    private void checkTimeouts() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : heartbeats.entrySet()) {
            String heartbeatId = entry.getKey();
            long lastPulse = entry.getValue();
            if ((currentTime - lastPulse) > 5000) {
                System.out.println("[ALERT] Service " + heartbeatId + " has died, last seen " + (currentTime - lastPulse) + " ms ago.");
                // We can insert some sort of restarts or something else here like we talked about in our meeting if we want
            }
        }
    }

    public static void main(String[] args) {
        CentralController controller = new CentralController(6355);
        controller.start();
    }
}

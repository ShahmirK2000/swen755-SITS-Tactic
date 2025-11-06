import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class OrderService implements Runnable {
    private volatile HealthStatus healthStatus = HealthStatus.HEALTHY;
    private final StoreNode storeNode;
    private Socket socket;
    private PrintWriter outWriter;
    private final int durationMillis;
    private final String fifoPath;
    private Thread fifoReaderThread;

    /**
     * Constructor
     * @param storeNode the reference to the store that's ordering
     */
    public OrderService(StoreNode storeNode, int durationMillis, String fifoPath) {
        this.storeNode = storeNode;
        this.durationMillis = durationMillis;
        this.fifoPath = fifoPath;
    }

    /**
     * Connect to the server
     * @param hostName the server host name or IP address
     * @param port the server port number
     * @throws IOException if an I/O error occurs when creating the socket
     */
    public void connectToServer(String hostName, int port) throws IOException {
        socket = new Socket(hostName, port);
        outWriter = new PrintWriter(socket.getOutputStream(), true);
        System.out.println(storeNode.getId() + " connected to server at " + hostName + ":" + port);
    }

    @Override
    public void run() {
        System.out.println("Order service is running...");

        if (fifoPath != null && !fifoPath.isEmpty()) {
            startFifoReader(fifoPath);
        }

        try {
            while (healthStatus.isHealthy()) {
                sendPulse();
                Thread.sleep(durationMillis);
            }
        } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(storeNode.getId() + " interrupted.");
            } finally {
                cleanup();
                System.out.println("Order service has shut down.");
            }
    }

    private void startFifoReader(String path) {
        fifoReaderThread = new Thread(() -> {
            File fifoFile = new File(path);
            try {
                if (!fifoFile.exists()) {
                    fifoFile.getParentFile().mkdirs();

                    // Should make this work on linux, mac, and windows
                    if(!System.getProperty("os.name").toLowerCase().contains("win")) {
                        Process process = new ProcessBuilder("mkfifo", path).inheritIO().start();
                        int rc = process.waitFor();
                        if (rc != 0 ) {
                            System.err.println("mkfifo returned " + rc);
                        }
                    } else {
                        fifoFile.createNewFile();
                    }
                }
            } catch (Exception e) {
                System.err.println("FIFO either doesn't exist or cannot be created: " + e.getMessage());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fifoFile)))) {
                String line;
                System.out.println(storeNode.getId() + " listening for orders placed at " + path);
                while (healthStatus.isHealthy()) {
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            handleOrder(line);
                        }
                    }
                    Thread.sleep(500); // ran into issues with blocking on windows, so this helps with retries
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }, storeNode.getId() + "-fifo-reader");

        fifoReaderThread.setDaemon(true);
        fifoReaderThread.start();
    }

    private void handleOrder(String line) {
        String product;
        int quantity;
        if (line.startsWith("ORDER:")) {
            String[] orderInformation = line.split(":", 3);
            if (orderInformation.length != 3) {
                System.out.println("Incorrect order format, must be: ORDER:product:quantity");
                return;
            }
            product = orderInformation[1].trim();
            quantity = Integer.parseInt(orderInformation[2].trim());

            System.out.println("Order received for | product: " + product + " quantity " + quantity);
        } else {
            System.out.println("Incorrect order format, must be: ORDER:product:quantity");
            return;
        }

        sendOrder(product, quantity);
    }

    private synchronized void sendOrder(String product, int quantity) {
        String order = String.format("ORDER:%s:%s:%d", storeNode.getId(), product, quantity);
        if (outWriter != null) {
            outWriter.println(order);
            outWriter.flush();
        }
        System.out.println("Order " + order + " sent.");
    }

    private void sendPulse() {
        if (outWriter != null) {
            outWriter.println("PULSE:" + storeNode.getId());
            System.out.println(storeNode.getId() + " : PULSE");
            outWriter.flush();
        }
    }

    public void shutdown() {
        if (fifoReaderThread != null) fifoReaderThread.interrupt();
    }

    /** Cleanup resources */
    private void cleanup() {
        try {
            shutdown();
            if (outWriter != null) {
                outWriter.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Cleaned up resources.");
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // I hard coded main for the purposes of our assingment, could be quickly reworked to use args or a GUI for a full implementation
        String storeId = "House-of-Zohran";

        // Change this line depending on where you want orders to go
        String fifoPath = "C:\\temp\\orders-House-of-Zohran.fifo";
        String host = "localhost";
        int port = 6355;
        int durationMillis = 1000;

        OrderService orderService = new OrderService(new StoreNode(storeId), durationMillis, fifoPath);
        try {
            orderService.connectToServer(host, port);
            orderService.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} // end class OrderService
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;

public class OrderService implements Runnable {
    private volatile HealthStatus healthStatus = HealthStatus.HEALTHY;
    private final StoreNode storeNode;
    private Socket socket;
    private PrintWriter outWriter;
    private final int durationMillis;
    private final String orderFilePath;
    private Thread orderFileReaderThread;

    /**
     * Constructor
     * @param storeNode the reference to the store that's ordering
     * @param durationMillis the time between pulses
     * @param orderFilePath the location of the temp file used to pipe orders
     */
    public OrderService(StoreNode storeNode, int durationMillis, String orderFilePath) {
        this.storeNode = storeNode;
        this.durationMillis = durationMillis;
        this.orderFilePath = orderFilePath;
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

        if (orderFilePath != null && !orderFilePath.isEmpty()) {
            startOrderFileReader(orderFilePath);
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

    private void startOrderFileReader(String path) {
        orderFileReaderThread = new Thread(() -> {
            File file = new File(path);
            while (healthStatus.isHealthy()) {
                try {
                    if (!file.exists()) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }

                    List<String> lines = Files.readAllLines(file.toPath());
                    if (!lines.isEmpty()) {
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                handleOrder(line);
                            }
                        }
                        // Clear the file after processing
                        new PrintWriter(file).close();
                    }

                    Thread.sleep(500); // Controls how often file polling is done. Kind of a dumb implementation but makes cross platform issues easier to handle
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, storeNode.getId() + "-file-reader");

        orderFileReaderThread.setDaemon(true);
        orderFileReaderThread.start();
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
            outWriter.flush();
            System.out.println(storeNode.getId() + " : PULSE");
        }
    }

    public void shutdown() {
        if (orderFileReaderThread != null) orderFileReaderThread.interrupt();
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
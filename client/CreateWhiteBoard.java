package client;

public class CreateWhiteBoard {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java client.CreateWhiteBoard <serverIPAddress> <serverPort> <username>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String username = args[2];

        WhiteboardClient client = new WhiteboardClient(host, port, username, true);
        client.start();
    }
}

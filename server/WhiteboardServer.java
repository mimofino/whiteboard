package server;

import common.AppConstants;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class WhiteboardServer {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java server.WhiteboardServer <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        try {
            WhiteboardServiceImpl service = new WhiteboardServiceImpl();
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(AppConstants.SERVICE_NAME, service);
            System.out.println("Whiteboard server running on port " + port);
            System.out.println("Service bound as: " + AppConstants.SERVICE_NAME);
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

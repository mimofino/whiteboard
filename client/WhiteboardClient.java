package client;

import common.*;
import common.AppConstants;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

public class WhiteboardClient {
    private final String serverHost;
    private final int serverPort;
    private final String username;
    private final boolean isCreator;

    private WhiteboardService service;
    private ClientCallbackImpl callback;
    private WhiteboardFrame frame;

    public WhiteboardClient(String serverHost, int serverPort, String username, boolean isCreator) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.username = username;
        this.isCreator = isCreator;
    }

    // try to connect to rmi server and register this client
    public void start() {
        try {
            callback = new ClientCallbackImpl();
            setupCallbacks();

            Registry registry = LocateRegistry.getRegistry(serverHost, serverPort);
            service = (WhiteboardService) registry.lookup(AppConstants.SERVICE_NAME);

            // check if someone already using this name
            if (service.isUsernameTaken(username)) {
                JOptionPane.showMessageDialog(null,
                        "Username '" + username + "' is already taken.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            boolean success;
            if (isCreator) {
                success = service.createWhiteboard(username, callback);
                if (!success) {
                    JOptionPane.showMessageDialog(null,
                            "Failed to create whiteboard. It may already exist.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                openMainWindow(true);
            } else {
                success = service.requestJoin(username, callback);
                if (!success) {
                    JOptionPane.showMessageDialog(null,
                            "Failed to request join. No whiteboard available.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                JOptionPane.showMessageDialog(null,
                        "Waiting for manager approval...",
                        "Join Request", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Connection failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }
    }

    // 注册所有回调事件，服务端会主动调用这些
    private void setupCallbacks() {
        callback.setOnJoinApproved(() -> SwingUtilities.invokeLater(() -> {
            if (frame == null) {
                try {
                    openMainWindow(service.isManager(username));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));

        callback.setOnJoinRejected(() -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    "Your join request was rejected by the manager.",
                    "Rejected", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }));

        // 有人想加入，弹窗让manager决定
        callback.setOnJoinRequest(requester -> SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(frame,
                    requester + " wants to share your whiteboard.\nAllow?",
                    "Join Request",
                    JOptionPane.YES_NO_OPTION);
            try {
                service.approveJoin(requester, choice == JOptionPane.YES_OPTION);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        callback.setOnUserJoined(u -> SwingUtilities.invokeLater(() ->
                frame.showStatus(u + " joined")));

        callback.setOnUserLeft(u -> SwingUtilities.invokeLater(() ->
                frame.showStatus(u + " left")));

        callback.setOnKicked(() -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame,
                    "You have been kicked out by the manager.",
                    "Kicked", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }));

        callback.setOnManagerClosed(() -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame,
                    "Manager has closed the whiteboard. Application will exit.",
                    "Closed", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }));

        callback.setOnShapeAdded(shape -> SwingUtilities.invokeLater(() ->
                frame.getDrawingPanel().addRemoteShape(shape)));

        callback.setOnWhiteboardUpdated(state -> SwingUtilities.invokeLater(() ->
                frame.getDrawingPanel().setShapes(state.getShapes())));

        callback.setOnChatMessage(parts -> SwingUtilities.invokeLater(() ->
                frame.getChatPanel().appendRemoteMessage(parts[0], parts[1])));

        callback.setOnUserListUpdated(users -> SwingUtilities.invokeLater(() ->
                frame.updateUserList(users)));
    }

    // open the main window and sync current whiteboard state from server
    private void openMainWindow(boolean isManager) throws Exception {
        frame = new WhiteboardFrame(username, isManager);
        frame.setWhiteboardClient(this);

        // get current state so new user sees the same canvas
        WhiteboardState state = service.getWhiteboardState();
        frame.getDrawingPanel().setShapes(state.getShapes());

        List<String> users = service.getUserList();
        frame.updateUserList(users);

        frame.getDrawingPanel().setOnShapeCompleted(shape -> {
            try {
                service.addShape(username, shape);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        frame.getChatPanel().setOnSendMessage(msg -> {
            try {
                service.sendChatMessage(username, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        frame.setVisible(true);
    }

    // user voluntarily disconnects
    public void leave() {
        try {
            if (service != null) {
                service.leave(username);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // manager closes the session, everyone gets kicked out
    public void closeWhiteboard() {
        try {
            service.closeWhiteboard(username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // kick someone out by username
    public void kickUser(String target) {
        try {
            service.kickUser(target, username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 清空画布，重新开始
    public void newWhiteboard() {
        try {
            WhiteboardState empty = new WhiteboardState();
            service.setWhiteboardState(username, empty);
            frame.getDrawingPanel().clearCanvas();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // load a .wb file and push the state to server so everyone sees it
    public void openWhiteboard(File file) {
        try {
            WhiteboardState state = BoardFileUtil.loadState(file);
            service.setWhiteboardState(username, state);
            frame.getDrawingPanel().setShapes(state.getShapes());
            frame.setCurrentFile(file);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Failed to open: " + e.getMessage());
        }
    }

    // grab shapes from canvas and write to disk
    public void saveWhiteboard(File file) {
        try {
            WhiteboardState state = new WhiteboardState(frame.getDrawingPanel().getShapes());
            BoardFileUtil.saveState(state, file);
            frame.setCurrentFile(file);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Failed to save: " + e.getMessage());
        }
    }

    private static class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
        private static final long serialVersionUID = 1L;

        private Consumer<String> onJoinRequest;
        private Runnable onJoinApproved;
        private Runnable onJoinRejected;
        private Consumer<String> onUserJoined;
        private Consumer<String> onUserLeft;
        private Runnable onKicked;
        private Runnable onManagerClosed;
        private Consumer<DrawingShape> onShapeAdded;
        private Consumer<WhiteboardState> onWhiteboardUpdated;
        private Consumer<String[]> onChatMessage;
        private Consumer<List<String>> onUserListUpdated;

        ClientCallbackImpl() throws RemoteException {
            super();
        }

        void setOnJoinRequest(Consumer<String> handler) { this.onJoinRequest = handler; }
        void setOnJoinApproved(Runnable handler) { this.onJoinApproved = handler; }
        void setOnJoinRejected(Runnable handler) { this.onJoinRejected = handler; }
        void setOnUserJoined(Consumer<String> handler) { this.onUserJoined = handler; }
        void setOnUserLeft(Consumer<String> handler) { this.onUserLeft = handler; }
        void setOnKicked(Runnable handler) { this.onKicked = handler; }
        void setOnManagerClosed(Runnable handler) { this.onManagerClosed = handler; }
        void setOnShapeAdded(Consumer<DrawingShape> handler) { this.onShapeAdded = handler; }
        void setOnWhiteboardUpdated(Consumer<WhiteboardState> handler) { this.onWhiteboardUpdated = handler; }
        void setOnChatMessage(Consumer<String[]> handler) { this.onChatMessage = handler; }
        void setOnUserListUpdated(Consumer<List<String>> handler) { this.onUserListUpdated = handler; }

        @Override public void onJoinRequest(String username) { if (onJoinRequest != null) onJoinRequest.accept(username); }
        @Override public void onJoinApproved() { if (onJoinApproved != null) onJoinApproved.run(); }
        @Override public void onJoinRejected() { if (onJoinRejected != null) onJoinRejected.run(); }
        @Override public void onUserJoined(String username) { if (onUserJoined != null) onUserJoined.accept(username); }
        @Override public void onUserLeft(String username) { if (onUserLeft != null) onUserLeft.accept(username); }
        @Override public void onKicked() { if (onKicked != null) onKicked.run(); }
        @Override public void onManagerClosed() { if (onManagerClosed != null) onManagerClosed.run(); }
        @Override public void onShapeAdded(DrawingShape shape) { if (onShapeAdded != null) onShapeAdded.accept(shape); }
        @Override public void onWhiteboardUpdated(WhiteboardState state) { if (onWhiteboardUpdated != null) onWhiteboardUpdated.accept(state); }
        @Override public void onChatMessage(String username, String message) { if (onChatMessage != null) onChatMessage.accept(new String[]{username, message}); }
        @Override public void onUserListUpdated(List<String> users) { if (onUserListUpdated != null) onUserListUpdated.accept(users); }
    }
}

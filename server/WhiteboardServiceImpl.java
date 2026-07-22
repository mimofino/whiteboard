package server;

import common.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class WhiteboardServiceImpl extends UnicastRemoteObject implements WhiteboardService {
    private static final long serialVersionUID = 1L;

    private final WhiteboardState state = new WhiteboardState();
    private String managerUsername;
    private final Map<String, ClientCallback> activeUsers = new LinkedHashMap<>();
    private final Map<String, ClientCallback> pendingUsers = new LinkedHashMap<>();

    public WhiteboardServiceImpl() throws RemoteException {
        super();
    }

    // first user to connect becomes the manager
    @Override
    public synchronized boolean createWhiteboard(String username, ClientCallback callback)
            throws RemoteException {
        if (managerUsername != null) {
            return false;
        }
        if (isUsernameTaken(username)) {
            return false;
        }
        managerUsername = username;
        activeUsers.put(username, callback);
        callback.onJoinApproved();
        refreshUserList();
        return true;
    }

    // put user in waiting list and ping the manager
    @Override
    public synchronized boolean requestJoin(String username, ClientCallback callback)
            throws RemoteException {
        if (managerUsername == null) {
            return false;
        }
        if (isUsernameTaken(username)) {
            return false;
        }
        pendingUsers.put(username, callback);
        ClientCallback managerCallback = activeUsers.get(managerUsername);
        if (managerCallback != null) {
            managerCallback.onJoinRequest(username);
        }
        return true;
    }

    // manager says yes or no to a pending join request
    @Override
    public synchronized void approveJoin(String username, boolean approved) throws RemoteException {
        ClientCallback callback = pendingUsers.remove(username);
        if (callback == null) {
            return;
        }
        if (approved) {
            activeUsers.put(username, callback);
            callback.onJoinApproved();
            callback.onWhiteboardUpdated(state);
            pushToAll(c -> c.onUserJoined(username));
            refreshUserList();
        } else {
            callback.onJoinRejected();
        }
    }

    // user left on their own
    @Override
    public synchronized void leave(String username) throws RemoteException {
        if (activeUsers.remove(username) != null) {
            pushToAll(c -> c.onUserLeft(username));
            refreshUserList();
        }
        pendingUsers.remove(username);
    }

    // only the manager can kick people, double check here
    @Override
    public synchronized void kickUser(String username, String managerName) throws RemoteException {
        if (!managerName.equals(managerUsername)) {
            return;
        }
        if (username.equals(managerUsername)) {
            return;
        }
        ClientCallback callback = activeUsers.remove(username);
        if (callback != null) {
            callback.onKicked();
            pushToAll(c -> c.onUserLeft(username));
            refreshUserList();
        }
    }

    // 关闭白板，通知所有连接的用户退出
    @Override
    public synchronized void closeWhiteboard(String managerName) throws RemoteException {
        if (!managerName.equals(managerUsername)) {
            return;
        }
        for (ClientCallback callback : activeUsers.values()) {
            try {
                callback.onManagerClosed();
            } catch (RemoteException e) {
                System.err.println("Failed to notify client: " + e.getMessage());
            }
        }
        activeUsers.clear();
        pendingUsers.clear();
        managerUsername = null;
        state.clear();
    }

    @Override
    public synchronized List<String> getUserList() throws RemoteException {
        return new ArrayList<>(activeUsers.keySet());
    }

    @Override
    public synchronized boolean isManager(String username) throws RemoteException {
        return username.equals(managerUsername);
    }

    @Override
    public synchronized String getManagerUsername() throws RemoteException {
        return managerUsername;
    }

    // recieve a new shape, store it, then broadcast to all other users
    @Override
    public synchronized void addShape(String username, DrawingShape shape) throws RemoteException {
        if (!activeUsers.containsKey(username)) {
            return;
        }
        state.addShape(shape);

        // skip the sender, they already drew it locally
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, ClientCallback> entry : activeUsers.entrySet()) {
            if (!entry.getKey().equals(username)) {
                try {
                    entry.getValue().onShapeAdded(shape);
                } catch (RemoteException e) {
                    System.err.println("Client unreachable, evicting: " + entry.getKey());
                    dead.add(entry.getKey());
                }
            }
        }
        dead.forEach(activeUsers::remove);
    }

    @Override
    public synchronized WhiteboardState getWhiteboardState() throws RemoteException {
        return new WhiteboardState(state.getShapes());
    }

    @Override
    public synchronized void setWhiteboardState(String managerName, WhiteboardState newState)
            throws RemoteException {
        if (!managerName.equals(managerUsername)) {
            return;
        }
        state.setShapes(newState.getShapes());
        pushToAll(c -> c.onWhiteboardUpdated(new WhiteboardState(state.getShapes())));
    }

    // braodcast a chat msg to everyone except the sender
    @Override
    public synchronized void sendChatMessage(String username, String message) throws RemoteException {
        if (!activeUsers.containsKey(username)) {
            return;
        }
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, ClientCallback> entry : activeUsers.entrySet()) {
            if (!entry.getKey().equals(username)) {
                try {
                    entry.getValue().onChatMessage(username, message);
                } catch (RemoteException e) {
                    System.err.println("Client unreachable, evicting: " + entry.getKey());
                    dead.add(entry.getKey());
                }
            }
        }
        dead.forEach(activeUsers::remove);
    }

    @Override
    public synchronized boolean isUsernameTaken(String username) throws RemoteException {
        return activeUsers.containsKey(username) || pendingUsers.containsKey(username);
    }

    // tell everyone the updated user list after someone joins or leaves
    private void refreshUserList() throws RemoteException {
        List<String> users = getUserList();
        pushToAll(c -> c.onUserListUpdated(users));
    }

    // helper to call some action on every connected client
    // if someone's connection is dead, collect and remove them after loop
    private void pushToAll(BroadcastTask action) throws RemoteException {
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, ClientCallback> entry : activeUsers.entrySet()) {
            try {
                action.apply(entry.getValue());
            } catch (RemoteException e) {
                System.err.println("Client unreachable, evicting: " + entry.getKey()
                        + " (" + e.getMessage() + ")");
                dead.add(entry.getKey());
            }
        }

        // 不能在迭代时删，所以用一个list暂存然后再删
        dead.forEach(activeUsers::remove);
    }

    @FunctionalInterface
    private interface BroadcastTask {
        void apply(ClientCallback callback) throws RemoteException;
    }
}

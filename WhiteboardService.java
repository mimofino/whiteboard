package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// server side rmi interface, clients call these to do stuff on the server
public interface WhiteboardService extends Remote {

    // manager creates the whiteboard, first one in
    boolean createWhiteboard(String username, ClientCallback callback) throws RemoteException;

    // peer wants to join, manager gets notified
    boolean requestJoin(String username, ClientCallback callback) throws RemoteException;

    // manager approves or rejects the join request
    void approveJoin(String username, boolean approved) throws RemoteException;

    // user leaves on their own
    void leave(String username) throws RemoteException;

    // manager kicks someone out
    void kickUser(String username, String managerName) throws RemoteException;

    // manager closes everything, all peers get notified
    void closeWhiteboard(String managerName) throws RemoteException;

    List<String> getUserList() throws RemoteException;

    boolean isManager(String username) throws RemoteException;

    String getManagerUsername() throws RemoteException;

    // add a shape and broadcast it to everyone
    void addShape(String username, DrawingShape shape) throws RemoteException;

    // get the full board state, used when a new user joins
    WhiteboardState getWhiteboardState() throws RemoteException;

    // replace the whole board, only manager can do this
    void setWhiteboardState(String managerName, WhiteboardState state) throws RemoteException;

    // send a chat msg to all users
    void sendChatMessage(String username, String message) throws RemoteException;

    // check if name is already in use
    boolean isUsernameTaken(String username) throws RemoteException;
}

package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

// client side callback interface, server calls these to push events back to clients
public interface ClientCallback extends Remote {

    // someone wants to join, tell the manager
    void onJoinRequest(String username) throws RemoteException;

    // join was approved
    void onJoinApproved() throws RemoteException;

    // join was rejected
    void onJoinRejected() throws RemoteException;

    // a new user joined
    void onUserJoined(String username) throws RemoteException;

    // a user left
    void onUserLeft(String username) throws RemoteException;

    // this client got kicked
    void onKicked() throws RemoteException;

    // manager shut down the board
    void onManagerClosed() throws RemoteException;

    // someone drew a new shape
    void onShapeAdded(DrawingShape shape) throws RemoteException;

    // whole board was replaced (open file or new)
    void onWhiteboardUpdated(WhiteboardState state) throws RemoteException;

    // recieve a chat msg
    void onChatMessage(String username, String message) throws RemoteException;

    // 用户列表更新了
    void onUserListUpdated(java.util.List<String> users) throws RemoteException;
}

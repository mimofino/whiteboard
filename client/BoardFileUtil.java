package client;

import common.WhiteboardState;
import java.io.*;

// utility class for reading/writing .wb files, no instance needed
public final class BoardFileUtil {
    private BoardFileUtil() {}

    // deserilize the whiteboard state from a .wb file
    public static WhiteboardState loadState(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (WhiteboardState) ois.readObject();
        }
    }

    // serialize and write state to disk
    public static void saveState(WhiteboardState state, File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(state);
        }
    }
}

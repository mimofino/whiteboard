package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardState implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<DrawingShape> shapes = new ArrayList<>();

    public WhiteboardState() {
    }

    public WhiteboardState(List<DrawingShape> shapes) {
        this.shapes = new ArrayList<>(shapes);
    }

    public List<DrawingShape> getShapes() {
        return shapes;
    }

    public void setShapes(List<DrawingShape> shapes) {
        this.shapes = new ArrayList<>(shapes);
    }

    public void addShape(DrawingShape shape) {
        shapes.add(shape);
    }

    public void clear() {
        shapes.clear();
    }
}

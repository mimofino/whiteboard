package common;

import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class DrawingShape implements Serializable {
    private static final long serialVersionUID = 1L;

    protected Color color;
    protected int strokeWidth;

    public DrawingShape(Color color, int strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    public Color getColor() { return color; }
    public int getStrokeWidth() { return strokeWidth; }
    public abstract String getType();

    // all the available drawing tools
    public enum DrawingTool {
        LINE, TRIANGLE, CIRCLE, RECTANGLE, FREE_DRAW, ERASER, TEXT
    }

    // concrete shape classes below
    public static class LineShape extends DrawingShape {
        private static final long serialVersionUID = 1L;
        private final int x1, y1, x2, y2;

        public LineShape(int x1, int y1, int x2, int y2, Color color, int strokeWidth) {
            super(color, strokeWidth);
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        }

        public int getX1() { return x1; }
        public int getY1() { return y1; }
        public int getX2() { return x2; }
        public int getY2() { return y2; }

        @Override public String getType() { return "LINE"; }
    }

    public static class RectangleShape extends DrawingShape {
        private static final long serialVersionUID = 1L;
        private final int x, y, width, height;

        public RectangleShape(int x, int y, int width, int height, Color color, int strokeWidth) {
            super(color, strokeWidth);
            this.x = x; this.y = y; this.width = width; this.height = height;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }

        @Override public String getType() { return "RECTANGLE"; }
    }

    public static class CircleShape extends DrawingShape {
        private static final long serialVersionUID = 1L;
        private final int x, y, width, height;

        public CircleShape(int x, int y, int width, int height, Color color, int strokeWidth) {
            super(color, strokeWidth);
            this.x = x; this.y = y; this.width = width; this.height = height;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }

        @Override public String getType() { return "CIRCLE"; }
    }

    public static class TriangleShape extends DrawingShape {
        private static final long serialVersionUID = 1L;
        private final int x1, y1, x2, y2, x3, y3;

        public TriangleShape(int x1, int y1, int x2, int y2, int x3, int y3,
                             Color color, int strokeWidth) {
            super(color, strokeWidth);
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.x3 = x3; this.y3 = y3;
        }

        public int getX1() { return x1; }
        public int getY1() { return y1; }
        public int getX2() { return x2; }
        public int getY2() { return y2; }
        public int getX3() { return x3; }
        public int getY3() { return y3; }

        @Override public String getType() { return "TRIANGLE"; }
    }

    public static class FreeDrawShape extends DrawingShape {
        private static final long serialVersionUID = 1L;
        private final List<Point> points = new ArrayList<>();

        public FreeDrawShape(Color color, int strokeWidth) {
            super(color, strokeWidth);
        }

        public void addPoint(int x, int y) { points.add(new Point(x, y)); }
        public List<Point> getPoints() { return points; }

        @Override public String getType() { return "FREE_DRAW"; }
    }

    public static class TextShape extends DrawingShape {
        private static final long serialVersionUID = 1L;
        private final int x, y;
        private final String text;
        private final int fontSize;

        public TextShape(int x, int y, String text, Color color, int fontSize) {
            super(color, 1);
            this.x = x; this.y = y; this.text = text; this.fontSize = fontSize;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public String getText() { return text; }
        public int getFontSize() { return fontSize; }

        @Override public String getType() { return "TEXT"; }
    }
}

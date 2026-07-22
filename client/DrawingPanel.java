package client;

import common.DrawingShape;
import common.DrawingShape.*;
import static common.DrawingShape.DrawingTool;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

public class DrawingPanel extends JPanel {
    private static final Color CANVAS_BG = Color.WHITE;

    private final List<DrawingShape> shapes = new ArrayList<>();
    private DrawingTool currentTool = DrawingTool.FREE_DRAW;
    private Color currentColor = Color.BLACK;
    private int strokeWidth = 2;
    private int eraserSize = 10;
    private int fontSize = 16;

    private int startX, startY;
    private DrawingShape currentShape;
    private boolean drawing;

    private Consumer<DrawingShape> onShapeCompleted;

    public DrawingPanel() {
        setBackground(CANVAS_BG);
        setPreferredSize(new Dimension(800, 600));

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // record where the user clicked
                startX = e.getX();
                startY = e.getY();
                drawing = true;

                if (currentTool == DrawingTool.FREE_DRAW) {
                    currentShape = new FreeDrawShape(currentColor, strokeWidth);
                    ((FreeDrawShape) currentShape).addPoint(startX, startY);
                } else if (currentTool == DrawingTool.ERASER) {
                    // eraser is basically free draw with white color
                    currentShape = new FreeDrawShape(CANVAS_BG, eraserSize);
                    ((FreeDrawShape) currentShape).addPoint(startX, startY);
                } else if (currentTool == DrawingTool.TEXT) {
                    drawing = false;
                    showTextInput(startX, startY);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!drawing) return;
                int x = e.getX();
                int y = e.getY();
                if (currentTool == DrawingTool.FREE_DRAW || currentTool == DrawingTool.ERASER) {
                    if (currentShape != null) {
                        ((FreeDrawShape) currentShape).addPoint(x, y);
                        repaint();
                    }
                } else {
                    // show a live preview of the shape while dragging
                    currentShape = createPreviewShape(startX, startY, x, y);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!drawing) return;
                drawing = false;
                int x = e.getX();
                int y = e.getY();

                // finalize the shape on mouse up
                DrawingShape finalShape;
                if (currentTool == DrawingTool.FREE_DRAW || currentTool == DrawingTool.ERASER) {
                    finalShape = currentShape;
                } else {
                    finalShape = createPreviewShape(startX, startY, x, y);
                }
                if (finalShape == null) {
                    currentShape = null;
                    return;
                }

                if (isValidShape(finalShape)) {
                    shapes.add(finalShape);
                    if (onShapeCompleted != null) {
                        onShapeCompleted.accept(finalShape);
                    }
                }
                currentShape = null;
                repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private DrawingShape createPreviewShape(int x1, int y1, int x2, int y2) {
        switch (currentTool) {
            case LINE:
                return new LineShape(x1, y1, x2, y2, currentColor, strokeWidth);
            case RECTANGLE:
                return new RectangleShape(
                        Math.min(x1, x2), Math.min(y1, y2),
                        Math.abs(x2 - x1), Math.abs(y2 - y1),
                        currentColor, strokeWidth);
            case CIRCLE:
                return new CircleShape(
                        Math.min(x1, x2), Math.min(y1, y2),
                        Math.abs(x2 - x1), Math.abs(y2 - y1),
                        currentColor, strokeWidth);
            case TRIANGLE:
                int midX = (x1 + x2) / 2;
                return new TriangleShape(midX, y1, x1, y2, x2, y2, currentColor, strokeWidth);
            default:
                return null;
        }
    }

    private boolean isValidShape(DrawingShape shape) {
        if (shape instanceof FreeDrawShape) {
            return ((FreeDrawShape) shape).getPoints().size() >= 2;
        }
        if (shape instanceof LineShape) {
            LineShape s = (LineShape) shape;
            return Math.abs(s.getX2() - s.getX1()) > 2 || Math.abs(s.getY2() - s.getY1()) > 2;
        }
        if (shape instanceof RectangleShape) {
            RectangleShape s = (RectangleShape) shape;
            return s.getWidth() > 2 && s.getHeight() > 2;
        }
        if (shape instanceof CircleShape) {
            CircleShape s = (CircleShape) shape;
            return s.getWidth() > 2 && s.getHeight() > 2;
        }
        return true;
    }

    // popup dialog - user enters text and picks a font size at the same time
    private void showTextInput(int x, int y) {
        JTextField textField = new JTextField(20);
        Integer[] sizes = {10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 64};
        JComboBox<Integer> sizeCombo = new JComboBox<>(sizes);
        sizeCombo.setSelectedItem(fontSize);

        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Text:"));
        panel.add(textField);
        panel.add(new JLabel("Font size:"));
        panel.add(sizeCombo);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Insert Text", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String text = textField.getText().trim();
            if (!text.isEmpty()) {
                fontSize = (Integer) sizeCombo.getSelectedItem();
                TextShape textShape = new TextShape(x, y, text, currentColor, fontSize);
                shapes.add(textShape);
                if (onShapeCompleted != null) {
                    onShapeCompleted.accept(textShape);
                }
                repaint();
            }
        }
    }

    // 每次repaint都会调这个，把所有shape重新画一遍
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        for (DrawingShape shape : shapes) {
            drawShape(g2, shape);
        }
        // also draw the in-progress shape (preview while dragging)
        if (currentShape != null && drawing) {
            drawShape(g2, currentShape);
        }
        g2.dispose();
    }

    // dispatch to the right draw method based on shape type
    private static void drawShape(Graphics2D g2, DrawingShape shape) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (shape instanceof LineShape) {
            LineShape s = (LineShape) shape;
            g2.setColor(s.getColor());
            g2.setStroke(new BasicStroke(s.getStrokeWidth()));
            g2.draw(new Line2D.Float(s.getX1(), s.getY1(), s.getX2(), s.getY2()));
        } else if (shape instanceof RectangleShape) {
            RectangleShape s = (RectangleShape) shape;
            g2.setColor(s.getColor());
            g2.setStroke(new BasicStroke(s.getStrokeWidth()));
            g2.drawRect(s.getX(), s.getY(), s.getWidth(), s.getHeight());
        } else if (shape instanceof CircleShape) {
            CircleShape s = (CircleShape) shape;
            g2.setColor(s.getColor());
            g2.setStroke(new BasicStroke(s.getStrokeWidth()));
            g2.draw(new Ellipse2D.Float(s.getX(), s.getY(), s.getWidth(), s.getHeight()));
        } else if (shape instanceof TriangleShape) {
            TriangleShape s = (TriangleShape) shape;
            g2.setColor(s.getColor());
            g2.setStroke(new BasicStroke(s.getStrokeWidth()));
            int[] xs = { s.getX1(), s.getX2(), s.getX3() };
            int[] ys = { s.getY1(), s.getY2(), s.getY3() };
            g2.drawPolygon(xs, ys, 3);
        } else if (shape instanceof FreeDrawShape) {
            FreeDrawShape s = (FreeDrawShape) shape;
            List<Point> points = s.getPoints();
            // need at least 2 points to draw a line segment
            if (points.size() < 2) return;
            g2.setColor(s.getColor());
            g2.setStroke(new BasicStroke(s.getStrokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < points.size(); i++) {
                Point p1 = points.get(i - 1);
                Point p2 = points.get(i);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        } else if (shape instanceof TextShape) {
            TextShape s = (TextShape) shape;
            g2.setColor(s.getColor());
            g2.setFont(new Font("SansSerif", Font.PLAIN, s.getFontSize()));
            g2.drawString(s.getText(), s.getX(), s.getY());
        }
    }

    public void setCurrentTool(DrawingTool tool) { this.currentTool = tool; }
    public void setCurrentColor(Color color) { this.currentColor = color; }
    public void setStrokeWidth(int width) { this.strokeWidth = width; }
    public void setEraserSize(int size) { this.eraserSize = size; }
    public void setFontSize(int size) { this.fontSize = size; }
    public void setOnShapeCompleted(Consumer<DrawingShape> listener) { this.onShapeCompleted = listener; }

    public List<DrawingShape> getShapes() { return new ArrayList<>(shapes); }

    public void setShapes(List<DrawingShape> newShapes) {
        shapes.clear();
        shapes.addAll(newShapes);
        repaint();
    }

    public void addRemoteShape(DrawingShape shape) {
        shapes.add(shape);
        repaint();
    }

    public void clearCanvas() {
        shapes.clear();
        repaint();
    }
}

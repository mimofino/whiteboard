package client;

import common.DrawingShape.DrawingTool;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.swing.*;

public class WhiteboardFrame extends JFrame {
    private static final int COLOR_COLS = 10;
    private static final float[] HUES = {
            0f, 30f/360, 60f/360, 90f/360, 120f/360,
            180f/360, 210f/360, 240f/360, 270f/360, 300f/360
    };
    private static final float[][] SHADE_SB = {
            {0.20f, 1.00f},
            {0.45f, 1.00f},
            {0.75f, 1.00f},
            {1.00f, 0.88f},
            {1.00f, 0.55f},
    };

    private final DrawingPanel drawingPanel = new DrawingPanel();
    private final ChatPanel chatPanel = new ChatPanel();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final JLabel statusLabel = new JLabel("Ready");

    private WhiteboardClient client;
    private final String username;
    private final boolean isManager;
    private File currentFile;
    private JButton colorIndicatorButton;
    private Color currentColor = Color.BLACK;

    public WhiteboardFrame(String username, boolean isManager) {
        this.username = username;
        this.isManager = isManager;

        setTitle("Distributed Whiteboard - " + username + (isManager ? " (Manager)" : ""));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleQuit();
            }
        });

        setJMenuBar(createMenuBar());
        setLayout(new BorderLayout(5, 5));

        add(createToolPanel(), BorderLayout.WEST);
        add(drawingPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setPreferredSize(new Dimension(250, 0));

        userList.setBorder(BorderFactory.createTitledBorder("Online Users"));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(250, 150));
        rightPanel.add(userScroll, BorderLayout.NORTH);
        rightPanel.add(chatPanel, BorderLayout.CENTER);

        if (isManager) {
            JButton kickButton = new JButton("Kick Selected User");
            kickButton.addActionListener(e -> kickSelectedUser());
            rightPanel.add(kickButton, BorderLayout.SOUTH);
        }

        add(rightPanel, BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As");
        JMenuItem closeItem = new JMenuItem("Close");

        newItem.addActionListener(e -> {
            if (!drawingPanel.getShapes().isEmpty()) {
                Object[] options = {"Save", "Don't Save", "Cancel"};
                int choice = JOptionPane.showOptionDialog(this,
                        "The current whiteboard has unsaved changes.\nDo you want to save before creating a new one?",
                        "Unsaved Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null, options, options[0]);
                if (choice == 0) {
                    // Save: save to current file if it exists, otherwise show Save As dialog
                    boolean saved;
                    if (currentFile != null) {
                        if (client != null) client.saveWhiteboard(currentFile);
                        saved = true;
                    } else {
                        saved = saveAs();
                    }
                    // Only proceed to new whiteboard if save completed (not cancelled)
                    if (saved && client != null) {
                        client.newWhiteboard();
                        resetFileState();
                    }
                } else if (choice == 1) {
                    // Don't Save: proceed directly
                    if (client != null) {
                        client.newWhiteboard();
                        resetFileState();
                    }
                }
                // Cancel or dialog closed: do nothing
            } else {
                if (client != null) {
                    client.newWhiteboard();
                    resetFileState();
                }
            }
        });
        openItem.addActionListener(e -> {
            // Only prompt to save if a file has already been saved/opened AND canvas is not empty
            if (currentFile != null && !drawingPanel.getShapes().isEmpty()) {
                Object[] options = {"Save", "Don't Save", "Cancel"};
                int choice = JOptionPane.showOptionDialog(this,
                        "The current whiteboard has unsaved changes.\nDo you want to save before opening another one?",
                        "Unsaved Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null, options, options[0]);
                if (choice == 0) {
                    // Save to the existing current file, then proceed to open
                    if (client != null) client.saveWhiteboard(currentFile);
                } else if (choice != 1) {
                    // Cancel or dialog closed: abort
                    return;
                }
                // Don't Save (choice == 1): fall through to open dialog
            }
            // If no current file exists, go directly to open dialog (no save prompt)
            FileDialog fd = new FileDialog(this, "Open Whiteboard", FileDialog.LOAD);
            fd.setDirectory(currentFile != null ? currentFile.getParent() : System.getProperty("user.home"));
            fd.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".wb"));
            fd.setVisible(true);
            if (fd.getFile() != null) {
                File chosen = new File(fd.getDirectory(), fd.getFile());
                if (client != null) client.openWhiteboard(chosen);
            }
        });
        saveItem.addActionListener(e -> {
            if (currentFile != null) {
                if (client != null) client.saveWhiteboard(currentFile);
            } else {
                saveAs();
            }
        });
        saveAsItem.addActionListener(e -> saveAs());
        closeItem.addActionListener(e -> handleQuit());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(closeItem);

        if (!isManager) {
            newItem.setEnabled(false);
            openItem.setEnabled(false);
            saveItem.setEnabled(false);
            saveAsItem.setEnabled(false);
            closeItem.setEnabled(false);
        }

        menuBar.add(fileMenu);
        return menuBar;
    }

    /** Clears the current file association after creating a new whiteboard. */
    private void resetFileState() {
        currentFile = null;
        setTitle("Distributed Whiteboard - " + username + (isManager ? " (Manager)" : ""));
    }

    /** Shows a Save As dialog. Returns true if the user completed the save, false if cancelled. */
    private boolean saveAs() {
        FileDialog fd = new FileDialog(this, "Save As", FileDialog.SAVE);
        if (currentFile != null) {
            fd.setDirectory(currentFile.getParent());
            fd.setFile(currentFile.getName());
        } else {
            fd.setDirectory(System.getProperty("user.home"));
            fd.setFile("whiteboard.wb");
        }
        fd.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".wb"));
        fd.setVisible(true);
        if (fd.getFile() != null) {
            String name = fd.getFile();
            if (!name.toLowerCase().endsWith(".wb")) {
                name = name + ".wb";
            }
            File chosen = new File(fd.getDirectory(), name);
            if (client != null) client.saveWhiteboard(chosen);
            return true;
        }
        return false;
    }

    private JPanel createToolPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 6, 8, 6)));
        panel.setPreferredSize(new Dimension(100, 0));

        addSectionLabel(panel, "DRAW");
        JPanel shapeGrid = new JPanel(new GridLayout(3, 2, 4, 4));
        shapeGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
        shapeGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        addIconToolButton(shapeGrid, DrawingTool.LINE,      "Line");
        addIconToolButton(shapeGrid, DrawingTool.RECTANGLE, "Rectangle");
        addIconToolButton(shapeGrid, DrawingTool.CIRCLE,    "Circle");
        addIconToolButton(shapeGrid, DrawingTool.TRIANGLE,  "Triangle");
        addIconToolButton(shapeGrid, DrawingTool.FREE_DRAW, "Free Draw");
        addIconToolButton(shapeGrid, DrawingTool.TEXT,      "Text");
        panel.add(shapeGrid);

        panel.add(Box.createVerticalStrut(10));
        addSectionLabel(panel, "STROKE");
        JComboBox<String> strokeCombo = new JComboBox<>(new String[]{"1", "2", "3", "5", "8"});
        strokeCombo.setSelectedIndex(1);
        strokeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        strokeCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        strokeCombo.addActionListener(e ->
                drawingPanel.setStrokeWidth(Integer.parseInt((String) strokeCombo.getSelectedItem())));
        panel.add(strokeCombo);

        panel.add(Box.createVerticalStrut(10));
        addSectionLabel(panel, "COLOR");
        colorIndicatorButton = new JButton();
        colorIndicatorButton.setBackground(currentColor);
        colorIndicatorButton.setOpaque(true);
        colorIndicatorButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        colorIndicatorButton.setPreferredSize(new Dimension(80, 32));
        colorIndicatorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        colorIndicatorButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        colorIndicatorButton.setToolTipText("Click to pick colour");
        colorIndicatorButton.addActionListener(e -> showColorPopup(colorIndicatorButton));
        panel.add(colorIndicatorButton);

        panel.add(Box.createVerticalStrut(10));
        addSectionLabel(panel, "ERASER");
        JPanel eraserGrid = new JPanel(new GridLayout(1, 3, 4, 0));
        eraserGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
        eraserGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        int[] eraserSizes = {8, 16, 32};
        String[] eraserTips = {"Small", "Medium", "Large"};
        for (int i = 0; i < 3; i++) {
            final int dotSize  = eraserSizes[i];
            JButton eb = new JButton(createEraserIcon(36, dotSize));
            eb.setToolTipText(eraserTips[i]);
            eb.setMargin(new Insets(4, 4, 4, 4));
            eb.addActionListener(e -> {
                drawingPanel.setCurrentTool(DrawingTool.ERASER);
                drawingPanel.setEraserSize(dotSize);
            });
            eraserGrid.add(eb);
        }
        panel.add(eraserGrid);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void addSectionLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setForeground(Color.GRAY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(3));
    }

    private void addIconToolButton(JPanel panel, DrawingTool tool, String tooltip) {
        JButton btn = new JButton(createShapeIcon(tool, 36));
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(4, 4, 4, 4));
        btn.addActionListener(e -> drawingPanel.setCurrentTool(tool));
        panel.add(btn);
    }

    private ImageIcon createEraserIcon(int size, int dotPx) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, size, size);

        // filled circle whose visual diameter is proportional to dotPx (8/16/32)
        int draw = Math.min(size - 6, dotPx / 2 + 4);
        int off = (size - draw) / 2;
        g.setColor(new Color(180, 180, 180));
        g.fillOval(off, off, draw, draw);
        g.setColor(new Color(100, 100, 100));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(off, off, draw, draw);
        g.dispose();
        return new ImageIcon(img);
    }

    private ImageIcon createShapeIcon(DrawingTool tool, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, size, size);

        Color ink = new Color(60, 60, 60);
        g.setColor(ink);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int m = 5;
        int w = size - m * 2;
        int h = size - m * 2;

        switch (tool) {
            case LINE:
                g.draw(new Line2D.Float(m, size - m, size - m, m));
                break;
            case RECTANGLE:
                g.draw(new RoundRectangle2D.Float(m, m + 3, w, h - 6, 4, 4));
                break;
            case CIRCLE:
                g.draw(new Ellipse2D.Float(m, m, w, h));
                break;
            case TRIANGLE: {
                int cx = size / 2;
                int[] xs = {cx, m, size - m};
                int[] ys = {m + 1, size - m, size - m};
                g.drawPolygon(xs, ys, 3);
                break;
            }
            case FREE_DRAW: {
                int[] px = {m, m + w / 4, m + w / 2, m + 3 * w / 4, size - m};
                int[] py = {size / 2, m + 4, size - m - 4, m + 4, size / 2};
                for (int i = 0; i < px.length - 1; i++) {
                    g.draw(new Line2D.Float(px[i], py[i], px[i + 1], py[i + 1]));
                }
                break;
            }
            case TEXT:
                g.setFont(new Font("SansSerif", Font.BOLD, size - 10));
                FontMetrics fm = g.getFontMetrics();
                String t = "A";
                int tx = (size - fm.stringWidth(t)) / 2;
                int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(t, tx, ty);
                break;
            default:
                break;
        }
        g.dispose();
        return new ImageIcon(img);
    }

    private Color[][] buildColorGrid() {
        Color[][] grid = new Color[6][COLOR_COLS];
        // Row 0: grayscale from white to black
        for (int c = 0; c < COLOR_COLS; c++) {
            int v = Math.round(255f * c / (COLOR_COLS - 1));
            grid[0][c] = new Color(255 - v, 255 - v, 255 - v);
        }
        // Rows 1-5: hues × shade levels
        for (int r = 0; r < SHADE_SB.length; r++) {
            for (int c = 0; c < COLOR_COLS; c++) {
                grid[r + 1][c] = Color.getHSBColor(HUES[c], SHADE_SB[r][0], SHADE_SB[r][1]);
            }
        }
        return grid;
    }

    private void showColorPopup(JButton anchor) {
        JPopupMenu popup = new JPopupMenu();
        JPanel grid = new JPanel(new GridLayout(6, COLOR_COLS, 2, 2));
        grid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Color[][] colors = buildColorGrid();
        for (Color[] row : colors) {
            for (Color color : row) {
                JButton swatch = new JButton();
                swatch.setBackground(color);
                swatch.setOpaque(true);
                swatch.setBorderPainted(false);
                swatch.setPreferredSize(new Dimension(20, 20));
                swatch.setMaximumSize(new Dimension(20, 20));
                swatch.setBorder(BorderFactory.createLineBorder(
                        color.darker(), 1));
                swatch.addActionListener(e -> {
                    currentColor = color;
                    drawingPanel.setCurrentColor(color);
                    colorIndicatorButton.setBackground(color);
                    colorIndicatorButton.repaint();
                    popup.setVisible(false);
                });
                swatch.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                        swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
                    }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) {
                        swatch.setBorder(BorderFactory.createLineBorder(color.darker(), 1));
                    }
                });
                grid.add(swatch);
            }
        }
        popup.add(grid);
        popup.show(anchor, anchor.getWidth() + 2, 0);
    }

    private void kickSelectedUser() {
        String selected = userList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a user to kick.");
            return;
        }
        if (selected.equals(username)) {
            JOptionPane.showMessageDialog(this, "You cannot kick yourself.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Kick user " + selected + "?",
                "Confirm Kick", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION && client != null) {
            client.kickUser(selected);
        }
    }

    private void handleQuit() {
        int choice = JOptionPane.showConfirmDialog(this,
                isManager ? "Close whiteboard for all users?" : "Leave whiteboard?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        if (isManager) {
            client.closeWhiteboard();
        } else {
            client.leave();
        }
        System.exit(0);
    }

    public DrawingPanel getDrawingPanel() {
        return drawingPanel;
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void setWhiteboardClient(WhiteboardClient client) {
        this.client = client;
    }

    public void updateUserList(List<String> users) {
        userListModel.clear();
        for (String user : users) {
            userListModel.addElement(user);
        }
    }

    public void showStatus(String message) {
        statusLabel.setText(message);
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        setTitle("Distributed Whiteboard - " + username
                + (isManager ? " (Manager)" : "") + " - " + file.getName());
    }
}

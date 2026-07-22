package client;

import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;
import javax.swing.*;

public class ChatPanel extends JPanel {
    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private Consumer<String> onSendMessage;

    public ChatPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Chat"));
        setPreferredSize(new Dimension(250, 0));

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputField.addActionListener(e -> sendMessage());
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty() && onSendMessage != null) {
            onSendMessage.accept(msg);
            appendLocalMessage("You", msg);
            inputField.setText("");
        }
    }

    public void appendLocalMessage(String sender, String message) {
        SwingUtilities.invokeLater(() ->
                chatArea.append("[" + sender + "]: " + message + "\n"));
    }

    public void appendRemoteMessage(String sender, String message) {
        SwingUtilities.invokeLater(() ->
                chatArea.append("[" + sender + "]: " + message + "\n"));
    }

    public void setOnSendMessage(Consumer<String> listener) {
        this.onSendMessage = listener;
    }
}

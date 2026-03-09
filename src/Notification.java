import java.time.LocalDateTime;

/**
 * Represents a notification sent to a user.
 *
 * UML: Notification
 *   - message   : String
 *   - timestamp : DateTime
 *   + sendEmail() : void
 */
public class Notification {

    private String        message;
    private LocalDateTime timestamp;
    private User          recipient;

    public Notification(User recipient, String message) {
        if (recipient == null)
            throw new IllegalArgumentException("Recipient cannot be null.");
        if (message == null || message.isEmpty())
            throw new IllegalArgumentException("Message cannot be empty.");
        this.recipient = recipient;
        this.message   = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Simulates sending an email to the recipient.
     */
    public void sendEmail() {
        System.out.println("[EMAIL → " + recipient.getEmail() + "] " + message);
    }

    // ── Getters ────────────────────────────────────────────────────
    public String        getMessage()   { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public User          getRecipient() { return recipient; }

    @Override
    public String toString() {
        return "Notification{ to='" + recipient.getEmail() + "', msg='" + message + "' }";
    }
}

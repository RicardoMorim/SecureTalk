package ricardo.messagingapp.domain.message;

enum MessageStatus {
    SENT,
    DELIVERED,
    READ;

    public static MessageStatus create() {
        return SENT;
    }

    /**
     * Returns the next status in the sequence.
     * If the current status is READ, it returns READ again.
     *
     * @return the next MessageStatus
     */
    public MessageStatus next() {
        return switch (this) {
            case SENT -> DELIVERED;
            case DELIVERED -> READ;
            default -> READ;
        };
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

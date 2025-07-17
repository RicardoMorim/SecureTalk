package ricardo.messagingapp.Eventhandlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ricardo.messagingapp.domain.message.DomainEvents.MessageDelivered;
import ricardo.messagingapp.domain.message.DomainEvents.MessageEdited;
import ricardo.messagingapp.domain.message.DomainEvents.MessageRead;
import ricardo.messagingapp.domain.message.DomainEvents.MessageSent;


@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventHandler {

    @EventListener
    @Async
    public void handleMessageSent(MessageSent event) {
        log.info("Message sent in conversation {}: {} -> {}",
                event.conversationId().getId(),
                event.senderId().getId(),
                event.receiverId().getId());

        // TODO:
        // - Send push notifications
        // - Update user activity status
        // - Trigger delivery mechanisms
    }

    @EventListener
    @Async
    public void handleMessageDelivered(MessageDelivered event) {
        log.info("Message {} delivered in conversation {} at {}",
                event.messageId(),
                event.conversationId().getId(),
                event.deliveredAt());

        // TODO:
        // - Notify sender of delivery
        // - Update delivery metrics
    }

    @EventListener
    @Async
    public void handleMessageRead(MessageRead event) {
        log.info("Message {} read by {} in conversation {} at {}",
                event.messageId(),
                event.readBy().getId(),
                event.conversationId().getId(),
                event.readAt());

        // TODO:
        // - Send read receipts
        // - Update conversation metrics
    }

    @EventListener
    @Async
    public void handleMessageEdited(MessageEdited event) {
        log.info("Message {} edited by {} in conversation {} at {}",
                event.messageId(),
                event.editedBy().getId(),
                event.conversationId().getId(),
                event.editedAt());

        // TODO:
        // - Notify participants of edit
        // - Log edit history
    }
}
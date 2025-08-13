package ricardo.messagingapp.eventhandlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.DTO.EditMessage;
import ricardo.messagingapp.domain.message.DTO.MessageNotification;
import ricardo.messagingapp.domain.message.DTO.ReadNotification;
import ricardo.messagingapp.domain.message.DomainEvents.MessageDelivered;
import ricardo.messagingapp.domain.message.DomainEvents.MessageEdited;
import ricardo.messagingapp.domain.message.DomainEvents.MessageRead;
import ricardo.messagingapp.domain.message.DomainEvents.MessageSent;
import ricardo.messagingapp.domain.message.UserId;
import ricardo.messagingapp.services.EncryptionService;
import ricardo.messagingapp.services.MessageService;
import ricardo.messagingapp.services.MetricsService;
import ricardo.messagingapp.services.NotificationService;

import java.time.ZoneId;


@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventHandler {

    MetricsService metricsService;
    NotificationService notificationService;
    SimpMessagingTemplate simpMessagingTemplate;
    EncryptionService encryptionService;
    MessageService messageService;


    @EventListener
    @Async
    public void handleMessageSent(MessageSent event) {
        try {
            log.info("Message sent in conversation {}: {} -> {}", event.conversationId().getId(), event.senderId().getId(), event.receiverId().getId());

            // TODO:
            // - Send push notifications
            // - Update user activity status
            // - Trigger delivery mechanisms

            notificationService.notifyNewMessage(event);
            metricsService.incrementMessageSentCount(event);
            simpMessagingTemplate.convertAndSendToUser(event.receiverId().getId().toString(), "/queue/messages", new MessageNotification(encryptionService.decrypt(event.encryptedContent()), event.senderId(), event.sentAt().atZone(ZoneId.systemDefault()).toInstant(), event.conversationId()));


        } catch (Exception e) {
            log.error("Error handling message sent event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleMessageDelivered(MessageDelivered event) {
        try {
            log.info("Message {} delivered in conversation {} at {}", event.messageId(), event.conversationId().getId(), event.deliveredAt());

            // TODO:
            // - Notify sender of delivery
            // - Update delivery metrics

            notificationService.notifyMessageDelivery(event);
            metricsService.incrementMessageDeliveredCount(event);
        } catch (Exception e) {
            log.error("Error handling message delivered event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleMessageRead(MessageRead event) {
        try {
            log.info("Message {} in conversation {} at {}", event.messageId(), event.conversationId().getId(), event.readAt());

            // TODO:
            // - Send read receipts
            // - Update conversation metrics

            UserId userId = ConversationId.extractTheOtherUserId(event.conversationId().getId(), UserId.valueOf(event.userWhoReadId().getId()));

            notificationService.notifyMessageRead(event);
            metricsService.updateMessageReadLatency(event);
            simpMessagingTemplate.convertAndSendToUser(String.valueOf(userId.getId()), "/queue/messages", new ReadNotification(event.messageId(), event.userWhoReadId(), event.conversationId()));
        } catch (
                Exception e) {
            log.error("Error handling message Read event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    @Async
    public void handleMessageEdited(MessageEdited event) {
        try {
            log.info("Message {} edited by {} in conversation {} at {}", event.messageId(), event.editedBy().getId(), event.conversationId().getId(), event.editedAt());

            if (!ConversationId.validateConversationId(event.conversationId().getId(), event.editedBy())) {
                log.warn("Edit attempt by unauthorized user");
                return;
            }

            // TODO:
            // - Notify participants of edit
            // - Log edit history
            UserId userId = ConversationId.extractTheOtherUserId(event.conversationId().getId(), UserId.valueOf(event.editedBy().getId()));

            notificationService.broadcastMessageEdit(new EditMessage(event.messageId(), event.newEncryptedContent(), event.editedBy(), event.conversationId(), event.editedAt().toString()));
            metricsService.trackEditFrequency(event);
            simpMessagingTemplate.convertAndSendToUser(String.valueOf(userId.getId()), "/queue/messages", new EditMessage(event.messageId(), encryptionService.decrypt(event.newEncryptedContent()), event.editedBy(), event.conversationId(), event.editedAt().toString()));
        } catch (Exception e) {
            log.error("Error handling message edit event: {}", e.getMessage(), e);
        }
    }
}
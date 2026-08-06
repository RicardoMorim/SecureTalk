package ricardo.messagingapp.messagingcore.eventhandlers;

import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.AppRole;
import com.ricardo.auth.domain.user.User;
import com.ricardo.auth.domain.user.Username;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ricardo.messagingapp.messagingcore.domain.message.DTO.EditMessage;
import ricardo.messagingapp.messagingcore.domain.message.DTO.MessageNotification;
import ricardo.messagingapp.messagingcore.domain.message.DTO.ReadNotification;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.*;
import ricardo.messagingapp.messagingcore.domain.message.MessageContent;
import ricardo.messagingapp.messagingcore.services.*;

import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventHandler {

    private final MetricsService metricsService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final EncryptionService encryptionService;
    private final MessageService messageService;
    private final UserService<User, AppRole, UUID> userService;
    private final ConversationService conversationService;


    @EventListener
    @Async
    public void handleMessageSent(MessageSent event) {
        logEvent("MessageSent", event.messageId(), event.senderUsername(), event.receiverUsername());
        try {
            String decrypted = decryptContent(event.encryptedContent());
            notificationService.notifyNewMessage(event);
            metricsService.incrementMessageSentCount(event);

            messagingTemplate.convertAndSendToUser(
                    event.receiverUsername().toString(),
                    "/queue/messages",
                    new MessageNotification(
                            MessageContent.valueOf(decrypted),
                            event.senderUsername(),
                            event.receiverUsername(),
                            event.sentAt().atZone(ZoneId.systemDefault()).toInstant()
                    )
            );
        } catch (Exception e) {
            handleError("MessageSent", e);
        }
    }

    @EventListener
    @Async
    public void handleMessageDelivered(MessageDelivered event) {
        logEvent("MessageDelivered", event.messageId(), null, event.receiverUsername());
        try {
            notificationService.notifyMessageDelivery(event);
            metricsService.incrementMessageDeliveredCount(event);
        } catch (Exception e) {
            handleError("MessageDelivered", e);
        }
    }

    @EventListener
    @Async
    public void handleMessageRead(MessageRead event) {
        logEvent("MessageRead", event.messageId(), event.userWhoSentUsername(), event.userWhoReadUsername());
        try {
            UUID senderId = userService
                    .getUserByUserName(event.userWhoSentUsername().getUsername())
                    .getId();

            notificationService.notifyMessageRead(event);
            metricsService.updateMessageReadLatency(event);

            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/messages",
                    new ReadNotification(event.messageId(), event.userWhoReadUsername())
            );
        } catch (Exception e) {
            handleError("MessageRead", e);
        }
    }

    @EventListener
    @Async
    public void handleMessageEdited(MessageEdited event) {
        logEvent("MessageEdited", event.messageId(), event.userWhoEditedUsername(), event.receiverUsername());
        try {
            User receiver = userService.getUserByUserName(event.receiverUsername().getUsername());
            User sender = userService.getUserByUserName(event.userWhoEditedUsername().getUsername());
            String decrypted = decryptContent(event.newEncryptedContent());

            notificationService.notifyMessageEdit(event);
            metricsService.trackEditFrequency(event);

            messagingTemplate.convertAndSendToUser(
                    receiver.getId().toString(),
                    "/queue/messages",
                    new EditMessage(
                            event.messageId(),
                            MessageContent.valueOf(decrypted),
                            Username.valueOf(sender.getUsername()),
                            event.editedAt()
                    )
            );
        } catch (Exception e) {
            handleError("MessageEdited", e);
        }
    }



    private void logEvent(String type, UUID messageId, Object actor, Object target) {
        log.info("{} received | messageId={} | actor={} | target={}",
                type, messageId, actor, target);
    }

    private void handleError(String type, Exception e) {
        log.error("Error handling {} event: {}", type, e.getMessage(), e);
    }

    private String decryptContent(Object encryptedContent) {
        return encryptionService.decrypt(encryptedContent.toString());
    }
}

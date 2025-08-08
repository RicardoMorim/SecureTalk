package ricardo.messagingapp.controller;

import com.ricardo.auth.core.RateLimiter;
import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.User;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.DTO.EditMessage;
import ricardo.messagingapp.domain.message.DTO.MessagePayload;
import ricardo.messagingapp.domain.message.DomainEvents.MessageDeleted;
import ricardo.messagingapp.domain.message.DomainEvents.MessageEdited;
import ricardo.messagingapp.domain.message.DomainEvents.MessageRead;
import ricardo.messagingapp.domain.message.MessageContent;
import ricardo.messagingapp.domain.message.UserId;
import ricardo.messagingapp.services.MessageService;
import ricardo.messagingapp.services.NotificationService;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class MessageController {
    // as we'll use the same rate limiter, this prefix will help us identify the rate limiters for WebSocket messages
    private static final String RATE_LIMITER_PREFIX = "ws:";

    private final MessageService messageService;
    private final UserService<User, Long> userService;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;

    public MessageController(MessageService messageService, UserService<User, Long> userService, NotificationService notificationService, RateLimiter rateLimiter) {
        this.messageService = messageService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.rateLimiter = rateLimiter;

    }

    @MessageMapping("/sendMessage")
    @SendTo("/user/{userId}/queue/messages")
    public String sendMessage(
            MessagePayload payload,
            Principal principal) {
        // Process the incoming message
        UserId userId = UserId.valueOf(userService.getUserByEmail(principal.getName()).getId());

        if (!checkRateLimit(userId)) {
            return "Rate limit exceeded";
        }


        if (!ConversationId.validateConversationId(payload.getConversationId(), userId)) {
            return "Invalid conversation ID";
        }

        boolean success = messageService.sendMessage(payload, userId);

        if (success) {
            return "Message sent successfully";
        } else {
            return "Failed to send message";
        }
    }

    @MessageMapping("/editMessage")
    @SendTo("/user/{userId}/queue/messages")
    public String editMessage(
            EditMessage payload,
            Principal principal) {

        UserId userId = UserId.valueOf(userService.getUserByEmail(principal.getName()).getId());

        if (!checkRateLimit(userId)) {
            return "Rate limit exceeded";
        }

        boolean success = messageService.updateMessage(payload.messageId(), MessageContent.valueOf(payload.newDecryptedContent()), userId);

        if (success) {
            // Publish the event
            MessageEdited event = new MessageEdited(
                    payload.messageId(),
                    ConversationId.fromConversationId(String.valueOf(payload.conversationId())),
                    userId,
                    LocalDateTime.now(),
                    payload.newDecryptedContent()
            );

            notificationService.notifyMessageEdited(event);

            return "Message edited successfully";
        } else {
            return "Failed to edit message";
        }
    }


    @MessageMapping("/deleteMessage")
    @SendTo("/user/{userId}/queue/messages")
    public String deleteMessage(
            Long messageId,
            Principal principal) {

        UserId userId = UserId.valueOf(userService.getUserByEmail(principal.getName()).getId());

        if (!checkRateLimit(userId)) {
            return "Rate limit exceeded";
        }

        boolean success = messageService.deleteMessage(messageId, userId);

        if (success) {
            MessageDeleted event = new MessageDeleted(
                    messageId,
                    ConversationId.fromConversationId(String.valueOf(messageService.getMessageConversationId(messageId))),
                    userId,
                    LocalDateTime.now()
            );

            notificationService.notifyMessageDeletion(event);
            return "Message deleted successfully";
        } else {
            return "Failed to delete message";
        }
    }


    @MessageMapping("/readNotification")
    @SendTo("/user/{userId}/queue/messages")
    public String readNotification(
            Long messageId,
            Principal principal) {

        UserId userId = UserId.valueOf(userService.getUserByEmail(principal.getName()).getId());

        if (!checkRateLimit(userId)) {
            return "Rate limit exceeded";
        }

        boolean success = messageService.markMessageAsRead(messageId, userId);

        if (success) {
            MessageRead event = new MessageRead(
                    messageId,
                    ConversationId.fromConversationId(String.valueOf(messageService.getMessageConversationId(messageId))),
                    userId,
                    LocalDateTime.now()
            );


            notificationService.notifyMessageRead(event);

            return "Message marked as read successfully";
        } else {
            return "Failed to mark message as read";
        }
    }

    public boolean checkRateLimit(User id) {
        // Rate limiting check
        String rateLimiterKey = RATE_LIMITER_PREFIX + userId.getId();
        if (!rateLimiter.allowRequest(rateLimiterKey)) {
            return false;
        }

        return true;
    }
}

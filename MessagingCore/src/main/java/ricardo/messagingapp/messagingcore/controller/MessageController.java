package ricardo.messagingapp.messagingcore.controller;

import com.ricardo.auth.core.RateLimiter;
import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.AppRole;
import com.ricardo.auth.domain.user.User;
import com.ricardo.auth.domain.user.Username;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import ricardo.messagingapp.messagingcore.config.Properties;
import ricardo.messagingapp.messagingcore.domain.message.DTO.EditMessage;
import ricardo.messagingapp.messagingcore.domain.message.DTO.MessagePayload;
import ricardo.messagingapp.messagingcore.services.MessageService;

import java.security.Principal;
import java.util.UUID;

@Controller
public class MessageController {
    // as we'll use the same rate limiter database, this prefix will help us identify the rate limiters for WebSocket messages (Auth Rate limiting is done by the Auth Service, and also has its own rate limiter prefix)
    private static final String RATE_LIMITER_PREFIX = "ws:";

    private final MessageService messageService;
    private final UserService<User, AppRole, UUID> userService;
    private final RateLimiter rateLimiter;
    private final Properties properties;

    public MessageController(MessageService messageService, UserService<User, AppRole, UUID> userService, @Qualifier("generalRateLimiter") RateLimiter rateLimiter, Properties properties) {
        this.messageService = messageService;
        this.userService = userService;
        this.rateLimiter = rateLimiter;
        this.properties = properties;

        this.rateLimiter.changeSettings(properties.getRateLimiter().getMaxRequests(), properties.getRateLimiter().getTimeWindowMs());

    }

    @MessageMapping("/sendMessage")
    public String sendMessage(
            MessagePayload payload,
            Principal principal) {
        // Process the incoming message
        UUID userId = userService.getUserByEmail(principal.getName()).getId();

        if (isRateLimited(userId)) {
            return "Rate limit exceeded";
        }

        boolean success = messageService.sendMessage(payload, userId);

        if (success) {
            return "Message sent successfully";
        } else {
            return "Failed to send message";
        }
    }

    @MessageMapping("/editMessage")
    public String editMessage(
            EditMessage payload,
            Principal principal) {

        UUID userId = userService.getUserByEmail(principal.getName()).getId();

        if (isRateLimited(userId)) {
            return "Rate limit exceeded";
        }

        boolean success = messageService.updateMessage(payload.messageId(), payload.newDecryptedContent(), userId);

        if (success) {
            return "Message edited successfully";
        } else {
            return "Failed to edit message";
        }
    }


    @MessageMapping("/deleteMessage")
    public String deleteMessage(
            UUID messageId,
            Principal principal) {

        UUID userId = userService.getUserByEmail(principal.getName()).getId();

        if (isRateLimited(userId)) {
            return "Rate limit exceeded";
        }

        boolean success = messageService.deleteMessage(messageId, userId);

        if (success) {
            return "Message deleted successfully";
        } else {
            return "Failed to delete message";
        }
    }


    @MessageMapping("/readNotification")
    public String readNotification(
            Username conversation,
            Principal principal) {

        UUID userId = userService.getUserByEmail(principal.getName()).getId();

        if (isRateLimited(userId)) {
            return "Rate limit exceeded";
        }

        boolean success = messageService.markConversationAsRead(conversation, userId);

        if (success) {
            return "Message marked as read successfully";
        } else {
            return "Failed to mark message as read";
        }
    }

    public boolean isRateLimited(UUID id) {
        // Rate limiting check
        String rateLimiterKey = RATE_LIMITER_PREFIX + id.toString();
        return !rateLimiter.allowRequest(rateLimiterKey);
    }
}

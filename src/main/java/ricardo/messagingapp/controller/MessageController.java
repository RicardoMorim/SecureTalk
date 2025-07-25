package ricardo.messagingapp.controller;

import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.User;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import ricardo.messagingapp.domain.message.DTO.MessagePayload;
import ricardo.messagingapp.domain.message.UserId;
import ricardo.messagingapp.services.MessageService;

import java.security.Principal;

@Controller
public class MessageController {

    private final MessageService messageService;
    private final UserService<User, Long> userService;

    public MessageController(MessageService messageService, UserService<User, Long> userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    @MessageMapping("/sendMessage")
    @SendTo("/user/{userId}/queue/messages")
    public String sendMessage(
            MessagePayload payload,
            Principal principal) {
        // Process the incoming message
        UserId userId = UserId.valueOf(userService.getUserByEmail(principal.getName()).getId());

        boolean success = messageService.sendMessage(payload, userId);
        if (success) {
            return "Message sent successfully";
        } else {
            return "Failed to send message";
        }
    }



}

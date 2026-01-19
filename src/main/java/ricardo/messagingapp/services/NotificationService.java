package ricardo.messagingapp.services;

import org.springframework.stereotype.Service;
import ricardo.messagingapp.domain.message.DTO.EditMessage;
import ricardo.messagingapp.domain.message.DomainEvents.*;


@Service
public class NotificationService {

    public void notifyNewMessage(MessageSent event) {
    }

    public void notifyMessageDelivery(MessageDelivered event) {

    }

    public void notifyMessageEdited(MessageEdited event) {
    }

    public void notifyMessageRead(MessageRead event) {
    }

    public void notifyMessageDeletion(MessageDeleted event) {
    }

    public void broadcastMessageEdit(MessageEdited event) {
    }

    public void sendPushNotification(String receiverId, String senderId) {
    }

    public void sendEmailNotification(String receiverId, String senderId) {
    }

}

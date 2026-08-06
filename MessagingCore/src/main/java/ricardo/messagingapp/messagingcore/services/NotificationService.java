package ricardo.messagingapp.messagingcore.services;

import org.springframework.stereotype.Service;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.*;


@Service
public class NotificationService {

    public void notifyNewMessage(MessageSent event) {
    }

    public void notifyMessageDelivery(MessageDelivered event) {

    }


    public void notifyMessageRead(MessageRead event) {
    }

    public void notifyMessageEdit(MessageEdited event) {
    }

    public void sendPushNotification(String receiverId, String senderId) {
    }

    public void sendEmailNotification(String receiverId, String senderId) {
    }

}

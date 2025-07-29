package ricardo.messagingapp.services;

import ricardo.messagingapp.domain.message.DTO.EditNotification;
import ricardo.messagingapp.domain.message.DomainEvents.MessageDelivered;
import ricardo.messagingapp.domain.message.DomainEvents.MessageRead;
import ricardo.messagingapp.domain.message.DomainEvents.MessageSent;

public class NotificationService {

    public void notifyNewMessage(MessageSent event) {
    }

    public void notifyMessageDelivery(MessageDelivered event) {

    }

    public void notifyMessageRead(MessageRead event) {
    }

    public void broadcastMessageEdit(EditNotification event) {
    }

    public void sendPushNotification(String receiverId, String senderId) {
    }

    public void sendEmailNotification(String receiverId, String senderId) {
    }

}

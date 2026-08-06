package ricardo.messagingapp.messagingcore.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.MessageDelivered;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.MessageEdited;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.MessageRead;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.MessageSent;

@Service
@RequiredArgsConstructor
public class MetricsService {


    public void incrementMessageSentCount(MessageSent event){
    }

    public void incrementMessageDeliveredCount(MessageDelivered event){
    }

    public void updateMessageReadLatency(MessageRead event){

    }


    public void trackConversationActivity(String conversationId) {
    }

    public void trackEditFrequency(MessageEdited event) {
    }

    public void trackUserActivity(String userId) {
    }
}

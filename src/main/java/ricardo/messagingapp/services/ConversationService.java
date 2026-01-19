package ricardo.messagingapp.services;

import lombok.AllArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import ricardo.messagingapp.domain.conversation.Conversation;
import ricardo.messagingapp.repositories.ConversationRepository;

import java.util.UUID;

@AllArgsConstructor
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;


    public Conversation getConversationByMessageId(UUID messageId, UUID loggedUser){


        Conversation conversation = conversationRepository.findByMessageId(messageId);

        if (!conversation.hasParticipant(loggedUser)) throw new AuthorizationDeniedException("You are not a participant in the conversation");

        return conversation;
    }

}

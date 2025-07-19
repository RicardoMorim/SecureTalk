package ricardo.messagingapp.domain.message;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class UserId {
    private final Long id;

    private UserId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number.");
        }
        this.id = id;
    }


    public static UserId valueOf(Long id) {
        return new UserId(id);
    }

    @Override
    public String toString() {
        return "User Id is " + id;
    }
}

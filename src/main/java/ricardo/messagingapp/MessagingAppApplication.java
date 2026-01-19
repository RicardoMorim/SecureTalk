package ricardo.messagingapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@ComponentScan(basePackages = {
        "ricardo.messagingapp",
        "com.ricardo.auth"
})
public class MessagingAppApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        SpringApplication.run(MessagingAppApplication.class, args);
    }

}

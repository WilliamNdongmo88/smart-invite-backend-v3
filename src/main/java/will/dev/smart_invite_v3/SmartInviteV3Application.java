package will.dev.smart_invite_v3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SmartInviteV3Application {

	public static void main(String[] args) {
		SpringApplication.run(SmartInviteV3Application.class, args);
	}

}

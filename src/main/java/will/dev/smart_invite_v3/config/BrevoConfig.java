package will.dev.smart_invite_v3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import sendinblue.ApiClient;
import sendinblue.Configuration;

@Component
public class BrevoConfig {

    @Value("${app.brevo.api-key}")
    private String apiKey;

    @Bean
    public ApiClient brevoApiClient() {

        ApiClient defaultClient = Configuration.getDefaultApiClient();

        defaultClient.setApiKey(apiKey);

        return defaultClient;

    }

}
package com.example.portail_b2b.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {

    @Override
    public void sendAccountActivationEmail(AccountActivationEmail email) {
        log.info(
                """
                [DEV] Account activation email
                  to: {}
                  login: {}
                  link: {}
                  expires: {}
                """,
                email.recipientEmail(),
                email.login(),
                email.activationUrl(),
                email.expiresAt()
        );
    }
}

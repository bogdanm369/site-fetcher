package ro.abm.notifications;

import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.abm.beans.EmailBean;

import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    @Value("${config.mailNotifications.gmailAccount}")
    private String username; // full Gmail address
    @Value("${config.mailNotifications.appPassword}")
    private String appPassword; // appPassowrd
    private final Properties props = new Properties();

    private Session session;

    private final Deque<EmailBean> emailsToSend = new ConcurrentLinkedDeque<>();

    public void addEmailsToSend(List<EmailBean> emails) {
        for (EmailBean email : emails) {
            emailsToSend.addLast(email);
        }
    }

    public void addEmailToSend(EmailBean emailBean) {
        emailsToSend.addLast(emailBean);
    }

    @PostConstruct
    private void init() {
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // TLS
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });

        startEmailSender();
    }

    private void startEmailSender() {

        log.info("Starting EmailSender Service...");

        new Thread(() -> {
            while (true) {
                try {
                    if (!emailsToSend.isEmpty()) {

                        EmailBean email = null;
                        while (!emailsToSend.isEmpty()) {
                            try {
                                email = emailsToSend.pollLast();

                                Message message = new MimeMessage(session);
                                message.setFrom(new InternetAddress(username));
                                message.setRecipients(
                                        Message.RecipientType.TO,
                                        InternetAddress.parse(email.getSendTo())
                                );
                                message.setSubject(email.getSubject());
                                message.setText(email.getContent());

                                Transport.send(message);
                            } catch (Exception e) {
                                log.error("Error sending email to: {} , subject: {},  content: {}",
                                        email.getSendTo(), email.getSubject(), email.getContent(), e);
                                //Add it back for another attempt
                                if (email != null) {
                                    emailsToSend.addFirst(email);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error while sending emails", e);
                }

                try {
                    log.debug("EmailService, sleeping for 5 seconds");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.warn("EmailSender Service is interrupted");
                }
            }
        }).start();
        log.info("EmailSender Service has started");
    }
}

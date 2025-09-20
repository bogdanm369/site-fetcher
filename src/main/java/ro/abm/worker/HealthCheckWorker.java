package ro.abm.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import ro.abm.beans.EmailBean;
import ro.abm.config.SiteFetcherConfig;
import ro.abm.notifications.EmailSender;

import java.util.List;

public class HealthCheckWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckWorker.class);

    private final EmailSender emailSender;
    private final SiteFetcherConfig siteFetcherConfig;


    public HealthCheckWorker(EmailSender emailSender, SiteFetcherConfig siteFetcherConfig) {
        this.emailSender = emailSender;
        this.siteFetcherConfig = siteFetcherConfig;
    }

    @Override
    public void run() {

        List<String> sendEmailsTo = siteFetcherConfig.getHealthCheck().getSendEmailsTo();
        if (CollectionUtils.isEmpty(sendEmailsTo)) {
            log.warn("No HealthCheck $config.healthCheck.sendEmailsTo configured");
            return;
        }

        log.info("HealthCheck Service has started");

        while (true) {
            try {
                for (String email : sendEmailsTo) {
                    emailSender.addEmailToSend(EmailBean.builder()
                            .sendTo(email)
                            .subject(siteFetcherConfig.getEmailTemplates().getHealthCheckSubject())
                            .content(siteFetcherConfig.getEmailTemplates().getHealthCheckContent())
                            .build());
                }
                Thread.sleep(siteFetcherConfig.getHealthCheck().getRate() * 1000);
            } catch (Exception e) {
                log.error("Error while sending HealthCheck emails", e);
            }
        }
    }
}

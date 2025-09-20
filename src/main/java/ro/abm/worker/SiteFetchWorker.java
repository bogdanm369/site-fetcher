package ro.abm.worker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ro.abm.beans.EmailBean;
import ro.abm.config.SiteFetcherConfig;
import ro.abm.notifications.EmailSender;
import ro.abm.util.FileLogAppender;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;


public class SiteFetchWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SiteFetchWorker.class);

    private String CURRENT_HASH = "";

    private SiteFetcherConfig.SiteFetchDetails siteDetails;
    private EmailSender emailSender;
    private SiteFetcherConfig fetcherConfig;
    private WebDriver driver;

    public SiteFetchWorker(SiteFetcherConfig.SiteFetchDetails siteDetails, EmailSender emailSender, SiteFetcherConfig fetcherConfig) {
        this.siteDetails = siteDetails;
        this.emailSender = emailSender;
        this.fetcherConfig = fetcherConfig;
        initializeDriver();
        log.info("SiteFetchWorker initialized for {}", siteDetails.getName());
    }

    private void initializeDriver() {
        log.debug("Attempting to reinitialize driver for site {}", siteDetails.getName());
        try {
            if (driver != null) {
                driver.quit();
            }
        } catch (Exception e) {
            log.error("Error while attempting to reinitialize WebDriver for site : {}", siteDetails.getName(), e);
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(options);
    }

    @Override
    public void run() {

        while (true) {
            try {
                log.info("SiteFetchWorker fetching site {} ; selectors {}", siteDetails.getName(), siteDetails.getSelectors());

                driver.get(siteDetails.getUrl());
                String siteContent = driver.getPageSource();

                Document doc = Jsoup.parse(siteContent);

                String cssSelector = siteDetails.getSelectors().get(0);

                Elements titles = doc.select(cssSelector);

                StringBuilder selectorContents = new StringBuilder();

                for (Element title : titles) {
                    selectorContents.append(title.text());
                }

                if (hashChangedAndUpdate(getContentHash(selectorContents.toString()))) {

                    LocalDateTime now = LocalDateTime.now();
                    log.info("Site {} Selector {} CHANGED", siteDetails.getName(), siteDetails.getSelectors());

                    String content = String.format("url %s CHANGED", siteDetails.getUrl());

                    if (StringUtils.hasText(siteDetails.getBeforeTimeTrigger())) {
                        String[] hourAndMinutes = siteDetails.getBeforeTimeTrigger().split(":");
                        if (now.getHour() <= Integer.parseInt(hourAndMinutes[0])
                                && now.getMinute() <= Integer.parseInt(hourAndMinutes[1])) {

                            log.info("Site {} CHANGED; set hour/minutes : {} , current hour/minutes : {}",
                                    siteDetails.getUrl(), siteDetails.getBeforeTimeTrigger(), now.getHour() + ":" + now.getMinute());

                            for (String email : siteDetails.getSendEmailsTo()) {
                                emailSender.addEmailToSend(EmailBean.builder().sendTo(email).content(content).subject(fetcherConfig.getEmailTemplates().getSiteChangeSubject()).build());
                            }

                            if (siteDetails.isWriteLogToFile()) {
                                FileLogAppender.writeToLogFile(now, siteDetails.getName(), selectorContents.toString());
                            }
                        }
                    } else {
                        for (String email : siteDetails.getSendEmailsTo()) {
                            emailSender.addEmailToSend(EmailBean.builder().sendTo(email).content(content).subject(fetcherConfig.getEmailTemplates().getSiteChangeSubject()).build());
                        }
                        if (siteDetails.isWriteLogToFile()) {
                            FileLogAppender.writeToLogFile(now, siteDetails.getName(), selectorContents.toString());
                        }
                    }
                } else {
                    log.debug("Site {} Selector {} NO CHANGE", siteDetails.getName(), siteDetails.getSelectors());
                }

            } catch (Exception e) {
                log.error("Error while attempting to fetch site : {}", siteDetails.getName(), e);
                try {
                    driver.quit();
                } finally {
                    initializeDriver();
                }
            }

            try {
                log.debug("Site {} , Sleeping for {} seconds", siteDetails.getName(), siteDetails.getScanInterval());
                Thread.sleep(siteDetails.getScanInterval() * 1000);
            } catch (InterruptedException e) {
                log.error("Error while sleeping for site : {}", siteDetails.getName(), e);
            }
        }
    }

    private String getContentHash(String content) throws Exception {

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));

        // Convert byte array to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private boolean hashChangedAndUpdate(String newHash) {

        boolean changed = false;

        if (StringUtils.isEmpty(CURRENT_HASH)) {
            CURRENT_HASH = newHash;
            changed = false;
        } else {
            changed = !CURRENT_HASH.equals(newHash);
            CURRENT_HASH = newHash;
        }

        return changed;
    }
}
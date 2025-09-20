package ro.abm.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "config")
@Data
public class SiteFetcherConfig {

    private static final Logger log = LoggerFactory.getLogger(SiteFetcherConfig.class);

    private List<String> mainSites;
    private Map<String, SiteFetchDetails> sites = new HashMap<>();
    private HealthCheck healthCheck;
    private EmailTemplates emailTemplates;

    @Data
    public static class EmailTemplates {
        private String siteChangeSubject;
        private String healthCheckSubject;
        private String healthCheckContent;
    }

    @Data
    public static class HealthCheck {
        private int rate;
        private List<String> sendEmailsTo;
    }

    @Data
    public static class SiteFetchDetails {
        private String name;
        private String url;
        private List<String> selectors;
        private int scanInterval;
        private String beforeTimeTrigger;
        private boolean writeLogToFile;
        private List<String> sendEmailsTo;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        if (CollectionUtils.isEmpty(mainSites)) {
            sb.append("mainSites is null or empty;");
        } else {
            sb.append(String.format("mainSites = [%s]\n", mainSites));
            for (String site : mainSites) {
                SiteFetchDetails siteDetails = sites.get(site);
                sb.append(String.format("Site %s , URL %s , selectors %s , scanInterval %s, beforeTimeTrigger %s\n",
                        site, siteDetails.url, siteDetails.selectors, siteDetails.scanInterval, siteDetails.beforeTimeTrigger));
            }
        }
        return sb.toString();
    }
}


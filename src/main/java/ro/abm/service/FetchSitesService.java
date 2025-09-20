package ro.abm.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ro.abm.config.SiteFetcherConfig;
import ro.abm.notifications.EmailSender;
import ro.abm.worker.HealthCheckWorker;
import ro.abm.worker.SiteFetchWorker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class FetchSitesService {

    private static final Logger log = LoggerFactory.getLogger(FetchSitesService.class);

    private ExecutorService executor;

    @Autowired
    private SiteFetcherConfig siteFetcherConfig;

    @Autowired
    private EmailSender emailSender;

    @PostConstruct
    private void init() {

        log.info("Initializing SiteFetcher");
        log.info("SiteFetcherConfig: {}", siteFetcherConfig.toString());

        List<String> sitesToScan = siteFetcherConfig.getMainSites();
        executor = Executors.newFixedThreadPool(sitesToScan.size() + 1);
        Map<String, SiteFetcherConfig.SiteFetchDetails> sites = siteFetcherConfig.getSites();

        executor.submit(new HealthCheckWorker(emailSender, siteFetcherConfig));

        for (String siteName : sitesToScan) {
            SiteFetcherConfig.SiteFetchDetails siteDetails = sites.get(siteName);
            siteDetails.setName(siteName);

            SiteFetchWorker sfw = new SiteFetchWorker(siteDetails, emailSender, siteFetcherConfig);
            executor.submit(sfw);
        }
    }
}

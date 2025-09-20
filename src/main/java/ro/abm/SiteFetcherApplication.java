package ro.abm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ro.abm.config.SiteFetcherConfig;

@SpringBootApplication
@EnableConfigurationProperties(SiteFetcherConfig.class)
public class SiteFetcherApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiteFetcherApplication.class, args);
    }
}
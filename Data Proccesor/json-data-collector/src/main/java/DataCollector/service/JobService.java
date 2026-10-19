package DataCollector.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    private static final Logger log =
            LoggerFactory.getLogger(JobService.class);

    private final ApiService apiService;
    private final JsonFileService jsonFileService;

    public JobService(
            ApiService apiService,
            JsonFileService jsonFileService
    ) {
        this.apiService = apiService;
        this.jsonFileService = jsonFileService;
    }

    public void runOnce() {

        log.info("========================================");
        log.info("eShopaid Data Processor Started");
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        String[] serviceMethods = {
                "PullPurchaseReturn",
                "PullSalesReturn",
                "PullSales",
                "PullPurchase"
        };

        int successCount = 0;
        int failureCount = 0;

        for (String serviceMethod : serviceMethods) {

            log.info("========================================");
            log.info("Processing: {}", serviceMethod);
            log.info("========================================");

            try {

                String json =
                        apiService.fetchJson(serviceMethod);

                log.info(
                        "Received response [{}]: {} characters",
                        serviceMethod,
                        json.length()
                );

                boolean processed =
                        jsonFileService.processResponse(
                                serviceMethod,
                                json
                        );

                if (processed) {
                    successCount++;
                } else {
                    failureCount++;
                }

            } catch (Exception e) {

                failureCount++;

                log.error(
                        "API processing failed: {}",
                        serviceMethod,
                        e
                );

                log.info("Continuing with next API...");
            }
        }

        long elapsed =
                System.currentTimeMillis() - startTime;

        log.info("========================================");
        log.info("Data Processor Finished");
        log.info("Successful APIs : {}", successCount);
        log.info("Failed APIs     : {}", failureCount);
        log.info("Total Time      : {} ms", elapsed);
        log.info("========================================");
    }
}
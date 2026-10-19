package DataCollector;

import DataCollector.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DemoApplication {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DemoApplication.class
            );

    public static void main(String[] args) {

        ConfigurableApplicationContext context =
                null;

        int exitCode = 0;

        try {

            log.info("========================================");
            log.info("eShopaid Data Processor Started");
            log.info("========================================");

            context =
                    SpringApplication.run(
                            DemoApplication.class,
                            args
                    );

            JobService jobService =
                    context.getBean(
                            JobService.class
                    );

            jobService.runOnce();

            log.info(
                    "Data Processor completed"
            );

        } catch (Exception e) {

            exitCode = 1;

            log.error(
                    "Data Processor failed",
                    e
            );

        } finally {

            if (context != null) {

                log.info(
                        "Shutting down Data Processor"
                );

                int finalExitCode =
                        exitCode;

                SpringApplication.exit(
                        context,
                        () -> finalExitCode
                );
            }
        }

        System.exit(exitCode);
    }
}
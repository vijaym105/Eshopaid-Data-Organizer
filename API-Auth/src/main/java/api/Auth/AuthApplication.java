package api.Auth;

import api.Auth.Service.AuthenticationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AuthApplication {

    private static final Logger log =
            LoggerFactory.getLogger(AuthApplication.class);

    public static void main(String[] args) {

        ConfigurableApplicationContext context = null;

        int exitCode = 0;

        try {

            log.info("========================================");
            log.info("eShopaid Authentication Job Started");
            log.info("========================================");

       
            context = SpringApplication.run(
                    AuthApplication.class,
                    args
            );

          
            AuthenticationService authenticationService =
                    context.getBean(AuthenticationService.class);

        
            authenticationService.authenticate();

            log.info(
                    "Authentication completed successfully"
            );

        } catch (Exception e) {

            exitCode = 1;

            log.error(
                    "Authentication job failed",
                    e
            );

        } finally {

            if (context != null) {

                log.info(
                        "Shutting down authentication application"
                );

                final int applicationExitCode = exitCode;

                SpringApplication.exit(
                        context,
                        () -> applicationExitCode
                );
            }
        }

        System.exit(exitCode);
    }
}
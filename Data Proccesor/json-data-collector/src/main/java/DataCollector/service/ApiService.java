package DataCollector.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ApiService {

    private static final Logger log =
            LoggerFactory.getLogger(ApiService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${purchase.return.api.url}")
    private String apiUrl;

    @Value("${purchase.return.token.file}")
    private String tokenFile;

    public ApiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        HttpClient httpClient =
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        this.restClient =
                RestClient.builder()
                        .requestFactory(requestFactory)
                        .build();
    }

    public String fetchJson(String serviceMethod) throws IOException {

        String currentDate =
                LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        log.info("----------------------------------------");
        log.info("Calling eShopaid API");
        log.info("Service Method : {}", serviceMethod);
        log.info("From Date      : {}", currentDate);
        log.info("To Date        : {}", currentDate);
        log.info("Page No        : 1");

        String token = readToken();

        log.info("Authentication token loaded successfully");

        return restClient
                .post()
                .uri(apiUrl)
                .header("SERVICEMETHODNAME", serviceMethod)
                .header("HEADERFROMDATE", currentDate)
                .header("HEADERTODATE", currentDate)
                .header("HEADERPAGENO", "1")
                .header("X-Auth-Token", "Bearer " + token)
                .header("Accept", "*/*")
                .header("User-Agent", "PostmanRuntime/2.10.0")
                .header("Cache-Control", "no-cache")
                .body("")
                .exchange((request, response) -> {

                    log.info(
                            "HTTP Status [{}]: {}",
                            serviceMethod,
                            response.getStatusCode()
                    );

                    byte[] responseBytes =
                            response.getBody().readAllBytes();

                    String responseBody =
                            new String(
                                    responseBytes,
                                    StandardCharsets.UTF_8
                            );

                    log.info(
                            "Response size [{}]: {} characters",
                            serviceMethod,
                            responseBody.length()
                    );

                    return responseBody;
                });
    }

    private String readToken() throws IOException {

        Path path = Path.of(tokenFile);

        if (!Files.exists(path)) {
            throw new IOException(
                    "Token file not found: "
                            + path.toAbsolutePath()
            );
        }

        String json =
                Files.readString(
                        path,
                        StandardCharsets.UTF_8
                );

        JsonNode root =
                objectMapper.readTree(json);

        JsonNode tokenNode =
                root.get("token");

        if (tokenNode == null
                || tokenNode.isNull()
                || tokenNode.asText().isBlank()) {

            throw new IOException(
                    "Token field not found in: "
                            + path.toAbsolutePath()
            );
        }

        return tokenNode.asText();
    }
}
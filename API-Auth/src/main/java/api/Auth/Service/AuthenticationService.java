package api.Auth.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

@Service
public class AuthenticationService {

        private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

        private final ObjectMapper objectMapper;
        private final RestClient restClient;

        @Value("${auth.api.url}")
        private String apiUrl;

        @Value("${auth.service-method}")
        private String serviceMethod;

        @Value("${auth.username}")
        private String username;

        @Value("${auth.password}")
        private String password;

        @Value("${auth.token-file}")
        private String tokenFile;

        public AuthenticationService(ObjectMapper objectMapper) {

                this.objectMapper = objectMapper;

                HttpClient httpClient = HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .build();

                JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

                this.restClient = RestClient.builder()
                                .requestFactory(requestFactory)
                                .build();
        }

        public void authenticate() throws IOException {

                log.info("========================================");
                log.info("Starting authentication request");
                log.info("========================================");

                log.info("Authentication API: {}", apiUrl);
                log.info("Service method: {}", serviceMethod);

                String response = restClient
                                .post()
                                .uri(apiUrl)

                                .header("Username", username)
                                .header("Password", password)
                                .header("SERVICEMETHODNAME", serviceMethod)

                                .header("Accept", "*/*")
                                .header("User-Agent", "PostmanRuntime/2.10.0")
                                .header("Cache-Control", "no-cache")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)

                                .body("")

                                .exchange((request, responseMessage) -> {

                                        log.info(
                                                        "HTTP Status: {}",
                                                        responseMessage.getStatusCode());

                                        log.info(
                                                        "Request Headers: {}",
                                                        request.getHeaders());

                                        log.info(
                                                        "Response Headers: {}",
                                                        responseMessage.getHeaders());

                                        byte[] responseBytes = responseMessage
                                                        .getBody()
                                                        .readAllBytes();

                                        String body = new String(
                                                        responseBytes,
                                                        StandardCharsets.UTF_8);

                                        log.info("Response Body: {}", body);

                                        return body;
                                });

                if (response == null || response.isBlank()) {

                        throw new IOException(
                                        "Authentication API returned an empty response");
                }

                log.info(
                                "Authentication response received. Response size: {} characters",
                                response.length());

                String accessToken = extractAccessToken(response);

                if (accessToken == null || accessToken.isBlank()) {

                        throw new IOException(
                                        "Access token was not found in authentication response");
                }

                log.info("Access token successfully extracted");

                /*
                 * Save token to token/token.json
                 */
                saveToken(accessToken);

                log.info("Authentication token file updated successfully");
        }

        private String extractAccessToken(String response)
                        throws IOException {

                JsonNode root = objectMapper.readTree(response);

                JsonNode responseNode = root.get("Response");

                if (responseNode == null || !responseNode.isObject()) {

                        throw new IOException(
                                        "Authentication response does not contain a valid 'Response' object");
                }

                JsonNode resultNode = responseNode.get("Result");

                if (resultNode != null) {

                        String result = resultNode.asText();

                        log.info(
                                        "Authentication result: {}",
                                        result);

                        if (!"SUCCESS".equalsIgnoreCase(result)) {

                                throw new IOException(
                                                "Authentication API returned result: "
                                                                + result);
                        }
                }

                JsonNode accessTokenNode = responseNode.get("Access_Token");

                if (accessTokenNode == null
                                || accessTokenNode.isNull()
                                || accessTokenNode.asText().isBlank()) {

                        return null;
                }

                return accessTokenNode.asText();
        }

        private void saveToken(String token) throws IOException {

                Path tokenPath = Path.of(tokenFile).toAbsolutePath().normalize();

                log.info("Saving token to: {}", tokenPath);

                Path parent = tokenPath.getParent();

                if (parent == null) {
                        throw new IOException("Invalid token file path: " + tokenPath);
                }

                log.info("Token directory: {}", parent);
                log.info("Token directory exists: {}", Files.exists(parent));
                log.info("Token directory is directory: {}", Files.isDirectory(parent));
                log.info("Token directory writable: {}", Files.isWritable(parent));

                if (!Files.exists(parent)) {
                        Files.createDirectories(parent);
                }

                String tokenJson = """
                                {
                                  "token": "%s",
                                  "generatedAt": "%s"
                                }
                                """.formatted(
                                token,
                                java.time.LocalDateTime.now());

                Files.writeString(
                                tokenPath,
                                tokenJson,
                                StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING);

                log.info("Token saved successfully: {}", tokenPath);
        }
}

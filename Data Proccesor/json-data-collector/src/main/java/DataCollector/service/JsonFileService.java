package DataCollector.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class JsonFileService {

        private static final Logger log = LoggerFactory.getLogger(JsonFileService.class);

        private final ObjectMapper objectMapper;
        private final Path baseOutputDirectory;

        public JsonFileService(
                        ObjectMapper objectMapper,
                        @Value("${processor.output.directory}") String outputDirectory) throws IOException {

                this.objectMapper = objectMapper;

                this.baseOutputDirectory = Path.of(outputDirectory)
                                .toAbsolutePath()
                                .normalize();

               
                Files.createDirectories(baseOutputDirectory);

            
                List<String> serviceMethods = List.of(
                                "PullPurchaseReturn",
                                "PullSalesReturn",
                                "PullSales",
                                "PullPurchase");

                for (String serviceMethod : serviceMethods) {

                        Path apiFolder = baseOutputDirectory.resolve(serviceMethod);

                        Files.createDirectories(apiFolder);

                        log.info("API output folder ready: {}", apiFolder);
                }

                log.info("Processed data directory initialized: {}", baseOutputDirectory);
        }

        // THIS IS THE METHOD YOUR JobService IS LOOKING FOR
        public boolean processResponse(
                        String serviceMethod,
                        String json) throws IOException {

                JsonNode root = objectMapper.readTree(json);

                JsonNode response = root.get("Response");

                if (response == null || !response.isObject()) {
                        log.error("[{}] Response object not found", serviceMethod);
                        return false;
                }

                String result = response.path("Result").asText();

                log.info(
                                "[{}] API Result: {}",
                                serviceMethod,
                                result);

                // ----------------------------------------
                // SUCCESS
                // ----------------------------------------

                if ("SUCCESS".equalsIgnoreCase(result)) {
                        return processSuccessfulResponse(
                                        serviceMethod,
                                        response);
                }

                // ----------------------------------------
                // FAILURE
                // ----------------------------------------

                String failureReason = extractFailureReason(response);

                log.warn(
                                "[{}] API returned FAILURE: {}",
                                serviceMethod,
                                failureReason);

                // No data is not a technical failure
                if ("No data found".equalsIgnoreCase(
                                failureReason.trim())) {

                        log.info(
                                        "[{}] No data available for current date",
                                        serviceMethod);

                        return true;
                }

                log.error(
                                "[{}] API failure reason: {}",
                                serviceMethod,
                                failureReason);

                return false;
        }

        // ----------------------------------------
        // PROCESS SUCCESS RESPONSE
        // ----------------------------------------

        private boolean processSuccessfulResponse(
                        String serviceMethod,
                        JsonNode response) throws IOException {

                JsonNode wsTransaction = response.get("WsTransaction");

                if (wsTransaction == null ||
                                !wsTransaction.isObject()) {

                        log.error(
                                        "[{}] WsTransaction object not found",
                                        serviceMethod);

                        return false;
                }

                // ----------------------------------------
                // PAGE SUMMARY
                // ----------------------------------------

                JsonNode pageSummary = wsTransaction.get("PageSummary");

                if (pageSummary != null) {

                        log.info(
                                        "[{}] PageCount: {}",
                                        serviceMethod,
                                        pageSummary.path("PageCount").asText());

                        log.info(
                                        "[{}] PageNo: {}",
                                        serviceMethod,
                                        pageSummary.path("PageNo").asText());

                        log.info(
                                        "[{}] NodeCount: {}",
                                        serviceMethod,
                                        pageSummary.path("NodeCount").asText());
                }

                // ----------------------------------------
                // FIND RECORD ARRAY
                // ----------------------------------------

                String arrayName = getArrayName(serviceMethod);

                JsonNode records = wsTransaction.get(arrayName);

                if (records == null ||
                                !records.isArray()) {

                        log.warn(
                                        "[{}] Expected array '{}' was not found",
                                        serviceMethod,
                                        arrayName);

                        return false;
                }

                // ----------------------------------------
                // NO RECORDS
                // ----------------------------------------

                if (records.isEmpty()) {

                        log.info(
                                        "[{}] No records found",
                                        serviceMethod);

                        return true;
                }

                log.info(
                                "[{}] Records found: {}",
                                serviceMethod,
                                records.size());

                // ----------------------------------------
                // CREATE SERVICE DIRECTORY
                // ----------------------------------------

                Path serviceDirectory = baseOutputDirectory
                                .resolve(serviceMethod)
                                .normalize();

                Files.createDirectories(serviceDirectory);

                // ----------------------------------------
                // TIMESTAMP
                // ----------------------------------------

                String timestamp = LocalDateTime.now()
                                .format(
                                                DateTimeFormatter.ofPattern(
                                                                "yyyy-MM-dd_HH-mm-ss"));

                // ----------------------------------------
                // SAVE EACH RECORD
                // ----------------------------------------

                int written = 0;

                for (JsonNode record : records) {

                        written++;

                        String identifier = getIdentifier(
                                        record,
                                        written);

                        String safeIdentifier = identifier.replaceAll(
                                        "[^a-zA-Z0-9._-]",
                                        "_");

                        if (safeIdentifier.isBlank()) {
                                safeIdentifier = String.valueOf(written);
                        }

                        String fileName = serviceMethod
                                        + "-"
                                        + written
                                        + "_"
                                        + safeIdentifier
                                        + "_"
                                        + timestamp
                                        + ".json";

                        Path file = serviceDirectory.resolve(fileName);

                        String formattedJson = objectMapper
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(record);

                        Files.writeString(
                                        file,
                                        formattedJson,
                                        StandardCharsets.UTF_8,
                                        StandardOpenOption.CREATE_NEW);

                        log.info(
                                        "[{}] Saved record {} -> {}",
                                        serviceMethod,
                                        written,
                                        file.toAbsolutePath());
                }

                log.info(
                                "[{}] Total files written: {}",
                                serviceMethod,
                                written);

                return true;
        }

        // ----------------------------------------
        // RESPONSE ARRAY NAME
        // ----------------------------------------

        private String getArrayName(
                        String serviceMethod) {

                return switch (serviceMethod) {

                        case "PullPurchaseReturn" ->
                                "Purchase";

                        case "PullSalesReturn" ->
                                "SalesReturn";

                        case "PullSales" ->
                                "Sales";

                        case "PullPurchase" ->
                                "Purchase";

                        default ->
                                serviceMethod;
                };
        }

        // ----------------------------------------
        // GET RECORD IDENTIFIER
        // ----------------------------------------

        private String getIdentifier(
                        JsonNode record,
                        int index) {

                String identifier = record.path("DocNumber").asText();

                if (identifier.isBlank()) {

                        identifier = record.path("BillNumber").asText();
                }

                if (identifier.isBlank()) {

                        identifier = record.path("StagingID").asText();
                }

                if (identifier.isBlank()) {

                        identifier = String.valueOf(index);
                }

                return identifier;
        }

        // ----------------------------------------
        // EXTRACT FAILURE REASON
        // ----------------------------------------

        private String extractFailureReason(
                        JsonNode response) {

                JsonNode failureReason = response.get("FailureReason");

                if (failureReason == null) {
                        return "Unknown API failure";
                }

                // Example:
                // "FailureReason": "No data found"

                if (failureReason.isTextual()) {
                        return failureReason.asText();
                }

                // Example:
                //
                // "FailureReason": {
                // "Response": {
                // "Result": "FAILURE",
                // "FailureReason": "No data found"
                // }
                // }

                JsonNode nestedResponse = failureReason.get("Response");

                if (nestedResponse != null) {

                        JsonNode nestedReason = nestedResponse.get(
                                        "FailureReason");

                        if (nestedReason != null) {

                                if (nestedReason.isTextual()) {
                                        return nestedReason.asText();
                                }

                                return nestedReason.toString();
                        }
                }

                return failureReason.toString();
        }
}
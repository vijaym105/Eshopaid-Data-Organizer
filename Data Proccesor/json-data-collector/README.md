# JSON Data Collector - Java / Spring Boot

A small Spring Boot 4 + Java 21 application that calls an external API using an HTTP Bearer token, receives JSON, saves the raw response, and splits an array of records into separate JSON files.

## Requirements

- Java 21
- Windows (for the Task Scheduler setup described below)
- Maven wrapper included (`mvnw.cmd`)

## 1. Configure the real API

Set these environment variables in PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
$env:EXTERNAL_API_URL="https://your-api.example.com/orders"
$env:EXTERNAL_API_TOKEN="YOUR_REAL_BEARER_TOKEN"
Invoke-WebRequest http://localhost:8080/run
```

The application sends:

```http
GET https://your-api.example.com/orders
Authorization: Bearer YOUR_REAL_BEARER_TOKEN
Accept: application/json
```


## 2. Run normally

```powershell
.\mvnw.cmd spring-boot:run
```

Check:

```text
http://localhost:8080/health
```

To manually run the collector:

```text
http://localhost:8080/run
```

## 3. Test without a real API

Start the app with the `demo` profile:

```powershell
$env:SPRING_PROFILES_ACTIVE="demo"
$env:EXTERNAL_API_URL="http://localhost:8080/mock/orders"
$env:EXTERNAL_API_TOKEN="DEMO_TOKEN"
.\mvnw.cmd spring-boot:run
```

Then open:

```text
http://localhost:8080/run
```

It will fetch three demo orders and create files in `data/`.

## 4. One-shot mode for Windows Task Scheduler

Build the JAR:

```powershell
.\mvnw.cmd clean package
```

Run one collection job and exit:

```powershell
java -jar target\demo-0.0.1-SNAPSHOT.jar --run-once
```

This is the mode intended for Windows Task Scheduler. Each scheduled launch does one fetch/process/write cycle and exits. The scheduler controls the repetition.

### Task Scheduler

Program:

```text
C:\Program Files\Java\jdk-21\bin\java.exe
```

Arguments:

```text
-jar "C:\path\to\demo\target\demo-0.0.1-SNAPSHOT.jar" --run-once
```

Start in:

```text
C:\path\to\demo
```

Set the same `EXTERNAL_API_URL` and `EXTERNAL_API_TOKEN` as Windows environment variables, or configure them as system environment variables so the scheduled task can read them.

## Output

The application creates:

```text
data/
├── raw-<timestamp>.json
├── record-1001.json
├── record-1002.json
└── record-1003.json
```

If the response is a JSON array, the array elements are split directly. If it is an object containing `orders`, `data`, `records`, or `items`, that array is split.

## Important

The current splitter uses Jackson's tree model, so the complete response is loaded into memory. For a 10-15 MB response this is normally manageable, but if the payload becomes very large, switch to Jackson streaming for lower memory usage.

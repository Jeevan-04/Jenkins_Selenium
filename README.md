# Atithya Selenium Tests

This module provides a Maven-based Selenium smoke suite for the Atithya web app.

## Run locally

```bash
cd selenium-tests
mvn test
```

## Configuration

The tests use these defaults:

- `BASE_URL` = `https://jeevan-04.github.io/Atithya`
- `CHROME_BINARY` = `/Applications/Chromium.app/Contents/MacOS/Chromium`

Override them when needed:

```bash
BASE_URL=http://localhost:8080 \
CHROME_BINARY=/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome \
mvn test
```

## Jenkins Setup

Use the `jenkins.war` file in the parent folder to start Jenkins locally:

```bash
cd /Users/jeevan/Desktop/Atithya_test
java -jar jenkins.war --httpPort=8080
```

Then open Jenkins, unlock it, and install the suggested plugins.

Create a Pipeline job that points to this GitHub repository and uses the `Jenkinsfile` in this folder. The pipeline already runs on a weekly cron schedule (`H H * * 1`), so Jenkins will build it once per week without any extra UI trigger.

For the job configuration:

1. Set the branch to `main`.
2. Use the Pipeline script from SCM option or a Multibranch Pipeline.
3. Make sure the agent can run Java 17+, Maven, and a Chrome-compatible browser.
4. Keep `SELENIUM_HEADLESS=true` for CI runs.

The suite is designed for Jenkins too: install Java, a Chromium-compatible browser, then run `mvn test` from this folder.
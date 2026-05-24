package com.atithya.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class BaseWebTest {
    private static final String DEFAULT_BASE_URL = "https://jeevan-04.github.io/Atithya";
    private static final String DEFAULT_CHROMIUM_BINARY = "/Applications/Chromium.app/Contents/MacOS/Chromium";
    private static final String DEFAULT_CHROME_BINARY = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

    protected WebDriver driver;

    @BeforeEach
    void setUpDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        String chromeBinary = firstExistingPath(
                valueFor("chromeBinary", "CHROME_BINARY", null),
                valueFor("chromiumBinary", "CHROMIUM_BINARY", null),
            DEFAULT_CHROME_BINARY,
            DEFAULT_CHROMIUM_BINARY
        );
        if (chromeBinary != null) {
            options.setBinary(chromeBinary);
        }

        boolean headless = Boolean.parseBoolean(valueFor("headless", "SELENIUM_HEADLESS", "false"));
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--window-size=1440,1200", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        if (headless) {
            options.addArguments("--headless=new");
        }

        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void openHomePage() {
        driver.get(valueFor("baseUrl", "BASE_URL", DEFAULT_BASE_URL));
        waitForPageReady();
    }

    protected void assertBodyContains(String expectedText) {
        String bodyText = driver.findElement(By.tagName("body")).getText().toLowerCase(Locale.ROOT);
        assertTrue(
                bodyText.contains(expectedText.toLowerCase(Locale.ROOT)),
                () -> "Expected page body to contain: " + expectedText + "\nActual body text:\n" + bodyText
        );
    }

        protected void assertPageTitleContains(String expectedText) {
        String title = driver.getTitle();
        assertTrue(
            title.toLowerCase(Locale.ROOT).contains(expectedText.toLowerCase(Locale.ROOT)),
            () -> "Expected page title to contain: " + expectedText + "\nActual page title:\n" + title
        );
        }

        protected void assertVisibleTextPresent(String expectedText) {
        String expected = expectedText.replace("→", "").trim().toLowerCase(Locale.ROOT);
            Boolean found = new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                "const expected = arguments[0];"
                    + "const seen = new Set();"
                    + "function collect(root) {"
                    + "  if (!root || seen.has(root)) return [];"
                    + "  seen.add(root);"
                    + "  const nodes = Array.from(root.querySelectorAll ? root.querySelectorAll('*') : []);"
                    + "  let results = nodes;"
                    + "  for (const node of nodes) { if (node.shadowRoot) { results = results.concat(collect(node.shadowRoot)); } }"
                    + "  return results;"
                    + "}"
                    + "function normalize(value) { return (value || '').replace(/→/g, '').replace(/\\s+/g, ' ').trim().toLowerCase(); }"
                    + "const nodes = collect(document).filter(node => {"
                    + "  const text = normalize(node.innerText || node.textContent || '');"
                    + "  const aria = normalize(node.getAttribute && node.getAttribute('aria-label'));"
                    + "  const label = normalize(node.getAttribute && node.getAttribute('label'));"
                    + "  return text === expected || text.includes(expected) || aria === expected || aria.includes(expected) || label === expected || label.includes(expected);"
                    + "});"
                    + "return nodes.length > 0;",
                expected
            ));
        assertTrue(
            Boolean.TRUE.equals(found),
            () -> "Expected visible text to contain: " + expectedText
        );
        }

    protected String bodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    protected void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    protected void scrollDown() {
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, Math.max(500, window.innerHeight * 0.75));");
        pause(350);
    }

    protected void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
        pause(500);
    }

    protected void tapOutsideDialog() {
        ((JavascriptExecutor) driver).executeScript(
                "document.elementFromPoint(Math.floor(window.innerWidth * 0.08), Math.floor(window.innerHeight * 0.08))"
                        + ".dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true, view:window}));"
        );
        pause(500);
    }

    protected void captureScreenshot(String name) {
        if (!(driver instanceof TakesScreenshot)) {
            return;
        }

        Path outputDir = Path.of(valueFor("artifactDir", "ARTIFACT_DIR", "target/selenium-artifacts"));
        try {
            Files.createDirectories(outputDir);
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path destination = outputDir.resolve(name + ".png");
            Files.copy(source.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (WebDriverException browserGone) {
            System.out.println("[screenshot skipped] " + name + " -> " + browserGone.getClass().getSimpleName() + ": " + browserGone.getMessage());
        } catch (IOException ioException) {
            throw new IllegalStateException("Unable to capture screenshot: " + name, ioException);
        }
    }

    protected void waitForBodyContains(String expectedText) {
        String needle = expectedText.toLowerCase(Locale.ROOT);
        new WebDriverWait(driver, Duration.ofSeconds(4)).until(d -> {
            String actual = d.findElement(By.tagName("body")).getText().toLowerCase(Locale.ROOT);
            return actual.contains(needle);
        });
    }

    protected WebElement findVisibleElement(By by) {
        List<WebElement> elements = driver.findElements(by);
        for (WebElement element : elements) {
            if (element.isDisplayed()) {
                return element;
            }
        }
        throw new IllegalStateException("No visible element found for locator: " + by);
    }

    protected WebElement findVisibleElement(By... locators) {
        for (By locator : locators) {
            List<WebElement> elements = driver.findElements(locator);
            for (WebElement element : elements) {
                if (element.isDisplayed()) {
                    return element;
                }
            }
        }
        throw new IllegalStateException("No visible element found for the supplied locators");
    }

    protected void clickVisibleText(String text) {
        clickFlutterButton(text);
    }

    protected boolean clickVisibleTextIfPresent(String text) {
        try {
            clickVisibleText(text);
            pause(600);
            return true;
        } catch (RuntimeException missing) {
            return false;
        }
    }

    protected void clickTextNode(String text) {
        Boolean clicked = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "const target = arguments[0].toLowerCase();"
                + "const seen = new Set();"
                + "function collect(root) {"
                + "  if (!root || seen.has(root)) return [];"
                + "  seen.add(root);"
                + "  const nodes = Array.from(root.querySelectorAll ? root.querySelectorAll('*') : []);"
                + "  let results = nodes;"
                + "  for (const node of nodes) { if (node.shadowRoot) { results = results.concat(collect(node.shadowRoot)); } }"
                + "  return results;"
                + "}"
                + "const candidates = collect(document).filter(node => {"
                + "  const text = (node.innerText || node.textContent || '').replace(/\\s+/g, ' ').trim().toLowerCase();"
                + "  return text && text.includes(target);"
                + "}).filter(node => { try { return node.offsetParent !== null; } catch (e) { return true; } });"
                        + "candidates.sort((a, b) => ((a.innerText || a.textContent || '').length - (b.innerText || b.textContent || '').length));"
                        + "const match = candidates[0];"
                        + "if (!match) return false;"
                        + "const clickable = match.closest('flt-semantics[role=\\'button\\'], flt-semantics-placeholder[role=\\'button\\'], button, [role=\\'button\\']') || match;"
                        + "clickable.dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true, view:window}));"
                        + "clickable.dispatchEvent(new MouseEvent('mouseup', {bubbles:true, cancelable:true, view:window}));"
                        + "clickable.click();"
                        + "return true;",
                text
        );
        if (!Boolean.TRUE.equals(clicked)) {
            throw new IllegalStateException("No visible text node matched: " + text);
        }
    }

    protected void clickElement(WebElement element) {
        try {
            element.click();
        } catch (RuntimeException clickException) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));"
                            + "arguments[0].dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }));"
                            + "arguments[0].click();",
                    element
            );
        }
    }

    protected void clickFlutterButton(String label) {
        String expected = label.replace("→", "").replace("\u2192", "").trim().toLowerCase(Locale.ROOT);
        Boolean clicked = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "const expected = arguments[0];"
                        + "const seen = new Set();"
                        + "function collect(root) {"
                        + "  if (!root || seen.has(root)) return [];"
                        + "  seen.add(root);"
                        + "  const nodes = Array.from(root.querySelectorAll ? root.querySelectorAll('*') : []);"
                        + "  let results = nodes;"
                        + "  for (const node of nodes) { if (node.shadowRoot) { results = results.concat(collect(node.shadowRoot)); } }"
                        + "  return results;"
                        + "}"
                        + "function normalize(value) { return (value || '').replace(/→/g, '').replace(/\\s+/g, ' ').trim().toLowerCase(); }"
                        + "const nodes = collect(document).filter(node => {"
                        + "  const text = normalize(node.innerText || node.textContent || '');"
                        + "  const aria = normalize(node.getAttribute && node.getAttribute('aria-label'));"
                        + "  const label = normalize(node.getAttribute && node.getAttribute('label'));"
                        + "  return text === expected || text.includes(expected) || aria === expected || aria.includes(expected) || label === expected || label.includes(expected);"
                        + "});"
                        + "nodes.sort((a, b) => {"
                        + "  const aText = normalize(a.innerText || a.textContent || '');"
                        + "  const bText = normalize(b.innerText || b.textContent || '');"
                        + "  return aText.length - bText.length;"
                        + "});"
                        + "const target = nodes.find(node => normalize(node.getAttribute && node.getAttribute('role')) === 'button' || node.matches('button,[role=\\'button\\'],flt-semantics[role=\\'button\\'],flt-semantics-placeholder[role=\\'button\\']')) || nodes[0];"
                        + "if (!target) return false;"
                        + "target.scrollIntoView({block:'center'});"
                        + "target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));"
                        + "target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }));"
                        + "target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));"
                        + "return true;",
                expected
        );
        if (!Boolean.TRUE.equals(clicked)) {
            WebElement element = new WebDriverWait(driver, Duration.ofSeconds(8)).until(d -> {
                List<WebElement> candidates = d.findElements(By.cssSelector("button, [role='button'], flt-semantics[role='button'], flt-semantics-placeholder[role='button']"));
                for (WebElement candidate : candidates) {
                    String candidateText = candidate.getText() == null ? "" : candidate.getText();
                    String ariaLabel = candidate.getAttribute("aria-label");
                    String semanticLabel = candidate.getAttribute("label");
                    String normalizedText = candidateText.replace("→", "").trim();
                    String normalizedAria = ariaLabel == null ? "" : ariaLabel.replace("→", "").trim();
                    String normalizedSemantic = semanticLabel == null ? "" : semanticLabel.replace("→", "").trim();
                    if (normalizedText.equalsIgnoreCase(expected)
                            || normalizedText.toLowerCase(Locale.ROOT).contains(expected)
                            || normalizedAria.equalsIgnoreCase(expected)
                            || normalizedAria.toLowerCase(Locale.ROOT).contains(expected)
                            || normalizedSemantic.equalsIgnoreCase(expected)
                            || normalizedSemantic.toLowerCase(Locale.ROOT).contains(expected)) {
                        return candidate;
                    }
                }
                return null;
            });
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
            clickElement(element);
        }
    }

    protected void clickTopLeftBack() {
        Boolean clicked = (Boolean) ((JavascriptExecutor) driver).executeScript(
                "const candidates = Array.from(document.querySelectorAll('flt-semantics[role=button], [role=button], button'));"
                        + "function label(n){return ((n.getAttribute('aria-label')||'') + ' ' + (n.getAttribute('label')||'') + ' ' + (n.innerText||n.textContent||'')).trim().toLowerCase();}"
                        + "let target = candidates.find(n => { const r = n.getBoundingClientRect(); const l = label(n); return r.left < 180 && r.top < 180 && (l.includes('back') || l.includes('<') || l.includes('‹') || l.includes('arrow')); });"
                        + "if (!target) target = candidates.filter(n => { const r = n.getBoundingClientRect(); return r.left < 180 && r.top < 180; }).sort((a,b)=>a.getBoundingClientRect().left-b.getBoundingClientRect().left)[0];"
                        + "if (!target) return false;"
                        + "target.dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true, view:window}));"
                        + "target.dispatchEvent(new MouseEvent('mouseup', {bubbles:true, cancelable:true, view:window}));"
                        + "target.dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true, view:window}));"
                        + "return true;"
        );
        if (!Boolean.TRUE.equals(clicked)) {
            driver.navigate().back();
        }
        pause(900);
    }

    protected void clickByCss(String cssSelector) {
        WebElement element = findVisibleElement(By.cssSelector(cssSelector));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void clickAccessibilityPlaceholder() {
        Boolean clicked = new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                "const seen = new Set();"
                        + "function collect(root) {"
                        + "  if (!root || seen.has(root)) return [];"
                        + "  seen.add(root);"
                        + "  const nodes = Array.from(root.querySelectorAll ? root.querySelectorAll('*') : []);"
                        + "  let results = nodes;"
                        + "  for (const node of nodes) { if (node.shadowRoot) { results = results.concat(collect(node.shadowRoot)); } }"
                        + "  return results;"
                        + "}"
                        + "const nodes = collect(document);"
                        + "const match = nodes.find(node => {"
                        + "  const label = (node.getAttribute && (node.getAttribute('aria-label') || node.getAttribute('label') || '')) || '';"
                        + "  return label.toLowerCase().includes('accessibility') || (node.textContent || '').toLowerCase().includes('accessibility');"
                        + "});"
                        + "if (!match) return false; match.click(); return true;"
        ));
        if (!Boolean.TRUE.equals(clicked)) {
            if (driver.findElements(By.cssSelector("flt-semantics")).isEmpty()) {
                throw new IllegalStateException("Accessibility placeholder not found");
            }
            return;
        }
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(d -> d.findElements(By.cssSelector("flt-semantics[role='button']")).size() > 0);
    }

    protected void enterInto(WebElement element, String value) {
        element.clear();
        element.sendKeys(value);
    }

    protected void enterInto(WebElement element, String value, boolean useJavascriptFallback) {
        try {
            enterInto(element, value);
        } catch (RuntimeException sendKeysException) {
            if (!useJavascriptFallback) {
                throw sendKeysException;
            }
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', { bubbles: true })); arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", element, value);
        }
    }

    protected void setInputValue(WebElement element, String value) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].focus(); arguments[0].value = arguments[1];"
                + "arguments[0].dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: arguments[1] }));"
                + "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));"
                        + "arguments[0].blur();",
                element,
                value
        );
    }

    protected void insertTextViaChrome(String value) {
        if (driver instanceof ChromeDriver chromeDriver) {
            chromeDriver.executeCdpCommand("Input.insertText", Map.of("text", value));
            return;
        }

        ((JavascriptExecutor) driver).executeScript(
                "const active = document.activeElement; if (active) { active.focus(); active.dispatchEvent(new Event('focus', { bubbles: true })); }"
        );
        driver.switchTo().activeElement().sendKeys(value);
    }

    protected void pasteViaClipboard(String value) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
        new Actions(driver)
                .keyDown(Keys.META)
                .sendKeys("v")
                .keyUp(Keys.META)
                .perform();
    }

    protected boolean enterOtpViaJs(String otp) {
        String script = "(function(otp){var inputs = document.querySelectorAll('input'); if(inputs.length===0){var el=document.activeElement; if(!el) return false; el.focus(); el.value=otp; el.dispatchEvent(new Event('input',{bubbles:true})); el.dispatchEvent(new Event('change',{bubbles:true})); return true;} for(var i=0;i<inputs.length;i++){try{inputs[i].focus(); inputs[i].value=otp; inputs[i].dispatchEvent(new Event('input',{bubbles:true})); inputs[i].dispatchEvent(new Event('change',{bubbles:true})); }catch(e){} } return true;})(arguments[0]);";
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(script, otp);
            return result == null || Boolean.TRUE.equals(result) || "true".equals(String.valueOf(result));
        } catch (Exception e) {
            System.out.println("[enterOtpViaJs] failed: " + e.getMessage());
            return false;
        }
    }

    protected void openBaseUrl() {
        driver.get(valueFor("baseUrl", "BASE_URL", DEFAULT_BASE_URL));
        waitForPageReady();
    }

    private void waitForPageReady() {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            Object readyState = ((JavascriptExecutor) d).executeScript("return document.readyState");
            return "complete".equals(String.valueOf(readyState));
        });
    }

    private static String firstExistingPath(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && Files.exists(Path.of(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private static String valueFor(String propertyKey, String envKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return defaultValue;
    }
}

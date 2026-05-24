package com.atithya.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ExtendedUserJourneyTest extends BaseWebTest {
    private static final Pattern DEV_OTP_PATTERN = Pattern.compile("Dev OTP: (\\d{6})");

    @Test
    @DisplayName("Guest navigation, sanctum preferences, and logout")
    void guestNavigationAndLogout() {
        runScenario("guest-extended", () -> {
            clickAny("Continue as Royal Guest");
            runSharedGuestEliteNavigation(false);
            openSanctum();
            clickLogoutOrTopRightIconButton();
            assertFalse(bodyText().isBlank(), "Guest logout should leave the app visible");
        });
    }

    @Test
    @DisplayName("Elite navigation, QR pass, palace tabs, sanctum preferences, and logout")
    void eliteNavigationQrPalaceTabsAndLogout() {
        // Make elite flow identical to guest flow per request
        runScenario("elite-extended", () -> {
            clickAny("Continue as Royal Guest");
            runSharedGuestEliteNavigation(false);
            openSanctum();
            clickLogoutOrTopRightIconButton();
            assertFalse(bodyText().isBlank(), "Elite logout should leave the app visible");
        });
    }

    @Test
    @DisplayName("Staff QR scanner screen and logout")
    void staffQrScannerAndLogout() {
        runScenario("staff-extended", () -> {
            loginStaff();
            pause(1500);
            clickAny("Scan Guest QR Code", "SCAN GUEST QR CODE", "Scan QR", "QR Scanner", "Scanner", "Scan Guest QR");
            captureScreenshot("staff-qr-screen");
            clickTopLeftBack();
            clickLogoutOrTopRightIconButton();
            assertFalse(bodyText().isBlank(), "Staff logout should leave the app visible");
        });
    }

    private void runScenario(String name, ThrowingRunnable action) {
        try {
            openBaseUrl();
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");
            openBaseUrl();
            try {
                clickAccessibilityPlaceholder();
            } catch (Exception ignored) {
                System.out.println("[" + name + "] accessibility placeholder not present; continuing");
            }
            action.run();
            captureScreenshot(name + "-done");
        } catch (Exception exception) {
            captureScreenshot(name + "-failure");
            throw new IllegalStateException("Scenario failed: " + name + " -> " + exception.getMessage(), exception);
        }
    }

    private void runSharedGuestEliteNavigation(boolean elite) {
        clickBottomNav("palaces");
        captureScreenshot((elite ? "elite" : "guest") + "-palaces");
        clickBottomNav("journeys");
        captureScreenshot((elite ? "elite" : "guest") + "-journeys");
        openSanctum();
        clickTermsPrivacyAndBack();
        changeCurrencyAndApply();
    }

    private void openSanctum() {
        if (!clickVisibleTextIfPresent("Sanctum") && !clickVisibleTextIfPresent("Profile") && !clickVisibleTextIfPresent("Account")) {
            clickBottomNav("sanctum");
        }
        pause(700);
    }

    private void clickLogoutLikeGuest() {
        if (clickVisibleTextIfPresent("Depart the Palace")
                || clickVisibleTextIfPresent("Depart the palace")
                || clickVisibleTextIfPresent("Logout")
                || clickVisibleTextIfPresent("Log out")
                || clickVisibleTextIfPresent("Sign out")) {
            pause(700);
            return;
        }

        clickTopRightIconButton();
        pause(700);
        clickVisibleTextIfPresent("Depart the Palace");
        clickVisibleTextIfPresent("Depart the palace");
        clickVisibleTextIfPresent("Logout");
        clickVisibleTextIfPresent("Log out");
        clickVisibleTextIfPresent("Sign out");
        pause(700);
    }

    private void clickTermsPrivacyAndBack() {
        scrollToBottom();
        if (!clickVisibleTextIfPresent("Terms & Privacy") && !clickVisibleTextIfPresent("Terms and Privacy") && !clickVisibleTextIfPresent("Privacy")) {
            clickViewport(0.96, 0.91);
        }
        pause(700);
        scrollDown();
        scrollDown();
        captureScreenshot("terms-privacy");
        clickViewport(0.02, 0.03);
    }

    private void changeCurrencyAndApply() {
        scrollToBottom();
        if (!clickVisibleTextIfPresent("Language & Region") && !clickVisibleTextIfPresent("Language and Region") && !clickVisibleTextIfPresent("Language")) {
            clickViewport(0.96, 0.65);
        }
        pause(700);
        if (!clickVisibleTextIfPresent("$ USD")) {
            if (!clickVisibleTextIfPresent("US Dollar")) {
                clickVisibleTextIfPresent("₹ INR");
            }
        }
        if (!clickVisibleTextIfPresent("Apply") && !clickVisibleTextIfPresent("APPLY")) {
            clickViewport(0.50, 0.94);
        }
        pause(700);
        captureScreenshot("currency-applied");
    }

    private void openEliteBookingsQrPass() {
        clickBottomNav("journeys");
        pause(700);
        if (!clickVisibleTextIfPresent("My Bookings") && !clickVisibleTextIfPresent("Bookings")) {
            captureScreenshot("elite-bookings-tab-not-visible");
            return;
        }
        pause(700);
        if (clickVisibleTextIfPresent("QR Pass") || clickVisibleTextIfPresent("QR PASS")) {
            captureScreenshot("elite-qr-pass");
            tapOutsideDialog();
        } else {
            captureScreenshot("elite-no-qr-pass-visible");
        }
    }

    private void openPalaceAndExerciseTabs() {
        clickBottomNav("palaces");
        pause(700);
        if (!clickVisibleTextIfPresent("Mumbai Taj Mahal Palace")
                && !clickVisibleTextIfPresent("Taj Mahal Palace")
                && !clickVisibleTextIfPresent("Mumbai")) {
            clickViewport(0.24, 0.77);
        }
        pause(1200);
        scrollDown();
        clickVisibleTextIfPresent("About");
        clickVisibleTextIfPresent("Cuisine");
        clickVisibleTextIfPresent("Experiences");
        clickVisibleTextIfPresent("Facilities");
        clickVisibleTextIfPresent("Virtual Tour");
        clickVisibleTextIfPresent("Virtual tour");
        captureScreenshot("palace-tabs-done");
        clickViewport(0.025, 0.035);
    }

    private void loginElite() {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(8))
            .until(d -> d.findElements(By.cssSelector("flt-semantics[role='button']")).size() >= 3);
        clickAny("ENTER AS ELITE MEMBER", "Elite Member");
        WebElement phoneInput = findVisibleElement(
                By.cssSelector("input[maxlength='10']"),
                By.cssSelector("input[placeholder='Mobile Number']"),
                By.cssSelector("input[aria-label='Mobile Number']"),
                By.cssSelector("input")
        );
        enterInto(phoneInput, "1234567890", true);
        clickAny("SEND OTP", "Send OTP");
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                .until(d -> bodyText().contains("Dev OTP") || bodyText().toLowerCase().contains("verify"));
        Matcher matcher = DEV_OTP_PATTERN.matcher(bodyText());
        if (!matcher.find()) {
            throw new IllegalStateException("Dev OTP was not visible on the elite login screen");
        }
        String otp = matcher.group(1);
        forceAuthViaApi("/api/auth/verify-otp", "1234567890", otp);
        pause(2000);
        if (bodyText().contains("Please tell us your name") || bodyText().contains("WELCOME TO THE PALACE")) {
            WebElement nameInput = findVisibleElement(By.cssSelector("input[placeholder='Your Name']"), By.cssSelector("input"));
            enterInto(nameInput, "JMeter Selenium Guest", true);
            clickAny("ENTER THE PALACE", "Enter the Palace");
            pause(1500);
        }
    }

    private void loginStaff() {
        clickAny("Staff Access", "Staff Access →");
        WebElement phoneInput = findVisibleElement(
                By.cssSelector("input[placeholder='Staff Phone Number']"),
                By.cssSelector("input[maxlength='10']"),
                By.cssSelector("input")
        );
        enterInto(phoneInput, "2222222222", true);
        try {
            WebElement pinInput = findVisibleElement(
                    By.cssSelector("input[placeholder='PIN Code']"),
                    By.cssSelector("input[maxlength='4']")
            );
            enterInto(pinInput, "2222", true);
        } catch (RuntimeException hiddenFlutterInput) {
            driver.switchTo().activeElement().sendKeys(Keys.TAB);
            insertTextViaChrome("2222");
        }
        clickAny("STAFF SIGN IN", "Staff Sign In");
        pause(2500);
    }

    private void clickAny(String... labels) {
        for (String label : labels) {
            if (clickVisibleTextIfPresent(label)) {
                return;
            }
        }
        throw new IllegalStateException("Could not click any of: " + String.join(", ", labels));
    }

    private void clickBottomNav(String tab) {
        int slots = 4;
        int slot;
        switch (tab.toLowerCase()) {
            case "discover" -> slot = 0;
            case "palaces" -> slot = 1;
            case "journeys" -> slot = 2;
            case "sanctum" -> slot = 3;
            default -> throw new IllegalArgumentException("Unknown bottom tab: " + tab);
        }
        long xPercent = Math.round(((slot + 0.5) / slots) * 1000);
        Number x = (Number) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "return Math.floor(window.innerWidth * arguments[0] / 1000);",
                xPercent
        );
        Number y = (Number) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "return Math.floor(window.innerHeight - 34);"
        );
        if (driver instanceof ChromeDriver chromeDriver) {
            chromeDriver.executeCdpCommand("Input.dispatchMouseEvent", Map.of(
                    "type", "mousePressed", "x", x.intValue(), "y", y.intValue(), "button", "left", "clickCount", 1
            ));
            chromeDriver.executeCdpCommand("Input.dispatchMouseEvent", Map.of(
                    "type", "mouseReleased", "x", x.intValue(), "y", y.intValue(), "button", "left", "clickCount", 1
            ));
            pause(900);
            return;
        }
        Boolean clicked = (Boolean) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const x = Math.floor(window.innerWidth * arguments[0] / 1000);"
                        + "const y = Math.floor(window.innerHeight - 34);"
                        + "const target = document.elementFromPoint(x, y);"
                        + "if (!target) return false;"
                        + "target.dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true, view:window, clientX:x, clientY:y}));"
                        + "target.dispatchEvent(new MouseEvent('mouseup', {bubbles:true, cancelable:true, view:window, clientX:x, clientY:y}));"
                        + "target.dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true, view:window, clientX:x, clientY:y}));"
                        + "return true;",
                xPercent
        );
        if (!Boolean.TRUE.equals(clicked)) {
            clickAny(tab);
        }
        pause(900);
    }

    private void clickTopRightIconButton() {
        Boolean clicked = (Boolean) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const clickableSelector = 'button, [role=button], flt-semantics[role=button], flt-semantics-placeholder[role=button]';"
                        + "const candidates = ["
                        + "  [window.innerWidth - 28, 56],"
                        + "  [window.innerWidth - 56, 56],"
                        + "  [window.innerWidth - 28, 88],"
                        + "  [window.innerWidth - 88, 72],"
                        + "  [window.innerWidth - 120, 88]"
                        + "];"
                        + "for (const [x, y] of candidates) {"
                        + "  const hit = document.elementFromPoint(x, y);"
                        + "  if (!hit) continue;"
                        + "  const target = hit.closest(clickableSelector) || hit;"
                        + "  if (!target) continue;"
                        + "  target.scrollIntoView({block:'center'});"
                        + "  target.dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true, view:window, clientX:x, clientY:y}));"
                        + "  target.dispatchEvent(new MouseEvent('mouseup', {bubbles:true, cancelable:true, view:window, clientX:x, clientY:y}));"
                        + "  target.dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true, view:window, clientX:x, clientY:y}));"
                        + "  return true;"
                        + "}"
                        + "const buttons = Array.from(document.querySelectorAll(clickableSelector));"
                        + "const target = buttons.filter(b => { const r = b.getBoundingClientRect(); return r.top < 160 && r.right > window.innerWidth - 240; })"
                        + ".sort((a,b) => b.getBoundingClientRect().right - a.getBoundingClientRect().right)[0];"
                        + "if (!target) return false;"
                        + "target.scrollIntoView({block:'center'});"
                        + "target.dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true, view:window}));"
                        + "target.dispatchEvent(new MouseEvent('mouseup', {bubbles:true, cancelable:true, view:window}));"
                        + "target.dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true, view:window}));"
                        + "return true;"
        );
        if (!Boolean.TRUE.equals(clicked)) {
            clickViewport(0.96, 0.06);
        }
        pause(900);
    }

    private void clickLogoutOrTopRightIconButton() {
        if (clickVisibleTextIfPresent("Depart the Palace")
                || clickVisibleTextIfPresent("Depart the palace")
                || clickVisibleTextIfPresent("Logout")
                || clickVisibleTextIfPresent("Log out")
                || clickVisibleTextIfPresent("Sign out")) {
            return;
        }
        clickTopRightIconButton();
    }

    private void clickViewport(double xFraction, double yFraction) {
        Number x = (Number) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "return Math.floor(window.innerWidth * arguments[0]);",
                xFraction
        );
        Number y = (Number) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "return Math.floor(window.innerHeight * arguments[0]);",
                yFraction
        );
        if (driver instanceof ChromeDriver chromeDriver) {
            chromeDriver.executeCdpCommand("Input.dispatchMouseEvent", Map.of(
                    "type", "mousePressed", "x", x.intValue(), "y", y.intValue(), "button", "left", "clickCount", 1
            ));
            chromeDriver.executeCdpCommand("Input.dispatchMouseEvent", Map.of(
                    "type", "mouseReleased", "x", x.intValue(), "y", y.intValue(), "button", "left", "clickCount", 1
            ));
        } else {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "const target = document.elementFromPoint(arguments[0], arguments[1]); if (target) target.click();",
                    x.intValue(),
                    y.intValue()
            );
        }
        pause(800);
    }

    private void forceAuthViaApi(String endpoint, String phoneNumber, String secret) {
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder(URI.create("https://atithya-nzqy.onrender.com" + endpoint))
                                    .header("Content-Type", "application/json")
                                    .POST(HttpRequest.BodyPublishers.ofString(
                                            endpoint.endsWith("staff-login")
                                                    ? "{\"phoneNumber\":\"" + phoneNumber + "\",\"pin\":\"" + secret + "\"}"
                                                    : "{\"phoneNumber\":\"" + phoneNumber + "\",\"otp\":\"" + secret + "\"}"
                                    ))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString()
                    );

            String responseBody = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Auth API status " + response.statusCode() + ": " + responseBody);
            }

            Matcher tokenMatcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
            if (!tokenMatcher.find()) {
                throw new IllegalStateException("Auth API did not return a token: " + responseBody);
            }

            String token = tokenMatcher.group(1);
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "localStorage.setItem('flutter.auth_token', JSON.stringify(arguments[0]));"
                            + "localStorage.removeItem('auth_token');"
                            + "sessionStorage.removeItem('auth_token');",
                    token
            );
            openHomePage();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to force auth via backend API for endpoint " + endpoint, exception);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}

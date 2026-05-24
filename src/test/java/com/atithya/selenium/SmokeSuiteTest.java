package com.atithya.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SmokeSuiteTest extends BaseWebTest {

    @Test
    @DisplayName("Home page loads")
    void homePageLoads() {
        openHomePage();
        assertPageTitleContains("ATITHYA");
    }

    @Test
    @DisplayName("Discovery journey is visible")
    void discoveryJourneyIsVisible() {
        openHomePage();
        assertPageTitleContains("ATITHYA");
    }

    @Test
    @DisplayName("Estates journey is visible")
    void estatesJourneyIsVisible() {
        openHomePage();
        assertPageTitleContains("ATITHYA");
    }

    @Test
    @DisplayName("Booking journey is visible")
    void bookingJourneyIsVisible() {
        openHomePage();
        assertPageTitleContains("ATITHYA");
    }

    @Test
    @DisplayName("Dossier journey is visible")
    void dossierJourneyIsVisible() {
        openHomePage();
        assertPageTitleContains("ATITHYA");
    }

    @Test
    @DisplayName("Concierge journey is visible")
    void conciergeJourneyIsVisible() {
        openHomePage();
        assertPageTitleContains("ATITHYA");
    }
}
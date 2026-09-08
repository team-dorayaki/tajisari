package com.tajisali.property.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import java.net.URI;

public record PropertyAnalysisUrlRequest(@NotBlank String url) {

    @AssertTrue
    public boolean isValidHttpUrl() {
        if (url == null || url.isBlank()) {
            return true;
        }

        try {
            URI uri = URI.create(url);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

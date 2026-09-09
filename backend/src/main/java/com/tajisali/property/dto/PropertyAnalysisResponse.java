package com.tajisali.property.dto;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public record PropertyAnalysisResponse(
        String inputType,
        String modelVersion,
        AnalysisMetadata analysisMetadata,
        Property property,
        List<PropertyCostItem> propertyCostItems,
        AnalysisDetails analysisDetails,
        JsonNode rawResult
) {
    public record AnalysisMetadata(String schemaVersion, SourceType sourceType, int imageCount) {
    }

    public record Property(
            SourceSite sourceSite,
            String sourceUrl,
            String propertyName,
            String prefecture,
            String city,
            BigDecimal exclusiveAreaM2,
            String nearestStation,
            Integer walkMinutes,
            Long rent,
            Long managementFee,
            Long deposit,
            Long keyMoney,
            String availableFrom,
            Integer contractPeriodMonths,
            Long listedInitialCostTotal
    ) {
    }

    public record PropertyCostItem(
            String rawName, String displayName, Long amount,
            String rawValue, ObligationStatus obligationStatus, Timing timing
    ) {
    }

    public record AnalysisDetails(
            List<FieldAnalysis> fieldAnalysis, List<CostItemAnalysis> costItemAnalysis,
            List<Station> allStations, List<AdditionalField> additionalFields,
            List<ReferenceInformation> referenceInformation, Validation validation
    ) {
    }

    public record FieldAnalysis(
            String field, String rawValue, BigDecimal confidence,
            boolean needsReview, List<Evidence> evidence
    ) {
    }

    public record CostItemAnalysis(
            int costItemIndex, Scope scope, BigDecimal confidence,
            boolean needsReview, List<Evidence> evidence
    ) {
    }

    public record Station(String lineName, String stationName, Integer walkMinutes, List<Evidence> evidence) {
    }

    public record AdditionalField(
            AdditionalFieldCategory category,
            String rawName,
            String displayName,
            String value,
            String unit,
            String rawValue,
            BigDecimal confidence,
            boolean needsReview,
            List<Evidence> evidence
    ) {
    }

    public record ReferenceInformation(
            ReferenceCategory category, String rawText,
            AppliesToListing appliesToListing, List<Evidence> evidence
    ) {
    }

    public record Validation(
            List<Conflict> conflicts, List<String> warnings,
            List<String> unknownFields, ValidationChecks checks
    ) {
    }

    public record Conflict(
            String field,
            List<String> values,
            String reason,
            ConflictResolution resolution,
            String resolvedValue,
            List<Evidence> evidence
    ) {
    }

    public record ValidationChecks(
            boolean evidenceOnly,
            boolean amountDoesNotImplyRequired,
            boolean zeroAndNullDistinguished,
            boolean duplicatesRemoved,
            boolean conflictsReviewed,
            boolean fixedCostsNotDuplicated,
            boolean listingTermsPreferred,
            boolean amountsMatchRawText,
            boolean requiredStatusHasEvidence
    ) {
    }

    public record Evidence(EvidenceSourceType sourceType, Integer sourceIndex, String sourceUrl, String rawText) {
    }

    public enum SourceType {
        IMAGE, URL, BOTH
    }

    public enum SourceSite {
        SUUMO, LIFULL_HOMES, ATHOME, LEOPALACE21, GTN_BEST_ESTATE,
        SOL_HOUSING, JAPAN_HOMES, UR, OTHER, UNKNOWN
    }

    public enum ObligationStatus {
        REQUIRED, OPTIONAL, UNKNOWN
    }

    public enum Timing {
        INITIAL, MONTHLY, RENEWAL, MOVE_OUT, CONDITIONAL, UNKNOWN
    }

    public enum Scope {
        LISTING_SPECIFIC
    }

    public enum AdditionalFieldCategory {
        PROPERTY, BUILDING, LOCATION, ACCESS, CONTRACT, CONDITION, FACILITY, AGENCY, LISTING, OTHER
    }

    public enum ReferenceCategory {
        COMPANY_POLICY, SITE_GUIDE
    }

    public enum AppliesToListing {
        YES, NO, UNKNOWN
    }

    public enum ConflictResolution {
        RESOLVED, UNKNOWN
    }

    public enum EvidenceSourceType {
        IMAGE, URL, POLICY
    }
}

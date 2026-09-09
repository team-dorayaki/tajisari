NULLABLE_STRING = {"type": ["string", "null"]}
NULLABLE_INTEGER = {"type": ["integer", "null"]}
NULLABLE_NUMBER = {"type": ["number", "null"]}
STRING_ARRAY = {"type": "array", "items": {"type": "string"}}


def required_object(properties: dict) -> dict:
    return {"type": "object", "properties": properties, "required": list(properties)}


EVIDENCE_SCHEMA = required_object(
    {
        "source_type": {"type": "string", "enum": ["IMAGE", "URL", "POLICY"]},
        "source_index": NULLABLE_INTEGER,
        "source_url": NULLABLE_STRING,
        "raw_text": {"type": "string"},
    }
)


FIELD_ANALYSIS_SCHEMA = required_object(
    {
        "field": {"type": "string"},
        "raw_value": NULLABLE_STRING,
        "confidence": {"type": "number", "minimum": 0, "maximum": 1},
        "needs_review": {"type": "boolean"},
        "evidence": {"type": "array", "items": EVIDENCE_SCHEMA},
    }
)


PROPERTY_COST_ITEM_SCHEMA = required_object(
    {
        "raw_name": {"type": "string"},
        "display_name": {"type": "string"},
        "amount": NULLABLE_INTEGER,
        "raw_value": NULLABLE_STRING,
        "obligation_status": {
            "type": "string",
            "enum": ["REQUIRED", "OPTIONAL", "UNKNOWN"],
        },
        "timing": {
            "type": "string",
            "enum": [
                "INITIAL",
                "MONTHLY",
                "RENEWAL",
                "MOVE_OUT",
                "CONDITIONAL",
                "UNKNOWN",
            ],
        },
    }
)


COST_ITEM_ANALYSIS_SCHEMA = required_object(
    {
        "cost_item_index": {"type": "integer", "minimum": 0},
        "scope": {"type": "string", "enum": ["LISTING_SPECIFIC"]},
        "confidence": {"type": "number", "minimum": 0, "maximum": 1},
        "needs_review": {"type": "boolean"},
        "evidence": {"type": "array", "items": EVIDENCE_SCHEMA},
    }
)


COST_CANDIDATE_SCHEMA = required_object(
    {
        "raw_text": {"type": "string"},
        "cost_name": NULLABLE_STRING,
        "display_name": NULLABLE_STRING,
        "applicability_condition": NULLABLE_STRING,
        "applies_to_listing": {
            "type": "string",
            "enum": ["YES", "CONDITIONAL", "NO", "UNKNOWN"],
        },
        "timing": {
            "type": "string",
            "enum": [
                "INITIAL", "MONTHLY", "RENEWAL", "MOVE_OUT",
                "CONDITIONAL", "UNKNOWN",
            ],
        },
        "destination": {
            "type": "string",
            "enum": [
                "PROPERTY", "PROPERTY_COST_ITEM",
                "REFERENCE_INFORMATION", "EXCLUDED",
            ],
        },
        "target_index": NULLABLE_INTEGER,
        "evidence": {"type": "array", "items": EVIDENCE_SCHEMA},
    }
)


STATION_SCHEMA = required_object(
    {
        "line_name": NULLABLE_STRING,
        "station_name": {"type": "string"},
        "walk_minutes": NULLABLE_INTEGER,
        "evidence": {"type": "array", "items": EVIDENCE_SCHEMA},
    }
)


ADDITIONAL_FIELD_SCHEMA = required_object(
    {
        "category": {
            "type": "string",
            "enum": [
                "PROPERTY",
                "BUILDING",
                "LOCATION",
                "ACCESS",
                "CONTRACT",
                "CONDITION",
                "FACILITY",
                "AGENCY",
                "LISTING",
                "OTHER",
            ],
        },
        "raw_name": {"type": "string"},
        "display_name": {"type": "string"},
        "value": NULLABLE_STRING,
        "unit": NULLABLE_STRING,
        "raw_value": NULLABLE_STRING,
        "confidence": {"type": "number", "minimum": 0, "maximum": 1},
        "needs_review": {"type": "boolean"},
        "evidence": {"type": "array", "items": EVIDENCE_SCHEMA},
    }
)


REFERENCE_INFORMATION_SCHEMA = required_object(
    {
        "category": {"type": "string", "enum": ["COMPANY_POLICY", "SITE_GUIDE"]},
        "raw_text": {"type": "string"},
        "applies_to_listing": {"type": "string", "enum": ["YES", "NO", "UNKNOWN"]},
        "evidence": {"type": "array", "items": EVIDENCE_SCHEMA},
    }
)


CONFLICT_SCHEMA = required_object(
    {
        "field": {"type": "string"},
        "values": STRING_ARRAY,
        "reason": NULLABLE_STRING,
        "resolution": {"type": "string", "enum": ["RESOLVED", "UNKNOWN"]},
        "resolved_value": NULLABLE_STRING,
        "evidence": {"type": "array", "items": EVIDENCE_SCHEMA},
    }
)


PROPERTY_SCHEMA = required_object(
    {
        "source_site": {
            "type": "string",
            "enum": [
                "SUUMO",
                "LIFULL_HOMES",
                "ATHOME",
                "LEOPALACE21",
                "GTN_BEST_ESTATE",
                "SOL_HOUSING",
                "JAPAN_HOMES",
                "UR",
                "OTHER",
                "UNKNOWN",
            ],
        },
        "source_url": NULLABLE_STRING,
        "property_name": NULLABLE_STRING,
        "prefecture": NULLABLE_STRING,
        "city": NULLABLE_STRING,
        "exclusive_area_m2": NULLABLE_NUMBER,
        "nearest_station": NULLABLE_STRING,
        "walk_minutes": NULLABLE_INTEGER,
        "rent": NULLABLE_INTEGER,
        "management_fee": NULLABLE_INTEGER,
        "deposit": NULLABLE_INTEGER,
        "key_money": NULLABLE_INTEGER,
        "available_from": NULLABLE_STRING,
        "contract_period_months": NULLABLE_INTEGER,
        "listed_initial_cost_total": NULLABLE_INTEGER,
    }
)


OUTPUT_SCHEMA = required_object(
    {
        "analysis_metadata": required_object(
            {
                "schema_version": {"type": "string", "enum": ["3.1"]},
                "source_type": {"type": "string", "enum": ["IMAGE", "URL", "BOTH"]},
                "image_count": {"type": "integer", "minimum": 0},
            }
        ),
        "property": PROPERTY_SCHEMA,
        "property_cost_items": {
            "type": "array",
            "items": PROPERTY_COST_ITEM_SCHEMA,
        },
        "analysis_details": required_object(
            {
                "field_analysis": {
                    "type": "array",
                    "items": FIELD_ANALYSIS_SCHEMA,
                },
                "cost_item_analysis": {
                    "type": "array",
                    "items": COST_ITEM_ANALYSIS_SCHEMA,
                },
                "cost_candidates": {
                    "type": "array",
                    "items": COST_CANDIDATE_SCHEMA,
                },
                "all_stations": {"type": "array", "items": STATION_SCHEMA},
                "additional_fields": {
                    "type": "array",
                    "items": ADDITIONAL_FIELD_SCHEMA,
                },
                "reference_information": {
                    "type": "array",
                    "items": REFERENCE_INFORMATION_SCHEMA,
                },
                "validation": required_object(
                    {
                        "conflicts": {"type": "array", "items": CONFLICT_SCHEMA},
                        "warnings": STRING_ARRAY,
                        "unknown_fields": STRING_ARRAY,
                        "checks": required_object(
                            {
                                "evidence_only": {"type": "boolean"},
                                "amount_does_not_imply_required": {"type": "boolean"},
                                "zero_and_null_distinguished": {"type": "boolean"},
                                "duplicates_removed": {"type": "boolean"},
                                "conflicts_reviewed": {"type": "boolean"},
                                "fixed_costs_not_duplicated": {"type": "boolean"},
                                "listing_terms_preferred": {"type": "boolean"},
                                "amounts_match_raw_text": {"type": "boolean"},
                                "required_status_has_evidence": {"type": "boolean"},
                                "cost_candidates_classified": {"type": "boolean"},
                            }
                        ),
                    }
                ),
            }
        ),
    }
)


REQUIRED_SECTIONS = {
    "analysis_metadata",
    "property",
    "property_cost_items",
    "analysis_details",
}

REQUIRED_NESTED_FIELDS = {
    "analysis_metadata": {"schema_version", "source_type", "image_count"},
    "property": set(PROPERTY_SCHEMA["properties"]),
    "analysis_details": {
        "field_analysis",
        "cost_item_analysis",
        "cost_candidates",
        "all_stations",
        "additional_fields",
        "reference_information",
        "validation",
    },
}

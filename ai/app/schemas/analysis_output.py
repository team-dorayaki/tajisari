NULLABLE_STRING = {"type": ["string", "null"]}
NULLABLE_INTEGER = {"type": ["integer", "null"]}
STRING_ARRAY = {"type": "array", "items": {"type": "string"}}


def required_object(properties: dict) -> dict:
    return {
        "type": "object",
        "properties": properties,
        "required": list(properties),
    }


EVIDENCE_SCHEMA = required_object(
    {
        "source_type": {"type": "string", "enum": ["IMAGE", "URL", "POLICY"]},
        "source_index": NULLABLE_INTEGER,
        "source_url": NULLABLE_STRING,
        "raw_text": {"type": "string"},
    }
)


def extracted_field(value_schema: dict) -> dict:
    return required_object(
        {
            "value": value_schema,
            "raw_value": NULLABLE_STRING,
            "confidence": {"type": "number", "minimum": 0, "maximum": 1},
            "needs_review": {"type": "boolean"},
            "evidence": {"type": "array", "items": EVIDENCE_SCHEMA},
        }
    )


COST_ITEM_SCHEMA = required_object(
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
        "calculation_basis": NULLABLE_STRING,
        "confidence": {"type": "number", "minimum": 0, "maximum": 1},
        "needs_review": {"type": "boolean"},
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


OUTPUT_SCHEMA = required_object(
    {
        "analysis_metadata": required_object(
            {
                "schema_version": {"type": "string", "enum": ["2.0"]},
                "source_type": {"type": "string", "enum": ["IMAGE", "URL", "BOTH"]},
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
                "image_count": {"type": "integer", "minimum": 0},
            }
        ),
        "property": required_object(
            {
                "source_property_id": extracted_field(NULLABLE_STRING),
                "rent": extracted_field(NULLABLE_INTEGER),
                "management_fee": extracted_field(NULLABLE_INTEGER),
                "deposit": extracted_field(NULLABLE_INTEGER),
                "key_money": extracted_field(NULLABLE_INTEGER),
                "available_from": extracted_field(NULLABLE_STRING),
                "contract_period_months": extracted_field(NULLABLE_INTEGER),
                "listed_initial_cost_total": extracted_field(NULLABLE_INTEGER),
            }
        ),
        "cost_items": {"type": "array", "items": COST_ITEM_SCHEMA},
        "additional_fields": {"type": "array", "items": ADDITIONAL_FIELD_SCHEMA},
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
                    }
                ),
            }
        ),
    }
)


REQUIRED_SECTIONS = {
    "analysis_metadata",
    "property",
    "cost_items",
    "additional_fields",
    "validation",
}

REQUIRED_NESTED_FIELDS = {
    "analysis_metadata": {
        "schema_version",
        "source_type",
        "source_site",
        "source_url",
        "image_count",
    },
    "property": {
        "source_property_id",
        "rent",
        "management_fee",
        "deposit",
        "key_money",
        "available_from",
        "contract_period_months",
        "listed_initial_cost_total",
    },
    "validation": {"conflicts", "warnings", "unknown_fields", "checks"},
}

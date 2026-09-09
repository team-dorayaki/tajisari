"""Public v3.0 contract, sourced from Git commit a5f9bc4.

Candidate extraction remains internal to the single-model analysis.
"""
from copy import deepcopy

from jsonschema import Draft202012Validator

from app.schemas.analysis_output_v3 import OUTPUT_SCHEMA


def to_v3(result: dict) -> dict:
    output = deepcopy(result)
    output['analysis_details'].pop('cost_candidates', None)
    output['analysis_details']['validation']['checks'].pop('cost_candidates_classified', None)
    output['analysis_metadata']['schema_version'] = '3.0'
    Draft202012Validator(OUTPUT_SCHEMA).validate(output)
    return output

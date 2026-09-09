from copy import deepcopy
import json
from unittest.mock import patch

import pytest
from jsonschema import ValidationError

from app.schemas.analysis_output import OUTPUT_SCHEMA
from app.services.response_v3 import to_v3
from app.services.gemini_analyzer import analyze_uploaded_images, analyze_property_url
from gemini_analyze_image import apply_semantic_checks, extract_direct_yen_amounts, split_compound_cost_candidates, split_facility_fields


def sample(schema):
    kind = schema['type']
    if kind == 'object':
        return {key: sample(value) for key, value in schema['properties'].items()}
    if kind == 'array':
        return []
    if isinstance(kind, list):
        return None
    return schema.get('enum', [{'string': '', 'integer': 0, 'number': 0, 'boolean': False}[kind]])[0]


def test_mapper_preserves_data_and_does_not_mutate_input():
    data = sample(OUTPUT_SCHEMA)
    data['property']['rent'] = 42000
    original = deepcopy(data)
    result = to_v3(data)
    assert data == original
    assert result['property'] == data['property']
    assert result['analysis_metadata']['schema_version'] == '3.0'
    assert 'cost_candidates' not in result['analysis_details']
    assert 'cost_candidates_classified' not in result['analysis_details']['validation']['checks']


def test_mapper_validates_nested_field_types():
    data = sample(OUTPUT_SCHEMA)
    data['property']['rent'] = '42000'
    with pytest.raises(ValidationError):
        to_v3(data)


def test_each_service_calls_one_model_once():
    data = json.dumps(sample(OUTPUT_SCHEMA))
    with patch('app.services.gemini_analyzer.analyze_images', return_value=data) as images:
        result = analyze_uploaded_images([('a.png', 'image/png', b'example')], model='one-model')
        images.assert_called_once()
        assert images.call_args.args[2] == 'one-model'
        assert result['analysis_metadata']['schema_version'] == '3.0'
    with patch('app.services.gemini_analyzer.analyze_webpage', return_value=data) as url:
        result = analyze_property_url('https://example.com', model='one-model')
        url.assert_called_once()
        assert url.call_args.args[2] == 'one-model'
        assert result['analysis_metadata']['schema_version'] == '3.0'


def test_recovered_candidate_keeps_explicit_optional_status():
    data = sample(OUTPUT_SCHEMA)
    data['analysis_details']['cost_candidates'] = [{
        'cost_name': '任意サービス', 'display_name': '선택 서비스',
        'raw_text': '任意サービス (任意) 2500円',
        'applicability_condition': None, 'applies_to_listing': 'YES',
        'timing': 'UNKNOWN', 'destination': 'EXCLUDED', 'target_index': None,
        'evidence': [{'source_type': 'IMAGE', 'source_index': 1, 'source_url': None,
                      'raw_text': '任意サービス (任意) 2500円'}],
    }]
    apply_semantic_checks(data)
    assert data['property_cost_items'][0]['obligation_status'] == 'OPTIONAL'


def test_period_suffix_does_not_become_an_extra_cost():
    result = split_compound_cost_candidates([{
        'raw_text': '10000円(契約時)/月額賃料の1％/毎月(入居期間中)',
        'target_index': 0, 'evidence': [],
    }])
    assert len(result) == 2
    assert result[1]['raw_text'] == '月額賃料の1％/毎月(入居期間中)'


def test_spaced_yen_amount_is_normalized():
    assert extract_direct_yen_amounts('10 000円/入居時') == {10000}


def test_facilities_are_split_into_individual_fields():
    fields = split_facility_fields([{'category': 'FACILITY', 'raw_name': '設備', 'display_name': '시설', 'value': 'エアコン、宅配ボックス', 'raw_value': 'エアコン、宅配ボックス'}])
    assert [field['raw_value'] for field in fields] == ['エアコン', '宅配ボックス']




def test_zero_without_explicit_zero_evidence_becomes_null():
    data = sample(OUTPUT_SCHEMA)
    data['property']['key_money'] = 0
    data['analysis_details']['field_analysis'] = [{
        'field': 'property.key_money', 'raw_value': '레이킨 1개월',
        'confidence': 1.0, 'needs_review': False,
        'evidence': [{'source_type': 'IMAGE', 'source_index': 1,
                      'source_url': None, 'raw_text': '레이킨 1개월'}],
    }]
    apply_semantic_checks(data)
    assert data['property']['key_money'] is None


def test_duplicate_unlabeled_zero_evidence_does_not_prove_key_money():
    data = sample(OUTPUT_SCHEMA)
    data['property']['deposit'] = 0
    data['property']['key_money'] = 0
    evidence = [{'source_type': 'IMAGE', 'source_index': 1, 'source_url': None, 'raw_text': '불필요'}]
    for field in ('deposit', 'key_money'):
        data['analysis_details']['field_analysis'].append({
            'field': f'property.{field}', 'raw_value': '불필요', 'confidence': 1,
            'needs_review': False, 'evidence': evidence,
        })
    apply_semantic_checks(data)
    assert data['property']['deposit'] == 0
    assert data['property']['key_money'] is None


def test_zero_validation_prefers_evidence_over_conflicting_raw_value():
    data = sample(OUTPUT_SCHEMA)
    data['property']['key_money'] = 0
    data['analysis_details']['field_analysis'].append({
        'field': 'property.key_money', 'raw_value': '불필요', 'confidence': 1,
        'needs_review': False,
        'evidence': [{'source_type': 'IMAGE', 'source_index': 1, 'source_url': None,
                      'raw_text': '레이킨 1개월'}],
    })
    apply_semantic_checks(data)
    assert data['property']['key_money'] is None

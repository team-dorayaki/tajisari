"""Run the supplied image or URL case through the actual FastAPI endpoint."""
import argparse
import json
from pathlib import Path
from fastapi.testclient import TestClient
from app.main import app

parser = argparse.ArgumentParser()
parser.add_argument('case', choices=['images', 'url'])
parser.add_argument('--label', default='first')
args = parser.parse_args()
root = Path(__file__).resolve().parents[3]
with TestClient(app) as client:
    if args.case == 'images':
        files = [('files', (f'test3_{i}.png', (root / 'ai' / f'test3_{i}.png').read_bytes(), 'image/png')) for i in range(1, 4)]
        response = client.post('/api/property-analyses/images', files=files)
    else:
        response = client.post('/api/property-analyses/url', json={'url': 'https://suumo.jp/chintai/jnc_000105784468/?bc=100487046549'})
out = Path(__file__).parent / 'results'
out.mkdir(exist_ok=True)
(out / f'{args.case}-{args.label}.json').write_text(json.dumps(response.json(), ensure_ascii=False, indent=2), encoding='utf-8')
print(f'{args.case}: HTTP {response.status_code}')
if response.status_code == 200:
    print(json.dumps(response.json()['result']['property'], ensure_ascii=True))
else:
    print(response.text)

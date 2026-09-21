#!/usr/bin/env python3
"""Check Git's prospective source export, not ignored local build artifacts."""
from pathlib import Path
import re,subprocess
root=Path(__file__).resolve().parents[1]
files=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
patterns=[re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),re.compile(r'/(?:home|Users)/[^/\s]+/'),re.compile(r'/mnt/' + r'sol-data/'),re.compile(r'\b(?:10\.\d+\.\d+\.\d+|192\.168\.\d+\.\d+)\b')]
errors=[]
for name in filter(None,files):
 p=root/name
 if p.suffix.lower() in {'.jks','.keystore','.apk','.npy','.pyc'} or name.startswith(('vendor/','build/','artifacts/')):errors.append(name+': excluded artifact')
 if p.suffix.lower() in {'.png','.jpg','.jpeg'}:continue
 data=p.read_bytes()
 if b'\x00' in data:errors.append(name+': unexpected binary');continue
 text=data.decode('utf-8')
 if any(pattern.search(text) for pattern in patterns):errors.append(name+': private key or machine-specific identifier pattern')
if errors:raise SystemExit('\n'.join(errors))
print('Tracked source export checks passed')

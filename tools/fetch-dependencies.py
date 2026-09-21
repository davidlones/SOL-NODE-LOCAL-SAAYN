#!/usr/bin/env python3
"""Fetch pinned archives; validate bytes before extracting any resources."""
import hashlib,json,shutil,tempfile,zipfile
from pathlib import Path
from urllib.request import urlopen
root=Path(__file__).resolve().parents[1];vendor=root/'vendor';vendor.mkdir(exist_ok=True)
for entry in json.loads((root/'dependencies.lock.json').read_text()):
 dest=vendor/entry['file']
 if not dest.exists() or hashlib.sha256(dest.read_bytes()).hexdigest()!=entry['sha256']:
  with tempfile.NamedTemporaryFile(dir=vendor,delete=False) as f:
   temporary=Path(f.name)
   try:
    with urlopen(entry['url'],timeout=60) as response:shutil.copyfileobj(response,f)
    f.close()
    if hashlib.sha256(temporary.read_bytes()).hexdigest()!=entry['sha256']:raise RuntimeError('SHA256 mismatch: '+entry['file'])
    temporary.replace(dest)
   finally:temporary.unlink(missing_ok=True)
 if dest.suffix=='.aar':
  with zipfile.ZipFile(dest) as z:
   (vendor/entry['jar']).write_bytes(z.read('classes.jar'))
   if entry.get('resources'):
    target=vendor/entry['resources']
    if target.exists():shutil.rmtree(target)
    for item in z.infolist():
     if not item.filename.startswith('res/') or item.is_dir():continue
     output=(target/item.filename).resolve()
     if not output.is_relative_to(target.resolve()):raise RuntimeError('Unsafe archive member')
     output.parent.mkdir(parents=True,exist_ok=True);output.write_bytes(z.read(item))
 print('Verified '+entry['file'])

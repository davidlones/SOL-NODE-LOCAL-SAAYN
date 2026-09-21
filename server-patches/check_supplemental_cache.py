#!/usr/bin/env python3
"""Isolated regression checks: no network or production model requests."""
import importlib.util
import sys
from pathlib import Path
from unittest.mock import patch
if len(sys.argv)!=2:
    raise SystemExit('Usage: check_supplemental_cache.py /path/to/sol_chat_api.py')
server=Path(sys.argv[1]).absolute()
sys.path.insert(0,str(server.parent))
spec=importlib.util.spec_from_file_location('sol_cache_test',server)
api=importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)
api.SUPPLEMENTAL_KNOWLEDGE_ENABLED=True
meta={'grounding_mode':'normal','hits':[{'path':'fixture','score':0.8,'text':'A fixture about orbital mechanics.'}]}
calls=[]
backend={'base_url':'http://fixture','model_id':'fixture-v1'}
def generate(*a,**kw):
    calls.append(kw)
    return 'orbital mechanics research'
def query(text='Explain orbits', context=meta, profile='text_fast'):
    return api.generate_supplemental_retrieval_query(user_text=text,retrieval_meta=context,backend_profile=profile)
with patch.object(api,'ensure_backend',return_value=backend), patch.object(api,'model_chat_once',side_effect=generate), patch.object(api,'log_event'):
    first=query();assert query()==first and len(calls)==1
    query(profile='reasoning');assert len(calls)==2
    query(context={'hits':[]});assert len(calls)==3
    backend['model_id']='fixture-v2';query();assert len(calls)==4
    with api._SUPPLEMENTAL_QUERY_CACHE_LOCK:
        for k,(stamp,val) in list(api._SUPPLEMENTAL_QUERY_CACHE.items()):
            api._SUPPLEMENTAL_QUERY_CACHE[k]=(stamp-301,val)
    query();assert len(calls)==5
    api._SUPPLEMENTAL_QUERY_CACHE_LIMIT=2
    for i in range(3):query(text='Explain orbit '+str(i))
    assert len(api._SUPPLEMENTAL_QUERY_CACHE)==2
    api._SUPPLEMENTAL_QUERY_CACHE.clear()
    with patch.object(api,'model_chat_once',return_value='NONE') as model, patch.object(api,'_fallback_supplemental_retrieval_query',return_value='fallback query') as fallback:
        assert query()=='fallback query' and query()=='fallback query'
        assert model.call_count==1 and fallback.call_count==2
    api._SUPPLEMENTAL_QUERY_CACHE.clear()
    with patch.object(api,'model_chat_once',side_effect=RuntimeError('fixture')):
        try:query()
        except RuntimeError:pass
        else:raise AssertionError('failure swallowed')
    assert not api._SUPPLEMENTAL_QUERY_CACHE
print('Supplemental cache: repeat hit, profile/model/context separation, expiry, bound, fallback and failure checks passed')

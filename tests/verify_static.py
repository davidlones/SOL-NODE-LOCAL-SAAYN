from pathlib import Path
import xml.etree.ElementTree as ET
r=ET.parse('AndroidManifest.xml').getroot(); ns='{http://schemas.android.com/apk/res/android}'
permissions={p.get(ns+'name') for p in r.findall('uses-permission')}
assert permissions=={'android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE','android.permission.READ_EXTERNAL_STORAGE'}
assert r.find('uses-sdk').get(ns+'minSdkVersion')=='18'
assert r.find('application/service').get(ns+'exported')=='false'
assert not r.findall('application/receiver')
client=Path('src/one/system42/solnode/SolApiClient.java').read_text()
assert 'allow_actions",false' in client and 'followRedirects(false)' in client
assert 'retryOnConnectionFailure(false)' in client
assert 'hostnameVerifier(' not in client and 'checkServerTrusted' not in client
print('API18 manifest, non-exported service, permission limits and network boundaries passed')

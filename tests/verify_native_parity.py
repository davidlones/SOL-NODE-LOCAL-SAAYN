from pathlib import Path
import subprocess
parts=['#include <stdio.h>\n#include <stdlib.h>\n#include <string.h>\n']
for name,file in [('baseline','native/saayn_s4_native.c'),('candidate','native/saayn_s4_optimized.c')]:
 s=Path(file).read_text();a=s.index('static void linear(');b=s.index('static void attention',a);parts.append(s[a:b].replace('linear(',name+'('))
fixture=Path('tests/native_linear_parity.c').read_text();parts.append(fixture[fixture.index('int main()'):]);Path('build/native-linear-current.c').write_text(''.join(parts))
subprocess.run(['cc','-O3','-ffp-contract=off','build/native-linear-current.c','-o','build/native-linear-test'],check=True)
subprocess.run(['build/native-linear-test'],check=True)

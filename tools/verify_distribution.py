#!/usr/bin/env python3
"""Check the ACTUAL built distribution. Never builds a fake JAR for validation."""
import argparse,json,re,struct,sys,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MODULES=['settings','timber','cropreplant','shulkerpreview','inventorysort','mousewheel','zoom','notes','gravestone','toolswap','minimap']
def check(ok,msg):
    if not ok:raise ValueError(msg)
def verify(path):
    expected=json.loads((ROOT/'distribution/src/main/resources/fabric.mod.json').read_text())
    with zipfile.ZipFile(path) as z:
        names=z.namelist();check(len(names)==len(set(names)),'Duplicate JAR entries')
        check('fabric.mod.json' in names,'Missing aggregate metadata')
        meta=json.loads(z.read('fabric.mod.json'))
        check(meta['id']=='vanilla_refined','Not the suite JAR')
        check(meta['environment']=='*','Distribution is not available on both physical sides')
        check(meta['entrypoints']==expected['entrypoints'],'Entrypoints do not match reviewed declaration')
        check(meta['mixins']==expected['mixins'],'Mixin environments changed')
        check(set(meta['provides'])=={'ownmods_'+m for m in MODULES},'Missing module identities')
        check('${' not in z.read('fabric.mod.json').decode(),'Unexpanded metadata')
        check('jars' not in meta,'Unexpected nested mods in merged distribution')
        check(not any(n.startswith(('net/minecraft/','net/fabricmc/','org/lwjgl/')) for n in names),'Third-party classes were accidentally shaded')
        check(all(not n.endswith('fabric.mod.json') or n=='fabric.mod.json' for n in names),'Multiple root module manifests')
        classes=[n for n in names if n.endswith('.class')]
        for n in classes:
            data=z.read(n)
            check(data[:4]==b'\xca\xfe\xba\xbe','Bad class file: '+n)
            check(struct.unpack('>H',data[6:8])[0]<=69,'Unsupported bytecode above Java 25: '+n)
            check(not n.startswith('de/ownmods/tests/') and 'compatcheck/' not in n,'Test classes leaked into runtime')
        for env,entrypoints in meta['entrypoints'].items():
            for cls in entrypoints:check(cls.replace('.','/')+'.class' in names,'Missing '+env+' initializer '+cls)
        for m in MODULES:
            check(any(n.startswith('de/ownmods/'+m+'/') for n in classes),'Missing module classes: '+m)
            for f in (ROOT/m/'src/main/resources').rglob('*'):
                if f.is_file() and f.name!='fabric.mod.json':check(f.relative_to(ROOT/m/'src/main/resources').as_posix() in names,'Missing resource: '+str(f))
        print(f'DISTRIBUTION PASS: {len(classes)} classes; 11 modules; one runtime JAR; guarded client entrypoints/mixins. No game launch implied.')
        return meta
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('jar',type=Path);a=p.parse_args()
    try:verify(a.jar)
    except (OSError,ValueError,KeyError,zipfile.BadZipFile) as e:print('DISTRIBUTION FAIL: '+str(e),file=sys.stderr);sys.exit(1)

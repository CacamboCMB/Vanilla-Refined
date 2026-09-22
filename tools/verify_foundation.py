#!/usr/bin/env python3
"""Fast structural gates. These do not replace compilation, server launch or gameplay tests."""
import json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MODULES=['settings','timber','cropreplant','shulkerpreview','inventorysort','mousewheel','zoom','notes','gravestone','toolswap','minimap']
SERVER={'settings','timber','cropreplant','inventorysort','gravestone','toolswap'}
def load(path):
    def unique(pairs):
        out={}
        for k,v in pairs:
            if k in out:raise ValueError(f'Duplicate JSON key {k} in {path}')
            out[k]=v
        return out
    return json.loads(path.read_text(encoding='utf-8'),object_pairs_hook=unique)
def check(ok,message):
    if not ok:raise ValueError(message)
def main():
    languages={'en_us':{},'de_de':{}};entries={'main':[],'client':[]};mixins=[]
    for m in MODULES:
        meta=load(ROOT/m/'src/main/resources/fabric.mod.json')
        check(meta['id']=='ownmods_'+m,'Mod ID drift: '+m)
        check(meta['environment']==('*' if m in SERVER else 'client'),'Wrong physical environment: '+m)
        for env in entries:
            values=meta.get('entrypoints',{}).get(env,[]);entries[env]+=values
            for c in values:
                side='client' if env=='client' else 'main'
                check((ROOT/m/f'src/{side}/java'/Path(c.replace('.','/')+'.java')).is_file(),f'Missing {env} entrypoint {c}')
        for item in meta.get('mixins',[]):
            mixins.append(item)
            cfg=item if isinstance(item,str) else item['config']
            conf=load(ROOT/m/'src/main/resources'/cfg)
            if 'client' in conf or m in {'inventorysort','mousewheel','zoom','shulkerpreview'}:
                check(isinstance(item,dict) and item.get('environment')=='client','Client mixin not environment-guarded: '+cfg)
        for p in (ROOT/m/'src/main/java').rglob('*.java'):
            text=p.read_text(encoding='utf-8')
            check(not re.search(r'import\s+(net\.minecraft\.client|net\.fabricmc\.fabric\.api\.client|org\.lwjgl|de\.ownmods\.\w+\.client)',text),f'Physical client import in common sources: {p}')
        for language in languages:
            for p in (ROOT/m/'src/main/resources/assets').glob(f'*/lang/{language}.json'):
                for key,value in load(p).items():
                    check(key not in languages[language],f'Duplicate translation: {key}')
                    check(isinstance(value,str) and value.strip(),f'Empty translation: {key}')
                    languages[language][key]=value
    check(languages['en_us'].keys()==languages['de_de'].keys(),'DE/EN key mismatch')
    for language in languages:check(languages[language]['ownmods.cropreplant.title']=='Farmer','Farmer name drift')
    for m in MODULES:
        for side in ['main','client']:
            for p in (ROOT/m/f'src/{side}/java').rglob('*.java'):
                for key in re.findall(r'Component\.translatable\("([\w.]+)"',p.read_text(encoding='utf-8')):
                    if key.startswith(('ownmods.','vr.')) and not key.endswith('.'):
                        check(key in languages['en_us'],f'Missing translation {key} in {p}')
    # Formatting placeholders are part of the runtime contract too.
    for key,text in languages['en_us'].items():
        tokens=lambda s: sorted(re.findall(r'%(?:\d+\$)?[sdf]',s))
        check(tokens(text)==tokens(languages['de_de'][key]),f'Placeholder mismatch: {key}')
    aggregate=load(ROOT/'distribution/src/main/resources/fabric.mod.json')
    check(aggregate['environment']=='*','Distribution must be client AND dedicated-server loadable')
    check(aggregate['entrypoints']==entries,'Aggregate entrypoints differ from modules')
    check(aggregate['mixins']==mixins,'Aggregate mixin declarations differ')
    check(set(aggregate['provides'])=={'ownmods_'+m for m in MODULES},'Missing module aliases')
    versions=load(ROOT/'compatibility/targets.json')
    prop=dict(line.split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
    check(prop['mod_version']==versions['modVersion'],'Release version drift')
    check('0.12.' in prop['mod_version'],'Unexpected foundation version')
    for target,data in versions['targets'].items():check(data['fabricApi'].endswith('+'+target),'API target mismatch')
    check('getSingleplayerServer' not in (ROOT/'inventorysort/src/client/java/de/ownmods/inventorysort/client/InventorySortClient.java').read_text(),'Inventory client still uses integrated-server shortcut')
    for m,path in [('timber','TimberMod.java'),('cropreplant','CropReplantEngine.java')]:
        check('isDedicatedServer()' not in (ROOT/m/'src/main/java/de/ownmods'/m/path).read_text(),'Old dedicated-server rejection present: '+m)
    print(f'FOUNDATION STRUCTURE PASS: {len(MODULES)} modules; {len(languages["en_us"])} DE/EN keys; aggregate entrypoints and physical sides checked.')
if __name__=='__main__':
    try: main()
    except (OSError,ValueError,KeyError) as e:print('FOUNDATION STRUCTURE FAIL: '+str(e),file=sys.stderr);sys.exit(1)

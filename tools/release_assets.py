#!/usr/bin/env python3
"""Create per-target immutable release metadata bound to the tested JAR's digest."""
import argparse,hashlib,json,os,re,shutil,sys
from pathlib import Path
from verify_distribution import verify
ROOT=Path(__file__).resolve().parents[1]
def run(a):
    target=json.loads((ROOT/'compatibility/targets.json').read_text())['targets'][a.minecraft]
    jars=list((ROOT/'distribution/build/libs').glob(f'VanillaRefined-*+mc{a.minecraft}.jar'))
    if len(jars)!=1:raise ValueError('Expected one exact target runtime JAR')
    jar=jars[0];meta=verify(jar);digest=hashlib.sha256(jar.read_bytes()).hexdigest()
    smoke=json.loads(a.smoke.read_text())
    if not (smoke['serverStartupPassed'] and smoke['cleanShutdownPassed'] and smoke['modSha256']==digest and smoke['minecraft']==a.minecraft):
        raise ValueError('No successful production smoke result for this exact JAR')
    tag=os.environ.get('GITHUB_REF_NAME','')
    if tag.startswith('v') and tag!='v'+meta['version']:raise ValueError('Tag does not match gradle.properties/mod metadata')
    a.output.mkdir(parents=True,exist_ok=True)
    for artifact in [jar,jar.with_name(jar.stem+'-sources.jar')]:
        if not artifact.is_file():raise ValueError('Missing '+str(artifact))
        shutil.copy2(artifact,a.output/artifact.name)
    # This candidate is NOT advertised as stable or fully gameplay-supported.
    manifest={'schemaVersion':1,'repository':'CacamboCMB/Vanilla-Refined','version':meta['version'],
        'minecraft':a.minecraft,'loader':target['loader'],'fabricApi':target['fabricApi'],'java':25,
        'artifact':jar.name,'sha256':digest,'sourceCommit':os.environ.get('GITHUB_SHA','local'),
        'validation':{'compile':True,'productionServerStart':True,'cleanShutdown':True,'multiplayerGameplay':False},
        'channel':'alpha','supportedForStableInstall':False}
    (a.output/f'compatibility-mc{a.minecraft}.json').write_text(json.dumps(manifest,indent=2)+'\n')
    shutil.copy2(a.smoke,a.output/f'smoke-result-mc{a.minecraft}.json')
    print('Release assets prepared for draft/prerelease only: '+a.minecraft)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--minecraft',required=True);p.add_argument('--smoke',required=True,type=Path);p.add_argument('--output',type=Path,default=ROOT/'build/release-assets');a=p.parse_args()
    try:run(a)
    except (OSError,ValueError,KeyError) as e:print('RELEASE ASSETS FAIL: '+str(e),file=sys.stderr);sys.exit(1)

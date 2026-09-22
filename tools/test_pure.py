#!/usr/bin/env python3
"""Compile actual dependency-free production sources; no Minecraft mocks or downloads."""
from pathlib import Path
import argparse, subprocess, tempfile, sys
ROOT=Path(__file__).resolve().parents[1]
def sources():
    paths=[]
    for module in ['settings','timber','cropreplant','inventorysort','gravestone','toolswap','notes','minimap','mousewheel','shulkerpreview','zoom']:
        for p in (ROOT/module/'src/main/java').rglob('*.java'):
            if any(part in {'server','network','compat','mixin'} for part in p.relative_to(ROOT).parts): continue
            text=p.read_text(encoding='utf-8')
            if any(x in text for x in ['net.minecraft','net.fabricmc','org.slf4j','org.spongepowered','com.mojang']): continue
            paths.append(p)
    paths+=list((ROOT/'tests/java').rglob('*.java'))
    paths+=list((ROOT/'timber/src/test/java').rglob('*.java'))
    paths+=list((ROOT/'tools/ui-check').rglob('*.java'))
    return sorted(set(paths))
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--release',default='21');args=parser.parse_args()
    with tempfile.TemporaryDirectory(prefix='vr-pure-') as d:
        argfile=Path(d)/'sources.txt'
        argfile.write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in sources()),encoding='utf-8')
        subprocess.run(['javac','-encoding','UTF-8','--release',args.release,'-d',d,'@'+str(argfile)],check=True,timeout=60)
        for cls in ['de.ownmods.tests.FoundationTests','de.ownmods.timber.core.CoreTests','de.ownmods.uicheck.LayoutChecks']:
            subprocess.run(['java','-cp',d,cls],check=True,timeout=60)
if __name__=='__main__':
    try:main()
    except (subprocess.SubprocessError,OSError) as e: print(e,file=sys.stderr);sys.exit(1)

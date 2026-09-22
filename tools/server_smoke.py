#!/usr/bin/env python3
"""Boot the actual packaged mod on a temporary production Fabric server, then stop cleanly.
Requires explicit EULA consent. Downloads official installer/API with SHA-256 checks.
No player login or inventory gameplay is simulated; those remain acceptance tests.
"""
import argparse,hashlib,json,os,queue,re,shutil,socket,subprocess,sys,tempfile,threading,time,urllib.request
from pathlib import Path
from verify_distribution import verify
ROOT=Path(__file__).resolve().parents[1]
INSTALLER='1.1.2'
def read(url,limit):
    request=urllib.request.Request(url,headers={'User-Agent':'VanillaRefined-CI/0.12.3'})
    with urllib.request.urlopen(request,timeout=90) as response:
        if not response.url.startswith('https://'):raise RuntimeError('Non-HTTPS download redirect')
        content=response.read(limit+1)
    if len(content)>limit:raise RuntimeError('Download too large: '+url)
    return content
def download_checked(url,path):
    expected=read(url+'.sha256',4096).decode('ascii').split()[0].lower()
    if not re.fullmatch('[0-9a-f]{64}',expected):raise ValueError('Malformed checksum: '+url)
    content=read(url,30*1024*1024)
    if hashlib.sha256(content).hexdigest()!=expected:raise ValueError('Checksum mismatch: '+url)
    path.write_bytes(content)
def run(args):
    if not args.accept_eula:raise ValueError('No EULA consent. Set MINECRAFT_EULA_ACCEPTED=true only after accepting the Minecraft EULA, or pass --accept-eula yourself.')
    profiles=json.loads((ROOT/'compatibility/targets.json').read_text())['targets']
    if args.minecraft not in profiles:raise ValueError('No build profile for '+args.minecraft)
    profile=profiles[args.minecraft];meta=verify(args.jar)
    if meta['custom']['vanilla_refined:build_minecraft']!=args.minecraft:raise ValueError('Wrong JAR target')
    args.output.mkdir(parents=True,exist_ok=True)
    report={'minecraft':args.minecraft,'modVersion':meta['version'],'modSha256':hashlib.sha256(args.jar.read_bytes()).hexdigest(),
            'serverStartupPassed':False,'cleanShutdownPassed':False,'multiplayerGameplayPassed':False}
    with tempfile.TemporaryDirectory(prefix='vr-server-smoke-') as temp:
        work=Path(temp);installer=work/'installer.jar';server=work/'server';server.mkdir()
        try:
            download_checked(f'https://maven.fabricmc.net/net/fabricmc/fabric-installer/{INSTALLER}/fabric-installer-{INSTALLER}.jar',installer)
            with (args.output/'installer.log').open('w',encoding='utf-8') as log:
                subprocess.run(['java','-jar',str(installer),'server','-dir',str(server),'-mcversion',args.minecraft,
                    '-loader',profile['loader'],'-downloadMinecraft'],stdout=log,stderr=subprocess.STDOUT,check=True,timeout=300,cwd=work)
            mods=server/'mods';mods.mkdir();shutil.copy2(args.jar,mods/args.jar.name)
            api=profile['fabricApi'];url=f'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{api}/fabric-api-{api}.jar'
            download_checked(url,mods/f'fabric-api-{api}.jar')
            # Ephemeral, loopback-only test world. No user world/config directory is accessed.
            (server/'eula.txt').write_text('eula=true\n',encoding='ascii')
            with socket.socket() as sock:sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
            (server/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=true\nmax-players=2\nview-distance=2\nsimulation-distance=2\nlevel-seed=0\nenable-rcon=false\nenable-query=false\nsync-chunk-writes=true\n',encoding='ascii')
            launcher=server/'fabric-server-launch.jar'
            if not launcher.is_file():raise ValueError('Official installer did not produce fabric-server-launch.jar')
            p=subprocess.Popen(['java','-Xms512M','-Xmx2G','-jar',str(launcher),'nogui'],cwd=server,stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace',bufsize=1)
            lines=queue.Queue();transcript=[]
            def pump():
                for line in p.stdout:lines.put(line)
            reader=threading.Thread(target=pump,daemon=True);reader.start();deadline=time.monotonic()+args.timeout
            ready=False;authority=False
            try:
                while time.monotonic()<deadline:
                    try:line=lines.get(timeout=.5)
                    except queue.Empty:
                        if p.poll() is not None:break
                        continue
                    transcript.append(line)
                    if 'Vanilla Refined server authority ready: 5 modules' in line:authority=True
                    if re.search(r'Done \([\d.,]+s\)!',line):ready=True;break
                if not ready:raise RuntimeError('Server did not reach Done before exit/timeout')
                if not authority:raise RuntimeError('Server booted without registering all five gameplay modules')
                if not (server/'config/vanilla_refined/server-policy.properties').is_file():raise RuntimeError('Server policy not created')
                report['serverStartupPassed']=True
                p.stdin.write('stop\n');p.stdin.flush();p.wait(timeout=90);reader.join(timeout=5)
                while not lines.empty():transcript.append(lines.get_nowait())
                if p.returncode!=0:raise RuntimeError('Server shutdown exit '+str(p.returncode))
                report['cleanShutdownPassed']=True
            finally:
                if p.poll() is None:p.kill();p.wait(timeout=15)
                reader.join(timeout=5)
                while not lines.empty():transcript.append(lines.get_nowait())
                (args.output/'server.log').write_text(''.join(transcript),encoding='utf-8')
        finally:
            (args.output/'smoke-result.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print('PRODUCTION SERVER SMOKE PASS: start + five authoritative modules + clean shutdown. Multiplayer gameplay NOT tested.')
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--minecraft',required=True);p.add_argument('--jar',required=True,type=Path)
    p.add_argument('--output',type=Path,default=ROOT/'build/server-smoke');p.add_argument('--timeout',type=int,default=240)
    p.add_argument('--accept-eula',action='store_true',default=os.environ.get('MINECRAFT_EULA_ACCEPTED','').lower()=='true');a=p.parse_args()
    a.jar=a.jar.resolve();a.output=a.output.resolve()
    try:run(a)
    except Exception as e:print('PRODUCTION SERVER SMOKE FAIL: '+str(e),file=sys.stderr);sys.exit(1)

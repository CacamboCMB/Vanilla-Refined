[CmdletBinding()]
param(
    [string]$SourceRoot = (Split-Path -Parent $PSScriptRoot),
    [switch]$NoPush
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$Repository = 'https://github.com/CacamboCMB/Vanilla-Refined.git'
$Branch = 'feature/server-foundation-0.12.3'
$ManifestPath = Join-Path $SourceRoot 'SOURCE-MANIFEST.json'
if (-not (Test-Path -LiteralPath $ManifestPath -PathType Leaf)) {
    throw 'SOURCE-MANIFEST.json fehlt. Dieses Skript mit dem gelieferten Quellpaket verwenden.'
}
$Manifest = Get-Content -LiteralPath $ManifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
if ($Manifest.repository -ne 'CacamboCMB/Vanilla-Refined' -or $Manifest.branch -ne $Branch) {
    throw 'Unerwartetes Repository im Quellmanifest.'
}
if ($Manifest.expectedRemoteCommit -notmatch '^[0-9a-f]{40}$') { throw 'Ungueltiger Ausgangscommit.' }
$GitCommand = Get-Command git -ErrorAction SilentlyContinue
if ($null -eq $GitCommand) { throw 'Git fuer Windows fehlt im PATH. Git installieren und PowerShell neu oeffnen. Es wurde nichts hochgeladen.' }
$GitExe = $GitCommand.Source
function Invoke-GitChecked {
    param([string[]]$GitArgs)
    $PreviousPreference = $ErrorActionPreference
    $GitExit = 1
    try {
        $ErrorActionPreference = 'Continue'
        & $GitExe @GitArgs
        $GitExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $PreviousPreference }
    if ($GitExit -ne 0) { throw "Git ist mit Exitcode $GitExit fehlgeschlagen. Kein Force-Push wird ausgefuehrt." }
}
$Verified = @()
$Seen = @{}
foreach ($File in $Manifest.files) {
    $Relative = [string]$File.path
    if ($Relative -notmatch '^[A-Za-z0-9_./-]+$' -or $Relative.StartsWith('/') -or
        ($Relative.Split('/') -contains '..') -or ($Relative.Split('/') -contains '.git') -or $Seen.ContainsKey($Relative.ToLowerInvariant())) {
        throw "Ungueltiger oder doppelter Paketpfad: $Relative"
    }
    $Seen[$Relative.ToLowerInvariant()] = $true
    $Path = Join-Path $SourceRoot ($Relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Paketdatei fehlt: $Relative" }
    if ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $File.sha256) {
        throw "Pruefsummenfehler: $Relative"
    }
    $Verified += $Relative
}
if ($Verified.Count -lt 150 -or -not $Seen.ContainsKey('build.gradle')) { throw 'Unvollstaendiges Quellpaket.' }
$LocalBase = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'VanillaRefined'
$Work = Join-Path $LocalBase ('GitHub-Import-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,6))
[void](New-Item -ItemType Directory -Path $LocalBase -Force)
Write-Host "Gepruefte Quelldateien: $($Verified.Count)"
Write-Host "Repository: $Repository"
Write-Host "Branch: $Branch"
Write-Host 'Git kann zur Anmeldung im Browser auffordern. Keine Tokens in den Chat kopieren.'
Invoke-GitChecked -GitArgs @('clone','--single-branch','--branch',$Branch,$Repository,$Work)
$Head = (& $GitExe -C $Work rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Git-Ausgangsstand konnte nicht gelesen werden.' }
if ($Head -ne $Manifest.expectedRemoteCommit) {
    throw "Der Remote-Branch wurde seit Erstellung des Pakets veraendert ($Head). Abbruch, damit keine neuen Arbeiten ueberschrieben werden. Arbeitskopie: $Work"
}
foreach ($Relative in $Verified) {
    $Source = Join-Path $SourceRoot ($Relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
    $Destination = Join-Path $Work ($Relative.Replace('/', [IO.Path]::DirectorySeparatorChar))
    [void](New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force)
    Copy-Item -LiteralPath $Source -Destination $Destination -Force
}
# Remove only the known preparation marker, never arbitrary repository files.
$Marker = Join-Path $Work 'SOURCE_IMPORT_REQUIRED.md'
if (Test-Path -LiteralPath $Marker) { Remove-Item -LiteralPath $Marker -Force }
# The manifest is retained as provenance, not as a Minecraft config or credential file.
Copy-Item -LiteralPath $ManifestPath -Destination (Join-Path $Work 'SOURCE-MANIFEST.json') -Force
Invoke-GitChecked -GitArgs @('-C',$Work,'config','core.autocrlf','false')
Invoke-GitChecked -GitArgs @('-C',$Work,'add','--all','--','.')
$Author = & $GitExe -C $Work config user.name
if (-not $Author) { Invoke-GitChecked -GitArgs @('-C',$Work,'config','user.name','CacamboCMB') }
$Email = & $GitExe -C $Work config user.email
if (-not $Email) { Invoke-GitChecked -GitArgs @('-C',$Work,'config','user.email','248357717+CacamboCMB@users.noreply.github.com') }
Invoke-GitChecked -GitArgs @('-C',$Work,'commit','-m','Implement 0.12.3-alpha.1 server foundation, distribution and test-gated releases')
if ($NoPush) {
    Write-Host "Nur lokale Arbeitskopie erstellt: $Work"
    Write-Host 'Noch nicht nach GitHub hochgeladen. Minecraft wurde nicht veraendert.'
    return
}
Invoke-GitChecked -GitArgs @('-C',$Work,'push','origin',('HEAD:refs/heads/' + $Branch))
Write-Host ''
Write-Host "Quellstand nach GitHub hochgeladen. Lokale Kopie: $Work"
Write-Host 'Build-Ergebnisse: https://github.com/CacamboCMB/Vanilla-Refined/actions'
Write-Host 'main wurde nicht ueberschrieben; kein Release wurde veroeffentlicht.'
Write-Host 'Fuer den echten Server-Starttest ist nach eigener EULA-Zustimmung die Repository-Variable MINECRAFT_EULA_ACCEPTED=true erforderlich.'
Write-Host 'Dies war ein Quellcode-Import. Es wurden keine Mods installiert und keine Welten oder Einstellungen geaendert.'

[CmdletBinding()]
param(
    [ValidateSet('26.2','26.3')][string]$MinecraftVersion = '26.3',
    [string]$GradlePath = '',
    [string]$JavaHome = ''
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$OldJava = $env:JAVA_HOME
$OldPath = $env:PATH
try {
    if ($JavaHome) {
        if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) { throw 'JDK-Verzeichnis ungueltig.' }
        $env:JAVA_HOME = $JavaHome
        $env:PATH = (Join-Path $JavaHome 'bin') + ';' + $env:PATH
    }
    if (-not $GradlePath) {
        $Command = Get-Command gradle -ErrorAction SilentlyContinue
        if ($null -eq $Command) { throw 'Gradle 9.6.0 fehlt. -GradlePath auf die vorhandene gradle.bat setzen oder GitHub CI verwenden.' }
        $GradlePath = $Command.Source
    }
    if (-not (Get-Command python -ErrorAction SilentlyContinue)) { throw 'Python 3.10 oder neuer fehlt im PATH.' }
    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw 'JDK 25 fehlt im PATH.' }
    $LogDir = Join-Path $Root 'validation\local'
    [void](New-Item -ItemType Directory -Path $LogDir -Force)
    Push-Location $Root
    try {
        $OldPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        & $GradlePath --no-daemon --console=plain "-Pminecraft_version=$MinecraftVersion" clean build assembleBundle 2>&1 | Tee-Object -FilePath (Join-Path $LogDir 'gradle-windows.log')
        $BuildExit = $LASTEXITCODE
        $ErrorActionPreference = $OldPreference
        if ($BuildExit -ne 0) { throw "Build fehlgeschlagen, Exitcode $BuildExit. Es wurde nichts installiert." }
        Write-Host "Build abgeschlossen: $Root\distribution\build\libs"
        Write-Host 'Echte Server-/Multiplayer-Tests bleiben getrennt. Minecraft wurde nicht veraendert.'
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $OldJava
    $env:PATH = $OldPath
}

param(
    [string]$UsbDirectory = 'D:\INSTALADOR_SISTEMA_ENVASES',
    [string]$MySqlDirectory = 'C:\Program Files\MySQL\MySQL Server 9.7'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$buildRoot = Join-Path $projectRoot 'target\installer'
$payload = Join-Path $buildRoot 'payload'
$appImage = Join-Path $buildRoot 'app-image\Sistema Envases'
$appArchive = Join-Path $payload 'SistemaEnvases.zip'
$mysqlPortable = Join-Path $buildRoot 'mysql-portable\MySQL'
$mysqlArchive = Join-Path $payload 'MySQLServer.zip'
$vcRedist = Join-Path $buildRoot 'vc_redist.x64.exe'
$sedPath = Join-Path $buildRoot 'SistemaEnvases.sed'
$outputExe = Join-Path $UsbDirectory 'INSTALAR_SISTEMA_ENVASES_V2.exe'

if (-not (Test-Path -LiteralPath (Join-Path $appImage 'Sistema Envases.exe'))) {
    throw 'Primero debe generarse la imagen ejecutable con jpackage.'
}
if (-not (Test-Path -LiteralPath (Join-Path $MySqlDirectory 'bin\mysqld.exe'))) {
    throw "No se encontro la instalacion fuente de MySQL: $MySqlDirectory"
}
if (-not (Test-Path -LiteralPath $vcRedist)) {
    throw 'No se encontro el componente Microsoft Visual C++.'
}

New-Item -ItemType Directory -Path $payload, $UsbDirectory -Force | Out-Null
Get-ChildItem -LiteralPath $payload -File -ErrorAction SilentlyContinue | Remove-Item -Force

Compress-Archive -LiteralPath $appImage -DestinationPath $appArchive -CompressionLevel Optimal -Force
$portableRoot = Split-Path -Parent $mysqlPortable
if (Test-Path -LiteralPath $portableRoot) {
    Remove-Item -LiteralPath $portableRoot -Recurse -Force
}
New-Item -ItemType Directory -Path (Join-Path $mysqlPortable 'bin'), (Join-Path $mysqlPortable 'lib') -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $MySqlDirectory 'bin\mysqld.exe'),
    (Join-Path $MySqlDirectory 'bin\mysql.exe') -Destination (Join-Path $mysqlPortable 'bin')
Copy-Item -Path (Join-Path $MySqlDirectory 'bin\*.dll') -Destination (Join-Path $mysqlPortable 'bin')
Copy-Item -LiteralPath (Join-Path $MySqlDirectory 'lib\plugin') -Destination (Join-Path $mysqlPortable 'lib') -Recurse
Copy-Item -LiteralPath (Join-Path $MySqlDirectory 'lib\private') -Destination (Join-Path $mysqlPortable 'lib') -Recurse
Copy-Item -LiteralPath (Join-Path $MySqlDirectory 'share') -Destination $mysqlPortable -Recurse
Copy-Item -LiteralPath (Join-Path $MySqlDirectory 'LICENSE'),
    (Join-Path $MySqlDirectory 'README') -Destination $mysqlPortable
Compress-Archive -LiteralPath $mysqlPortable -DestinationPath $mysqlArchive -CompressionLevel Optimal -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'Instalar.cmd') -Destination $payload
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'Instalar.ps1') -Destination $payload
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'Manual_Instalacion.txt') -Destination $payload
Copy-Item -LiteralPath (Join-Path $projectRoot 'database\schema.sql') -Destination $payload
Copy-Item -LiteralPath $vcRedist -Destination $payload

$files = @(
    'Instalar.cmd',
    'Instalar.ps1',
    'Manual_Instalacion.txt',
    'schema.sql',
    'SistemaEnvases.zip',
    'MySQLServer.zip',
    'vc_redist.x64.exe'
)

$strings = [System.Text.StringBuilder]::new()
$sourceEntries = [System.Text.StringBuilder]::new()
for ($index = 0; $index -lt $files.Count; $index++) {
    [void]$strings.AppendLine(('FILE{0}="{1}"' -f $index, $files[$index]))
    [void]$sourceEntries.AppendLine(('%FILE{0}%=' -f $index))
}

$payloadWithSlash = $payload.TrimEnd('\') + '\'
$sed = @"
[Version]
Class=IEXPRESS
SEDVersion=3

[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=1
HideExtractAnimation=0
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=
DisplayLicense=
FinishMessage=
TargetName=$outputExe
FriendlyName=Sistema de Envases Cero Dietas
AppLaunched=Instalar.cmd
PostInstallCmd=<None>
AdminQuietInstCmd=
UserQuietInstCmd=
SourceFiles=SourceFiles

[Strings]
$strings
[SourceFiles]
SourceFiles0=$payloadWithSlash

[SourceFiles0]
$sourceEntries
"@

Set-Content -LiteralPath $sedPath -Value $sed -Encoding ASCII
if (Test-Path -LiteralPath $outputExe) {
    Remove-Item -LiteralPath $outputExe -Force
}
$packagingStarted = Get-Date
& "$env:SystemRoot\System32\iexpress.exe" /N $sedPath
$deadline = $packagingStarted.AddMinutes(10)
do {
    Start-Sleep -Seconds 2
    $packagingProcesses = Get-Process iexpress, makecab -ErrorAction SilentlyContinue |
        Where-Object { $_.StartTime -ge $packagingStarted.AddSeconds(-2) }
} while ($packagingProcesses -and (Get-Date) -lt $deadline)

if ($packagingProcesses -or -not (Test-Path -LiteralPath $outputExe)) {
    throw 'IExpress no pudo generar el instalador final.'
}

$previousInstaller = Join-Path $UsbDirectory 'INSTALAR_SISTEMA_ENVASES.exe'
if (Test-Path -LiteralPath $previousInstaller) {
    Remove-Item -LiteralPath $previousInstaller -Force
}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'Manual_Instalacion.txt') -Destination $UsbDirectory -Force
$installerHash = Get-FileHash -Algorithm SHA256 -LiteralPath $outputExe
"SHA256  $($installerHash.Hash)  $($installerHash.Path | Split-Path -Leaf)" |
    Set-Content -LiteralPath (Join-Path $UsbDirectory 'SHA256.txt') -Encoding ASCII
$installerHash

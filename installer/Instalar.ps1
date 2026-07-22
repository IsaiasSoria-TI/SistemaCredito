$ErrorActionPreference = 'Stop'

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function New-SecurePassword {
    $bytes = [byte[]]::new(32)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes).Replace('+', '-').Replace('/', '_').TrimEnd('=')
}

function Set-ConfigPermissions([string]$Path, [bool]$AllowUsers) {
    & icacls.exe $Path /inheritance:r | Out-Null
    & icacls.exe $Path /grant:r '*S-1-5-18:(F)' '*S-1-5-32-544:(F)' | Out-Null
    if ($AllowUsers) {
        & icacls.exe $Path /grant:r '*S-1-5-32-545:(R)' | Out-Null
    }
}

function Wait-MySql([string]$Client, [string]$RootPassword, [bool]$HasPassword) {
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        try {
            $arguments = @('--protocol=TCP', '-h', '127.0.0.1', '-P', '3307', '-u', 'root')
            if ($HasPassword) {
                $arguments += "--password=$RootPassword"
            }
            $arguments += @('-N', '-e', 'SELECT 1;')
            $output = & $Client @arguments 2>$null
            if ($LASTEXITCODE -eq 0 -and $output -contains '1') {
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 1
    }
    throw 'MySQL no respondio en el puerto local 3307.'
}

if (-not (Test-Administrator)) {
    $arguments = @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', ('"{0}"' -f $PSCommandPath)
    )
    $process = Start-Process powershell.exe -Verb RunAs -ArgumentList $arguments -Wait -PassThru
    exit $process.ExitCode
}

$source = Split-Path -Parent $PSCommandPath
$programRoot = Join-Path $env:ProgramFiles 'Cero Dietas'
$applicationDirectory = Join-Path $programRoot 'Sistema Envases'
$mysqlDirectory = Join-Path $programRoot 'MySQL'
$dataRoot = Join-Path $env:ProgramData 'CeroDietas'
$databaseRoot = Join-Path $dataRoot 'Database'
$databaseData = Join-Path $databaseRoot 'data'
$databaseConfig = Join-Path $databaseRoot 'my.ini'
$applicationConfig = Join-Path $dataRoot 'database.properties'
$adminConfig = Join-Path $dataRoot 'database-admin.properties'
$serviceName = 'CeroDietasMySQL'
$logPath = Join-Path $dataRoot 'instalacion.log'
$appArchive = Join-Path $source 'SistemaEnvases.zip'
$schemaPath = Join-Path $source 'schema.sql'
$mysqlArchive = Join-Path $source 'MySQLServer.zip'
$vcInstaller = Join-Path $source 'vc_redist.x64.exe'

New-Item -ItemType Directory -Path $dataRoot, $databaseRoot -Force | Out-Null
& icacls.exe $databaseRoot /inheritance:r /grant:r '*S-1-5-18:(OI)(CI)(F)' '*S-1-5-32-544:(OI)(CI)(F)' | Out-Null
Start-Transcript -Path $logPath -Append | Out-Null

try {
    Write-Host 'Instalando Sistema de Envases Cero Dietas...' -ForegroundColor Cyan

    if (-not (Test-Path -LiteralPath $appArchive) -or
        -not (Test-Path -LiteralPath $mysqlArchive) -or
        -not (Test-Path -LiteralPath $schemaPath)) {
        throw 'El instalador esta incompleto: faltan archivos de la aplicacion.'
    }

    if (Test-Path -LiteralPath $vcInstaller) {
        $vcProcess = Start-Process -FilePath $vcInstaller -ArgumentList '/install', '/quiet', '/norestart' -Wait -PassThru
        if ($vcProcess.ExitCode -notin 0, 1638, 3010) {
            throw "No se pudo instalar Microsoft Visual C++ (codigo $($vcProcess.ExitCode))."
        }
    }

    $mysqld = Join-Path $mysqlDirectory 'bin\mysqld.exe'
    $mysql = Join-Path $mysqlDirectory 'bin\mysql.exe'
    if (-not (Test-Path -LiteralPath $mysqld) -or -not (Test-Path -LiteralPath $mysql)) {
        Write-Host 'Copiando el motor local de base de datos...'
        $mysqlStaging = Join-Path $env:TEMP ("CeroDietasMySQL-{0}" -f [Guid]::NewGuid())
        New-Item -ItemType Directory -Path $mysqlStaging -Force | Out-Null
        try {
            Expand-Archive -LiteralPath $mysqlArchive -DestinationPath $mysqlStaging -Force
            $newMySql = Join-Path $mysqlStaging 'MySQL'
            if (-not (Test-Path -LiteralPath (Join-Path $newMySql 'bin\mysqld.exe')) -or
                -not (Test-Path -LiteralPath (Join-Path $newMySql 'bin\mysql.exe'))) {
                throw 'El paquete del motor MySQL esta incompleto.'
            }
            New-Item -ItemType Directory -Path $programRoot -Force | Out-Null
            if (Test-Path -LiteralPath $mysqlDirectory) {
                Remove-Item -LiteralPath $mysqlDirectory -Recurse -Force
            }
            Move-Item -LiteralPath $newMySql -Destination $mysqlDirectory
        } finally {
            if (Test-Path -LiteralPath $mysqlStaging) {
                Remove-Item -LiteralPath $mysqlStaging -Recurse -Force
            }
        }
    }

    if (-not (Test-Path -LiteralPath $mysqld) -or -not (Test-Path -LiteralPath $mysql)) {
        throw 'No se encontraron los ejecutables del motor MySQL incluido.'
    }

    $baseDirectory = Split-Path -Parent (Split-Path -Parent $mysqld)
    $baseIni = $baseDirectory.Replace('\', '/')
    $dataIni = $databaseData.Replace('\', '/')
    $iniContent = @"
[mysqld]
basedir=$baseIni
datadir=$dataIni
port=3307
bind-address=127.0.0.1
mysqlx=0
character-set-server=utf8mb4
collation-server=utf8mb4_0900_ai_ci
max_connections=80

[client]
port=3307
host=127.0.0.1
"@
    Set-Content -LiteralPath $databaseConfig -Value $iniContent -Encoding ASCII

    $freshDatabase = -not (Test-Path -LiteralPath (Join-Path $databaseData 'mysql'))
    if ($freshDatabase) {
        New-Item -ItemType Directory -Path $databaseData -Force | Out-Null
        Write-Host 'Preparando la base de datos por primera vez...'
        & $mysqld "--defaults-file=$databaseConfig" '--initialize-insecure' '--console'
        if ($LASTEXITCODE -ne 0) {
            throw 'MySQL no pudo inicializar su carpeta de datos.'
        }
    }

    $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    if (-not $service) {
        & $mysqld '--install' $serviceName "--defaults-file=$databaseConfig"
        if ($LASTEXITCODE -ne 0) {
            throw 'No se pudo registrar el servicio local de MySQL.'
        }
    }

    Set-Service -Name $serviceName -StartupType Automatic
    $service = Get-Service -Name $serviceName
    if ($service.Status -ne 'Running') {
        Start-Service -Name $serviceName
    }

    if ($freshDatabase) {
        Wait-MySql -Client $mysql -RootPassword '' -HasPassword $false
        $appPassword = New-SecurePassword
        $rootPassword = New-SecurePassword

        Get-Content -LiteralPath $schemaPath -Raw | & $mysql '--protocol=TCP' '-h' '127.0.0.1' '-P' '3307' '-u' 'root'
        if ($LASTEXITCODE -ne 0) {
            throw 'No se pudo crear la estructura inicial de la base de datos.'
        }

        $securitySql = @"
CREATE USER IF NOT EXISTS 'cero_dietas_app'@'127.0.0.1' IDENTIFIED BY '$appPassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON sistemagarantia.* TO 'cero_dietas_app'@'127.0.0.1';
ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';
FLUSH PRIVILEGES;
"@
        $securitySql | & $mysql '--protocol=TCP' '-h' '127.0.0.1' '-P' '3307' '-u' 'root'
        if ($LASTEXITCODE -ne 0) {
            throw 'No se pudieron proteger las cuentas internas de la base de datos.'
        }

        @("host=127.0.0.1", "port=3307", "name=sistemagarantia", "user=cero_dietas_app", "password=$appPassword") |
            Set-Content -LiteralPath $applicationConfig -Encoding ASCII
        @("host=127.0.0.1", "port=3307", "user=root", "password=$rootPassword") |
            Set-Content -LiteralPath $adminConfig -Encoding ASCII
        Set-ConfigPermissions -Path $applicationConfig -AllowUsers $true
        Set-ConfigPermissions -Path $adminConfig -AllowUsers $false
    } else {
        if (-not (Test-Path -LiteralPath $applicationConfig) -or -not (Test-Path -LiteralPath $adminConfig)) {
            throw 'La base ya existe, pero faltan sus archivos protegidos de configuracion.'
        }
        $adminValues = ConvertFrom-StringData (Get-Content -LiteralPath $adminConfig -Raw)
        Wait-MySql -Client $mysql -RootPassword $adminValues.password -HasPassword $true
    }

    Write-Host 'Copiando la aplicacion...'
    $staging = Join-Path $env:TEMP ("SistemaEnvases-{0}" -f [Guid]::NewGuid())
    New-Item -ItemType Directory -Path $staging -Force | Out-Null
    try {
        Expand-Archive -LiteralPath $appArchive -DestinationPath $staging -Force
        $newApplication = Join-Path $staging 'Sistema Envases'
        if (-not (Test-Path -LiteralPath (Join-Path $newApplication 'Sistema Envases.exe'))) {
            throw 'El paquete de la aplicacion no contiene el ejecutable esperado.'
        }
        Get-Process -Name 'Sistema Envases' -ErrorAction SilentlyContinue | Stop-Process -Force
        New-Item -ItemType Directory -Path $programRoot -Force | Out-Null
        if (Test-Path -LiteralPath $applicationDirectory) {
            Remove-Item -LiteralPath $applicationDirectory -Recurse -Force
        }
        Move-Item -LiteralPath $newApplication -Destination $applicationDirectory
    } finally {
        if (Test-Path -LiteralPath $staging) {
            Remove-Item -LiteralPath $staging -Recurse -Force
        }
    }

    $executable = Join-Path $applicationDirectory 'Sistema Envases.exe'
    $shell = New-Object -ComObject WScript.Shell
    $desktopShortcut = $shell.CreateShortcut((Join-Path ([Environment]::GetFolderPath('CommonDesktopDirectory')) 'Sistema Envases.lnk'))
    $desktopShortcut.TargetPath = $executable
    $desktopShortcut.WorkingDirectory = $applicationDirectory
    $desktopShortcut.Save()

    $startMenu = Join-Path $env:ProgramData 'Microsoft\Windows\Start Menu\Programs\Cero Dietas'
    New-Item -ItemType Directory -Path $startMenu -Force | Out-Null
    $menuShortcut = $shell.CreateShortcut((Join-Path $startMenu 'Sistema Envases.lnk'))
    $menuShortcut.TargetPath = $executable
    $menuShortcut.WorkingDirectory = $applicationDirectory
    $menuShortcut.Save()

    Write-Host ''
    Write-Host 'Instalacion completada correctamente.' -ForegroundColor Green
    Write-Host 'Usuario inicial: admin'
    Write-Host 'Contrasena inicial: Admin123*'
    Write-Host 'Cambie esa contrasena desde Mi Perfil al ingresar.' -ForegroundColor Yellow
    Start-Process -FilePath $executable
    exit 0
} catch {
    Write-Host ''
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Registro: $logPath"
    exit 1
} finally {
    Stop-Transcript -ErrorAction SilentlyContinue | Out-Null
}

param(
[switch]$SkipAgent,
[switch]$SkipFrontend,
[switch]$SkipBackend,
[switch]$ReinstallAgent,
[switch]$OpenBrowser
)

$ErrorActionPreference = "Stop"

function Write-Step {
param([string]$Message)
Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host $Message -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
}

function Escape-PSPath {
param([string]$Path)
return ($Path -replace "'", "''")
}

function Start-PSWindow {
param(
[string]$Name,
[string]$CommandText
)

$bytes = [System.Text.Encoding]::Unicode.GetBytes($CommandText)
$encoded = [Convert]::ToBase64String($bytes)

Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit -ExecutionPolicy Bypass -EncodedCommand $encoded"

Write-Host "Started window: $Name" -ForegroundColor Green

}

$ProjectRoot = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
$ProjectRoot = (Get-Location).Path
}

if (!(Test-Path (Join-Path $ProjectRoot "backend-java"))) {
$ProjectRoot = (Get-Location).Path
}

if (!(Test-Path (Join-Path $ProjectRoot "backend-java"))) {
throw "Cannot find backend-java. Please put start-local.ps1 in the repair-ai-saas project root."
}

$BackendDir = Join-Path $ProjectRoot "backend-java"
$AgentDir = Join-Path $ProjectRoot "agent-python"
$FrontendDir = Join-Path $ProjectRoot "frontend"
$MavenCmd = Join-Path $ProjectRoot ".tools\apache-maven-3.9.9\bin\mvn.cmd"

$ProjectRootQ = Escape-PSPath $ProjectRoot
$BackendDirQ = Escape-PSPath $BackendDir
$AgentDirQ = Escape-PSPath $AgentDir
$FrontendDirQ = Escape-PSPath $FrontendDir
$MavenCmdQ = Escape-PSPath $MavenCmd

Write-Step "repair-ai-saas local startup"
Write-Host "Project root: $ProjectRoot"

Set-Location $ProjectRoot

Write-Step "1. Git status"
try {
git branch --show-current
git status --short
} catch {
Write-Host "Git check failed. Continue startup." -ForegroundColor Yellow
}

Write-Step "2. Start Docker services"
docker compose up -d mysql redis qdrant

Write-Host ""
Write-Host "Docker status:" -ForegroundColor Green
docker compose ps

if (-not $SkipBackend) {
if (!(Test-Path $MavenCmd)) {
throw "Maven not found: $MavenCmd"
}

$BackendCommand = @"

`$ErrorActionPreference = 'Stop'
Set-Location '$ProjectRootQ'
Write-Host ''
Write-Host 'Starting Java backend: http://localhost:8080' -ForegroundColor Cyan
Write-Host 'Backend dir: $BackendDirQ' -ForegroundColor Gray
& '$MavenCmdQ' -f 'backend-java\pom.xml' spring-boot:run '-Dspring-boot.run.profiles=dev'
"@

Write-Step "3. Start Java backend window"
Start-PSWindow -Name "backend-java" -CommandText $BackendCommand

} else {
Write-Host "Skip Java backend." -ForegroundColor Yellow
}

if (-not $SkipAgent) {
if (!(Test-Path $AgentDir)) {
Write-Host "agent-python not found. Skip Python Agent." -ForegroundColor Yellow
} else {
$ReinstallValue = if ($ReinstallAgent) { "`$true" } else { "`$false" }

    $AgentCommand = @"

`$ErrorActionPreference = 'Stop'
Set-Location '$AgentDirQ'

Write-Host ''
Write-Host 'Starting Python Agent: http://localhost:8090' -ForegroundColor Cyan
Write-Host 'Agent dir: $AgentDirQ' -ForegroundColor Gray

if (!(Test-Path '.venv\Scripts\python.exe')) {
Write-Host 'Creating Python virtual environment...' -ForegroundColor Yellow

if (Get-Command python -ErrorAction SilentlyContinue) {
    python -m venv .venv
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    py -3 -m venv .venv
} else {
    throw 'Python command not found. Please install Python 3 first.'
}

}

`$PythonExe = Join-Path (Get-Location) '.venv\Scripts\python.exe'
`$Marker = Join-Path (Get-Location) '.venv.deps-installed'
`$ReinstallAgent = $ReinstallValue

if (`$ReinstallAgent -or !(Test-Path `$Marker)) {
Write-Host 'Installing Python Agent dependencies...' -ForegroundColor Yellow

& `$PythonExe -m pip install --upgrade pip setuptools wheel

if (Test-Path 'pyproject.toml') {
    & `$PythonExe -m pip install -e .
} elseif (Test-Path 'requirements.txt') {
    & `$PythonExe -m pip install -r requirements.txt
} else {
    throw 'No pyproject.toml or requirements.txt found in agent-python.'
}

& `$PythonExe -m pip install 'uvicorn[standard]'

New-Item -ItemType File -Force `$Marker | Out-Null

} else {
Write-Host 'Python dependencies already installed. Use -ReinstallAgent to reinstall.' -ForegroundColor Green
}

Write-Host 'Running uvicorn app.main:app ...' -ForegroundColor Green
& `$PythonExe -m uvicorn app.main:app --host 0.0.0.0 --port 8090 --reload
"@

    Write-Step "4. Start Python Agent window"
    Start-PSWindow -Name "agent-python" -CommandText $AgentCommand
}

} else {
Write-Host "Skip Python Agent." -ForegroundColor Yellow
}

if (-not $SkipFrontend) {
if (!(Test-Path $FrontendDir)) {
Write-Host "frontend not found. Skip Web frontend." -ForegroundColor Yellow
} else {
$FrontendCommand = @"
`$ErrorActionPreference = 'Stop'
Set-Location '$FrontendDirQ'

Write-Host ''
Write-Host 'Starting Web frontend: http://localhost:3000' -ForegroundColor Cyan
Write-Host 'Frontend dir: $FrontendDirQ' -ForegroundColor Gray

if (!(Test-Path 'node_modules')) {
Write-Host 'node_modules not found. Running npm install...' -ForegroundColor Yellow
npm install
}

npm run dev
"@

    Write-Step "5. Start Web frontend window"
    Start-PSWindow -Name "frontend" -CommandText $FrontendCommand
}

} else {
Write-Host "Skip Web frontend." -ForegroundColor Yellow
}

Write-Step "6. Startup commands sent"

Write-Host "Backend:    http://localhost:8080" -ForegroundColor Green
Write-Host "Agent:      http://localhost:8090" -ForegroundColor Green
Write-Host "Frontend:   http://localhost:3000" -ForegroundColor Green

Write-Host ""
Write-Host "Common pages:" -ForegroundColor Cyan
Write-Host "Admin login:     http://localhost:3000/admin/login"
Write-Host "Platform tenants:http://localhost:3000/platform/tenants"
Write-Host "Admin tickets:   http://localhost:3000/admin/tickets"
Write-Host "Technician H5:   http://localhost:3000/technician/login"

Write-Host ""
Write-Host "Miniapp:" -ForegroundColor Cyan
Write-Host "Import this folder in WeChat DevTools:"
Write-Host "$ProjectRoot\miniapp-repair"
Write-Host "For local debugging, enable: do not verify legal domain / TLS / HTTPS certificate."

if ($OpenBrowser) {
Start-Sleep -Seconds 3
Start-Process "http://localhost:3000/admin/login"
}

Write-Host ""
Write-Host "To stop Java / Python / Frontend: press Ctrl + C in each window." -ForegroundColor Yellow
Write-Host "To stop Docker services: docker compose stop mysql redis qdrant" -ForegroundColor Yellow

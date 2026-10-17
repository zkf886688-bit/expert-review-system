# 运行全部自动化测试（Windows）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

if (-not $env:JAVA_HOME) {
    $java = (Get-Command java -ErrorAction SilentlyContinue).Source
    if ($java) { $env:JAVA_HOME = Split-Path (Split-Path $java -Parent) -Parent }
}

Write-Host "=== 后端 mvn test ===" -ForegroundColor Cyan
Push-Location (Join-Path $root "backend")
mvn -s ..\maven-settings.xml test
Pop-Location

Write-Host "=== 前端 build ===" -ForegroundColor Cyan
Push-Location (Join-Path $root "frontend")
npm run build
Write-Host "=== 前端 E2E ===" -ForegroundColor Cyan
npm run test:e2e
Pop-Location

Write-Host "全部测试通过" -ForegroundColor Green

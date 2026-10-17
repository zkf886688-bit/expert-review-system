# 启动后端（Windows PowerShell）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backend = Join-Path $root "backend"

function Stop-Port8080 {
    $pids = @(
        netstat -ano |
            Select-String ":8080\s+.*LISTENING" |
            ForEach-Object { ($_ -split '\s+')[-1] } |
            Where-Object { $_ -match '^\d+$' } |
            Select-Object -Unique
    )
    foreach ($procId in $pids) {
        Write-Host "检测到 8080 端口已被 PID $procId 占用，正在停止旧后端..."
        taskkill /PID $procId /F 2>$null | Out-Null
    }
    if ($pids.Count -gt 0) {
        Start-Sleep -Seconds 2
    }
}

if (-not $env:JAVA_HOME) {
    $java = (Get-Command java -ErrorAction SilentlyContinue).Source
    if ($java) {
        $env:JAVA_HOME = Split-Path (Split-Path $java -Parent) -Parent
        Write-Host "JAVA_HOME=$env:JAVA_HOME"
    } else {
        Write-Error "未找到 Java，请安装 JDK 17 并设置 JAVA_HOME"
    }
}

Stop-Port8080

Set-Location $backend
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"
Write-Host "Starting backend http://localhost:8080 ..."
Write-Host "Wait for: Started ExpertReviewApplication"
Write-Host "Health: http://localhost:8080/api/public/health"
Write-Host "Frontend: http://localhost:5173"
mvn -s ..\maven-settings.xml spring-boot:run

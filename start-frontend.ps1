# 启动前端（Windows PowerShell）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $root "frontend")

function Stop-Port5173 {
    $pids = @(
        netstat -ano |
            Select-String ":5173\s+.*LISTENING" |
            ForEach-Object { ($_ -split '\s+')[-1] } |
            Where-Object { $_ -match '^\d+$' } |
            Select-Object -Unique
    )
    foreach ($procId in $pids) {
        Write-Host "检测到 5173 端口已被 PID $procId 占用，正在停止旧前端..."
        taskkill /PID $procId /F 2>$null | Out-Null
    }
    if ($pids.Count -gt 0) {
        Start-Sleep -Seconds 1
    }
}

Stop-Port5173

Write-Host "正在启动前端 http://localhost:5173 ..."
Write-Host "请确保后端已在 http://localhost:8080 运行"
npm run dev

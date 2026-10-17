# 停止占用 8080 的后端进程
$pids = @(
    netstat -ano |
        Select-String ":8080\s+.*LISTENING" |
        ForEach-Object { ($_ -split '\s+')[-1] } |
        Where-Object { $_ -match '^\d+$' } |
        Select-Object -Unique
)
if ($pids.Count -eq 0) {
    Write-Host "8080 端口上没有运行中的后端。"
    exit 0
}
foreach ($procId in $pids) {
    Write-Host "停止 PID $procId ..."
    taskkill /PID $procId /F
}
Write-Host "后端已停止。"

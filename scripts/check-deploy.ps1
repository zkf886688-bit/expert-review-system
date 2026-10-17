# 部署前自检（Windows PowerShell）
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$fail = $false
function Ok($msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow; $script:fail = $true }

if (Test-Path ".env") { Ok ".env 已存在" } else { Warn "缺少 .env，请复制 .env.example 并填写" }

if (Test-Path ".env") {
    $content = Get-Content ".env" -Raw
    if ($content -match "change-me") { Warn ".env 中仍含 change-me 占位符" }
    if ($content -match "(?m)^JWT_SECRET=(.+)$") {
        $secret = $Matches[1].Trim()
        if ($secret.Length -lt 32) { Warn "JWT_SECRET 长度不足 32 字符" }
    }
    if ($content -match "(?m)^MYSQL_ROOT_PASSWORD=(.+)$") {
        $dbPass = $Matches[1].Trim()
        if ($dbPass -eq "root" -or $dbPass.Length -lt 12) {
            Warn "MYSQL_ROOT_PASSWORD 过弱，请使用至少 12 位强密码"
        }
    }
}

try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/public/health" -UseBasicParsing -TimeoutSec 3
    if ($r.StatusCode -eq 200) { Ok "后端健康检查正常" }
} catch {
    Warn "后端未响应（若尚未启动可忽略）"
}

if (-not $fail) {
    Write-Host "部署自检通过" -ForegroundColor Green
} else {
    Write-Host "部署自检有警告，请处理后再上线" -ForegroundColor Yellow
    exit 1
}

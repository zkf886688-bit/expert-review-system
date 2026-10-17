# 一键升级已有 expert_review 数据库（Windows PowerShell）
param(
    [string]$Host = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Database = "expert_review"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$sql = Join-Path $root "sql\fix_startup.sql"

if (-not (Test-Path $sql)) {
    Write-Error "找不到 $sql"
}

$pass = Read-Host "MySQL 密码 ($User)" -AsSecureString
$BSTR = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($pass)
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

Write-Host "正在执行 fix_startup.sql ..."
Get-Content $sql -Raw | & mysql -h $Host -P $Port -u $User -p$plain $Database
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "数据库升级完成"

# MySQL 备份（Windows PowerShell）
param(
    [string]$Host = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Database = "expert_review",
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $OutDir) {
    $OutDir = Join-Path $root "backups"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outFile = Join-Path $OutDir "${Database}_${stamp}.sql"

Write-Host "备份 $Database -> $outFile"
$pass = Read-Host "MySQL 密码 ($User)" -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($pass))

& mysqldump -h $Host -P $Port -u $User "-p$plain" --single-transaction --routines --triggers $Database | Out-File -FilePath $outFile -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw "mysqldump 失败" }
Write-Host "[OK] 备份完成: $outFile" -ForegroundColor Green

param(
    [Parameter(Mandatory=$true)][string]$Device,
    [Parameter(Mandatory=$true)][string]$ApkPath,
    [string]$BackupDirectory=(Join-Path (Get-Location) '.covermanager-backups'),
    [switch]$Preview
)
$ErrorActionPreference='Stop'
function Invoke-Device([string[]]$Arguments) {
    $result=& adb -s $Device @Arguments
    if($LASTEXITCODE -ne 0){throw "ADB failed: $($Arguments[0])"}
    return ($result -join "`n").Trim()
}
function Quote-Shell([string]$Value){return "'"+$Value.Replace("'","'\''")+"'"}
$version=Invoke-Device @('shell','dumpsys','package','com.samsung.android.multistar')
if($version -notmatch '(?m)^\s*versionName=([^\r\n]+)'){throw 'MultiStar is not installed.'}
$installedVersion=$Matches[1].Trim()
$taskUser=Invoke-Device @('shell','am','get-current-user')
if($taskUser -ne '0'){throw 'This setup script supports the primary Android user only.'}
$key='multistar_setting_repository'
$before=Invoke-Device @('shell','settings','get','secure',$key)
$fields=$before.Split([char]'/',[StringSplitOptions]::None)
if($fields.Length -ne 28 -or $fields[27] -ne ''){throw 'Unexpected configuration format.'}
foreach($index in @(20,25)){
    if($fields[$index] -in @('','0')){continue}
    $pattern=if($index -eq 20){'^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+$'}else{'^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+,\d+$'}
    foreach($entry in $fields[$index].Split(';')){if($entry -notmatch $pattern){throw 'Unexpected application list format.'}}
}
$changed=$fields.Clone()
foreach($pair in @(@(20,'local.covermanager'),@(25,'local.covermanager,0'))){
    $index=[int]$pair[0];$entry=[string]$pair[1]
    $entries=if($changed[$index] -in @('','0')){@()}else{@($changed[$index].Split(';'))}
    if($entries -notcontains $entry){$entries+= $entry}
    $changed[$index]=$entries -join ';'
}
$after=$changed -join '/'
for($i=0;$i -lt 28;$i++){if($i -notin @(20,25) -and $fields[$i] -cne $changed[$i]){throw 'Unrelated field changed.'}}
if($Preview){Write-Output 'Preview passed: compatible configuration. Only fields 20 and 25 may change. No installation, grant, backup, or write performed.';exit 0}
if(-not(Test-Path -LiteralPath $ApkPath -PathType Leaf)){throw 'APK file not found.'}
New-Item -ItemType Directory -Force -Path $BackupDirectory | Out-Null
$backupPath=Join-Path $BackupDirectory ((Get-Date -Format 'yyyyMMdd-HHmmss-fff')+'.json')
@{namespace='secure';key=$key;value=$before;multistar=$installedVersion} | ConvertTo-Json | Set-Content -LiteralPath $backupPath -Encoding UTF8
Write-Output (Invoke-Device @('install','-r',(Resolve-Path -LiteralPath $ApkPath).Path))
Invoke-Device @('shell','pm','grant','--user','0','local.covermanager','android.permission.WRITE_SECURE_SETTINGS') | Out-Null
if((Invoke-Device @('shell','settings','get','secure',$key)) -cne $before){throw 'Configuration changed concurrently; retry. No settings written.'}
Invoke-Device @('shell','settings','put','secure',$key,(Quote-Shell $after)) | Out-Null
if((Invoke-Device @('shell','settings','get','secure',$key)) -cne $after){throw "Settings verification failed. Local backup: $backupPath"}
Write-Output "Setup complete. Backup saved locally: $backupPath"
Write-Output 'Unlock the cover screen; switch MultiStar widgets to refresh the app icon if needed.'

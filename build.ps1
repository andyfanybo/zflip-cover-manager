param(
    [Parameter(Mandatory=$true)][string]$Jdk,
    [Parameter(Mandatory=$true)][string]$AndroidSdk,
    [string]$BuildToolsVersion='35.0.0',
    [string]$KeyStore=''
)
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$taskBuild=Join-Path $projectRoot 'build'
$taskClasses=Join-Path $taskBuild 'classes'
$taskDex=Join-Path $taskBuild 'dex'
$taskTools=Join-Path $AndroidSdk "build-tools\$BuildToolsVersion"
$taskJar=Join-Path $AndroidSdk 'platforms\android-35\android.jar'
$taskJava=Join-Path $Jdk 'bin\java.exe'
function Run-Native([string]$Executable,[string[]]$Arguments) {
    & $Executable @Arguments
    if($LASTEXITCODE -ne 0){throw "Build failed: $Executable"}
}
New-Item -ItemType Directory -Force -Path $taskClasses,$taskDex | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskLibs=Join-Path $projectRoot 'libs'
New-Item -ItemType Directory -Force -Path $taskLibs | Out-Null
$taskDependencies=Get-Content -LiteralPath (Join-Path $projectRoot 'dependencies.json') -Raw | ConvertFrom-Json
$taskJars=@()
foreach($dependency in $taskDependencies){
    $taskArchive=Join-Path $taskLibs $dependency.name
    $taskModule=$dependency.name.Replace('-13.1.5.aar','')
    if(-not(Test-Path -LiteralPath $taskArchive)){
        Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/dev/rikka/shizuku/$taskModule/13.1.5/$($dependency.name)" -OutFile $taskArchive -UseBasicParsing
    }
    if((Get-FileHash -LiteralPath $taskArchive -Algorithm SHA256).Hash.ToLower() -ne $dependency.sha256){throw "Dependency checksum mismatch: $taskModule"}
    $taskLibraryJar=Join-Path $taskBuild "$taskModule.jar"
    $taskArchiveZip=[IO.Compression.ZipFile]::OpenRead($taskArchive)
    try {[IO.Compression.ZipFileExtensions]::ExtractToFile($taskArchiveZip.GetEntry('classes.jar'),$taskLibraryJar,$true)} finally {$taskArchiveZip.Dispose()}
    $taskJars+= $taskLibraryJar
}
$taskClasspath=(@($taskJar)+$taskJars) -join ';'
$taskSources=@(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
Run-Native (Join-Path $Jdk 'bin\javac.exe') (@('-encoding','UTF-8','-source','8','-target','8','-classpath',$taskClasspath,'-d',$taskClasses)+$taskSources)
$taskTests=Join-Path $taskBuild 'tests'
New-Item -ItemType Directory -Force -Path $taskTests | Out-Null
Run-Native (Join-Path $Jdk 'bin\javac.exe') @('-encoding','UTF-8','-classpath',$taskClasses,'-d',$taskTests,(Join-Path $projectRoot 'RepositoryTest.java'))
Run-Native $taskJava @('-cp',"$taskClasses;$taskTests",'RepositoryTest')
$taskClassFiles=@(Get-ChildItem -LiteralPath $taskClasses -Recurse -Filter '*.class' | ForEach-Object {$_.FullName})
Run-Native $taskJava (@('-cp',(Join-Path $taskTools 'lib\d8.jar'),'com.android.tools.r8.D8','--lib',$taskJar,'--min-api','30','--output',$taskDex)+$taskClassFiles+$taskJars)
$taskUnsigned=Join-Path $taskBuild 'unsigned.apk'
Run-Native (Join-Path $taskTools 'aapt.exe') @('package','-f','-M',(Join-Path $projectRoot 'AndroidManifest.xml'),'-S',(Join-Path $projectRoot 'res'),'-A',(Join-Path $projectRoot 'assets'),'-I',$taskJar,'-F',$taskUnsigned)
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskZip=[IO.Compression.ZipFile]::Open($taskUnsigned,[IO.Compression.ZipArchiveMode]::Update)
try {[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($taskZip,(Join-Path $taskDex 'classes.dex'),'classes.dex') | Out-Null} finally {$taskZip.Dispose()}
$taskAligned=Join-Path $taskBuild 'aligned.apk'
Run-Native (Join-Path $taskTools 'zipalign.exe') @('-f','4',$taskUnsigned,$taskAligned)
$taskKey=if($KeyStore){$KeyStore}else{Join-Path $projectRoot 'development.p12'}
if(-not(Test-Path -LiteralPath $taskKey)){
    Run-Native (Join-Path $Jdk 'bin\keytool.exe') @('-genkeypair','-keystore',$taskKey,'-storetype','PKCS12','-storepass','covermanager-local','-keypass','covermanager-local','-alias','covermanager','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Cover Manager Development')
}
$taskApk=Join-Path $taskBuild 'CoverManager-v0.3.1.apk'
Run-Native $taskJava @('-jar',(Join-Path $taskTools 'lib\apksigner.jar'),'sign','--ks',$taskKey,'--ks-pass','pass:covermanager-local','--out',$taskApk,$taskAligned)
Run-Native $taskJava @('-jar',(Join-Path $taskTools 'lib\apksigner.jar'),'verify','--verbose',$taskApk)
Write-Output "Built: $taskApk"

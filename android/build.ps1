param(
    [Parameter(Mandatory)] [string] $Sdk,
    [Parameter(Mandatory)] [string] $Keystore,
    [string] $Pnpm = 'pnpm',
    [string] $Alias = 'shanda'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
if (!$env:SHANDA_SIGN_PASS) { throw 'Set SHANDA_SIGN_PASS to the keystore password.' }
$tools = Join-Path $Sdk 'build-tools/35.0.0'
$platform = Join-Path $Sdk 'platforms/android-34/android.jar'
$Keystore = (Resolve-Path -LiteralPath $Keystore).Path
Push-Location $PSScriptRoot
try {
    Push-Location web
    try {
        & $Pnpm install --frozen-lockfile
        node build.mjs
        & $Pnpm exec tailwindcss -i app.css -o ../build/assets/app.css --minify
    } finally { Pop-Location }
    $work = New-Item -ItemType Directory -Path ("build/package-" + [guid]::NewGuid())
    $classes = New-Item -ItemType Directory -Path "$work/classes"
    $dex = New-Item -ItemType Directory -Path "$work/dex"
    javac --release 8 -encoding UTF-8 -classpath $platform -d $classes src/main/java/cn/rstrict/shanda/MainActivity.java
    jar cf "$work/classes.jar" -C $classes .
    & "$tools/d8.bat" --min-api 28 --lib $platform --output $dex "$work/classes.jar"
    & "$tools/aapt2.exe" link -I $platform --manifest src/main/AndroidManifest.xml -A build/assets -o "$work/base.apk"
    jar uf "$work/base.apk" -C $dex classes.dex
    & "$tools/zipalign.exe" -f 4 "$work/base.apk" "$work/aligned.apk"
    $apk = 'build/Shanda844-1.0.0.apk'
    & "$tools/apksigner.bat" sign --ks $Keystore --ks-key-alias $Alias --ks-pass env:SHANDA_SIGN_PASS --out $apk "$work/aligned.apk"
    & "$tools/apksigner.bat" verify --verbose --print-certs $apk
} finally { Pop-Location }

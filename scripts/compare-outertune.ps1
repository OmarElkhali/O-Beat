param(
    [string]$UpstreamPath = (Join-Path $PSScriptRoot '..\..\OuterTune-upstream'),
    [ValidatePattern('^[a-zA-Z0-9][a-zA-Z0-9._/-]*$')]
    [string]$Branch = 'dev'
)

$ErrorActionPreference = 'Stop'
$repository = 'https://github.com/OuterTune/OuterTune.git'
$projectPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$UpstreamPath = [IO.Path]::GetFullPath($UpstreamPath)

if (-not (Test-Path -LiteralPath (Join-Path $UpstreamPath '.git'))) {
    if (Test-Path -LiteralPath $UpstreamPath) {
        throw "The upstream directory exists but is not a Git checkout: $UpstreamPath"
    }
    git clone --depth=1 --single-branch --branch $Branch $repository $UpstreamPath
    if ($LASTEXITCODE -ne 0) { throw 'Could not clone OuterTune.' }
}

$origin = git -C $UpstreamPath remote get-url origin
if ($LASTEXITCODE -ne 0 -or $origin.TrimEnd('/') -ne $repository) {
    throw 'The upstream checkout must use the official OuterTune repository as origin.'
}
git -C $UpstreamPath fetch --depth=1 origin "${Branch}:refs/remotes/origin/$Branch"
if ($LASTEXITCODE -ne 0) { throw 'Could not fetch the requested upstream branch.' }
$revision = git -C $UpstreamPath rev-parse "refs/remotes/origin/$Branch"
if ($LASTEXITCODE -ne 0) { throw 'Could not resolve the upstream revision.' }

$reportPath = Join-Path $projectPath "out\outertune-compare\$revision"
New-Item -ItemType Directory -Path $reportPath -Force | Out-Null
$encoding = [Text.UTF8Encoding]::new($false)
[IO.File]::WriteAllText((Join-Path $reportPath 'revision.txt'), "$repository`n$Branch`n$revision`n", $encoding)

$files = @(
    'innertube/src/main/java/com/zionhuang/innertube/models/YouTubeClient.kt',
    'innertube/src/main/java/com/zionhuang/innertube/models/Context.kt',
    'innertube/src/main/java/com/zionhuang/innertube/InnerTube.kt',
    'innertube/src/main/java/com/zionhuang/innertube/YouTube.kt',
    'innertube/src/main/java/com/zionhuang/innertube/NewPipe.kt',
    'app/src/main/java/com/dd3boh/outertune/utils/YTPlayerUtils.kt'
)

foreach ($file in $files) {
    $name = $file.Replace('/', '_')
    $snapshotPath = Join-Path $reportPath "$name.upstream"
    $content = git -C $UpstreamPath show "${revision}:$file" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Missing on upstream branch '$Branch': $file"
        continue
    }
    [IO.File]::WriteAllText($snapshotPath, (($content -join "`n") + "`n"), $encoding)
    $difference = git -c core.autocrlf=false diff --no-index --ignore-space-at-eol -- (Join-Path $projectPath $file) $snapshotPath
    if ($LASTEXITCODE -gt 1) { throw "Could not compare $file" }
    [IO.File]::WriteAllText((Join-Path $reportPath "$name.diff"), ($difference -join "`n"), $encoding)
}

Write-Output "Upstream revision: $revision ($Branch)"
Write-Output "Comparison reports: $reportPath"

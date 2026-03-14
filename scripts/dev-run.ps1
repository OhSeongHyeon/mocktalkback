param(
    [string]$EnvFile = ".env.dev",
    [string]$Profile = "dev",
    [switch]$KeepOpen
)

$ErrorActionPreference = "Stop"

try {
    # 스크립트 위치 기준으로 프로젝트 루트를 계산한다.
    $repoRoot = Split-Path -Parent $PSScriptRoot

    if ([System.IO.Path]::IsPathRooted($EnvFile)) {
        $envPath = $EnvFile
    } else {
        $envPath = Join-Path $repoRoot $EnvFile
    }

    if (-not (Test-Path -LiteralPath $envPath)) {
        throw "Environment file not found: $envPath"
    }

    # .env 형식(KEY=VALUE)을 프로세스 환경변수에 주입한다.
    Get-Content -Path $envPath | ForEach-Object {
        $line = $_.Trim()

        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            return
        }

        if ($line -notmatch "=") {
            return
        }

        $pair = $line -split "=", 2
        $name = $pair[0].Trim()
        $value = $pair[1].Trim()

        if (-not $name) {
            return
        }

        # 값 뒤에 붙는 주석("  # 설명")은 제거한다.
        $value = $value -replace "\s+#.*$", ""

        # 따옴표로 감싼 값은 감싼 따옴표를 제거한다.
        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }

    Set-Item -Path "Env:SPRING_PROFILES_ACTIVE" -Value $Profile

    Push-Location $repoRoot
    try {
        & ".\gradlew.bat" "--no-daemon" "bootRun" "--args=--spring.profiles.active=$Profile"
    } finally {
        Pop-Location
    }
} catch {
    Write-Error $_
    if ($KeepOpen) {
        Read-Host "Run failed. Press Enter to close this window." | Out-Null
    }
    exit 1
}

if ($KeepOpen) {
    Read-Host "Run completed. Press Enter to close this window." | Out-Null
}

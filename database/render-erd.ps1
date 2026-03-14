param(
    [string]$InFile = "erd.mmd",
    [string]$OutFile = "erd.svg"
)

# ERD 원본(.mmd)을 SVG로 렌더링한다.
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$inputPath = Join-Path $scriptDir $InFile
$outputPath = Join-Path $scriptDir $OutFile

mmdc -i $inputPath -o $outputPath --backgroundColor transparent

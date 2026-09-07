param(
  [string]$SourceDirectory = (Join-Path $PSScriptRoot '..\public\sprites'),
  [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\public\sprites\web'),
  [int]$TargetHeight = 128
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$sourcePath = [System.IO.Path]::GetFullPath($SourceDirectory)
$outputPath = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($outputPath) | Out-Null

1..9 | ForEach-Object {
  $spriteNumber = $_
  $frames = 1..3 | ForEach-Object {
    $frameNumber = $_
    $filePath = Join-Path $sourcePath "$spriteNumber-D-$frameNumber.png"
    if (-not (Test-Path -LiteralPath $filePath)) {
      throw "Missing source sprite: $filePath"
    }

    $image = [System.Drawing.Image]::FromFile($filePath)
    try {
      [pscustomobject]@{
        FrameNumber = $frameNumber
        FilePath = $filePath
        Width = [Math]::Max(1, [int][Math]::Round($image.Width * $TargetHeight / $image.Height))
      }
    }
    finally {
      $image.Dispose()
    }
  }

  $canvasWidth = ($frames | Measure-Object -Property Width -Maximum).Maximum
  foreach ($frame in $frames) {
    $source = [System.Drawing.Image]::FromFile($frame.FilePath)
    $canvas = [System.Drawing.Bitmap]::new($canvasWidth, $TargetHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    try {
      $graphics.Clear([System.Drawing.Color]::Transparent)
      $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
      $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
      $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
      $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
      $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

      $offsetX = [int][Math]::Floor(($canvasWidth - $frame.Width) / 2)
      $destination = [System.Drawing.Rectangle]::new($offsetX, 0, $frame.Width, $TargetHeight)
      $graphics.DrawImage($source, $destination)

      $outputFile = Join-Path $outputPath "$spriteNumber-D-$($frame.FrameNumber).png"
      $canvas.Save($outputFile, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
      $graphics.Dispose()
      $canvas.Dispose()
      $source.Dispose()
    }
  }
}


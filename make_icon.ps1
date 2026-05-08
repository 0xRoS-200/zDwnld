Add-Type -AssemblyName System.Drawing

$IcoPath = 'C:\Users\offic\Desktop\zDwnld\Icon.ico'
$sizes = 16, 32, 48, 256
$entries = @()

foreach ($size in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

    # Dark background rounded rect
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 30, 30, 35))
    $g.FillRectangle($bgBrush, 0, 0, $size, $size)

    # Orange accent border
    $borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 255, 140, 0), [float]($size / 16.0))
    $g.DrawRectangle($borderPen, [float]($size * 0.05), [float]($size * 0.05), [float]($size * 0.9), [float]($size * 0.9))

    # Draw download arrow using filled polygons
    $orange = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 140, 0))

    # Arrow shaft (vertical rectangle)
    $shaftX = [int]($size * 0.38)
    $shaftY = [int]($size * 0.15)
    $shaftW = [int]($size * 0.24)
    $shaftH = [int]($size * 0.40)
    $g.FillRectangle($orange, $shaftX, $shaftY, $shaftW, $shaftH)

    # Arrow head (triangle pointing down)
    $tipX   = [int]($size * 0.50)
    $tipY   = [int]($size * 0.72)
    $leftX  = [int]($size * 0.20)
    $rightX = [int]($size * 0.80)
    $topY   = [int]($size * 0.55)
    $arrowHead = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new($leftX, $topY),
        [System.Drawing.PointF]::new($rightX, $topY),
        [System.Drawing.PointF]::new($tipX, $tipY)
    )
    $g.FillPolygon($orange, $arrowHead)

    # Base line (horizontal bar at bottom)
    $barBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 140, 0))
    $barH = [int]($size * 0.1)
    $barY = [int]($size * 0.80)
    $g.FillRectangle($barBrush, [int]($size * 0.15), $barY, [int]($size * 0.70), $barH)

    $g.Dispose()

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $entries += [PSCustomObject]@{ Size = $size; Data = $ms.ToArray() }
    $ms.Dispose()
    $bmp.Dispose()
    Write-Host "Rendered $size x $size"
}

$fs = [System.IO.File]::Create($IcoPath)
$w = New-Object System.IO.BinaryWriter($fs)
$w.Write([uint16]0); $w.Write([uint16]1); $w.Write([uint16]$entries.Count)
$offset = 6 + 16 * $entries.Count
foreach ($e in $entries) {
    $s = if ($e.Size -eq 256) { 0 } else { $e.Size }
    $w.Write([byte]$s); $w.Write([byte]$s); $w.Write([byte]0); $w.Write([byte]0)
    $w.Write([uint16]1); $w.Write([uint16]32)
    $w.Write([uint32]$e.Data.Length); $w.Write([uint32]$offset)
    $offset += $e.Data.Length
}
foreach ($e in $entries) { $w.Write($e.Data) }
$w.Close(); $fs.Close()
Write-Host "Done: Icon.ico created"

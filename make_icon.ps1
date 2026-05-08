Add-Type -AssemblyName System.Drawing

$PngPath = 'C:\Users\offic\Desktop\zDwnld\Icon.png'
$IcoPath = 'C:\Users\offic\Desktop\zDwnld\Icon.ico'
$sizes = 16, 32, 48, 256
$entries = @()

foreach ($size in $sizes) {
    $src = [System.Drawing.Image]::FromFile($PngPath)
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::White)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.DrawImage($src, 0, 0, $size, $size)
    $g.Dispose()
    $src.Dispose()

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $entries += [PSCustomObject]@{ Size = $size; Data = $ms.ToArray() }
    $ms.Dispose()
    $bmp.Dispose()
    Write-Host "Processed $size x $size"
}

$fs = [System.IO.File]::Create($IcoPath)
$w = New-Object System.IO.BinaryWriter($fs)

$w.Write([uint16]0)
$w.Write([uint16]1)
$w.Write([uint16]$entries.Count)

$offset = 6 + 16 * $entries.Count

foreach ($entry in $entries) {
    $s = if ($entry.Size -eq 256) { 0 } else { $entry.Size }
    $w.Write([byte]$s)
    $w.Write([byte]$s)
    $w.Write([byte]0)
    $w.Write([byte]0)
    $w.Write([uint16]1)
    $w.Write([uint16]32)
    $w.Write([uint32]$entry.Data.Length)
    $w.Write([uint32]$offset)
    $offset += $entry.Data.Length
}

foreach ($entry in $entries) {
    $w.Write($entry.Data)
}

$w.Close()
$fs.Close()
Write-Host ""
Write-Host "Done: Icon.ico created at $IcoPath"

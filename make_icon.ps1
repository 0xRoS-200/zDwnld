Add-Type -AssemblyName System.Drawing

$PngPath = 'C:\Users\offic\Desktop\zDwnld\Icon.png'
$IcoPath = 'C:\Users\offic\Desktop\zDwnld\Icon.ico'
$sizes = 16, 32, 48, 256

$entries = @()

foreach ($size in $sizes) {
    $src = [System.Drawing.Image]::FromFile($PngPath)
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.DrawImage($src, 0, 0, $size, $size)
    $g.Dispose()
    $src.Dispose()

    # Build raw BGRA pixel array (bottom-up for BMP)
    $pixelData = New-Object byte[] ($size * $size * 4)
    for ($y = $size - 1; $y -ge 0; $y--) {
        for ($x = 0; $x -lt $size; $x++) {
            $color = $bmp.GetPixel($x, $y)
            $idx = (($size - 1 - $y) * $size + $x) * 4
            $pixelData[$idx]     = $color.B
            $pixelData[$idx + 1] = $color.G
            $pixelData[$idx + 2] = $color.R
            $pixelData[$idx + 3] = $color.A
        }
    }
    $bmp.Dispose()

    # BITMAPINFOHEADER (40 bytes)
    $hdr = New-Object byte[] 40
    [System.BitConverter]::GetBytes([int32]40).CopyTo($hdr, 0)
    [System.BitConverter]::GetBytes([int32]$size).CopyTo($hdr, 4)
    [System.BitConverter]::GetBytes([int32]($size * 2)).CopyTo($hdr, 8)   # height doubled
    [System.BitConverter]::GetBytes([int16]1).CopyTo($hdr, 12)
    [System.BitConverter]::GetBytes([int16]32).CopyTo($hdr, 14)

    # AND mask (1 bit per pixel, padded to DWORD rows — all zeros = transparent handled by alpha)
    $maskRowBytes = [Math]::Ceiling($size / 32.0) * 4
    $maskData = New-Object byte[] ($maskRowBytes * $size)

    $imageData = New-Object byte[] ($hdr.Length + $pixelData.Length + $maskData.Length)
    $hdr.CopyTo($imageData, 0)
    $pixelData.CopyTo($imageData, $hdr.Length)
    $maskData.CopyTo($imageData, $hdr.Length + $pixelData.Length)

    $entries += [PSCustomObject]@{
        Size = $size
        Data = $imageData
    }

    Write-Host "Processed $size x $size"
}

$fs = [System.IO.File]::Create($IcoPath)
$w = New-Object System.IO.BinaryWriter($fs)

# ICONDIR
$w.Write([uint16]0)
$w.Write([uint16]1)
$w.Write([uint16]$entries.Count)

# Calculate data offset: 6 (header) + 16 (entry) * count
$offset = 6 + 16 * $entries.Count

# ICONDIRENTRY for each image
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

# Write all image data blobs
foreach ($entry in $entries) {
    $w.Write($entry.Data)
}

$w.Close()
$fs.Close()

Write-Host ""
Write-Host "Done: Icon.ico created at $IcoPath"


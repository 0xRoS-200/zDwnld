Add-Type -AssemblyName System.Drawing

$PngPath = 'C:\Users\offic\Desktop\zDwnld\Icon.png'
$IcoPath = 'C:\Users\offic\Desktop\zDwnld\Icon.ico'
$sizes = 16, 32, 48, 256
$pngDataList = New-Object System.Collections.ArrayList

foreach ($size in $sizes) {
    $src = [System.Drawing.Image]::FromFile($PngPath)
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($src, 0, 0, $size, $size)
    $g.Dispose()
    $src.Dispose()
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $null = $pngDataList.Add($ms.ToArray())
    $ms.Dispose()
    $bmp.Dispose()
}

$fs = [System.IO.File]::Create($IcoPath)
$writer = New-Object System.IO.BinaryWriter($fs)

$writer.Write([uint16]0)
$writer.Write([uint16]1)
$writer.Write([uint16]$sizes.Count)

$offset = 6 + 16 * $sizes.Count

for ($i = 0; $i -lt $sizes.Count; $i++) {
    $s = if ($sizes[$i] -eq 256) { 0 } else { $sizes[$i] }
    $writer.Write([byte]$s)
    $writer.Write([byte]$s)
    $writer.Write([byte]0)
    $writer.Write([byte]0)
    $writer.Write([uint16]1)
    $writer.Write([uint16]32)
    $writer.Write([uint32]$pngDataList[$i].Length)
    $writer.Write([uint32]$offset)
    $offset += $pngDataList[$i].Length
}

foreach ($data in $pngDataList) {
    $writer.Write($data)
}

$writer.Close()
$fs.Close()
Write-Host "Icon.ico created with sizes: 16, 32, 48, 256"

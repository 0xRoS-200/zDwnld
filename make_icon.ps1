Add-Type -AssemblyName System.Drawing
$PngPath = 'C:\Users\offic\Desktop\zDwnld\Icon.png'
$IcoPath = 'C:\Users\offic\Desktop\zDwnld\Icon.ico'

$sizes = 16, 32, 48, 256
$imageData = @()
$pngSource = [System.Drawing.Image]::FromFile($PngPath)

foreach ($size in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($pngSource, 0, 0, $size, $size)
    $g.Dispose()

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $imageData += ,$ms.ToArray()
    $ms.Dispose()
    $bmp.Dispose()
}
$pngSource.Dispose()

$fs = [System.IO.File]::Create($IcoPath)
$w = New-Object System.IO.BinaryWriter($fs)

$w.Write([uint16]0)
$w.Write([uint16]1)
$w.Write([uint16]$sizes.Count)

$offset = 6 + ($sizes.Count * 16)

for ($i=0; $i -lt $sizes.Count; $i++) {
    $size = $sizes[$i]
    $data = $imageData[$i]
    
    $val = if ($size -eq 256) { 0 } else { $size }
    $w.Write([byte]$val) # Width
    $w.Write([byte]$val) # Height
    $w.Write([byte]0) # Color count
    $w.Write([byte]0) # Reserved
    $w.Write([uint16]1) # Planes
    $w.Write([uint16]32) # Bits
    $w.Write([uint32]$data.Length)
    $w.Write([uint32]$offset)
    
    $offset += $data.Length
}

foreach ($data in $imageData) {
    $w.Write($data)
}

$w.Close()
$fs.Close()
Write-Host "Multi-resolution Icon.ico created successfully."

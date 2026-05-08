$PngPath = 'C:\Users\offic\Desktop\zDwnld\Icon.png'
$IcoPath = 'C:\Users\offic\Desktop\zDwnld\Icon.ico'

$pngBytes = [System.IO.File]::ReadAllBytes($PngPath)
$fs = [System.IO.File]::Create($IcoPath)
$w = New-Object System.IO.BinaryWriter($fs)

# ICONDIR header (6 bytes)
$w.Write([uint16]0) # Reserved
$w.Write([uint16]1) # Type (1 for Icon)
$w.Write([uint16]1) # Count (1 image)

# ICONDIRENTRY (16 bytes)
$w.Write([byte]0)   # Width (0 = 256)
$w.Write([byte]0)   # Height (0 = 256)
$w.Write([byte]0)   # Color count
$w.Write([byte]0)   # Reserved
$w.Write([uint16]1) # Planes
$w.Write([uint16]32)# Bit count
$w.Write([uint32]$pngBytes.Length) # Image size
$w.Write([uint32]22)               # Offset (6 + 16)

# Image data
$w.Write($pngBytes)

$w.Close()
$fs.Close()
Write-Host "High-quality 256x256 PNG-based Icon.ico created."

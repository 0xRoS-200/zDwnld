Write-Host "Step 1: Building portable app image using build_installer.bat..."
cmd.exe /c build_installer.bat
if ($LASTEXITCODE -ne 0) {
    Write-Error "build_installer.bat failed!"
    exit 1
}

if (-not (Test-Path "zDwnld")) {
    Write-Error "zDwnld directory not found!"
    exit 1
}

Write-Host "Step 2: Zipping portable app image to app.zip..."
if (Test-Path "app.zip") { Remove-Item "app.zip" -Force }

# Compress the contents of the zDwnld directory
Compress-Archive -Path "zDwnld\*" -DestinationPath "app.zip" -Force
if (-not (Test-Path "app.zip")) {
    Write-Error "Failed to create app.zip!"
    exit 1
}

Write-Host "Step 3: Compiling Launcher.cs with csc.exe..."
$cscPath = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (-not (Test-Path $cscPath)) {
    Write-Error "csc.exe not found at $cscPath"
    exit 1
}

if (Test-Path "zDwnld_portable.exe") { Remove-Item "zDwnld_portable.exe" -Force }

$args = @(
    "/target:winexe",
    "/win32icon:Icon.ico",
    "/out:zDwnld_portable.exe",
    "/resource:app.zip",
    "/r:System.IO.Compression.dll",
    "/r:System.IO.Compression.FileSystem.dll",
    "/r:System.Windows.Forms.dll",
    "Launcher.cs"
)

& $cscPath $args
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation with csc.exe failed!"
    exit 1
}

Write-Host "Step 4: Cleaning up temporary build files..."
if (Test-Path "app.zip") { Remove-Item "app.zip" -Force }

Write-Host "--------------------------------------------------------"
Write-Host "Success! Standalone executable created: zDwnld_portable.exe"
Write-Host "This single file contains the entire application and JRE."
Write-Host "You can distribute zDwnld_portable.exe on GitHub!"
Write-Host "--------------------------------------------------------"

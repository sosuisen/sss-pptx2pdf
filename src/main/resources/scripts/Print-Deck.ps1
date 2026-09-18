<#
.SYNOPSIS
Prints a PowerPoint deck to a PDF file through "Microsoft Print to PDF" at 100 % on A3 landscape.
.DESCRIPTION
The user's printing preferences of the printer (per-user DEVMODE, SetPrinter level 9, no admin
rights needed) are switched to A3 landscape for the duration of the print and restored afterwards.
PowerPoint prints the slides at actual size (FitToPage off), so each slide sits centred on the A3
page and can be cropped to the slide size afterwards.
.EXAMPLE
powershell -NoProfile -File Print-Deck.ps1 -Path deck.pptx -Out deck-a3.pdf
#>
param(
    [Parameter(Mandatory)][string]$Path,
    [Parameter(Mandatory)][string]$Out,
    [string]$Printer = 'Microsoft Print to PDF',
    [int]$FromSlide = 0,
    [int]$ToSlide = 0
)
$ErrorActionPreference = 'Stop'

Add-Type -Namespace Native -Name Winspool -MemberDefinition @'
[DllImport("winspool.drv", CharSet = CharSet.Unicode, SetLastError = true)]
public static extern bool OpenPrinter(string pPrinterName, out IntPtr phPrinter, IntPtr pDefault);
[DllImport("winspool.drv", SetLastError = true)]
public static extern bool ClosePrinter(IntPtr hPrinter);
[DllImport("winspool.drv", CharSet = CharSet.Unicode, SetLastError = true)]
public static extern bool GetPrinter(IntPtr hPrinter, uint Level, IntPtr pPrinter, uint cbBuf, out uint pcbNeeded);
[DllImport("winspool.drv", CharSet = CharSet.Unicode, SetLastError = true)]
public static extern bool SetPrinter(IntPtr hPrinter, uint Level, IntPtr pPrinter, uint Command);
'@

# DEVMODEW field offsets (Unicode): dmDeviceName[32] = 64 bytes, dmSpecVersion 64, dmDriverVersion 66,
# dmSize 68, dmDriverExtra 70, dmFields 72 (DWORD), dmOrientation 76, dmPaperSize 78 (shorts).
$DM_ORIENTATION = 0x1; $DM_PAPERSIZE = 0x2
$DMORIENT_LANDSCAPE = 2; $DMPAPER_A3 = 8
$OFF_FIELDS = 72; $OFF_ORIENTATION = 76; $OFF_PAPERSIZE = 78

function Get-UserDevMode([IntPtr]$h) {
    # PRINTER_INFO_9 = { LPDEVMODE pDevMode }. GetPrinter level 9 returns the user's preferences.
    $needed = 0
    [void][Native.Winspool]::GetPrinter($h, 9, [IntPtr]::Zero, 0, [ref]$needed)
    if ($needed -eq 0) { throw "GetPrinter(9) failed: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())" }
    $buf = [Runtime.InteropServices.Marshal]::AllocHGlobal($needed)
    if (-not [Native.Winspool]::GetPrinter($h, 9, $buf, $needed, [ref]$needed)) {
        throw "GetPrinter(9) failed: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())"
    }
    $pDevMode = [Runtime.InteropServices.Marshal]::ReadIntPtr($buf)
    if ($pDevMode -eq [IntPtr]::Zero) { throw "the printer has no per-user DEVMODE" }
    $size = [Runtime.InteropServices.Marshal]::ReadInt16($pDevMode, 68) + [Runtime.InteropServices.Marshal]::ReadInt16($pDevMode, 70)  # dmSize + dmDriverExtra
    $copy = New-Object byte[] $size
    [Runtime.InteropServices.Marshal]::Copy($pDevMode, $copy, 0, $size)
    [Runtime.InteropServices.Marshal]::FreeHGlobal($buf)
    return $copy
}

function Set-UserDevMode([IntPtr]$h, [byte[]]$devMode) {
    $pDevMode = [Runtime.InteropServices.Marshal]::AllocHGlobal($devMode.Length)
    [Runtime.InteropServices.Marshal]::Copy($devMode, 0, $pDevMode, $devMode.Length)
    $info = [Runtime.InteropServices.Marshal]::AllocHGlobal([IntPtr]::Size)
    [Runtime.InteropServices.Marshal]::WriteIntPtr($info, $pDevMode)
    $ok = [Native.Winspool]::SetPrinter($h, 9, $info, 0)
    $err = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
    [Runtime.InteropServices.Marshal]::FreeHGlobal($info)
    [Runtime.InteropServices.Marshal]::FreeHGlobal($pDevMode)
    if (-not $ok) { throw "SetPrinter(9) failed: $err" }
}

function With-A3Landscape([byte[]]$devMode) {
    $dm = [byte[]]$devMode.Clone()
    $fields = [BitConverter]::ToInt32($dm, $OFF_FIELDS) -bor $DM_ORIENTATION -bor $DM_PAPERSIZE
    [Array]::Copy([BitConverter]::GetBytes([int]$fields), 0, $dm, $OFF_FIELDS, 4)
    [Array]::Copy([BitConverter]::GetBytes([int16]$DMORIENT_LANDSCAPE), 0, $dm, $OFF_ORIENTATION, 2)
    [Array]::Copy([BitConverter]::GetBytes([int16]$DMPAPER_A3), 0, $dm, $OFF_PAPERSIZE, 2)
    return $dm
}

$hPrinter = [IntPtr]::Zero
if (-not [Native.Winspool]::OpenPrinter($Printer, [ref]$hPrinter, [IntPtr]::Zero)) {
    throw "OpenPrinter failed: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())"
}
$original = Get-UserDevMode $hPrinter
$app = $null; $pres = $null; $createdApp = $false
try {
    Set-UserDevMode $hPrinter (With-A3Landscape $original)
    try { $app = [Runtime.InteropServices.Marshal]::GetActiveObject('PowerPoint.Application') }
    catch { $app = New-Object -ComObject PowerPoint.Application; $createdApp = $true }
    $abs = [IO.Path]::GetFullPath($Path)
    $outAbs = [IO.Path]::GetFullPath($Out)
    $pres = $app.Presentations.Open($abs, -1, 0, 0)   # ReadOnly, Untitled=false, WithWindow=false
    $opts = $pres.PrintOptions
    $opts.ActivePrinter = $Printer
    $opts.FitToPage = 0            # msoFalse: print at 100 %
    $opts.OutputType = 1           # ppPrintOutputSlides
    $opts.PrintHiddenSlides = 0
    $opts.FrameSlides = 0
    $from = if ($FromSlide -gt 0) { $FromSlide } else { 1 }
    $to = if ($ToSlide -gt 0) { $ToSlide } else { $pres.Slides.Count }
    $pres.PrintOut($from, $to, $outAbs, 1, 0)
    # PrintOut returns before the spooler finishes; wait for the file to be complete.
    $deadline = (Get-Date).AddMinutes(10)
    do { Start-Sleep -Milliseconds 500 } while (-not (Test-Path $outAbs) -and (Get-Date) -lt $deadline)
    $lastSize = -1
    while ((Get-Date) -lt $deadline) {
        $size = (Get-Item $outAbs).Length
        if ($size -gt 0 -and $size -eq $lastSize) { try { [IO.File]::Open($outAbs, 'Open', 'Read', 'None').Dispose(); break } catch { } }
        $lastSize = $size
        Start-Sleep -Milliseconds 500
    }
    "printed: $outAbs ($from-$to)"
} finally {
    if ($pres) { $pres.Close() }
    if ($createdApp -and $app) { $app.Quit() }
    Set-UserDevMode $hPrinter $original
    [void][Native.Winspool]::ClosePrinter($hPrinter)
}

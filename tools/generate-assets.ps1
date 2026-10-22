# Genera los assets visuales del mod (estilo steampunk, inspirado en la tecnología goblin del juego):
#  - Pipe: modelos de cada forma, textura, rule set de conexiones y JSON del bloque.
#  - Texturas de la Terminal y la Unit.
#  - Íconos de inventario de los tres bloques.
# Uso (desde la raíz del proyecto):  powershell -ExecutionPolicy Bypass -File tools\generate-assets.ps1
#
# Coordenadas de los modelos: un bloque mide 32 unidades (X/Z de -16 a 16, Y de 0 a 32).
# Direcciones de Hytale: North = -Z, South = +Z, East = +X, West = -X, Up = +Y, Down = -Y.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$res = Join-Path $root 'src\main\resources'
$modelDir = Join-Path $res 'Common\Blocks\NetworkStorage'
$blockTexDir = Join-Path $res 'Common\BlockTextures'
$iconDir = Join-Path $res 'Common\Icons\ItemsGenerated'
$ruleDir = Join-Path $res 'Server\Item\ConnectedBlockRuleSets'
$itemDir = Join-Path $res 'Server\Item\Items\NetworkStorage'
New-Item -ItemType Directory -Force $modelDir, $blockTexDir, $iconDir, $ruleDir, $itemDir | Out-Null

$utf8 = New-Object System.Text.UTF8Encoding($false)
function Write-Json($path, $obj) {
    # Compacto: el formato con sangría de PowerShell 5 infla mucho los archivos
    [IO.File]::WriteAllText($path, ($obj | ConvertTo-Json -Depth 30 -Compress), $utf8)
}

$dirs = [ordered]@{
    Up    = @(0, 1, 0)
    Down  = @(0, -1, 0)
    North = @(0, 0, -1)
    South = @(0, 0, 1)
    East  = @(1, 0, 0)
    West  = @(-1, 0, 0)
}
$allDirs = @($dirs.Keys)

# Formas del pipe: nombre -> caras conectadas en su orientación base.
# Las reglas prueban todas las rotaciones, así que cada combinación de vecinos cae en una de estas.
$shapes = [ordered]@{
    Single   = @()
    End      = @('Up')
    Straight = @('Up', 'Down')
    Corner   = @('Up', 'North')
    T        = @('Up', 'Down', 'North')
    Corner3  = @('Up', 'North', 'East')
    Cross    = @('Up', 'Down', 'North', 'South')
    Four     = @('Up', 'Down', 'North', 'East')
    Five     = @('Up', 'Down', 'North', 'South', 'East')
    Six      = @('Up', 'Down', 'North', 'South', 'East', 'West')
}

# Regiones de la textura (64x64)
$texArm = @{ x = 0; y = 0 }
$texCore = @{ x = 32; y = 0 }
$texFlange = @{ x = 0; y = 32 }

$script:nextId = 1
function New-Box($name, $pos, $size, $tex) {
    $face = @{ offset = @{ x = $tex.x; y = $tex.y }; mirror = @{ x = $false; y = $false }; angle = 0 }
    $script:nextId++
    return [ordered]@{
        id          = [string]$script:nextId
        name        = $name
        position    = @{ x = $pos[0]; y = $pos[1]; z = $pos[2] }
        orientation = @{ x = 0; y = 0; z = 0; w = 1 }
        shape       = [ordered]@{
            type          = 'box'
            offset        = @{ x = 0; y = 0; z = 0 }
            stretch       = @{ x = 1; y = 1; z = 1 }
            settings      = @{ isPiece = $false; size = @{ x = $size[0]; y = $size[1]; z = $size[2] } }
            textureLayout = [ordered]@{ front = $face; back = $face; left = $face; right = $face; top = $face; bottom = $face }
            unwrapMode    = 'custom'
            visible       = $true
            doubleSided   = $false
            shadingMode   = 'standard'
        }
        children    = @()
    }
}

function Get-AxisSize($d, $along, $across) {
    # Tamaño de una caja que se extiende "along" en el eje de la dirección y "across" en los otros dos
    return @(
        $(if ($d[0] -ne 0) { $along } else { $across }),
        $(if ($d[1] -ne 0) { $along } else { $across }),
        $(if ($d[2] -ne 0) { $along } else { $across })
    )
}

foreach ($shapeName in $shapes.Keys) {
    $script:nextId = 1
    $children = @()
    # Núcleo
    $children += New-Box 'Core' @(0, 0, 0) @(12, 12, 12) $texCore
    foreach ($dirName in $shapes[$shapeName]) {
        $d = $dirs[$dirName]
        # Brazo: del borde del núcleo (6) al borde del bloque (16)
        $children += New-Box "Arm_$dirName" @(($d[0] * 11), ($d[1] * 11), ($d[2] * 11)) (Get-AxisSize $d 10 8) $texArm
        # Brida en la punta
        $children += New-Box "Flange_$dirName" @(($d[0] * 15), ($d[1] * 15), ($d[2] * 15)) (Get-AxisSize $d 2 10) $texFlange
    }
    $model = [ordered]@{
        nodes  = @(
            [ordered]@{
                id          = '1'
                name        = 'Origin'
                position    = @{ x = 0; y = 16; z = 0 }
                orientation = @{ x = 0; y = 0; z = 0; w = 1 }
                shape       = [ordered]@{
                    type          = 'none'
                    offset        = @{ x = 0; y = 0; z = 0 }
                    stretch       = @{ x = 1; y = 1; z = 1 }
                    settings      = @{ isPiece = $false }
                    textureLayout = @{}
                    unwrapMode    = 'custom'
                    visible       = $true
                    doubleSided   = $false
                    shadingMode   = 'flat'
                }
                children    = $children
            }
        )
        format = 'prop'
        lod    = 'auto'
    }
    Write-Json (Join-Path $modelDir "Pipe_$shapeName.blockymodel") $model
}

# ---------------------------------------------------------------------------------------------
# Pixel art
# ---------------------------------------------------------------------------------------------
Add-Type -AssemblyName System.Drawing
$rand = New-Object System.Random 42

# Paleta (referencia: la tecnología goblin del juego: hierro oxidado, cobre y brillo cian)
$iron = @(112, 110, 100); $ironDark = @(52, 50, 46); $ironLight = @(176, 172, 160)
$rust = @(160, 88, 40); $rustDark = @(104, 56, 28)
$copper = @(196, 108, 52); $copperDark = @(116, 54, 24); $copperLight = @(246, 178, 112)
$wood = @(84, 58, 40); $woodGap = @(36, 24, 16)
$glow = @(70, 226, 240); $glowDark = @(18, 104, 122); $glowCore = @(200, 252, 255)
$cream = @(226, 214, 182); $needle = @(170, 40, 30)

function Clamp($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }
function Px($bmp, $x, $y, $c, $d = 0) {
    $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, (Clamp ($c[0] + $d)), (Clamp ($c[1] + $d)), (Clamp ($c[2] + $d))))
}
function Fill($bmp, $x0, $y0, $w, $h, $c, $amp = 0) {
    for ($x = $x0; $x -lt $x0 + $w; $x++) {
        for ($y = $y0; $y -lt $y0 + $h; $y++) { Px $bmp $x $y $c ($rand.Next(-$amp, $amp + 1)) }
    }
}
function Rust($bmp, $x0, $y0, $w, $h, $count) {
    # Manchas de óxido: blobs irregulares, más oscuros en el centro
    for ($i = 0; $i -lt $count; $i++) {
        $cx = $x0 + $rand.Next(0, $w); $cy = $y0 + $rand.Next(0, $h); $r = 0.8 + $rand.NextDouble() * 2.2
        for ($x = [int][Math]::Floor($cx - $r); $x -le [int][Math]::Ceiling($cx + $r); $x++) {
            for ($y = [int][Math]::Floor($cy - $r); $y -le [int][Math]::Ceiling($cy + $r); $y++) {
                if ($x -lt $x0 -or $y -lt $y0 -or $x -ge $x0 + $w -or $y -ge $y0 + $h) { continue }
                $dist = [Math]::Sqrt(($x - $cx) * ($x - $cx) + ($y - $cy) * ($y - $cy)) + $rand.NextDouble() * 0.9
                if ($dist -le $r * 0.5) { Px $bmp $x $y $rustDark ($rand.Next(-6, 7)) }
                elseif ($dist -le $r) { Px $bmp $x $y $rust ($rand.Next(-8, 9)) }
            }
        }
    }
}
function Iron($bmp, $x0, $y0, $w, $h, $rustCount) {
    # Chapa de hierro gris con ruido y óxido
    Fill $bmp $x0 $y0 $w $h $iron 7
    Rust $bmp $x0 $y0 $w $h $rustCount
}
function Bevel($bmp, $x0, $y0, $w, $h, $light, $dark) {
    for ($x = $x0; $x -lt $x0 + $w; $x++) { Px $bmp $x $y0 $light; Px $bmp $x ($y0 + $h - 1) $dark }
    for ($y = $y0; $y -lt $y0 + $h; $y++) { Px $bmp $x0 $y $light; Px $bmp ($x0 + $w - 1) $y $dark }
}
function Inset($bmp, $x0, $y0, $w, $h) {
    # Borde de un panel hundido: sombra arriba a la izquierda, filo claro abajo a la derecha
    for ($x = $x0; $x -lt $x0 + $w; $x++) { Px $bmp $x $y0 $ironDark; Px $bmp $x ($y0 + $h - 1) $ironLight -20 }
    for ($y = $y0; $y -lt $y0 + $h; $y++) { Px $bmp $x0 $y $ironDark; Px $bmp ($x0 + $w - 1) $y $ironLight -20 }
}
function Rivet($bmp, $x, $y) {
    # Remache de 2x2: brillo arriba a la izquierda, sombra abajo a la derecha
    Px $bmp $x $y $ironLight 30; Px $bmp ($x + 1) $y $ironLight
    Px $bmp $x ($y + 1) $ironLight -15; Px $bmp ($x + 1) ($y + 1) $ironDark
}
function Glow($bmp, $x0, $y0, $w, $h) {
    # Pantalla de energía cian: más brillante en el centro
    $cx = $x0 + ($w - 1) / 2; $cy = $y0 + ($h - 1) / 2
    for ($x = $x0; $x -lt $x0 + $w; $x++) {
        for ($y = $y0; $y -lt $y0 + $h; $y++) {
            $t = [Math]::Max([Math]::Abs($x - $cx) / ($w / 2), [Math]::Abs($y - $cy) / ($h / 2))
            $c = @(0, 0, 0)
            for ($k = 0; $k -lt 3; $k++) { $c[$k] = $glow[$k] + ($glowDark[$k] - $glow[$k]) * $t }
            Px $bmp $x $y $c ($rand.Next(-6, 7))
        }
    }
}
function Gauge($bmp, $cx, $cy, $r) {
    # Manómetro: aro de cobre, esfera crema, aguja roja
    for ($x = [Math]::Floor($cx - $r - 1); $x -le [Math]::Ceiling($cx + $r + 1); $x++) {
        for ($y = [Math]::Floor($cy - $r - 1); $y -le [Math]::Ceiling($cy + $r + 1); $y++) {
            $dist = [Math]::Sqrt(($x - $cx) * ($x - $cx) + ($y - $cy) * ($y - $cy))
            if ($dist -le $r - 1) { Px $bmp $x $y $cream ($rand.Next(-4, 5)) }
            elseif ($dist -le $r + 0.5) { Px $bmp $x $y $(if ($x + $y -lt $cx + $cy) { $copperLight } else { $copperDark }) }
        }
    }
    $steps = [Math]::Max(2, [int]($r - 1))
    for ($i = 0; $i -le $steps; $i++) {
        Px $bmp ([int][Math]::Round($cx + $i * 0.7)) ([int][Math]::Round($cy - $i * 0.7)) $needle
    }
    Px $bmp ([int][Math]::Round($cx)) ([int][Math]::Round($cy)) $ironDark
}
function Planks($bmp, $x0, $y0, $w, $h, $plank, $vertical) {
    # Tablas de madera con veta y separación oscura
    $count = [Math]::Ceiling(($(if ($vertical) { $w } else { $h })) / $plank)
    for ($p = 0; $p -lt $count; $p++) {
        $tone = $rand.Next(-10, 11)
        for ($a = 0; $a -lt $plank; $a++) {
            $len = $(if ($vertical) { $h } else { $w })
            for ($b = 0; $b -lt $len; $b++) {
                $x = $(if ($vertical) { $x0 + $p * $plank + $a } else { $x0 + $b })
                $y = $(if ($vertical) { $y0 + $b } else { $y0 + $p * $plank + $a })
                if ($x -ge $x0 + $w -or $y -ge $y0 + $h) { continue }
                if ($a -eq 0) { Px $bmp $x $y $woodGap; continue }
                $grain = $(if ($rand.Next(0, 9) -eq 0) { -14 } else { 0 })
                Px $bmp $x $y $wood ($tone + $grain + $rand.Next(-4, 5))
            }
        }
    }
}
function Frame($bmp, $size, $thick) {
    # Marco de hierro oxidado con remaches en las esquinas y al medio
    Iron $bmp 0 0 $size $thick 2; Iron $bmp 0 ($size - $thick) $size $thick 2
    Iron $bmp 0 0 $thick $size 2; Iron $bmp ($size - $thick) 0 $thick $size 2
    Bevel $bmp 0 0 $size $size $ironLight $ironDark
    Inset $bmp $thick $thick ($size - 2 * $thick) ($size - 2 * $thick)
    $m = [int]($size / 2) - 1; $e = $size - $thick + [int](($thick - 2) / 2); $s = [int](($thick - 2) / 2)
    foreach ($p in @(@($s, $s), @($e, $s), @($s, $e), @($e, $e), @($m, $s), @($m, $e), @($s, $m), @($e, $m))) { Rivet $bmp $p[0] $p[1] }
}
function Save($bmp, $path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose() }

# --- Pipe (atlas 64x64) ---
$bmp = New-Object System.Drawing.Bitmap 64, 64
# Brazo (caras de 8x8 y 8x10): cobre con brillo de tubo; oscuro en los bordes y claro cerca del centro
for ($x = 0; $x -lt 32; $x++) {
    for ($y = 0; $y -lt 32; $y++) {
        $edge = [Math]::Min([Math]::Min($x, 7) , 7 - [Math]::Min($x, 7))
        $edgeY = [Math]::Min([Math]::Min($y, 7), 7 - [Math]::Min($y, 7))
        $d = @(-46, -18, 18, 26)[[Math]::Min(3, [Math]::Min($edge, $edgeY))]
        Px $bmp $x $y $copper ($d + $rand.Next(-5, 6))
    }
}
for ($i = 0; $i -lt 5; $i++) { Px $bmp ($rand.Next(1, 7)) ($rand.Next(1, 9)) $copperDark ($rand.Next(-8, 9)) }
Px $bmp 2 2 $copperLight; Px $bmp 3 2 $copperLight
# Núcleo (12x12): caja de hierro oxidado remachada con una ventana de energía cian
Iron $bmp 32 0 32 32 3
Bevel $bmp 32 0 12 12 $ironLight $ironDark
foreach ($p in @(@(1, 1), @(9, 1), @(1, 9), @(9, 9))) { Rivet $bmp (32 + $p[0]) $p[1] }
Inset $bmp 35 3 6 6
Glow $bmp 36 4 4 4
Px $bmp 37 5 $glowCore
# Brida (10x10 y 10x2): anillo de cobre oscuro con remaches
Fill $bmp 0 32 32 32 $copper 6
Bevel $bmp 0 32 10 10 $copperLight $copperDark
for ($i = 0; $i -lt 10; $i++) { Px $bmp $i 33 $copperLight -30 }
Fill $bmp 3 35 4 4 $copperDark 4
foreach ($p in @(@(1, 1), @(7, 1), @(1, 7), @(7, 7))) { Rivet $bmp $p[0] (32 + $p[1]) }
Save $bmp (Join-Path $modelDir 'Pipe.png')

# --- Unit: cajón de madera oscura con marco de hierro oxidado ---
$bmp = New-Object System.Drawing.Bitmap 32, 32
Planks $bmp 0 0 32 32 6 $true
Frame $bmp 32 4
Iron $bmp 4 14 24 4 2
Bevel $bmp 4 14 24 4 $ironLight $ironDark
Rivet $bmp 7 15; Rivet $bmp 23 15
# Placa de cobre en el centro
Fill $bmp 13 13 6 6 $copper 6
Bevel $bmp 13 13 6 6 $copperLight $copperDark
Px $bmp 15 15 $copperDark; Px $bmp 16 15 $copperDark; Px $bmp 15 16 $copperDark; Px $bmp 16 16 $ironDark
Save $bmp (Join-Path $blockTexDir 'NetworkStorage_Unit_Side.png')

$bmp = New-Object System.Drawing.Bitmap 32, 32
Planks $bmp 0 0 32 32 6 $false
Frame $bmp 32 4
Iron $bmp 14 4 4 24 2
Bevel $bmp 14 4 4 24 $ironLight $ironDark
Rivet $bmp 15 7; Rivet $bmp 15 23
Save $bmp (Join-Path $blockTexDir 'NetworkStorage_Unit_Top.png')

# --- Terminal: chapa de hierro con pantalla cian, manómetro y perillas de cobre ---
$bmp = New-Object System.Drawing.Bitmap 32, 32
Iron $bmp 0 0 32 32 9
Bevel $bmp 0 0 32 32 $ironLight $ironDark
foreach ($p in @(@(1, 1), @(29, 1), @(1, 29), @(29, 29), @(15, 1))) { Rivet $bmp $p[0] $p[1] }
# Pantalla con un "rastro" de datos que la cruza
Inset $bmp 5 4 22 14
Glow $bmp 6 5 20 12
foreach ($p in @(@(9, 13), @(10, 12), @(11, 12), @(12, 11), @(13, 11), @(14, 10), @(15, 9), @(16, 9), @(17, 8), @(18, 8), @(19, 9), @(20, 8), @(21, 7), @(22, 7))) {
    Px $bmp $p[0] $p[1] $glowCore
}
# Tubo de cobre bajo la pantalla
Fill $bmp 4 20 24 3 $copper 5
for ($x = 4; $x -lt 28; $x++) { Px $bmp $x 20 $copperLight; Px $bmp $x 22 $copperDark }
Gauge $bmp 9.5 25.5 3.2
foreach ($kx in @(17, 22)) {
    Fill $bmp $kx 25 3 3 $copper 4
    Bevel $bmp $kx 25 3 3 $copperLight $copperDark
}
Save $bmp (Join-Path $blockTexDir 'NetworkStorage_Terminal_Side.png')

$bmp = New-Object System.Drawing.Bitmap 32, 32
Iron $bmp 0 0 32 32 8
Frame $bmp 32 3
# Rejilla de ventilación con brillo cian por dentro
for ($y = 8; $y -le 20; $y += 4) {
    Fill $bmp 8 $y 16 2 $glowDark 4
    for ($x = 9; $x -lt 23; $x++) { Px $bmp $x $y $glow -30 }
    for ($x = 8; $x -lt 24; $x++) { Px $bmp $x ($y + 2) $ironLight -20 }
}
Save $bmp (Join-Path $blockTexDir 'NetworkStorage_Terminal_Top.png')

# --- Íconos (64x64) ---
function IsoIcon($topPath, $sidePath, $outPath) {
    # Cubo en perspectiva isométrica a partir de las texturas del bloque
    $icon = New-Object System.Drawing.Bitmap 64, 64
    $g = [System.Drawing.Graphics]::FromImage($icon)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $top = [System.Drawing.Image]::FromFile($topPath)
    $side = [System.Drawing.Image]::FromFile($sidePath)
    $P = { param($x, $y) New-Object System.Drawing.PointF($x, $y) }
    $g.DrawImage($top, [System.Drawing.PointF[]]@((& $P 4 17), (& $P 32 3), (& $P 32 31)))
    $g.DrawImage($side, [System.Drawing.PointF[]]@((& $P 4 17), (& $P 32 31), (& $P 4 47)))
    $g.DrawImage($side, [System.Drawing.PointF[]]@((& $P 32 31), (& $P 60 17), (& $P 32 61)))
    # Sombreado de las caras laterales
    $left = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(70, 0, 0, 0))
    $right = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(35, 0, 0, 0))
    $g.FillPolygon($left, [System.Drawing.PointF[]]@((& $P 4 17), (& $P 32 31), (& $P 32 61), (& $P 4 47)))
    $g.FillPolygon($right, [System.Drawing.PointF[]]@((& $P 32 31), (& $P 60 17), (& $P 60 47), (& $P 32 61)))
    $g.Dispose(); $top.Dispose(); $side.Dispose()
    Save $icon $outPath
}
IsoIcon (Join-Path $blockTexDir 'NetworkStorage_Terminal_Top.png') (Join-Path $blockTexDir 'NetworkStorage_Terminal_Side.png') (Join-Path $iconDir 'NetworkStorage_Terminal.png')
IsoIcon (Join-Path $blockTexDir 'NetworkStorage_Unit_Top.png') (Join-Path $blockTexDir 'NetworkStorage_Unit_Side.png') (Join-Path $iconDir 'NetworkStorage_Unit.png')

# Ícono del pipe: tubo de cobre horizontal con bridas y el núcleo de hierro con ventana cian (dibujado a 32x32 y escalado x2)
$small = New-Object System.Drawing.Bitmap 32, 32
Fill $small 1 13 30 6 $copper 5
for ($x = 1; $x -lt 31; $x++) { Px $small $x 13 $copperDark; Px $small $x 14 $copperLight; Px $small $x 15 $copper 18; Px $small $x 18 $copperDark }
foreach ($fx in @(1, 28)) {
    Fill $small $fx 11 3 10 $copper 4
    Bevel $small $fx 11 3 10 $copperLight $copperDark
}
Iron $small 10 9 12 14 3
Bevel $small 10 9 12 14 $ironLight $ironDark
Rivet $small 11 10; Rivet $small 19 10; Rivet $small 11 20; Rivet $small 19 20
Inset $small 13 12 6 8
Glow $small 14 13 4 6
Px $small 15 14 $glowCore
$icon = New-Object System.Drawing.Bitmap 64, 64
$g = [System.Drawing.Graphics]::FromImage($icon)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($small, 0, 0, 64, 64)
$g.Dispose(); $small.Dispose()
Save $icon (Join-Path $iconDir 'NetworkStorage_Pipe.png')

# Rule set: cada forma del pipe y los otros bloques de la red exponen una "conexión" en sus 6 caras,
# así un pipe se conecta a cualquier bloque de la red que tenga al lado.
$tag = 'NetworkStorageLink'
$allFaces = [ordered]@{}
foreach ($d in $allDirs) { $allFaces[$d] = @($tag) }

function FaceTagRule($faces, $matchType) {
    $tags = [ordered]@{}
    foreach ($f in $faces) { $tags[$f] = @($tag) }
    return [ordered]@{ Type = 'FaceTag'; FaceTags = $tags; MatchType = $matchType }
}

$ruleShapes = [ordered]@{}
foreach ($shapeName in $shapes.Keys) {
    $ruleShapes[$shapeName] = [ordered]@{ Type = 'Templated'; FaceTags = $allFaces; TemplateId = $shapeName }
}
$ruleShapes['Terminal'] = [ordered]@{ Type = 'Block'; Block = 'NetworkStorage_Terminal'; FaceTags = $allFaces }
$ruleShapes['Unit'] = [ordered]@{ Type = 'Block'; Block = 'NetworkStorage_Unit'; FaceTags = $allFaces }

$patterns = @()
foreach ($shapeName in $shapes.Keys) {
    $connected = @($shapes[$shapeName])
    $others = @($allDirs | Where-Object { $connected -notcontains $_ })
    if ($connected.Count -eq 0) {
        $rule = [ordered]@{ Type = 'Not'; Rule = (FaceTagRule $others 'Any') }
    } elseif ($others.Count -eq 0) {
        $rule = FaceTagRule $connected 'All'
    } else {
        $rule = [ordered]@{
            Type  = 'And'
            Rules = @(
                (FaceTagRule $connected 'All'),
                [ordered]@{ Type = 'Not'; Rule = (FaceTagRule $others 'Any') }
            )
        }
    }
    $patterns += [ordered]@{
        Rule               = $rule
        RotationTransforms = @(@{ Yaw = 'All'; Pitch = 'All'; Roll = 'All' })
        Output             = @{ ShapeId = $shapeName }
    }
}
Write-Json (Join-Path $ruleDir 'NetworkStoragePipe.json') ([ordered]@{ Shapes = $ruleShapes; Patterns = $patterns; UpdateMode = 'UpdatePlaceUpdate' })

# Bloque: el estado por defecto es "Single"; el resto son estados con su modelo
$texture = @(@{ Texture = 'Blocks/NetworkStorage/Pipe.png'; Weight = 1 })
$definitions = [ordered]@{}
$templated = [ordered]@{ Single = @{ State = 'default' } }
foreach ($shapeName in $shapes.Keys) {
    if ($shapeName -eq 'Single') { continue }
    $definitions[$shapeName] = [ordered]@{
        DrawType           = 'Model'
        CustomModel        = "Blocks/NetworkStorage/Pipe_$shapeName.blockymodel"
        CustomModelTexture = $texture
    }
    $templated[$shapeName] = @{ State = $shapeName }
}

$pipe = [ordered]@{
    TranslationProperties = [ordered]@{
        Name        = 'server.items.NetworkStorage_Pipe.name'
        Description = 'server.items.NetworkStorage_Pipe.description'
    }
    PlayerAnimationsId    = 'Item'
    Categories            = @('Furniture.Containers')
    Icon                  = 'Icons/ItemsGenerated/NetworkStorage_Pipe.png'
    Recipe                = [ordered]@{
        TimeSeconds      = 1
        Input            = @(@{ ItemId = 'Ingredient_Bar_Iron'; Quantity = 1 })
        OutputQuantity   = 4
        BenchRequirement = @(@{ Id = 'Workbench'; Type = 'Crafting'; Categories = @('Workbench_Tinkering') })
    }
    Interactions          = [ordered]@{ Primary = 'Block_Primary'; Secondary = 'Block_Secondary' }
    BlockType             = [ordered]@{
        Material              = 'Solid'
        DrawType              = 'Model'
        Opacity               = 'Transparent'
        CustomModel           = 'Blocks/NetworkStorage/Pipe_Single.blockymodel'
        CustomModelTexture    = $texture
        HitboxType            = 'Full'
        Group                 = 'Metal'
        VariantRotation       = 'All'
        IgnoreSupportWhenPlaced = $true
        Gathering             = @{ Breaking = @{ GatherType = 'Rocks' } }
        BlockParticleSetId    = 'Metal'
        BlockSoundSetId       = 'Metal'
        PhysicalMaterialId    = 'Metal'
        ParticleColor         = '#c46c34'
        State                 = @{ Definitions = $definitions }
        ConnectedBlockRuleSet = [ordered]@{ Type = 'Patterned'; RuleSetId = 'NetworkStoragePipe'; TemplatedShapes = $templated }
    }
    Tags                  = @{ Type = @('Furniture'); Family = @('NetworkStorage') }
    IconProperties        = [ordered]@{ Scale = 0.58823; Rotation = @(22.5, 45, 22.5); Translation = @(0, -13.5) }
    ItemSoundSetId        = 'ISS_Items_Metal'
    MaxStack              = 100
}
Write-Json (Join-Path $itemDir 'NetworkStorage_Pipe.json') $pipe

Write-Host "Generados: $($shapes.Count) modelos del pipe, texturas, iconos, rule set y bloque del pipe."

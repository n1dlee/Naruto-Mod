$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")

function Get-FileContent {
    param([string] $Path)
    $fullPath = Join-Path $root $Path
    if (!(Test-Path -LiteralPath $fullPath)) {
        throw "Missing file: $Path"
    }
    return Get-Content -LiteralPath $fullPath -Raw
}

function Assert-FileContains {
    param(
        [string] $Path,
        [string] $Pattern
    )
    $content = Get-FileContent $Path
    if ($content -notmatch $Pattern) {
        throw "Missing pattern '$Pattern' in $Path"
    }
}

function Assert-FileDoesNotContain {
    param(
        [string] $Path,
        [string] $Pattern
    )
    $content = Get-FileContent $Path
    if ($content -match $Pattern) {
        throw "Forbidden pattern '$Pattern' in $Path"
    }
}

Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'NINJA_SPEED_UUID'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'Attributes\.MOVEMENT_SPEED'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'AttributeModifier\.Operation\.MULTIPLY_BASE'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'updateNinjaSpeed\(Player player\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'speedAttr\.removeModifier\(NINJA_SPEED_UUID\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'CHAKRA_DASH_ABILITY'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'rankSpeeds\s*=\s*new double\[\]\s*\{0\.3D,\s*0\.5D,\s*0\.8D,\s*1\.2D,\s*2\.0D\}'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'speedBonus\s*\+=\s*this\.gatesOpen\s*\*\s*0\.4D'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'speedBonus\s*\+=\s*0\.3D'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'speedBonus\s*\+=\s*0\.8D'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'this\.updateNinjaSpeed\(player\);'

Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/abilities/utility/ChakraDashAbility.java" 'MobEffects\.MOVEMENT_SPEED'
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/abilities/utility/ChakraDashAbility.java" 'MobEffectInstance'

$ninjaData = Get-FileContent "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java"
$movementSpeedMatches = [regex]::Matches($ninjaData, 'MobEffects\.MOVEMENT_SPEED')
if ($movementSpeedMatches.Count -gt 2) {
    throw "Expected only legacy clan/chidori vanilla speed references to remain in NinjaData, found $($movementSpeedMatches.Count)"
}

Write-Host "Phase 6 speed static checks passed."

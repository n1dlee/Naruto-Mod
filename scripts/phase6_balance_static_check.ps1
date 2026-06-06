$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")

function Assert-FileContains {
    param(
        [string] $Path,
        [string] $Pattern
    )
    $fullPath = Join-Path $root $Path
    if (!(Test-Path -LiteralPath $fullPath)) {
        throw "Missing file: $Path"
    }
    $content = Get-Content -LiteralPath $fullPath -Raw
    if ($content -notmatch $Pattern) {
        throw "Missing pattern '$Pattern' in $Path"
    }
}

Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'RANK_STAMINA_BONUS\s*=\s*new float\[\]\s*\{0,\s*50,\s*200,\s*500,\s*900\}'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'this\.stamina\s*=\s*this\.maxStamina;'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'this\.chakra\s*=\s*this\.maxChakra;'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" 'float getRankDamageMultiplier\(\);'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'getRankDamageMultiplier\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" '0\.5f\s*\+\s*this\.ninjaRank\s*\*\s*0\.3f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'player\.isSprinting\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'useStamina\(0\.1F,\s*5\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'this\.useStamina\(this\.gatesOpen\s*\*\s*0\.5F,\s*5\)'

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/LeapAbility.java" 'leapScales'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/DoubleJumpAbility.java" 'jumpBoost'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/ChakraDashAbility.java" 'STAMINA_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/ChakraDashAbility.java" 'useStamina\(STAMINA_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" 'WALL_WALK_STAMINA_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" 'useStamina\(WALL_WALK_STAMINA_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/BodyFlickerAbility.java" 'STAMINA_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/BodyFlickerAbility.java" 'RANK_RANGES'

$damageFiles = @(
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/AirPalmAbility.java",
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/EarthSpikesAbility.java",
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/FalseDarknessAbility.java",
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/WaterDragonAbility.java",
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/GreatBreakthroughAbility.java",
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/EightTrigramsRotationAbility.java",
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/MagnetReleaseAbility.java",
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java",
    "src/main/java/com/sekwah/narutomod/abilities/jutsus/AdamantineChainsAbility.java",
    "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java"
)

foreach ($file in $damageFiles) {
    Assert-FileContains $file 'getRankDamageMultiplier\(\)'
}

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/RasenganJutsuAbility.java" 'setDamageMultiplier\(ninjaData\.getRankDamageMultiplier\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/RasenganEntity.java" 'damageMultiplier'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/FireballJutsuAbility.java" 'setChargeAmount\(charge,\s*isUchiha,\s*ninjaData\.getRankDamageMultiplier\(\)\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/FireballJutsuEntity.java" 'rankDamageMultiplier'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/AmaterasuAbility.java" 'setDamageMultiplier\(ninjaData\.getRankDamageMultiplier\(\)\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/AmaterasuFireEntity.java" 'damageMultiplier'

Write-Host "Phase 6 balance static checks passed."

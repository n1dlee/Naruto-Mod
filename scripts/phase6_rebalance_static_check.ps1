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

Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" 'float getClanLightningDamageMultiplier\(\);'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" 'float getClanJutsuRangeMultiplier\(\);'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'getClanChakraMultiplier\(\)[\s\S]*1\.3f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'getClanChakraRegenMultiplier\(\)[\s\S]*1\.5f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'getClanLightningDamageMultiplier\(\)[\s\S]*1\.15f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'getClanJutsuRangeMultiplier\(\)[\s\S]*1\.10f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'player\.heal\(0\.03f\)'

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/RasenganJutsuAbility.java" 'CHAKRA_PER_TICK'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java" 'CHAKRA_COST\s*=\s*40\.0F'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/AirPalmAbility.java" 'CHAKRA_COST\s*=\s*25f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/EarthSpikesAbility.java" 'CHAKRA_COST\s*=\s*30f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/FalseDarknessAbility.java" 'CHAKRA_COST\s*=\s*45f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/WaterDragonAbility.java" 'CHAKRA_COST\s*=\s*55f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/GreatBreakthroughAbility.java" 'CHAKRA_COST\s*=\s*35f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/MultipleShadowCloneJutsuAbility.java" 'CHAKRA_COST\s*=\s*80'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ShadowCloneJutsuAbility.java" 'CHAKRA_COST\s*=\s*20'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/BodyFlickerAbility.java" 'CHAKRA_COST\s*=\s*15f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/EightGatesAbility.java" 'CHAKRA_PER_GATE\s*=\s*20f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/KuramaCloakAbility.java" 'BOND_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/FlyingThunderGodAbility.java" 'SEAL_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/FlyingThunderGodAbility.java" 'BRAND_RANGE'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/WoodReleaseAbility.java" 'CHAKRA_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/AmaterasuAbility.java" 'CHAKRA_COST\s*=\s*70\.0F'

Assert-FileContains "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/FireballJutsuEntity.java" 'uchihaBonus\s*\?\s*1\.25f\s*:\s*1\.0f'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/FalseDarknessAbility.java" 'getClanLightningDamageMultiplier\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java" 'getClanLightningDamageMultiplier\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" 'getClanLightningDamageMultiplier\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ShadowPossessionAbility.java" 'getClanJutsuRangeMultiplier\(\)'

Write-Host "Phase 6 rebalance static checks passed."

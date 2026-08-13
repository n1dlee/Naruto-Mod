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

function Assert-FileDoesNotContain {
    param(
        [string] $Path,
        [string] $Pattern
    )
    $fullPath = Join-Path $root $Path
    if (!(Test-Path -LiteralPath $fullPath)) {
        throw "Missing file: $Path"
    }
    $content = Get-Content -LiteralPath $fullPath -Raw
    if ($content -match $Pattern) {
        throw "Forbidden pattern '$Pattern' in $Path"
    }
}

Assert-FileContains "src/main/java/com/sekwah/narutomod/network/c2s/ServerSelectClanPacket.java" '"senju"'
Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NinjaCommand.java" '"senju"'

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/EightGatesAbility.java" 'ninjaData\.useChakra\(CHAKRA_PER_GATE,\s*10\);\s*return true;'
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/abilities/jutsus/EightGatesAbility.java" 'implements Ability\.Cooldown'

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/FlyingThunderGodAbility.java" 'clearSeals'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/FlyingThunderGodAbility.java" 'SEAL_COST'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/FlyingThunderGodAbility.java" 'BRAND_RANGE'

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/WoodReleaseAbility.java" 'CHAKRA_COST'
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/abilities/jutsus/WoodReleaseAbility.java" 'center\.offset\(dx,\s*dy,\s*dz\);\s*if \(p\.level\(\)\.getBlockState\(pos\)\.is\(Blocks\.OAK_LOG\)\)'

Assert-FileContains "src/main/java/com/sekwah/narutomod/item/weapons/ChakraBladeItem.java" 'if \(level\.isClientSide\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/item/weapons/KusanagiSwordItem.java" 'if \(level\.isClientSide\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/item/weapons/SoldierPillItem.java" 'if \(level\.isClientSide\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/item/weapons/SmokeBombItem.java" 'if \(level\.isClientSide\)'

Assert-FileContains "src/main/resources/assets/narutomod/models/item/chakra_blade.json" '"parent"'
Assert-FileContains "src/main/resources/assets/narutomod/models/item/kusanagi.json" '"parent"'

Write-Host "Bugfix regression static checks passed."

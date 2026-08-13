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

function Assert-FileExists {
    param([string] $Path)
    $fullPath = Join-Path $root $Path
    if (!(Test-Path -LiteralPath $fullPath)) {
        throw "Missing file: $Path"
    }
}

Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" "RANK_CHAKRA_POOL"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "triggerWallWalk"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "WALL_WALK_COST"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriAbility.java" "elementLevelRequired"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java" "return 2121;"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java" "elementLevelRequired"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/NarutoAbilities.java" "CHIDORI"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/NarutoAbilities.java" "CHIDORI_DASH"
Assert-FileContains "src/main/java/com/sekwah/narutomod/damagetypes/NarutoDamageTypes.java" "CHIDORI"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/renderer/entity/jutsuprojectile/RasenganRenderer.java" "rasengan_2.png"
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/client/renderer/entity/jutsuprojectile/RasenganRenderer.java" "FireballJutsuModel"
Assert-FileExists "src/main/resources/assets/narutomod/textures/entity/jutsu/projectiles/rasengan.png"
Assert-FileExists "src/main/resources/assets/narutomod/textures/particles/chidori_spark.png"
Assert-FileExists "src/main/resources/assets/narutomod/sounds/chidori.ogg"
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"narutomod:chidori"'

Write-Host "Progression, Rasengan, Wall Walk, and Chidori static checks passed."

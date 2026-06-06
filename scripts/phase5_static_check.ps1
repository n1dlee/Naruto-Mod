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

function Assert-FileExists {
    param([string] $Path)
    $fullPath = Join-Path $root $Path
    if (!(Test-Path -LiteralPath $fullPath)) {
        throw "Missing file: $Path"
    }
}

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/DojutsuAbility.java" "defaultCombo\(\)\s*\{[\s\S]*return 11;"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/SharinganAbility.java" "defaultCombo\(\)\s*\{[\s\S]*return -1;"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/AmaterasuAbility.java" "defaultCombo\(\)\s*\{[\s\S]*return 113;"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/NarutoAbilities.java" "SHARINGAN"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/NarutoAbilities.java" "AMATERASU"
Assert-FileContains "src/main/java/com/sekwah/narutomod/entity/NarutoEntities.java" "AMATERASU_FIRE"
Assert-FileContains "src/main/java/com/sekwah/narutomod/damagetypes/NarutoDamageTypes.java" "AMATERASU"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "SharinganOverlayGUI"
Assert-FileContains "src/main/resources/assets/narutomod/sounds.json" "sharingan_activate"
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"narutomod:sharingan"'
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"narutomod:amaterasu"'
Assert-FileExists "src/main/resources/assets/narutomod/textures/effects/sharingan/sharingan_1.png"
Assert-FileExists "src/main/resources/assets/narutomod/textures/effects/sharingan/sharingan_2.png"
Assert-FileExists "src/main/resources/assets/narutomod/textures/effects/sharingan/sharingan_3.png"
Assert-FileExists "src/main/resources/assets/narutomod/textures/effects/sharingan/mangekyou.png"
Assert-FileExists "src/main/resources/assets/narutomod/textures/effects/sharingan/amaterasu.png"
Assert-FileExists "src/main/resources/assets/narutomod/sounds/sharingan_activate.ogg"

Write-Host "Phase 5 static checks passed."

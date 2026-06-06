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

Assert-FileExists "src/main/java/com/sekwah/narutomod/abilities/jutsus/DojutsuAbility.java"
Assert-FileExists "src/main/java/com/sekwah/narutomod/abilities/jutsus/ByakuganAbility.java"
Assert-FileExists "src/main/java/com/sekwah/narutomod/client/gui/ByakuganOverlayGUI.java"
Assert-FileExists "src/main/java/com/sekwah/narutomod/client/gui/ByakuganEntityVisionGUI.java"
Assert-FileExists "src/main/resources/assets/narutomod/textures/effects/byakugan/byakugan.png"

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/DojutsuAbility.java" "return 11;"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/DojutsuAbility.java" '"uchiha"'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/DojutsuAbility.java" '"hyuga"'
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/SharinganAbility.java" "defaultCombo\(\)\s*\{[\s\S]*return -1;"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ByakuganAbility.java" "BYAKUGAN_RANGE\s*=\s*new int\[\]\s*\{20,\s*50,\s*150,\s*400,\s*1000\}"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ByakuganAbility.java" "CHAKRA_COST\s*=\s*0\.5F"
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" "getByakuganRange"
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" "isByakuganActive"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/NarutoAbilities.java" "DOJUTSU"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/NarutoAbilities.java" "BYAKUGAN"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "ByakuganOverlayGUI"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "ByakuganEntityVisionGUI"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/ByakuganEntityVisionGUI.java" "RenderSystem\.disableDepthTest"
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/client/gui/ByakuganEntityVisionGUI.java" "setGlowingTag|setGlowing"
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"narutomod:dojutsu"'
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"narutomod:byakugan"'
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"jutsu.fail.dojutsu"'
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"jutsu.fail.hyuga"'

Write-Host "Phase 6 Byakugan static checks passed."

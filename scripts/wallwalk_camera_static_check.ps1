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

Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "ViewportEvent\.ComputeCameraAngles"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "WALL_WALK_FEEDBACK_ROLL\s*=\s*6\.0F"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "getWallWalkCameraTargetRoll"
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "WALL_WALK_MAX_ROLL\s*=\s*88\.0F"
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "88\.0F"

Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "WALL_WALK_RUN_SPEED\s*=\s*0\.21D"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "WALL_WALK_SNEAK_SPEED\s*=\s*0\.08D"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "WALL_GRIP_PUSH\s*=\s*0\.035D"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "getWallPlaneForward"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "projectOntoWallPlane"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "getDeltaMovement"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "wallRight"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "wallRight\.scale\(-horizontalInput \* baseSpeed\)"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "verticalInput \* verticalSpeed"
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "new Vec3\(-normal\.z,\s*0\.0D,\s*normal\.x\)"

Write-Host "Naruto-style Wall Walk camera static checks passed."

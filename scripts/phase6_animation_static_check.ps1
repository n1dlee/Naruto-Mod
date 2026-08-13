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

Assert-FileContains "src/main/java/com/sekwah/narutomod/mixin/client/PlayerModelMixin.java" 'PlayerAnimHandler\.sprintingAnim\(player,\s*\(PlayerModel\) \(Object\) this,\s*limbSwing,\s*limbSwingAmount,\s*ageInTicks\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'applyChanneledJutsuPose'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'applySageIdlePose'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'applyKuramaCloakPose'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'applyEightGatesShake'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'isSageModeActive\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'isKuramaCloakActive\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'gatesOpen >= 5'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'Math\.sin\(ageInTicks\s*\*\s*3\.0F\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'NarutoAbilities\.CHAKRA_CHARGE\.getId\(\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'PoseBlender'
Assert-FileContains "src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java" 'Track\.GATES'

Write-Host "Phase 6 animation static checks passed."

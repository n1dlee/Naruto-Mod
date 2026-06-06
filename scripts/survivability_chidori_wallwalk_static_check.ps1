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

Assert-FileContains "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" "LivingHurtEvent"
Assert-FileContains "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" "Attributes\.MAX_HEALTH"
Assert-FileContains "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" "AttributeModifier\.Operation\.ADDITION"
Assert-FileContains "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" "HEALTH_BONUS_VALUES\s*=\s*new double\[\]\s*\{0\.0D,\s*8\.0D,\s*16\.0D,\s*28\.0D,\s*48\.0D\}"
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" "new\s+MobEffectInstance\s*\(\s*MobEffects\.HEALTH_BOOST"
Assert-FileContains "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" "MobEffects\.REGENERATION"
Assert-FileContains "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" "MobEffects\.DAMAGE_RESISTANCE"
Assert-FileContains "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java" "MOB_DAMAGE_MULTIPLIERS\s*=\s*new float\[\]\s*\{1\.0F,\s*0\.9F,\s*0\.8F,\s*0\.65F,\s*0\.5F\}"

Assert-FileExists "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriAbility.java"
Assert-FileExists "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java"
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriAbility.java" "implements\s+Ability\.Channeled"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriAbility.java" "BASE_COST\s*=\s*60\.0F"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriAbility.java" "ACTIVE_TICKS\s*=\s*8\s*\*\s*20"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java" "return 2121;"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java" "ActivationType\.INSTANT"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java" "DASH_DISTANCE\s*=\s*10\.0D"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriDashAbility.java" "getEntitiesOfClass"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/NarutoAbilities.java" "CHIDORI_DASH"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/keybinds/NarutoKeyHandler.java" "CHIDORI_KEY"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/keybinds/NarutoKeyHandler.java" "KeyEvent\.VK_G"

Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" "getChidoriTicks"
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" "getWallWalkDirection"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "setWallWalkDirection"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "setWallWalkAttached"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "applyWallPlaneMovement"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "getWallPlaneForward"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "projectOntoWallPlane"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "wallRight"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "getWallWalkDetachTicks"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "spawnWallWalkParticles"
Assert-FileContains "src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java" "playWallWalkStep"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "ViewportEvent\.ComputeCameraAngles"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "WALL_WALK_FEEDBACK_ROLL\s*=\s*6\.0F"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "getWallWalkCameraTargetRoll"
Assert-FileDoesNotContain "src/main/java/com/sekwah/narutomod/client/gui/NarutoInGameGUI.java" "WALL_WALK_MAX_ROLL\s*=\s*88\.0F"
Assert-FileContains "src/main/java/com/sekwah/narutomod/network/c2s/ServerWallWalkDetachPacket.java" "setWallWalkDetachTicks"
Assert-FileContains "src/main/java/com/sekwah/narutomod/network/PacketHandler.java" "ServerWallWalkDetachPacket"
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/keybinds/NarutoKeyHandler.java" "ServerWallWalkDetachPacket"
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"naruto.keys.chidori"'
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"narutomod:chidori_dash"'
Assert-FileContains "src/main/java/com/sekwah/narutomod/client/renderer/entity/jutsuprojectile/RasenganRenderer.java" "rasengan_2\.png"

Write-Host "Survivability, Chidori rework, and cinematic Wall Walk static checks passed."

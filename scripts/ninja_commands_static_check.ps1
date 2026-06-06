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

Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NinjaCommand.java" 'Commands\.literal\("ninja"\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NinjaCommand.java" 'requires\(source -> source\.hasPermission\(2\)\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NinjaCommand.java" 'Commands\.literal\("get"\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NinjaCommand.java" 'Commands\.literal\("set"\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NinjaCommand.java" 'Commands\.literal\("add"\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NinjaCommand.java" 'Commands\.literal\("reset"\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NinjaCommand.java" '"senju"'
Assert-FileContains "src/main/java/com/sekwah/narutomod/commands/NarutoCommands.java" 'NinjaCommand\.register\(dispatcher\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" 'void setNinjaRank\(int rank\);'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/INinjaData.java" 'void resetProgression\(\);'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'setNinjaRank\(int rank\)'
Assert-FileContains "src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java" 'resetProgression\(\)'
Assert-FileContains "src/main/resources/assets/narutomod/lang/en_us.json" '"commands\.narutomod\.ninja\.get"'

Write-Host "Ninja command static checks passed."

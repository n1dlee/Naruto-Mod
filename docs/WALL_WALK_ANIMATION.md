# Анимация бега по стенам

## Цель

Заменить текущую позу лазания на каноничный Naruto-style wall run:

- игрок остаётся визуально вертикальным;
- корпус слегка наклонён в сторону стены;
- руки отведены назад;
- ноги работают как при беге, в том числе при движении строго вверх или вниз;
- вход в позу и выход из неё плавные;
- физика, хитбокс и камера не меняют ориентацию.

Полный поворот модели на 90 градусов здесь **не нужен**. Он подходит для механики
«гравитация действует на стене», но для забега по стене создаёт горизонтальную
позу и снова считывается как Spider-Man.

## Что уже реализовано

Серверная часть не требует переделки:

```text
WaterWalkAbility
  └─ определяет стену и перемещает игрока в её плоскости
       └─ NinjaData.wallWalkDirection / wallWalkAttached
            └─ синхронизируются клиенту
```

* `WaterWalkAbility#applyWallPlaneMovement` отвечает за коллизии, скорость,
  отключение гравитации и расход чакры.
* `NinjaData#getWallWalkDirection` уже предоставляет клиенту сторону стены.
* `PlayerAnimHandler#applyWallClimbPose` — единственная часть, которая сейчас
  намеренно рисует позу карабканья: поднятые разведённые руки и короткий шаг.

Следовательно, фикс должен быть преимущественно клиентским и не затрагивать
серверную авторитетность движения.

## Архитектура после изменения

```text
WaterWalkAbility                     gameplay / server
        │
        ▼
NinjaData                             synchronized state
  - wallWalkAttached
  - wallWalkDirection
        │
        ├──────────────► PlayerAnimHandler
        │                 stance + leg run cycle
        │
        └──────────────► RenderEvents (optional)
                          tiny third-person wall offset only
```

## Изменение `PlayerAnimHandler`

### 1. Приоритет поз

Стеновой бег должен иметь приоритет над обычным Naruto sprint. Иначе его поза
может быть частично или полностью перезаписана обработчиком спринта.

```java
boolean wallRunning = ninjaData.isWallWalkAttached()
        && ninjaData.getWallWalkDirection() != null;

if (wallRunning) {
    float wallWeight = PoseBlender.weight(
            entity, Track.WALL_CLIMB, true, RAMP_WALL_CLIMB, ageInTicks
    );
    if (wallWeight > PoseBlender.EPSILON) {
        applyWallRunPose(playerModel, player, wallWeight, ageInTicks);
    }
} else if (entity.isSprinting()) {
    float sprintWeight = PoseBlender.weight(
            entity, Track.SPRINT, true, RAMP_SPRINT, ageInTicks
    );
    if (sprintWeight > PoseBlender.EPSILON) {
        applyNarutoSprintPose(playerModel, sprintWeight);
    }
}
```

Если в текущем методе вес `sprintWeight` уже вычисляется выше, его стоит
переиспользовать, а не вычислять второй раз.

### 2. Новая поза бега

Заменить `applyWallClimbPose(...)` на `applyWallRunPose(...)`.

```java
private static void applyWallRunPose(
        PlayerModel playerModel,
        Player player,
        float weight,
        float ageInTicks
) {
    // deltaMovement включает вертикальную скорость, поэтому цикл не замирает,
    // когда игрок движется только вверх по стене.
    float speed = (float) Mth.clamp(
            player.getDeltaMovement().length() * 7.0D, 0.0D, 1.0D
    );
    float stride = Mth.cos(ageInTicks * 1.45F) * 0.95F * speed;

    // Лёгкий наклон к стене. Тело не укладывается на стену.
    PoseBlender.rotate(playerModel.body, weight, 0.24F, 0.0F, 0.0F);
    PoseBlender.addRotation(playerModel.head, weight, -0.10F, 0.0F, 0.0F);

    // Полноценный беговой цикл ног.
    PoseBlender.rotate(playerModel.rightLeg, weight,  stride, 0.0F, 0.0F);
    PoseBlender.rotate(playerModel.leftLeg,  weight, -stride, 0.0F, 0.0F);

    // Характерный Naruto run: руки назад, а не перед собой на стене.
    PoseBlender.rotate(playerModel.rightArm, weight, 1.35F, -0.12F, 0.0F);
    PoseBlender.rotate(playerModel.leftArm,  weight, 1.35F,  0.12F, 0.0F);
    PoseBlender.position(playerModel.rightArm, weight, -5.0F, 3.7F, -4.0F);
    PoseBlender.position(playerModel.leftArm,  weight,  5.0F, 3.4F, -4.0F);
}
```

Импорты для примера:

```java
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
```

### Почему используется `deltaMovement`, а не только `limbSwing`

Vanilla `limbSwing` надёжно отражает обычное движение по земле. При движении
строго вверх его амплитуда может оказаться маленькой или нулевой, хотя игрок
реально карабкается. `player.getDeltaMovement().length()` учитывает вертикальную
скорость и сохраняет беговой ритм. В покое `speed` стремится к нулю, поэтому
ноги естественно останавливаются.

## Необязательная косметика в `RenderEvents`

Если модель заметно «висит» перед стеной, в `RenderPlayerEvent.Pre` можно
сдвинуть только отрисовку на 0.03–0.05 блока к стене. Не менять фактическую
позицию сущности и не менять hitbox.

```java
Direction wall = ninjaData.getWallWalkDirection();
if (ninjaData.isWallWalkAttached() && wall != null) {
    double visualOffset = 0.04D;
    event.getPoseStack().translate(
            wall.getStepX() * visualOffset,
            0.0D,
            wall.getStepZ() * visualOffset
    );
}
```

Этот блок добавлять только после ранних `return` / `setCanceled(true)` для
полных форм Susanoo и Kurama: их модель не должна получать обычное смещение
игрока.

## Чего не делать

- Не вращать `Player` или его hitbox на сервере.
- Не менять `setNoGravity`, коллизии и вычисление `wallForward` ради анимации.
- Не отправлять отдельный пакет для позы: `wallWalkAttached` и направление уже
  синхронизированы capability.
- Не поворачивать first-person камеру на 90°. Текущий небольшой camera roll —
  достаточная обратная связь контакта со стеной.
- Не использовать одну статическую переменную фазы анимации для всех игроков:
  она будет смешивать ритм разных сущностей. Пример выше вообще обходится без
  состояния, используя `ageInTicks`.

## Проверка в игре

1. Подбежать к стене и начать подъём: руки должны уйти назад, ноги — чередоваться.
2. Двигаться только `W` вверх: цикл ног не должен замирать.
3. Двигаться `A` / `D` вдоль стены: анимация остаётся беговой.
4. Отпустить клавиши у стены: ноги останавливаются, руки не поднимаются к блоку.
5. Спуститься на землю: стеновая поза плавно уступает обычной.
6. Проверить north/south/east/west стены, third-person и first-person.
7. Проверить Susanoo, Kurama, Baika и броню: они не должны получать лишнее
   смещение или наклон.

## Рекомендуемый порядок внедрения

1. Заменить позу в `PlayerAnimHandler` и поправить приоритет со sprint.
2. Собрать проект (`gradlew.bat compileJava`).
3. Протестировать семь сценариев выше в клиенте.
4. Только если визуально остаётся зазор до стены — добавить микросмещение в
   `RenderEvents` и повторить проверку.

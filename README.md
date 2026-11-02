# AntiTrap

Клиентский Fabric-мод для **Minecraft 1.21.11**. Подсвечивает ловушечные блоки в сфере вокруг игрока (по умолчанию **50** блоков).

Одна клавиша **R** полностью включает и выключает мод: сканер, контуры и GPU-меш гасятся вместе. Настройки — `/antitrap menu`.

## Что определяется

| Категория | Блоки |
|---|---|
| `tripwire` | нить, натяжной крючок |
| `signs` | таблички, настенные и подвесные |
| `doors` | двери и калитки |
| `trapdoors` | люки |
| `dispensers` | раздатчик, капельник, crafter |
| `pistons` | поршень, липкий поршень, голова поршня |
| `redstone` | рычаг, редстоун-факел, редстоун-блок/лампа, copper bulb, громоотвод, наблюдатель, TNT, воронка, кнопки, пластины, рельс с питанием, скалк, провод, повторитель, компаратор, мишень, датчик света, кафедра |
| `containers` | сундук-ловушка, сундук, бочка, шалкер, спавнер, trial spawner |
| `hazards` | паутина, порошковый снег, магма, soul sand, огонь, костры, сталактит, леса, слизь/мёд, note block, большая drip leaf, sculk catalyst, куст сладких ягод, detector/activator rail, инфистед-блоки, wither rose |

Лава, вода, ковры и кактусы намеренно не подсвечиваются — слишком много ложных срабатываний.

Капельники, стоящие кучно (Chebyshev ≤ 6), поднимаются до жёлтого/красного.

## Уровни опасности

- **Низкий** (зелёный) — ловушечный блок в радиусе, не на линии шага.
- **Средний** (жёлтый) — на линии шага, поршень/раздатчик смотрит в коридор, кластер капельников 2–3.
- **Высокий** (красный) — нить/редстоун/hazard на линии шага, поршень/раздатчик в лицо, кластер капельников 4+.

## Управление

| Действие | По умолчанию |
|---|---|
| Полное вкл/выкл | `R` (переназначается в Controls) |
| Настройки | `/antitrap menu` |
| Команда-тоггл | `/antitrap` |

```
/antitrap
/antitrap on
/antitrap off
/antitrap menu
/antitrap reload
/antitrap radius 50
/antitrap category tripwire off
```

Категории: `tripwire`, `signs`, `doors`, `trapdoors`, `dispensers`, `pistons`, `redstone`, `containers`, `hazards`.

## Конфиг

`minecraft/config/antitrap.json`

Пример: [`config/antitrap.example.json`](config/antitrap.example.json).

- `enabled` — полное вкл/выкл
- `scanRadius` — сфера 8–64, по умолчанию **50**
- `outlineIntensity` — прозрачность контура (0.2–1.0)
- `colorLow` / `colorMedium` / `colorHigh` — HEX
- `categories` — вкл/выкл типов

HUD нет: только контуры в мире.

## Сборка

Нужны **JDK 21** и интернет.

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew.bat build
```

Готовый мод: `build/libs/antitraps-1.0.2.jar` (не `*-sources.jar`).

## Куда положить .jar

1. Fabric Loader 0.18.1+ для Minecraft **1.21.11**.
2. Fabric API `0.141.x+1.21.11` в `mods`.
3. `antitraps-1.0.2.jar` в `mods` того же инстанса.

| Система | Путь |
|---|---|
| Windows | `%appdata%\.minecraft\mods` |
| Linux | `~/.minecraft/mods` |
| macOS | `~/Library/Application Support/minecraft/mods` |

## Архитектура

```
dev.antitrap
├── AntiTrapMod              клиентский вход, setEnabled()
├── config                   JSON + экран настроек
├── detect                   категории и реестр блоков
├── scan                     сфера 50 + BE + palette skip
├── render                   GPU-меш, camera через modelOffset
├── input                    одна клавиша R
└── command                  /antitrap, /antitrap menu
```

Сканер: сначала block entities (таблички, капельники, сундуки), затем секции чанка с `maybeHas` — воздух/камень не обходятся. Полное обновление раз в 8 тиков или после сдвига на 2 блока. Контур пересобирается только когда меняется набор блоков, не каждый кадр.

## Документация HTML

- `html/AntiTrap/index.html`
- копия: `Desktop/html/AntiTrap/index.html`

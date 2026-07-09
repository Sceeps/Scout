# Scout

Paper 1.21. Сканер приватов WorldGuard: предмет SCULK_SHRIEKER с лимитом использований.

## Как работает

1. Крафт (или выдача предмета с PDC)
2. Игрок ставит SCULK_SHRIEKER
3. 3 секунды скана (action bar)
4. Поиск регионов WG в радиусе по префиксам id (по умолчанию `ps_x`, как у Bastion)
5. Сообщение: нашёл / не нашёл
6. После `max-uses` блок пропадает; при ломании раньше предмет возвращается со счётчиком

Внутри привата ставить нельзя.

## Крафт

```
O R O
R D R
O R O
```

O = Observer, R = Redstone, D = Diamond Block.

## Команды

| Команда | Право | Что делает |
|---------|-------|------------|
| `/scout reload` | `scout.admin` | конфиг + рецепт |

Права: `scout.use`, `scout.admin`.

## Конфиг

```yaml
scan-radius: 100    # 1..500
max-uses: 25
glow: true
region-prefixes:
  - ps_x
```

## Зависимости

WorldEdit, WorldGuard.

## Сборка

JDK 21+.

```bash
./gradlew build
```

Jar: `build/libs/Scout.jar`

WG/WE jar'ы можно кинуть в `libs/`, если Maven недоступен.

## Установка

1. WorldGuard + WorldEdit
2. `plugins/Scout.jar`
3. Рестарт, при необходимости `config.yml`

## Структура

```
src/main/java/dev/portfolio/scout/
  Scout.java
  Blocks.java
src/main/resources/
  plugin.yml
  config.yml
```

## Лицензия

MIT, [LICENSE](LICENSE).

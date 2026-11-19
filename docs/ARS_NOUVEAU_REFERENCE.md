# Ars Nouveau 5.13.x (NeoForge 1.21.1) — справочник для аддона

Составлено по исходникам `baileyholl/Ars-Nouveau`, ветка `main` (версия 5.13.5, 1479 Java-файлов).
Наш мод собирается против 5.13.2.1418 — API между этими версиями совпадает.
Пакет: `com.hollingsworth.arsnouveau`. Ниже `api.` и `common.` — его подпакеты.

---

## 1. Карта пакетов

| Пакет | Что внутри |
|---|---|
| `api.spell` | Ядро заклинаний: `Spell`, `SpellContext`, `SpellResolver`, `SpellStats`, абстрактные глифы, тиры, школы, валидаторы |
| `api.spell.wrapped_caster` | Кто кастует: `PlayerCaster`, `LivingCaster`, `TileCaster`, `RuneCaster`, `EmptyCaster` |
| `api.registry` | Статические реестры: глифы, ритуалы, фамильяры, перки, кастеры, поведения, документация, таймлайны частиц |
| `api.event` | События NeoForge-шины от Ars (каст, резолв, урон, мана, стоимость) + очередь таймеров `EventQueue` |
| `api.mana` | `IManaCap`, `IManaDiscountEquipment` |
| `api.source` | Source (мана блоков): `ISourceTile`, `SourceManager`, `ISpecialSourceProvider` |
| `api.perk` | Нити брони: `IPerk`, `PerkSlot`, `PerkAttributes` |
| `api.ritual` | `AbstractRitual`, готовые основы (`RangeRitual`, `StructureRitual`, `ConjureBiomeRitual`, …) |
| `api.familiar` | `AbstractFamiliarHolder`, `IFamiliar` |
| `api.documentation` | Новая книга (Worn Notebook): категории, записи, страницы, `ReloadDocumentationEvent` |
| `api.particle` | Эмиттеры, «движения» и таймлайны частиц, настраиваемые в Spell Style |
| `api.camera` | `ICameraMountable` — камера скрайера (вид через удалённую сущность `ScryerCamera`) |
| `api.util` | `SpellUtil`, `ManaUtil`, `SourceUtil`, `BlockUtil`, `CuriosUtil`, `DamageUtil`, `LootUtil`, `InvUtil` |
| `common.spell.method/effect/augment` | Все 5 форм, 67 эффектов, 13 аугментов |
| `common.entity` | Мобы (Starbuncle, Drygmy, Whirlisprig, Wixie, Bookwyrm, Amethyst Golem, Wilden, Alakarkinos, Lily, Nook …) и сущности заклинаний |
| `common.entity.statemachine` | `SimpleStateMachine` + состояния Старбанкла и Алакаркиноса |
| `common.entity.pathfinding` | Порт навигации MineColonies (`MinecoloniesAdvancedPathNavigate`) |
| `common.block.tile` | 66 блок-сущностей: Source Jar, Relay, Sourcelink, Apparatus, Imbuement, Scribes, Turret, Brazier, Repository … |
| `common.crafting.recipes` | 18 типов рецептов |
| `common.perk` | 22 перка |
| `common.ritual` | 24 ритуала |
| `common.network` | 65 пакетов, `Networking.sendToPlayerClient / sendToNearbyClient` |
| `setup.registry` | `DeferredRegister`-ы: блоки, предметы, сущности, звуки, компоненты, меню, рецепты, Capability |
| `setup.config` | `ServerConfig`, `Config` (клиент/общий), `StartupConfig` |

---

## 2. Заклинание: от клика до эффекта

### 2.1 Данные
- **`Spell`** — неизменяемый record-подобный класс:
  - `recipe` (`List<AbstractSpellPart>`), `name`, `color` (`ParticleColor`), `sound`, `particleTimeline` (`TimelineMap`);
  - мутация — через `spell.mutable()…immutable()` или `withX(...)`; сериализация `toJson/fromJson`, `toBinaryBase64`.
- **Структура рецепта:** `[форма] [эффект] [аугменты…] [эффект] [аугменты…] …`.
  - `getCastMethod()` — первый элемент, если это форма.
  - `getAugments(i, caster)` — все аугменты подряд **после** индекса `i` до следующего не-аугмента.
- **`getCost()`:** сумма `getCastingCost()` частей. Аугмент берёт цену из `augment.getCostForPart(part)`, то есть из конфига
  `augment_cost_overrides` того глифа, к которому прицеплен.

### 2.2 Кастер
- `AbstractCaster<T>` — data-компонент на предмете (книга, жезл, свиток…), слоты заклинаний `SpellSlotMap`.
  - `SpellCasterRegistry.from(stack)` достаёт кастер из любого предмета; `register(item, provider)` — для своих.
  - `castSpell(level, entity, hand, invalidMessage)` → строит `SpellContext` → `getSpellResolver(...)` → `resolver.onCast(...)`.
  - `modifySpellBeforeCasting(...)` — хук, чтобы подменить заклинание (так работают наши посохи и перчатки).
- `IWrappedCaster` — абстракция «кто платит ману и где инвентарь»: `enoughMana`, `expendMana`, `getInvManager`, `getPosition`.
  - `LivingCaster.from(mob)` у мобов (наш Котто), `PlayerCaster` у игрока, `TileCaster` у турелей (мана не тратится, если тип `TURRET`).

### 2.3 `SpellContext`
- Курсор по рецепту (`currentIndex`, `nextPart()`, `hasNextPart()`), отмена (`setCanceled`, `CancelReason`), задержка (`delay(DelayedSpellEvent)`).
- Цвет, инструмент кастера, именованные вложения: `getOrCreateAttachment(id, attachment)`.
- `makeChildContext()` — продолжение «остатка» заклинания в новом контексте (Linger, Wall, Burst, Orbit):
  - сначала спрашивает каждый `IContextManipulator` в остатке (так работает `EffectReset`);
  - иначе клонирует контекст со `spell = getRemainingSpell()`.
- `CasterType`: `RUNE`, `TURRET`, `ENTITY`, `LIVING_ENTITY`, `PLAYER`, `OTHER`.

### 2.4 `SpellResolver` — порядок вызовов
1. `onCast / onCastOnBlock / onCastOnEntity` →
   - `canCast()`: валидатор `getSpellCastingSpellValidator()` + `enoughMana` (креатив пропускает);
   - `SpellCastEvent` (можно отменить);
   - `castType.onCastX(...)` со `SpellStats` для формы;
   - при `CastResolveType.SUCCESS` → `expendMana()`.
2. Форма (снаряд, касание…) в момент попадания вызывает `resolver.onResolveEffect(level, hitResult)` → `resolveAllEffects` (сброс счётчика) → `resume(level)`.
3. `resume`:
   - `SpellResolveEvent.Pre` (можно отменить).
   - Затем цикл по частям; аугменты и выключенные глифы пропускаются. Для каждого эффекта:
     - `SpellStats.Builder().setAugments(…).addItemsFromEntity(shooter).build(part, hit, level, shooter, ctx)` — применяет `AbstractAugment.applyModifiers`, `ISpellModifierItem.applyItemModifiers` для каждого надетого или держимого предмета (руки, броня, Curios), затем `SpellModifierEvent`;
     - `EffectResolveEvent.Pre` (отмена пропускает **только этот** эффект);
     - capability `BLOCK_SPELL_RESOLVE_CAP` / `ENTITY_SPELL_RESOLVE_CAP` (`IResolveListener`): `CONTINUE`, `SKIP` или `STOP_ALL`;
     - `effect.onResolve(...)` → `onResolveEntity` / `onResolveBlock`;
     - `onPostResolve` у слушателей, `EffectResolveEvent.Post`.
   - После цикла — `SpellResolveEvent.Post`.
4. **Цена:** `spell.getCost() − ManaUtil.getPlayerDiscounts(...)`, затем `SpellCostCalcEvent.Pre` (проверка) или `.Post` (списание).
5. `withSilent(true)` глушит сообщения «не хватает маны» и ошибки валидации — это для мобов.
6. `getNewResolver(ctx)` — аддоны могут подсунуть свой резолвер; `hasFocus(stack)` проверяет фокус в Curios.

### 2.5 `SpellStats`
| Поле | Аугмент | Шаг |
|---|---|---|
| `amp` | Amplify / Dampen | +1 / −1 |
| `aoe` | AOE | +1 |
| `duration` | Extend Time / Duration Down | +1 / −1 |
| `acceleration` | Accelerate / Decelerate | +1 / −0.5 |
| `sensitive` | Sensitive | флаг |
| `randomize` | Randomize | флаг |
| `damageModifier` | — | меняют предметы и события |

- Pierce, Split, Fortune, Extract не меняют поля — эффекты сами считают `getBuffCount(Augment.INSTANCE)`.
- `getDurationInTicks()`, `getAmpMultiplier()`, `getAoeMultiplier()`, `isSensitive()`.

### 2.6 Валидация
- `StandardSpellValidator` объединяет: одна форма и только в начале, непустое заклинание, совместимость аугментов
  (`getCompatibleAugments`), лимиты аугментов (`augment_limits` в конфиге глифа), запрещённые комбо, известность глифа,
  максимальный тир книги.
- Ошибка — `SpellValidationError.makeTextComponentExisting()`.

---

## 3. Глифы

### 3.1 Базовые классы
- `AbstractSpellPart(ResourceLocation id, String name)`:
  - `getDefaultManaCost()`, `getCompatibleAugments()`, `defaultTier()`, `getSchools()`, `getBookDescription()`, `addAugmentDescriptions(map)`;
  - `buildConfig(builder)` — у каждого глифа своя секция конфига: enabled, cost, tier, augment limits / cost overrides / invalid combos, starter;
  - `defaultedStarterGlyph()` — выдаётся при старте.
- **`AbstractEffect`:**
  - `onResolveEntity`, `onResolveBlock`, `summonLivingEntity`, `canBlockBeHarvested`, `getBaseHarvestLevel`, `applyEnchantments`;
  - конфиг-хелперы: `addDamageConfig`, `addAmpConfig`, `addPotionConfig`, `addExtendTimeConfig`, `addGenericInt/Double`;
  - готовые поля `DAMAGE`, `AMP_VALUE`, `POTION_TIME`, `EXTEND_TIME`, `GENERIC_INT`, `RANDOMIZE_CHANCE`, `DURATION_DOWN_TIME`.
- `AbstractCastMethod`: `onCast`, `onCastOnBlock(UseOnContext)`, `onCastOnBlock(BlockHitResult, …)`, `onCastOnEntity` → `CastResolveType`.
- `AbstractAugment`: `applyModifiers(builder, part, hit, level, shooter, ctx)`, цена для конкретного глифа — `getCostForPart`.
- **Интерфейсы-примеси:**
  - `IDamageEffect.attemptDamage(...)`: базовый урон + `damageModifier` + атрибут `SPELL_DAMAGE_BONUS` + случайность, затем `SpellDamageEvent.Pre/Post`; союзников (`isAlliedTo`) не бьёт;
  - `IPotionEffect` — длительность и сила зелья по аугментам;
  - `IContextManipulator`, `ILightable`, `ITurretBehavior`, `ITimelined`.

### 3.2 Регистрация
- `GlyphRegistry.registerSpell(part)` создаёт и предмет `Glyph` (`part.glyphItem`).
- Рецепт для Scribes Table — тип `ars_nouveau:glyph`:
  ```json
  {"type":"ars_nouveau:glyph","exp":55,
   "inputs":[{"item":"minecraft:sugar"}, …],
   "output":{"id":"<mod>:glyph_<id>","count":1}}
  ```
  `exp` — стоимость в уровнях опыта.
- Тиры: `SpellTier.ONE/TWO/THREE/CREATIVE(99)`, свои — `SpellTier.createTier(id, value)`.
- Школы: `ABJURATION`, `CONJURATION`, `NECROMANCY`, `MANIPULATION`, `ELEMENTAL` (подшколы AIR/EARTH/FIRE/WATER).

### 3.3 Формы
| Форма | Мана | Суть |
|---|---|---|
| Projectile | 10 | `EntityProjectileSpell`. Скорость `max(0.1, 0.75 + acc/2)`; Split даёт +1 снаряд веером по 10°; Pierce уменьшает `pierceLeft` при каждом попадании; Sensitive — клип `OUTLINE` (задевает траву); время жизни из конфига, 60 с |
| Touch | 5 | По блоку или сущности под прицелом |
| Self | 10 | На кастера |
| Underfoot | 5 | Блок под ногами |
| Pantomime | 5 | Точка в воздухе перед игроком |

### 3.4 Аугменты (мана по умолчанию)
| Аугмент | Мана | Аугмент | Мана |
|---|---|---|---|
| AOE | 35 | Extract (шёлк) | 30 |
| Amplify | 20 | Fortune | 80 |
| Dampen | 0 | Pierce | 40 |
| Accelerate | 10 | Randomize | 0 |
| Decelerate | 5 | Sensitive | 10 |
| Extend Time | 10 | Split | 20 |
| Duration Down | 15 | | |

### 3.5 Эффекты: мана и тир
- **Тир 1:**
  - Break 10, Harm 15, Ignite 15, Freeze 15, Knockback 15, Pull 15, Launch 30, Leap 25, Light 25, Bounce 50, Bubble 20;
  - Craft 50, Cut 0, Delay 0, Dispel 30, Evaporate 50, Fell 150, Harvest 10, Interact 10, PhantomBlock 5, Pickup 10, PlaceBlock 10;
  - Prestidigitation 0, Redstone 0, Reset 0, Rotate 10, Rune 30, Snare 100, SummonSteed 100, SummonWolves 100, Toss 10, Windburst 30, Wololo 30.
- **Тир 2:**
  - Animate 200, ColdSnap 30, ConjureWater 80, Crush 30, EnderChest 50, Exchange 50, Explosion 200, Firework 50, Flare 40, Gravity 15;
  - Grow 70, Heal 50, Infuse 30, Invisibility 30, Name 25, SenseMagic 50, Slowfall 30, Smelt 100, Windshear 50.
- **Тир 3:**
  - Blink 50, Burst 500, Fangs 35, Glide 100, Hex 100, Intangible 30, Lightning 100, Linger 500, Orbit 50, Rewind 100;
  - SummonDecoy 200, SummonUndead 150, SummonVex 150, Wall 500, Wither 100.

**Образцы реализации:**
- **Break:**
  - AOE и Pierce → `SpellUtil.calcAOEBlocks(shooter, pos, hit, aoe, pierce)`;
  - инструмент — рука, если она подходит, иначе алмазная кирка (с Sensitive — ножницы);
  - Fortune и Extract зачаровывают копию инструмента;
  - перед ломанием проверка `BlockUtil.destroyRespectsClaim` и тега `BREAK_BLACKLIST`, само ломание — `BlockUtil.breakExtraBlock`;
  - Mining Fatigue снижает amp.
- **Delay:**
  - длительность: база 20 тиков + Extend Time × 20 тиков − Duration Down × 10 тиков;
  - `spellContext.delay(new DelayedSpellEvent(...))` + `EventQueue.getServerInstance().addEvent(...)`;
  - по таймеру → `resolver.resume(world)`.

---

## 4. События (`api.event`, шина `NeoForge.EVENT_BUS`)
| Событие | Когда | Что можно |
|---|---|---|
| `SpellCastEvent` | перед кастом | отменить |
| `SpellResolveEvent.Pre/Post` | вокруг резолва | Pre — отменить весь резолв |
| `EffectResolveEvent.Pre/Post` | вокруг каждого эффекта | Pre — пропустить эффект; доступны `stats`, `effect` |
| `SpellModifierEvent` | сборка `SpellStats` | менять builder (amp, aoe, урон…) |
| `SpellCostCalcEvent.Pre/Post` | проверка и списание цены | `currentCost` |
| `SpellDamageEvent.Pre/Post` | урон заклинанием | менять `damage`, `damageSource`, отменить |
| `SpellProjectileHitEvent` | снаряд попал | отменить |
| `MaxManaCalcEvent` / `ManaRegenCalcEvent` | пересчёт маны игрока | `setMax`, `setReserve` / `setRegen` |
| `FamiliarSummonEvent`, `SummonEvent`, `ChimeraSummonEvent` | призывы | — |
| `DispelEvent` | Dispel на сущность | — |
| `ScribesTableInteractEvent` | клик по столу | — |
| `ParticleTimelineEvent` | частицы таймлайна | — |
| `ITimedEvent` + `EventQueue` | свои таймеры: `EventQueue.getServerInstance().addEvent(e)` | — |

---

## 5. Мана игрока
- Capability: `CapabilityRegistry.getMana(player)` → `ManaCap`.
  - `getCurrentMana`, `getMaxMana`, `addMana`, `removeMana`, `setMana`, `getGlyphBonus`, `getBookTier`, `getReserve`, `syncToClient`.
- **Максимум** (`ManaUtil.calcMaxMana`):
  ```
  baseMax 100 + glyphmax 15 × выученные глифы + tierMax 50 × тир книги
  ```
  - эта сумма идёт временным модификатором в атрибут `ars_nouveau.perk.max_mana`;
  - итог = значение атрибута → `MaxManaCalcEvent`;
  - резерв (фамильяры) вычитается: `getRealMax() = max × (1 − reserve)`.
- **Реген** (`ManaUtil.getManaRegen`):
  ```
  baseRegen 5 + glyphRegen 0.33 × глифы + tierRegen 1 × тир
  ```
  - итог тоже идёт через атрибут `mana_regen` → `ManaRegenCalcEvent`;
  - тик в `ManaCapEvents.playerOnTick` раз в `updateInterval` = 5 тиков, порция = реген / (20/5) — то есть значение регена в секунду;
  - синхронизация с клиентом при изменении и принудительно каждые 60 тиков.
- **Скидки:** `IManaDiscountEquipment.getManaDiscount(stack, spell)` — с Curios, брони и самого кастера.
- **Атрибуты** `PerkAttributes`:
  - `WARDING`, `MANA_REGEN_BONUS`, `MAX_MANA`, `SPELL_DAMAGE_BONUS`, `WHIRLIESPRIG`, `WIXIE`, `DRYGMY`, `FEATHER`, `WEIGHT`;
  - всё синхронизируемое. Самый чистый способ дать предмету +мана — модификатор этих атрибутов, как делают `AbstractManaCurio` и `AnimatedMagicArmor`.
- Прочие ключи `ServerConfig`: `manaBoost` 25 (зачарование), `manaRegenEnchantment` 2, `enforceCapOnCast`, `enforceGlyphLimitOnCast`, `infiniteSpells`.

---

## 6. Source (мана блоков)
- `ISourceTile` (`addSource / removeSource(amount, simulate)`, `canProvideSource`) — Source Jar, реле, машины.
- `SourceManager.INSTANCE`:
  - набор провайдеров на каждый мир, тик на `LevelTickEvent.Post`;
  - `SourceUtil.takeSource(pos, level, range, amount)`, `takeSourceWithParticles`, `takeSourceMultiple`, `hasSourceNearby`, `canGiveSource`.
- **Генерация:**
  - `SourcelinkTile` и наследники: Volcanic (огонь), Agronomic (рост растений), Mycelial (еда), Vitalic (смерть и рождение), Alchemical (зелья);
  - события в радиусе превращаются в Source.
- **Реле:** `RelayTile` (from/to), Splitter, Collector, Deposit, Warp.
  - Связь — жезлом доминиона через `IWandable.onFinishedConnectionFirst/Last`.

---

## 7. Крафт: типы рецептов (`ars_nouveau:*`)
| Тип | Формат JSON |
|---|---|
| `glyph` | `inputs[]`, `output`, `exp` |
| `enchanting_apparatus` | `reagent`, `pedestalItems[]`, `result`, `sourceCost`, `keepNbtOfReagent` |
| `imbuement` | `input`, `pedestalItems[]`, `output`, `source` |
| `crush` | `input`, `output[{stack, chance, maxRange}]` |
| `summon_ritual` | `augment`, `count`, `mobs[{mob, weight}]`, `source{mobSource}` |
| `budding_conversion` / `alakarkinos_conversion` | `input` → `result` |
| `enchantment`, `reactive_enchantment` | зачарование на аппарате |
| `armor_upgrade`, `book_upgrade`, `spell_write`, `caster_tome`, `potion_flask`, `dye`, `dispel_entity`, `prestidigitation`, `scry_ritual` | прочее |

- Все рецепты аппарата: `ArsNouveauAPI.getInstance().getEnchantingApparatusRecipes(level)`.

---

## 8. Ритуалы
- `AbstractRitual`:
  - `tick()` вызывается брейзером (`RitualBrazierTile`);
  - `getRegistryName()`, `canStart(player)`, `onStart`, `onEnd`, `setFinished()`, `incrementProgress()`;
  - предметы-катализаторы: `canConsumeItem` / `onItemConsumed` / `didConsumeItem`;
  - Source: `getSourceCost()`, `setNeedsSource(true)`, затем `needsSourceNow()` / `takeSourceNow()`.
- Готовые основы: `RangeRitual`, `RangeEffectRitual`, `StructureRitual`, `FeaturePlacementRitual`, `ConjureBiomeRitual`.
- Регистрация: `RitualRegistry.registerRitual(ritual)` в конструкторе мода; создаёт и предмет `RitualTablet`.

---

## 9. Фамильяры
- `AbstractFamiliarHolder(id, predicate)`: `getSummonEntity(level, tag)` → `IFamiliar`.
- `FamiliarRegistry.registerFamiliar(holder)`; свиток `FamiliarScript`.
- Готовые: Starbuncle, Drygmy, Whirlisprig, Wixie, Bookwyrm, Amethyst Golem, Jabberwog.
  - `FamiliarEntity` / `FlyingFamiliarEntity`, `ISpellCastListener` реагирует на касты владельца.
  - Резерв маны через `MaxManaCalcEvent.setReserve`.

---

## 10. Нити (перки) брони
- `IPerk` (`Perk`): атрибуты по уровню слота, `minimumSlot()`, `validForSlot(...)`, `onAdded` / `onRemoved`.
  - `ITickablePerk`, `IEffectResolvePerk` — реакция на эффекты.
- `PerkSlot.ONE/TWO/THREE`; `PerkRegistry.registerPerk`, `registerPerkProvider(item, List<List<PerkSlot>>)` — у брони список слотов по тирам.
- 22 перка: Bonded, Chilling, Depths, Feather, Gliding, Ignite, Immolate, JumpHeight, KnockbackResist, Looting, MagicCapacity,
  MagicResist, PotionDuration, Repairing, Saturation, SpellDamage, Starbuncle, StepHeight, Totem, Toughness, Vampiric.

---

## 11. Мобы и ИИ — полезно для Котто
- **Машина состояний** (`common.entity.statemachine`):
  - `IState`: `onStart`, `onEnd`, `tick()` → следующее состояние или `null`, `onEvent(e)`;
  - `SimpleStateMachine.tick()` переключает состояния.
- **Старбанкл:**
  - `DecideStarbyActionState` выбирает по приоритету: кровать (если запитана) → отнести → взять из контейнера → подобрать предметы → ягоды → кровать;
  - у каждой ветки свой случайный backoff (`takeItemBackoff`, `findItemBackoff`, `berryBackoff`, растущий до 10 с) — чтобы не дёргаться каждый тик;
  - `TravelToPosState`: раз в 100 тиков перепроверяет цель, «дошёл» при ≤ 2,5 блока, `tryMoveToBlockPos(pos, 1.3)`, сдаётся, если `!path.canReach()`.
- **Навигация** `MinecoloniesAdvancedPathNavigate` — асинхронный поиск пути с учётом дверей, лестниц и воды:
  - подключение: переопределить `getNavigation()`;
  - `PathingStuckHandler.createStuckHandler().withTeleportOnFullStuck().withTeleportSteps(5)` — телепорт при полном застревании;
  - это прямой кандидат, чтобы Котто ходил надёжнее ванильной навигации.
- **Поведения:** `BehaviorRegistry.register(id, (entity, tag) -> behavior)` — сменные «профессии» Старбанкла (транспорт, зелья).
- **Отладка:** `addGoalDebug` / `DebugEvent` — лента событий моба (у нас аналог — `CattoBrain.debug()`).
- **Прочие мобы:**
  - Drygmy — лут с мобов в банке;
  - Whirlisprig — Source от растений;
  - Wixie — зелья и крафт в котле;
  - Amethyst Golem — аметист;
  - Bookwyrm — кастует;
  - Wilden — враги;
  - Alakarkinos — археология (своя машина состояний).

---

## 12. Книга (Worn Notebook)
- `DocumentationRegistry`:
  - `registerMainCategory(DocCategory)`, `registerEntry(category, DocEntry)`;
  - категории: `GETTING_STARTED`, `SPELL_CASTING`, `ITEMS`, `CRAFTING`, `FIELD_GUIDE`, `SOURCE`, `ARMOR`, `RITUAL_INDEX`, `ENCHANTING`, `FAMILIARS`.
- **Свои записи** добавляются в слушателе `ReloadDocumentationEvent.AddEntries`:
  - событие срабатывает при каждом входе в мир, поэтому слушатель должен быть идемпотентным;
  - правки чужих записей — только в `.Post`.
- **Хелперы** `setup.registry.Documentation`:
  - `addPage(new DocEntryBuilder(category, id).withIcon(...).withIntroPage().withCraftingPages(item))`, `addBasicItem(item, category)`;
  - страницы: `TextEntry`, `CraftingEntry`, `ApparatusEntry`, `ImbuementRecipeEntry`, `GlyphEntry`, `EntityEntry`, `PerkDiagramEntry` …
- Для каждого глифа запись создаётся автоматически (иконка, тир, школы как поисковые теги).

---

## 13. Частицы и стиль заклинаний
- `TimelineMap` в `Spell` — по таймлайну на каждую форму или эффект:
  - Projectile, Touch, Self, Burst, Linger, Wall, Orbit, Rune, Light, Delay, Pantomime, Wololo, MageBlock, Prestidigitation;
  - реестр `ParticleTimelineRegistry`, тип `IParticleTimelineType<T>`.
- Движения: `Burst`, `Helix`, `Spiral`, `Tornado`, `Trail`, `Wave`, `Wisp`, `ZigZag`, `UpwardsField`, `UpwardsWall`, `LightBlob`, `None`.
- `ParticleEmitter(posSupplier, rotSupplier, TimelineEntryData)` → `.tick(level)`.
  - У глифа `createStaticEmitter(entryData, pos)` — мы это уже используем для своих эффектов.

---

## 14. Прочее
- **Сеть:** `Networking.sendToPlayerClient(packet, player)`, `sendToNearbyClient(level, pos, packet)`; `PortUtil.sendMessageNoSpam`.
- **Утилиты:**
  - `SpellUtil.calcAOEBlocks(...)`, `rayTrace(entity, len, offset, liquids)`, `DFSBlockstates(level, start, max, predicate)`;
  - `BlockUtil.breakExtraBlock`, `destroyRespectsClaim`;
  - `ANFakePlayer.getOrFakePlayer(level, entity)` — фейковый игрок для урона и ломания от лица моба.
- **Capability:** `CapabilityRegistry` — `MANA`, `PLAYER_DATA` (`ANPlayerDataCap`: выученные глифы, фамильяры), `BLOCK_SPELL_RESOLVE_CAP`, `ENTITY_SPELL_RESOLVE_CAP`.
- **Камера скрайера** (`ICameraMountable`, `CameraController`): сервер подменяет `serverPlayer.camera` на сущность-камеру и форсирует чанки.
  - Это тяжёлый механизм для вида издалека.
  - Для кат-сцены диалога наш `CameraMixin` (подмена позиции и поворота в `Camera.setup`) проще и правильнее.

---

## 15. Что из этого стоит взять в наш мод
1. **Котто:** перейти на `MinecoloniesAdvancedPathNavigate` + stuck-handler.
   - Это решает застревания у дверей и на лестницах.
   - Перед этим проверить, что класс доступен без internal-пакета (он в `common.entity.pathfinding`, публичный).
2. **Бонусы маны от наших аксессуаров и снаряжения Котто:** делать модификаторами атрибутов `PerkAttributes.MAX_MANA` / `MANA_REGEN_BONUS`, а не правкой `ManaCap` напрямую.
   - Тогда они честно учитываются в UI и событиях.
3. **Скидки на заклинания:** `IManaDiscountEquipment` вместо собственных хаков через `SpellCostCalcEvent`.
   - Событие оставить только для условных скидок.
4. **Блоки и мобы, реагирующие на заклинания** (поглотить, отразить): capability `IResolveListener` чище, чем `EffectResolveEvent`.
5. **Свои отложенные действия:** `ITimedEvent` + `EventQueue` вместо самописных счётчиков.

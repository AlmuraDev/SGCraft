# API

SGCraft has a Stargate API for add-on mods: find and dial gates, read and change their properties, add energy sources, rights, address books and gate types, and listen to events, on NeoForge and Fabric alike.

## Project layout

SGCraft is built from four projects:

| Project | What it is |
| --- | --- |
| `api` | The API: the `com.almuradev.sgcraft.api` packages and nothing else. Add-ons build against this alone. |
| `core` | The mod itself, written against vanilla Minecraft only (`com.almuradev.sgcraft.mod`). Not part of the API. |
| `neoforge` | Loads core on NeoForge. |
| `fabric` | Loads core on Fabric. |

Each loader's SGCraft jar has the API and core compiled into it, so the API classes are there at run time whenever SGCraft is installed. Compile against the api jar and don't ship it in your own jar. The api jar is published as `com.almuradev.sgcraft:sgcraft-api-26.3`:

```groovy
dependencies {
    compileOnly "com.almuradev.sgcraft:sgcraft-api-26.3:${sgcraft_version}"
}
```

SGCraft's mod ID is `sgcraft`. Declare it as a dependency, required or optional, in your mod's metadata.

### Optional dependency

If your mod runs without SGCraft, ask your mod loader whether `sgcraft` is installed before your code touches any class in the API: `ModList.get().isLoaded("sgcraft")` on NeoForge, `FabricLoader.getInstance().isModLoaded("sgcraft")` on Fabric. Keep the code that uses the API in classes of its own, and load them only after that check passes.

## Getting the API

`SGCraftApi.get()` returns the one API instance. It throws `IllegalStateException` if no implementation is installed. The instance is usable as soon as it loads, in whatever order mods start. It hands out each service:

| Method | Returns | For |
| --- | --- | --- |
| `addressing()` | `Addressing` | Turning gate positions into addresses and back |
| `stargates()` | `Stargates` | Finding and placing gates |
| `addressBooks()` | `AddressBooks` | Players' address books |
| `energy()` | `EnergyService` | Energy bridge rates |
| `eventBus()` | `EventBus` | Listening to events |
| `causeFactory()` | `Cause.Factory` | Behind `Cause.of`; use `Cause.of` instead |

`SGCraftApi.NAMESPACE` is `sgcraft`, the namespace of every ID the API defines. `SGCraftApi.VERSION` is the version of the API contracts.

## Threading

Use gates, access, address books and energy on the server thread. The implementation may throw `IllegalStateException` when called from another thread. Events are fired on the server thread, and the `CompletionStage` a dial returns completes on the server thread.

`Stargates` and `AddressBooks` need a running server. Without one, lookups find nothing and placement fails with `INVALID_POSITION`.

## Cause and ActionResult

Every action takes a `Cause`: every object that had a hand in it, from the most direct (the root) to the least. A player dialing with a DHD gives the DHD's block entity, then the player.

- Make one with `Cause.of(root, others...)`. Put an object of your own at the root, such as your block entity, your entity or a record you define. `cause.withDirect(object)` puts a new root in front of an existing cause.
- Read one with `first(type)`, `last(type)`, `all(type)`, `contains(type)`, `root()` and `objects()`. Look up the types you care about, so causes holding types you don't know still work.
- Causes are immutable and compare by their objects in order.

Actions that either happen or don't return an `ActionResult`:

| Value | Meaning |
| --- | --- |
| `DONE` | It happened |
| `NO_CHANGE` | Nothing needed doing, such as closing a closed iris |
| `DENIED` | The cause isn't allowed |
| `CANCELED` | A listener canceled the action's event |
| `NOT_POSSIBLE` | It can't happen in the gate's current state, or the gate no longer exists |

## Stargates

### Finding gates

`Stargates` finds gates:

| Method | What it does |
| --- | --- |
| `at(GlobalPos)` | The gate a block is part of |
| `byAddress(Address, ResourceKey<Level> from)` | The gate an address reaches from a world |
| `in(ResourceKey<Level>)` | Every gate in a world, including gates in unloaded chunks |
| `mostAppropriate(GlobalPos)` | The gate best suited to a position, such as to dial from |
| `mostAppropriate(GlobalPos, Cause)` | The same, leaving out gates the cause may not use |
| `place(GatePlacement, Cause)` | Build a whole formed gate |

A `Stargate` is a view of a gate. You may keep one, but the gate can be broken at any time. After that, `exists()` is false, the getters return the last values, and actions report the gate is gone.

### Reading a gate

A `Stargate` has these getters: `position()` (the base block, which the address points to), `orientation()`, `type()`, `size()`, `chevrons()`, `state()`, `connection()`, `iris()`, `energy()`, `access()`, `owner()` and `get(property)`.

`GateState` is `IDLE`, `DIALING`, `INCOMING`, `OPENING`, `CONNECTED` or `CLOSING`. A `Connection` gives the `dialer()`, the `target()`, the `address()`, `other(gate)`, `ticksOpen()` and `ticksRemaining()`.

### Dialing

- `dial(address, cause)` dials in `DialMode.STANDARD`, a symbol at a time. `dial(address, mode, cause)` takes `DialMode.QUICK` to lock every chevron at once. `dial(GlobalPos, cause)` dials the gate `mostAppropriate` picks there, by the shortest address.
- Each returns a `CompletionStage<DialResult>`, completed with `CONNECTED` once the wormhole opens, or with the reason it failed. A dial refused at the start returns a stage already complete.
- `engage(symbol, cause)` locks the next chevron at once, as a DHD button does. `engaged()` lists the symbols locked so far, and `dialEngaged(cause)` dials them.
- `disconnect(cause)` stops a dial (which ends `INTERRUPTED`) or closes the wormhole, from either end.

`DialResult` values: `CONNECTED`, `INVALID_ADDRESS`, `UNREACHABLE`, `NOT_ENOUGH_CHEVRONS`, `NO_GATE`, `SAME_GATE`, `BUSY`, `TARGET_BUSY`, `INCOMPATIBLE`, `NOT_ENOUGH_ENERGY`, `DENIED`, `CANCELED`, `INTERRUPTED`, `GATE_GONE`. `INCOMPATIBLE` means the two gates can't connect because one of their types refuses the other (see `GateType.connectsTo` under [Registries](#registries)). See [Dialing](dialing.md) for what players see for each.

### Changing a gate

| Method | Needs |
| --- | --- |
| `set(property, value, cause)` | The `configure` right; `mayManage` for `default_rights`, `player_rights`, `breakable` and `generated` |
| `remove(property, cause)` | The same |
| `setOwner(Optional<UUID>, cause)` | `mayManage` |
| `setType(type, cause)` | An idle gate, and a type that supports the gate's size |
| `setChevrons(count, cause)` | An idle gate, and at least 1 chevron |
| `setOrientation(FrontAndTop, cause)` | An idle gate whose ring covers the same blocks that way (a flat gate turns face up or face down) |
| `placeFixtures(FixtureBuffer, PlacementMode, cause)` | Room for the blocks, unless the mode is `FORCE` |

### Placing a gate

Build a `GatePlacement` with `GatePlacement.builder()`. Set `position` (the base block), `orientation`, `type` and `chevrons`. The rest have defaults: `size` is `GateSize.STANDARD` (radius 2), `mode` is `PlacementMode.ATTEMPT`, `searchRadius` is 0 (the position only), and `fixtures` are the type's own (such as its DHD) unless you set others or call `noFixtures()`.

`ATTEMPT` places the gate only where its space is clear, in the nearest clear spot within the search radius. `FORCE` destroys what is in the way, without drops, but never another gate. `place` returns a `PlacementResult` with a `status()` (`PLACED`, `OBSTRUCTED`, `GATE_IN_THE_WAY`, `UNSUPPORTED_SIZE`, `RULE_FAILED`, `INVALID_POSITION`, `DENIED` or `CANCELED`), the `gate()` if placed, and a `reason()` to show players. A player in the cause owns the new gate.

### Properties

A `GateProperty<T>` is a codec and an optional default. `GateProperties` holds every property SGCraft registers, such as `GateProperties.OPEN_TICKS`. [Settings](settings.md#gate-properties) lists them all. `gate.get(property)` returns the gate's own value, else the server's [gate default](settings.md#gate-defaults), else the property's default.

An add-on can register its own properties in the `sgcraft:gate_property` registry, in its own namespace. The configurator shows them on its **Other** page as JSON.

## Addressing

- `Address` is a list of `Symbol`s. `Address.parse(text)` reads any grouping and ignores case, dashes and spaces. `toString()` writes `XXXX-XXX`, then a dash before any symbols after the seventh. `Address.CODEC` saves it as that text.
- `Symbol` has 37 values (`Symbol.COUNT`), written A to Z, 0 to 9 and `@`. `Symbol.GIZA` (index 36, `@`) is the default point of origin.
- `Addressing.addressOf(target, from)` gives the shortest address that reaches a gate's base from a world. `resolve(address, from)` gives the world and chunk an address points to. `reachOf`, `reaches()`, `partition(world)`, `groups()`, `defaultGroup()` and `groupOf(world)` give the rest.

Addresses are relative to the world they are dialed from. The three reaches every implementation registers (`AddressReaches`) are:

| Reach | Symbols | Reaches |
| --- | --- | --- |
| `sgcraft:same_world` | 7 | A gate in the dialing gate's world |
| `sgcraft:same_group` | 8 | A gate in another world of the same group: position, then the world's symbol |
| `sgcraft:other_group` | 9 | A gate in a world of another group: position, world symbol, then the group's symbol |

An add-on can register another `AddressReach` with a new symbol count, such as ten symbols. Only one reach may have each count. See [Addresses](addresses.md).

## Address books

`AddressBooks.forPlayer(uuid)` gives a player's own book, kept whether they are online or not. `available(player)` gives every book they can use now: their own first, then each provider's.

An `AddressBook` lists `AddressBookEntry` records (an `address` and a `name`) in display order. It has `size()`, `stream()`, `find(address)`, and `add`, `rename`, `remove` and `move` (each with a cause). A book that can't be changed returns `NOT_POSSIBLE` from those. Its `source()` says what holds it: `AddressBookSource.PLAYER`, `ITEM`, `BLOCK`, or a kind of your own.

Register an `AddressBookProvider` in `sgcraft:address_book_provider` to offer players more books, such as one shared by a team.

## Energy

All amounts are in the API's own energy unit.

- `GateEnergy` (from `gate.energy()`) is the gate's store: `stored()`, `capacity()`, `insert(amount, simulate)` and `extract(amount, simulate)`.
- An `EnergySource` is something a gate draws from: `available()` and `draw(wanted, simulate)`. When not simulating, the source changes what backs it, such as using up an item.
- An `EnergySourceProvider` in `sgcraft:energy_source_provider` returns the sources a gate can reach right now. SGCraft asks every provider each time a gate needs energy, so don't keep sources. SGCraft's own are `sgcraft:dhd_fuel`, `sgcraft:power_unit` and `sgcraft:zpm`. A gate draws from one source after another, in the order each provider returns them, and moves on only when a source runs short. SGCraft asks its own providers in the order `dhd_fuel`, `power_unit`, `zpm`, and each returns its sources nearest the gate's base first.
- An `EnergyBridge` in `sgcraft:energy_bridge` links an outside energy system. A bridge does its own work: it makes gates reachable from its system and moves energy in through `GateEnergy.insert`, at the rate `SGCraftApi.get().energy().rate(bridge)` gives. That is the server's `energy.bridge_rates` value for the bridge, or else its `defaultRate()`. SGCraft's own bridges are `sgcraft:direct` (1.0) and `sgcraft:forge_energy` (80 FE to one GE).

A gate whose `requires_power` is false dials without drawing anything. See [Power](power.md).

## Access

`gate.access()` returns a `GateAccess`:

| Method | Answers |
| --- | --- |
| `allowsIncoming(from)` | Whether the gate accepts a dial from another gate: its address rule, else `allow_incoming` |
| `allowsOutgoing(to)` | Whether the gate may dial another: its address rule, else `allow_outgoing` |
| `allows(UUID, GateRight)` | Whether a player has a right: every right for the owner, else their `player_rights`, else `default_rights` |
| `allows(Cause, GateRight)` | The same, for the most direct player in the cause |
| `mayManage(Cause)` | Whether the cause manages the gate: changes rights, breakability and the owner |

The rights are `GateRight.USE` (dial, close, travel), `GateRight.IRIS` and `GateRight.CONFIGURE`. No right grants managing: SGCraft lets the owner and operators manage a gate, and causes with no player in them. An add-on can make its own `GateRight` in its own namespace and check it with `allows`. SGCraft stores such rights but gives them no meaning. An `AddressRule` is a record of `incoming` and `outgoing`. See [Access](access.md).

## Registries

`SGCraftRegistries` holds the keys. Register into them through your mod loader, as you register blocks or items.

| Key | Entries | SGCraft's own |
| --- | --- | --- |
| `sgcraft:address_reach` | `AddressReach` | `same_world`, `same_group`, `other_group` |
| `sgcraft:gate_type` | `GateType` | `milky_way`, `pegasus`, `universe`, `ori` (`GateTypes`) |
| `sgcraft:iris_type` | `IrisType` | `metal`, `shield` (`IrisTypes`) |
| `sgcraft:energy_source_provider` | `EnergySourceProvider` | `dhd_fuel`, `power_unit` |
| `sgcraft:energy_bridge` | `EnergyBridge` | `direct`, `forge_energy` |
| `sgcraft:gate_property` | `GateProperty<?>` | Every property in `GateProperties` |
| `sgcraft:address_book_provider` | `AddressBookProvider` | `player`, `pdd` |

A `GateType` gives its `name()`, whether it `supports(size)`, the `fixtures(size)` it builds around a gate, its `placementRules()`, and whether it `acceptsDhd(style)`. A type that accepts no DHD is dialed some other way, as the Universe gate is.

Two more methods have defaults that allow everything, so a type overrides them only to refuse:

- `connectsTo(other)` answers whether a gate of this type can connect to a gate of type `other`. A wormhole forms only if both types allow it, each asked about the other's type, so either type can refuse. A refused dial ends `INCOMPATIBLE`. The default is `true`.
- `acceptsIris(iris)` answers whether a gate of this type can have an iris of that kind fitted. The default is `true`.

`GateTypes.ORI`, the Ori supergate, is built only as a supergate, connects only to other Ori supergates and takes no iris. `IrisTypes.METAL` is the metal iris, which closes like a camera shutter. `IrisTypes.SHIELD` is an energy shield, which rises and falls in moments and needs energy to stay raised. A raised shield stops travelers as a shut iris does. Both are irises to the API: `gate.iris()` returns an `Iris` whose `type()` names the kind, and `close(cause)` raises a shield as `open(cause)` lowers it.

## Events

`SGCraftApi.get().eventBus()` takes listeners:

- `register(type, priority, ignoreCanceled, listener)` returns a `Registration`. Call `unregister()` on it to remove the listener.
- `register(type, listener)` registers at `EventPriority.NORMAL` and also runs for canceled events.
- Priorities run `FIRST`, `EARLY`, `NORMAL`, `LATE`, `LAST`. Listeners at the same priority run in the order they registered. Listeners at `LAST` should not change the event.
- A listener for a type hears its subtypes too, so a `GateEvent` listener hears every gate event.
- A listener that throws is logged, and the others still run.
- `Pre` events come before an action and can be canceled (a later listener may un-cancel). `Post` events tell you what happened. Every event has a `cause()`.

| Event | Cancelable | What you can change | When |
| --- | --- | --- | --- |
| `AccessEvent.CheckPlayer` | No | `setAllowed` | Each time a gate answers whether a player has a right |
| `AccessEvent.CheckConnection` | No | `setAllowed` | Each time a gate answers whether it connects with another |
| `AccessEvent.ChangeAddressRule.Pre` | Yes | `setNewRule` | Before one address rule, or the incoming and outgoing defaults, change |
| `AccessEvent.ChangeAddressRule.Post` | No | | After |
| `AccessEvent.ChangeRights.Pre` | Yes | `setNewRights` | Before one player's rights, or the default rights, change |
| `AccessEvent.ChangeRights.Post` | No | | After |
| `BreakGateEvent.Pre` | Yes | | Before a gate breaks; some causes, such as commands, can't be stopped |
| `BreakGateEvent.Post` | No | | After; `exists()` is false |
| `ChangeGateOwnerEvent.Pre` | Yes | | Before `setOwner` hands the gate over |
| `ChangeGateOwnerEvent.Post` | No | | After |
| `ChangeGatePropertyEvent.Pre` | Yes | `setNewValue` | Before a property is set or removed |
| `ChangeGatePropertyEvent.Post` | No | | After |
| `ChangeGateStateEvent` | No | | After a gate's state changes |
| `ConnectionEvent.Open` | No | | Once a wormhole is open (once per connection) |
| `ConnectionEvent.Disconnect` | Yes | | Before a wormhole closes because someone asked |
| `ConnectionEvent.Close` | No | | Once a wormhole has closed, for any reason |
| `DialEvent.Pre` | Yes | | After the address is checked, before the first chevron locks; canceling ends the dial `CANCELED` |
| `DialEvent.Cost` | No | `setChevronCost`, `setOpenCost`, `setTickCost` | After `Pre`, to settle the cost; not fired when `requires_power` is false |
| `DialEvent.LockChevron` | No | | Each time a chevron locks on the dialing gate |
| `DialEvent.Post` | No | | When the dial ends, with its `result()` |
| `FormGateEvent.Pre` | Yes | | When a player's gate is complete, before it forms |
| `FormGateEvent.Post` | No | | Once it has formed |
| `IrisEvent.Pre` | Yes | | Before an iris starts to move |
| `IrisEvent.Post` | No | | Once it has finished moving |
| `PlaceGateEvent.Pre` | Yes | `setPlacement` | Before `Stargates.place` does anything |
| `PlaceGateEvent.Post` | No | | Once the gate is placed and formed |
| `TravelEvent.Pre` | Yes | `setDestination` | When something enters a wormhole, before it moves |
| `TravelEvent.Post` | No | | Once it has come out of the destination |

Canceling a `ChangeAddressRule.Pre` or `ChangeRights.Pre` keeps that one entry as it was. Other entries changed at the same time still change. Each such change fires before the general `ChangeGatePropertyEvent`, which sees the value as those listeners left it.

### Example: stopping dials into the Nether

This listener cancels any dial a player makes to a gate in the Nether. The player sees "Something stopped the dial".

```java
public final class NetherLock {

    public void register() {
        SGCraftApi.get().eventBus().register(DialEvent.Pre.class, EventPriority.NORMAL, true, this::onDial);
    }

    private void onDial(final DialEvent.Pre event) {
        final boolean intoNether = event.target().position().dimension().equals(Level.NETHER);
        if (intoNether && event.cause().first(ServerPlayer.class).isPresent()) {
            event.setCanceled(true);
        }
    }
}
```

### Example: an energy source

This provider lets gates draw from an add-on's generators near the gate's base. `Generators` and `GeneratorBlockEntity` stand for your own code.

```java
public final class GeneratorSources implements EnergySourceProvider {

    @Override
    public List<EnergySource> sourcesFor(final Stargate gate) {
        return Generators.near(gate.position(), 4).stream()
                .<EnergySource>map(GeneratorSource::new)
                .toList();
    }

    private record GeneratorSource(GeneratorBlockEntity generator) implements EnergySource {

        @Override
        public long available() {
            return this.generator.stored();
        }

        @Override
        public long draw(final long wanted, final boolean simulate) {
            final long drawn = Math.min(wanted, this.available());
            if (!simulate) {
                this.generator.take(drawn);
            }
            return drawn;
        }
    }
}
```

Register it on NeoForge from your mod's `RegisterEvent` listener:

```java
private static void register(final RegisterEvent event) {
    event.register(SGCraftRegistries.ENERGY_SOURCE_PROVIDER, helper -> helper.register(
            ResourceKey.create(SGCraftRegistries.ENERGY_SOURCE_PROVIDER,
                    Identifier.fromNamespaceAndPath("mymod", "generators")),
            new GeneratorSources()));
}
```

On Fabric, register it into the same registry with `Registry.register` as your mod starts.

## Versioning

The API contracts carry their own version, `SGCraftApi.VERSION`, separate from SGCraft's version. It is `0.1.0`, and it follows semantic versioning: a new major version may break callers, and a new minor version only adds. Every API type says `@since` the version that added it.

A minor version may add constants to `DialMode`, `DialResult`, `GateState` and `PlacementResult.Status`. A `switch` over one of these needs a `default` branch.

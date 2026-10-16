# Access

Every gate has an owner and a set of rights that decide who may dial it, work its iris and change its settings. Address rules decide which other gates may connect to it.

## Owners

A gate belongs to the player whose block finished it, the player who placed its [spawner](building-gates.md#gate-spawners), or the player who ran `/sgcraft place`. Gates the world builds have no owner, so operators manage them (see [World generation](worldgen.md#generated-gates)).

The owner has every right on the gate and manages it: only the owner and operators change who may do what.

- `/sgcraft owner` shows who owns the gate. Anyone can use it.
- `/sgcraft owner <player>` hands the gate to another player. `/sgcraft owner --none` leaves it with no owner. Only the owner or an operator can do either.
- The configurator's **Access** page shows the owner at the top.

## Rights

| Right | Lets a player |
| --- | --- |
| `sgcraft:use` | Dial the gate (DHD, PDD, tablet, command), stop a dial, close the wormhole, travel through it, and add the gate to a gate control tablet |
| `sgcraft:iris` | Open and close the gate's iris, with a GDO, a tablet or `/sgcraft iris` |
| `sgcraft:configure` | Change the gate's settings: its properties, address rules, orientation and camouflage, fit upgrades, and open its configurator. Type, chevrons and iris change with upgrade items, or freely for operators and creative players |

The configure right doesn't cover who may do what: see [Managed settings](#managed-settings).

To travel, every player in a stack of riders needs `use` at both gates. Anyone without it is told "You aren't allowed to travel through this gate".

> **Note:** Add-ons can add rights of their own, and their event listeners have the final say on every check. See [API](api.md).

## Who has which rights

A gate answers in this order:

1. The owner has every right.
2. Operators (permission level 2) have every right.
3. Anything that isn't a player has every right: redstone, command blocks, the server console and the server itself. While the gate's **Redstone works iris** setting is on, a redstone signal always works the iris.
4. A listed player has exactly the rights the gate lists for them, in place of the defaults.
5. Everyone else has the gate's default rights, which are `use` and `iris` unless changed.

> **Note:** For travel, the gate checks only its owner, its list and its defaults. An operator who isn't the owner needs `use` at both gates to travel, like anyone else.

## Default and per-player rights

The default rights (`sgcraft:default_rights`) are what players the gate doesn't list may do. A player's own entry in `sgcraft:player_rights` replaces the defaults for that player, so a listed player with only `use` can't work the iris even when the defaults allow it.

Change them with commands:

- `/sgcraft rights` shows the owner, the defaults and every listed player.
- `/sgcraft rights default <rights>` sets the defaults.
- `/sgcraft rights <player> <rights>` lists a player with their own rights. `none` lists them with no rights. `reset` takes them off the list, so the defaults apply to them again.

`<rights>` is a comma-separated list such as `use,iris,configure`, `use,iris` or `use`, or `none`. The player must have been seen on the server.

Or use the configurator's [Access page](#the-configurators-access-page).

## Managed settings

These settings decide who may do what, so the configure right isn't enough to change them. Only the owner, an operator, or something that isn't a player changes them:

- the default rights (`sgcraft:default_rights`)
- the listed players (`sgcraft:player_rights`)
- whether anyone may break the gate (`sgcraft:breakable`)
- whether the world built it (`sgcraft:generated`)
- the owner

A player with only the configure right sees these grayed out in the configurator, with the note "Only the owner or an operator changes rights".

## Address rules

Two settings decide which gates may connect:

- **Incoming allowed** (`sgcraft:allow_incoming`, on): other gates may dial this one.
- **Outgoing allowed** (`sgcraft:allow_outgoing`, on): this gate may dial others.

An address rule sets both for one other gate and wins over these two settings. A rule names the other gate by address, read from this gate's world: a 7 symbol address names a gate in the same world, and the longer forms name gates in other worlds.

A dial goes ahead only when both gates allow it: the dialing gate's rule for the target (or its **Outgoing allowed**), and the target's rule for the dialing gate (or its **Incoming allowed**). Otherwise the dial fails with "You aren't allowed to dial that".

Any player with the configure right can change the address rules and the two settings. They don't have to be the owner.

On the gate, the rules are kept as the `sgcraft:address_rules` property:

```json
{
  "ABCD-EFG": { "incoming": false, "outgoing": true }
}
```

Operators can also use commands:

- `/sgcraft rule` shows the two settings and every rule.
- `/sgcraft rule set <incoming> <outgoing> <address>` adds or changes a rule, such as `/sgcraft rule set false true ABCD-EFG`.
- `/sgcraft rule remove <address>` takes a rule off.

## Breaking gates

A player may break a block of a formed gate when one of these holds:

- They are an operator.
- They own the gate and `owners_break_gates` is on (it is by default).
- The gate's **Breakable** setting (`sgcraft:breakable`) is on. It is off by default. A generated gate with no value of its own uses `generated_gates_breakable`, off by default.

Anyone else is told "This gate can't be broken". Explosions and commands still break gates. Breaking a gate while its wormhole is open sets off a breach explosion: see [Building gates](building-gates.md#breach-explosion).

## The configurator's Access page

Open a gate's [configurator](tools.md#stargate-configurator) and choose the **Access** tab.

- At the top: "Owner: <name>", or "Owner: none, so operators manage it". A player who doesn't manage the gate also sees "Only the owner or an operator changes rights".
- **Incoming allowed** and **Outgoing allowed**.
- **All: use**, **All: iris** and **All: configure**: the default rights. Only the owner or an operator can change them.
- The listed players, five at a time, each with **use**, **iris** and **configure** switches and an **x** button that takes them off the list. **<** and **>** turn the pages.
- **Player name** and **List player**: type a name and press **List player** to list that player with the gate's current default rights, then change their switches.
- The address rules, each with an **In** and an **Out** switch and an **x** button. Type an address in **Address** and press **Add rule** to add a rule that allows both ways, then turn off what you want refused.

Changes wait until you press **Save**. **List player** saves at once, with any other changes you made. A switch in gray shows a value the gate has no setting of its own for.

## Server admins

- `/sgcraft settings owners_break_gates <true|false>` and `/sgcraft settings generated_gates_breakable <true|false>` change the breaking rules and write the settings file.
- `gate_defaults` gives gates values without their own. For example, to let players without their own entry only dial and travel:

```
/sgcraft gate_defaults sgcraft:default_rights ["sgcraft:use"]
```

- `/sgcraft gate <pos> ...` runs any gate command on the gate at a position.

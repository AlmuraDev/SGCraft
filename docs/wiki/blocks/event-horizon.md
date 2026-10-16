# Event Horizon

The Event Horizon is the unseen block that fills the opening of a gate while its wormhole is open. It lights the area around the gate and controls how fluid passes into the wormhole.

```infobox
Event Horizon
ID: sgcraft:event_horizon
Category: Technical
Type: Block
Tool: None
Hardness: Unbreakable
Blast resistance: 3,600,000
Light: 9 to 15 on a standard gate while the wormhole is open, 0 behind a shut iris
Waterloggable: No
Drops: Nothing
Block entity: No
```

## Obtaining

The Event Horizon has no item. You can't craft it, mine it or find it in the creative tab. Gates place it and take it away themselves.

## Behavior

Once a wormhole has opened and its kawoosh is over, both gates fill their openings with event horizon blocks. Only the air blocks of the opening fill: a block of the opening that holds fluid stays fluid. The gate checks its opening every 10 ticks and fills any air that has appeared. When the wormhole closes, the event horizon blocks turn back to air.

- The block is invisible. The gate draws the wormhole's surface itself, from its base.
- It has no collision and no shape, so you walk through it and can't target it.
- It does nothing to what touches it. Travel is the gate's work: anything that crosses the opening from the front travels. See [Dialing](../dialing.md#traveling-through-the-wormhole).
- The kawoosh's damage comes from the gate in the 20 ticks before the event horizon appears. See [Dialing](../dialing.md#the-kawoosh).
- It can't be broken, and pistons can't move it. Fluid never replaces it, and the kawoosh never clears it.
- A gate's ring counts an event horizon block in its opening as empty, the same as air or fluid.

## Light

The event horizon gives off light that rises and falls as waves run out across the opening from its center. A wave takes 40 ticks to pass, and the light moves on every 4 ticks.

- On a standard gate, each block of the horizon gives light from 9 to 15.
- On a supergate, only some blocks of the horizon give light, spread across the opening at least 3 blocks apart (a third of the radius apart on bigger supergates). The rest give none.
- Behind a shut iris, the event horizon gives no light. See [The iris](../iris.md#what-a-shut-iris-does).

## Fluid

The event horizon of a gate that sends through the wormhole takes fluid in. The event horizon of a gate that doesn't send holds fluid back, as a wall would. Fluid goes into and comes out of the front of a gate only, and never runs through a gate from front to back.

- The dialing gate sends. The dialed gate also sends when travel goes both ways, which it does when the dialing gate's `one_way_travel` property is off.
- Neither gate sends while an iris at either end is shut, and a gate doesn't send while it pours out fluid that came from the far end.
- Fluid flowing into the front of a sending horizon goes into the wormhole.
- A bucket emptied into a sending horizon, from in front of it, pours its fluid into the block in front, to run in from there. That block must be air. A bucket emptied anywhere else on the horizon is refused.

Both gates need their `spills_fluid` property on (the default) for fluid to go through. See [Dialing](../dialing.md#fluid-through-the-wormhole).

## Block states

| State | Values | Meaning |
| --- | --- | --- |
| `sending` | `true`, `false` | Whether the gate sends through the wormhole and takes fluid in |
| `facing` | `north`, `south`, `east`, `west`, `up`, `down` | The way the gate's front faces |
| `level` | `0` to `15` | How much light the block gives |

## Settings

- The client setting `hd_event_horizon` (default `true`) draws the wormhole's surface with its detailed texture. `false` uses the original's plain one. See [Settings](../settings.md#client-settings).
- The gate property `horizon_transparent` (default `true`, **See-through horizon** in the configurator) lets a little of what is behind the open wormhole show through.

## See also

- [Dialing](../dialing.md)
- [The iris](../iris.md)
- [Milky Way Stargate Base](stargate-base.md)

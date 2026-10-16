# Iris plate

An iris plate is an unseen entity that makes a shut iris solid. A gate puts plates across its opening while its iris is shut, so nothing can walk into the gate from either side.

```infobox
Iris plate
ID: sgcraft:iris_plate
Category: Technical
Type: Entity (technical)
Size: Up to 2 blocks across and 2 blocks up, 0.3 blocks thick
Health: None (can't be hurt)
Speed: Doesn't move
Summonable: No
Saved: No
Drops: None
```

## Obtaining

You can't get an iris plate. It has no item, `/summon` refuses it, and it has no loot table. Only a gate's base makes plates.

## Behavior

- A gate puts plates across its opening once its iris is half shut, which is when the iris starts stopping travelers. It takes them away as the iris opens past half.
- The gate splits its opening into a grid of plates, each at most 2 blocks across and 2 blocks up. A plate is 0.3 blocks thick and stands where the blades are, a little in front of the event horizon.
- A plate is solid to walk into, as a boat is. Players and mobs bump against it.
- You can't see it, hit it, push it or select it with the crosshair. It takes no damage, ignores explosions and doesn't burn. It has no gravity and moves through blocks.
- A plate that its gate no longer claims removes itself within 20 ticks, such as when the gate is broken or its base is gone.
- Plates go when the gate's base is broken, and when the gate is stood up or laid flat. A gate whose iris is still shut puts new plates down.

The plates only block walking. What happens to things that arrive through the wormhole at a shut iris is the gate's own work. See [What a shut iris does](../iris.md#what-a-shut-iris-does).

## Server admins

- Plates show in entity counts and selectors such as `@e`, as `sgcraft:iris_plate`. A gate with a shut iris has one plate for each part of its opening grid.
- Plates aren't saved with the world. The gate's base puts them back the next time it ticks after its chunk loads.
- `/kill @e` removes the plates, but a gate whose iris is still shut puts them back on its next tick. To clear them for good, open the iris (see [Commands](../commands.md#iris)).
- Clients track plates within 10 chunks.

## Add-on developers

- The type is `SGEntityTypes.IRIS_PLATE` in the core module (`com.almuradev.sgcraft.mod.entity.IrisPlateEntity`). It is in the `misc` mob category, registered with no loot table, no save and no summon, and fire immune.
- Its registered size is 0 by 0. The real bounding box comes from synced data: half width, half height, the way the gate faces and the way the top of its ring points. The box is 0.15 blocks deep either side of the plate's middle, along the gate's facing axis, so it fits standing and flat gates.
- `canBeCollidedWith` returns true for every entity, so it acts as a solid hitbox. `isPickable` and `isPushable` return false, `push` does nothing, `hurtServer` returns false and `ignoreExplosion` returns true.
- Each plate remembers its gate's base position. Every 20 ticks it checks that the block entity there is a `StargateBaseBlockEntity` whose `holdsPlate` returns true for it, and discards itself if not.
- `shouldBeSaved` returns false, and the plate reads and writes no data. Don't keep references to plates across chunk unloads: the base makes new ones.
- The client draws nothing for it (a no-op renderer). The iris you see is drawn with the gate.
- Treat the plate as internal. The public API in `api/` doesn't expose it, so read a gate's iris through `Stargate.iris()` instead. See [Reading a gate](../api.md#reading-a-gate).

## See also

- [The iris](../iris.md)
- [What a shut iris does](../iris.md#what-a-shut-iris-does)
- [Opening and closing](../iris.md#opening-and-closing)

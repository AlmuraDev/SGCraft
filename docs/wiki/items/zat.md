# Zat

The Zat, the Goa'uld zat'nik'tel, fires a bolt of lightning where you look, as in SGCraft 2.2.0. As on the show, one shot stuns and a second shot while the target is still stunned kills.

```infobox
Zat
ID: sgcraft:zat
Category: Weapons
Type: Item
Stackable: No
Range: 30 blocks (zat.range)
Cooldown: 1 second (zat.cooldown_ticks)
```

## Obtaining

### Crafting

```recipe
sgcraft:zat
```

## Usage

Use it to fire. It charges as its sound starts, and the shot comes three quarters of a second later, where you look by then. The bolt stops at the first block or creature in its way, shown as a line of sparks.

- A first shot does 4 harm (`zat.stun_damage`) through armor and stuns for 10 seconds (`zat.stun_ticks`): the target is slowed, weakened and tired, and a player's view sways.
- A second shot while the target is stunned kills it (`zat.second_shot_kills`).

A Zat only stuns what it may hurt there: not players in creative mode, nor players where there is no PvP. Its death message is "<player> was shot dead with <shooter>'s zat".

## Settings

The `zat` section of the server's settings:

| Setting | Default | Meaning |
| --- | --- | --- |
| `range` | 30 | How far a shot reaches, in blocks |
| `cooldown_ticks` | 20 | Ticks before it fires again |
| `stun_ticks` | 200 | How long a shot stuns for, and so how long a second shot kills in |
| `stun_damage` | 4 | The harm a stunning shot does |
| `second_shot_kills` | true | Whether a second shot while stunned kills |

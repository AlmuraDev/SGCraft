# Computer Control

With ComputerCraft or OpenComputers, a computer can dial a gate, work its iris and send messages through the
wormhole.

Place the **ComputerCraft Stargate Interface** or **OpenComputers Stargate Interface** **directly below the gate's
base block**. It appears as a peripheral (ComputerCraft) or component (OpenComputers) named `stargate`.

## Methods

Errors come back as `nil, message`.

| Method | Returns / does |
|---|---|
| `stargateState()` | state (`Idle`, `Dialling`, `Opening`, `Connected`, `Closing`, or `Offline` with no gate), number of chevrons engaged, direction (`Outgoing`, `Incoming`, `Not Connected`) |
| `energyAvailable()` | energy the gate can use, in gate units |
| `energyToDial(address)` | energy needed to open a connection to that address, in gate units |
| `zpmEnergy()` | ZPM energy available, and number of ZPMs |
| `zpmDevices()` | a table of ZPM devices: type, x, y, z, energy, zpms, linked |
| `localAddress()` | this gate's address |
| `remoteAddress()` | the connected gate's address |
| `dial(address)` | dials (chevrons never lock-on-dial from a computer) |
| `disconnect()` | closes the connection |
| `irisState()` | `Open`, `Closing`, `Closed`, `Opening`, or `Offline` with no iris |
| `openIris()`, `closeIris()` | work the iris or shield |
| `sendMessage(...)` | sends the arguments to the computer at the other gate (up to 32 arguments) |

`dial` and `energyToDial` look addresses up, and are rate-limited: one a second, at most 20 a minute, then a
10-minute lockout (server settings).

A computer dial isn't made by a player, so the gate's player access list doesn't apply to it.

## Events

| Event | Arguments |
|---|---|
| `sgStargateStateChange` | new state, old state |
| `sgDialOut` / `sgDialIn` | address |
| `sgChevronEngaged` | chevron number, symbol |
| `sgChevronUnset` | |
| `sgIrisStateChange` | new state, old state |
| `sgMessageReceived` | the sender's `sendMessage` arguments |

ComputerCraft events are prefixed with the peripheral's side, as usual.

## OpenComputers networking

- A **Network Card** in the interface's slot relays network messages through an open wormhole to the computers at
  the other end.
- Wireless messages sent near a connected gate are rebroadcast at the other end (strength 50, a server setting).

## Example (OpenComputers)

```lua
local sg = require("component").stargate
print(sg.localAddress())
local ok, err = sg.dial("ABCD-EFG-HI")
if not ok then print(err) end
```

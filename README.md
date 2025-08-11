# Autonomous minecarts

Autonomous minecarts is a mod which allows minecarts to load chunks independently of the player. This allows players to send minecarts over long distances without having to worry about them stopping when they reach the edge of the player loaded area.

The mod works by identifying moving minecarts and adding them to a list of "active" carts. Active carts are each given a chunk ticket which follows the minecart and loads a small number of chunks surrounding it. If a cart stops moving it becomes "inactive" and the chunk ticket is removed after a few minutes.

_**Note:** Dimensions only tick when a player is present or there are force loaded chunks present. If you want this mod to work in dimensions when no players are present, you will need to force load at least one chunk somewhere in that dimension._

## What about performance?

By default, the mod loads only a 3x3 area of entity processing chunks around each active minecart. This means that server load due to this mod should be negligible. 

However, as the mod loads chunks, it may be abused by players to create chunk loaders. For this reason, it's probably best used in single player worlds or on small private servers.

## Configurability

The mod comes sensibly configured out of the box, but can also be customized by users. Configuration can be found under `.minecraft/config/`, or if installed on a dedicated server, then under `<server-root>/config/`.

### Basic config options:

 - `idleTimeoutTicks` - How long minecarts should remain loaded once they become inactive. Defaults to 6000 ticks (5 minutes).
- `idleThreshold` - The threshold velocity in blocks per tick above which a cart will be considered active. Defaults to 0.2 or 4 blocks per second (half the vanilla maximum cart speed).
- `chunkLoadRadius` - How large an area to load surrounding each minecart in chunks. Defaults to 2 for a 3x3 area.
- `ticketDuration` - Duration of chunk tickets created by the mod in ticks. Defaults to 60 (3 seconds).

### Advanced configuration options:

The mod implements a mechanism for preventing carts which are moving but not going anywhere (e.g. are on small circular tracks), from loading chunks indefinitely.

This mechanism works by calculating an exponental moving average of the carts position over time and comparing it to the carts current position. If the cart stays close to its average position, then it will be considered inactive and will eventually be unloaded as if it were stationary, in accordance with `idleTimeoutTicks`.

This behavior is controlled by the following configuration options:

- `positionAverageFactor` - Smoothing factor for the exponential moving average of cart position. Larger values mean a longer average.
- `positionAverageDistance` - Distance threshold for moving cart rejection. This controls the distance in blocks that the cart must be from it's average position to be considered "active".

**WARNING** 

Improper configuration of these last two options may lead to carts remaining loaded in perpetuity, or carts not loading at all. Configure at your own risk!

That's all, enjoy!

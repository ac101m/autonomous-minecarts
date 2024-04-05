# Autonomous Minecarts
A server side fabric mod which loads chunks around moving minecarts, enabling them to pass through unloaded chunks.

## Configuration
Configuration can be found in the server config directory.
- `idleTimeoutTicks` - How long minecarts should remain loaded once they stop moving. Defaults to 6000 (5 minutes).
- `idleThreshold` - The threshold, in blocks per tick, above which a cart will be considered moving. Defaults to 0.2
  (4 blocks per second - half maximum vanilla cart speed).
- `chunkLoadRadius` - How large an area to load surrounding each minecart. Defaults to 2 for a 3x3 area.
- `ticketDuration` - Duration of chunk tickets created by the mod in ticks. Defaults to 60 (3 seconds).
- `positionAverageFactor` - Smoothing factor for moving exponential average of cart position. Higher values reduce the 
  effective duration of the average. This parameter can be tweaked to tune the ignoring of carts which are moving, but 
  not leaving a given area.
- `positionAverageDistance` - Distance from the moving average that a cart must be in order to be considered active. 
  This parameter can be tweaked to tune the ignoring of carts which are moving, but not leaving a given area.

### Warning!
Setting either `positionAverageFactor` or `positionAverageDistance` incorrectly may result in carts not loading at all!
Tweak at your own risk!

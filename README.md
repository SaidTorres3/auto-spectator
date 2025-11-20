# AutoSpectator

A Minecraft Spigot plugin that automatically spectates players and records their perspective. AutoSpectator allows you to set up automatic camera accounts that follow players during gameplay, perfect for creating cinematic recordings without manual camera control.

## Features

- **Automatic Spectating**: Automatically spectate players or cycle through multiple players
- **Configurable Triggers**: Respond to in-game events like player death, damage, or hostile encounters
- **Time-Based Spectating**: Set custom durations for spectating before switching targets
- **Auto Mode**: Automatically cycle through online players at configurable intervals
- **Command-Based Control**: Easily manage spectator settings with in-game commands

## Requirements

Before building and running this plugin, ensure you have the following installed:

### System Requirements

- **Java Development Kit (JDK)**: Version 16 or higher

  - Download from: https://www.oracle.com/java/technologies/downloads/
  - Or use OpenJDK: https://openjdk.org/
- **Apache Maven**: Version 3.6.0 or higher

  - Download from: https://maven.apache.org/download.cgi
  - Installation guide: https://maven.apache.org/install.html
- **Minecraft Server**: Spigot 1.20.4 (or compatible version)

  - Download from: https://getbukkit.org/download/spigot

### Verify Installation

Check if Maven and Java are installed:

```powershell
java -version
mvn --version
```

Both commands should return version information.

## Building the Plugin

### Using the build script (recommended)

```powershell
.\build.ps1
```

### Using Maven directly

```powershell
mvn clean package
```

The compiled plugin JAR file will be generated in:

```
target/auto-spectator-1.0-SNAPSHOT.jar
```

## Installation

1. Build the plugin using the steps above
2. Copy the generated JAR file to your Minecraft server's `plugins` folder
3. Restart the server

## Project Structure

```
auto-spectator/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/autospectator/plugin/
│   │   │       ├── Main.java                  # Main plugin class
│   │   │       ├── AutoSpectateCommand.java   # Command handler
│   │   │       ├── SpectatorManager.java      # Core spectator logic
│   │   │       └── SpectatorListener.java     # Event listener
│   │   └── resources/
│   │       ├── plugin.yml                     # Plugin metadata
│   │       └── config.yml                     # Plugin configuration
│   └── test/
├── pom.xml                            # Maven configuration
├── build.ps1                          # PowerShell build script
└── README.md                          # This file
```

## Usage

### Basic Commands

- `/autospectate` - Toggle auto-spectator mode on/off
- `/autospectate <player>` - Spectate a specific player
- `/autospectate time <seconds>` - Set spectate duration before switching targets
- `/autospectate auto` - Enable auto mode to cycle through players
- `/autospectate perspective <followup|cinematic>` - Set the camera perspective mode. `followup` uses an orbital follow camera; `cinematic` uses offset cinematic cameras; default is `followup`.

### Permissions

- `autospectator.use` - Allows players to use the autospectate command (default: op)

## Configuration

Edit `config.yml` (in `src/main/resources`) to configure triggers and camera behavior. Current default keys:

```yaml
# General timings
spectate-duration: 15                     # Default spectate duration in seconds
spectate-death-duration: 10               # Duration to spectate on player death
non-interruption-in-death-spectation: true # If true, death spectating won't be interrupted by other triggers

# Cinematic camera offsets (used for cinematic, smooth follow)
cinematic:
  distance-min: 6                          # Minimum distance in blocks from the player
  distance-max: 9                          # Maximum distance in blocks from the player
  height-min: -2                           # Minimum height offset relative to player
  height-max: 6                            # Maximum height offset relative to player

# Follow-up (orbital) camera settings
followup:
  distance: 5.0                            # Orbital radius around the player
  hover-height-offset: 3.0                 # Vertical hover offset for the follow camera

triggers:
  damage: true                             # Trigger on player damage
  damage-threshold: 5.0                    # Minimum damage to trigger spectating
  hostile-mob-hit: true                    # Trigger when hitting hostile mobs
  fall-damage-prediction: true             # Trigger on predicted fall damage
  fall-distance-threshold: 5.0             # Minimum fall distance to trigger
```

## Development

The main plugin class is located at:

```
src/main/java/com/autospectator/plugin/Main.java
```

Key classes:

- `Main.java` - Plugin entry point and initialization
- `AutoSpectateCommand.java` - Command handler for `/autospectate` command
- `SpectatorManager.java` - Core logic for managing spectator sessions
- `SpectatorListener.java` - Event listener for in-game triggers

## License

MIT

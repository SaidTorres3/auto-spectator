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
│   │   │       ├── SpectatorListener.java     # Event listener
│   │   │       └── SpectatorSession.java      # Session management
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

### Permissions

- `autospectator.use` - Allows players to use the autospectate command (default: op)

## Configuration

Edit `config.yml` to configure triggers and settings:

```yaml
spectate-duration: 15                   # Default spectate duration in seconds

triggers:
  damage: true                          # Trigger on player damage
  damage-threshold: 5.0                 # Minimum damage to trigger spectating
  hostile-mob-hit: true                 # Trigger when hitting hostile mobs
  fall-damage-prediction: true          # Trigger on predicted fall damage
  fall-distance-threshold: 5.0          # Minimum fall distance to trigger
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
- `SpectatorSession.java` - Manages individual spectator sessions

## License

MIT

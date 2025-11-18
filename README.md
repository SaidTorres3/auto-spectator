# AutoSpectator

An auto-spectate Minecraft Spigot plugin for automatically spectate players in the background without using a finger.

## Requirements

Before building and running this plugin, ensure you have the following installed:

### System Requirements

- **Java Development Kit (JDK)**: Version 8 or higher

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
│   │   │   └── com/example/plugin/
│   │   │       └── Main.java          # Main plugin class
│   │   └── resources/
│   │       └── plugin.yml             # Plugin configuration
│   └── test/
├── pom.xml                            # Maven configuration
├── build.ps1                          # PowerShell build script
└── README.md                          # This file
```

## Configuration

Edit `src/main/resources/plugin.yml` to customize:

- Plugin name
- Version
- Description
- Commands and permissions

## Development

The main plugin class is located at:

```
src/main/java/com/example/plugin/Main.java
```

This class extends `JavaPlugin` and provides:

- `onEnable()`: Called when the plugin is loaded
- `onDisable()`: Called when the plugin is unloaded

## License

MIT

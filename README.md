# What is Anvil Map Editor?
Anvil Map Editor is a Paper plugin you can use to edit your Anvil maps and convert them to the [Polar](https://github.com/hollow-cube/polar) world format. **This plugin is meant to be run on a standalone Paper server.** The Polar world format is generally more efficient than Anvil worlds when loading and using them in a [Minestom](https://github.com/Minestom/Minestom) server. Additionally, this tool allows you to add miscellaneous nbt data that can be represented in a physical/non-physical form via block displays as "markers" for where these points are in your world while viewing them on the Paper server. All nbt data is stored in one simple file, allowing you to easily get the data via a plugin or on your Minestom server.
**This Plugin is is designed and tested for `1.20.4` for Minestom and Paper**

# Usage
To use this plugin, you have two routes you can go:

## 1. Download the [server setup](https://github.com/hapily04/AnvilMapEditor/releases/download/2.0/AnvilMapEditorSetup.zip)
This option is the easiest as everything is setup for you & the config files are optimized. Additionally, a sample map is provided for you to mess around with.

## 2. Download the [paper plugin](https://github.com/hapily04/AnvilMapEditor/releases/download/2.0/AnvilMapEditor-2.0.jar)
This option is more difficult as it requires you to do a few steps of your own to get everthing working.
- A world named `ame_lobby` is required as it's the spawn world and lobby world of the server.
- You need to configure your config files for optimization

## Server is setup, now what?
Once your server is properly setup and configured, you need to start it. Once it's started, you may notice two new folders pop up in the server directory:
- `anvil_input` - The folder in which you drag and drop your regular worlds to use as inputs.
- `polar_output` - The folder in which polar worlds will be output to should you decide to use the polar conversion tool.
Additionally, once you begin editing a world, you may see a few new items in your inventory.
To save the world, right-click the book.
To save and convert the world to the Polar world format, right click the commandblock.
To exit your current edit session, right click the structure void.

### Commands
- `/edit <map>` - Begins the editing process for the map provided. The map argument should match a world in the `anvil_input` folder.
- `/data (create/add/set) <key> <nbtdata>` - Creates miscellaneous data without a physical point. Overrides data if present.
- `/data physical <location> <block> <size> <key> <nbtdata>` - Creates a physical data point entity.
- `/data delete [<key>]` - Deletes data of the provided key. If no key is provided, it attempts to find the physical data point that you're looking at.
- `/data get [<key>]` - Prints the nbt data of the provided key. If no key is provided, it prints all of the data.

# Dependency Overview
- `io.papermc.paper:paper-api` - Pretty self explanatory; necessary for a plugin.
- `io.papermc.paperweight.userdev (nms)` - Needed to convert the given nbt data in the arguments. Also useful to print the nbt colorfully with the `/data get` command.
- `dev.jorel:commandapi-bukkit-shade` - Helpful to make commands & much better than what the bukkit api provides
- `dev.hollowcube:polar` - Necessary for Polar conversion
- `me.nullicorn:Nedit` - The NBT library used to read and write the miscellaneous data.
- `io.github.jglrxavpok.hephaistos` - Required for Polar to work
- `org.jetbrains.kotlin:kotlin-stdlib` - Required for Hephaistos to work

<details><summary>Why are there Minestom packages and classes?</summary>
Polar requires them to work, and I don't want to import all of Minestom.
</detauls>

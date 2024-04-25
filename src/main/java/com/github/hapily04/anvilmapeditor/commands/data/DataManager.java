package com.github.hapily04.anvilmapeditor.commands.data;

import com.github.hapily04.anvilmapeditor.util.FileUtils;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.NBTWriter;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


public class DataManager implements Listener {

	public static final String DATA_FILE_NAME = "data.dat";
	private static final String METADATA_KEY = "NBT-Key";
	public static final String NBT_ENTITIES_KEY = "PhysicalEntities";
	public static final String NBT_DATA_KEY = "Data";
	private static final Vector3f ZEROED_VECTOR = new Vector3f(0, 0, 0);
	private static final AxisAngle4f DEFAULT_AXISANGLE = new AxisAngle4f();

	private final Map<World, DataTuple> worldDataMap = new HashMap<>();

	private final Plugin plugin;

	public DataManager(Plugin plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("CallToPrintStackTrace")
	public void saveExternalData(World world, boolean removeEntities, boolean async) {
		DataTuple tuple = worldDataMap.get(world);
		File outputDataFile = FileUtils.defendFile(tuple.getDataFile()); // defending it in-case they deleted it prior
		if (async) {
			BukkitScheduler scheduler = Bukkit.getScheduler();
			scheduler.runTaskAsynchronously(plugin, () -> {
				try {
					NBTWriter.writeToFile(tuple.getData(), outputDataFile);
					if (removeEntities) {
						scheduler.runTask(plugin, () -> {
							clearEntities(world);
							worldDataMap.remove(world);
						});
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} else {
			try {
				NBTWriter.writeToFile(tuple.getData(), outputDataFile);
				if (removeEntities) {
					clearEntities(world);
					worldDataMap.remove(world);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void saveExternalData(World world, boolean removeEntities) {
		saveExternalData(world, removeEntities, false);
	}

	public void loadExternalData(World world, File worldFolder) {
		if (worldDataMap.containsKey(world)) return;
		File dataFile = FileUtils.defendFile(new File(worldFolder, DATA_FILE_NAME));
		NBTCompound nbtCompound = new NBTCompound();
		if (dataFile.length() != 0) {
			try {
				nbtCompound = NBTReader.readFile(dataFile);
			} catch (IOException ignored) {}
		}
		worldDataMap.put(world, new DataTuple(dataFile, nbtCompound));
		loadEntities(world);
	}

	public void addData(World world, String key, NBTCompound data) {
		NBTList dataList = getOrCreateDataList(world, NBT_DATA_KEY);
		boolean replaced = false;
		for	(Object o : dataList) {
			NBTCompound nbtCompound = (NBTCompound) o;
			if (nbtCompound.containsKey(key)) {
				nbtCompound.put(key, data); // replace
				replaced = true;
			}
		}
		if (!replaced) {
			NBTCompound keyedDataCompound = new NBTCompound();
			keyedDataCompound.put(key, data);
			dataList.add(keyedDataCompound);
		}
	}

	public void deleteData(World world, String key) {
		NBTList dataList = getOrCreateDataList(world, NBT_DATA_KEY);
		NBTList copy = new NBTList(TagType.COMPOUND);
		copy.addAll(dataList);
		for (Object o : copy) {
			NBTCompound nbtCompound = (NBTCompound) o;
			if (nbtCompound.containsKey(key)) {
				dataList.remove(nbtCompound);
				return;
			}
		}
	}

	public NBTCompound getData(World world) {
		//if (!worldDataMap.containsKey(world)) return null;
		return worldDataMap.get(world).getData();
	}

	public boolean hasData(World world, String key) {
		if (!worldDataMap.containsKey(world)) return false;
		if (key == null || key.isBlank()) return false;
		NBTList dataList = getOrCreateDataList(world, NBT_DATA_KEY);
		for (Object o : dataList) {
			NBTCompound nbtCompound = (NBTCompound) o;
			if (nbtCompound.containsKey(key)) {
				return true;
			}
		}
		return false;
	}

	public void clearEntities(World world) {
		for (Entity entity : world.getEntities()) {
			if (entity.getType() == EntityType.PLAYER) continue;
			if (entity.hasMetadata(METADATA_KEY)) entity.remove();
		}
	}

	public void loadEntities(World world) {
		NBTCompound nbtCompound = worldDataMap.get(world).getData();
		if (nbtCompound.containsKey(NBT_ENTITIES_KEY)) {
			NBTList entitiesList = nbtCompound.getList(NBT_ENTITIES_KEY);
			assert entitiesList != null; // shouldn't be null because of our check for the key
			for (Object o : entitiesList) {
				NBTCompound entity = (NBTCompound) o;
				float size = entity.getFloat("Size", 1);
				float adjustBy = (float) (0.5*size);
				Location loc = new Location(world, entity.getFloat("x", 0)-adjustBy,
						entity.getFloat("y", 0), entity.getFloat("z", 0)-adjustBy);
				Material material = Material.valueOf(entity.getString("Material"));
				String key = entity.getString("Key");
				BlockDisplay blockDisplay = world.spawn(loc, BlockDisplay.class);
				blockDisplay.setBlock(material.createBlockData());
				Vector3f scale = new Vector3f(size, size, size);
				Transformation transformation = new Transformation(ZEROED_VECTOR, DEFAULT_AXISANGLE, scale, DEFAULT_AXISANGLE);
				blockDisplay.setTransformation(transformation);
				blockDisplay.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
				Location interactionSpawn = loc.add(adjustBy, 0, adjustBy); // didn't align correctly before with the block display
				Interaction interaction = world.spawn(interactionSpawn, Interaction.class);
				interaction.setInteractionWidth(size);
				interaction.setInteractionHeight(size);
				interaction.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
				interaction.setMetadata("Key", new FixedMetadataValue(plugin, key));
			}
		}
	}

	void addEntityData(World world, NBTCompound entityNBT) {
		NBTList entityDataList = getOrCreateDataList(world, NBT_ENTITIES_KEY);
		if (!entityDataList.isEmpty()) {
			NBTList copy = new NBTList(TagType.COMPOUND);
			copy.addAll(entityDataList);
			for (Object o : copy) {
				NBTCompound entityCompound = (NBTCompound) o;
				if (!entityCompound.getString("Key").equalsIgnoreCase(entityNBT.getString("Key"))) continue;
				entityDataList.remove(entityCompound);
			}
		}
		entityDataList.add(entityNBT);
	}

	void removeEntityData(World world, String key) {
		DataTuple dataTuple = worldDataMap.get(world);
		NBTCompound baseCompound = dataTuple.getData();
		if (!baseCompound.containsKey(NBT_ENTITIES_KEY)) return; // no entities nothing to worry about
		NBTList entityDataList = getOrCreateDataList(world, NBT_ENTITIES_KEY);
		NBTList copy = new NBTList(TagType.COMPOUND);
		copy.addAll(entityDataList);
		for (Object o : copy) {
			NBTCompound entityCompound = (NBTCompound) o;
			if (entityCompound.getString("Key").equalsIgnoreCase(key)) {
				entityDataList.remove(entityCompound);
				return;
			}
		}
	}

	public File getDataFile(World world) {
		return worldDataMap.get(world).getDataFile();
	}

	public Set<World> getWorlds() {
		return Collections.unmodifiableSet(worldDataMap.keySet());
	}

	NBTList getOrCreateDataList(World world, String key) {
		NBTCompound baseCompound = worldDataMap.get(world).getData();
		return (NBTList) getOrPutIfAbsent(baseCompound, key, unused -> new NBTList(TagType.COMPOUND));
	}

	@SuppressWarnings("SameParameterValue")
	private Object getOrPutIfAbsent(NBTCompound compound, String key, Function<Void, Object> function) {
		if (compound.containsKey(key)) return compound.get(key);
		Object o = function.apply(null);
		compound.put(key, o);
		return o;
	}

	public static class DataTuple {

		private final File dataFile;
		private final NBTCompound data;

		private DataTuple(File dataFile, NBTCompound data) {
			this.dataFile = dataFile;
			this.data = data;
		}

		public File getDataFile() {
			return dataFile;
		}

		public NBTCompound getData() {
			return data;
		}

	}

}

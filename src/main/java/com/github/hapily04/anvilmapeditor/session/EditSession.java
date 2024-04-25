package com.github.hapily04.anvilmapeditor.session;

import org.bukkit.World;

import java.io.File;
import java.util.UUID;

record EditSession(UUID uuid, World editingWorld, File worldFolder) {


}

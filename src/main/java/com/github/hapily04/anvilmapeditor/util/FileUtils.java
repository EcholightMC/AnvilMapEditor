package com.github.hapily04.anvilmapeditor.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileUtils {

	/**
	 * Ensures that the provided file and its directories are created.
	 *
	 * @param file the file to defend
	 * @return the file being defended (ignorable)
	 */
	public static File defendFile(File file) {
		return defendFile(file, false);
	}

	/**
	 * Ensures that the provided file and its directories are created.
	 *
	 * @param file the file to defend
	 * @param directory is the file provided intended to be a directory?
	 * @return the file being defended (ignorable)
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static File defendFile(File file, boolean directory) {
		try {
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			if (directory) file.mkdir();
			else file.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return file;
	}

	@SuppressWarnings({"unchecked", "SameParameterValue"})
	public static <T, R extends T> Class<R>[] getClasses(String pkg, Class<T> clazz, File jar) throws IOException, ClassNotFoundException {
		List<Class<? extends T>> classes = new ArrayList<>();
		pkg = pkg.replace('.', '/');
		try (JarFile file = new JarFile(jar)) {
			for (Enumeration<JarEntry> enu = file.entries(); enu.hasMoreElements(); ) {
				JarEntry jarEntry = enu.nextElement();
				String path = jarEntry.getName();
				if (!jarEntry.isDirectory() && path.startsWith(pkg) && path.endsWith(".class")) {
					path = path.substring(0, path.length() - 6); // 6 is the length of the ".class" string
					path = path.replace('/', '.');
					Class<?> clz = Class.forName(path);
					if (clz.equals(clazz)) continue;
					if (clazz.isAssignableFrom(clz)) {
						classes.add((Class<R>) clz);
					}
				}
			}
		}
		return (Class<R>[]) classes.toArray(new Class<?>[0]);
	}

}
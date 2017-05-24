package com.github.zachdeibert.mavendependencyruntime;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class that scans through the classpath for certain files
 * 
 * @author Zach Deibert
 * @since 1.0.0
 */
public class ClassPathScanner {
	/**
	 * Finds a list of resources that are in a specified folder in the classpath
	 * 
	 * @param dir
	 *            The folder to search in
	 * @param cl
	 *            The {@link ClassLoader} to use the classpath of
	 * @return An array of the resources that were found
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static String[] listResources(String dir, ClassLoader cl) throws IOException {
		URL dirUrl = cl.getResource(dir);
		if (dirUrl.getProtocol().equals("jar")) {
			List<String> resources = new ArrayList<String>();
			String jarFile = URLDecoder.decode(dirUrl.getFile(), "UTF-8");
			JarFile jar = new JarFile(jarFile.substring(5, jarFile.indexOf("!")));
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				String file = entries.nextElement().getName();
				if (file.startsWith(dir) && file.length() > dir.length() + 5) {
					file = file.substring(dir.length() + 1, file.length() - (file.endsWith("/") ? 1 : 0));
					if (!file.contains("/")) {
						resources.add(file);
					}
				}
			}
			jar.close();
			return resources.toArray(new String[0]);
		} else {
			try {
				return new File(new URI(dirUrl.toString()).getPath()).list();
			} catch (URISyntaxException ex) {
				throw new IOException(ex);
			}
		}
	}

	/**
	 * Finds a list of resources that are in a specified folder in the system
	 * classpath
	 * 
	 * @param dir
	 *            The folder to search in
	 * @return An array of the resources that were found
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static String[] listSystemResources(String dir) throws IOException {
		return listResources(dir, ClassLoader.getSystemClassLoader());
	}
}

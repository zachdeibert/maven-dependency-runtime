package com.github.zachdeibert.mavendependencyruntime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * A repository that has some overrides for common artifacts that cannot be
 * found in the Maven repositories
 * 
 * @author Zach Deibert
 * @since 1.0.0
 */
final class CommonOverrides extends Repository {
	/**
	 * A list of the artifact names that can be overridden (sorted
	 * alphabetically)
	 * 
	 * @see CommonOverrides#OVERRIDDEN_URLS
	 * @since 1.0.0
	 */
	private static String[] DEPENDENCY_OVERRIDES = null;
	/**
	 * A list of the urls from which to download the artifacts that this
	 * repository resolves (sorted in the same order as the
	 * {@link CommonOverrides#DEPENDENCY_OVERRIDES}
	 * 
	 * @see CommonOverrides#DEPENDENCY_OVERRIDES
	 * @since 1.0.0
	 */
	private static String[] OVERRIDDEN_URLS = null;

	/**
	 * Reads a file and returns all of the lines of text contained in that file
	 * 
	 * @param resource
	 *            The name of the file resource, relative to this class
	 * @return The array of lines in the file
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	private static String[] load(String resource) throws IOException {
		InputStream stream = CommonOverrides.class.getResourceAsStream(resource);
		Scanner scan = new Scanner(stream);
		List<String> lines = new ArrayList<String>();
		while (scan.hasNext()) {
			lines.add(scan.nextLine());
		}
		scan.close();
		stream.close();
		return lines.toArray(new String[0]);
	}

	/**
	 * Loads the arrays from the files in the classpath
	 * 
	 * @see CommonOverrides#DEPENDENCY_OVERRIDES
	 * @see CommonOverrides#OVERRIDDEN_URLS
	 * @since 1.0.0
	 * @throws IOException
	 */
	private static void init() throws IOException {
		if (DEPENDENCY_OVERRIDES == null) {
			DEPENDENCY_OVERRIDES = load("overrides.deps");
		}
		if (OVERRIDDEN_URLS == null) {
			OVERRIDDEN_URLS = load("overrides.urls");
		}
	}

	@Override
	public String getUrl() {
		return null;
	}

	@Override
	public void download(Dependency dep, File out) throws IOException {
		if (out.getName().endsWith(".jar")) {
			init();
			int index = Arrays.binarySearch(DEPENDENCY_OVERRIDES, dep.toString());
			if (index < 0) {
				throw new IOException("Unable to find override");
			} else {
				URL url = new URL(OVERRIDDEN_URLS[index]);
				InputStream ins = url.openStream();
				OutputStream outs = new FileOutputStream(out);
				byte[] buffer = new byte[4096];
				for (int len; (len = ins.read(buffer)) > 0; outs.write(buffer, 0, len))
					;
				outs.close();
				ins.close();
			}
		}
	}

	@Override
	public void setVersion(Dependency dep) throws IOException {
		throw new IOException("Not supported by this repository");
	}
}

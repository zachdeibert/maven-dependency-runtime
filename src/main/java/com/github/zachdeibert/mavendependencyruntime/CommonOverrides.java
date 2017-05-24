package com.github.zachdeibert.mavendependencyruntime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;

final class CommonOverrides extends Repository {
	private static final String[] DEPENDENCY_OVERRIDES = new String[] { "org.skife.kasparov:csv:1.0" };
	private static final String[] OVERRIDDEN_URLS = new String[] { "http://kasparov.skife.org/csv/csv-1.0.jar" };

	@Override
	public String getUrl() {
		return null;
	}

	@Override
	public void download(Dependency dep, File out) throws IOException {
		if (out.getName().endsWith(".jar")) {
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

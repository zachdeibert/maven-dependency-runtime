package com.github.zachdeibert.mavendependencyruntime;

import java.io.File;
import java.text.ParseException;

import org.w3c.dom.Element;

public class Dependency extends AbstractXmlParser {
	private static final String LATEST_VERSION = "latest";
	private final String groupId;
	private final String artifactId;
	private String version;
	private final DependencyScope scope;

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version == LATEST_VERSION ? null : version;
	}

	public void setVersion(String version) {
		if (this.version != LATEST_VERSION) {
			throw new IllegalStateException("Version is already resolved");
		} else if (version == LATEST_VERSION) {
			throw new IllegalArgumentException("Cannot set version to the latest");
		} else {
			this.version = version;
		}
	}

	public Version[] getInstalledVersions(File dir) {
		for (String part : getGroupId().split("\\.")) {
			dir = new File(dir, part);
		}
		dir = new File(dir, getArtifactId());
		String[] strs = dir.list();
		Version[] vers = new Version[strs.length];
		for (int i = 0; i < strs.length; ++i) {
			vers[i] = new Version(strs[i]);
		}
		return vers;
	}

	public DependencyScope getScope() {
		return scope;
	}

	public File getFile(File dir, String ext) {
		for (String part : getGroupId().split("\\.")) {
			dir = new File(dir, part);
		}
		dir = new File(dir, getArtifactId());
		dir = new File(dir, getVersion());
		dir = new File(dir, String.format("%s-%s.%s", getArtifactId(), getVersion(), ext));
		return dir;
	}

	@Override
	public String toString() {
		return String.format("%s:%s:%s", groupId, artifactId, version);
	}

	public Dependency(String groupId, String artifactId, String version, DependencyScope scope) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version.contains("$") ? LATEST_VERSION : version; // TODO
																			// Make
																			// it
																			// support
																			// variables
		this.scope = scope;
	}

	public Dependency(Element node) throws ParseException {
		this(find("groupId", node, null), find("artifactId", node, null), find("version", node, LATEST_VERSION),
				DependencyScope.valueOf(find("scope", node, "compile").toUpperCase()));
	}
}

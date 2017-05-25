package com.github.zachdeibert.mavendependencyruntime;

import java.io.File;
import java.text.ParseException;

import org.w3c.dom.Element;

/**
 * Represents a dependency that needs to be downloaded and injected into the
 * classpath
 * 
 * @author Zach Deibert
 * @since 1.0.0
 */
public class Dependency extends AbstractXmlParser {
	/**
	 * A placeholder string for when the version has not been specified
	 * 
	 * @since 1.0.0
	 */
	private static final String LATEST_VERSION = "latest";
	/**
	 * The ID of the group for this dependency
	 * 
	 * @since 1.0.0
	 */
	private final String groupId;
	/**
	 * The ID of the artifact for this dependency
	 * 
	 * @since 1.0.0
	 */
	private final String artifactId;
	/**
	 * The version of the artifact to download, or
	 * {@link Dependency#LATEST_VERSION} if it is not specified in the pom
	 * 
	 * @since 1.0.0
	 */
	private String version;
	/**
	 * The scope of the dependency
	 * 
	 * @since 1.0.0
	 */
	private final DependencyScope scope;

	/**
	 * Gets the ID of the group for this dependency
	 * 
	 * @return The group ID
	 * @since 1.0.0
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Gets the ID of the artifact for this dependency
	 * 
	 * @return The artifact ID
	 * @since 1.0.0
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Gets the version of the artifact to download
	 * 
	 * @return The version, or <code>null</code> if the latest version should be
	 *         downloaded and the latest version has not been determined yet
	 * @since 1.0.0
	 */
	public String getVersion() {
		return version == LATEST_VERSION ? null : version;
	}

	/**
	 * Sets the latest version of this dependency
	 * 
	 * @param version
	 *            The latest version
	 * @since 1.0.0
	 * @throws IllegalStateException
	 *             If this dependency has a specific version already
	 */
	public void setVersion(String version) {
		if (this.version != LATEST_VERSION) {
			throw new IllegalStateException("Version is already resolved");
		} else if (version == LATEST_VERSION) {
			throw new IllegalArgumentException("Cannot set version to the latest");
		} else {
			this.version = version;
		}
	}

	/**
	 * Gets a list of all of the versions of this artifact that are currently
	 * downloaded on this computer
	 * 
	 * @param dir
	 *            The directory to store downloaded artifacts in
	 * @return An array of the versions that are already downloaded
	 * @since 1.0.0
	 */
	public Version[] getInstalledVersions(File dir) {
		for (String part : getGroupId().split("\\.")) {
			dir = new File(dir, part);
		}
		dir = new File(dir, getArtifactId());
		String[] strs = dir.list();
		if (strs == null) {
			return new Version[0];
		}
		Version[] vers = new Version[strs.length];
		for (int i = 0; i < strs.length; ++i) {
			vers[i] = new Version(strs[i]);
		}
		return vers;
	}

	/**
	 * Gets the scope of this dependency
	 * 
	 * @return The scope
	 * @since 1.0.0
	 */
	public DependencyScope getScope() {
		return scope;
	}

	/**
	 * Gets the file that the downloaded artifact should be stored in
	 * 
	 * @param dir
	 *            The directory to store downloaded artifacts in
	 * @param ext
	 *            The file extension to download (should be either
	 *            <code>"jar"</code> or <code>"pom"</code>
	 * @return The file to download into
	 * @since 1.0.0
	 */
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Dependency)) {
			return false;
		}
		Dependency other = (Dependency) obj;
		if (groupId == null) {
			if (other.groupId != null) {
				return false;
			}
		} else if (!groupId.equals(other.groupId)) {
			return false;
		}
		if (artifactId == null) {
			if (other.artifactId != null) {
				return false;
			}
		} else if (!artifactId.equals(other.artifactId)) {
			return false;
		}
		if (version == LATEST_VERSION || other.version == LATEST_VERSION) {
		} else if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
			return false;
		}
		return true;
	}

	/**
	 * Creates a new dependency
	 * 
	 * @param groupId
	 *            The group ID
	 * @param artifactId
	 *            The artifact ID
	 * @param version
	 *            The version to download
	 * @param scope
	 *            The scope
	 * @since 1.0.0
	 */
	public Dependency(String groupId, String artifactId, String version, DependencyScope scope) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version.contains("$") || version.contains("[") || version.contains("(") ? LATEST_VERSION
				: version; // TODO Make if support variables and ranges
		this.scope = scope;
	}

	/**
	 * Creates a new dependency from the specified element in the pom
	 * 
	 * @param node
	 *            The element to create the dependency from
	 * @since 1.0.0
	 * @throws ParseException
	 *             If the xml could not be parsed
	 */
	public Dependency(Element node) throws ParseException {
		this(find("groupId", node, null), find("artifactId", node, null), find("version", node, LATEST_VERSION),
				DependencyScope.valueOf(find("scope", node, "compile").toUpperCase()));
	}
}

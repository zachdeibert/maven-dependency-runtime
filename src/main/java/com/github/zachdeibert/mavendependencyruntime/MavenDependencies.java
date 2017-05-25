package com.github.zachdeibert.mavendependencyruntime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The class that contains all of the methods needed for downloading and
 * injecting dependencies into the classpath.
 * 
 * @author Zach Deibert
 * @since 1.0.0
 */
public abstract class MavenDependencies extends AbstractXmlParser {
	/**
	 * The directory to download and store artifacts in
	 * 
	 * @since 1.0.0
	 */
	private static final File BASE_DIR = new File(new File(System.getProperty("user.home"), ".runtime-deps"), "maven");
	/**
	 * The scopes to download dependencies for by default
	 * 
	 * @since 1.0.0
	 */
	private static final DependencyScope[] DEFAULT_SCOPES = { DependencyScope.COMPILE, DependencyScope.RUNTIME };
	/**
	 * If debugging information should be logged to {@link System#out}
	 * 
	 * @since 1.0.0
	 */
	private static final boolean ENABLE_LOGGING = "true"
			.equals(System.getProperty("com.github.zachdeibert.mavendependencyruntime.logging"));
	/**
	 * A set of all of the dependencies that have already been injected into the
	 * classpath, so they should not be reinjected (to prevent cyclic
	 * dependencies from freezing the code in a loop)
	 * 
	 * @since 1.0.0
	 */
	private static final Set<Dependency> INJECTED_DEPENDENCIES = new HashSet<Dependency>();
	/**
	 * The current indentation for debug logging
	 * 
	 * @since 1.0.0
	 */
	private static String indent = "";

	static {
		INJECTED_DEPENDENCIES.add(new Dependency("com.github.zachdeibert", "maven-dependency-runtime", "1.0.0-SNAPSHOT",
				DependencyScope.PROVIDED));
	}

	/**
	 * Makes sure that the {@link MavenDependencies#BASE_DIR} exists
	 * 
	 * @since 1.0.0
	 */
	private static void createBaseDir() {
		BASE_DIR.mkdirs();
		// TODO make it hidden
	}

	/**
	 * Injects a set of dependencies into the classpath
	 * 
	 * @param dependencies
	 *            The dependencies to inject
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static void injectClasspath(Set<Dependency> dependencies) throws IOException {
		ClassLoader genericLoader = ClassLoader.getSystemClassLoader();
		if (genericLoader instanceof URLClassLoader) {
			try {
				URLClassLoader urlLoader = (URLClassLoader) genericLoader;
				Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				addUrl.setAccessible(true);
				for (Dependency dep : dependencies) {
					File file = dep.getFile(BASE_DIR, "jar");
					if (file.exists()) {
						addUrl.invoke(urlLoader, file.toURI().toURL());
					}
				}
			} catch (ReflectiveOperationException ex) {
				throw new IOException("Unable to inject the classpath", ex);
			}
		} else {
			throw new IOException("Unable to inject the classpath");
		}
	}

	/**
	 * Downloads a dependency along with all of its dependencies and stores them
	 * in the {@link MavenDependencies#BASE_DIR}.
	 * 
	 * @param repositories
	 *            The list of repositories to try to download from
	 * @param dependency
	 *            The dependency to download
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(List<Repository> repositories, Dependency dependency) throws IOException {
		if (INJECTED_DEPENDENCIES.contains(dependency)) {
			return new HashSet<Dependency>();
		}
		if (ENABLE_LOGGING) {
			System.out.printf("%sResolving dependency %s\n", indent, dependency);
			indent = indent.concat(" ");
		}
		try {
			if (dependency.getVersion() == null) {
				IOException e = null;
				for (Repository repo : repositories) {
					try {
						repo.setVersion(dependency);
						e = null;
						break;
					} catch (IOException ex) {
						if (e == null) {
							e = new IOException(String.format("Unable to find latest version of %s", dependency));
						}
						e.addSuppressed(ex);
					}
				}
				if (e != null) {
					Version max = null;
					for (Version ver : dependency.getInstalledVersions(BASE_DIR)) {
						if (max == null || ver.compareTo(max) > 0) {
							max = ver;
						}
					}
					if (max == null) {
						throw e;
					} else {
						dependency.setVersion(max.toString());
					}
				}
			}
			File pom = dependency.getFile(BASE_DIR, "pom");
			File jar = dependency.getFile(BASE_DIR, "jar");
			Set<Dependency> downloaded = new HashSet<Dependency>();
			downloaded.add(dependency);
			if (jar.exists()) {
				INJECTED_DEPENDENCIES.add(dependency);
				if (pom.exists()) {
					downloaded.addAll(download(pom.toURI().toURL()));
				}
				return downloaded;
			}
			pom.getParentFile().mkdirs();
			IOException e = null;
			for (Repository repo : repositories) {
				try {
					repo.download(dependency, pom);
					try {
						repo.download(dependency, jar);
					} catch (IOException exception) {
						try {
							DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
							DocumentBuilder builder = factory.newDocumentBuilder();
							Document xml = builder.parse(pom);
							try {
								if (find("packaging", xml.getDocumentElement(), "pom").equals("jar")) {
									throw exception;
								}
							} catch (ParseException ex) {
								ex.addSuppressed(exception);
								throw new IOException("Unable to find packaging information in pom.xml", ex);
							}
						} catch (ParserConfigurationException ex) {
							ex.addSuppressed(exception);
							throw new IOException("Unable to load pom.xml parser", ex);
						} catch (SAXException ex) {
							ex.addSuppressed(exception);
							throw new IOException("Unable to parse pom.xml", ex);
						} catch (IOException ex) {
							if (ex != exception) {
								ex.addSuppressed(exception);
							}
							throw ex;
						}
					}
					if (pom.exists()) {
						downloaded.addAll(download(pom.toURI().toURL()));
					}
					e = null;
					break;
				} catch (IOException ex) {
					if (e == null) {
						e = new IOException(String.format("Unable to find download for %s", dependency));
					}
					e.addSuppressed(ex);
				}
			}
			if (e != null) {
				throw e;
			}
			return downloaded;
		} finally {
			if (ENABLE_LOGGING) {
				indent = indent.substring(1);
			}
		}
	}

	/**
	 * Downloads a list of dependencies along with all of their dependencies and
	 * stores them in the {@link MavenDependencies#BASE_DIR}.
	 * 
	 * @param repositories
	 *            The list of repositories to try to download from
	 * @param dependencies
	 *            The list of dependencies to download
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(List<Repository> repositories, List<Dependency> dependencies)
			throws IOException {
		createBaseDir();
		Set<Dependency> downloaded = new HashSet<Dependency>();
		for (Dependency dep : dependencies) {
			downloaded.addAll(download(repositories, dep));
		}
		injectClasspath(downloaded);
		return downloaded;
	}

	/**
	 * Downloads all of the dependencies specified in the pom
	 * 
	 * @param pom
	 *            The parsed pom file
	 * @param scopes
	 *            The scopes to download for
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(Document pom, DependencyScope... scopes) throws IOException {
		List<Repository> repos = new ArrayList<Repository>();
		List<Dependency> deps = new ArrayList<Dependency>();
		Set<DependencyScope> scopeSet = new HashSet<DependencyScope>();
		scopeSet.addAll(Arrays.asList(scopes));
		NodeList nodes = pom.getDocumentElement().getChildNodes();
		repos.add(new Repository());
		try {
			for (int i = 0; i < nodes.getLength(); ++i) {
				Node node = nodes.item(i);
				if (node.getNodeName() == "repositories") {
					nodes = ((Element) node).getElementsByTagName("repository");
					for (i = 0; i < nodes.getLength(); ++i) {
						Element e = (Element) nodes.item(i);
						repos.add(new Repository(e));
					}
					break;
				}
			}
		} catch (ParseException ex) {
			throw new IOException("Unable to parse repositories", ex);
		}
		try {
			InputStream stream = MavenDependencies.class.getResourceAsStream("overrides.repos");
			Scanner scan = new Scanner(stream);
			while (scan.hasNext()) {
				repos.add(new Repository(scan.nextLine()));
			}
			scan.close();
			stream.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		repos.add(new CommonOverrides());
		nodes = pom.getElementsByTagName("dependency");
		try {
			for (int i = 0; i < nodes.getLength(); ++i) {
				Dependency dep = new Dependency((Element) nodes.item(i));
				if (scopeSet.contains(dep.getScope())) {
					deps.add(dep);
				}
			}
		} catch (ParseException ex) {
			throw new IOException("Unable to parse dependencies", ex);
		}
		return download(repos, deps);
	}

	/**
	 * Downloads all of the dependencies specified in the pom for the default
	 * scopes
	 * 
	 * @param pom
	 *            The parsed pom file
	 * @return The set of all dependencies that were downloaded
	 * @see MavenDependencies#DEFAULT_SCOPES
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(Document pom) throws IOException {
		return download(pom, DEFAULT_SCOPES);
	}

	/**
	 * Downloads all of the dependencies specified in the pom
	 * 
	 * @param pom
	 *            The stream containing the pom file
	 * @param scopes
	 *            The scopes to download for
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(InputStream pom, DependencyScope... scopes) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document xml = builder.parse(pom);
			return download(xml, scopes);
		} catch (ParserConfigurationException ex) {
			throw new IOException("Unable to load pom.xml parser", ex);
		} catch (SAXException ex) {
			throw new IOException("Unable to parse pom.xml", ex);
		}
	}

	/**
	 * Downloads all of the dependencies specified in the pom for the default
	 * scopes
	 * 
	 * @param pom
	 *            The stream containing the pom file
	 * @return The set of all dependencies that were downloaded
	 * @see MavenDependencies#DEFAULT_SCOPES
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(InputStream pom) throws IOException {
		return download(pom, DEFAULT_SCOPES);
	}

	/**
	 * Downloads all of the dependencies specified in the pom
	 * 
	 * @param pomPath
	 *            The url to the pom
	 * @param scopes
	 *            The scopes to download for
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(URL pomPath, DependencyScope... scopes) throws IOException {
		InputStream pom = pomPath.openStream();
		Set<Dependency> downloaded = download(pom, scopes);
		pom.close();
		return downloaded;
	}

	/**
	 * Downloads all of the dependencies specified in the pom for the default
	 * scopes
	 * 
	 * @param pomPath
	 *            The url to the pom
	 * @return The set of all dependencies that were downloaded
	 * @see MavenDependencies#DEFAULT_SCOPES
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(URL pomPath) throws IOException {
		return download(pomPath, DEFAULT_SCOPES);
	}

	/**
	 * Downloads all of the dependencies specified in the pom
	 * 
	 * @param pomPath
	 *            The path to the pom in the classpath
	 * @param scopes
	 *            The scopes to download for
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(String pomPath, DependencyScope... scopes) throws IOException {
		return download(ClassLoader.getSystemResource(pomPath), scopes);
	}

	/**
	 * Downloads all of the dependencies specified in the pom for the default
	 * scopes
	 * 
	 * @param pomPath
	 *            The path to the pom in the classpath
	 * @return The set of all dependencies that were downloaded
	 * @see MavenDependencies#DEFAULT_SCOPES
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(String pomPath) throws IOException {
		return download(pomPath, DEFAULT_SCOPES);
	}

	/**
	 * Downloads all of the dependencies specified in the pom
	 * 
	 * @param groupId
	 *            The group ID of the artifact to download dependencies for
	 * @param artifactId
	 *            The artifact ID to download dependencies for
	 * @param scopes
	 *            The scopes to download for
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(String groupId, String artifactId, DependencyScope... scopes)
			throws IOException {
		return download(String.format("META-INF/maven/%s/%s/pom.xml", groupId, artifactId), scopes);
	}

	/**
	 * Downloads all of the dependencies specified in the pom for the default
	 * scopes
	 * 
	 * @param groupId
	 *            The group ID of the artifact to download dependencies for
	 * @param artifactId
	 *            The artifact ID to download dependencies for
	 * @return The set of all dependencies that were downloaded
	 * @see MavenDependencies#DEFAULT_SCOPES
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(String groupId, String artifactId) throws IOException {
		return download(groupId, artifactId, DEFAULT_SCOPES);
	}

	/**
	 * Downloads all of the dependencies specified in all poms contained inside
	 * the manifest in the current classpath
	 * 
	 * @param scopes
	 *            The scopes to download for
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download(DependencyScope... scopes) throws IOException {
		Set<Dependency> deps = new HashSet<Dependency>();
		for (String group : ClassPathScanner.listSystemResources("META-INF/maven")) {
			for (String artifact : ClassPathScanner.listSystemResources("META-INF/maven/".concat(group))) {
				deps.addAll(download(group, artifact, scopes));
			}
		}
		return deps;
	}

	/**
	 * Downloads all of the dependencies specified in all poms contained inside
	 * the manifest in the current classpath for the default scopes
	 * 
	 * @return The set of all dependencies that were downloaded
	 * @since 1.0.0
	 * @throws IOException
	 *             If an I/O error has occurred
	 */
	public static Set<Dependency> download() throws IOException {
		return download(DEFAULT_SCOPES);
	}
}

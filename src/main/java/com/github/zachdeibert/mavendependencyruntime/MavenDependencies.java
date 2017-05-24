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
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class MavenDependencies {
	private static final File BASE_DIR = new File(new File(System.getProperty("user.home"), ".runtime-deps"), "maven");
	private static final DependencyScope[] DEFAULT_SCOPES = { DependencyScope.COMPILE, DependencyScope.RUNTIME };
	private static final boolean ENABLE_LOGGING = "true"
			.equals(System.getProperty("com.github.zachdeibert.mavendependencyruntime.logging"));
	private static String indent = "";

	private static void createBaseDir() {
		BASE_DIR.mkdirs();
		// TODO make it hidden
	}

	public static void injectClasspath(Set<Dependency> dependencies) throws IOException {
		ClassLoader genericLoader = ClassLoader.getSystemClassLoader();
		if (genericLoader instanceof URLClassLoader) {
			try {
				URLClassLoader urlLoader = (URLClassLoader) genericLoader;
				Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				addUrl.setAccessible(true);
				for (Dependency dep : dependencies) {
					addUrl.invoke(urlLoader, dep.getFile(BASE_DIR, "jar").toURI().toURL());
				}
			} catch (ReflectiveOperationException ex) {
				throw new IOException("Unable to inject the classpath", ex);
			}
		} else {
			throw new IOException("Unable to inject the classpath");
		}
	}

	public static Set<Dependency> download(List<Repository> repositories, Dependency dependency) throws IOException {
		if (dependency.getGroupId().equals("com.github.zachdeibert")
				&& dependency.getArtifactId().equals("maven-dependency-runtime")) {
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
					repo.download(dependency, jar);
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

	public static Set<Dependency> download(Document pom) throws IOException {
		return download(pom, DEFAULT_SCOPES);
	}

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

	public static Set<Dependency> download(InputStream pom) throws IOException {
		return download(pom, DEFAULT_SCOPES);
	}

	public static Set<Dependency> download(URL pomPath, DependencyScope... scopes) throws IOException {
		InputStream pom = pomPath.openStream();
		Set<Dependency> downloaded = download(pom, scopes);
		pom.close();
		return downloaded;
	}

	public static Set<Dependency> download(URL pomPath) throws IOException {
		return download(pomPath, DEFAULT_SCOPES);
	}

	public static Set<Dependency> download(String pomPath, DependencyScope... scopes) throws IOException {
		return download(ClassLoader.getSystemResource(pomPath), scopes);
	}

	public static Set<Dependency> download(String pomPath) throws IOException {
		return download(pomPath, DEFAULT_SCOPES);
	}

	public static Set<Dependency> download(String groupId, String artifactId, DependencyScope... scopes)
			throws IOException {
		return download(String.format("META-INF/maven/%s/%s/pom.xml", groupId, artifactId), scopes);
	}

	public static Set<Dependency> download(String groupId, String artifactId) throws IOException {
		return download(groupId, artifactId, DEFAULT_SCOPES);
	}

	public static Set<Dependency> download(DependencyScope... scopes) throws IOException {
		Set<Dependency> deps = new HashSet<Dependency>();
		for (String group : ClassPathScanner.listSystemResources("META-INF/maven")) {
			for (String artifact : ClassPathScanner.listSystemResources("META-INF/maven/".concat(group))) {
				deps.addAll(download(group, artifact, scopes));
			}
		}
		return deps;
	}

	public static Set<Dependency> download() throws IOException {
		return download(DEFAULT_SCOPES);
	}
}

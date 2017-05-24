package com.github.zachdeibert.mavendependencyruntime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Repository extends AbstractXmlParser {
	private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2";
	private final String url;

	public String getUrl() {
		return url;
	}

	public void download(Dependency dep, File out) throws IOException {
		URL url = new URL(String.format("%s/%s/%s/%s/%s", getUrl(), dep.getGroupId().replace('.', '/'),
				dep.getArtifactId(), dep.getVersion(), out.getName()));
		InputStream ins = url.openStream();
		OutputStream outs = new FileOutputStream(out);
		byte[] buffer = new byte[4096];
		for (int len; (len = ins.read(buffer)) > 0; outs.write(buffer, 0, len))
			;
		outs.close();
		ins.close();
	}

	public void setVersion(Dependency dep) throws IOException {
		URL url = new URL(String.format("%s/%s/%s/maven-metadata.xml", getUrl(), dep.getGroupId().replace('.', '/'),
				dep.getArtifactId()));
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream ins = url.openStream();
			Document doc = builder.parse(ins);
			dep.setVersion(find("release", doc.getDocumentElement(), null));
		} catch (IOException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

	public Repository(String url) {
		this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	public Repository(Element node) throws ParseException {
		this(find("url", node, null));
	}

	public Repository() {
		this(MAVEN_CENTRAL);
	}
}

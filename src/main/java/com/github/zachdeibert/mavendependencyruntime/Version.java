package com.github.zachdeibert.mavendependencyruntime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Version implements Comparable<Version> {
	private final List<Integer> parts;
	private final String version;

	public int compareTo(Version o) {
		Iterator<Integer> us = parts.iterator();
		Iterator<Integer> them = o.parts.iterator();
		while (us.hasNext() && them.hasNext()) {
			int diff = us.next().compareTo(them.next());
			if (diff != 0) {
				return diff;
			}
		}
		return us.hasNext() ? 1 : them.hasNext() ? -1 : 0;
	}

	@Override
	public String toString() {
		return version;
	}

	public Version(String version) {
		parts = new ArrayList<Integer>();
		for (String part : version.split("[^0-9]")) {
			if (!part.isEmpty()) {
				parts.add(Integer.parseInt(part));
			}
		}
		this.version = version;
	}
}

package com.github.zachdeibert.mavendependencyruntime;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Scanner;

class Main {
	public static void main(String[] args) throws Exception {
		MavenDependencies.download();
		InputStream stream = ClassLoader.getSystemResourceAsStream("META-INF/MANIFEST.MF");
		Scanner scan = new Scanner(stream);
		String mainCls = null;
		while (scan.hasNext()) {
			String key = scan.findInLine("[^:]+");
			scan.findInLine(": *");
			mainCls = scan.nextLine();
			if (key.equals("Real-Main-Class")) {
				break;
			}
		}
		scan.close();
		stream.close();
		if (mainCls == null) {
			throw new ClassNotFoundException("Unable to find main class");
		}
		Class<?> cls = Class.forName(mainCls);
		Method main = cls.getMethod("main", String[].class);
		main.invoke(null, (Object) args);
	}
}

package com.github.zachdeibert.mavendependencyruntime;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Scanner;

class Main {
	private static void execMain(String mainCls, String[] args) throws Exception {
		Class<?> cls = Class.forName(mainCls);
		Method main = cls.getMethod("main", String[].class);
		main.invoke(null, (Object) args);
	}

	public static void main(String[] args) throws Exception {
		for (String premain : ClassPathScanner
				.listSystemResources("com/github/zachdeibert/mavendependencyruntime/premains")) {
			InputStream stream = ClassLoader.getSystemResourceAsStream(
					"com/github/zachdeibert/mavendependencyruntime/premains/".concat(premain));
			Scanner scan = new Scanner(stream);
			while (scan.hasNext()) {
				String mainCls = scan.nextLine();
				if (!mainCls.isEmpty()) {
					execMain(mainCls, args);
				}
			}
			scan.close();
			stream.close();
		}
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
		execMain(mainCls, args);
	}
}

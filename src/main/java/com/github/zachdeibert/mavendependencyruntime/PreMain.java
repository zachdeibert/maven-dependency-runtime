package com.github.zachdeibert.mavendependencyruntime;

class PreMain {
	public static void main(String[] args) throws Exception {
		MavenDependencies.download();
	}
}

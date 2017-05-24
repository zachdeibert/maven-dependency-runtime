# Maven Dependency Runtime

Adding this library to your project allows you to put just your code in a jar
file, then all of the dependencies will be downloaded from Maven when they are
needed at runtime.

## Usage

1. Set `com.github.zachdeibert.mavendependencyruntime.Main` as the main class
   for your jar file.
2. Add an entry to the manifest of your jar in which the key is
   `Real-Main-Class` and the value is the main class in your code.
3. Merge the Maven Dependency Runtime jar with your jar.
4. Distribute the merged jar, and it will automatically download the
   dependencies for you!

## Example Code

#### test/mavendependencies/App.java
```java
package test.mavendependencies;

import org.apache.logging.log4j.LogManager;

public class App {
	public static void main(String[] args) {
		LogManager.getLogger().fatal("Hello, world!");
	}
}
```

#### pom.xml
Note: this file should go in `/META-INF/maven/test/mavendependencies/pom.xml` in
the jar file.
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>test</groupId>
    <artifactId>mavendependencies</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-api</artifactId>
            <version>2.8.2</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>2.8.2</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

#### META-INF/MANIFEST.MF
```
Manifest-Version: 1.0
Main-Class: com.github.zachdeibert.mavendependencyruntime.Main
Real-Main-Class: test.mavendependencies.App
```

## Legal

This project has the MIT license, so you can use the code for your project,
regardless of what your project is or what license your project has.
There is a copyright notice for my code embedded in the jar, so all you need to
do is merge the jars, then you are free to distribute it.

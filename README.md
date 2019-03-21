## ResourceFS

This project provides a poc `java.nio.file.spi.FileSystemProvider` for use against class path resources.

e.g.

```java
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"),
			getClass().getResource("jars/jquantlib-0.1.2.jar"));

		URI fsRoot = new URI(ResourceFS.SCHEME, "foo", null, null, null);

		try (FileSystem fileSystem = FileSystems.newFileSystem(
				fsRoot, Collections.singletonMap(ResourceFS.URLS, urls))) {

			assertNotNull(fileSystem);

			Path rootDir = fileSystem.getRootDirectories()
				.iterator()
				.next();

			List<Path> list = Files.find(rootDir, 30, (p, a) -> a.isRegularFile())
				.sorted()
				.collect(Collectors.toList());

			assertEquals(2, list.size());

			assertEquals("guava-14.0.1.jar", list.get(0)
				.toFile()
				.getName());
			assertEquals("jquantlib-0.1.2.jar", list.get(1)
				.toFile()
				.getName());
		}

```

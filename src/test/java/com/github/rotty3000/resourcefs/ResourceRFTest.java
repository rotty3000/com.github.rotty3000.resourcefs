/**
 * Copyright 2019 Raymond Augé
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rotty3000.resourcefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class ResourceRFTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Test(expected = FileSystemAlreadyExistsException.class)
	public void createAndCheckCannotDuplicate() throws Exception {
		List<URL> urls = Collections.emptyList();

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			createFileSystem("foo", urls);
		}
	}

	@Test
	public void createMultiple() throws Exception {
		List<URL> urls = Collections.emptyList();

		try (FileSystem fileSystemA = createFileSystem("foo", urls)) {
			assertNotNull(fileSystemA);

			try (FileSystem fileSystemB = createFileSystem("bar", urls)) {
				assertNotNull(fileSystemB);
			}
		}
	}

	@Test
	public void createWithUrls() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"),
			getClass().getResource("jars/jquantlib-0.1.2.jar"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			Iterable<FileStore> fileStores = fileSystem.getFileStores();

			assertNotNull(fileStores);
			FileStore fileStore = fileStores.iterator()
				.next();
			assertNotNull(fileStore);
		}
	}

	@Test
	public void findFiles() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"),
			getClass().getResource("jars/jquantlib-0.1.2.jar"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
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
	}

	@Test
	public void findDirectories() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"),
			getClass().getResource("jars/jquantlib-0.1.2.jar"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			Path rootDir = fileSystem.getRootDirectories()
				.iterator()
				.next();

			List<Path> list = Files.find(rootDir, 30, (p, a) -> a.isDirectory())
				.sorted()
				.collect(Collectors.toList());

			assertEquals(11, list.size());
		}
	}

	@Test
	public void readFile() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/resource.txt"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			Path rootDir = fileSystem.getRootDirectories()
				.iterator()
				.next();

			Optional<Path> path = Files.find(rootDir, 30, (p, a) -> a.isRegularFile())
				.findFirst();

			assertTrue(path.isPresent());

			List<String> lines = Files.readAllLines(path.get());

			assertEquals(1, lines.size());
			assertEquals("test", lines.get(0));
		}
	}

	@Test
	public void copyFile() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"),
			getClass().getResource("jars/jquantlib-0.1.2.jar"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			Path rootDir = fileSystem.getRootDirectories()
				.iterator()
				.next();

			Optional<Path> path = Files.find(rootDir, 30, (p, a) -> a.isRegularFile())
				.sorted()
				.findFirst();

			assertTrue(path.isPresent());

			File newFile = new File(tmp.getRoot(), "guava.jar");

			Files.copy(path.get(), newFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);

			try (JarFile jar = new JarFile(newFile)) {
				assertEquals("com.google.guava", jar.getManifest().getMainAttributes().getValue("Bundle-SymbolicName"));
			}
		}
	}

	@Test
	public void loadAJar_ZipInoutStream() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			Path rootDir = fileSystem.getRootDirectories()
				.iterator()
				.next();

			Optional<Path> first = Files.find(rootDir, 30, (p, a) -> p.toFile().getName().endsWith(".jar"))
				.sorted().findFirst();

			assertTrue(first.isPresent());

			Path outDir = Paths.get(tmp.newFolder().toURI());
			byte[] buffer = new byte[2048];

			try (InputStream is = fileSystem.provider().newInputStream(first.get());
					ZipInputStream zis = new ZipInputStream(is)) {

				ZipEntry ze;

				while ((ze = zis.getNextEntry()) != null) {
					if (ze.isDirectory()) {
						continue;
					}

					Path filePath = outDir.resolve(ze.getName());
					filePath.toFile().getParentFile().mkdirs();

					try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
							BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {

						int len;
						while ((len = zis.read(buffer)) > 0) {
							bos.write(buffer, 0, len);
						}
					}
				}
			}
			System.out.println("Done!");
		}
	}

	@Ignore("This doesn't work because Zip FS only accepts Paths from the default FS")
	@Test
	public void loadAJar_ZipFS_usingPath() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			Path rootDir = fileSystem.getRootDirectories()
				.iterator()
				.next();

			Optional<Path> first = Files.find(rootDir, 30, (p, a) -> p.toFile().getName().endsWith(".jar"))
				.sorted().findFirst();

			assertTrue(first.isPresent());

			FileSystem jarFS = FileSystems.newFileSystem(first.get(), null);

			assertNotNull(jarFS);
		}
	}

	@Test
	public void loadAJar_ZipFS_usingURI() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			Path rootDir = fileSystem.getRootDirectories()
				.iterator()
				.next();

			Optional<Path> first = Files.find(rootDir, 30, (p, a) -> p.toFile().getName().endsWith(".jar"))
				.sorted().findFirst();

			assertTrue(first.isPresent());

			URI jarURI = new URI("jar", first.get().toUri().toString(), null);

			// create a Zip file system from the JAR

			FileSystem jarFS = FileSystems.newFileSystem(jarURI, Collections.emptyMap());

			assertNotNull(jarFS);

			Path zipRootDir = jarFS.getRootDirectories()
				.iterator()
				.next();

			List<Path> zipPaths = Files.find(zipRootDir, 30, (p, a) -> a.isRegularFile())
				.sorted().collect(Collectors.toList());

			assertEquals(1597, zipPaths.size());
		}
	}

	@Ignore("Currently testing with equinox which handles bundles using the ZipFile API which doesn't support custom FS providers")
	@Test
	public void populateFramework() throws Exception {
		List<URL> urls = Arrays.asList(getClass().getResource("jars/guava-14.0.1.jar"));

		try (FileSystem fileSystem = createFileSystem("foo", urls)) {
			assertNotNull(fileSystem);

			FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

			Framework framework = factory.newFramework(
				Collections.singletonMap("osgi.install.area", tmp.newFolder().getAbsolutePath()));

			try {
				framework.init();

				framework.start();

				Path rootDir = fileSystem.getRootDirectories()
					.iterator()
					.next();

				BundleContext bundleContext = framework.getBundleContext();

				Files.find(rootDir, 30, (p, a) -> p.toFile().getName().endsWith(".jar"))
					.sorted().forEach(path -> {
						try {
							String ref = "reference:" + path.toString();
							bundleContext.installBundle(ref);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					});
			}
			finally {
				framework.stop();
			}
		}
	}

	FileSystem createFileSystem(String authority, List<URL> urls) throws Exception {
		return FileSystems.newFileSystem(new URI(ResourceFS.SCHEME, authority, null, null, null),
			Collections.singletonMap(ResourceFS.URLS, urls));
	}

}

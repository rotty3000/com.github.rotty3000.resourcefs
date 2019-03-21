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

import java.io.File;
import java.net.URL;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BasicTest extends BaseTest {

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

}

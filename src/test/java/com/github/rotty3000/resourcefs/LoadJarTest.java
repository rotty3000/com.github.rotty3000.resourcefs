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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LoadJarTest extends BaseTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

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
			int count = 0;

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
						count++;
					}
				}
			}

			assertEquals(1597, count);
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

}

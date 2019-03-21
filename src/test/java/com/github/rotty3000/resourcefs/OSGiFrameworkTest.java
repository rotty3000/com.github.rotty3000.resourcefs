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

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class OSGiFrameworkTest extends BaseTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

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
					.sorted().forEach(path -> installBundle(bundleContext, "reference:" + path.toString()));
			}
			finally {
				framework.stop();
			}
		}
	}

	void installBundle(BundleContext bundleContext, String location) {
		try {
			bundleContext.installBundle(location);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}

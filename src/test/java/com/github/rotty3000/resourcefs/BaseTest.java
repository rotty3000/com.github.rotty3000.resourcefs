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

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.List;

import com.github.rotty3000.resourcefs.ResourceFS;

public abstract class BaseTest {

	FileSystem createFileSystem(String authority, List<URL> urls) throws Exception {
		URI fsRoot = new URI(ResourceFS.SCHEME, authority, null, null, null);

		return FileSystems.newFileSystem(
			fsRoot, Collections.singletonMap(ResourceFS.URLS, urls));
	}

}

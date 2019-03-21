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

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class ResourceFileStore extends FileStore {

	static final String NAME = "default";

	final ResourceFileSystem	fileSystem;
	final ResourceFileStoreAttributeView	fileStoreAttributeView	= new ResourceFileStoreAttributeView();
	final Map<ResourcePath, ResourceAttributeView>	views					= new ConcurrentHashMap<>();
	final LongAdder								totalSize				= new LongAdder();

	ResourceFileStore(ResourceFileSystem fileSystem) {
		this.fileSystem = fileSystem;
		this.fileSystem.urls.forEach(this::process);
	}

	private void process(URL url) {
		ResourcePath original = new ResourcePath(fileSystem, url.getPath());
		String path = ResourceFS.SEPARATOR;

		for (int i = 0; i < original.segments.length; i++) {
			path += original.segments[i];
			ResourcePath current = new ResourcePath(fileSystem, path);
			ResourcePath parent = current.getParent();
			if (parent == null) {
				parent = fileSystem.basePath;
			}

			views.computeIfAbsent(current, key -> {
				ResourceAttributeView view;

				// last segment is the file
				if (current.compareTo(original) == 0) {
					view = new ResourceAttributeView(current, new ResourceFileAttributes(current, url));
				} else {
					view = new ResourceAttributeView(current, new ResourceDirAttributes(current));
				}

				totalSize.add(view.size());
				return view;
			});

			path += ResourceFS.SEPARATOR;
		}
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String type() {
		return NAME;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public long getTotalSpace() throws IOException {
		return totalSize.longValue();
	}

	@Override
	public long getUsableSpace() throws IOException {
		return 0;
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		return 0;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return type.isAssignableFrom(ResourceAttributeView.class);
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		return "basic".equals(name);
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		if (type.isAssignableFrom(ResourceFileStoreAttributeView.class)) {
			return type.cast(fileStoreAttributeView);
		}
		return null;
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		return null;
	}

}

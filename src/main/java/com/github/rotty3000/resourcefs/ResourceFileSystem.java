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
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class ResourceFileSystem extends FileSystem {

	static final Set<String>	supportedViews	= Collections.singleton("basic");

	final ResourceFS	provider;
	final String authority;
	final List<URL>	urls;
	final ResourcePath			basePath;
	final ResourceFileStore			fileStore;
	final List<FileStore>	fileStores;

	@SuppressWarnings("unchecked")
	public ResourceFileSystem(ResourceFS provider, String authority, Map<String, ?> env) {
		this.provider = provider;
		this.authority = authority;
		Object urlsObject = Objects.requireNonNull(env.get(ResourceFS.URLS));
		this.urls = new ArrayList<>(Collections.checkedCollection((Collection<URL>) urlsObject, URL.class));
		this.basePath = new ResourcePath(this, ResourceFS.SEPARATOR);
		this.fileStore = new ResourceFileStore(this);
		this.fileStores = new ArrayList<>();
		fileStores.add(fileStore);
	}

	@Override
	public FileSystemProvider provider() {
		return provider;
	}

	@Override
	public void close() throws IOException {
		provider.fileSystems.remove(authority, this);
	}

	@Override
	public boolean isOpen() {
		return provider.fileSystems.containsValue(this);
	}

	@Override
	public boolean isReadOnly() {
		return isOpen();
	}

	@Override
	public String getSeparator() {
		return ResourceFS.SEPARATOR;
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return fileStore.views.keySet()
			.stream()
			.filter(resourcePath -> resourcePath.getParent() == null)
			.map(Path.class::cast)
			.collect(Collectors.toList());
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		return fileStores;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return supportedViews;
	}

	@Override
	public ResourcePath getPath(String first, String... more) {
		if (first == null || ResourceFS.SEPARATOR.equals(first)) {
			return null;
		}
		return new ResourcePath(this, ResourceFS.join(first, more));
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		return new PathMatcher() {

			@Override
			public boolean matches(Path path) {
				return false;
			}
		};
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException();
	}

}
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

class ResourcePath implements Path {

	final ResourceFileSystem	fileSystem;
	final URI				uri;
	final byte[]			path;
	final String[]			segments;
	volatile String			string;

	ResourcePath(ResourceFileSystem fileSystem, String path) {
		this.fileSystem = fileSystem;
		path = Objects.requireNonNull(path, "path cannot be null");
		String[] segs;
		if (ResourceFS.SEPARATOR.equals(path)) {
			path = "";
			segs = new String[0];
		} else if (path.startsWith(ResourceFS.SEPARATOR)) {
			path = path.substring(1);
			segs = path.split(ResourceFS.SEPARATOR);
		} else {
			segs = path.split(ResourceFS.SEPARATOR);
		}
		this.segments = segs;
		this.path = path.getBytes(StandardCharsets.UTF_8);
		this.uri = ResourceFS.build(fileSystem.authority, ResourceFS.join(path));
	}

	@Override
	public int compareTo(Path other) {
		int len1 = path.length;
		int len2 = ((ResourcePath) other).path.length;

		int n = Math.min(len1, len2);
		byte v1[] = path;
		byte v2[] = ((ResourcePath) other).path;

		int k = 0;
		while (k < n) {
			int c1 = v1[k] & 0xff;
			int c2 = v2[k] & 0xff;
			if (c1 != c2) {
				return c1 - c2;
			}
			k++;
		}
		return len1 - len2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourcePath other = (ResourcePath) obj;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public boolean endsWith(Path other) {
		int index = segments.length - 1;
		while ((index > -1) && other.getFileName()
			.toString()
			.equals(segments[index])) {
			if (other.getParent() == null) {
				return true;
			}
			other = other.getParent();
			index--;
		}
		return false;
	}

	@Override
	public final boolean endsWith(String other) {
		return endsWith(fileSystem.getPath(other));
	}

	@Override
	public ResourcePath getFileName() {
		return fileSystem.getPath(segments[segments.length - 1]);
	}

	@Override
	public ResourceFileSystem getFileSystem() {
		return fileSystem;
	}

	@Override
	public Path getName(int index) {
		return fileSystem.getPath(segments[index]);
	}

	@Override
	public int getNameCount() {
		return segments.length;
	}

	@Override
	public ResourcePath getParent() {
		if (segments.length == 0) {
			return null;
		}

		URI parent = uri.getPath()
			.endsWith("/") ? uri.resolve("..") : uri.resolve(".");

		return fileSystem.getPath(parent.getPath());
	}

	@Override
	public Path getRoot() {
		return fileSystem.getPath(segments[0]);
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public final Iterator<Path> iterator() {
		return new Iterator<Path>() {
			private int i = 0;

			@Override
			public boolean hasNext() {
				return (i < getNameCount());
			}

			@Override
			public Path next() {
				if (i < getNameCount()) {
					Path result = getName(i);
					i++;
					return result;
				} else {
					throw new NoSuchElementException();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public Path normalize() {
		return this;
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		return null;
	}

	@Override
	public final WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
		return register(watcher, events, new WatchEvent.Modifier[0]);
	}

	@Override
	public Path relativize(Path other) {
		// TODO
		return null;
	}

	@Override
	public Path resolve(Path other) {
		// TODO
		return null;
	}

	@Override
	public final Path resolve(String other) {
		return resolve(getFileSystem().getPath(other));
	}

	@Override
	public final Path resolveSibling(Path other) {
		if (other == null)
			throw new NullPointerException();
		Path parent = getParent();
		return (parent == null) ? other : parent.resolve(other);
	}

	@Override
	public final Path resolveSibling(String other) {
		return resolveSibling(getFileSystem().getPath(other));
	}

	@Override
	public boolean startsWith(Path other) {
		if (other.getNameCount() > segments.length) {
			return false;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < other.getNameCount(); i++) {
			sb.append(ResourceFS.SEPARATOR);
			sb.append(segments[i]);
		}
		return fileSystem.getPath(sb.toString())
			.equals(other);
	}

	@Override
	public final boolean startsWith(String other) {
		return startsWith(getFileSystem().getPath(other));
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		StringBuilder sb = new StringBuilder();
		for (int i = beginIndex; i < endIndex; i++) {
			if (i > beginIndex) {
				sb.append(ResourceFS.SEPARATOR);
			}
			sb.append(segments[i]);
		}
		return fileSystem.getPath(sb.toString());
	}

	@Override
	public Path toAbsolutePath() {
		// TODO
		return this;
	}

	@Override
	public final File toFile() {
		return new File(toString()) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isAbsolute() {
				return ResourcePath.this.isAbsolute();
			}
			@Override
			public boolean exists() {
				return true;
			}
			@Override
			public Path toPath() {
				return ResourcePath.this;
			}
		};
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		// TODO
		return null;
	}

	@Override
	public String toString() {
		return uri.toString();
	}

	@Override
	public URI toUri() {
		return uri;
	}

}

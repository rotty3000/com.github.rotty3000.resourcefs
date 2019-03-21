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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ResourceFS extends FileSystemProvider {

	public static final String	SCHEME		= "resources";
	public static final String	SEPARATOR	= File.separatorChar + "";
	public static final String	URLS		= SCHEME;

	private static ResourceFS	INSTANCE	= new ResourceFS();

	static URI build(String authority, String path) {
		try {
			return new URI(SCHEME, authority, path, null, null);
		} catch (URISyntaxException e) {
			throw thro(e);
		}
	}

	static String join(String first, String... more) {
		StringBuilder sb = new StringBuilder(ResourceFS.SEPARATOR);
		if (first.startsWith(ResourceFS.SEPARATOR)) {
			sb.append(first.substring(1));
		}
		else {
			sb.append(first);
		}
		if (more != null && more.length > 0) {
			for (String it : more) {
				sb.append(ResourceFS.SEPARATOR);
				sb.append(it);
			}
		}
		return sb.toString();
	}

	public static RuntimeException thro(Throwable t) {
		throwsUnchecked(t);
		throw new AssertionError("unreachable");
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwsUnchecked(Throwable throwable) throws E {
		throw (E) throwable;
	}

	public static FileSystem newFileSystem(String name, Collection<URL> urls) throws IOException {
		return INSTANCE.newFileSystem(build(name, null), Collections.singletonMap(URLS, urls));
	}

	final Map<String, ResourceFileSystem>	fileSystems	= new ConcurrentHashMap<>();

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		Arrays.sort(modes);

		if (Arrays.binarySearch(modes, AccessMode.EXECUTE) > -1) {
			throw new IOException("EXECUTE not supported by ResourceFS");
		}
		if (Arrays.binarySearch(modes, AccessMode.WRITE) > -1) {
			throw new IOException("WRITE not supported by ResourceFS");
		}
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(Path path) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean deleteIfExists(Path path) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		if (!(path instanceof ResourcePath) && !type.isAssignableFrom(ResourceAttributeView.class)) {
			return null;
		}

		ResourcePath resourcePath = (ResourcePath) path;

		ResourceFileSystem fileSystem = getFileSystem(resourcePath.uri);
		if (fileSystem == null) {
			return null;
		}

		ResourceAttributeView view = fileSystem.fileStore.views.get(resourcePath);

		if (view == null) {
			return null;
		}

		return type.cast(view);
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		ResourceFileSystem fileSystem = getFileSystem(path.toUri());
		if (fileSystem == null) {
			return null;
		}
		return fileSystem.fileStore;
	}

	@Override
	public ResourceFileSystem getFileSystem(URI uri) {
		if (!SCHEME.equals(uri.getScheme())) {
			return null;
		}
		return fileSystems.get(uri.getAuthority());
	}

	@Override
	public ResourcePath getPath(URI uri) {
		ResourceFileSystem fileSystem = getFileSystem(uri);
		if (fileSystem == null) {
			return null;
		}
		return new ResourcePath(fileSystem, uri.getPath());
	}

	@Override
	public String getScheme() {
		return SCHEME;
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		return false;
	}

	@Override
	public boolean isSameFile(Path pathA, Path pathB) throws IOException {
		if (!(pathA instanceof ResourcePath) || !(pathB instanceof ResourcePath)) {
			return false;
		}

		ResourcePath urLsPathA = (ResourcePath) pathA;
		ResourcePath urLsPathB = (ResourcePath) pathB;

		if (!urLsPathA.fileSystem.authority.equals(urLsPathB.fileSystem.authority)) {
			return false;
		}

		if (Arrays.equals(urLsPathA.path, urLsPathB.path)) {
			return true;
		}

		return false;
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
		throws IOException {

		ResourceFileSystem fileSystem = getFileSystem(path.toUri());
		if (fileSystem == null) {
			return null;
		}
		return new ResourceReadOnlyChannel(path, options);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter)
		throws IOException {

		if (!(dir instanceof ResourcePath)) {
			throw new IOException("invalid Path dir " + dir);
		}

		return new DirectoryStream<Path>() {

			private volatile boolean closed = false;

			@Override
			public void close() throws IOException {
				closed = true;
			}

			@Override
			public Iterator<Path> iterator() {
				if (closed) {
					throw new IllegalStateException("Directory stream is closed");
				}

				List<Path> matches = getFileSystem(dir.toUri()).fileStore.views.keySet()
					.stream()
					.filter(path -> path.startsWith(dir) && (path.getNameCount() == (dir.getNameCount() + 1)))
					.map(Path.class::cast)
					.collect(Collectors.toList());

				return matches.iterator();
			}
		};
	}

	@Override
	public ResourceFileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		if (!SCHEME.equals(uri.getScheme())) {
			throw new IOException("URLsFS requires scheme " + SCHEME);
		}
		return fileSystems.compute(
			Objects.requireNonNull(uri.getAuthority(), "URLsFS requires authority to be non-null"),
			(auth, existing) -> {
			if (existing != null) {
				throw new FileSystemAlreadyExistsException(uri.toString());
			}
				return new ResourceFileSystem(this, auth, env);
		});
	}

	@Override
	public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
		ResourceFileSystem fileSystem = getFileSystem(path.toUri());
		if (fileSystem == null) {
			return null;
		}

		ResourcePath resourcePath = (ResourcePath) path;

		ResourceAttributeView urLsFileAttributeView = fileSystem.fileStore.views.get(resourcePath);

		if (!urLsFileAttributeView.attributes.isRegularFile()) {
			throw new IOException("Path is not a file");
		}

		return ((ResourceFileAttributes) urLsFileAttributeView.attributes).url.openStream();
	}

	@Override
	public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
		throws IOException {

		if (!(path instanceof ResourcePath) && !type.isAssignableFrom(ResourceFileAttributes.class)
			&& !type.isAssignableFrom(ResourceDirAttributes.class)) {
			return null;
		}

		ResourcePath resourcePath = (ResourcePath) path;

		ResourceFileSystem fileSystem = getFileSystem(resourcePath.uri);
		if (fileSystem == null) {
			return null;
		}

		ResourceAttributeView view = fileSystem.fileStore.views.get(resourcePath);

		if (view == null) {
			return null;
		}

		return type.cast(view.getAttributes());
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		BasicFileAttributes basicFileAttributes = readAttributes(path, BasicFileAttributes.class, options);

		if ((basicFileAttributes == null) || (!attributes.startsWith("basic:") && attributes.contains(":"))) {
			return Collections.emptyMap();
		}

		if (attributes.startsWith("basic:")) {
			attributes = attributes.substring(6);
		}

		Map<String, Object> map = new HashMap<>();

//		for (String it : attributes.split("\\s*,\\s*")) {
			// TODO
//		}

		return map;
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return SCHEME + ":";
	}

}

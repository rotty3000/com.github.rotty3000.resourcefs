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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceReadOnlyChannel implements SeekableByteChannel {

	private final Path path;
	private final Set<? extends OpenOption> options;
	private final long size;

	private final InputStream stream;

	private final long position;
	private final Set<Boolean> closed = ConcurrentHashMap.newKeySet();

	public ResourceReadOnlyChannel(Path path, Set<? extends OpenOption> options) throws IOException {
		this(path, options, 0);
	}

	ResourceReadOnlyChannel(Path path, Set<? extends OpenOption> options, long position) throws IOException {
		this.path = path;
		this.options = options;
		this.position = position;
		closed.add(Boolean.FALSE);
		this.size = Files.readAttributes(path, BasicFileAttributes.class).size();
		this.stream = Files.newInputStream(path, options.toArray(new OpenOption[0]));
	}

	@Override
	public boolean isOpen() {
		return !closed.isEmpty();
	}

	@Override
	public void close() throws IOException {
		closed.removeIf(b -> {
			try {
				stream.close();
				return true;
			}
			catch (IOException e) {
				throw ResourceFS.thro(e);
			}
		});
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long size() throws IOException {
		return size;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (!isOpen()) {
			throw new IOException("closed");
		}
		stream.skip(position);
		return stream.readNBytes(dst.array(), 0, dst.capacity());
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		return new ResourceReadOnlyChannel(path, options, newPosition);
	}

	@Override
	public long position() throws IOException {
		return position;
	}

}

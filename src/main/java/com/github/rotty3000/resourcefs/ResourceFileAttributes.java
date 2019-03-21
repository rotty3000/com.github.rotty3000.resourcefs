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
import java.net.URLConnection;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

class ResourceFileAttributes implements BasicFileAttributes {

	final ResourcePath	resourcePath;
	final URL		url;
	final FileTime	lastModifiedTime;
	final FileTime	creationTime;
	final long		size;

	ResourceFileAttributes(ResourcePath resourcePath, URL url) {
		this.resourcePath = resourcePath;
		this.url = url;

		try {
			URLConnection connection = this.url.openConnection();
			this.lastModifiedTime = FileTime.fromMillis(connection.getLastModified());
			this.creationTime = FileTime.fromMillis(connection.getDate());
			this.size = connection.getContentLengthLong();
		} catch (IOException e) {
			throw ResourceFS.thro(e);
		}
	}

	@Override
	public FileTime lastModifiedTime() {
		return lastModifiedTime;
	}

	@Override
	public FileTime lastAccessTime() {
		return lastModifiedTime;
	}

	@Override
	public FileTime creationTime() {
		return creationTime;
	}

	@Override
	public boolean isRegularFile() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isSymbolicLink() {
		return false;
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public Object fileKey() {
		return resourcePath;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "$" + resourcePath.toString();
	}

}

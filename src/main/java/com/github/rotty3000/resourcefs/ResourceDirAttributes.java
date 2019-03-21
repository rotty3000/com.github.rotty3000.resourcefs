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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

class ResourceDirAttributes implements BasicFileAttributes {

	final ResourcePath	resourcePath;
	final FileTime	lastModifiedTime;
	final FileTime	creationTime;

	ResourceDirAttributes(ResourcePath resourcePath) {
		this.resourcePath = resourcePath;
		this.lastModifiedTime = FileTime.fromMillis(0);
		this.creationTime = FileTime.fromMillis(0);
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
		return false;
	}

	@Override
	public boolean isDirectory() {
		return true;
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
		return 0;
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

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
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class ResourceAttributeView implements BasicFileAttributeView {

	final BasicFileAttributes attributes;
	final ResourcePath				resourcePath;

	ResourceAttributeView(ResourcePath resourcePath, BasicFileAttributes attributes) {
		this.resourcePath = resourcePath;
		this.attributes = attributes;
	}

	@Override
	public String name() {
		return "basic";
	}

	public BasicFileAttributes getAttributes() {
		return attributes;
	}

	public ResourcePath getPath() {
		return resourcePath;
	}

	@Override
	public BasicFileAttributes readAttributes() throws IOException {
		return attributes;
	}

	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
		throw new UnsupportedOperationException();
	}

	public long size() {
		return attributes.size();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "$" + attributes.getClass()
			.getSimpleName() + "$" + resourcePath.toString();
	}

}

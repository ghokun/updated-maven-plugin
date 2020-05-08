/*
 * MIT License
 *
 * Copyright (c) 2020 ghokun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.ghokun.scm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class holding source code changes as a tree.
 *
 * @author ghokun
 * @since 1.0.0
 */
public class SourceCodeChanges implements Iterable<SourceCodeChanges> {
	
	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String path;
	private final Set<SourceCodeDiff> diffs = new HashSet<>();
	private final Set<SourceCodeChanges> modules = new HashSet<>();
	
	public SourceCodeChanges(String groupId, String artifactId, String version, String path) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.path = path;
	}
	
	public String getGroupId() {
		return this.groupId;
	}
	
	public String getArtifactId() {
		return this.artifactId;
	}
	
	public String getCoords() {
		return this.getGroupId() + ":" + this.getArtifactId();
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public String getRelativePath(String rootPath) {
		return this.path.replaceFirst(rootPath, "");
	}
	
	public Set<SourceCodeDiff> getDiffs() {
		return this.diffs;
	}
	
	public Set<SourceCodeChanges> getModules() {
		return this.modules;
	}
	
	public boolean hasDiff() {
		return !this.getDiffs().isEmpty();
	}
	
	public int diffCount() {
		return this.getDiffs().size();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.artifactId, this.groupId, this.version);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SourceCodeChanges)) {
			return false;
		}
		final SourceCodeChanges other = (SourceCodeChanges) obj;
		return Objects.equals(this.artifactId, other.artifactId) && Objects.equals(this.groupId, other.groupId)
				&& Objects.equals(this.version, other.version);
	}
	
	@Override
	public String toString() {
		return this.print(new StringBuilder(), 0);
	}
	
	private String print(StringBuilder sb, int level) {
		if (sb == null) {
			sb = new StringBuilder();
		}
		if (!this.diffs.isEmpty()) {
			sb
				.append("------------------------------------------------------------------------")
				.append(System.lineSeparator())
				.append("Changes for ")
				.append(this.groupId + ":" + this.artifactId + ":" + this.version)
				.append(System.lineSeparator())
				.append("------------------------------------------------------------------------");
			for (final SourceCodeDiff diff : this.diffs) {
				sb.append(System.lineSeparator()).append(diff.toString());
			}
			sb.append(System.lineSeparator()).append(System.lineSeparator());
		}
		if (!this.getModules().isEmpty()) {
			level++;
			for (final SourceCodeChanges module : this.modules) {
				module.print(sb, level);
			}
		}
		return sb.toString();
	}
	
	private List<SourceCodeChanges> toList() {
		final List<SourceCodeChanges> list = new ArrayList<>();
		list.add(this);
		if (!this.getModules().isEmpty()) {
			for (final SourceCodeChanges module : this.getModules()) {
				list.addAll(module.toList());
			}
		}
		return list;
	}
	
	@Override
	public Iterator<SourceCodeChanges> iterator() {
		return this.toList().iterator();
	}
	
}
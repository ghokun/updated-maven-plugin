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

import java.util.Objects;

/**
 * Source code difference class showing diff type with paths.
 * 
 * @author ghokun
 * @since 1.0.0
 */
public class SourceCodeDiff {
	
	private final DiffType type;
	private final String oldPath;
	private final String newPath;
	
	public SourceCodeDiff(DiffType type, String oldPath, String newPath) {
		super();
		this.type = type;
		this.oldPath = oldPath;
		this.newPath = newPath;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("[").append(this.type).append(" ");
		switch (this.type) {
			case ADD:
				sb.append(this.newPath);
				break;
			case COPY:
				sb.append(this.oldPath + "->" + this.newPath);
				break;
			case DELETE:
				sb.append(this.oldPath);
				break;
			case MODIFY:
				sb.append(this.oldPath);
				break;
			case RENAME:
				sb.append(this.oldPath + "->" + this.newPath);
				break;
		}
		return sb.append("]").toString();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.newPath, this.oldPath, this.type);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SourceCodeDiff)) {
			return false;
		}
		final SourceCodeDiff other = (SourceCodeDiff) obj;
		return Objects.equals(this.newPath, other.newPath) && Objects.equals(this.oldPath, other.oldPath)
				&& this.type == other.type;
	}
}
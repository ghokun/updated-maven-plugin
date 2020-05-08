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

package io.github.ghokun.updated.enumeration;

import java.util.HashMap;
import java.util.Map;

/**
 * LineEnding enumeration.
 *
 * @author ghokun
 * @since 1.0.0
 */
public enum LineEnding {

	CR("\r"),
	LF("\n"),
	CRLF("\r\n");

	private final String value;

	private LineEnding(final String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	private static Map<String, LineEnding> cache;

	static {
		cache = new HashMap<>();
		for (final LineEnding lineEnding : LineEnding.values()) {
			cache.put(lineEnding.value, lineEnding);
		}
	}

	public static LineEnding fromValue(final String value) {
		return cache.get(value);
	}
}
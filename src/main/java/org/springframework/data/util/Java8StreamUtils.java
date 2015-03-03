/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;

/**
 * Spring Data specific Java {@link Stream} utility methods and classes.
 * 
 * @author Thomas Darimont
 * @since 1.8
 */
public class Java8StreamUtils {

	private Java8StreamUtils() {}

	/**
	 * Returns a {@link Stream} backed by the given {@link Iterator}.
	 * <p>
	 * If the given iterator is an {@link CloseableIterator} add a {@link CloseableIteratorDisposingRunnable} wrapping the
	 * given iterator to propagate {@link Stream#close()} accordingly.
	 * 
	 * @param iterator must not be {@literal null}
	 * @return
	 * @since 1.8
	 */
	public static <T> Stream<T> createStreamFromIterator(Iterator<T> iterator) {

		Assert.notNull(iterator, "Iterator must not be null!");

		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL);
		Stream<T> stream = StreamSupport.stream(spliterator, false);

		return iterator instanceof CloseableIterator ? stream.onClose(new CloseableIteratorDisposingRunnable(
				(CloseableIterator<T>) iterator)) : stream;
	}
}

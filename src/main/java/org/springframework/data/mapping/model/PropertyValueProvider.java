/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * SPI for components to provide values for as {@link PersistentProperty}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface PropertyValueProvider<P extends PersistentProperty<P>> {

	/**
	 * Returns a value for the given {@link PersistentProperty}.
	 *
	 * @param property will never be {@literal null}.
	 * @return
	 */
	@Nullable
	<T> T getPropertyValue(P property);
}

/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.mapping;

import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Exception being thrown when creating {@link PropertyPath} instances.
 * 
 * @author Oliver Gierke
 */
public class PropertyReferenceException extends RuntimeException {

	private static final long serialVersionUID = -5254424051438976570L;
	private static final String ERROR_TEMPLATE = "No property %s found for type %s";

	private final String propertyName;
	private final TypeInformation<?> type;
	private final PropertyPath base;

	/**
	 * Creates a new {@link PropertyReferenceException}.
	 * 
	 * @param propertyName the name of the property not found on the given type.
	 * @param type the type the property could not be found on.
	 * @param base the base {@link PropertyPath}.
	 */
	public PropertyReferenceException(String propertyName, TypeInformation<?> type, PropertyPath base) {

		Assert.hasText(propertyName);
		Assert.notNull(type);

		this.propertyName = propertyName;
		this.type = type;
		this.base = base;
	}

	/**
	 * Returns the name of the property not found.
	 * 
	 * @return will not be {@literal null} or empty.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Returns the type the property could not be found on.
	 * 
	 * @return the type
	 */
	public TypeInformation<?> getType() {
		return type;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return String.format(ERROR_TEMPLATE, propertyName, type.getType().getName());
	}

	/**
	 * Returns the {@link PropertyPath} which could be resolved so far.
	 * 
	 * @return
	 */
	public PropertyPath getBaseProperty() {
		return base;
	}
}

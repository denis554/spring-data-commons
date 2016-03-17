/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Example.NullHandler;
import org.springframework.data.domain.PropertySpecifier.NoOpPropertyValueTransformer;
import org.springframework.data.domain.PropertySpecifier.PropertyValueTransformer;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Support for query by example (QBE).
 *
 * @author Christoph Strobl
 * @param <T>
 * @since 1.12
 */
public class Example<T> {

	private final T sampleObject;

	private NullHandler nullHandler = NullHandler.IGNORE;
	private StringMatcher defaultStringMatcher = StringMatcher.DEFAULT;
	private PropertySpecifiers propertySpecifiers = new PropertySpecifiers();
	private Set<String> ignoredPaths = new LinkedHashSet<String>();

	private boolean ignoreCase = false;

	/**
	 * Create a new {@link Example} including all non-null properties by default.
	 * 
	 * @param sampleObject The example to use. Must not be {@literal null}.
	 */
	public Example(T sampleObject) {

		Assert.notNull(sampleObject, "SampleObject must not be null!");
		this.sampleObject = sampleObject;
	}

	/**
	 * Get the example used.
	 * 
	 * @return never {@literal null}.
	 */
	public T getSampleObject() {
		return sampleObject;
	}

	/**
	 * Get defined null handling.
	 * 
	 * @return never {@literal null}
	 */
	public NullHandler getNullHandler() {
		return nullHandler;
	}

	/**
	 * Get defined {@link StringMatcher}.
	 * 
	 * @return never {@literal null}.
	 */
	public StringMatcher getDefaultStringMatcher() {
		return defaultStringMatcher;
	}

	/**
	 * @return {@literal true} if {@link String} should be matched with ignore case option.
	 */
	public boolean isIngnoreCaseEnabled() {
		return this.ignoreCase;
	}

	/**
	 * @param path
	 * @return return {@literal true} if path was set to be ignored.
	 */
	public boolean isIgnoredPath(String path) {
		return this.ignoredPaths.contains(path);
	}

	/**
	 * @return unmodifiable {@link Set} of ignored paths.
	 */
	public Set<String> getIgnoredPaths() {
		return Collections.unmodifiableSet(ignoredPaths);
	}

	/**
	 * @return unmodifiable {@link Collection} of {@link PropertySpecifier}s.
	 */
	public Collection<PropertySpecifier> getPropertySpecifiers() {
		return propertySpecifiers.getSpecifiers();
	}

	/**
	 * @param path Dot-Path to property.
	 * @return {@literal true} in case {@link PropertySpecifier} defined for given path.
	 */
	public boolean hasPropertySpecifier(String path) {
		return propertySpecifiers.hasSpecifierForPath(path);
	}

	/**
	 * Get the {@link PropertySpecifier} for given path. <br />
	 * Please check if {@link #hasPropertySpecifier(String)} to avoid running into {@literal null} values.
	 * 
	 * @param path Dot-Path to property.
	 * @return {@literal null} when no {@link PropertySpecifier} defined for path.
	 */
	public PropertySpecifier getPropertySpecifier(String path) {
		return propertySpecifiers.getForPath(path);
	}

	/**
	 * @return true if at least one {@link PropertySpecifier} defined.
	 */
	public boolean hasPropertySpecifiers() {
		return this.propertySpecifiers.hasValues();
	}

	/**
	 * Get the {@link StringMatcher} for a given path or return the default one if none defined.
	 * 
	 * @param path
	 * @return never {@literal null}.
	 */
	public StringMatcher getStringMatcherForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return getDefaultStringMatcher();
		}

		PropertySpecifier specifier = getPropertySpecifier(path);
		return specifier.getStringMatcher() != null ? specifier.getStringMatcher() : getDefaultStringMatcher();
	}

	/**
	 * Get the ignore case flag for a given path or return the default one if none defined.
	 * 
	 * @param path
	 * @return never {@literal null}.
	 */
	public boolean isIgnoreCaseForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return isIngnoreCaseEnabled();
		}

		PropertySpecifier specifier = getPropertySpecifier(path);
		return specifier.getIgnoreCase() != null ? specifier.getIgnoreCase().booleanValue() : isIngnoreCaseEnabled();
	}

	/**
	 * Get the ignore case flag for a given path or return {@link NoOpPropertyValueTransformer} if none defined.
	 * 
	 * @param path
	 * @return never {@literal null}.
	 */
	public PropertyValueTransformer getValueTransformerForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return NoOpPropertyValueTransformer.INSTANCE;
		}

		return getPropertySpecifier(path).getPropertyValueTransformer();
	}

	/**
	 * Get the actual type for the example used. This is usually the given class, but the original class in case of a
	 * CGLIB-generated subclass.
	 * 
	 * @return
	 * @see ClassUtils#getUserClass(Class)
	 */
	@SuppressWarnings("unchecked")
	public Class<T> getSampleType() {
		return (Class<T>) ClassUtils.getUserClass(sampleObject.getClass());
	}

	/**
	 * Create a new {@link Example} including all non-null properties by default.
	 * 
	 * @param sampleObject must not be {@literal null}.
	 * @return
	 */
	public static <T> Example<T> exampleOf(T sampleObject) {
		return new Example<T>(sampleObject);
	}

	/**
	 * Create a new {@link Example} including all non-null properties, excluding explicitly named properties to ignore.
	 * 
	 * @param sampleObject must not be {@literal null}.
	 * @return
	 */
	public static <T> Example<T> exampleOf(T value, String... ignoredProperties) {
		return new Builder<T>(value).ignore(ignoredProperties).get();
	}

	/**
	 * Create new {@link Builder} for specifying {@link Example}.
	 * 
	 * @param sampleObject must not be {@literal null}.
	 * @return
	 * @see Builder
	 */
	public static <T> Builder<T> newExampleOf(T sampleObject) {
		return new Builder<T>(sampleObject);
	}

	/**
	 * Builder for specifying desired behavior of {@link Example}.
	 * 
	 * @author Christoph Strobl
	 * @param <T>
	 */
	public static class Builder<T> {

		private Example<T> example;

		Builder(T sampleObject) {
			example = new Example<T>(sampleObject);
		}

		/**
		 * Sets {@link NullHandler} used for {@link Example}.
		 * 
		 * @param nullHandling
		 * @return
		 * @see Builder#nullHandling(NullHandler)
		 */
		public Builder<T> withNullHandler(NullHandler nullHandling) {
			return handleNullValues(nullHandling);
		}

		/**
		 * Sets default {@link StringMatcher} used for {@link Example}.
		 * 
		 * @param stringMatcher
		 * @return
		 * @see Builder#matchStrings(StringMatcher)
		 */
		public Builder<T> withStringMatcher(StringMatcher stringMatcher) {
			return matchStrings(stringMatcher);
		}

		/**
		 * Adds {@link PropertySpecifier} used for {@link Example}.
		 * 
		 * @param specifier
		 * @return
		 * @see Builder#specify(PropertySpecifier...)
		 */
		public Builder<T> withPropertySpecifier(PropertySpecifier... specifiers) {
			return specify(specifiers);
		}

		/**
		 * Sets {@link NullHandler} used for {@link Example}.
		 * 
		 * @param nullHandling Defaulted to {@link NullHandler#INCLUDE} in case of {@literal null}.
		 * @return
		 */
		public Builder<T> handleNullValues(NullHandler nullHandling) {

			example.nullHandler = nullHandling == null ? NullHandler.IGNORE : nullHandling;
			return this;
		}

		/**
		 * Sets treatment of {@literal null} values to {@link NullHandler#INCLUDE}
		 * 
		 * @return
		 */
		public Builder<T> includeNullValues() {
			return handleNullValues(NullHandler.INCLUDE);
		}

		/**
		 * Sets treatment of {@literal null} values to {@link NullHandler#IGNORE}
		 * 
		 * @return
		 */
		public Builder<T> ignoreNullValues() {
			return handleNullValues(NullHandler.IGNORE);
		}

		/**
		 * Sets the default {@link StringMatcher} used for {@link Example}.
		 * 
		 * @param stringMatcher Defaulted to {@link StringMatcher#DEFAULT} in case of {@literal null}.
		 * @return
		 */
		public Builder<T> matchStrings(StringMatcher stringMatcher) {
			return matchStrings(stringMatcher, example.ignoreCase);
		}

		/**
		 * Sets the default {@link StringMatcher} used for {@link Example}.
		 * 
		 * @param stringMatcher Defaulted to {@link StringMatcher#DEFAULT} in case of {@literal null}.
		 * @param ignoreCase
		 * @return
		 */
		public Builder<T> matchStrings(StringMatcher stringMatching, boolean ignoreCase) {

			example.defaultStringMatcher = stringMatching == null ? StringMatcher.DEFAULT : stringMatching;
			example.ignoreCase = ignoreCase;
			return this;
		}

		/**
		 * Enable case ignoring string matching.
		 * 
		 * @return
		 */
		public Builder<T> matchStringsWithIgnoreCase() {
			example.ignoreCase = true;
			return this;
		}

		/**
		 * Set string matching to {@link StringMatcher#STARTING}
		 * 
		 * @return
		 */
		public Builder<T> matchStringsStartingWith() {
			return matchStrings(StringMatcher.STARTING);
		}

		/**
		 * Set string matching to {@link StringMatcher#ENDING}
		 * 
		 * @return
		 */
		public Builder<T> matchStringsEndingWith() {
			return matchStrings(StringMatcher.ENDING);
		}

		/**
		 * Set string matching to {@link StringMatcher#CONTAINING}
		 * 
		 * @return
		 */
		public Builder<T> matchStringsContaining() {
			return matchStrings(StringMatcher.CONTAINING);
		}

		/**
		 * Define specific property handling.
		 * 
		 * @param specifiers
		 * @return
		 */
		public Builder<T> specify(PropertySpecifier... specifiers) {

			for (PropertySpecifier specifier : specifiers) {
				example.propertySpecifiers.add(specifier);
			}
			return this;
		}

		/**
		 * Ignore given properties.
		 * 
		 * @param ignoredProperties
		 * @return
		 */
		public Builder<T> ignore(String... ignoredProperties) {

			for (String ignoredProperty : ignoredProperties) {
				example.ignoredPaths.add(ignoredProperty);
			}
			return this;
		}

		/**
		 * @return {@link Example} as defined.
		 */
		public Example<T> get() {
			return this.example;
		}
	}

	/**
	 * Null handling for creating criterion out of an {@link Example}.
	 * 
	 * @author Christoph Strobl
	 */
	public static enum NullHandler {

		INCLUDE, IGNORE
	}

	/**
	 * Match modes for treatment of {@link String} values.
	 * 
	 * @author Christoph Strobl
	 */
	public static enum StringMatcher {

		/**
		 * Store specific default.
		 */
		DEFAULT(null),
		/**
		 * Matches the exact string
		 */
		EXACT(Type.SIMPLE_PROPERTY),
		/**
		 * Matches string starting with pattern
		 */
		STARTING(Type.STARTING_WITH),
		/**
		 * Matches string ending with pattern
		 */
		ENDING(Type.ENDING_WITH),
		/**
		 * Matches string containing pattern
		 */
		CONTAINING(Type.CONTAINING),
		/**
		 * Treats strings as regular expression patterns
		 */
		REGEX(Type.REGEX);

		private Type type;

		private StringMatcher(Type type) {
			this.type = type;
		}

		/**
		 * Get the according {@link Part.Type}.
		 * 
		 * @return {@literal null} for {@link StringMatcher#DEFAULT}.
		 */
		public Type getPartType() {
			return type;
		}

	}

	private static class PropertySpecifiers {

		private Map<String, PropertySpecifier> propertySpecifiers = new LinkedHashMap<String, PropertySpecifier>();

		public void add(PropertySpecifier specifier) {

			Assert.notNull(specifier, "PropertySpecifier must not be null!");
			propertySpecifiers.put(specifier.getPath(), specifier);
		}

		public boolean hasSpecifierForPath(String path) {
			return propertySpecifiers.containsKey(path);
		}

		public PropertySpecifier getForPath(String path) {
			return propertySpecifiers.get(path);
		}

		public boolean hasValues() {
			return !propertySpecifiers.isEmpty();
		}

		public Collection<PropertySpecifier> getSpecifiers() {
			return propertySpecifiers.values();
		}
	}

}

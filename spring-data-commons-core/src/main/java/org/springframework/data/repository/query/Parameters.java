/*
 * Copyright 2008-2012 the original author or authors.
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
package org.springframework.data.repository.query;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Abstracts method parameters that have to be bound to query parameters or applied to the query independently.
 * 
 * @author Oliver Gierke
 */
public class Parameters implements Iterable<Parameter> {

	@SuppressWarnings("unchecked")
	public static final List<Class<?>> TYPES = Arrays.asList(Pageable.class, Sort.class);

	private static final String ALL_OR_NOTHING = String.format("Either use @%s "
			+ "on all parameters except %s and %s typed once, or none at all!", Param.class.getSimpleName(),
			Pageable.class.getSimpleName(), Sort.class.getSimpleName());

	private final int pageableIndex;
	private final int sortIndex;

	private final List<Parameter> parameters;
	private final ParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

	/**
	 * Creates a new instance of {@link Parameters}.
	 * 
	 * @param method
	 */
	public Parameters(Method method) {

		Assert.notNull(method);

		this.parameters = new ArrayList<Parameter>();

		List<Class<?>> types = Arrays.asList(method.getParameterTypes());

		for (int i = 0; i < types.size(); i++) {
			MethodParameter parameter = new MethodParameter(method, i);
			parameter.initParameterNameDiscovery(discoverer);
			parameters.add(createParameter(parameter));
		}

		this.pageableIndex = types.indexOf(Pageable.class);
		this.sortIndex = types.indexOf(Sort.class);

		assertEitherAllParamAnnotatedOrNone();
	}

	/**
	 * Creates a new {@link Parameters} instance with the given {@link Parameter}s put into new context.
	 * 
	 * @param originals
	 */
	private Parameters(List<Parameter> originals) {

		this.parameters = new ArrayList<Parameter>();

		int pageableIndexTemp = -1;
		int sortIndexTemp = -1;

		for (int i = 0; i < originals.size(); i++) {

			Parameter original = originals.get(i);
			this.parameters.add(original);

			pageableIndexTemp = original.isPageable() ? i : -1;
			sortIndexTemp = original.isSort() ? i : -1;
		}

		this.pageableIndex = pageableIndexTemp;
		this.sortIndex = sortIndexTemp;
	}

	protected Parameter createParameter(MethodParameter parameter) {
		return new Parameter(parameter);
	}

	/**
	 * Returns whether the method the {@link Parameters} was created for contains a {@link Pageable} argument.
	 * 
	 * @return
	 */
	public boolean hasPageableParameter() {

		return pageableIndex != -1;
	}

	/**
	 * Returns the index of the {@link Pageable} {@link Method} parameter if available. Will return {@literal -1} if there
	 * is no {@link Pageable} argument in the {@link Method}'s parameter list.
	 * 
	 * @return the pageableIndex
	 */
	public int getPageableIndex() {

		return pageableIndex;
	}

	/**
	 * Returns the index of the {@link Sort} {@link Method} parameter if available. Will return {@literal -1} if there is
	 * no {@link Sort} argument in the {@link Method}'s parameter list.
	 * 
	 * @return
	 */
	public int getSortIndex() {

		return sortIndex;
	}

	/**
	 * Returns whether the method the {@link Parameters} was created for contains a {@link Sort} argument.
	 * 
	 * @return
	 */
	public boolean hasSortParameter() {

		return sortIndex != -1;
	}

	/**
	 * Returns whether we potentially find a {@link Sort} parameter in the parameters.
	 * 
	 * @return
	 */
	public boolean potentiallySortsDynamically() {

		return hasSortParameter() || hasPageableParameter();
	}

	/**
	 * Returns the parameter with the given index.
	 * 
	 * @param index
	 * @return
	 */
	public Parameter getParameter(int index) {

		try {
			return parameters.get(index);
		} catch (IndexOutOfBoundsException e) {
			throw new ParameterOutOfBoundsException(
					"Invalid parameter index! You seem to have declare too little query method parameters!", e);
		}
	}

	/**
	 * Returns whether we have a parameter at the given position.
	 * 
	 * @param position
	 * @return
	 */
	public boolean hasParameterAt(int position) {

		try {
			return null != getParameter(position);
		} catch (ParameterOutOfBoundsException e) {
			return false;
		}
	}

	/**
	 * Returns whether the method signature contains one of the special parameters ({@link Pageable}, {@link Sort}).
	 * 
	 * @return
	 */
	public boolean hasSpecialParameter() {

		return hasSortParameter() || hasPageableParameter();
	}

	/**
	 * Returns the number of parameters.
	 * 
	 * @return
	 */
	public int getNumberOfParameters() {

		return parameters.size();
	}

	/**
	 * Returns a {@link Parameters} instance with effectively all special parameters removed.
	 * 
	 * @return
	 * @see Parameter#TYPES
	 * @see Parameter#isSpecialParameter()
	 */
	public Parameters getBindableParameters() {

		List<Parameter> bindables = new ArrayList<Parameter>();

		for (Parameter candidate : this) {

			if (candidate.isBindable()) {
				bindables.add(candidate);
			}
		}

		return new Parameters(bindables);
	}

	/**
	 * Returns a bindable parameter with the given index. So for a method with a signature of
	 * {@code (Pageable pageable, String name)} a call to {@code #getBindableParameter(0)} will return the {@link String}
	 * parameter.
	 * 
	 * @param bindableIndex
	 * @return
	 */
	public Parameter getBindableParameter(int bindableIndex) {

		return getBindableParameters().getParameter(bindableIndex);
	}

	/**
	 * Asserts that either all of the non special parameters ({@link Pageable}, {@link Sort}) are annotated with
	 * {@link Param} or none of them is.
	 * 
	 * @param method
	 */
	private void assertEitherAllParamAnnotatedOrNone() {

		boolean nameFound = false;

		for (Parameter parameter : this.getBindableParameters()) {

			if (parameter.isNamedParameter()) {
				Assert.isTrue(nameFound || parameter.isFirst(), ALL_OR_NOTHING);
				nameFound = true;
			} else {
				Assert.isTrue(!nameFound, ALL_OR_NOTHING);
			}
		}
	}

	/**
	 * Returns whether the given type is a bindable parameter.
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isBindable(Class<?> type) {

		return !TYPES.contains(type);
	}

	/*
			 * (non-Javadoc)
			 *
			 * @see java.lang.Iterable#iterator()
			 */
	public Iterator<Parameter> iterator() {

		return parameters.iterator();
	}
}
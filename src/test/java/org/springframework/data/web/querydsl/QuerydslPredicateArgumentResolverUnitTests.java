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
package org.springframework.data.web.querydsl;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.User;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.StringPath;

/**
 * @author Christoph Strobl
 */
public class QuerydslPredicateArgumentResolverUnitTests {

	QuerydslPredicateArgumentResolver resolver;
	MockHttpServletRequest request;

	@Before
	public void setUp() {

		resolver = new QuerydslPredicateArgumentResolver(null);
		request = new MockHttpServletRequest();
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void supportsParameterReturnsTrueWhenMethodParameterIsPredicateAndAnnotatedCorrectly() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("simpleFind", Predicate.class)), is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void supportsParameterReturnsTrueWhenMethodParameterIsPredicateButNotAnnotatedAsSuch() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("predicateWithoutAnnotation", Predicate.class)),
				is(true));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test(expected = IllegalArgumentException.class)
	public void supportsParameterShouldThrowExceptionWhenMethodParameterIsNoPredicateButAnnotatedAsSuch() {
		resolver.supportsParameter(getMethodParameterFor("nonPredicateWithAnnotation", String.class));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void supportsParameterReturnsFalseWhenMethodParameterIsNoPredicate() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("nonPredicateWithoutAnnotation", String.class)),
				is(false));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() throws Exception {

		request.addParameter("firstname", "rand");

		Object predicate = (BooleanExpression) resolver.resolveArgument(
				getMethodParameterFor("simpleFind", Predicate.class), null, new ServletWebRequest(request), null);

		assertThat(predicate, Is.<Object> is(QUser.user.firstname.eq("rand")));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateMultipleParametersPredicateCorrectly() throws Exception {

		request.addParameter("firstname", "rand");
		request.addParameter("lastname", "al'thor");

		Object predicate = resolver.resolveArgument(getMethodParameterFor("simpleFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate, Is.<Object> is(QUser.user.firstname.eq("rand").and(QUser.user.lastname.eq("al'thor"))));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldCreateNestedObjectPredicateCorrectly() throws Exception {

		request.addParameter("address.city", "two rivers");

		Object predicate = resolver.resolveArgument(getMethodParameterFor("simpleFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate.toString(), is(QUser.user.address.city.eq("two rivers").toString()));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldResolveTypePropertyFromPageCorrectly() throws Exception {

		request.addParameter("address.city", "tar valon");

		Object predicate = resolver.resolveArgument(getMethodParameterFor("pagedFind", Predicate.class, Pageable.class),
				null, new ServletWebRequest(request), null);

		assertThat(predicate.toString(), is(QUser.user.address.city.eq("tar valon").toString()));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void resolveArgumentShouldHonorCustomSpeficifcation() throws Exception {

		request.addParameter("firstname", "egwene");
		request.addParameter("lastname", "al'vere");

		Object predicate = resolver.resolveArgument(getMethodParameterFor("specificFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate.toString(),
				is(QUser.user.firstname.eq("egwene".toUpperCase()).and(QUser.user.lastname.toLowerCase().eq("al'vere"))
						.toString()));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldCreatePredicateForNonStringPropertyCorrectly() throws Exception {

		request.addParameter("inceptionYear", "978");

		Object predicate = (BooleanExpression) resolver.resolveArgument(
				getMethodParameterFor("specificFind", Predicate.class), null, new ServletWebRequest(request), null);

		assertThat(predicate, Is.<Object> is(QUser.user.inceptionYear.eq(978L)));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldCreatePredicateForNonStringListPropertyCorrectly() throws Exception {

		request.addParameter("inceptionYear", new String[] { "978", "998" });

		Object predicate = (BooleanExpression) resolver.resolveArgument(
				getMethodParameterFor("specificFind", Predicate.class), null, new ServletWebRequest(request), null);

		assertThat(predicate, Is.<Object> is(QUser.user.inceptionYear.in(978L, 998L)));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldExcludePorpertiesCorrectly() throws Exception {

		request.addParameter("address.street", "downhill");
		request.addParameter("inceptionYear", "973");

		Object predicate = resolver.resolveArgument(getMethodParameterFor("specificFind", Predicate.class), null,
				new ServletWebRequest(request), null);

		assertThat(predicate.toString(), is(QUser.user.inceptionYear.eq(973L).toString()));
	}

	private MethodParameter getMethodParameterFor(String methodName, Class<?>... args) throws RuntimeException {

		try {
			return new MethodParameter(Sample.class.getMethod(methodName, args), 0);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	static class SpecificBinding extends QuerydslBindings {

		public SpecificBinding() {

			bind("firstname", new QuerydslBinding<StringPath>() {

				@Override
				public Predicate bind(StringPath path, Object value) {
					return path.eq(value.toString().toUpperCase());
				}
			});

			bind(QUser.user.lastname, new QuerydslBinding<StringPath>() {

				@Override
				public Predicate bind(StringPath path, Object value) {
					return path.toLowerCase().eq(value.toString());
				}
			});

			excluding("address");
		}
	}

	static interface Sample {

		User predicateWithoutAnnotation(Predicate predicate);

		User nonPredicateWithAnnotation(@QuerydslPredicate String predicate);

		User nonPredicateWithoutAnnotation(String predicate);

		User simpleFind(@QuerydslPredicate Predicate predicate);

		Page<User> pagedFind(@QuerydslPredicate Predicate predicate, Pageable page);

		User specificFind(@QuerydslPredicate(bindings = SpecificBinding.class) Predicate predicate);
	}

}

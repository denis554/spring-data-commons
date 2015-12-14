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
package org.springframework.data.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author Oliver Gierke
 */
public class ResultProcessorUnitTests {

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void leavesNonProjectingResultUntouched() throws Exception {

		ResultProcessor information = new ResultProcessor(getQueryMethod("findAll"), new SpelAwareProxyProjectionFactory());

		Sample sample = new Sample("Dave", "Matthews");
		List<Sample> result = new ArrayList<Sample>(Arrays.asList(sample));
		List<Sample> converted = information.processResult(result);

		assertThat(converted, contains(sample));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void createsProjectionFromProperties() throws Exception {

		ResultProcessor information = getFactory("findOneProjection");

		SampleProjection result = information.processResult(Arrays.asList("Matthews"));

		assertThat(result.getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void createsListOfProjectionsFormNestedLists() throws Exception {

		ResultProcessor information = getFactory("findAllProjection");

		List<String> columns = Arrays.asList("Matthews");
		List<List<String>> source = new ArrayList<List<String>>(Arrays.asList(columns));

		List<SampleProjection> result = information.processResult(source);

		assertThat(result, hasSize(1));
		assertThat(result.get(0).getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void createsListOfProjectionsFromMaps() throws Exception {

		ResultProcessor information = getFactory("findAllProjection");

		List<Map<String, Object>> source = new ArrayList<Map<String, Object>>(
				Arrays.asList(Collections.<String, Object> singletonMap("lastname", "Matthews")));

		List<SampleProjection> result = information.processResult(source);

		assertThat(result, hasSize(1));
		assertThat(result.get(0).getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void createsListOfProjectionsFromEntity() throws Exception {

		ResultProcessor information = getFactory("findAllProjection");

		List<Sample> source = new ArrayList<Sample>(Arrays.asList(new Sample("Dave", "Matthews")));
		List<SampleProjection> result = information.processResult(source);

		assertThat(result, hasSize(1));
		assertThat(result.get(0).getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void createsPageOfProjectionsFromEntity() throws Exception {

		ResultProcessor information = getFactory("findPageProjection", Pageable.class);

		Page<Sample> source = new PageImpl<Sample>(Arrays.asList(new Sample("Dave", "Matthews")));
		Page<SampleProjection> result = information.processResult(source);

		assertThat(result.getContent(), hasSize(1));
		assertThat(result.getContent().get(0).getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void createsDynamicProjectionFromEntity() throws Exception {

		ResultProcessor information = getFactory("findOneOpenProjection");

		OpenProjection result = information.processResult(new Sample("Dave", "Matthews"));

		assertThat(result.getLastname(), is("Matthews"));
		assertThat(result.getFullName(), is("Dave Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void findsDynamicProjection() throws Exception {

		ParameterAccessor accessor = mock(ParameterAccessor.class);

		ResultProcessor factory = getFactory("findOneDynamic", Class.class);
		assertThat(factory.withDynamicProjection(null), is(factory));
		assertThat(factory.withDynamicProjection(accessor), is(factory));

		doReturn(SampleProjection.class).when(accessor).getDynamicProjection();

		ResultProcessor processor = factory.withDynamicProjection(accessor);
		assertThat(processor.getReturnedType().getReturnedType(), is(typeCompatibleWith(SampleProjection.class)));
	}

	private static ResultProcessor getFactory(String methodName, Class<?>... parameters) throws Exception {
		return getQueryMethod(methodName, parameters).getResultProcessor();
	}

	private static QueryMethod getQueryMethod(String name, Class<?>... parameters) throws Exception {

		Method method = SampleRepository.class.getMethod(name, parameters);
		return new QueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory());
	}

	interface SampleRepository extends Repository<Sample, Long> {

		List<Sample> findAll();

		List<SampleDTO> findAllDtos();

		List<SampleProjection> findAllProjection();

		Sample findOne();

		SampleDTO findOneDto();

		SampleProjection findOneProjection();

		OpenProjection findOneOpenProjection();

		Page<SampleProjection> findPageProjection(Pageable pageable);

		<T> T findOneDynamic(Class<T> type);
	}

	static class Sample {
		public String firstname, lastname;

		public Sample(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}
	}

	static class SampleDTO {}

	interface SampleProjection {

		String getLastname();
	}

	interface OpenProjection {

		String getLastname();

		@Value("#{target.firstname + ' ' + target.lastname}")
		String getFullName();
	}
}

/*
 * Copyright 2015-2017 the original author or authors.
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
package org.springframework.data.web.config;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.web.ProjectingJackson2HttpMessageConverter;
import org.springframework.data.web.XmlBeamHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.xmlbeam.XBProjector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;

/**
 * Integration test for {@link SpringDataWebConfiguration}.
 *
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Oliver Gierke
 */
public class SpringDataWebConfigurationIntegrationTests {

	@Test // DATACMNS-987
	public void shouldNotLoadJacksonConverterWhenJacksonNotPresent() {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader(HidingClassLoader.hide(ObjectMapper.class)).extendMessageConverters(converters);

		assertThat(converters).areNot(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class));
	}

	@Test // DATACMNS-987
	public void shouldNotLoadJacksonConverterWhenJaywayNotPresent() {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader(HidingClassLoader.hide(DocumentContext.class)).extendMessageConverters(converters);

		assertThat(converters).areNot(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class));
	}

	@Test // DATACMNS-987
	public void shouldNotLoadXBeamConverterWhenXBeamNotPresent() throws Exception {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		ClassLoader classLoader = HidingClassLoader.hide(XBProjector.class);
		createConfigWithClassLoader(classLoader).extendMessageConverters(converters);

		assertThat(converters).areNot(instanceWithClassName(XmlBeamHttpMessageConverter.class));
	}

	@Test // DATACMNS-987
	public void shouldLoadAllConvertersWhenDependenciesArePresent() throws Exception {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader(getClass().getClassLoader()).extendMessageConverters(converters);

		assertThat(converters).haveAtLeastOne(instanceWithClassName(XmlBeamHttpMessageConverter.class));
		assertThat(converters).haveAtLeastOne(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class));
	}

	private SpringDataWebConfiguration createConfigWithClassLoader(ClassLoader classLoader) {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				SpringDataWebConfiguration.class);

		context.setClassLoader(classLoader);

		try {
			return context.getBean(SpringDataWebConfiguration.class);
		} finally {
			context.close();
		}
	}

	/**
	 * Creates a {@link Condition} that checks if an object is an instance of a class with the same name as the provided
	 * class. This is necessary since we are dealing with multiple classloaders which would make a simple instanceof fail
	 * all the time
	 *
	 * @param expectedClass the class that is expected (possibly loaded by a different classloader).
	 * @return a {@link Condition}
	 */
	private static Condition<Object> instanceWithClassName(Class<?> expectedClass) {

		return new Condition<>(it -> it.getClass().getName().equals(expectedClass.getName()), //
				"with class name %s!", expectedClass.getName());
	}
}

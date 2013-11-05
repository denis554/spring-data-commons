/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.data.repository.config;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

/**
 * Unit tests for {@link AnnotationRepositoryConfigurationSource}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class AnnotationRepositoryConfigurationSourceUnitTests {

	RepositoryConfigurationSource source;
	Environment environment;

	@Before
	public void setUp() {

		AnnotationMetadata annotationMetadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		environment = new StandardEnvironment();
		source = new AnnotationRepositoryConfigurationSource(annotationMetadata, EnableRepositories.class, environment);
	}

	@Test
	public void findsBasePackagesForClasses() {

		Iterable<String> basePackages = source.getBasePackages();
		assertThat(basePackages, hasItem(AnnotationRepositoryConfigurationSourceUnitTests.class.getPackage().getName()));
	}

	@Test
	public void evaluatesExcludeFiltersCorrectly() {

		Collection<BeanDefinition> candidates = source.getCandidates(new DefaultResourceLoader());
		assertThat(candidates, hasSize(1));

		BeanDefinition candidate = candidates.iterator().next();
		assertThat(candidate.getBeanClassName(), is(MyRepository.class.getName()));
	}

	@Test
	public void defaultsToPackageOfAnnotatedClass() {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(DefaultConfiguration.class);
		AnnotationRepositoryConfigurationSource source = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositories.class, environment);

		Iterable<String> packages = source.getBasePackages();
		assertThat(packages, hasItem(DefaultConfiguration.class.getPackage().getName()));
		assertThat(source.shouldConsiderNestedRepositories(), is(false));
	}

	@Test
	public void returnsConfiguredBasePackage() {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(DefaultConfigurationWithBasePackage.class);
		AnnotationRepositoryConfigurationSource source = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositories.class, environment);

		Iterable<String> packages = source.getBasePackages();
		assertThat(packages, hasItem("foo"));
	}

	/**
	 * @see DATACMNS-90
	 */
	@Test
	public void returnsConsiderNestedRepositories() {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(DefaultConfigurationWithNestedRepositories.class);
		AnnotationRepositoryConfigurationSource source = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositories.class, environment);

		assertThat(source.shouldConsiderNestedRepositories(), is(true));
	}

	public static class Person {}

	@EnableRepositories
	static class DefaultConfiguration {}

	@EnableRepositories(basePackages = "foo")
	static class DefaultConfigurationWithBasePackage {}

	@EnableRepositories(considerNestedRepositories = true)
	static class DefaultConfigurationWithNestedRepositories {}
}

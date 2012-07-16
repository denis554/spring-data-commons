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
package org.springframework.data.repository.config;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.sample.SampleAnnotatedRepository;

/**
 * Unit tests for {@link RepositoryComponentProvider}.
 * 
 * @author Oliver Gierke
 */
public class RepositoryComponentProviderUnitTests {

	@Test
	public void findsAnnotatedRepositoryInterface() {

		RepositoryComponentProvider provider = new RepositoryComponentProvider(Collections.<TypeFilter> emptyList());
		Set<BeanDefinition> components = provider.findCandidateComponents("org.springframework.data.repository.sample");

		assertThat(components.size(), is(1));
		assertThat(components.iterator().next().getBeanClassName(), is(SampleAnnotatedRepository.class.getName()));
	}

	@Test
	public void limitsFoundRepositoriesToIncludeFiltersOnly() {

		List<? extends TypeFilter> filters = Arrays.asList(new AssignableTypeFilter(MyOtherRepository.class));

		RepositoryComponentProvider provider = new RepositoryComponentProvider(filters);
		Set<BeanDefinition> components = provider.findCandidateComponents("org.springframework.data.repository");

		assertThat(components.size(), is(1));
		assertThat(components.iterator().next().getBeanClassName(), is(MyOtherRepository.class.getName()));
	}
}

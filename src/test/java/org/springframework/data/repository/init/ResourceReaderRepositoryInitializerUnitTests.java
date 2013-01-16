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
package org.springframework.data.repository.init;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.Repositories;

/**
 * Unit tests for {@link UnmarshallingRepositoryInitializer}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceReaderRepositoryInitializerUnitTests {

	@Mock
	ResourceReader reader;
	@Mock
	Repositories repositories;
	@Mock
	Resource resource;
	@Mock
	CrudRepository<Object, Serializable> repo;

	@Mock
	ApplicationEventPublisher publisher;

	@Test
	public void storesSingleObjectCorrectly() throws Exception {

		Object reference = new Object();

		setUpReferenceAndInititalize(reference);

		verify(repo, times(1)).save(reference);
	}

	@Test
	public void storesCollectionOfObjectsCorrectly() throws Exception {

		Object object = new Object();
		Collection<Object> reference = Collections.singletonList(object);

		setUpReferenceAndInititalize(reference);

		verify(repo, times(1)).save(object);
	}

	/**
	 * @see DATACMNS-224
	 */
	@Test
	public void emitsRepositoriesPopulatedEventIfPublisherConfigured() throws Exception {

		RepositoryPopulator populator = setUpReferenceAndInititalize(new Object(), publisher);

		ApplicationEvent event = new RepositoriesPopulatedEvent(populator, repositories);
		verify(publisher, times(1)).publishEvent(event);
	}

	private RepositoryPopulator setUpReferenceAndInititalize(Object reference, ApplicationEventPublisher publish)
			throws Exception {

		when(reader.readFrom(any(Resource.class), any(ClassLoader.class))).thenReturn(reference);
		when(repositories.getRepositoryFor(Object.class)).thenReturn(repo);

		ResourceReaderRepositoryPopulator populator = new ResourceReaderRepositoryPopulator(reader);
		populator.setResources(resource);
		populator.setApplicationEventPublisher(publisher);
		populator.populate(repositories);

		return populator;
	}

	private RepositoryPopulator setUpReferenceAndInititalize(Object reference) throws Exception {
		return setUpReferenceAndInititalize(reference, null);
	}
}

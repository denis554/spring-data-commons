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
package org.springframework.data.projection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 * Unit tests for {@link DefaultProjectionInformation}.
 * 
 * @author Oliver Gierke
 */
public class DefaultProjectionInformationUnitTests {

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void discoversInputProperties() {

		ProjectionInformation information = new DefaultProjectionInformation(CustomerProjection.class);

		assertThat(toNames(information.getInputProperties()), contains("firstname", "lastname"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void discoversAllInputProperties() {

		ProjectionInformation information = new DefaultProjectionInformation(ExtendedProjection.class);

		assertThat(toNames(information.getInputProperties()), hasItems("age", "firstname", "lastname"));
	}

	/**
	 * @see DATACMNS-967
	 */
	@Test
	public void doesNotConsiderDefaultMethodInputProperties() throws Exception {

		ProjectionInformation information = new DefaultProjectionInformation(WithDefaultMethod.class);

		assertThat(information.isClosed(), is(true));
		assertThat(toNames(information.getInputProperties()), hasItems("firstname"));
	}

	private static List<String> toNames(List<PropertyDescriptor> descriptors) {

		return descriptors.stream()//
				.map(it -> it.getName())//
				.collect(Collectors.toList());
	}

	interface CustomerProjection {

		String getFirstname();

		String getLastname();
	}

	interface ExtendedProjection extends CustomerProjection {

		int getAge();
	}

	interface WithDefaultMethod {

		String getFirstname();

		default String getLastname() {
			return null;
		}
	}
}

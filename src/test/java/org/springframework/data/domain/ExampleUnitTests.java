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

import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Example.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link Example}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class ExampleUnitTests {

	private Person person;
	private Example<Person> example;

	@Before
	public void setUp() {

		person = new Person();
		person.firstname = "rand";

		example = of(person);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test(expected = IllegalArgumentException.class)
	public void exampleOfNullThrowsException() {
		Example.of(null);
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void getSampleTypeRetunsSampleObjectsClass() {
		assertThat(example.getProbeType(), equalTo(Person.class));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void createTypedExample() {

		Example<Person> example = of(new Person(), ExampleSpec.typed(Contact.class));

		assertThat(example.getProbeType(), equalTo(Person.class));
		assertThat(example.getResultType(), equalTo((Class) Contact.class));
		assertThat(example.getProbeType(), equalTo(Person.class));
	}

	/**
	 * @see DATACMNS-810
	 */
	@Test
	public void createUntypedExample() {

		Example<Person> example = of(new Person(), ExampleSpec.untyped());

		assertThat(example.getProbeType(), equalTo(Person.class));
		assertThat(example.getResultType(), equalTo((Class) Person.class));
		assertThat(example.getProbeType(), equalTo(Person.class));
	}

	static class Contact {

		String label;
	}

	static class Person {

		String firstname;
	}

}

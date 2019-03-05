/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import lombok.Getter;

import java.util.Collection;

import org.junit.Test;
import org.springframework.data.util.MethodInvocationRecorder.Recorded;

/**
 * Unit tests for {@link MethodInvocationRecorder}.
 *
 * @author Oliver Gierke
 * @soundtrack The Intersphere - Don't Think Twice (The Grand Delusion)
 */
public class MethodInvocationRecorderUnitTests {

	Recorded<Foo> recorder = MethodInvocationRecorder.forProxyOf(Foo.class);

	@Test // DATACMNS-1449
	public void createsPropertyPathForSimpleMethodReference() {

		Recorded<Bar> wrapper = recorder.record(Foo::getBar);

		assertThat(wrapper.getPropertyPath()).hasValue("bar");
	}

	@Test // DATACMNS-1449
	public void createsPropertyPathForNestedMethodReference() {

		Recorded<FooBar> wrapper = recorder.record(Foo::getBar).record(Bar::getFooBar);

		assertThat(wrapper.getPropertyPath()).hasValue("bar.fooBar");
	}

	@Test // DATACMNS-1449
	public void createsPropertyPathForNestedCall() {

		Recorded<FooBar> wrapper = recorder.record((Foo source) -> source.getBar().getFooBar());

		assertThat(wrapper.getPropertyPath()).hasValue("bar.fooBar");
	}

	@Test // DATACMNS-1449
	public void usesCustomPropertyNamingStrategy() {

		Recorded<Bar> recorded = MethodInvocationRecorder.forProxyOf(Foo.class).record(Foo::getBar);

		assertThat(recorded.getPropertyPath(method -> method.getName())).hasValue("getBar");
	}

	@Test // DATACMNS-1449
	public void registersLookupToFinalType() {
		assertThat(recorder.record(Foo::getName).getPropertyPath()).hasValue("name");
	}

	@Test
	public void recordsInvocationOnInterface() {

		Recorded<Sample> recorder = MethodInvocationRecorder.forProxyOf(Sample.class);

		assertThat(recorder.record(Sample::getName).getPropertyPath()).hasValue("name");
	}

	@Getter
	static class Foo {
		Bar bar;
		Collection<Bar> bars;
		String name;
	}

	@Getter
	static class Bar {
		FooBar fooBar;
	}

	static class FooBar {}

	interface Sample {
		String getName();
	}
}

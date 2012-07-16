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

import java.util.Collection;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.repository.query.QueryLookupStrategy;

/**
 * Interface containing the configurable options for the Spring Data repository subsystem.
 * 
 * @author Oliver Gierke
 */
public interface RepositoryConfigurationSource {

	/**
	 * Returns the actual source object that the configuration originated from. Will be used by the tooling to give visual
	 * feedback on where the repository instances actually come from.
	 * 
	 * @return must not be {@literal null}.
	 */
	Object getSource();

	/**
	 * Returns the base packages the repository interfaces shall be found under.
	 * 
	 * @return must not be {@literal null}.
	 */
	Iterable<String> getBasePackages();

	/**
	 * Returns the {@link QueryLookupStrategy.Key} to define how query methods shall be resolved.
	 * 
	 * @return
	 */
	Object getQueryLookupStrategyKey();

	/**
	 * Returns the configured postfix to be used for looking up custom implementation classes.
	 * 
	 * @return the postfix to use or {@literal null} in case none is configured.
	 */
	String getRepositoryImplementationPostfix();

	/**
	 * @return
	 */
	String getNamedQueryLocation();

	/**
	 * Returns the name of the class of the {@link FactoryBean} to actually create repository instances.
	 * 
	 * @return
	 */
	String getRepositoryFactoryBeanName();

	/**
	 * Returns the fully-qualified names of the repository interfaces to create repository instances for.
	 * 
	 * @param loader
	 * @return
	 */
	Collection<String> getCandidates(ResourceLoader loader);
}

/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.repository.cdi;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.UnsatisfiedResolutionException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.data.repository.config.FragmentMetadata;
import org.springframework.data.repository.config.RepositoryFragmentConfiguration;
import org.springframework.data.repository.config.RepositoryFragmentDiscovery;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Context for CDI repositories. This class provides {@link ClassLoader} and
 * {@link org.springframework.data.repository.core.support.RepositoryFragment detection} which are commonly used within
 * CDI.
 * 
 * @author Mark Paluch
 * @since 2.1
 */
public class CdiRepositoryContext {

	private final ClassLoader classLoader;
	private final CustomRepositoryImplementationDetector detector;
	private final MetadataReaderFactory metadataReaderFactory;

	/**
	 * Create a new {@link CdiRepositoryContext} given {@link ClassLoader} and initialize
	 * {@link CachingMetadataReaderFactory}.
	 * 
	 * @param classLoader must not be {@literal null}.
	 */
	public CdiRepositoryContext(ClassLoader classLoader) {

		Assert.notNull(classLoader, "ClassLoader must not be null!");

		this.classLoader = classLoader;

		Environment environment = new StandardEnvironment();
		ResourceLoader resourceLoader = new PathMatchingResourcePatternResolver(classLoader);

		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		this.detector = new CustomRepositoryImplementationDetector(metadataReaderFactory, environment, resourceLoader);
	}

	/**
	 * Create a new {@link CdiRepositoryContext} given {@link ClassLoader} and
	 * {@link CustomRepositoryImplementationDetector}.
	 * 
	 * @param classLoader must not be {@literal null}.
	 * @param detector must not be {@literal null}.
	 */
	public CdiRepositoryContext(ClassLoader classLoader, CustomRepositoryImplementationDetector detector) {

		Assert.notNull(classLoader, "ClassLoader must not be null!");
		Assert.notNull(detector, "CustomRepositoryImplementationDetector must not be null!");

		ResourceLoader resourceLoader = new PathMatchingResourcePatternResolver(classLoader);

		this.classLoader = classLoader;
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		this.detector = detector;
	}

	CustomRepositoryImplementationDetector getCustomRepositoryImplementationDetector() {
		return detector;
	}

	/**
	 * Load a {@link Class} using the CDI {@link ClassLoader}.
	 * 
	 * @param className
	 * @return
	 * @throws UnsatisfiedResolutionException if the class cannot be found.
	 */
	Class<?> loadClass(String className) {

		try {
			return ClassUtils.forName(className, classLoader);
		} catch (ClassNotFoundException e) {
			throw new UnsatisfiedResolutionException(String.format("Unable to resolve class for '%s'", className), e);
		}
	}

	/**
	 * Discover {@link RepositoryFragmentConfiguration fragment configurations} for a {@link Class repository interface}.
	 * 
	 * @param configuration must not be {@literal null}.
	 * @param repositoryInterface must not be {@literal null}.
	 * @return {@link Stream} of {@link RepositoryFragmentConfiguration fragment configurations}.
	 */
	Stream<RepositoryFragmentConfiguration> getRepositoryFragments(CdiRepositoryConfiguration configuration,
			Class<?> repositoryInterface) {

		ClassMetadata classMetadata = getClassMetadata(metadataReaderFactory, repositoryInterface.getName());

		RepositoryFragmentDiscovery fragmentConfiguration = new CdiRepositoryFragmentDiscovery(configuration);

		return Arrays.stream(classMetadata.getInterfaceNames()) //
				.filter(it -> FragmentMetadata.isCandidate(it, metadataReaderFactory)) //
				.map(it -> FragmentMetadata.of(it, fragmentConfiguration)) //
				.map(this::detectRepositoryFragmentConfiguration) //
				.flatMap(Optionals::toStream);
	}

	/**
	 * Retrieves a custom repository interfaces from a repository type. This works for the whole class hierarchy and can
	 * find also a custom repository which is inherited over many levels.
	 *
	 * @param repositoryType The class representing the repository.
	 * @param cdiRepositoryConfiguration The configuration for CDI usage.
	 * @return the interface class or {@literal null}.
	 */
	Optional<Class<?>> getCustomImplementationClass(Class<?> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		String className = getCustomImplementationClassName(repositoryType, cdiRepositoryConfiguration);

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation( //
				className, //
				className, Collections.singleton(repositoryType.getPackage().getName()), //
				Collections.emptySet(), //
				BeanDefinition::getBeanClassName);

		return beanDefinition.map(it -> loadClass(it.getBeanClassName()));
	}

	private Optional<RepositoryFragmentConfiguration> detectRepositoryFragmentConfiguration(
			FragmentMetadata configuration) {

		String className = configuration.getFragmentImplementationClassName();

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation(className, null,
				configuration.getBasePackages(), configuration.getExclusions(), BeanDefinition::getBeanClassName);

		return beanDefinition.map(bd -> new RepositoryFragmentConfiguration(configuration.getFragmentInterfaceName(), bd));
	}

	private static ClassMetadata getClassMetadata(MetadataReaderFactory metadataReaderFactory, String className) {

		try {
			return metadataReaderFactory.getMetadataReader(className).getClassMetadata();
		} catch (IOException e) {
			throw new CreationException(String.format("Cannot parse %s metadata.", className), e);
		}
	}

	private static String getCustomImplementationClassName(Class<?> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		String configuredPostfix = cdiRepositoryConfiguration.getRepositoryImplementationPostfix();
		Assert.hasText(configuredPostfix, "Configured repository postfix must not be null or empty!");

		return ClassUtils.getShortName(repositoryType) + configuredPostfix;
	}

	@RequiredArgsConstructor
	private static class CdiRepositoryFragmentDiscovery implements RepositoryFragmentDiscovery {

		private final CdiRepositoryConfiguration configuration;

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.RepositoryFragmentDiscovery#getExcludeFilters()
		 */
		@Override
		public Streamable<TypeFilter> getExcludeFilters() {
			return Streamable.of(new AnnotationTypeFilter(NoRepositoryBean.class));
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.RepositoryFragmentDiscovery#getRepositoryImplementationPostfix()
		 */
		@Override
		public Optional<String> getRepositoryImplementationPostfix() {
			return Optional.of(configuration.getRepositoryImplementationPostfix());
		}
	}
}

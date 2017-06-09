/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.util.ClassUtils;

/**
 * Special {@link BeanNameGenerator} to create bean names for Spring Data repositories. Will delegate to an
 * {@link AnnotationBeanNameGenerator} but let the delegate work with a customized {@link BeanDefinition} to make sure
 * the repository interface is inspected and not the actual bean definition class.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
public class RepositoryBeanNameGenerator {

	private static final BeanNameGenerator DELEGATE = new AnnotationBeanNameGenerator();

	private final ClassLoader beanClassLoader;

	public RepositoryBeanNameGenerator(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	/**
	 * Generate a bean name for the given bean definition.
	 *
	 * @param definition the bean definition to generate a name for
	 * @return the generated bean name
	 * @since 2.0
	 */
	public String generateBeanName(BeanDefinition definition) {

		AnnotatedBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(getRepositoryInterfaceFrom(definition));
		return DELEGATE.generateBeanName(beanDefinition, null);
	}

	/**
	 * Returns the type configured for the {@code repositoryInterface} property of the given bean definition. Uses a
	 * potential {@link Class} being configured as is or tries to load a class with the given value's {@link #toString()}
	 * representation.
	 *
	 * @param beanDefinition
	 * @return
	 */
	private Class<?> getRepositoryInterfaceFrom(BeanDefinition beanDefinition) {

		if (beanDefinition instanceof ScannedGenericBeanDefinition) {
			try {
				return ((ScannedGenericBeanDefinition) beanDefinition).resolveBeanClass(beanClassLoader);
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Could not resolve bean class.", e);
			}
		} else {
			return getRepositoryInterfaceFromFactory(beanDefinition);
		}
	}

	private Class<?> getRepositoryInterfaceFromFactory(BeanDefinition beanDefinition) {

		Object value = beanDefinition.getConstructorArgumentValues().getArgumentValue(0, Class.class).getValue();

		if (value instanceof Class<?>) {
			return (Class<?>) value;
		} else {
			try {
				return ClassUtils.forName(value.toString(), beanClassLoader);
			} catch (Exception o_O) {
				throw new RuntimeException(o_O);
			}
		}
	}
}

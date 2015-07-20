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
package org.springframework.data.web.querydsl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link java.lang.annotation.Annotation} to specify aspects of {@link com.mysema.query.types.Predicate} used in Spring
 * MVC {@link org.springframework.web.servlet.mvc.Controller}.
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
@Target({ ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface QuerydslPredicate {

	/**
	 * Root type to be used for {@link com.mysema.query.types.Path}
	 * 
	 * @return
	 */
	Class<?>root() default Object.class;

	/**
	 * Configuration class providing options on a per field base.
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends QuerydslBinderCustomizer>bindings() default QuerydslBinderCustomizer.class;
}

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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.PropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;

/**
 * Builder assembling {@link Predicate} out of {@link PropertyValues}.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.11
 */
public class QuerydslPredicateBuilder {

	private final ConversionService conversionService;
	private final MultiValueBinding<?, ?> defaultBinding;
	private final Map<PropertyPath, Path<?>> paths;
	private final EntityPathResolver resolver;

	public QuerydslPredicateBuilder(ConversionService conversionService, EntityPathResolver resolver) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.defaultBinding = new QuerydslDefaultBinding();
		this.conversionService = conversionService;
		this.paths = new HashMap<PropertyPath, Path<?>>();
		this.resolver = resolver;
	}

	/**
	 * Creates a Querydsl {@link Predicate} for the given values, {@link QuerydslBindings} on the given
	 * {@link TypeInformation}.
	 * 
	 * @param type the type to create a predicate for.
	 * @param values the values to bind.
	 * @param bindings the {@link QuerydslBindings} for the predicate.
	 * @return
	 */
	public Predicate getPredicate(TypeInformation<?> type, MultiValueMap<String, String> values,
			QuerydslBindings bindings) {

		Assert.notNull(bindings, "Context must not be null!");

		BooleanBuilder builder = new BooleanBuilder();

		if (values.isEmpty()) {
			return builder.getValue();
		}

		for (Entry<String, List<String>> entry : values.entrySet()) {

			if (isSingleElementCollectionWithoutText(entry.getValue())) {
				continue;
			}

			try {

				PropertyPath propertyPath = PropertyPath.from(entry.getKey(), type);

				if (bindings.isPathVisible(propertyPath)) {

					Collection<Object> value = convertToPropertyPathSpecificType(entry.getValue(), propertyPath);

					Predicate predicate = invokeBinding(propertyPath, bindings, value);

					if (predicate != null) {
						builder.and(predicate);
					}
				}
			} catch (PropertyReferenceException o_O) {
				// not a property of the domain object, continue
			}
		}

		return builder.getValue();
	}

	/**
	 * Invokes the binding of the given values, for the given {@link PropertyPath} and {@link QuerydslBindings}.
	 * 
	 * @param dotPath must not be {@literal null}.
	 * @param bindings must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Predicate invokeBinding(PropertyPath dotPath, QuerydslBindings bindings, Collection<Object> values) {

		Path<?> path = getPath(dotPath, bindings);

		MultiValueBinding binding = bindings.getBindingForPath(dotPath);
		binding = binding == null ? defaultBinding : binding;

		return binding.bind(path, values);
	}

	/**
	 * Returns the {@link Path} for the given {@link PropertyPath} and {@link QuerydslBindings}. Will try to obtain the
	 * {@link Path} from the bindings first but fall back to reifying it from the PropertyPath in case no specific binding
	 * has been configured.
	 * 
	 * @param path must not be {@literal null}.
	 * @param bindings must not be {@literal null}.
	 * @return
	 */
	private Path<?> getPath(PropertyPath path, QuerydslBindings bindings) {

		Path<?> resolvedPath = bindings.getExistingPath(path);

		if (resolvedPath != null) {
			return resolvedPath;
		}

		resolvedPath = paths.get(resolvedPath);

		if (resolvedPath != null) {
			return resolvedPath;
		}

		resolvedPath = reifyPath(path, null);
		paths.put(path, resolvedPath);

		return resolvedPath;
	}

	/**
	 * Tries to reify a Querydsl {@link Path} from the given {@link PropertyPath} and base.
	 * 
	 * @param path must not be {@literal null}.
	 * @param base can be {@literal null}.
	 * @return
	 */
	private Path<?> reifyPath(PropertyPath path, Path<?> base) {

		Path<?> entityPath = base != null ? base : resolver.createPath(path.getOwningType().getType());

		Field field = ReflectionUtils.findField(entityPath.getClass(), path.getSegment());
		Object value = ReflectionUtils.getField(field, entityPath);

		if (path.hasNext()) {
			return reifyPath(path.next(), (Path<?>) value);
		}

		return (Path<?>) value;
	}

	/**
	 * Converts the given source values into a collection of elements that are of the given {@link PropertyPath}'s type.
	 * Considers a single element list with an empty {@link String} an empty collection because this basically indicates
	 * the property having been submitted but no value provided.
	 * 
	 * @param source must not be {@literal null}.
	 * @param path must not be {@literal null}.
	 * @return
	 */
	private Collection<Object> convertToPropertyPathSpecificType(List<String> source, PropertyPath path) {

		Class<?> targetType = path.getLeafProperty().getType();

		if (source.isEmpty() || isSingleElementCollectionWithoutText(source)) {
			return Collections.emptyList();
		}

		Collection<Object> target = new ArrayList<Object>(source.size());

		for (String value : source) {

			target.add(conversionService.canConvert(value.getClass(), targetType)
					? conversionService.convert(value, targetType) : value);
		}

		return target;
	}

	/**
	 * Returns whether the given collection has exactly one element that doesn't contain any text. This is basically an
	 * indicator that a request parameter has been submitted but no value for it.
	 * 
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static boolean isSingleElementCollectionWithoutText(List<String> source) {
		return source.size() == 1 && !StringUtils.hasText(source.get(0));
	}
}

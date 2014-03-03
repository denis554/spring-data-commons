/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.TypeFilterParser;
import org.springframework.data.config.TypeFilterParser.Type;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * XML based {@link RepositoryConfigurationSource}. Uses configuration defined on {@link Element} attributes.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class XmlRepositoryConfigurationSource extends RepositoryConfigurationSourceSupport {

	private static final String QUERY_LOOKUP_STRATEGY = "query-lookup-strategy";
	private static final String BASE_PACKAGE = "base-package";
	private static final String NAMED_QUERIES_LOCATION = "named-queries-location";
	private static final String REPOSITORY_IMPL_POSTFIX = "repository-impl-postfix";
	private static final String REPOSITORY_FACTORY_BEAN_CLASS_NAME = "factory-class";
	private static final String CONSIDER_NESTED_REPOSITORIES = "consider-nested-repositories";

	private static final Pattern CAMEL_CASE_PARTS = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

	private final Element element;
	private final ParserContext context;

	private final Iterable<TypeFilter> includeFilters;
	private final Iterable<TypeFilter> excludeFilters;

	/**
	 * Creates a new {@link XmlRepositoryConfigurationSource} using the given {@link Element} and {@link ParserContext}.
	 * 
	 * @param element must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 */
	public XmlRepositoryConfigurationSource(Element element, ParserContext context, Environment environment) {

		super(environment);

		Assert.notNull(element);
		Assert.notNull(context);

		this.element = element;
		this.context = context;

		TypeFilterParser parser = new TypeFilterParser(context.getReaderContext());
		this.includeFilters = parser.parseTypeFilters(element, Type.INCLUDE);
		this.excludeFilters = parser.parseTypeFilters(element, Type.EXCLUDE);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getSource()
	 */
	public Object getSource() {
		return context.extractSource(element);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getBasePackages()
	 */
	public Iterable<String> getBasePackages() {

		String attribute = element.getAttribute(BASE_PACKAGE);
		return Arrays.asList(StringUtils.delimitedListToStringArray(attribute, ",", " "));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getQueryLookupStrategyKey()
	 */
	public Object getQueryLookupStrategyKey() {
		return QueryLookupStrategy.Key.create(getNullDefaultedAttribute(element, QUERY_LOOKUP_STRATEGY));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getNamedQueryLocation()
	 */
	public String getNamedQueryLocation() {
		return getNullDefaultedAttribute(element, NAMED_QUERIES_LOCATION);
	}

	/**
	 * Returns the XML element backing the configuration.
	 * 
	 * @return the element
	 */
	public Element getElement() {
		return element;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSourceSupport#getExcludeFilters()
	 */
	@Override
	protected Iterable<TypeFilter> getExcludeFilters() {
		return excludeFilters;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSourceSupport#getIncludeFilters()
	 */
	@Override
	protected Iterable<TypeFilter> getIncludeFilters() {
		return includeFilters;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryImplementationPostfix()
	 */
	public String getRepositoryImplementationPostfix() {
		return getNullDefaultedAttribute(element, REPOSITORY_IMPL_POSTFIX);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryFactoryBeanName()
	 */
	public String getRepositoryFactoryBeanName() {
		return getNullDefaultedAttribute(element, REPOSITORY_FACTORY_BEAN_CLASS_NAME);
	}

	private String getNullDefaultedAttribute(Element element, String attributeName) {
		String attribute = element.getAttribute(attributeName);
		return StringUtils.hasText(attribute) ? attribute : null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSourceSupport#isConsideringNestedRepositoriesEnabled()
	 */
	@Override
	public boolean shouldConsiderNestedRepositories() {

		String attribute = getNullDefaultedAttribute(element, CONSIDER_NESTED_REPOSITORIES);
		return attribute != null && Boolean.parseBoolean(attribute);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getAttribute(java.lang.String)
	 */
	@Override
	public String getAttribute(String name) {

		List<String> lowercase = new ArrayList<String>();

		for (String part : Arrays.asList(CAMEL_CASE_PARTS.split(name))) {
			lowercase.add(part.toLowerCase(Locale.US));
		}

		String xmlAttributeName = StringUtils.collectionToDelimitedString(lowercase, "-");
		String attribute = element.getAttribute(xmlAttributeName);

		return StringUtils.hasText(attribute) ? attribute : null;
	}
}

/*
 * Copyright 2011-2012 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Simple value object to capture information of {@link PersistentEntity}s.
 * 
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Patryk Wasik
 */
public class BasicPersistentEntity<T, P extends PersistentProperty<P>> implements MutablePersistentEntity<T, P> {

	private final PreferredConstructor<T, P> constructor;
	private final TypeInformation<T> information;
	private final Set<P> properties;
	private final Set<Association<P>> associations;

	private P idProperty;
	private P versionProperty;

	/**
	 * Creates a new {@link BasicPersistentEntity} from the given {@link TypeInformation}.
	 * 
	 * @param information must not be {@literal null}.
	 */
	public BasicPersistentEntity(TypeInformation<T> information) {
		this(information, null);
	}

	/**
	 * Creates a new {@link BasicPersistentEntity} for the given {@link TypeInformation} and {@link Comparator}. The given
	 * {@link Comparator} will be used to define the order of the {@link PersistentProperty} instances added to the
	 * entity.
	 * 
	 * @param information must not be {@literal null}
	 * @param comparator
	 */
	public BasicPersistentEntity(TypeInformation<T> information, Comparator<P> comparator) {

		Assert.notNull(information);

		this.information = information;
		this.properties = comparator == null ? new HashSet<P>() : new TreeSet<P>(comparator);
		this.constructor = new PreferredConstructorDiscoverer<T, P>(information, this).getConstructor();
		this.associations = comparator == null ? new HashSet<Association<P>>() : new TreeSet<Association<P>>(
				new AssociationComparator<P>(comparator));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistenceConstructor()
	 */
	public PreferredConstructor<T, P> getPersistenceConstructor() {
		return constructor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isConstructorArgument(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isConstructorArgument(PersistentProperty<?> property) {
		return constructor == null ? false : constructor.isConstructorParameter(property);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isIdProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isIdProperty(PersistentProperty<?> property) {
		return this.idProperty == null ? false : this.idProperty.equals(property);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#isVersionProperty(org.springframework.data.mapping.PersistentProperty)
	 */
	public boolean isVersionProperty(PersistentProperty<?> property) {
		return this.versionProperty == null ? false : this.versionProperty.equals(property);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getName()
	 */
	public String getName() {
		return getType().getName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getIdProperty()
	 */
	public P getIdProperty() {
		return idProperty;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getVersionProperty()
	 */
	public P getVersionProperty() {
		return versionProperty;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#hasIdProperty()
	 */
	public boolean hasIdProperty() {
		return idProperty != null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#hasVersionProperty()
	 */
	public boolean hasVersionProperty() {
		return versionProperty != null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addPersistentProperty(P)
	 */
	public void addPersistentProperty(P property) {

		Assert.notNull(property);
		properties.add(property);

		if (property.isIdProperty()) {

			if (this.idProperty != null) {
				throw new MappingException(String.format(
						"Attempt to add id property %s but already have property %s registered "
								+ "as id. Check your mapping configuration!", property.getField(), idProperty.getField()));
			}

			this.idProperty = property;
		}

		if (property.isVersionProperty()) {

			if (this.versionProperty != null) {
				throw new MappingException(String.format(
						"Attempt to add version property %s but already have property %s registered "
								+ "as version. Check your mapping configuration!", property.getField(), versionProperty.getField()));
			}

			this.versionProperty = property;
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#addAssociation(org.springframework.data.mapping.model.Association)
	 */
	public void addAssociation(Association<P> association) {
		associations.add(association);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getPersistentProperty(java.lang.String)
	 */
	public P getPersistentProperty(String name) {

		for (P property : properties) {
			if (property.getName().equals(name)) {
				return property;
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getType()
	 */
	public Class<T> getType() {
		return information.getType();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getTypeAlias()
	 */
	public Object getTypeAlias() {

		TypeAlias alias = getType().getAnnotation(TypeAlias.class);
		return alias == null ? null : StringUtils.hasText(alias.value()) ? alias.value() : null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#getTypeInformation()
	 */
	public TypeInformation<T> getTypeInformation() {
		return information;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithProperties(org.springframework.data.mapping.PropertyHandler)
	 */
	public void doWithProperties(PropertyHandler<P> handler) {
		Assert.notNull(handler);
		for (P property : properties) {
			if (!property.isTransient() && !property.isAssociation()) {
				handler.doWithPersistentProperty(property);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.PersistentEntity#doWithAssociations(org.springframework.data.mapping.AssociationHandler)
	 */
	public void doWithAssociations(AssociationHandler<P> handler) {
		Assert.notNull(handler);
		for (Association<P> association : associations) {
			handler.doWithAssociation(association);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.MutablePersistentEntity#verify()
	 */
	public void verify() {

	}

	/**
	 * Simple {@link Comparator} adaptor to delegate ordering to the inverse properties of the association.
	 * 
	 * @author Oliver Gierke
	 */
	private static final class AssociationComparator<P extends PersistentProperty<P>> implements
			Comparator<Association<P>>, Serializable {

		private static final long serialVersionUID = 4508054194886854513L;
		private final Comparator<P> delegate;

		public AssociationComparator(Comparator<P> delegate) {
			Assert.notNull(delegate);
			this.delegate = delegate;
		}

		public int compare(Association<P> left, Association<P> right) {
			return delegate.compare(left.getInverse(), right.getInverse());
		}
	}
}

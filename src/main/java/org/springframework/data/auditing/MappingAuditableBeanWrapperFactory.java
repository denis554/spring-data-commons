/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.auditing;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.util.Assert;

/**
 * {@link AuditableBeanWrapperFactory} that will create am {@link AuditableBeanWrapper} using mapping information
 * obtained from a {@link MappingContext} to detect auditing configuration and eventually invoking setting the auditing
 * values.
 * 
 * @author Oliver Gierke
 * @since 1.8
 */
class MappingAuditableBeanWrapperFactory extends AuditableBeanWrapperFactory {

	private final MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext;
	private final Map<Class<?>, MappingAuditingMetadata> metadataCache;

	/**
	 * Creates a new {@link MappingAuditableBeanWrapperFactory} using the given {@link MappingContext}.
	 * 
	 * @param mappingContext must not be {@literal null}.
	 */
	public MappingAuditableBeanWrapperFactory(
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.mappingContext = mappingContext;
		this.metadataCache = new HashMap<Class<?>, MappingAuditingMetadata>();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.AuditableBeanWrapperFactory#getBeanWrapperFor(java.lang.Object)
	 */
	@Override
	public AuditableBeanWrapper getBeanWrapperFor(Object source) {

		if (source instanceof Auditable) {
			return super.getBeanWrapperFor(source);
		}

		Class<?> type = source.getClass();
		PersistentEntity<?, ?> entity = mappingContext.getPersistentEntity(type);

		if (entity == null) {
			return super.getBeanWrapperFor(source);
		}

		MappingAuditingMetadata metadata = metadataCache.get(type);

		if (metadata == null) {
			metadata = new MappingAuditingMetadata(entity);
			metadataCache.put(type, metadata);
		}

		return metadata.isAuditable() ? new MappingMetadataAuditableBeanWrapper(source, metadata) : null;
	}

	/**
	 * Captures {@link PersistentProperty} instances equipped with auditing annotations.
	 * 
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	static class MappingAuditingMetadata {

		private final PersistentProperty<?> createdByProperty, createdDateProperty, lastModifiedByProperty,
				lastModifiedDateProperty;

		/**
		 * Creates a new {@link MappingAuditingMetadata} instance from the given {@link PersistentEntity}.
		 * 
		 * @param entity must not be {@literal null}.
		 */
		public MappingAuditingMetadata(PersistentEntity<?, ? extends PersistentProperty<?>> entity) {

			Assert.notNull(entity, "PersistentEntity must not be null!");

			this.createdByProperty = entity.getPersistentProperty(CreatedBy.class);
			this.createdDateProperty = entity.getPersistentProperty(CreatedDate.class);
			this.lastModifiedByProperty = entity.getPersistentProperty(LastModifiedBy.class);
			this.lastModifiedDateProperty = entity.getPersistentProperty(LastModifiedDate.class);
		}

		/**
		 * Returns whether the {@link PersistentEntity} is auditable at all (read: any of the auditing annotations is
		 * present).
		 * 
		 * @return
		 */
		public boolean isAuditable() {
			return createdByProperty != null || createdDateProperty != null || lastModifiedByProperty != null
					|| lastModifiedDateProperty != null;
		}
	}

	/**
	 * {@link AuditableBeanWrapper} using {@link MappingAuditingMetadata} and a {@link BeanWrapper} to set values on
	 * auditing properties.
	 * 
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	static class MappingMetadataAuditableBeanWrapper extends DateConvertingAuditableBeanWrapper {

		private final BeanWrapper<Object> wrapper;
		private final MappingAuditingMetadata metadata;

		/**
		 * Creates a new {@link MappingMetadataAuditableBeanWrapper} for the given taregt and
		 * {@link MappingAuditingMetadata}.
		 * 
		 * @param target must not be {@literal null}.
		 * @param metadata must not be {@literal null}.
		 */
		public MappingMetadataAuditableBeanWrapper(Object target, MappingAuditingMetadata metadata) {

			Assert.notNull(target, "Target object must not be null!");
			Assert.notNull(metadata, "Auditing metadata must not be null!");

			this.wrapper = BeanWrapper.create(target, null);
			this.metadata = metadata;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedBy(java.lang.Object)
		 */
		@Override
		public void setCreatedBy(Object value) {

			if (metadata.createdByProperty != null) {
				this.wrapper.setProperty(metadata.createdByProperty, value);
			}
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedDate(java.util.Calendar)
		 */
		@Override
		public void setCreatedDate(Calendar value) {

			PersistentProperty<?> property = metadata.createdDateProperty;

			if (property != null) {
				this.wrapper.setProperty(property, getDateValueToSet(value, property.getType(), property));
			}
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedBy(java.lang.Object)
		 */
		@Override
		public void setLastModifiedBy(Object value) {

			if (metadata.lastModifiedByProperty != null) {
				this.wrapper.setProperty(metadata.lastModifiedByProperty, value);
			}
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(java.util.Calendar)
		 */
		@Override
		public void setLastModifiedDate(Calendar value) {

			PersistentProperty<?> property = metadata.lastModifiedDateProperty;

			if (property != null) {
				this.wrapper.setProperty(property, getDateValueToSet(value, property.getType(), property));
			}
		}
	}
}

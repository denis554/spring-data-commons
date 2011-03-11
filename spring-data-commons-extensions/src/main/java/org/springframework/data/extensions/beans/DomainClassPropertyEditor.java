/*
 * Copyright 2008-2011 the original author or authors.
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
package org.springframework.data.extensions.beans;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.Serializable;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.support.EntityInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Generic {@link PropertyEditor} to map entities handled by a
 * {@link Repository} to their id's and vice versa.
 * 
 * @author Oliver Gierke
 */
public class DomainClassPropertyEditor<T, ID extends Serializable> extends
        PropertyEditorSupport {

    private final Repository<T, ID> repository;
    private final EntityInformation<T, ID> information;
    private final PropertyEditorRegistry registry;


    /**
     * Creates a new {@link DomainClassPropertyEditor} for the given
     * {@link Repository}.
     * 
     * @param repository
     * @param registry
     */
    public DomainClassPropertyEditor(Repository<T, ID> repository,
            EntityInformation<T, ID> information,
            PropertyEditorRegistry registry) {

        Assert.notNull(repository);
        Assert.notNull(registry);

        this.repository = repository;
        this.information = information;
        this.registry = registry;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
     */
    @Override
    public void setAsText(String idAsString) throws IllegalArgumentException {

        if (!StringUtils.hasText(idAsString)) {
            setValue(null);
            return;
        }

        setValue(repository.findById(getId(idAsString)));
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.beans.PropertyEditorSupport#getAsText()
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getAsText() {

        T entity = (T) getValue();

        if (null == entity) {
            return null;
        }

        Object id = getId(entity);
        return id == null ? null : id.toString();
    }


    /**
     * Looks up the id of the given entity using one of the
     * {@link org.synyx.hades.dao.orm.GenericDaoSupport.IdAware} implementations
     * of Hades.
     * 
     * @param entity
     * @return
     */
    private ID getId(T entity) {

        return information.getId(entity);
    }


    /**
     * Returns the actual typed id. Looks up an available customly registered
     * {@link PropertyEditor} from the {@link PropertyEditorRegistry} before
     * falling back on a {@link SimpleTypeConverter} to translate the
     * {@link String} id into the type one.
     * 
     * @param idAsString
     * @return
     */
    @SuppressWarnings("unchecked")
    private ID getId(String idAsString) {

        Class<ID> idClass = information.getIdType();

        PropertyEditor idEditor = registry.findCustomEditor(idClass, null);

        if (idEditor != null) {
            idEditor.setAsText(idAsString);
            return (ID) idEditor.getValue();
        }

        return new SimpleTypeConverter()
                .convertIfNecessary(idAsString, idClass);
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        DomainClassPropertyEditor<?, ?> that =
                (DomainClassPropertyEditor<?, ?>) obj;

        return this.repository.equals(that.repository)
                && this.registry.equals(that.registry)
                && this.information.equals(that.information);
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        int hashCode = 17;
        hashCode += repository.hashCode() * 32;
        hashCode += information.hashCode() * 32;
        hashCode += registry.hashCode() * 32;
        return hashCode;
    }
}

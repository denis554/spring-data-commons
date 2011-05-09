package org.springframework.data.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.data.mapping.Person;

/**
 * Unit tests for {@link ClassTypeInformation}.
 * 
 * @author Oliver Gierke
 */
public class ClassTypeInformationUnitTests {

  @Test
  public void discoversTypeForSimpleGenericField() {

    TypeInformation<ConcreteType> discoverer = ClassTypeInformation.from(ConcreteType.class);
    assertEquals(ConcreteType.class, discoverer.getType());
    TypeInformation<?> content = discoverer.getProperty("content");
    assertEquals(String.class, content.getType());
    assertNull(content.getComponentType());
    assertNull(content.getMapValueType());
  }

  @Test
  public void discoversTypeForNestedGenericField() {

    TypeInformation<ConcreteWrapper> discoverer = ClassTypeInformation.from(ConcreteWrapper.class);
    assertEquals(ConcreteWrapper.class, discoverer.getType());
    TypeInformation<?> wrapper = discoverer.getProperty("wrapped");
    assertEquals(GenericType.class, wrapper.getType());
    TypeInformation<?> content = wrapper.getProperty("content");

    assertEquals(String.class, content.getType());
    assertEquals(String.class,
        discoverer.getProperty("wrapped").getProperty("content").getType());
    assertEquals(String.class, discoverer.getProperty("wrapped.content")
        .getType());
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void discoversBoundType() {

    TypeInformation<GenericTypeWithBound> information = ClassTypeInformation.from(
        GenericTypeWithBound.class);
    assertEquals(Person.class, information.getProperty("person").getType());
  }

  @Test
  public void discoversBoundTypeForSpecialization() {

    TypeInformation<SpecialGenericTypeWithBound> information = ClassTypeInformation.from(
        SpecialGenericTypeWithBound.class);
    assertEquals(SpecialPerson.class, information.getProperty("person")
        .getType());
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void discoversBoundTypeForNested() {

    TypeInformation<AnotherGenericType> information = ClassTypeInformation.from(
        AnotherGenericType.class);
    assertEquals(GenericTypeWithBound.class, information.getProperty("nested")
        .getType());
    assertEquals(Person.class, information.getProperty("nested.person")
        .getType());
  }
  
  @Test
  public void discoversArraysAndCollections() {
    TypeInformation<StringCollectionContainer> information = ClassTypeInformation.from(StringCollectionContainer.class);
    
    TypeInformation<?> property = information.getProperty("array");
    assertEquals(property.getComponentType().getType(), String.class);
    
    Class<?> type = property.getType();
    assertEquals(String[].class, type);
    assertThat(type.isArray(), is(true));
    
    property = information.getProperty("foo");
    assertEquals(Collection[].class, property.getType());
    assertEquals(Collection.class, property.getComponentType().getType());
    assertEquals(String.class, property.getComponentType().getComponentType().getType());
    
    property = information.getProperty("rawSet");
    assertEquals(Set.class, property.getType());
    assertEquals(Object.class, property.getComponentType().getType());
    assertNull(property.getMapValueType());
  }
  
  @Test
  public void discoversMapValueType() {
    
    TypeInformation<StringMapContainer> information = ClassTypeInformation.from(StringMapContainer.class);
    TypeInformation<?> genericMap = information.getProperty("genericMap");
    assertEquals(Map.class, genericMap.getType());
    assertEquals(String.class, genericMap.getMapValueType().getType());

    TypeInformation<?> map = information.getProperty("map");
    assertEquals(Map.class, map.getType());
    assertEquals(Calendar.class, map.getMapValueType().getType());
  }
  
  static class StringMapContainer extends MapContainer<String> {
    
  }
  
  static class MapContainer<T> {
    Map<String, T> genericMap;
    Map<String, Calendar> map;
  }

  static class StringCollectionContainer extends CollectionContainer<String> {
    
  }
  
  @SuppressWarnings("rawtypes")
  static class CollectionContainer<T> {
    
    T[] array;
    Collection<T>[] foo;
    Set<String> set;
    Set rawSet;
  }

  static class GenericTypeWithBound<T extends Person> {

    T person;
  }

  static class AnotherGenericType<T extends Person, S extends GenericTypeWithBound<T>> {
    S nested;
  }

  static class SpecialGenericTypeWithBound extends
      GenericTypeWithBound<SpecialPerson> {

  }

  static abstract class SpecialPerson extends Person {
    protected SpecialPerson(Integer ssn, String firstName, String lastName) {
      super(ssn, firstName, lastName);
    }
  }

  static class GenericType<T, S> {

    Long index;
    T content;
  }
  

  static class ConcreteType extends GenericType<String, Object> {

  }

  static class GenericWrapper<S> {

    GenericType<S, Object> wrapped;
  }

  static class ConcreteWrapper extends GenericWrapper<String> {

  }
}

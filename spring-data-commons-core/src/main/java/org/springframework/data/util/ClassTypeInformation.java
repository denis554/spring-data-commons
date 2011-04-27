package org.springframework.data.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Property information for a plain {@link Class}.
 * 
 * @author Oliver Gierke
 */
public class ClassTypeInformation extends TypeDiscoverer {

  private final Class<?> type;

  /**
   * Creates {@link ClassTypeInformation} for the given type.
   * 
   * @param type
   */
  public ClassTypeInformation(Class<?> type) {
    this(type, GenericTypeResolver.getTypeVariableMap(type), null);
  }

  ClassTypeInformation(Class<?> type, TypeDiscoverer parent) {
    this(type, null, parent);
  }

  @SuppressWarnings("rawtypes")
  ClassTypeInformation(Class<?> type, Map<TypeVariable, Type> typeVariableMap,
      TypeDiscoverer parent) {
    super(type, typeVariableMap, parent);
    this.type = type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.springframework.data.document.mongodb.TypeDiscovererTest.FieldInformation#getType()
   */
  @Override
  public Class<?> getType() {
    return type;
  }

  /* (non-Javadoc)
   * @see org.springframework.data.util.TypeDiscoverer#getComponentType()
   */
  @Override
  public TypeInformation getComponentType() {
    
    if (type.isArray()) {
      return createInfo(resolveArrayType(type));
    }

    TypeVariable<?>[] typeParameters = type.getTypeParameters();
    return typeParameters.length > 0 ? new TypeVariableTypeInformation(typeParameters[0], this.getType(), this) : null;
  }
  
  private static Type resolveArrayType(Class<?> type) {
    Assert.isTrue(type.isArray());
    Class<?> componentType = type.getComponentType();
    return componentType.isArray() ? resolveArrayType(componentType) : componentType;
  }

  /* (non-Javadoc)
   * @see org.springframework.data.util.TypeDiscoverer#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {

    if (!super.equals(obj)) {
      return false;
    }

    ClassTypeInformation that = (ClassTypeInformation) obj;
    return this.type.equals(that.type);
  }

  /* (non-Javadoc)
   * @see org.springframework.data.util.TypeDiscoverer#hashCode()
   */
  @Override
  public int hashCode() {
    int result = super.hashCode();
    return result += 31 * type.hashCode();
  }
}
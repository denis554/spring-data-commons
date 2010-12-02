package org.springframework.data.repository.query.parser;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.repository.util.ClassUtils;
import org.springframework.util.StringUtils;


/**
 * A single part of a method name that has to be transformed into a query part.
 * The actual transformation is defined by a {@link Type} that is determined
 * from inspecting the given part. The query part can then be looked up via
 * {@link #getQueryPart()}.
 * 
 * @author Oliver Gierke
 */
public class Part {

    private final String property;
    private final Part.Type type;


    /**
     * Creates a new {@link Part} from the given method name part, the
     * {@link Class} the part originates from and the start parameter index.
     * 
     * @param part
     * @param clazz
     */
    public Part(String part, Class<?> clazz) {

        this.type = Type.fromProperty(part, clazz);
        this.property = type.extractProperty(part);
    }


    public boolean getParameterRequired() {

        return getNumberOfArguments() > 0;
    }


    /**
     * Returns how many method parameters are bound by this part.
     * 
     * @return
     */
    public int getNumberOfArguments() {

        return type.getNumberOfArguments();
    }


    /**
     * @return the part
     */
    public String getProperty() {

        return property;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        Part that = (Part) obj;

        return this.property.equals(that.property)
                && this.type.equals(that.type);
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        int result = 37;
        result += 17 * property.hashCode();
        result += 17 * type.hashCode();
        return result;
    }


    /**
     * @return the type
     */
    public Part.Type getType() {

        return type;
    }

    /**
     * The type of a method name part. Used to create query parts in various
     * ways.
     * 
     * @author Oliver Gierke
     */
    public static enum Type {

        BETWEEN(null, 2, "Between"),

        IS_NOT_NULL(null, 0, "IsNotNull", "NotNull"),

        IS_NULL(null, 0, "IsNull", "Null"),

        LESS_THAN("<", "LessThan"),

        GREATER_THAN(">", "GreaterThan"),

        NOT_LIKE("not like", "NotLike"),

        LIKE("like", "Like"),

        NEGATING_SIMPLE_PROPERTY("<>", "Not"),

        SIMPLE_PROPERTY("=");

        // Need to list them again explicitly as the order is important
        // (esp. for IS_NULL, IS_NOT_NULL)
        private static final List<Part.Type> ALL = Arrays.asList(IS_NOT_NULL,
                IS_NULL, BETWEEN, LESS_THAN, GREATER_THAN, NOT_LIKE, LIKE,
                NEGATING_SIMPLE_PROPERTY, SIMPLE_PROPERTY);
        private List<String> keywords;
        private String operator;
        private int numberOfArguments;


        /**
         * Creates a new {@link Type} using the given keyword, number of
         * arguments to be bound and operator. Keyword and operator can be
         * {@literal null}.
         * 
         * @param operator
         * @param numberOfArguments
         * @param keywords
         */
        private Type(String operator, int numberOfArguments, String... keywords) {

            this.operator = operator;
            this.numberOfArguments = numberOfArguments;
            this.keywords = Arrays.asList(keywords);
        }


        private Type(String operator, String... keywords) {

            this(operator, 1, keywords);
        }


        /**
         * Returns the {@link Type} of the {@link Part} for the given raw
         * property and the given {@link Class}. This will try to detect e.g.
         * keywords contained in the raw property that trigger special query
         * creation. Returns {@link #SIMPLE_PROPERTY} by default.
         * 
         * @param rawProperty
         * @param clazz
         * @return
         */
        public static Part.Type fromProperty(String rawProperty, Class<?> clazz) {

            for (Part.Type type : ALL) {
                if (type.supports(rawProperty, clazz)) {
                    return type;
                }
            }

            return SIMPLE_PROPERTY;
        }


        public String getOperator() {

            return this.operator;
        }


        /**
         * Returns whether the the type supports the given raw property. Default
         * implementation checks whether the property ends with the registered
         * keyword. Does not support the keyword if the property is a valid
         * field as is.
         * 
         * @param property
         * @param clazz
         * @return
         */
        protected boolean supports(String property, Class<?> clazz) {

            if (keywords == null) {
                return true;
            }

            if (ClassUtils.hasProperty(clazz, property)) {
                return false;
            }

            for (String keyword : keywords) {
                if (property.endsWith(keyword)) {
                    return true;
                }
            }

            return false;
        }


        /**
         * Returns the number of arguments the property binds. By default this
         * exactly one argument.
         * 
         * @return
         */
        public int getNumberOfArguments() {

            return numberOfArguments;
        }


        /**
         * Callback method to extract the actual property to be bound from the
         * given part. Strips the keyword from the part's end if available.
         * 
         * @param part
         * @return
         */
        public String extractProperty(String part) {

            String candidate = StringUtils.uncapitalize(part);

            for (String keyword : keywords) {
                if (candidate.endsWith(keyword)) {
                    return candidate.substring(0, candidate.indexOf(keyword));
                }
            }

            return candidate;
        }
    }
}
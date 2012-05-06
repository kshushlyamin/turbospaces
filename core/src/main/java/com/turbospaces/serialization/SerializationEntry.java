package com.turbospaces.serialization;

/**
 * serialization/de-serialization entry wrapper. holder for de-serialized object and property values of the object.
 * 
 * @since 0.1
 */
@SuppressWarnings("javadoc")
public class SerializationEntry {
    private final Object object;
    private final Object[] propertyValues;

    public SerializationEntry(final Object object, final Object[] propertyValues) {
        super();
        this.object = object;
        this.propertyValues = propertyValues;
    }

    public Object getObject() {
        return object;
    }

    public Object[] getPropertyValues() {
        return propertyValues;
    }
}
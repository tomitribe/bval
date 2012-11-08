/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.bval.jsr303.xml;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import com.github.mbenson.privileged.Privileged;

/**
 * Description: <br/>
 * InvocationHandler implementation of <code>Annotation</code> that pretends it
 * is a "real" source code annotation.
 * <p/>
 */
// TODO move this guy up to org.apache.bval.jsr303 or
// org.apache.bval.jsr303.model
class AnnotationProxy implements Annotation, InvocationHandler, Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 1L;

    private final Class<? extends Annotation> annotationType;
    private final Map<String, Object> values;

    /**
     * Create a new AnnotationProxy instance.
     * 
     * @param <A>
     * @param descriptor
     */
    public <A extends Annotation> AnnotationProxy(AnnotationProxyBuilder<A> descriptor) {
        this.annotationType = descriptor.getType();
        values = getAnnotationValues(descriptor);
    }

    private static <A extends Annotation> Map<String, Object> getAnnotationValues(AnnotationProxyBuilder<A> descriptor) {
        Map<String, Object> result = new TreeMap<String, Object>();
        int processedValuesFromDescriptor = 0;
        for (Method m : getDeclaredMethods(descriptor.getType())) {
            if (descriptor.contains(m.getName())) {
                result.put(m.getName(), descriptor.getValue(m.getName()));
                processedValuesFromDescriptor++;
            } else if (m.getDefaultValue() != null) {
                result.put(m.getName(), m.getDefaultValue());
            } else {
                throw new IllegalArgumentException("No value provided for " + m.getName());
            }
        }
        if (processedValuesFromDescriptor != descriptor.size()) {
            throw new RuntimeException("Trying to instantiate " + descriptor.getType() + " with unknown paramters.");
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (values.containsKey(method.getName())) {
            return values.get(method.getName());
        }
        return method.invoke(this, args);
    }

    /**
     * {@inheritDoc}
     */
    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder result = new StringBuilder().append('@').append(annotationType().getName()).append('(');
        boolean comma = false;
        for (Map.Entry<String, Object> e : values.entrySet()) {
            if (comma) {
                result.append(", ");
            } else {
                comma = true;
            }
            result.append(e.getKey()).append('=').append(e.getValue());
        }
        return result.append(")").toString();
    }

    @Privileged
    private static Method[] getDeclaredMethods(Class<?> type) {
        Method[] result = type.getDeclaredMethods();
        if (result.length > 0) {
            AccessibleObject.setAccessible(result, true);
        }
        return result;
    }

}

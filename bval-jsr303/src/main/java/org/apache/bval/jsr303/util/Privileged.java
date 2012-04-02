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
package org.apache.bval.jsr303.util;

import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

/**
 * Extended utility object for Bean Validation-specific privileged work.
 */
@SuppressWarnings("restriction")
public class Privileged extends org.apache.bval.util.Privileged {
    /**
     * Get the named field declared by the specified class. The result of the action will be {@code null} if there is no
     * such field.
     */
    public Field getDeclaredField(final Class<?> clazz, final String fieldName) {
        return run(new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    final Field f = clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f;
                } catch (final NoSuchFieldException ex) {
                    return null;
                }
            }
        });
    }

    /**
     * Get all fields declared by the specified class.
     */
    public Field[] getDeclaredFields(final Class<?> clazz) {
        return run(new PrivilegedAction<Field[]>() {
            public Field[] run() {
                final Field[] fields = clazz.getDeclaredFields();
                if (fields.length > 0) {
                    AccessibleObject.setAccessible(fields, true);
                }
                return fields;
            }
        });
    }

    /**
     * Get all methods declared by the specified class.
     */
    public Method[] getDeclaredMethods(final Class<?> clazz) {
        return run(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                final Method[] result = clazz.getDeclaredMethods();
                AccessibleObject.setAccessible(result, true);
                return result;
            }
        });
    }

    /**
     * Get the named method declared by the specified class or by one of its ancestors. The result of the action will be
     * {@code null} if there is no such method.
     * 
     * @param clazz
     * @param methodName
     * @param parameterTypes
     * @return public method or {@code null}.
     */
    public Method getPublicMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        return run(new PrivilegedAction<Method>() {
            public Method run() {
                try {
                    return clazz.getMethod(methodName, parameterTypes);
                } catch (final NoSuchMethodException ex) {
                    return null;
                }
            }
        });
    }

    /**
     * Unmarshall JAXB XML.
     * 
     * @param schema
     * @param inputStream
     * @param type
     * @return T
     * @throws JAXBException
     */
    public <T> T unmarshallXml(final Schema schema, final InputStream inputStream, final Class<T> type)
        throws JAXBException {
        try {
            return run(new PrivilegedExceptionAction<T>() {

                public T run() throws JAXBException {
                    JAXBContext jc = JAXBContext.newInstance(type);
                    Unmarshaller unmarshaller = jc.createUnmarshaller();
                    unmarshaller.setSchema(schema);
                    StreamSource stream = new StreamSource(inputStream);
                    JAXBElement<T> root = unmarshaller.unmarshal(stream, type);
                    return root.getValue();
                }
            });
        } catch (Exception e) {
            throw (JAXBException) e;
        }
    }

}

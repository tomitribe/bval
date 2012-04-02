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
package org.apache.bval.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.bval.BValPermission;
import org.apache.commons.lang3.ClassUtils;

/**
 * Utility object for privileged work.
 */
public class Privileged {
    {
        if (System.getSecurityManager() != null) {
            AccessController.checkPermission(new BValPermission(BValPermission.Name.doPrivileged));
        }
    }

    /**
     * Construct a new {@link Privileged} instance. Requires {@link BValPermission}
     * {@link BValPermission.Name#doPrivileged}.
     */
    public Privileged() {
    }

    /**
     * Perform action with AccessController.doPrivileged() if security enabled.
     * 
     * @param action
     *            - the action to run
     * @return result of running the action
     */
    public final <T> T run(PrivilegedAction<T> action) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(action);
        }
        return action.run();
    }

    /**
     * Perform action with AccessController.doPrivileged() if security enabled. Unwraps
     * {@link PrivilegedActionException}s.
     * 
     * @param action
     *            - the action to run
     * @return result of running the action
     */
    public final <T> T run(final PrivilegedExceptionAction<T> action) throws Exception {
        if (System.getSecurityManager() != null) {
            try {
                return AccessController.doPrivileged(action);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
        return action.run();
    }

    /**
     * Get the context classloader of the current thread.
     * 
     * @see Thread#getContextClassLoader()
     */
    public ClassLoader getContextClassLoader() {
        return run(new PrivilegedAction<ClassLoader>() {

            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    /**
     * Get a Class by name.
     * 
     * @param classLoader
     * @param className
     * @return Class
     * @exception ClassNotFoundException
     */
    public Class<?> getClass(final ClassLoader classLoader, final String className) throws ClassNotFoundException {
        try {
            return run(new PrivilegedExceptionAction<Class<?>>() {
                public Class<?> run() throws ClassNotFoundException {
                    return ClassUtils.getClass(classLoader, className, true);
                }
            });
        } catch (Exception e) {
            throw (ClassNotFoundException) e;
        }
    }

    /**
     * Use the privilege APIs to read an annotation value.
     * 
     * Requires security policy 'permission java.lang.RuntimePermission "accessDeclaredMembers";' 'permission
     * java.lang.reflect.ReflectPermission "suppressAccessChecks";'
     * 
     * @return Object
     * @exception IllegalAccessException
     *                , InvocationTargetException
     */
    public Object getAnnotationValue(final Annotation annotation, final String name) {
        return run(new PrivilegedAction<Object>() {
            public Object run() {
                Method valueMethod;
                try {
                    valueMethod = annotation.annotationType().getDeclaredMethod(name);
                } catch (NoSuchMethodException ex) {
                    // do nothing
                    valueMethod = null;
                }
                if (null != valueMethod) {
                    try {
                        valueMethod.setAccessible(true);
                        return valueMethod.invoke(annotation);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
        });
    }

    /**
     * Returns the most appropriate {@link ClassLoader} for most situations.
     * This is defined for the purposes of BVal as the context {@link ClassLoader},
     * if available; otherwise the loader of {@code clazz} is returned.
     * 
     * Requires security policy: 'permission java.lang.RuntimePermission "getClassLoader";'
     * 
     * @return Classloader
     */
    public ClassLoader getClassLoader(final Class<?> clazz) {
        return run(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = clazz.getClassLoader();
                }
                return cl;
            }
        });
    }

    /**
     * Get a system property.
     * 
     * Requires security policy: 'permission java.util.PropertyPermission "read";'
     * 
     * @see System#getProperty(String)
     * @return String
     */
    public String getProperty(final String name) {
        return run(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(name);
            }
        });
    }

}

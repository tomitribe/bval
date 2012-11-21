/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.bval.jsr303.resolver;

import java.lang.annotation.ElementType;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.Path;
import javax.validation.TraversableResolver;

import org.apache.bval.jsr303.util.ClassHelper;

import org.apache.commons.privilizer.Privileged;

/** @see javax.validation.TraversableResolver */
public class DefaultTraversableResolver implements TraversableResolver, CachingRelevant {
    private static final Logger log = Logger.getLogger(DefaultTraversableResolver.class.getName());

    /** Class to load to check whether JPA 2 is on the classpath. */
    private static final String PERSISTENCE_UTIL_CLASSNAME =
          "javax.persistence.PersistenceUtil";

    /** Class to instantiate in case JPA 2 is on the classpath. */
    private static final String JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME =
          "org.apache.bval.jsr303.resolver.JPATraversableResolver";


    private TraversableResolver jpaTR;

    /**
     * Create a new DefaultTraversableResolver instance.
     */
    public DefaultTraversableResolver() {
        initJpa();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReachable(Object traversableObject, Path.Node traversableProperty,
                               Class<?> rootBeanType, Path pathToTraversableObject,
                               ElementType elementType) {
        return jpaTR == null || jpaTR.isReachable(traversableObject, traversableProperty,
              rootBeanType, pathToTraversableObject, elementType);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCascadable(Object traversableObject, Path.Node traversableProperty,
                                Class<?> rootBeanType, Path pathToTraversableObject,
                                ElementType elementType) {
        return jpaTR == null || jpaTR.isCascadable(traversableObject, traversableProperty,
              rootBeanType, pathToTraversableObject, elementType);
    }

    /** Tries to load detect and load JPA. */
    @SuppressWarnings("unchecked")
    private void initJpa() {
        ClassLoader classLoader = contextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        try {
            // no security needed as classLoader should not be null:
            Class.forName(PERSISTENCE_UTIL_CLASSNAME, true, classLoader);
            log.log(Level.FINEST, String.format("Found %s on classpath.", PERSISTENCE_UTIL_CLASSNAME));
        } catch (Exception e) {
            log.log(Level.FINEST, String.format("Cannot find %s on classpath. All properties will per default be traversable.", PERSISTENCE_UTIL_CLASSNAME));
            return;
        }

        try {
            Class<? extends TraversableResolver> jpaAwareResolverClass =
                Class.forName(JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME, true, classLoader).asSubclass(TraversableResolver.class);
            jpaTR = jpaAwareResolverClass.newInstance();
            log.log(Level.FINEST, String.format("Instantiated an instance of %s.", JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME));
        } catch (Exception e) {
			log.log(Level.WARNING,
					String.format(
							"Unable to load or instantiate JPA aware resolver %s. All properties will per default be traversable.",
							JPA_AWARE_TRAVERSABLE_RESOLVER_CLASSNAME, e));
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsCaching() {
        return jpaTR != null && CachingTraversableResolver.needsCaching(jpaTR);
    }

    @Privileged
    private static ClassLoader contextClassLoader() {
        try {
            return Thread.currentThread().getContextClassLoader();
        } catch (Exception e) {
            return null;
        }
    }
}

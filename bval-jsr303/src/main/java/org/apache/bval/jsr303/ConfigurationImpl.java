/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.bval.jsr303;

import java.io.InputStream;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.spi.BootstrapState;
import javax.validation.spi.ConfigurationState;
import javax.validation.spi.ValidationProvider;

import org.apache.bval.jsr303.resolver.DefaultTraversableResolver;
import org.apache.bval.jsr303.util.Privileged;
import org.apache.bval.jsr303.xml.ValidationParser;

/**
 * Description: used to configure {@link ApacheValidatorFactory}. Implementation
 * of {@link Configuration} that also implements {@link ConfigurationState},
 * hence this can be passed to
 * {@link ValidationProvider#buildValidatorFactory(ConfigurationState)}.
 */
public class ConfigurationImpl implements ApacheValidatorConfiguration, ConfigurationState {
    private static final Logger log = Logger.getLogger(ConfigurationImpl.class.getName());
    private static final Privileged PRIVILEGED = new Privileged();

    private static volatile TraversableResolver defaultTraversableResolver;
    private static volatile MessageInterpolator defaultMessageInterpolator;
    private static volatile ConstraintValidatorFactory defaultConstraintValidatorFactory;

    /**
     * Configured {@link ValidationProvider}
     */
    //couldn't this be parameterized <ApacheValidatorConfiguration> or <? super ApacheValidatorConfiguration>?
    protected final ValidationProvider<?> provider;

    /**
     * Configured {@link ValidationProviderResolver}
     */
    protected final ValidationProviderResolver providerResolver;

    /**
     * Configured {@link ValidationProvider} class
     */
    protected Class<? extends ValidationProvider<?>> providerClass;

    /**
     * Configured {@link MessageInterpolator}
     */
    protected MessageInterpolator messageInterpolator;

    /**
     * Configured {@link ConstraintValidatorFactory}
     */
    protected ConstraintValidatorFactory constraintValidatorFactory;

    private TraversableResolver traversableResolver;

    // BEGIN DEFAULTS
    /**
     * false = dirty flag (to prevent from multiple parsing validation.xml)
     */
    private boolean prepared = false;

    private final Set<InputStream> mappingStreams = new HashSet<InputStream>();
    private final Map<String, String> properties = new HashMap<String, String>();

    private boolean ignoreXmlConfiguration = false;

    /**
     * Create a new ConfigurationImpl instance.
     * @param bootstrapState
     * @param provider
     */
    public ConfigurationImpl(BootstrapState bootstrapState, ValidationProvider<?> provider) {
        this.provider = provider;
        if (provider != null) {
            this.providerResolver = null;
            return;
        }
        if (bootstrapState != null) {
            ValidationProviderResolver validationProviderResolver = bootstrapState.getValidationProviderResolver();
            this.providerResolver =
                validationProviderResolver == null ? bootstrapState.getDefaultValidationProviderResolver()
                    : validationProviderResolver;
            return;
        }
        throw new ValidationException("either ValidationProvider or BootstrapState is required");
    }

    /**
     * {@inheritDoc}
     */
    public ApacheValidatorConfiguration traversableResolver(TraversableResolver resolver) {
        traversableResolver = resolver;
        prepared = false;
        return this;
    }

    /**
     * {@inheritDoc}
     * Ignore data from the <i>META-INF/validation.xml</i> file if this
     * method is called.
     *
     * @return this
     */
    public ApacheValidatorConfiguration ignoreXmlConfiguration() {
        ignoreXmlConfiguration = true;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationImpl messageInterpolator(MessageInterpolator resolver) {
        this.messageInterpolator = resolver;
        prepared = false;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationImpl constraintValidatorFactory(
          ConstraintValidatorFactory constraintFactory) {
        this.constraintValidatorFactory = constraintFactory;
        prepared = false;
        return this;
    }

    /**
     * {@inheritDoc}
     * Add a stream describing constraint mapping in the Bean Validation
     * XML format.
     *
     * @return this
     */
    public ApacheValidatorConfiguration addMapping(InputStream stream) {
        mappingStreams.add(stream);
        return this;
    }

    /**
     * {@inheritDoc}
     * Add a provider specific property. This property is equivalent to
     * XML configuration properties.
     * If we do not know how to handle the property, we silently ignore it.
     *
     * @return this
     */
    public ApacheValidatorConfiguration addProperty(String name, String value) {
        properties.put(name, value);
        return this;
    }

    /**
     * {@inheritDoc}
     * Return a map of non type-safe custom properties.
     *
     * @return null
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     * Returns true if Configuration.ignoreXMLConfiguration() has been called.
     * In this case, we ignore META-INF/validation.xml
     *
     * @return true
     */
    public boolean isIgnoreXmlConfiguration() {
        return ignoreXmlConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    public Set<InputStream> getMappingStreams() {
        return mappingStreams;
    }

    /**
     * {@inheritDoc}
     */
    public MessageInterpolator getMessageInterpolator() {
        return messageInterpolator;
    }

    /**
     * {@inheritDoc}
     */
    public MessageInterpolator getDefaultMessageInterpolator() {
        if (defaultMessageInterpolator == null) {
            defaultMessageInterpolator = new DefaultMessageInterpolator();
        }
        return defaultMessageInterpolator;
    }

    /**
     * {@inheritDoc}
     */
    public TraversableResolver getDefaultTraversableResolver() {
        if (defaultTraversableResolver == null) {
            defaultTraversableResolver = new DefaultTraversableResolver();
        }
        return defaultTraversableResolver;
    }

    /**
     * {@inheritDoc}
     */
    public ConstraintValidatorFactory getDefaultConstraintValidatorFactory() {
        if (defaultConstraintValidatorFactory == null) {
            defaultConstraintValidatorFactory = new DefaultConstraintValidatorFactory();
        }
        return defaultConstraintValidatorFactory;
    }

    /**
     * {@inheritDoc}
     * main factory method to build a ValidatorFactory
     *
     * @throws ValidationException if the ValidatorFactory cannot be built
     */
    public ValidatorFactory buildValidatorFactory() {
        // execute with privileges where necessary:
        prepare();
        return findProvider().buildValidatorFactory(this);
    }

    private synchronized void prepare() {
        if (prepared) {
            return;
        }
        // TODO refactor xml vs. java bootstrapping priority
        parseValidationXml();
        applyDefaults();
        prepared = true;
    }

    /** Check whether a validation.xml file exists and parses it with JAXB */
    private void parseValidationXml() {
        if (isIgnoreXmlConfiguration()) {
            log.info("ignoreXmlConfiguration == true");
            return;
        }
        PRIVILEGED.run(new PrivilegedAction<Void>() {

            public Void run() {
                new ValidationParser(getProperties().get(
                    Properties.VALIDATION_XML_PATH))
                    .processValidationConfig(ConfigurationImpl.this);
                return null;
            }
        });
    }

    private void applyDefaults() {
        if (getMessageInterpolator() == null) {
            messageInterpolator(getDefaultMessageInterpolator());
        }
        if (getTraversableResolver() == null) {
            traversableResolver(getDefaultTraversableResolver());
        }
        if (getConstraintValidatorFactory() == null) {
            constraintValidatorFactory(getDefaultConstraintValidatorFactory());
        }
    }

    /**
     * {@inheritDoc}
     * @return the constraint validator factory of this configuration.
     */
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return constraintValidatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    public TraversableResolver getTraversableResolver() {
        return traversableResolver;
    }

    /**
     * Set {@link ValidationProvider} class.
     * @param providerClass
     */
    public void setProviderClass(Class<? extends ValidationProvider<?>> providerClass) {
        this.providerClass = providerClass;
    }

    private ValidationProvider<?> findProvider() {
        if (provider != null) {
            return provider;
        }
        final Iterator<ValidationProvider<?>> iter = providerResolver.getValidationProviders().iterator();
        if (!iter.hasNext()) {
            throw new ValidationException("No available ValidationProvider implementation");
        }
        if (providerClass == null) {
            return iter.next();
        }
        while (iter.hasNext()) {
            ValidationProvider<?> provider = iter.next();
            if (providerClass.isInstance(provider)) {
                return provider;
            }
        }
        throw new ValidationException(String.format("Unable to find ValidationProvider of type %s", providerClass));
    }

}

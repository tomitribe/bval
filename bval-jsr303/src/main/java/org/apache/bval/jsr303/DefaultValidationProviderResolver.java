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
package org.apache.bval.jsr303;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;

import org.apache.bval.jsr303.util.IOUtils;
import org.apache.bval.jsr303.util.Privileged;

public class DefaultValidationProviderResolver implements ValidationProviderResolver {

    // TODO - Spec recommends caching per classloader
    private static final String SPI_CFG = "META-INF/services/javax.validation.spi.ValidationProvider";
    private static final Privileged PRIVILEGED = new Privileged();

    /**
     * {@inheritDoc}
     */
    public List<ValidationProvider<?>> getValidationProviders() {
        final List<ValidationProvider<?>> target = new ArrayList<ValidationProvider<?>>();

        // get our classloader
        final ClassLoader classLoader = PRIVILEGED.getClassLoader(getClass());

        // find all service provider cfgs
        final Enumeration<URL> cfgs;
        try {
            cfgs = classLoader.getResources(SPI_CFG);
        } catch (IOException e) {
            throw new ValidationException(String.format("Error trying to read a %s", SPI_CFG), e);
        }

        while (cfgs.hasMoreElements()) {
            final URL url = cfgs.nextElement();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(url.openStream()), 256);
                for (String line = br.readLine().trim(); line != null; line = br.readLine().trim()) {
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        continue;
                    }
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ValidationProvider<?>> providerType =
                            (Class<? extends ValidationProvider<?>>) PRIVILEGED.getClass(classLoader, line);
                        target.add(providerType.newInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new ValidationException(String.format(
                            "Error creating ValidationProvider of type %s configured in resource %s", line, url), e);
                    }
                }
            } catch (IOException e) {
                throw new ValidationException(String.format("Error trying to read url %s", url), e);
            } finally {
                IOUtils.closeQuietly(br);
            }
        }
        // caller must handle the case of no providers found
        return target;
    }
}

/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.bval.jsr303.extensions;

import org.apache.bval.util.AccessStrategy;
import org.apache.commons.lang.NotImplementedException;

import java.lang.annotation.ElementType;
import java.lang.reflect.Type;


/**
 * Implementation of {@link AccessStrategy} for method parameters.
 *
 * @author Carlos Vara
 */
public class ParameterAccess extends AccessStrategy {

    private Type paramType;
    private int paramIdx;

    /**
     * Create a new ParameterAccess instance.
     * @param paramType
     * @param paramIdx
     */
    public ParameterAccess(Type paramType, int paramIdx ) {
        this.paramType = paramType;
        this.paramIdx = paramIdx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object instance) {
        throw new NotImplementedException("Obtaining a parameter value not yet implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementType getElementType() {
        return ElementType.PARAMETER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getJavaType() {
        return this.paramType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyName() {
        return "" + paramIdx;
    }

}

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
package org.apache.bval.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.Size;
import java.util.Map;

/** Check that a Map's size is between min and max. */
public class SizeValidatorForMap extends SizeValidator implements ConstraintValidator<Size, Map<?, ?>> {
    /**
     * Checks the number of entries in a map.
     *
     * @param map     The map to validate.
     * @return Returns <code>true</code> if the map is <code>null</code> or the number of entries in <code>map</code>
     *         is between the specified <code>min</code> and <code>max</code> values (inclusive),
     *         <code>false</code> otherwise.
     */
    public boolean isValid(Map<?, ?> map) {
        if (map == null) {
            return true;
        }
        final int size = map.size();
        return size >= min && size <= max;
	}

}

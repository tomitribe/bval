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

/** Check that a string's length is between min and max. */
public class SizeValidatorForCharSequence extends SizeValidator implements ConstraintValidator<Size, CharSequence> {
    /**
     * Checks the length of the specified string.
     *
     * @param s       The string to validate.
     * @return Returns <code>true</code> if the string is <code>null</code> or the length of <code>s</code> between the specified
     *         <code>min</code> and <code>max</code> values (inclusive), <code>false</code> otherwise.
     */
    public boolean isValid(CharSequence s) {
        if (s == null) {
            return true;
        }
        final int length = s.length();
        return length >= min && length <= max;
    }

}

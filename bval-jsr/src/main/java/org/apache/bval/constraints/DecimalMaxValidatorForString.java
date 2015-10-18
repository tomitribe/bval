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
import javax.validation.constraints.DecimalMax;
import java.math.BigDecimal;

/**
 * Check that the String being validated represents a number, and has a value
 * <= maxvalue
 */
public class DecimalMaxValidatorForString implements ConstraintValidator<DecimalMax, String> {

    private BigDecimal maxValue;

    public void initialize(DecimalMax annotation) {
        try {
            this.maxValue = new BigDecimal(annotation.value());
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(annotation.value() + " does not represent a valid BigDecimal format");
        }
    }

    public boolean isValid(String value) {
        if (value == null) {
            return true;
        }
        try {
            return new BigDecimal(value).compareTo(maxValue) != 1;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}

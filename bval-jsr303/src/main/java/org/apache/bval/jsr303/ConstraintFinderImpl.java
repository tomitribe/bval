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

import org.apache.bval.jsr303.groups.Group;
import org.apache.bval.jsr303.groups.Groups;
import org.apache.bval.jsr303.groups.GroupsComputer;
import org.apache.bval.model.MetaBean;

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.ElementDescriptor.ConstraintFinder;
import javax.validation.metadata.Scope;
import java.lang.annotation.ElementType;
import java.util.*;

/**
 * Description: Implementation of the fluent {@link ConstraintFinder} interface.<br/>
 */
final class ConstraintFinderImpl implements ElementDescriptor.ConstraintFinder {
    private final MetaBean metaBean;
    private final Set<Scope> findInScopes;
    private Set<ConstraintValidation<?>> constraintDescriptors;

    /**
     * Create a new ConstraintFinderImpl instance.
     * 
     * @param metaBean
     * @param constraintDescriptors
     */
    ConstraintFinderImpl(MetaBean metaBean, Set<ConstraintValidation<?>> constraintDescriptors) {
        this.metaBean = metaBean;
        this.constraintDescriptors = constraintDescriptors;
        this.findInScopes = new HashSet<Scope>(Arrays.asList(Scope.values()));
    }

    /**
     * {@inheritDoc}
     */
    public ElementDescriptor.ConstraintFinder unorderedAndMatchingGroups(Class<?>... groups) {
        Set<ConstraintValidation<?>> matchingDescriptors =
            new HashSet<ConstraintValidation<?>>(constraintDescriptors.size());
        Groups groupChain = new GroupsComputer().computeGroups(groups);
        for (Group group : groupChain.getGroups()) {
            if (group.isDefault()) {
                // If group is default, check if it gets redefined
                List<Group> expandedDefaultGroup = metaBean.getFeature(Jsr303Features.Bean.GROUP_SEQUENCE);
                for (Group defaultGroupMember : expandedDefaultGroup) {
                    for (ConstraintValidation<?> descriptor : constraintDescriptors) {
                        if (isInScope(descriptor) && isInGroup(descriptor, defaultGroupMember)) {
                            matchingDescriptors.add(descriptor);
                        }
                    }
                }
            } else {
                for (ConstraintValidation<?> descriptor : constraintDescriptors) {
                    if (isInScope(descriptor) && isInGroup(descriptor, group)) {
                        matchingDescriptors.add(descriptor);
                    }
                }
            }
        }
        return thisWith(matchingDescriptors);
    }

    /**
     * {@inheritDoc}
     */
    public ElementDescriptor.ConstraintFinder lookingAt(Scope scope) {
        if (scope.equals(Scope.LOCAL_ELEMENT)) {
            findInScopes.remove(Scope.HIERARCHY);
            for (Iterator<ConstraintValidation<?>> it = constraintDescriptors.iterator(); it.hasNext();) {
                ConstraintValidation<?> cv = it.next();
                if (cv.getOwner() != metaBean.getBeanClass()) {
                    it.remove();
                }
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ElementDescriptor.ConstraintFinder declaredOn(ElementType... elementTypes) {
        Set<ConstraintValidation<?>> matchingDescriptors =
            new HashSet<ConstraintValidation<?>>(constraintDescriptors.size());
        for (ElementType each : elementTypes) {
            for (ConstraintValidation<?> descriptor : constraintDescriptors) {
                if (isInScope(descriptor) && isAtElement(descriptor, each)) {
                    matchingDescriptors.add(descriptor);
                }
            }
        }
        return thisWith(matchingDescriptors);
    }

    private boolean isAtElement(ConstraintValidation<?> descriptor, ElementType each) {
        return descriptor.getAccess().getElementType() == each;
    }

    private boolean isInScope(ConstraintValidation<?> descriptor) {
        if (findInScopes.size() == Scope.values().length)
            return true; // all scopes
        if (metaBean != null) {
            Class<?> owner = descriptor.getOwner();
            for (Scope scope : findInScopes) {
                switch (scope) {
                case LOCAL_ELEMENT:
                    if (owner.equals(metaBean.getBeanClass()))
                        return true;
                    break;
                case HIERARCHY:
                    if (!owner.equals(metaBean.getBeanClass()))
                        return true;
                    break;
                }
            }
        }
        return false;
    }

    private boolean isInGroup(ConstraintValidation<?> descriptor, Group group) {
        return descriptor.getGroups().contains(group.getGroup());
    }

    private ElementDescriptor.ConstraintFinder thisWith(Set<ConstraintValidation<?>> matchingDescriptors) {
        constraintDescriptors = matchingDescriptors;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Set<ConstraintDescriptor<?>> getConstraintDescriptors() {
        return constraintDescriptors.isEmpty() ? Collections.<ConstraintDescriptor<?>> emptySet() : Collections
            .<ConstraintDescriptor<?>> unmodifiableSet(constraintDescriptors);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasConstraints() {
        return !constraintDescriptors.isEmpty();
    }
}

package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.models.IPolicy;
import com.jasminesoftwaresolutions.witchwood.models.IRole;
import com.jasminesoftwaresolutions.witchwood.models.IRolePolicyAssignment;

/**
 * Canonical implementation of {@link IRolePolicyAssignment}.
 */
public record RolePolicyAssignment(IRole role, IPolicy policy) implements IRolePolicyAssignment {
}

package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.models.IPolicy;
import com.jasminesoftwaresolutions.witchwood.models.IPrincipal;
import com.jasminesoftwaresolutions.witchwood.models.IPrincipalPolicyAssignment;

/**
 * Canonical implementation of {@link IPrincipalPolicyAssignment}.
 */
public record PrincipalPolicyAssignment(IPrincipal principal, IPolicy policy) implements IPrincipalPolicyAssignment {
}

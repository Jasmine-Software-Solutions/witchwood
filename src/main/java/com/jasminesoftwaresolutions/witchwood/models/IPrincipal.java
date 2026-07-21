package com.jasminesoftwaresolutions.witchwood.models;

import java.util.Set;
import java.util.UUID;

public interface IPrincipal {
    UUID id();

    Set<IPrincipalRoleAssignment> roles();
    Set<IPrincipalPolicyAssignment> policies();
}

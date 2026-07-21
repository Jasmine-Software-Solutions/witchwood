package com.jasminesoftwaresolutions.witchwood.models;

import java.util.Set;
import java.util.UUID;

public interface IRole {
    UUID id();

    Set<IRolePolicyAssignment> policies();
}

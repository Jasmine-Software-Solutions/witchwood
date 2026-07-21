package com.jasminesoftwaresolutions.witchwood.models;

import java.time.Instant;

public interface IPrincipalRoleAssignment {
    IPrincipal principal();
    IRole role();

    Instant effective();
    Instant expiration();
}

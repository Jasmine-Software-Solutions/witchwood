package com.jasminesoftwaresolutions.witchwood.models;

import java.util.Set;
import java.util.UUID;

public interface IPolicy {
    UUID id();

    Set<IPolicyStatement> statements();
}

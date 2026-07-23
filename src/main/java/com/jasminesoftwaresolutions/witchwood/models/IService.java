package com.jasminesoftwaresolutions.witchwood.models;

import java.util.Set;
import java.util.UUID;

public interface IService {
    UUID id();

    String name();

    Set<IServiceAction> actions();
}

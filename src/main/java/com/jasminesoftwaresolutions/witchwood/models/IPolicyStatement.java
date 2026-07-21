package com.jasminesoftwaresolutions.witchwood.models;

import java.util.Set;

public interface IPolicyStatement {
    IPolicy policy();
    int id();

    PolicyEffect effect();
    Set<String> actions();
    Set<String> resources();

    String condition();
}

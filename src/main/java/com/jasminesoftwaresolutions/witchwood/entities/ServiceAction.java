package com.jasminesoftwaresolutions.witchwood.entities;

import com.jasminesoftwaresolutions.witchwood.models.IService;
import com.jasminesoftwaresolutions.witchwood.models.IServiceAction;

/**
 * Canonical implementation of {@link IServiceAction}.
 */
public record ServiceAction(IService service, String actionKey) implements IServiceAction {
}

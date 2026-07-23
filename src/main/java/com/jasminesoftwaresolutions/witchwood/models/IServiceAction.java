package com.jasminesoftwaresolutions.witchwood.models;

public interface IServiceAction {
    IService service();

    /**
     * The key portion of an action string
     * (e.g., for {@code "billing:charge"}, this is {@code "charge"}).
     */
    String actionKey();
}

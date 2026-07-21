package com.jasminesoftwaresolutions.witchwood.util;

public record ArrayView<T>(
        T[] array,
        int start,
        int end
) {}
package org.marmotgraph.search.common.model.source;

import java.util.Comparator;

public interface FullNameRef {
    String getId();

    String getFullName();

    Comparator<FullNameRef> COMPARATOR = Comparator.comparing(FullNameRef::getFullName, Comparator.nullsFirst(String::compareToIgnoreCase));

}

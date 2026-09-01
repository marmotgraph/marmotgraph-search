package org.marmotgraph.search.common.model.source;

import java.util.Comparator;

public interface DisplayNameRef {
    String getId();

    String getDisplayName();

    Comparator<DisplayNameRef> COMPARATOR = Comparator.comparing(DisplayNameRef::getDisplayName, Comparator.nullsFirst(String::compareToIgnoreCase));

}

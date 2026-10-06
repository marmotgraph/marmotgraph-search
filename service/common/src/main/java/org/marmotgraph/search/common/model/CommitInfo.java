package org.marmotgraph.search.common.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CommitInfo {
    private final String appCommit;
    private final String libraryCommit;

    public String toString() {
        return appCommit != null ? String.format("%s / %s", appCommit, libraryCommit) : libraryCommit;
    }

}
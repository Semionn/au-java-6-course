package com.au.mit.vcs.common.branch;

import com.au.mit.vcs.common.commit.Commit;

/**
 * Created by semionn on 22.09.16.
 */
public class Branch implements java.io.Serializable {
    private final String name;
    private final Commit parentCommit;
    private Commit lastCommit;

    public Branch(String name, Commit parentCommit) {
        this.name = name;
        this.parentCommit = parentCommit;
        this.lastCommit = parentCommit;
    }

    public String getName() {
        return name;
    }

    public Commit getLastCommit() {
        return lastCommit;
    }

    public void setLastCommit(Commit lastCommit) {
        this.lastCommit = lastCommit;
    }
}

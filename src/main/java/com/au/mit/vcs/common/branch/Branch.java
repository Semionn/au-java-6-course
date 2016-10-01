package com.au.mit.vcs.common.branch;

import com.au.mit.vcs.common.commit.Commit;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by semionn on 22.09.16.
 */
public class Branch implements java.io.Serializable {
    private final String name;
    private Commit lastCommit;
    private List<Branch> children;
    private boolean deleted;

    public Branch(String name, Commit parentCommit) {
        this.name = name;
        this.lastCommit = parentCommit;
        deleted = false;
        children = new ArrayList<>();
        if (parentCommit != null) {
            parentCommit.getBranch().addChildBranch(this);
        }
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

    public void addChildBranch(Branch branch) {
        children.add(branch);
    }

    public void markDeleted() {
        deleted = true;
    }

    public boolean possibleToDelete() {
        for (Branch branch : children) {
            if (!branch.possibleToDelete()) {
                return false;
            }
        }
        return deleted;
    }
}

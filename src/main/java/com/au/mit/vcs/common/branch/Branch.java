package com.au.mit.vcs.common.branch;

import com.au.mit.vcs.common.commit.Commit;

import java.util.ArrayList;
import java.util.List;

/**
 * Corresponds to branch in VCS.
 * Stores pointer to last branch commit and child branches.
 * Could be marked as deleted, to remove it just after deleting child branches
 */
public class Branch implements java.io.Serializable {
    private final String name;
    private Commit lastCommit;
    private List<Branch> children;
    private boolean deleted;


    /**
     * Constructor, add the branch to child list of parent branch (if exists)
     * @param name name of the branch
     * @param parentCommit first commit of the branch
     */
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

    /**
     * Check deleted mark for self and child branches recursively
     */
    public boolean possibleToDelete() {
        for (Branch branch : children) {
            if (!branch.possibleToDelete()) {
                return false;
            }
        }
        return deleted;
    }
}

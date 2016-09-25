package com.au.mit.vcs.common.commit;

import com.au.mit.vcs.common.branch.Branch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by semionn on 22.09.16.
 */
public class Commit implements java.io.Serializable {
    private final String hash;
    private final String message;
    private final Branch branch;
    private final Commit previousCommit;
    private final List<Diff> diffList;
    private final int depth;

    public Commit(String hash, String message, Branch branch, Commit previousCommit, List<Diff> diffList) {
        this.hash = hash;
        this.message = message;
        this.branch = branch;
        this.previousCommit = previousCommit;
        this.diffList = new ArrayList<>(diffList);
        depth = previousCommit != null ? previousCommit.depth + 1 : 0;
    }

    public String getHash() {
        return hash;
    }

    public String getMessage() {
        return message;
    }

    public Branch getBranch() {
        return branch;
    }

    public Commit getPreviousCommit() {
        return previousCommit;
    }

    public List<Diff> getDiffList() {
        return diffList;
    }

    public Map<String, Diff> getDiffMap() {
        return diffList.stream().collect(Collectors.toMap(Diff::getFileStrPath, Function.identity()));
    }

    public int getDepth() {
        return depth;
    }

    public String print() {
        return String.format("%s: %s", hash, message);
    }
}

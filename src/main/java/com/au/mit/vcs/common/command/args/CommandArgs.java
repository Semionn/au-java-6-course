package com.au.mit.vcs.common.command.args;

import java.util.List;
import java.util.Map;

/**
 * Interface for VCS commands arguments
 * Returns both positional and optional arguments
 */
public interface CommandArgs {

    /**
     * Return values of positional arguments
     */
    List<String> getArgs();

    /**
     * Return map of names and values of optional arguments
     */
    Map<String, String> getOptions();
}

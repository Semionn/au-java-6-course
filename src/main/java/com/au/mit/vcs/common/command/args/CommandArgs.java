package com.au.mit.vcs.common.command.args;

import java.util.List;
import java.util.Map;

/**
 * Created by semionn on 23.09.16.
 */
public interface CommandArgs {
    List<String> getArgs();
    Map<String, String> getOptions();
}

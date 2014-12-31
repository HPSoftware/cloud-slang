package org.openscore.lang.compiler.utils;
/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/


/*
 * Created by orius123 on 05/11/14.
 */

import ch.lambdaj.Lambda;
import org.openscore.lang.compiler.SlangTextualKeys;
import org.openscore.lang.compiler.model.Executable;
import org.openscore.lang.compiler.model.Flow;
import org.openscore.lang.compiler.model.Task;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.Matchers.equalTo;

@Component
public class DependenciesHelper {

    /**
     * fetches all of the slang files from the given path
     *
     * @param path the path to look in
     * @return a List of {@link java.io.File} that has slang extensions
     */
    public List<File> fetchSlangFiles(Set<File> path) {
        return filterFiles(path, Arrays.asList(System.getProperty("slang.extensions", "yaml,yml,sl").split(",")));
    }

//    /**
//     * fetches all of the script files from the given path
//     *
//     * @param path the path to look in
//     * @return a List of {@link java.io.File} that has script extensions (currently only python)
//     */
//    public List<File> fetchScriptFiles(Set<File> path) {
//        return filterFiles(path, Arrays.asList(System.getProperty("script.extensions", "py").split(",")));
//    }

    /**
     * filter a path by extension
     *
     * @param path       the path to look in
     * @param extensions List of String to filter by
     * @return a List of {@link java.io.File} that has the requested extensions
     */
    private List<File> filterFiles(Set<File> path, List<String> extensions) {
        List<File> filteredClassPath = new ArrayList<>();
        for (File file : path) {
            if (file.isDirectory()) {
                filteredClassPath.addAll(FileUtils.listFiles(file, extensions.toArray(new String[extensions.size()]), true));
            } else {
                if (extensions.contains(FilenameUtils.getExtension(file.getAbsolutePath()))) {
                    filteredClassPath.add(file);
                }
            }
        }
        return filteredClassPath;
    }

    /**
     * recursive matches executables with their references
     *
     * @param availableDependencies the executables to match from
     * @return a map of a the executables that were successfully matched
     */
    public Map<String, Executable> matchReferences(Executable executable, Collection<Executable> availableDependencies) {
        Validate.isTrue(executable.getType().equals(SlangTextualKeys.FLOW_TYPE), "Executable: " + executable.getId() + " is not a flow, therefore it has no references");
        Map<String, Executable> resolvedDependencies = new HashMap<>();
        return fetchFlowReferences((Flow) executable, availableDependencies, resolvedDependencies);
    }

    private Map<String, Executable> fetchFlowReferences(Flow flow,
                                                                Collection<Executable> availableDependencies,
                                                                Map<String, Executable> resolvedDependencies) {
        Deque<Task> tasks = flow.getWorkflow().getTasks();
        for (Task task : tasks) {
            String refId = task.getRefId();
            //if it is already in the references we do nothing
            if (resolvedDependencies.get(refId) == null) {
                Executable matchingRef = Lambda.selectFirst(availableDependencies, having(on(Executable.class).getId(), equalTo(refId)));
                if (matchingRef == null) {
                    throw new RuntimeException("Reference: " + refId + " of task: " + task.getName() + " in flow: "
                            + flow.getName() + " wasn't found in path");
                }
                validateNavigation(task, matchingRef);
                //first we put the reference on the map
                resolvedDependencies.put(matchingRef.getId(), matchingRef);
                if (matchingRef.getType().equals(SlangTextualKeys.FLOW_TYPE)) {
                    //if it is a flow  we recursively
                    resolvedDependencies.putAll(fetchFlowReferences((Flow) matchingRef, availableDependencies, resolvedDependencies));
                }
            }
        }
        return resolvedDependencies;
    }

    private void validateNavigation(Task task, Executable matchingRef) {
        Validate.notEmpty(matchingRef.getResults());
        Validate.notEmpty(task.getNavigationStrings());
    }
}
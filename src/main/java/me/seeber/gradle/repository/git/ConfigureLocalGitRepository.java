/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.seeber.gradle.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * Configure the local Git repository
 */
public class ConfigureLocalGitRepository extends ConventionTask {

    /**
     * Regular expression used to escape file patterns
     */
    protected static final Pattern ESCAPE_PATTERN = Pattern.compile("[#\\\\]");

    /**
     * Regular expression used to unescape file patterns
     */
    protected static final Pattern UNESCAPE_PATTERN = Pattern.compile("\\\\([#\\\\])");

    /**
     * Remote Git repositories
     */
    @Input
    private Map<String, String> remoteRepositories = new HashMap<>();

    /**
     * Entries for <code>.gitignore</code>
     */
    @Input
    private SortedSet<@NonNull String> ignores = new TreeSet<>();

    /**
     * Run task
     *
     * @throws IOException if something really bad happens
     */
    @TaskAction
    public void configure() throws IOException {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repositoryBuilder.addCeilingDirectory(getProject().getProjectDir());
        repositoryBuilder.findGitDir(getProject().getProjectDir());

        boolean create = (repositoryBuilder.getGitDir() == null);

        repositoryBuilder.setWorkTree(getProject().getProjectDir());

        Repository repository = repositoryBuilder.build();

        if (create) {
            repository.create();
        }

        try (Git git = new Git(repository)) {
            StoredConfig config = git.getRepository().getConfig();

            getRemoteRepositories().forEach((name, url) -> {
                config.setString("remote", name, "url", url);
            });

            config.save();
        }

        File ignoreFile = getProject().file(".gitignore");
        SortedSet<@NonNull String> remainingIgnores = new TreeSet<>(getIgnores());
        List<String> lines = new ArrayList<>();

        if (ignoreFile.exists()) {
            Files.lines(ignoreFile.toPath()).forEach(l -> {
                lines.add(l);

                if (!l.trim().startsWith("#")) {
                    remainingIgnores.remove(unescapePattern(l));
                }
            });
        }

        if (!remainingIgnores.isEmpty()) {
            List<@NonNull String> escapedIgnores = remainingIgnores.stream().map(l -> escapePattern(l))
                    .collect(Collectors.toList());
            lines.addAll(escapedIgnores);
            Files.write(ignoreFile.toPath(), lines, StandardOpenOption.CREATE);
        }
    }

    /**
     * Escape a file pattern
     *
     * @param pattern Pattern to escape
     * @return Escaped pattern
     */
    protected String escapePattern(String pattern) {
        String escaped = ESCAPE_PATTERN.matcher(pattern).replaceAll("\\\\$0");
        return escaped;
    }

    /**
     * Unescape a file pattern
     *
     * @param pattern Pattern to unescape
     * @return Unescaped pattern
     */
    protected String unescapePattern(String pattern) {
        String escaped = UNESCAPE_PATTERN.matcher(pattern).replaceAll("$1");
        return escaped;
    }

    /**
     * Get the remote Git repositories
     *
     * @return Remote Git repositories
     */
    public Map<String, String> getRemoteRepositories() {
        return this.remoteRepositories;
    }

    /**
     * Set the remote Git repositories
     *
     * @param repositories Remote Git repositories
     */
    public void setRemoteRepositories(Map<String, String> repositories) {
        this.remoteRepositories = new HashMap<>(repositories);
    }

    /**
     * Get the entries for <code>.gitignore</code>
     *
     * @return entries for <code>.gitignore</code>
     */
    public Set<@NonNull String> getIgnores() {
        return this.ignores;
    }

    /**
     * Set the entries for <code>.gitignore</code>
     *
     * @param ignores Entries for <code>.gitignore</code>
     */
    public void setIgnores(Set<@NonNull String> ignores) {
        this.ignores.clear();
        this.ignores.addAll(ignores);
    }
}

/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Jochen Seeber
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

import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.model.Validate;

import me.seeber.gradle.plugin.AbstractProjectConfigPlugin;
import me.seeber.gradle.project.base.ProjectConfig;
import me.seeber.gradle.project.base.ProjectConfigPlugin;
import me.seeber.gradle.repository.git.GitConfigPlugin.PluginRules.JavaRules;

/**
 * Configure project for Github
 */
public class GitConfigPlugin extends AbstractProjectConfigPlugin {

    /**
     * Plugin rules
     */
    public static class PluginRules extends RuleSource {

        /**
         * Provide Git configuration
         *
         * @param gitConfig Git configuration
         */
        @Model
        public void gitConfig(GitConfig gitConfig) {
        }

        /**
         * Validate a Git repository
         *
         * @param repository Git repository to validate
         */
        @Validate
        public void validateGitConfig(@Each GitRepository repository) {
            if (repository.getUrl() == null) {
                throw new GradleException(
                        String.format("Please set 'gitConfig.remoteRepositories.%s.url'", repository.getName()));
            }
        }

        /**
         * Create the Git tasks
         *
         * @param tasks Task container to add tasks to
         * @param projectConfig Project configuration
         */
        @Mutate
        public void createGitTasks(ModelMap<Task> tasks, ProjectConfig projectConfig) {
            tasks.create("configureLocalGitRepository", ConfigureLocalGitRepository.class, t -> {
                t.setDescription("Configure the local Git repository, creating it if necessary.");
                t.setGroup("build setup");
                t.setIgnores(projectConfig.getVersionControl().getIgnores());
                t.getRemoteRepositories().put("origin", projectConfig.getRepository().getDeveloperConnection());
            });

            tasks.named("configure", t -> {
                t.dependsOn("configureLocalGitRepository");
            });
        }

        /**
         * Additional rules for Java projects
         */
        public static class JavaRules extends RuleSource {
            /**
             * Initialize the Project configuration
             *
             * @param projectConfig Project configuration to initialize
             */
            @Defaults
            public void initializeProjectConfig(ProjectConfig projectConfig) {
                projectConfig.getVersionControl().ignore("/.checkstyle");
            }
        }

    }

    /**
     * @see me.seeber.gradle.plugin.AbstractProjectConfigPlugin#initialize()
     */
    @Override
    public void initialize() {
        getProject().getPlugins().apply(ProjectConfigPlugin.class);

        getProject().getPluginManager().withPlugin("java", p -> {
            getProject().getPluginManager().apply(JavaRules.class);
        });
    }
}

package com.infloop.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.BasePlugin

public class ChocolateyPlugin implements Plugin<Project> {
    public void apply(Project project) {
        project.task('nugetSpec', type: GenerateNuGetSpec) {
            group = BasePlugin.BUILD_GROUP
            description = 'Generates the NuGet spec file.'
        }

        project.task('greeting', type: Greeting) {
            group = BasePlugin.BUILD_GROUP
            description = 'Test greeting task'
        }
    }
}

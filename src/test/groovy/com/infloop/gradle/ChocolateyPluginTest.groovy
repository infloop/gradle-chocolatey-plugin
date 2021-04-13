package com.infloop.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import spock.lang.Specification

/**
 * A simple unit test for the 'com.infloop.gradle.chocolatey' plugin.
 */
public class ChocolateyPluginTest extends Specification {
    def "plugin registers task"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("com.infloop.gradle.chocolatey")

        then:
        project.tasks.findByName("greeting") != null
    }
}

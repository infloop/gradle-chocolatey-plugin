package com.infloop.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class Greeting extends DefaultTask {

    @TaskAction
    public void greeting() {
        println('Greeting')
    }
}

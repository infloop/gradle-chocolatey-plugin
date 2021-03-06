package com.infloop.gradle

import com.infloop.gradle.packagesparser.NugetParser
import com.infloop.gradle.packagesparser.PackageReferenceParser
import com.infloop.gradle.packagesparser.PackagesConfigParser
import com.infloop.gradle.packagesparser.ProjectJsonParser
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile

class GenerateNuGetSpec extends Exec {

    @Internal
    def nuspecFile

    @Internal
    def nuspec

    void exec() {
        generateNuspecFile()
    }

    @Internal
    File getTempNuspecFile() {
        new File(temporaryDir, project.name + '.nuspec')
    }

    @OutputFile
    File getNuspecFile() {
        nuspecFile ?: getTempNuspecFile()
    }

    void generateNuspecFile() {
        def nuspecXml = generateNuspec()
        if (nuspecXml) {
            getNuspecFile().write(nuspecXml, 'utf-8')
        }
    }

    String generateNuspec() {
        if (nuspec) {
            def sw = new StringWriter()
            new groovy.xml.MarkupBuilder(sw).with {
                def visitor
                visitor = { entry ->
                    switch (entry) {
                        case Closure:
                            entry.resolveStrategy = DELEGATE_FIRST
                            entry.delegate = delegate
                            entry.call()
                            break
                        case Map.Entry:
                            "$entry.key" { visitor entry.value }
                            break
                        case Map:
                        case Collection:
                            entry.collect(visitor)
                            break
                        default:
                            mkp.yield(entry)
                            break
                    }
                }
                'package' (xmlns: 'http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd') {
                    visitor nuspec
                }
            }
            supplementDefaultValueOnNuspec sw.toString()
        }
    }

    String supplementDefaultValueOnNuspec(String nuspecString) {
        def final msbuildTaskExists = project.tasks.findByName('msbuild') != null
        def final packageConfigFileName = 'packages.config'
        def final projectJsonFileName = 'project.json'

        GPathResult root = new XmlSlurper(false, false).parseText(nuspecString)

        def defaultMetadata = []
        def setDefaultMetadata = { String node, value ->
            if (root.metadata[node].isEmpty()) {
                defaultMetadata.add( { delegate."$node" value } )
            }
        }

        setDefaultMetadata ('id', project.name)
        setDefaultMetadata ('version', project.version)
        setDefaultMetadata ('description', project.description ? project.description : project.name)

        def appendAndCreateParentIfNeeded = {
            String parentNodeName, List children ->
                if(!children.isEmpty()) {
                    if (root."$parentNodeName".isEmpty()) {
                        root << { "$parentNodeName" children }
                    } else {
                        root."$parentNodeName" << children
                    }
                }
        }

        if (msbuildTaskExists) {
            project.logger.debug("Msbuild plugin detected")
            if (project.msbuild.parseProject) {
                project.logger.debug("Add defaults from Msbuild plugin")
                def mainProject = project.msbuild.mainProject

                if (root.files.file.isEmpty()) {
                    project.logger.debug("No files already defined in the NuGet spec, will add the ones from the msbuild task.")
                    def defaultFiles = []
                    project.msbuild.mainProject.dotnetArtifacts.each {
                        artifact ->
                            def fwkFolderVersion = mainProject.properties.TargetFrameworkVersion.toString().replace('v', '').replace('.', '')
                            defaultFiles.add({ file(src: artifact.toString(), target: 'lib/net' + fwkFolderVersion) })
                    }
                    appendAndCreateParentIfNeeded('files', defaultFiles)
                }
                def dependencies = []
                dependencies.addAll getDependencies(mainProject, packageConfigFileName, new PackagesConfigParser())
                dependencies.addAll getDependencies(mainProject, projectJsonFileName, new ProjectJsonParser())
                dependencies.addAll getDependencies(mainProject, mainProject.properties.MSBuildProjectFile.toString(), new PackageReferenceParser())

                if (!dependencies.isEmpty())
                    setDefaultMetadata('dependencies', dependencies)
            }
            else {
                project.logger.debug("Msbuild plugin is configured with parseProject=false, no defaults added")
            }
        }

        appendAndCreateParentIfNeeded('metadata', defaultMetadata)

        project.logger.info("Generated NuGetSpec file with ${root.files.file.size()} files " +
                "and ${root.dependencies.dependecy.size()} dependencies")
        XmlUtil.serialize (root)
    }

    Collection getDependencies(def mainProject, String fileName, NugetParser parser) {
        def file = new File(
                new File(mainProject.projectFile).parentFile,
                fileName)
        if (file.exists()) {
            project.logger.debug("Adding dependencies from ${fileName}")
            return parser.getDependencies(file)
        }
        return []
    }
}

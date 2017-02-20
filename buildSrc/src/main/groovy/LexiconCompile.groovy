import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LexiconCompile extends DefaultTask {
    @InputFile
    File allophonesFile = project.file("modules/$project.locale/lexicon/allophones.${project.locale}.xml")

    @InputFile
    File lexiconFile = project.file("modules/$project.locale/lexicon/${project.locale}.txt")

    @Optional
    @Input
    Map<String, String> phoneMapping

    @OutputFile
    File ltsFile = project.file("$temporaryDir/${project.locale}.lts")

    @OutputFile
    File fstFile = project.file("$temporaryDir/${project.locale}_lexicon.fst")

    @TaskAction
    void compile() {
        project.copy {
            from allophonesFile, lexiconFile
            into temporaryDir
        }
        project.javaexec {
            classpath project.configurations.lexiconCompile
            main 'marytts.tools.transcription.LTSLexiconPOSBuilder'
            args "$temporaryDir/$allophonesFile.name", "$temporaryDir/$lexiconFile.name"
        }
    }
}

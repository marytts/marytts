import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

import marytts.modules.phonemiser.AllophoneSet

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
        // load allophoneset
        def allophoneSet = AllophoneSet.getAllophoneSet(allophonesFile.newInputStream(), project.locale)

        // read transcriptions
        def lexicon = [:]
        lexiconFile.eachLine('UTF-8') { line ->
            def fields = line.split('\\s')
            if (fields.first().startsWith('#')) {
                // a comment
            } else if (fields.size() == 1) {
                project.logger.info "No transcription found in line: $line"
            } else {
                def (lemma, transcription) = fields.take(2)
                // remap phones
                phoneMapping.each { before, after ->
                    transcription = transcription.replaceAll ~/$before/, after
                }
                if (allophoneSet.checkAllophoneSyntax(transcription)) {
                    // store valid transcription
                    lexicon[lemma] = transcription
                } else {
                    project.logger.warn "Invalid transcription for '$lemma': [$transcription]"
                }
            }
        }
        assert lexicon
    }
}

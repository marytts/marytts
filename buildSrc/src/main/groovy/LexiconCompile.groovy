import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

import marytts.fst.AlignerTrainer
import marytts.fst.TransducerTrie
import marytts.modules.phonemiser.AllophoneSet
import marytts.tools.newlanguage.LTSTrainer

class LexiconCompile extends DefaultTask {
    @InputFile
    File allophonesFile = project.file("modules/$project.locale/lexicon/allophones.${project.locale}.xml")

    @InputFile
    File lexiconFile = project.file("modules/$project.locale/lexicon/${project.locale}.txt")

    @Optional
    @Input
    Map<String, String> phoneMapping = [:]

    @OutputFile
    File ltsFile = project.file("$temporaryDir/${project.locale}.lts")

    @OutputFile
    File fstFile = project.file("$temporaryDir/${project.locale}_lexicon.fst")

    @Internal
    File sampaLexiconFile = project.file("$temporaryDir/${project.locale}_lexicon.dict")

    @TaskAction
    void compile() {
        // load allophoneset
        def allophoneSet = AllophoneSet.getAllophoneSet(allophonesFile.newInputStream(), project.locale)

        // read transcriptions
        def lexicon = [:]
        lexiconFile.eachLine('UTF-8') { line ->
            def fields = line.trim().split('\\s+')
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

        // adapted code from LTSLexiconPOSBuilder#trainLTS and LTSLexiconPOSBuilder#saveTranscription
        project.logger.lifecycle "train and predict"
        def ltsTrainer = new LTSTrainer(allophoneSet, true, true, 2)
        ltsTrainer.readLexicon(lexicon)
        5.times {
            project.logger.lifecycle "iteration ${it + 1}"
            ltsTrainer.alignIteration()
        }
        def tree = ltsTrainer.trainTree(100)
        ltsTrainer.save(tree, ltsFile.path)

        project.logger.lifecycle "save transcription"
        sampaLexiconFile.withWriter('UTF-8') { writer ->
            lexicon.each { lemma, transcription ->
                def transcriptionStr = allophoneSet.splitAllophoneString(transcription)
                writer.println "$lemma|$transcriptionStr"
            }
        }
        def aligner = new AlignerTrainer(false, true)
        aligner.readLexicon(sampaLexiconFile.newReader('UTF-8'), '\\|')
        4.times { aligner.alignIteration() }

        def trie = new TransducerTrie()
        aligner.lexiconSize().times {
            trie.add(aligner.getAlignment(it))
            trie.add(aligner.getInfoAlignment(it))
        }
        trie.computeMinimization()
        trie.writeFST(fstFile.newDataOutputStream(), 'UTF-8')
    }
}

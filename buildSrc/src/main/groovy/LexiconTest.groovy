import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

import marytts.fst.FSTLookup

class LexiconTest extends DefaultTask {

    @OutputFile
    File reportFile = project.file("$temporaryDir/report.txt")

    @TaskAction
    void test() {
        // adapted code from TranscriptionTableModel#testFST
        def fstFile = project.compileLexicon.fstFile
        def fst = new FSTLookup(fstFile.path)
        def correct = 0
        def failed = 0
        reportFile.withWriter('UTF-8') { report ->
            project.compileLexicon.sampaLexiconFile.eachLine('UTF-8') { line ->
                def (lemma, transcription) = line.split('\\|')
                def result = fst.lookup(lemma)
                assert result
                if (result.first() == transcription) {
                    correct++
                } else {
                    failed++
                    report.print "Problem looking up key '$lemma': Expected value '$transcription', but got "
                    switch (result.length) {
                        case 0:
                            report.println 'no result'
                            break
                        case 1:
                            report.println "result '${result.first()}'"
                            break
                        default:
                            report.println "$result.length results: $result"
                            break
                    }
                }
            }
            report.println "Testing complete. ${correct + failed} entries ($correct correct, $failed failed)"
        }
        project.logger.lifecycle reportFile.text.trim()
        assert failed == 0
    }
}

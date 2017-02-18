import org.gradle.api.*

class LexiconPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.configurations.create 'lexiconCompile'

        project.dependencies {
            lexiconCompile(group: 'de.dfki.mary', name: 'marytts-transcription', version: '5.2') {
                exclude module: 'sgt'
                exclude module: 'mwdumper'
            }
        }

        project.task('compileLexicon', type: LexiconCompile)
    }
}

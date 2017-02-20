import org.gradle.api.*
import org.gradle.api.plugins.JavaPlugin

class LexiconPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(JavaPlugin)

        project.configurations.create 'lexiconCompile'

        project.dependencies {
            lexiconCompile(group: 'de.dfki.mary', name: 'marytts-transcription', version: '5.2') {
                exclude module: 'sgt'
                exclude module: 'mwdumper'
            }
        }

        project.task('compileLexicon', type: LexiconCompile)

        project.processResources {
            from project.compileLexicon
            from project.compileLexicon.allophonesFile
            eachFile {
                it.path = "marytts/language/$project.locale/lexicon/$it.name"
            }
        }
    }
}

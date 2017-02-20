import org.gradle.api.*
import org.gradle.api.plugins.JavaPlugin

class LexiconPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(JavaPlugin)

        project.task('compileLexicon', type: LexiconCompile)

        project.processResources {
            from project.compileLexicon
            from project.compileLexicon.allophonesFile
            eachFile {
                it.path = "marytts/language/$project.locale/lexicon/$it.name"
            }
        }

        project.task('testLexicon', type: LexiconTest) {
            inputs.files project.compileLexicon
            project.test.dependsOn it
        }
    }
}

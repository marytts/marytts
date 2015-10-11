import groovy.xml.*
import com.twmacinta.util.MD5

def zipFile = new File(project.build.directory, "${project.build.finalName}.zip")
def zipFileHash = MD5.asHex(MD5.getHash(zipFile))
def builder = new StreamingMarkupBuilder()
def xml = builder.bind {
	'marytts-install'(xmlns: 'http://mary.dfki.de/installer') {
		voice(gender: 'female', locale: 'en-US', name: project.properties.voicename, type: 'hsmm', version: project.version) {
			delegate.description project.description
			license(href: 'http://mary.dfki.de/download/voices/arctic-license.html')
			'package'(filename: zipFile.name, md5sum: zipFileHash, size: zipFile.size()) {
				location(folder: true, href: "http://mary.dfki.de/download/$project.version/")
			}
			files "lib/${project.build.finalName}.jar"
			depends(language: 'en-US', version: project.version)
		}
	}
}
def xmlFile = new File(project.build.directory, "$project.build.finalName-component.xml")
xmlFile.text = XmlUtil.serialize(xml)

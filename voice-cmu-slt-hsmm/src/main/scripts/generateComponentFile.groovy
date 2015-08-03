import com.twmacinta.util.MD5

// First, fill the zip file's size and md5 sum into properties:
def zipfile = new File("$project.build.directory/$project.artifactId-${project.version}.zip")
def zip_size = zipfile.size()
def zip_md5 = MD5.asHex(MD5.getHash(zipfile))

// Second, copy component file template into target space with the right name
def tempdir = new File("$project.build.directory/tmp-component/")
tempdir.mkdirs()
def target = new File("$tempdir/$project.artifactId-$project.version-component.xml")
def source = new File("$project.basedir.path/src/non-packaged-resources/voice-component.xml")
target.write(source.text)

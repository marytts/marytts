export CLP=~/dev-version/istc-eclipse/OpenMary-IT/java/
javac -cp $CLP/maryclient.jar:$CLP/mary-common.jar  MaryClientTranscription.java 
java -cp .:$CLP/maryclient.jar:$CLP/mary-common.jar MaryClientTranscription


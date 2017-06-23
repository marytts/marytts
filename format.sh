#!/bin/zsh

eclipse -nosplash -vm /usr/bin/java -application org.eclipse.jdt.core.JavaCodeFormatter -verbose -config eclipse-code-style.xml  **/*.java

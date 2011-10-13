#LIA_phon directory, just for the making of the ressources
LIA_PHON_REP=$MARY_BASE/lib/modules/fr/lia_phon
export LIA_PHON_REP

#Install csh
sudo apt-get install csh

#Make the ressources
make all
make ressource
#make check

#Set the lexicon to 80k words
make lex80k
LIA_PHON_LEX=lex80k
export LIA_PHON_LEX

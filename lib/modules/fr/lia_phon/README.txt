    --------------------------------------------------------
    LIA_PHON : Un systeme complet de phonetisation de textes
    --------------------------------------------------------

    Copyright (C) 2001 FREDERIC BECHET

    ..................................................................

    This file is part of LIA_PHON

    LIA_PHON is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
    ..................................................................

    For any publication related to scientific work using LIA_PHON,
    the following reference paper must be mentioned in the bibliography: 
        
    Bechet F., 2001, "LIA_PHON - Un systeme complet de phonetisation
    de textes", revue Traitement Automatique des Langues (T.A.L.)
    volume 42, numero 1/2001, edition Hermes
    ..................................................................
                              
    Contact :
              FREDERIC BECHET - LIA - UNIVERSITE D'AVIGNON
              AGROPARC BP1228 84911  AVIGNON  CEDEX 09  FRANCE
              frederic.bechet@lia.univ-avignon.fr
    ..................................................................


 ------
 README
 ------

  I   : Content of the package LIA_PHON
  II  : Installation
  III : Useful scripts
  IV  : Quick overview of each LIA_PHON command
  V   : Data format
  VI  : Warning
 

I - Content of the package LIA_PHON
-----------------------------------

Once you have uncompress and untar the file 'lia_phon.tar.gz',
a directory 'lia_phon' is created, containing the following
files and directories:

  - Makefile : compiling sources and data files of LIA_PHON
  - README   : this present file
  - /bin     : directory which will contain the executable files
  - /data    : directory containing the following data files
        - desigle.pron		   : decision spelled/read for acronyms
        - epeler_sig.pron	   : pronunciation rules for spelled acronyms
        - lire_sig.pron		   : pronunciation rules for read acronyms
        - french01.pron		   : pronunciation rules for standard French
        - h_aspi.sirlex		   : exception list of words starting with an 'h'
        - initfile.lia		   : correspondence phonetic alphabets MBROLA/LIA_PHON
        - lex10k		   : 10000 words lexicon used by the POS tagger
	- lex80k                   ; a 80k word lexicon used by the POS tagger
        - list_chif_virgule	   : list of words possibly following a digit string
        - list_exep		   : pronunciation exceptions of 'french01.pron' rule database
        - lm3classe.arpa	   : ARPA-standard LM for POS tagging produced by CMU-Cambridge SLMT
        - model_[un,bi,tri]	   : 3-letter models for the proper-name identification
        - model_morpho.[un,bi,tri] : 3-letter models for out-of-vocabulary words in POS tagging
        - propername_[1-8].pron    : pronunciation rules for different proper-name sets
        - regles_l.pro3		   : rules managing the liaison insertion between words
        - regles_retik		   : post-processing POS tagging specific to Text-to-Speech transcription
        - rule_phon.pro		   : post-processing rules for the phonetic strings
        - rule_variante.pro	   : rules allowing to generate alternate pronunciations
  - /doc     : documentation about LIA_PHON as follows
	- gnu_gpl.txt		   : GNU General Public License Version 2
	- lia_phon.htm		   : complete description of LIA_PHON components
	- test.txt		   : test file
	- test.ola		   : result you should obtain from the test file if everything is OK
  - /object  : object files generated
  - /script  : script command for using LIA_PHON
	- lia_make_lexique_reacc   : compilation of the lexicon data of the POS tagger
	- lia_nett		   : tokenize in word and sentence, process digit string
	- lia_delete_lexicon       : delete all the compiled files of a given lexicon
	- lia_taggreac		   : POS tagging + text accentuation
	- lia_phon		   : grapheme to phoneme transcription
	- lia_text2phon		   : text to phonetization (LIA_PHON format)
	- lia_text2mbrola	   : text to speech with prosodic information (MBROLA format)
	- lia_lex2phon             : phonetization of a lexicon for all the possible POSs of each word
	- lia_genere_phrase_mbrola : one MBROLA file per sentence
  - /src     : source files contained in the following directories
	- aphon			   : grapheme to phoneme transcription
	- format		   : tokenization
	- liaison		   : liaison generation
	- libgram		   : n-gram LM
	- post_process		   : phonetic string post-processing
	- prosodie		   : prosodic information generation
	- retik			   : specific tagging to grapheme to phoneme transcription
	- tagg			   : POS tagging + text accentuation
	- variante		   : alternate pronunciation generation




II - Installation
-----------------

LIA_PHON is composed of a set of modules all written in
standard C, using only standard libraries. It has been
successfully compiled on the following UNIX environments:
- HP-UX
- SUN-SOLARIS
- IRIX
- LINUX

1) To install the package, you must first set the environment
   variable: 'LIA_PHON_REP' with the path of the root directory 'lia_phon'.

   For example, in c-shell:

   setenv LIA_PHON_REP /usr/tools/lia_phon

2) Then edit the file 'Makefile' in order to change, if
   necessary the C-compiler and the compilation options
   which will be used to compile the package.

3) Compile the package with the command: 'make all'

4) Compile the resources with the command: 'make ressource'

5) Check is everything is OK with the command 'make check'

6) If you want to use the 80k word lexicon instead of the default
   10k word lexicon (unnecessary for text-to-speech application but
   useful for the phonetization of a lexicon) just add the following
   commands:
   make lex80k
   setenv LIA_PHON_LEX lex80k

That's all !!

If you want to suppress the compiled files, just execute:

make clean
make clean_ressource




III - Useful scripts
--------------------

- Text-to-speech scripts

LIA_PHON contains a set of scripts that transform a raw
text into its phonetic form, which can be used as an input
for the speech synthesizer MBROLA. There are four main steps in
this process:
1) Formatting the text
2) POS tagging + accentuation
3) Grapheme-to-Phoneme transcription
4) MBROLA format

Three scripts summarize all these steps:

1- lia_text2phon [-reacc -syllabe]
	input = stdin
	output= stdout
  Take as input a raw text and output a tagged, phonetized
  text in the LIA_PHON format
  For example:
  $LIA_PHON_REP/script/lia_text2phon < test.txt > test.pho

  The option '-syllabe' output the syllabisation of the input words as well as their phonetisation.
  The syllabisation is in the second field (with TAB), for example:
  $LIA_PHON_REP/script/lia_text2phon -syllabe < test.txt | cut -f2
  will produce the syllabisation of the file test.txt

2- lia_text2mbrola [-reacc]
	input = stdin
	output= stdout
  Take as input a raw text and output a MBROLA file with
  very very basic prosodic information.
  (see http://tcts.fpms.ac.be/synthesis/ for information about
  MBROLA speech synthesizer)
  For example:
  $LIA_PHON_REP/script/lia_text2mbrola < test.txt > test.ola
  Then, to produce a '.wav' file with MBROLA, one can use:
  $MBROLA_REP/mbrola -I $LIA_PHON_REP/data/initfile.lia $MBROLA_REP/fr1 test.ola test.wav 

3- lia_genere_phrase_mbrola [-reacc]
	input  = filename
	output = filename.i.ola
  Take as input a text filename and output N '.ola' files
  for each sentence of the text file.

All these scripts accept the option '-reacc'. If this option is set, the text-to-speech
system considers that some accents might be missing on some words and tries to correct them.
However this can lead to some accentuation errors. If you are sure that your text has all
its accent correct, it's better not to use this option.

- Phonetization of a lexicon

LIA_PHON contains also a script which takes as input a list of words and output
one or several phonetization for each word. The input format is a text file
with one word on each line.
The output format is a text file with, on each line, a triplet:
<word> <phonetical form> <[POS]>
If a word has several POSs in the LIA_PHON_LEX lexicon, one phonetization
will be proposed for each POS, even if the phonetization are similar. For
example, if the input file contains:

mange
couvent

The output file will be:

mange mmanjj [V2S]
mange mmanjj [V3S]
mange mmanjj [V1S]
couvent kkouvv [V3P]
couvent kkouvvan [NMS]

For the acronyms and the proper names, only one phonetization is proposed.

The command which performs this process is:

$LIA_PHON_REP/script/lia_lex2phon

where the input file is given on the standard input and the result
in the standard output.

IV - Quick overview of each LIA_PHON command
--------------------------------------------

Each step in the text processing will be now illustrated on this
small raw text example:
--
Ceci est un test. De plus en plus, le 1 janvier tombe un lundi. Il couttent
vrraiment 1,08 euros. Il eleve un debat deja eleve sur le TALN.
--

1) Formatting the text
This is done with the script: $LIA_PHON_REP/script/lia_nett
- input = stdin
- output= stdout

This script tokenize the text according to the lexicon used by the
POS tagger. Then, the text is split into sentences, marked by the
tags <s> (beginning of a sentence) and </s> (end of a sentence).
The eventual capital letter of each first word of a sentence is then
removed.
Finally, digit strings are converted into words with respect to the symbol ','
and the output text contains one word on each line.

This is the result of this script on the previous text:

<s>
ceci
est
un
test
.
</s>
<s>
de_plus_en_plus
,
le
premier
janvier
tombe
un
lundi
.
</s>
<s>
il
couttent
vrraiment
un
virgule
zéro
huit
euros
.
</s>
<s>
il
eleve
un
debat
deja
eleve
sur
le
TALN
.
</s>


2) POS tagging + accentuation
This is done by the script: $LIA_PHON_REP/script/lia_taggreac
   - input  : stdin
   - output : stdout
This script takes the output of 'lia_nett' and process a POS tagging and
a text accentuation. The out-of-vocabulary words are processed by a POS guesser
(for example, the words 'couttent' and 'vrraiment' in the previous example),
except for unknown proper-names (capitalize words within a sentence) which
remain with the tag 'MOTINC'.

This is the result of this script on the output of 'lia_nett':

<s>		ZTRM
ceci		PDEMMS
est		VE3S
un		DETMS
test		NMS
.		YPFOR
</s>		ZTRM
<s>		ZTRM
de_plus_en_plus	ADV
,		YPFAI
le		DETMS
premier		AMS
janvier		NMS
tombe		V3S
un		DETMS
lundi		NMS
.		YPFOR
</s>		ZTRM
<s>		ZTRM
il		PPER3MS
couttent	V3S
vrraiment	ADV
un		DETMS
virgule		CHIF
zéro		CHIF
huit		CHIF
euros		NMP
.		YPFOR
</s>		ZTRM
<s>		ZTRM
il		PPER3MS
élève		V3S
un		DETMS
débat		NMS
déjà		ADV
élevé		AMS
sur		PREP
le		DETMS
TALN		MOTINC
.		YPFOR
</s>		ZTRM


3) Grapheme-to-Phoneme transcription
This is done with the script $LIA_PHON_REP/script/lia_phon
   - input  : stdin
   - output : stdout
This script takes as input the output of 'lia_taggreac' and
phonetize, insert liaison between words, decide to read or spell
acronyms, calculate a pronunciation for proper-names, and
check for exceptions to the transcription rules.

This is the result of this script on the output of 'lia_taggreac'
on the previous text:

<s>		##			[ZTRM->EXCEPTION]
ceci		sseessii		[PDEMMS]
est		ei			[VE3S]
un		ttun			[DETMS]
test		ttaissttee		[NMS]
pause		##
</s>		##			[ZTRM->EXCEPTION]
<s>		##			[ZTRM->EXCEPTION]
de_plus_en_plus	ddeepplluuzzanpplluuss	[ADV]
pause		##
le		llee			[DETMS]
premier		pprreemmyyei		[AMS]
janvier		jjanvvyyei		[NMS]
tombe		ttonbb			[V3S]
un		un			[DETMS]
lundi		llunddii		[NMS]
pause		##
</s>		##			[ZTRM->EXCEPTION]
<s>		##			[ZTRM->EXCEPTION]
il		iill			[PPER3MS]
couttent	kkoutt			[V3S]
vrraiment	vvrrrraimman		[ADV]
un		un			[DETMS]
virgule		vviirrgguull		[CHIF]
zéro		zzeirrau		[CHIF]
huit		uyii			[CHIF]
euros		tteurrau		[NMP]
pause		##
</s>		##			[ZTRM->EXCEPTION]
<s>		##			[ZTRM->EXCEPTION]
il		iill			[PPER3MS]
élève		eillaivv		[V3S]
un		un			[DETMS]
débat		ddeibbaa		[NMS]
déjà		ddeijjaa		[ADV]
élevé		eilleevvei		[AMS]
sur		ssuurr			[PREP]
le		llee			[DETMS]
TALN		ttaallnn		[MOTINC->SIGLE->SIGLE_LU]
pause		##
</s>		##			[ZTRM->EXCEPTION]
<FIN>		????			[]


4) MBROLA format
In order to use the MBROLA speech synthesizer
(see http://tcts.fpms.ac.be/synthesis/ for more details),
the output of 'lia_phon' can be formatted into MBROLA
format, by adding some (minimal) prosodic information
to the phonetic output.
WARNING: this prosodic generation is here just to avoid
listening to a 'flat' voice, but have no pretension
to reflect a 'realistic' prosody !!
To format the output of 'lia_phon' you must use the
command:
$LIA_PHON_REP/bin/lia_add_proso
   - input  : stdin
   - output : stdout




V - Data format
---------------

LIA_PHON uses several kind of resources:

1- Grapheme-to-Phoneme transcription rule databases
2- Lexicons
3- Part-Of-Speech resources
4- Tagging and post-processing rules

You can update any of these resources and the following
quick description of each of them should answer most of the
questions. BUT, don't forget to compile again the resources
each time you modify one of the data files.


1- Grapheme-to-Phoneme transcription rule databases

The main database is called 'french01.pron' and contains
more than 1000 rules for the pronunciation of standard
French.

New rules can be added anywhere as the database is sorted
each time it is loaded.

The format of the rules of 'french01.pron' is as follow:

regle(num,<left,graf,right>,pho,cont,ex) -> ;

num    : identification number of the rule
left   : left context of the grapheme to process
graf   : grapheme to transcribe cut into syllable
right  : right context of the grapheme to process
pho    : phonetic string corresponding to the grapheme to process
cont   : morphological constraint (verb V or non-verb NV)
ex     : example

The following symbols can be used in the graphical context :

V      : any vowel
C      : any consonant
S      : 's' or nothing

The grapheme are encoded as  ASCII  ISO8859-1

WARNING: the grapheme strings MUST be cut into syllables,
for example, the word 'balbutier' will be split into 'bal bu tier'

The phonetic alphabet used is describe as follows (with the correspondence
with the SAMPA alphabet used in the MBROLA system) :

LIA  SAMPA    EXAMPLES
ii     i     idiot, ami
ei     e     ému, été
ee     3     ce, je, ne
ai     E     perdu, maison
aa     a     alarme, patte
oo     O     obstacle, corps
au     o     auditeur, beau
ou     u     coupable, loup
uu     y     punir, élu
EU     2     creuser, deux
oe     9     malheureux, peur
eu     @     petite, fortement
in     e~    peinture, matin
an     a~    vantardise, temps
on     o~    rondeur, bon
un     9~    lundi, brun
yy     j     piétiner, choyer
ww     w     quoi, fouine
pp     p     patte, repas, cap
tt     t     tête, net
kk     k     carte, écaille, bec
bb     b     bête, habile, robe
dd     d     dire, rondeur, chaud
gg     g     gauche, égal, bague
ff     f     feu, affiche, chef
ss     s     soeur, assez, passe
ch     S     chanter, machine, poche
vv     v     vent, inventer, rêve
zz     z     zéro, raisonner, rose
jj     Z     jardin, manger, piège
ll     l     long, élire, bal
rr     R     rond, charriot, sentir
mm     m     madame, aimer, pomme
nn     n     nous, punir, bonne
gn     J     oignon, charlemagne, agneau
vv     v     voiture, voile, voix
uy     H     huit, lui, poursuivre
##     _     (silence marker)


Several other database are used by LIA_PHON, with the
same format:
        - desigle.pron		   : decision spelled/read for acronyms
        - epeler_sig.pron	   : pronunciation rules for spelled acronyms
        - lire_sig.pron		   : pronunciation rules for read acronyms
        - french01.pron		   : pronunciation rules for standard French
        - propername_[1-8].pron    : pronunciation rules for different proper-name sets


2- Lexicons

There is 3 lexicons used by LIA_PHON (except the POS resources):
- h_aspi.sirlex
  Exception list of words starting with an 'h'
  Format of each line: indice\tword

- list_exep
  Pronunciation exceptions of 'french01.pron' database
  Format of each line: word phonetic_string

- list_chif_virgule
  List of words possibly following a digit string
  Format of each line: word


3- Part-Of-Speech resources

The main editable resource of the POS tagging is the lexicon. The default one
given with LIA_PHON contains 10K words, and is stored in the text file: lex10K
The format of the dictionary is as follows:

- one word on each line
- format of each line:
word POS1 freq(word,POS1) lemma(word,{POS1) POS2 freq(word,POS2) lemma(word,POS2) ....

For example:
assise AFS 213 assis NFS 733 assise VPPFS 320 asseoir

To add a new word, just add a new line at this file, and if you
don't have a corpus for estimating the frequency of each
couple (word,POS), just put '1' instead.

There's also a 80k word lexicon which can be used instead of the default one
(not necessary for standard text-to-speech application). To use it, you
have to perform the folllowing commands (in the LIA_PHON_REP directory):

make lex80k
setenv LIA_PHON_LEX lex80k

4- Tagging and post-processing rules

These rules post-process the phonetic strings produced by LIA_PHON, calculate
a possible liaison between two words and add new tags specific to the
Grapheme-to-Phoneme transcription process.
They are stored in the files: rule_phon.pro, regles_retik and regles_l.pro3




VI - Warning

Be careful of the accent encoding of the files !!
Check that the accents of the rule file 'french01.pron' are
correct (ASCII  ISO8859-1). If not, you have to use an
accent transcoder to correct them. This transcoding MUST
be applied to ALL the files in the directories /data
and /src


Contact
-------

If you have any problem or question, contact me at :

frederic.bechet@univ-avignon.fr
 


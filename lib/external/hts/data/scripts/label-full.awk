# ----------------------------------------------------------------- #
#           The HMM-Based Speech Synthesis System (HTS)             #
#           developed by HTS Working Group                          #
#           http://hts.sp.nitech.ac.jp/                             #
# ----------------------------------------------------------------- #
#                                                                   #
#  Copyright (c) 2001-2011  Nagoya Institute of Technology          #
#                           Department of Computer Science          #
#                                                                   #
#                2001-2008  Tokyo Institute of Technology           #
#                           Interdisciplinary Graduate School of    #
#                           Science and Engineering                 #
#                                                                   #
# All rights reserved.                                              #
#                                                                   #
# Redistribution and use in source and binary forms, with or        #
# without modification, are permitted provided that the following   #
# conditions are met:                                               #
#                                                                   #
# - Redistributions of source code must retain the above copyright  #
#   notice, this list of conditions and the following disclaimer.   #
# - Redistributions in binary form must reproduce the above         #
#   copyright notice, this list of conditions and the following     #
#   disclaimer in the documentation and/or other materials provided #
#   with the distribution.                                          #
# - Neither the name of the HTS working group nor the names of its  #
#   contributors may be used to endorse or promote products derived #
#   from this software without specific prior written permission.   #
#                                                                   #
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            #
# CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       #
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          #
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          #
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS #
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          #
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   #
# TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     #
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON #
# ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   #
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    #
# OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           #
# POSSIBILITY OF SUCH DAMAGE.                                       #
# ----------------------------------------------------------------- #

{
##############################
###  SEGMENT

#  boundary
   printf "%10.0f %10.0f ", 1e7 * $65, 1e7 * $66

#  pp.name
    printf "%s",  ($63 == "0") ? "x" : $63
#  p.name
    printf "^%s", ($1  == "0") ? "x" : $1
#  c.name
    printf "-%s", $2
#  n.name
    printf "+%s", ($3  == "0") ? "x" : $3
#  nn.name
    printf "=%s", ($64 == "0") ? "x" : $64 

#  position in syllable (segment)
    printf "@"
    printf "%s",  ($2 == "pau") ? "x" : $4 + 1
    printf "_%s", ($2 == "pau") ? "x" : $12 - $4

##############################
###  SYLLABLE

## previous syllable

#  p.stress
    printf "/A:%s", ($2 == "pau") ? $49 : $5
#  p.accent
    printf "_%s", ($2 == "pau") ? $51 : $8
#  p.length
    printf "_%s", ($2 == "pau") ? $53 : $11

## current syllable

#  c.stress
    printf "/B:%s", ($2 == "pau") ? "x" : $6
#  c.accent
    printf "-%s", ($2 == "pau") ? "x" : $9
#  c.length
    printf "-%s", ($2 == "pau") ? "x" : $12

#  position in word (syllable)
    printf "@%s", ($2 == "pau") ? "x" : $14 + 1
    printf "-%s", ($2 == "pau") ? "x" : $30 - $14

#  position in phrase (syllable)
    printf "&%s", ($2 == "pau") ? "x" : $15 + 1
    printf "-%s", ($2 == "pau") ? "x" : $16 + 1

#  position in phrase (stressed syllable)
    printf "#%s", ($2 == "pau") ? "x" : $17 + 1
    printf "-%s", ($2 == "pau") ? "x" : $18 + 1

#  position in phrase (accented syllable)
    printf  "$"
    printf "%s", ($2 == "pau") ? "x" : $19 + 1
    printf "-%s", ($2 == "pau") ? "x" : $20 + 1

#  distance from stressed syllable
    printf "!%s", ($2 == "pau") ? "x" : $21
    printf "-%s", ($2 == "pau") ? "x" : $22

#  distance from accented syllable 
    printf ";%s", ($2 == "pau") ? "x" : $23
    printf "-%s", ($2 == "pau") ? "x" : $24

#  name of the vowel of current syllable
    printf "|%s", ($2 == "pau") ? "x" : $25

## next syllable

#  n.stress
    printf "/C:%s", ($2 == "pau") ? $50 : $7
#  n.accent
    printf "+%s", ($2 == "pau") ? $52 : $10
#  n.length
    printf "+%s", ($2 == "pau") ? $54 : $13

##############################
#  WORD

##################
## previous word

#  p.gpos
    printf "/D:%s", ($2 == "pau") ? $55 : $26
#  p.lenght (syllable)
    printf "_%s", ($2 == "pau") ? $57 : $29

#################
## current word

#  c.gpos
    printf "/E:%s", ($2 == "pau") ? "x" : $27
#  c.lenght (syllable)
    printf "+%s", ($2 == "pau") ? "x" : $30

#  position in phrase (word)
    printf "@%s", ($2 == "pau") ? "x" : $32 + 1
    printf "+%s", ($2 == "pau") ? "x" : $33

#  position in phrase (content word)
    printf "&%s", ($2 == "pau") ? "x" : $34 + 1
    printf "+%s", ($2 == "pau") ? "x" : $35

#  distance from content word in phrase
    printf "#%s", ($2 == "pau") ? "x" : $36
    printf "+%s", ($2 == "pau") ? "x" : $37

##############
## next word

#  n.gpos
    printf "/F:%s", ($2 == "pau") ? $56 : $28
#  n.lenghte (syllable)
    printf "_%s", ($2 == "pau") ? $58 : $31

##############################
#  PHRASE

####################
## previous phrase

#  length of previous phrase (syllable)
    printf "/G:%s", ($2 == "pau") ? $59 : $38

#  length of previous phrase (word)
    printf "_%s"  , ($2 == "pau") ? $61 : $41

####################
## current phrase

#  length of current phrase (syllable)
    printf "/H:%s", ($2 == "pau") ? "x" : $39

#  length of current phrase (word)
    printf "=%s",   ($2 == "pau") ? "x" : $42

#  position in major phrase (phrase)
    printf "^";
    printf "%s", $44 + 1
    printf "=%s", $48 - $44

#  type of tobi endtone of current phrase
    printf "|%s",  $45

####################
## next phrase

#  length of next phrase (syllable)
    printf "/I:%s", ($2 == "pau") ? $60 : $40

#  length of next phrase (word)
    printf "=%s",   ($2 == "pau") ? $62 : $43

##############################
#  UTTERANCE

#  length (syllable)
    printf "/J:%s", $46

#  length (word)
    printf "+%s", $47

#  length (phrase)
    printf "-%s", $48

    printf "\n"
}

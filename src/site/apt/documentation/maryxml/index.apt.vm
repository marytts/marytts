   ---
   MaryXML
   ---

MaryXML

*Introduction

	The MARY system uses an internal XML-based representation language called MaryXML. The purpose of an XML-based representation language is to serve as the data representation format inside a TTS system. For that reason, the concepts represented in it are low-level, detailed, and specific to the design decisions, modules, and scientific theories underlying the TTS system. By means of the Document Object Model (DOM), a standardised object-oriented representation of an XML document, the TTS system modules can operate directly on the XML document, interpreting and adding information.

	The syntax of a MaryXML document reflects the information required by the modules in the TTS system. Concepts which can also be encoded in speech synthesis input markup languages, such as sentence boundaries and global prosodic settings, are represented by the same tags as used in the W3C SSML specification.

	Most of the information to be represented in MaryXML, however, is too detailed to be expressed using tags from input markup languages. Specific MaryXML tags represent the low-level information required during various processing steps.

	The MaryXML syntax was designed to maintain a certain degree of readability for the human user, by keeping information redundancy at a minimum.

	The syntax of a correct MaryXML document is defined in the {{{../../lib/MaryXML.xsd}MaryXML schema file}}.
	
*Examples

	MaryXML, when used as a rich input to a speech synthesis system, provides a powerful method for controlling the behaviour of the TTS system for the given input. It can be thought of as a "remote control" for the TTS system. 
	The input data below is to be fed into the system as data of type RAWMARYXML, e.g. via the {{{${project.url}:59125}online demo}}
	
	<<Note:>> These examples refer to MaryXML version 0.5, as it is used since MARY TTS 4.0. In previous versions, some attribute names were different, e.g. the attribute name "sampa" was used instead of the current "ph".

**Overall prosodic settings

+------------------------------------------+
<?xml version="1.0" encoding="UTF-8" ?>
<maryxml version="0.4"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns="${project.url}/2002/MaryXML"
xml:lang="en">
<prosody rate="+20%" pitch="+20%" range="-10%" volume="loud">
This is something you have to see!
</prosody>
</maryxml>
+------------------------------------------+

**Forcing location and/or type of ToBI accents and boundaries

+------------------------------------------+
<?xml version="1.0" encoding="UTF-8" ?>
<maryxml version="0.5"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns="${project.url}/2002/MaryXML"
xml:lang="en-US">
Can you <boundary duration="100"/>
<t accent="H*">please</t> <boundary duration="100"/>
<t accent="unknown">listen</t> to <t accent="none">John</t>!
</maryxml>
+------------------------------------------+

**Marking individual words as English in German text

+------------------------------------------+
<?xml version="1.0" encoding="UTF-8" ?>
<maryxml version="0.5"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns="${project.url}/2002/MaryXML"
xml:lang="de">
Klicken Sie den <t xml:lang="en">Button</t>, um die <t xml:lang="en">Beatles</t> zu spielen.
</maryxml>

+------------------------------------------+
	(doesn't work in 4.3, see also {{{http://mary.opendfki.de/ticket/394}ticket #394}})

**Force pronounciation of individual words

+------------------------------------------+
<?xml version="1.0" encoding="UTF-8" ?>
<maryxml version="0.5"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns="${project.url}/2002/MaryXML"
xml:lang="de">
Klicken Sie den <t ph="'ba-t@n">Button</t>, um die <t ph="'bi:-t@ls">Beatles</t> zu spielen.
</maryxml>
+------------------------------------------+

**Force boundaries and question intonation

+------------------------------------------+
<?xml version="1.0" encoding="UTF-8" ?>
<maryxml version="0.5"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns="${project.url}/2002/MaryXML"
xml:lang="de">
Möchten Sie <boundary duration="400"/>

<prosody contour="(0%,-10%) (50%,-20%) (70%,-10%) (100%,+200%)">
etwas Musik 
</prosody>
</maryxml>
+------------------------------------------+

	Here, the intonation contour is explicitly given by a sequence of frequency-time targets (x,y). In each of these targets, the first item is a percentage of the duration of the enclosed text; the second number is a relative change of the intonation contour over the default.














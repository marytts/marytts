<h1>SpeechSynthesisTool</h1><br />

This is a Product for the Plone Content Management System (http://plone.org) implementing
a plone-based access to speech synthesis. 

1. <h2>Installation</h2>

	First, install MARY TTS and run the server -- see http://mary.dfki.de

	Then, unpack SpeechSynthesisTool in your portal's Products/ folder; restart zope; install product using the portal_quickinstaller (in the Zope Management Interface); and configure the location of your MARY TTS server under "site setup"->"SpeechSynthesisTool Setup".


2. <h2>What is this product about?</h2>

	This Product provides a SpeechSynthesisTool for plone. The tool
	implements a client to the client-server based MARY TTS system.
	It can connect to a configurable text-to-speech synthesis server,
	to convert text into speech audio. A simple-to-use API is provided
	enabling the use of the speech synthesis tool from page templates.

3. <h2>Credits</h2>

	Developed by Christian Federmann and Marc Schröder (DFKI GmbH)

	This work has been done for the EC Project HUMAINE<br />
	(IST-507422) -- see http://emotion-research.net.

4. <h2>Changelog</h2>

	2006-02-21 0.1
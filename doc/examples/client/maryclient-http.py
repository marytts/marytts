#!/usr/bin/env python
import httplib, urllib

# A basic mary client in Python,
# kindly donated to the MARY TTS project
# by Hugh Sasse. Thanks Hugh!

# A very basic Python class for accessing
# the MARY TTS system using the modern 
# HTTP server.
# Warning, this is probably ghastly Python,
# most of my time of late has been with 
# other languages, so I'm not up to date
# with all the stylistic conventions of 
# modern Python.
# This does seem to work OK though.

class maryclient:
    """A basic handler for MARY-TTS HTTP clients
    
       At present, there is no checking for
       allowed voices, locales, and so on.
       Most of the useful parameters can be
       accessed by get_ and set_ methods.
       Relying on winsound, this is Windows
       specific.
    """
    def __init__(self):
        """Set up useful defaults (for
           people in England, anyway)"""
        self.host = "127.0.0.1"
        self.port = 59125
        self.input_type = "TEXT"
        self.output_type = "AUDIO"
        self.audio = "WAVE_FILE"
        self.locale = "en_GB"
        self.voice = "dfki-prudence-hsmm"

    def set_host(self, a_host):
        """Set the host for the TTS server."""
        self.host = a_host

    def get_host(self):
        """Get the host for the TTS server."""
        self.host

    def set_port(self, a_port):
        """Set the port for the TTS server."""
        self.port = a_port

    def get_port(self):
        """Get the port for the TTS server."""
        self.port

    def set_input_type(self, type):
        """Set the type of input being 
           supplied to the TTS server
           (such as 'TEXT')."""
        self.input_type = type

    def get_input_type(self):
        """Get the type of input being 
           supplied to the TTS server
           (such as 'TEXT')."""
        self.input_type

    def set_output_type(self, type):
        """Set the type of input being 
           supplied to the TTS server
           (such as 'AUDIO')."""
        self.output_type = type

    def get_output_type(self):
        """Get the type of input being 
           supplied to the TTS server
           (such as "AUDIO")."""
        self.output_type

    def set_locale(self, a_locale):
        """Set the locale
           (such as "en_GB")."""
        self.locale = a_locale

    def get_locale(self):
        """Get the locale
           (such as "en_GB")."""
        self.locale

    def set_audio(self, audio_type):
        """Set the audio type for playback
           (such as "WAVE_FILE")."""
        self.audio = audio_type

    def get_audio(self):
        """Get the audio type for playback
           (such as "WAVE_FILE")."""
        self.audio

    def set_voice(self, a_voice):
        """Set the voice to speak with
           (such as "dfki-prudence-hsmm")."""
        self.voice = a_voice

    def get_voice(self):
        """Get the voice to speak with
           (such as "dfki-prudence-hsmm")."""
        self.voice

    def generate(self, message):
        """Given a message in message,
           return a response in the appropriate
           format."""
        raw_params = {"INPUT_TEXT": message,
                "INPUT_TYPE": self.input_type,
                "OUTPUT_TYPE": self.output_type,
                "LOCALE": self.locale,
                "AUDIO": self.audio,
                "VOICE": self.voice,
                }
        params = urllib.urlencode(raw_params)
        headers = {}

        # Open connection to self.host, self.port.
        conn = httplib.HTTPConnection(self.host, self.port)

        # conn.set_debuglevel(5)
        
        conn.request("POST", "/process", params, headers)
        response = conn.getresponse()
        if response.status != 200:
            print response.getheaders()
            raise RuntimeError("{0}: {1}".format(response.status,
                response.reason))
        return response.read()

# If this is invoked as a program, just give
# a greeting to show it is working.
# The platform specific code is moved to this
# part so that this file may be imported without
# bringing platform specific code in.
if __name__ == "__main__":

    # For handling command line arguments:
    import sys
    import platform

    # check we are on Windows:
    system = platform.system().lower()
    if (system == "windows"):

        import winsound

        class Player:
            def __init__(self):
                pass

            def play(self, a_sound):
                winsound.PlaySound(a_sound, winsound.SND_MEMORY)

    #if ("cygwin" in system):
    else:
        # Not sure how to do audio on cygwin,
        # portably for python. So have a sound
        # player class that doesn't play sounds.
        # A null object, if you like.
        class Player:
            def __init__(self):
                pass

            def play(self, a_sound):
            	print("Here I would play a sound if I knew how")
                pass

    # Probably want to parse arguments to 
    # set the voice, etc., here

    client = maryclient()
    client.set_audio("WAVE_FILE")  # for example

    player = Player()
    the_sound = client.generate("hello from Mary Text to Speech, with Python.")
    if client.output_type == "AUDIO":
        player.play(the_sound)

# vi:set sw=4 et:

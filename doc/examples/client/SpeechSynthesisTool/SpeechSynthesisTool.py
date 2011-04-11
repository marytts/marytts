from Products.CMFCore.utils import UniqueObject
from Products.CMFCore.utils import getToolByName
from OFS.SimpleItem import SimpleItem 
from Globals import InitializeClass
from StringIO import StringIO
from MaryClient import MaryClient, Voice, DataType
from AccessControl import ClassSecurityInfo
from zLOG import LOG, INFO, DEBUG, WARNING

class SpeechSynthesisTool (UniqueObject, SimpleItem): 
    """
    Speech Synthesis Tool provides Plone with access to speech synthesis.
    It implements a client for the MARY TTS client/server system.
    You need a separate MARY TTS server running on a machine that you can
    configure via speech synthesis properties.
    You can download it from: http://mary.dfki.de
    """ 
    id = 'speech_synthesis_tool' 
    meta_type = 'Speech Synthesis Tool' 
    plone_tool = 1
    security = ClassSecurityInfo()
    
    def __init__(self):
        self.id = 'speech_synthesis_tool'
    
    def __maryClient(self):
        if not hasattr(self, 'maryClient') or self.maryClient is None:
            # else, try to instantiate a maryclient
            speech_synthesis_properties = getToolByName(self, 'portal_properties').speech_synthesis_properties
            try:
                self.maryClient = MaryClient(
                    host=speech_synthesis_properties.server_host,
                    port=speech_synthesis_properties.server_port,
                    profile=True,
                    quiet=False
                )
                LOG('SpeechSynthesisTool', INFO, 'Connected to MARY server %s' % self.maryClient.getServerVersionInfo())
            except:
                message = "Cannot connect to MARY TTS server on '%s:%i'" \
                    % (speech_synthesis_properties.server_host,speech_synthesis_properties.server_port)
                LOG('SpeechSynthesisTool', WARNING, message)
                raise Exception(message)
        return self.maryClient
    
    def __dataTypeToDict(self, dataType):
        assert isinstance(dataType, DataType)
        return {'name':dataType.name,
                'locale':dataType.locale,
                'isInputType':dataType.isInputType,
                'isOutputType':dataType.isOutputType,
                'isTextType':dataType.isTextType()}
                
    def __voiceToDict(self, voice):
        assert isinstance(voice, Voice)
        return {'name':voice.name,
                'locale':voice.locale,
                'gender':voice.gender,
                'domain':voice.domain,
                'isLimitedDomain':voice.isLimitedDomain,
                'toString':str(voice)}

    security.declarePublic('getServerVersionInfo')
    def getServerVersionInfo(self):
        """ Get the MARY TTS server version info """
        return self.__maryClient().getServerVersionInfo()

    security.declarePublic('getAllDataTypes')
    def getAllDataTypes(self, locale=None):
        """ Get all data types from MARY TTS server.
        Returns a list of dicts, each of which represents one data type.
        """
        return [self.__dataTypeToDict(d) for d in self.__maryClient().getAllDataTypes(locale)]

    security.declarePublic('getInputDataTypes')
    def getInputDataTypes(self, locale=None):
        """ Get all input data types from MARY TTS server.
        Returns a list of dicts, each of which represents one data type.
        """
        return [self.__dataTypeToDict(d) for d in self.__maryClient().getInputDataTypes(locale)]

    security.declarePublic('getOutputDataTypes')
    def getOutputDataTypes(self, locale=None):
        """ Get all output data types from MARY TTS server.
        Returns a list of dicts, each of which represents one data type.
        """
        return [self.__dataTypeToDict(d) for d in self.__maryClient().getOutputDataTypes(locale)]

    security.declarePublic('getVoices')
    def getVoices(self, locale=None):
        """ Get all voices from MARY TTS server.
        Returns a list of dicts, each of which represents one voice.
        """
        return [self.__voiceToDict(v) for v in self.__maryClient().getVoices(locale)]

    security.declarePublic('getGeneralDomainVoices')    
    def getGeneralDomainVoices(self, locale=None):
        """ Get all general domain voices from MARY TTS server.
        Returns a list of dicts, each of which represents one voice.
        """
        return [self.__voiceToDict(v) for v in self.__maryClient().getGeneralDomainVoices(locale)]

    security.declarePublic('getLimitedDomainVoices')
    def getLimitedDomainVoices(self, locale=None):
        """ Get all limited domain voices from MARY TTS server.
        Returns a list of dicts, each of which represents one voice.
        """
        return [self.__voiceToDict(v) for v in self.__maryClient().getLimitedDomainVoices(locale)]

    security.declarePublic('getAvailableLanguages')
    def getAvailableLanguages(self):
        """ Check available voices and return a list of tuples (abbrev, name)
        representing the available languages -- e.g. [('en', 'English'),('de', 'German')].
        """
        return self.__maryClient().getAvailableLanguages()

    security.declarePublic('getServerExampleText')
    def getServerExampleText(self, dataType):
        """ """
        return self.__maryClient().getServerExampleText(dataType)
    
    security.declarePublic('process')
    def process(self, input, inputType, outputType, audioType=None, voice=None, REQUEST=None):
        """ """
        output = StringIO()
        if REQUEST is not None:
            mimetypes = {'AU':'basic', 'AIFF':'x-aiff', 'WAVE':'x-wav', 'MP3':'mp3'}
            extensions = {'AU':'au', 'AIFF':'aif', 'WAVE':'wav', 'MP3':'mp3'}
            if outputType == 'AUDIO':
                assert mimetypes.has_key(audioType), "Unknown audio type '%s'" % (audioType)
                mimetype = "audio/%s" % (mimetypes[audioType])
                filename = "mary.%s" % (extensions[audioType])
            else:
                mimetype = "text/plain"
                filename = "mary.txt"
            REQUEST.RESPONSE.setHeader('Content-Type', mimetype)
            REQUEST.RESPONSE.setHeader('Content-Disposition', 'filename="%s"' % (filename))
        
        self.__maryClient().process(input, inputType, outputType, audioType, voice, output)
        return output.getvalue()


InitializeClass(SpeechSynthesisTool)

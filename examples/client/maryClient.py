#!/usr/bin/python
# -*- coding: utf-8 -*-
import socket, sys, types, getopt

class MaryClient:
    specificationVersion = "0.1"
    
    """Python implementation of a MARY TTS client"""
    def __init__( self, host="cling.dfki.uni-sb.de", port=59125, profile=False, quiet=False ):
        self.host = host
        self.port = port
        self.profile = profile
        self.quiet = quiet
        self.allVoices = None # array of Voice objects
        self.voicesByLocaleMap = {} # Map locale strings to arrays of Voice objects
        self.allDataTypes = None # array of DataType objects
        self.inputDataTypes = None # array of DataType objects
        self.outputDataTypes = None # array of DataType objects
        self.serverExampleTexts = {}
        self.voiceExampleTexts = {}
        self.serverVersionInfo = ""
        
        if not self.quiet:
            sys.stderr.write( "MARY TTS Python Client %s\n" % ( self.specificationVersion ) )
            try:
                info = self.__getServerVersionInfo()
            except:
                sys.stderr.write( "Problem connecting to mary server at %s:%i\n" % ( self.host, self.port ) )
                raise
            sys.stderr.write( "Connected to %s:%i, " % ( self.host, self.port ) )
            sys.stderr.write( info )
            sys.stderr.write( '\n' )

    def __getServerInfo( self, request="", marySocket=None ):
        "Get answer to request from mary server. Returns a list of unicode strings"
        closeSocket = False
        if marySocket is None:
            closeSocket = True
            marySocket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
            marySocket.connect( ( self.host, self.port ) )
        assert type( marySocket ) is socket.SocketType
        maryFile = marySocket.makefile( 'rwb', 1 ) # read-write, line-buffered
        maryFile.write( unicode( request+"\n" ).encode( 'utf-8' ) )
        result = []
        while True:
            got = unicode( maryFile.readline().strip(), 'utf-8' )
            # read until end of file or an empty line is read:
            if not got: break
            result.append(got)
        if closeSocket:
            marySocket.close()
        return result

    def __getServerVersionInfo( self ):
        "Get version info from server. Returns a string"
        if self.serverVersionInfo == "":
            # need to get it from server
            self.serverVersionInfo = '\n'.join(self.__getServerInfo("MARY VERSION"))
        return self.serverVersionInfo

    def getAllDataTypes( self ):
        """Obtain a list of all data types known to the server. If the information is not
        yet available, the server is queried. This is optional information
        which is not required for the normal operation of the client, but
        may help to avoid incompatibilities.
        Returns an arry of DataType objects
        """
        if self.allDataTypes is None:
            self.__fillDataTypes()
        assert self.allDataTypes is not None and len( self.allDataTypes ) > 0
        return self.allDataTypes
         
    def getInputDataTypes( self ):
        """Obtain a list of input data types known to the server. If the information is not
        yet available, the server is queried. This is optional information
        which is not required for the normal operation of the client, but
        may help to avoid incompatibilities.
        Returns an arry of DataType objects
        """
        if self.inputDataTypes is None:
            self.__fillDataTypes()
        assert self.inputDataTypes is not None and len( self.inputDataTypes ) > 0
        return self.inputDataTypes

    def getOutputDataTypes( self ):
        """Obtain a list of output data types known to the server. If the information is not
        yet available, the server is queried. This is optional information
        which is not required for the normal operation of the client, but
        may help to avoid incompatibilities.
        Returns an arry of DataType objects
        """
        if self.outputDataTypes is None:
            self.__fillDataTypes()
        assert self.outputDataTypes is not None and len( self.outputDataTypes ) > 0
        return self.outputDataTypes


    def __fillDataTypes( self ):
        self.allDataTypes = []
        self.inputDataTypes = []
        self.outputDataTypes = []
        marySocket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
        marySocket.connect( ( self.host, self.port ) )
        # Expect a variable number of lines of the kind
        # RAWMARYXML INPUT OUTPUT
        # TEXT_DE LOCALE=de INPUT
        # AUDIO OUTPUT
        typeStrings = self.__getServerInfo( "MARY LIST DATATYPES", marySocket )
        if not typeStrings or len(typeStrings) == 0:
            raise IOError( "Could not get list of data types from Mary server" )
        marySocket.close()
        for typeString in typeStrings:
            parts = typeString.split()
            if len( parts ) == 0:
                continue
            name = parts[0]
            isInputType = False
            isOutputType = False
            locale = None
            for part in parts[1:]:
                if part[:6] == "LOCALE=":
                    locale = part[7:]
                elif part == "INPUT":
                    isInputType = True
                elif part == "OUTPUT":
                    isOutputType = True
            dt = DataType( name, locale, isInputType, isOutputType )
            self.allDataTypes.append( dt )
            if dt.isInputType:
                self.inputDataTypes.append( dt )
            if dt.isOutputType:
                self.outputDataTypes.append( dt )

    def getVoices( self, locale=None ):
        """Obtain a list of voices known to the server. If the information is not
        yet available, the server is queried. This is optional information
        which is not required for the normal operation of the client, but
        may help to avoid incompatibilities.
        Returns an array of Voice objects
        """
        if self.allVoices is None:
            self.__fillVoices()
        assert self.allVoices is not None and len( self.allVoices ) > 0
        if locale is None:
            return self.allVoices
        return self.voicesByLocaleMap[locale]

    def __fillVoices( self ):
        self.allVoices = []
        self.voicesByLocaleMap = {}
        marySocket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
        marySocket.connect( ( self.host, self.port ) )
        # Expect a variable number of lines of the kind
        # de7 de female
        # us2 en male
        # dfki-stadium-emo de male limited
        voiceStrings = self.__getServerInfo( "MARY LIST VOICES", marySocket )
        if not voiceStrings or len(voiceStrings) == 0:
            raise IOError( "Could not get list of voices from Mary server" )
        marySocket.close()
        for voiceString in voiceStrings:
            parts = voiceString.split()
            if len( parts ) < 3:
                continue
            name = parts[0]
            locale = parts[1]
            gender = parts[2]
            domain = None
            if len( parts ) > 3:
                domain = parts[3]
            voice = Voice( name, locale, gender, domain )
            self.allVoices.append( voice )
            localeVoices = None
            if self.voicesByLocaleMap.has_key( locale ):
                localeVoices = self.voicesByLocaleMap[locale]
            else:
                localeVoices = []
                self.voicesByLocaleMap[locale] = localeVoices
            localeVoices.append( voice )

    def getGeneralDomainVoices( self, locale=None ):
        """Obtain a list of general domain voices known to the server. If the information is not
        yet available, the server is queried. This is optional information
        which is not required for the normal operation of the client, but
        may help to avoid incompatibilities.
        Returns an array of Voice objects
        """
        return [v for v in self.getVoices( locale ) if not v.isLimitedDomain]

    def getLimitedDomainVoices( self, locale=None ):
        """Obtain a list of limited domain voices known to the server. If the information is not
        yet available, the server is queried. This is optional information
        which is not required for the normal operation of the client, but
        may help to avoid incompatibilities.
        Returns an array of Voice objects
        """
        return [v for v in self.getVoices( locale ) if v.isLimitedDomain]

    def getServerExampleText( self, dataType ):
        """Request an example text for a given data type from the server.
        dataType the string representation of the data type,
        e.g. "RAWMARYXML". This is optional information
        which is not required for the normal operation of the client, but
        may help to avoid incompatibilities."""
        if not self.serverExampleTexts.has_key( dataType ):
            exampleText = self.__getServerInfo( "MARY EXAMPLETEXT %s" % ( dataType ) )[0]
            if not exampleText:
                raise IOError( "Could not get example text from Mary server" )
            self.serverExampleTexts[dataType] = exampleText
        return self.serverExampleTexts[dataType]

    def process( self, input, inputType, outputType, audioType=None, defaultVoiceName=None, output=sys.stdout ):
        assert type( input ) in types.StringTypes
        assert type( inputType ) in types.StringTypes
        assert type( outputType ) in types.StringTypes
        assert audioType is None or type( audioType ) in types.StringTypes
        assert defaultVoiceName is None or type( defaultVoiceName ) in types.StringTypes
        assert callable( getattr( output, 'write' ) )
        if type( input ) != types.UnicodeType:
            input = unicode( input, 'utf-8' )
        maryInfoSocket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
        maryInfoSocket.connect( ( self.host, self.port ) )
        assert type( maryInfoSocket ) is socket.SocketType
        maryInfo = maryInfoSocket.makefile( 'rwb', 1 ) # read-write, line-buffered
        maryInfo.write( unicode( "MARY IN=%s OUT=%s" % ( inputType, outputType ), 'utf-8' ) )
        if audioType:
            maryInfo.write( unicode( " AUDIO=%s" % ( audioType ), 'utf-8' ) )
        if defaultVoiceName:
            maryInfo.write( unicode( " VOICE=%s" % ( defaultVoiceName ), 'utf-8' ) )
        maryInfo.write( "\r\n" )
        # Receive a request ID:
        id = maryInfo.readline()
        maryDataSocket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
        maryDataSocket.connect( ( self.host, self.port ) )
        assert type( maryDataSocket ) is socket.SocketType
        maryDataSocket.sendall( id ) # includes newline
        maryDataSocket.sendall( input.encode( 'utf-8' ) )
        maryDataSocket.shutdown( 1 ) # shutdown writing
        # Set mary info socket to non-blocking, so we only read somthing
        # if there is something to read:
        maryInfoSocket.setblocking( 0 )
        while True:
            try:
                err = maryInfoSocket.recv( 8192 )
                if err: sys.stderr.write( err )
            except:
                pass
            got = maryDataSocket.recv( 8192 )
            if not got: break
            output.write( got )
        maryInfoSocket.setblocking( 1 )
        while True:
            err = maryInfoSocket.recv( 8192 )
            if not err: break
            sys.stderr.write( err )
        


################ data representation classes ##################

class DataType:
    def __init__( self, name, locale=None, isInputType=False, isOutputType=False ):
        self.name = name
        self.locale = locale
        self.isInputType = isInputType
        self.isOutputType = isOutputType
    
    def isTextType( self ):
        return self.name != "AUDIO"

class Voice:
    def __init__( self, name, locale, gender, domain="general" ):
        self.name = name
        self.locale = locale
        self.gender = gender
        self.domain = domain
        if not domain or domain == "general":
            self.isLimitedDomain = False
        else:
            self.isLimitedDomain = True
            
    
##################### Main #########################

if __name__ == '__main__':
    
    serverHost = "cling.dfki.uni-sb.de"
    serverPort = 59125
    inputType = "TEXT"
    outputType = "AUDIO"
    audioType = "WAVE"
    defaultVoice = None
    inputEncoding = 'utf-8'
    ( options, rest ) = getopt.getopt( sys.argv[1:], '', \
        ['server.host=', 'server.port=', 'input.type=', 'output.type=', \
          'audio.type=', 'voice.default=', 'input.encoding='] )
    for ( option, value ) in options:
        if option == '--server.host': serverHost = value
        elif option == '--server.port': serverPort = int( value )
        elif option == '--input.type': inputType = value
        elif option == '--output.type': outputType = value
        elif option == '--audio.type': audioType = value
        elif option == '--voice.default': defaultVoice = value
        elif option == '--input.encoding': inputEncoding = value
    if len( rest )>0: # have input file
        inputFile = file( rest[0] )
    else:
        inputFile = sys.stdin
    input = unicode( ''.join( inputFile.readlines() ), inputEncoding )
    if len( rest )>1: # also have output file
        outputFile = file( rest[1] )
    else:
        outputFile = sys.stdout
    
    maryClient = MaryClient( serverHost, serverPort )
    maryClient.process( input, inputType, outputType, audioType, defaultVoice, outputFile )

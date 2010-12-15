##########################################################################
#                                                                        #
#                copyright (c) 2006 DFKI GmbH, Saarbruecken              #
#             written by: Christian Federmann, cfedermann@dfki.de        #
#             adapted by: Marc Schröder, schroed@dfki.de                 #
#                                                                        #
#             This work has been done for the EC Project HUMAINE         #
#	       (IST-507422) -- see http://emotion-research.net.          #
#                                                                        #
##########################################################################

from Products.CMFCore.DirectoryView import registerDirectory
from Products.CMFCore.utils import ToolInit
from config import PROJECTNAME, SKINS_DIR, GLOBALS
from Products.SpeechSynthesisTool.SpeechSynthesisTool import SpeechSynthesisTool

registerDirectory(SKINS_DIR, GLOBALS)

tools = (SpeechSynthesisTool,)


def initialize(context):
    ToolInit('Speech Synthesis Tool',
              tools=tools, 
              product_name=PROJECTNAME,
              icon='tool.gif',  
    ).initialize(context)

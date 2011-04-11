##########################################################################
#                                                                        #
#                copyright (c) 2006 DFKI GmbH, Saarbruecken              #
#             written by: Christian Federmann, cfedermann@dfki.de        #
#                                                                        #
#             This work has been done for the EC Project HUMAINE         #
#	       (IST-507422) -- see http://emotion-research.net.          #
#                                                                        #
##########################################################################

"""
Plone installer/uninstaller script
"""

from StringIO import StringIO
from Products.CMFCore.utils import getToolByName
from Products.Archetypes.public import listTypes
from Products.Archetypes.Extensions.utils import installTypes, install_subskin

from Products.SpeechSynthesisTool.config import PROJECTNAME, GLOBALS
from Products.SpeechSynthesisTool.permissions import MANAGE_CONTENT_PERMISSION

def add_configlet(self, out):
    cp = getToolByName(self, 'portal_controlpanel', None)

    if not cp:
        out.write("WARNING: could not add %s preferences panel.\n" % PROJECTNAME)
    else:
        cp.addAction(id='%sSetup' % PROJECTNAME,
                     name='%s Setup' % PROJECTNAME,
                     action='string:${portal_url}/speech_synthesis_preferences',
                     permission=MANAGE_CONTENT_PERMISSION,
                     category='Products',
                     appId='%s' % PROJECTNAME,
                     imageUrl='site_icon.gif',
                     description='Configure global settings of the %s.' % PROJECTNAME)
        out.write("Added the %s preferences panel.\n" % PROJECTNAME)

def del_configlet(self, out):
    cp = getToolByName(self, 'portal_controlpanel', None)

    if not cp:
        out.write("WARNING: could not delete %s preferences panel.\n" % PROJECTNAME)
    else:
        cp.unregisterApplication('%s' % PROJECTNAME)
        out.write("Deleted the %s preferences panel.\n"  % PROJECTNAME)

def add_properties(self, out):
    ptool = getToolByName(self, 'portal_properties')

    try:
        ptool.manage_addPropertySheet('speech_synthesis_properties', 'Speech Synthesis Properties')
        out.write("Added the property sheet 'speech_synthesis_properties'.\n")
    except:
        out.write("WARNING: could not add the property sheet 'speech_synthesis_properties'.\n")

    try:
        sproperties = ptool.speech_synthesis_properties
        sproperties.manage_addProperty('server_host', 'localhost', 'string')
        sproperties.manage_addProperty('server_port', '59125', 'int')
        out.write("Added the %s properties.\n" % PROJECTNAME)
    except:
        out.write("WARNING: could not add the %s properties.\n" % PROJECTNAME)

def del_properties(self, out):
    ptool = getToolByName(self, 'portal_properties')

    try:
        ptool.manage_delObjects (['speech_synthesis_properties'])
        out.write("Deleted the property sheet 'speech_synthesis_properties'.\n")
    except:
        out.write("WARNING: could not delete the property sheet 'speech_synthesis_properties'.\n")

def add_tool(self, out):
    # Check that the tool has not been added using its id
    if not hasattr(self, 'speech_synthesis_tool'):
        try:
            addTool = self.manage_addProduct['SpeechSynthesisTool'].manage_addTool
            # Add the tool by its meta_type
            addTool('Speech Synthesis Tool')
            out.write("Added 'speech_synthesis_tool' to portal.\n")
        except:
            out.write("WARNING: could not add 'speech_synthesis_tool' to portal.\n")

def del_tool(self, out):
    if hasattr(self, 'speech_synthesis_tool'):
        try:
            self.manage_delObjects (['speech_synthesis_tool'])
            out.write("Deleted 'speech_synthesis_tool' from portal.\n")
        except:
            out.write("WARNING: could not delete 'speech_synthesis_tool' from portal.\n")
        
def install(self):
    out = StringIO()

    install_subskin(self, out, GLOBALS)
    
    add_properties(self, out)
    add_configlet(self, out)
    add_tool(self, out)

    out.write("Successfully installed %s.\n\n" % PROJECTNAME)
    return out.getvalue()

def uninstall(self):
    out = StringIO()

    del_properties(self, out)
    del_configlet(self, out)
    del_tool(self, out)
    
    out.write("Successfully uninstalled %s.\n\n" % PROJECTNAME)
    return out.getvalue()

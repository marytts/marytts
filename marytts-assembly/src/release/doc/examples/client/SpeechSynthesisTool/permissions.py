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
SpeechSynthesisTool permissions
"""

from Products.CMFCore.CMFCorePermissions import setDefaultRoles

from config import PROJECTNAME

MANAGE_CONTENT_PERMISSION = PROJECTNAME + ': Manage Content'
setDefaultRoles(MANAGE_CONTENT_PERMISSION, ('Manager',))

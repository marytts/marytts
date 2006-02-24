## Controller Script (Python) "speech_synthesis_preferences_update"
##bind container=container
##bind context=context
##bind namespace=
##bind script=script
##bind state=state
##bind subpath=traverse_subpath
##parameters=
##title=Setup Preferences Action
##

server_host = context.REQUEST.get('server_host', '')
server_port = context.REQUEST.get('server_port', '')

try:
    sproperties = context.portal_url.portal_properties.speech_synthesis_properties
    sproperties.manage_changeProperties({'server_host':server_host,
                                         'server_port':server_port})
    message = "Your changes have been saved."
except:
    message = "WARNING: your changes could not be saved."

state.setKwargs({'portal_status_message':message})
return state


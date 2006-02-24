## Controlled Python Script "validate_server_port"
##bind container=container
##bind context=context
##bind namespace=
##bind script=script
##bind state=state
##bind subpath=traverse_subpath
##parameters=email=''
##title=Validates the server port as an integer
##

server_port = context.REQUEST.get('server_port', '')
try:
	int(server_port)
except:
	state.setError('server_port', 'Server port must be an integer', 'invalid_server_port')

if state.getErrors():
    return state.set(status='failure', portal_status_message='Please correct the indicated errors.')
else:
    return state


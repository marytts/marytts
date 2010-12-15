## Controller Script (Python) "speech_synthesis_process"
##bind container=container
##bind context=context
##bind namespace=
##bind script=script
##bind state=state
##bind subpath=traverse_subpath
##parameters=
##title=Setup Preferences Action
##

request = context.REQUEST
lang = request.get('lang', None)
inputType = request.get('inputType', None)
if lang and not inputType:
	request.set('inputType', context.texttype(lang))

outputType = request.get('outputType')
if outputType == "AUDIO":
	state.set(status="audio")
else:
	state.set(status="text")

return state


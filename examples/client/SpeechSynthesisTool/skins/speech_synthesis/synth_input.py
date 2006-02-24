## Script (Python) "texttype"
##bind container=container
##bind context=context
##bind namespace=
##bind script=script
##bind subpath=traverse_subpath
##parameters=input,inputType,prevInputType,lang,prevLang
##title=Setup Preferences Action
##

# Depending on the input configuration, provide different types of input text
# for display in the input text area in speech_synthesis.cpt
if input and not (inputType and prevInputType and inputType != prevInputType or
                   lang and prevLang and lang != prevLang):
    # Only ignore input if we detect a type change:
    return input
elif inputType:
    return context.speech_synthesis_tool.getServerExampleText(inputType)
else:
    assert lang is not None
    return context.speech_synthesis_tool.getServerExampleText(context.texttype(lang))

## Script (Python) "texttype"
##bind container=container
##bind context=context
##bind namespace=
##bind script=script
##bind subpath=traverse_subpath
##parameters=lang
##title=Setup Preferences Action
##

# Convert language into corresponding text input type,
# e.g. 'de' into TEXT_DE or 'en_US' into TEXT_EN
if '_' in lang:
    lang1 = lang[:lang.find('_')]
else:
    lang1 = lang
return 'TEXT_%s' % (lang1.upper())

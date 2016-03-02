

function GetXmlHttpObject()
{
    var xmlHttp=null;
    try
    {
        // Firefox, Opera 8.0+, Safari
        xmlHttp=new XMLHttpRequest();
    }
    catch (e)
    {
        // Internet Explorer
        try
        {
            xmlHttp=new ActiveXObject("Msxml2.XMLHTTP");
        }
        catch (e)
        {
            xmlHttp=new ActiveXObject("Microsoft.XMLHTTP");
        }
    }
    return xmlHttp;
}

function addOption(id, text)
{
	var opt=document.createElement('option');
  	opt.text=text
    var x=document.getElementById(id);
    try {
        x.add(opt,null); // standards compliant
    } catch(ex) {
        x.add(opt); // IE only
    }
}

function initForm()
{
    document.getElementById('VOICE_SELECTIONS').selectedIndex = 0;
    document.getElementById('INPUT_TYPE').selectedIndex = 0;
    document.getElementById('OUTPUT_TYPE').selectedIndex = 0;
    document.getElementById('AUDIO_OUT').selectedIndex = 0;
    document.getElementById('INPUT_TEXT').value = '';
    document.getElementById('OUTPUT_TEXT').value = '';
    document.getElementById('LOCALE').value = 'fill-me';
    document.getElementById('VOICE').value = 'fill-me';
    fillVoices();
	fillTypes();
    fillAudioFormats();
    fillEffects();
	setVisibilities("AUDIO");
	setAudio("WAVE_FILE");
};


function fillVoices()
{
	var xmlHttp = GetXmlHttpObject();
    if (xmlHttp==null) {
        alert ("Your browser does not support AJAX!");
        return;
    }
    url = "voices";
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState==4) {
        	if (xmlHttp.status == 200) {
	            var response = xmlHttp.responseText;
	            var lines = response.split('\n');
	            var localeElt = document.getElementById('LOCALE');
	            var voiceElt = document.getElementById('VOICE');
	            for (l in lines) {
	            	var line = lines[l];
	            	if (line.length > 0) {
		            	addOption("VOICE_SELECTIONS", line);
		            	if (localeElt.value == 'fill-me') {
							var items = line.split(' ', 2);
							voiceElt.value = items[0];
							localeElt.value = items[1];
							updateInputText(true);
							setModificationVisibility(null, "AUDIO"); // AUDIO is default on load
		            	}
	            	}
	            }
        	} else {
        		alert(xmlHttp.responseText);
        	}
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}

function fillTypes()
{
	var xmlHttp = GetXmlHttpObject();
    if (xmlHttp==null) {
        alert ("Your browser does not support AJAX!");
        return;
    }
    url = "datatypes";
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState==4) {
        	if (xmlHttp.status == 200) {
	            var response = xmlHttp.responseText;
	            var lines = response.split('\n');
	            for (l in lines) {
	            	var line = lines[l];
	            	if (line.length > 0) {
		            	var fields = line.split(' ', 1);
		            	if (line.indexOf('INPUT') != -1) {
			            	addOption("INPUT_TYPE", fields[0]);
			            	if (fields[0]=="TEXT") {
			            		var sel = document.getElementById("INPUT_TYPE");
			            		sel.selectedIndex = sel.length - 1;
			            		updateInputText(true);
			            	}
		            	}
		            	if (line.indexOf('OUTPUT') != -1) {
		            		addOption("OUTPUT_TYPE", fields[0]);
			            	if (fields[0]=="AUDIO") {
			            		var sel = document.getElementById("OUTPUT_TYPE");
			            		sel.selectedIndex = sel.length - 1;
			            	}
		            	}
	            	}
	            }
        	} else {
        		alert(xmlHttp.responseText);
        	}
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}

function fillAudioFormats()
{
    var xmlHttp = GetXmlHttpObject();
    if (xmlHttp==null) {
        alert ("Your browser does not support AJAX!");
        return;
    }
    url = "audioformats";
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState==4) {
        	if (xmlHttp.status == 200) {
	            var response = xmlHttp.responseText;
	            var lines = response.split('\n');
	            for (l in lines) {
	            	var line = lines[l];
	            	if (line.length > 0) {
		            	addOption("AUDIO_OUT", line);
	            		if (line.indexOf("WAVE_FILE") != -1) {
	            			document.getElementById("AUDIO_OUT").selectedIndex = document.getElementById("AUDIO_OUT").length - 1;
	            		}
	            	}
	            }
        	} else {
        		alert(xmlHttp.responseText);
        	}
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}



function fillEffects()
{
    var xmlHttp = GetXmlHttpObject();
    if (xmlHttp==null) {
        alert ("Your browser does not support AJAX!");
        return;
    }
    url = "audioeffects";
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState==4) {
        	if (xmlHttp.status == 200) {
	            var response = xmlHttp.responseText;
	            var lines = response.split('\n');
	            for (l in lines) {
	            	var line = lines[l];
	            	if (line.length > 0) {
		            	addEffect(line);
	            	}
	            }
        	} else {
        		alert(xmlHttp.responseText);
        	}
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}

function addEffect(line)
{
	var iSpace = line.indexOf(" ");
	var effect = line.substring(0, iSpace);
	var params = line.substring(iSpace+1);
	var effectsTable = document.getElementById('effectsTable');
	var row = effectsTable.insertRow(effectsTable.rows.length);
	row.setAttribute("id", "effect_"+effect);
	var checkboxCell = row.insertCell(0);
	var checkbox = document.createElement("input");
	checkbox.setAttribute("type", "checkbox");
	checkbox.setAttribute("id", "effect_"+effect+"_selected");
	checkbox.setAttribute("name", "effect_"+effect+"_selected");
	checkbox.checked = false;	
	checkboxCell.appendChild(checkbox);
	var nameCell = row.insertCell(1);
	nameCell.innerHTML = effect;
	var paramCell = row.insertCell(2);
	var textarea = document.createElement("textarea");
	textarea.setAttribute("rows", "1");
	textarea.setAttribute("cols", "20");
	textarea.setAttribute("id", "effect_"+effect+"_parameters");
	textarea.setAttribute("name", "effect_"+effect+"_parameters");
	textarea.value = params;
	paramCell.appendChild(textarea);
	var defaultCell = row.insertCell(3);
	var defaultButton = document.createElement("input");
	defaultButton.setAttribute("type", "button");
	defaultButton.setAttribute("id", "effect_"+effect+"_default");
	defaultButton.setAttribute("name", "effect_"+effect+"_default");
	defaultButton.setAttribute("value", "Default");
	defaultButton.setAttribute("onClick", "defaultEffectParams(this)");
	defaultCell.appendChild(defaultButton);	
	var helpCell = row.insertCell(4);
	var helpButton = document.createElement("input");
	helpButton.setAttribute("type", "button");
	helpButton.setAttribute("id", "effect_"+effect+"_help");
	helpButton.setAttribute("name", "effect_"+effect+"_help");
	helpButton.setAttribute("value", "Help");
	helpButton.setAttribute("onClick", "helpEffect(this)");
	helpCell.appendChild(helpButton);	
}

function defaultEffectParams(button)
{
	var parts = button.getAttribute("id").split("_");
	var effect = parts[1];
    var xmlHttp = GetXmlHttpObject();
    if (xmlHttp==null) {
        alert ("Your browser does not support AJAX!");
        return;
    }
    url = "audioeffect-default-param?effect="+effect;
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState==4) {
        	if (xmlHttp.status == 200) {
                document.getElementById('effect_'+effect+'_parameters').value = xmlHttp.responseText;
        	} else {
        		alert(xmlHttp.responseText);
        	}
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}

function helpEffect(button)
{
	var parts = button.getAttribute("id").split("_");
	var effect = parts[1];
    var xmlHttp = GetXmlHttpObject();
    if (xmlHttp==null) {
        alert ("Your browser does not support AJAX!");
        return;
    }
    url = "audioeffect-help?effect="+effect;
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState==4) {
        	if (xmlHttp.status == 200) {
                document.getElementById('HELP_TEXT').value = xmlHttp.responseText;
        	} else {
        		alert(xmlHttp.responseText);
        	}
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}


function inputTypeChanged()
{
	updateInputText(true); // replace input
}

/**
 * Update the current input text.
 * Retrieves 
 * (1) the example text for the given input type and locale, as well as
 * (2) the example texts for the current voice, if any.
 * If there is a voice-specific example text and the input type is TEXT, use the voice-specific example; else, use the type example.
 * Replace the content of input text only if replaceInput is true.
 */
function updateInputText(replaceInput)
{
    var inputTypeSelect = document.getElementById('INPUT_TYPE');
    var locale = document.getElementById('LOCALE').value;
    if (inputTypeSelect.options.length == 0 || locale == 'fill-me') { // nothing to do yet
	    return;
    }
   	var inputType = inputTypeSelect.options[inputTypeSelect.selectedIndex].text;
   	
	// Keep track of AJAX concurrency across the two requests:
	var retrievingVoiceExample = false;
	var haveVoiceExample = false;
	var retrievingTypeExample = false;
	var typeExample = "";
	
	// Only worth requesting type example if replaceInput is true:
	if (replaceInput) {
	    var xmlHttp = GetXmlHttpObject();
	    if (xmlHttp==null) {
	        alert ("Your browser does not support AJAX!");
	        return;
	    }
	    var url = "exampletext?datatype=" + inputType + "&locale=" + locale;
	    xmlHttp.onreadystatechange = function() {
	        if (xmlHttp.readyState==4) {
	        	if (xmlHttp.status == 200) {
		            typeExample = xmlHttp.responseText;
	        	} else {
	        		alert(xmlHttp.responseText);
	        	}
	        	retrievingTypeExample = false;
	        	if (replaceInput && !retrievingTypeExample && !retrievingVoiceExample) {
	        		if (haveVoiceExample) {
	        			exampleChanged();
	        		} else {
	        			document.getElementById('INPUT_TEXT').value = typeExample;
	        		}
	        	}
	        }
	    };
	    retrievingTypeExample = true;
	    xmlHttp.open("GET", url, true);
	    xmlHttp.send(null);
	}
    
	
	
    // Only worth requesting voice example if input type is TEXT:
    if (inputType == "TEXT") {
	    var xmlHttp2 = GetXmlHttpObject();
	    var voice = document.getElementById('VOICE').value;
	    var url2 = "exampletext?voice=" + voice;
    	xmlHttp2.onreadystatechange = function() {
	        if (xmlHttp2.readyState==4) {
	        	if (xmlHttp2.status == 200) {
	        		var examples = xmlHttp2.responseText;
	        		
	        		
	        		
	        		if (examples != "") {
		            	haveVoiceExample = true;
	   					document.getElementById("exampleTexts").style.display = 'inline';
						document.getElementById("exampleTexts").length = 0;
	   			        var lines = examples.split('\n');
			            for (l in lines) {
		    	        	var line = lines[l];
		        	    	if (line.length > 0) {
			        	    	addOption("exampleTexts", line);
			            	}
		            	}
	        		} else {
		            	haveVoiceExample = false;
	   					document.getElementById("exampleTexts").style.display = 'none';
		            }
	        	} else {
	        		alert(xmlHttp.responseText);
	        	}
	        	retrievingVoiceExample = false;
	        	if (replaceInput && !retrievingTypeExample && !retrievingVoiceExample) {
	        		if (haveVoiceExample) {
	        			exampleChanged();
	        		} else {
	        			document.getElementById('INPUT_TEXT').value = typeExample;
	        		}
	        	}
	        }
	    };
	    retrievingVoiceExample = true;
	    xmlHttp2.open("GET", url2, true);
	    xmlHttp2.send(null);
    	
    } else{ // input type not text, hide examples, don't send request
    	document.getElementById("exampleTexts").style.display = 'none';
    }
    
    
}

function getOutputType() {
	var select = document.getElementById("OUTPUT_TYPE");
    var outputType = select.options[select.selectedIndex].text;
	return outputType;
}

function outputTypeChanged()
{
	var outputType = getOutputType();
	setVisibilities(outputType);
    setModificationVisibility(null, outputType);
}



function setVisibilities(outputType)
{
    if (outputType == "AUDIO") {
    	document.getElementById("outputSection").style.display = 'none';
    	document.getElementById("audioEffectsSection").style.display = 'inline';
    	document.getElementById("showHideEffectsButton").style.display = 'inline';
    	//document.getElementById("helpSection").style.display = 'inline';
    	document.getElementById("PROCESS").style.display = 'none';
    	document.getElementById("SPEAK").style.display = 'inline';
    	document.getElementById("audioDestination").style.display = 'inline';
    	document.getElementById("showHideTargetFeatures").style.display = 'none';
    } else {
    	document.getElementById("outputSection").style.display = 'inline';
    	document.getElementById("audioEffectsSection").style.display = 'none';
    	document.getElementById("showHideEffectsButton").style.display = 'none';
    	//document.getElementById("helpSection").style.display = 'none';
    	document.getElementById("PROCESS").style.display = 'inline';
    	document.getElementById("SPEAK").style.display = 'none';
    	document.getElementById("audioDestination").style.display = 'none';
    	if (outputType == "TARGETFEATURES" || outputType == "HALFPHONE_TARGETFEATURES") {
    		document.getElementById("showHideTargetFeatures").style.display = 'inline';
    	} else {
    		document.getElementById("showHideTargetFeatures").style.display = 'none';
    	}
    	if(inputType == "PHONEMES"){
    		
    	}
    }
};

function toggleEffectsVisibility()
{
	var currentVisibility = document.getElementById("innerAudioEffectsSection").style.display;
	if (currentVisibility == 'none') {
		document.getElementById("innerAudioEffectsSection").style.display = 'inline';
		document.getElementById("TOGGLE_EFFECTS").value = 'Hide Audio Effects';
	} else {
		document.getElementById("innerAudioEffectsSection").style.display = 'none';
		document.getElementById("TOGGLE_EFFECTS").value = 'Show Audio Effects';
	}
}

/**
 * Set visibility of the modification checkbox so that it is shown if the selected voice is of type 
 * "unitselection" and the selected output type is one that requires AUDIO, and hidden otherwise.
 * 
 * @param {} voiceType "unitselection", "hmm", etc. will be filled if null
 * @param {} outputType "AUDIO", etc. will be filled if null
 * @return 
 * @type 
 */
function setModificationVisibility(voiceType, outputType) {
	// check for unitselection voice:
	if (voiceType == null) {
		voiceType = getVoiceItems()[3];
	}
	if (voiceType != "unitselection") {
		document.getElementById("showHideModification").style.display = 'none';
		return;
	}
	
	// otherwise check for output type requiring AUDIO:
	if (outputType == null) {
		outputType = getOutputType();
	}
	// TODO: as long as there is no InfoRequestHandler that returns the output types which require AUDIO, just do:
	var outputTypesWithAudio = new Array("AUDIO", "REALISED_ACOUSTPARAMS", "REALISED_DURATIONS", "PRAAT_TEXTGRID");
	var isOutputTypeWithAudio = false;
	for (var i = 0; i < outputTypesWithAudio.length; i++) {
		if (outputTypesWithAudio[i] == outputType) {
			isOutputTypeWithAudio = true;
			break;
		}
	}
	if (isOutputTypeWithAudio) {
		document.getElementById("showHideModification").style.display = 'inline';
	} else {
		document.getElementById("showHideModification").style.display = 'none';
	}
}

function getVoiceItems() {
	var select = document.getElementById('VOICE_SELECTIONS');
	var voice = select.options[select.selectedIndex].text;
	var items = voice.split(' ');
	return items;
}

function voiceChanged()
{
	var items = getVoiceItems();
	document.getElementById('VOICE').value = items[0];
	var newLocale = items[1];
	var prevLocale = document.getElementById('LOCALE').value;
	if (prevLocale != newLocale) {
		document.getElementById('LOCALE').value = newLocale;
		updateInputText(true); // replace input
	} else {
		updateInputText(false); // do not replace input
	}
	var voiceType = items[3];
	setModificationVisibility(voiceType, null);
};

function exampleChanged()
{
	var select = document.getElementById('exampleTexts');
	var example = select.options[select.selectedIndex].text;
	document.getElementById('INPUT_TEXT').value = example;
}


function audioOutChanged()
{
	var select = document.getElementById('AUDIO_OUT');
    setAudio(select.options[select.selectedIndex].text);
    requestSynthesis();
}

function setAudio(value)
{
    document.getElementById('AUDIO').value = value;
}


function doSubmit()
{
    document.getElementById('maryWebClient').submit();
}

function requestSynthesis()
{
    var url = "process";
    var param = "";
	var maryForm=document.getElementById("maryWebClient");
	for (var i=0;i<maryForm.length;i++) {
		var element = maryForm.elements[i];
		var key = element.name;
		var value;
		if (element.nodeName == "SELECT") {
			if (element.options.length > 0) {
				value = element.options[element.selectedIndex].text;
			} else {
				value = "";
			}
		}
		else if (element.getAttribute("type") == "checkbox") {
			// some special checkboxes that have nothing to do with effects:
			if (element.id == "modification") {
				// if modification is visible and selected, set OUTPUT_TYPE_PARAMS:
				if (document.getElementById("showHideModification").style.display != 'none' && element.checked) {
					key = "OUTPUT_TYPE_PARAMS";
					value = "MODIFICATION";
				}
			} else if (element.id == "specifyTargetFeatures") {
				// if target features is visible and selected and not empty, set OUTPUT_TYPE_PARAMS:
				if (document.getElementById("showHideTargetFeatures").style.display != 'none' && element.checked) {
					var targetFeatures = document.getElementById("targetFeatureList").value;
					if (targetFeatures.length > 0) {
						key = "OUTPUT_TYPE_PARAMS";
						value = targetFeatures;
					}
				}
			} else {
		    	value = element.checked ? "on" : "";
		    }
		} else {
		    value = element.value;
		}
		
		if (key.length == 0) {
			continue; // don't add keyless params!
		}
		
    	if (param.length > 0) param = param + "&";
        param = param + key + "=" + encodeURIComponent(value);
    }
	
	var outputType = getOutputType();
	if (outputType == "AUDIO") {
        //doSubmit();
        url = url + "?" + param;
        var audioDestination = document.getElementById("audioDestination");
        while (audioDestination.childNodes.length > 0) {
        	audioDestination.removeChild(audioDestination.firstChild);
        }
        
        // Check whether <audio> tag is supported:
        var audioType = document.getElementById('AUDIO').value;
        var mimeType = 'audio/'+audioType.substring(0, audioType.indexOf('_')).toLowerCase();
        if (mimeType == 'audio/wave') {
        	mimeType = "audio/wav"; // for some reason Chrome likes this better
        }
        var audioTag = document.createElement('audio');
		if(! audioTag.canPlayType || audioTag.canPlayType(mimeType)=="no" || audioTag.canPlayType(mimeType)=="") {
			alert("cannot use audio tag for "+mimeType);
	        audioDestination.innerHTML = '<object classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" '
	          + ' codebase="http://www.apple.com/qtactivex/qtplugin.cab" width="200" height="16">'
	          + '<param name="src" value="' + url + '" />'
			  + '<param name="controller" value="true" />'
			  + '<param name="qtsrcdontusebrowser" value="true" />'
			  + '<param name="autoplay" value="true" />'
			  + '<param name="autostart" value="1" />'
			  + '<param name="pluginspage" value="http://www.apple.com/quicktime/download/" />\n'
			  + '<!--[if !IE]> <-->\n'
			  + '<object data="'+url+'" width="200" height="16">'
			  + '<param name="src" value="' + url + '" />'
			  + '<param name="controller" value="true" />'
			  + '<param name="autoplay" value="true" />'
			  + '<param name="autostart" value="1" />'
			  + '<param name="pluginurl" value="http://www.apple.com/quicktime/download/" />'
		      + '</object>\n'
			  + '<!--> <![endif]-->\n'
			  + '</object>';
		} else {
			//alert("audio tag should be ok for "+mimeType);
	        audioDestination.innerHTML = '<audio src="' + url + '" autoplay controls>'
			  + '</audio>';
		}

	    // alert(audioDestination.innerHTML);
        var fallback = document.createElement("a");
        fallback.setAttribute("href", url);
		var fallbackText = document.createTextNode("Save audio file");
		fallback.appendChild(fallbackText);
        var div = document.createElement("div");
        div.appendChild(fallback);
        audioDestination.appendChild(div);
	} else {
		// for non-audio types, fill OUTPUT_TEXT via AJAX
        var xmlHttp = GetXmlHttpObject();
	    if (xmlHttp==null) {
	        alert ("Your browser does not support AJAX!");
	        return;
	    }
	    xmlHttp.onreadystatechange = function() {
	        if (xmlHttp.readyState==4) {
	        	if (xmlHttp.status == 200) {
		            document.getElementById('OUTPUT_TEXT').value = xmlHttp.responseText;
	        	} else {
	        		alert(xmlHttp.responseText);
	        	}
	        }
	    };
	    xmlHttp.open("POST", url, true);
	    xmlHttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	    xmlHttp.send(param);
	}
}





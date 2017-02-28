

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
  	opt.text=text;
    var x=document.getElementById(id);
    try {
        x.add(opt,null); // standards compliant
    } catch(ex) {
        x.add(opt); // IE only
    }
}

function initForm()
{
    document.getElementById('INPUT_TEXT').value = '';
    document.getElementById('OUTPUT_TEXT').value = '';
    document.getElementById("CONFIGURATION").value = "";
};


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

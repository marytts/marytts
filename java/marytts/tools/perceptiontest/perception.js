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

function initForm()
{
    setVisibilities("GETEMAIL");
};


function fillOptions()
{
	var xmlHttp = GetXmlHttpObject();
    if (xmlHttp==null) {
        alert ("Your browser does not support AJAX!");
        return;
    }
    url = "options";
    
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState==4) {
        	if (xmlHttp.status == 200) {
	            var response = xmlHttp.responseText;
	            var lines = response.split('\n');
	            
	            
	            addOptionsTable(lines[4]);
	            /*
        	    var optionElt = document.getElementById('OPTION_SELECTIONS');
	            var items = lines[0].split(' ');
	            optionElt.value = items[0];
	            for (l in items) {
	            	var word = items[l];
	            	if ( word != "") {
	            		addOption("OPTION_SELECTIONS", word);
	            	}
	            }
	            */
	            
	            
	        } else {
        		alert(xmlHttp.responseText);
        	}
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}

function fillQuestion()
{
	var xmlHttp = GetXmlHttpObject();
    if (xmlHttp==null) {
        alert ("Your browser does not support AJAX!");
        return;
    }
    url = "queryStatement";
    
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState==4) {
        	if (xmlHttp.status == 200) {
	            document.getElementById('QUESTION_TEXT').value = xmlHttp.responseText; 
	        } else {
        		alert(xmlHttp.responseText);
        	}
        }
    };
    xmlHttp.open("GET", url, true);
    xmlHttp.send(null);
}	            
       

function addFirstOption(id, text)
{
	var opt=document.createElement('option');
  	opt.text=text;
    var x=document.getElementById(id);
    
    // remove all previous options
    var i;
    for (i = x.length - 1; i>=0; i--) {
          x.remove(i);
    }
    
    // now add given option
    try {
        x.add(opt,null); // standards compliant
    } catch(ex) {
        x.add(opt); // IE only
    }
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

function isValidEmail(str) {
   return (str.indexOf(".") > 2) && (str.indexOf("@") > 0);
}


function getEmailID() {
	
	var xmlHttp = GetXmlHttpObject();
	    if (xmlHttp==null) {
	        alert ("Your browser does not support AJAX!");
	        return;
	    }
	
	var url = "perceptionTest";
	//var param = "EMAIL"+"=";
	var myEmailID = document.getElementById('getMyEmailID');
	var param = "EMAIL" + "=" + myEmailID.value;
	url = url + "?" + param;
	//alert("You entered: " + url);
	if(!isValidEmail(myEmailID.value)) {
		alert("Would you please enter a valid E-Mail ID?");	
	} else {
		xmlHttp.onreadystatechange = function() {
	        if (xmlHttp.readyState==4) {
	        	if (xmlHttp.status == 200) {

		            var httpResponse = xmlHttp.responseText;
		            //alert(httpResponse);
		            var lines = httpResponse.split('\n');
		            
		            if(lines[1] <=  lines[2]) {
		            	setVisibilities("COMPLETED");
		            	return;
		            }
		            
		            document.getElementById('EMAIL_ID').value = lines[0];
		            document.getElementById('NUMBER_OF_SAMPLES').value = lines[1];
		            document.getElementById('PRESENT_SAMPLE_NUMBER').value = lines[2];
		            document.getElementById('PRESENT_SAMPLE_BASENAME').value = lines[3];
		            addOptionsTable(lines[4]);
		            /*
		            var optionElt = document.getElementById('OPTION_SELECTIONS');
	                var items = lines[4].split(' ');
	                //optionElt.value = items[0];
	                //addFirstOption("OPTION_SELECTIONS", items[0]);
	                for (l in items) {
	            		var word = items[l];
	            		if ( word ==  items[0] ) {
	            			addFirstOption("OPTION_SELECTIONS", word);
	            		} 
	            		else if ( word != "") {
	            			addOption("OPTION_SELECTIONS", word);
	            		}
	            	}
		            */
		            //document.getElementById('QUESTION_TEXT').value = lines[5];
		            document.getElementById('QUESTION_TEXT').value = getQuestionText(httpResponse);
		            playWave();
		        
		        } else {
	        		alert(xmlHttp.responseText);
	        	}
	        }
	    };
		
		xmlHttp.open("POST", url, true);
        xmlHttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	    xmlHttp.send(param);
	    setVisibilities("PERCEPTION-TEST");
	    
	}
}


function postSampleDetails() {
	
	var xmlHttp = GetXmlHttpObject();
	    if (xmlHttp==null) {
	        alert ("Your browser does not support AJAX!");
	        return;
	    }
	
	var url = "perceptionTest";
	var myEmailID = document.getElementById('getMyEmailID');
	var param = "EMAIL" + "=" + myEmailID.value;
	var presentSampleID = document.getElementById('PRESENT_SAMPLE_NUMBER');
	param = param + "&PRESENT_SAMPLE_NUMBER" + "=" + presentSampleID.value;
	url = url + "?" + param;
	
	//alert("You entered: " + url);
	if(!isValidEmail(myEmailID.value)) {
		alert("Would you please enter a valid E-Mail ID?");	
	} else {
		xmlHttp.onreadystatechange = function() {
	        if (xmlHttp.readyState==4) {
	        	if (xmlHttp.status == 200) {
		            
		            var httpResponse = xmlHttp.responseText;
		            var lines = httpResponse.split('\n');
		            
		            if(lines[1] <=  lines[2]) {
		            	setVisibilities("COMPLETED");
		            	return;
		            }
		            
		            document.getElementById('EMAIL_ID').value = lines[0];
		            document.getElementById('NUMBER_OF_SAMPLES').value = lines[1];
		            document.getElementById('PRESENT_SAMPLE_NUMBER').value = lines[2];
		            document.getElementById('PRESENT_SAMPLE_BASENAME').value = lines[3];
		            
		            addOptionsTable(lines[4]);
		            /*
		            var optionElt = document.getElementById('OPTION_SELECTIONS');
	                var items = lines[4].split(' ');
	                //optionElt.value = items[0];
	                //addFirstOption("OPTION_SELECTIONS", items[0]);
	                for (l in items) {
	            		var word = items[l];
	            		if ( word ==  items[0] ) {
	            			addFirstOption("OPTION_SELECTIONS", word);
	            		} 
	            		else if ( word != "") {
	            			addOption("OPTION_SELECTIONS", word);
	            		}
	            	}
		            */
		            //document.getElementById('QUESTION_TEXT').value = lines[5];
		            document.getElementById('QUESTION_TEXT').value = getQuestionText(httpResponse);
		        
		        } else {
	        		alert(xmlHttp.responseText);
	        	}
	        }
	    };
		
		xmlHttp.open("POST", url, true);
        xmlHttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	    xmlHttp.send(param);
	    setVisibilities("PERCEPTION-TEST");
	}
		
}

function getQuestionText(httpResponse){
	var lines = httpResponse.split('\n');
	var i;
	var questionText = "";
	for(i=5; i<lines.length; i++){
		questionText += lines[i]+"\n"; 
	}
	return questionText;
}

function searchKeyPress(e) {
	 // look for window.event in case event isn't passed in
     //if (window.event) { e = window.event; }
     if (e.keyCode == 13)
     {
        getEmailID();
     }
}

function showNextSample(){
	
	if(!checkSelections()){
		alert("Please provide your rating for this sample!");
		return;
	}
	sendResult();
	//alert(" 1 ");
	// increment the present value; 
	var presentSample = document.getElementById('PRESENT_SAMPLE_NUMBER').value;
	var numberSamples = document.getElementById('NUMBER_OF_SAMPLES').value;
	document.getElementById('PRESENT_SAMPLE_NUMBER').value = ++presentSample;
	
	
	if ( presentSample < (numberSamples-1)) {
		postSampleDetails();
	    playWave();
	}
	else if(presentSample == (numberSamples-1)){
		document.getElementById('NEXT').value = "FINISH";
		postSampleDetails();
	    playWave();
	}
	else if(presentSample == numberSamples) {
		document.getElementById('NEXT').value = "COMPLETED";
		setVisibilities("COMPLETED");
	}
	
}

function checkSelections() {
	var optionsTable = document.getElementById('optionsTable');
	var i = 0;
	//alert(optionsTable.id);
	var result = "";
	var newElements = optionsTable.getElementsByTagName("input");
	var isSelected = false;
	//.elements.length;
	for (i=0; i<newElements.length; i++) {
		if(newElements[i].className.indexOf("OPTIONSLIST") !=-1){
			isSelected = isSelected || newElements[i].checked;
			//if( newElements[i].checked ) {
				//result = result+":"+newElements[i].name;
			//}
			//alert(newElements[i].name+" "+newElements[i].checked);
		}
	}
	
	return isSelected;
}

/*
function showPreviousSample(){
	
	sendResult();
	//alert(" 1 ");
	// increment the present value; 
	var presentSample = document.getElementById('PRESENT_SAMPLE_NUMBER').value;
	var numberSamples = document.getElementById('NUMBER_OF_SAMPLES').value;
	
	
	if ( presentSample < (numberSamples-1) && presentSample > 0) {
		document.getElementById('PRESENT_SAMPLE_NUMBER').value = --presentSample;
		postSampleDetails();
	    playWave();
	}
	else if(presentSample == (numberSamples-1)){
		document.getElementById('PRESENT_SAMPLE_NUMBER').value = --presentSample;
		document.getElementById('NEXT').value = "NEXT";
		postSampleDetails();
	    playWave();
	}
}*/

function sendResult() {
	
	var xmlHttp = GetXmlHttpObject();
	    if (xmlHttp==null) {
	        alert ("Your browser does not support AJAX!");
	        return;
	    }
	
	var optionsTable = document.getElementById('optionsTable');
	var i = 0;
	//alert(optionsTable.id);
	var result = "";
	var newElements = optionsTable.getElementsByTagName("input");
	
	//.elements.length;
	for (i=0; i<newElements.length; i++) {
		if(newElements[i].className.indexOf("OPTIONSLIST") !=-1){
			if( newElements[i].checked ) {
				result = result+":"+newElements[i].name;
			}
			//alert(newElements[i].name+" "+newElements[i].checked);
		}
	}
	result = result.substring(1);
	//alert(result);
	
	var url = "userRating";
	var myEmailID = document.getElementById('getMyEmailID');
	var param = "EMAIL" + "=" + myEmailID.value;
	var presentSampleID = document.getElementById('PRESENT_SAMPLE_NUMBER');
	param = param + "&PRESENT_SAMPLE_NUMBER" + "=" + presentSampleID.value;
	var presentSampleBaseName = document.getElementById('PRESENT_SAMPLE_BASENAME');
	param = param + "&PRESENT_SAMPLE_BASENAME" + "=" + presentSampleBaseName.value;
	param = param + "&RESULTS" + "=" + result;
	
	url = url + "?" + param;
	
	
	if(!isValidEmail(myEmailID.value)) {
		alert("Would you please enter a valid E-Mail ID?");	
	} else {
		xmlHttp.onreadystatechange = function() {
	        if (xmlHttp.readyState==4) {
	        	if (xmlHttp.status == 200) {
		            var httpResponse = xmlHttp.responseText;
		        } else {
	        		alert(xmlHttp.responseText);
	        	}
	        }
	};
		
	xmlHttp.open("POST", url, true);
    xmlHttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xmlHttp.send(param);
	    //setVisibilities("PERCEPTION-TEST");
	}
}

 
function playWave()
{
	
    var url = "process";
    var param = "PRESENT_SAMPLE_NUMBER=" + document.getElementById("PRESENT_SAMPLE_NUMBER").value;
    url = url + "?" + param;  
    showSampleNumbers();
	//alert(url);
		var audioDestination = document.getElementById("audioDestination");
        while (audioDestination.childNodes.length > 0) {
        	audioDestination.removeChild(audioDestination.firstChild);
        }
       
        // Check whether <audio> tag is supported:
        //var audioType = document.getElementById('AUDIO').value;
        //var mimeType = 'audio/'+audioType.substring(0, audioType.indexOf('_')).toLowerCase();
        var audioTag = document.createElement('audio');
        
		if(! audioTag.canPlayType || audioTag.canPlayType('wave')=="no") {
			//alert("cannot use audio tag for ");
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
			//alert("audio tag should be ok for ");
	        //audioDestination.innerHTML = '<audio src="' + url + '" autoplay controls>'
			  + '</audio>';
			audioDestination.innerHTML = '<audio src="' + url + '" controls>'
			  + '</audio>';
		}
       //alert(audioDestination.innerHTML);
	  
}

function showSampleNumbers() {
	var presentSample = document.getElementById("PRESENT_SAMPLE_NUMBER").value;
	var nSamples = document.getElementById("NUMBER_OF_SAMPLES").value;
	var showSample = document.getElementById("sampleNumber");
	
	showSample.innerHTML = (++presentSample) + "/" + nSamples;
	
}

function setVisibilities(outputType)
{
	if( outputType == "GETEMAIL" ) {
		document.getElementById("getEmailID").style.display = 'inline';
    	document.getElementById("questionSpan").style.display = 'none';
    	document.getElementById("answerSpan").style.display = 'none';
    	document.getElementById("completedSpan").style.display = 'none';
	}
	else if ( outputType == "COMPLETED" ) {
		document.getElementById("getEmailID").style.display = 'none';
    	document.getElementById("questionSpan").style.display = 'none';
    	document.getElementById("answerSpan").style.display = 'none';
		document.getElementById("completedSpan").style.display = 'inline';
	}
	else {
    	document.getElementById("getEmailID").style.display = 'none';
    	document.getElementById("questionSpan").style.display = 'inline';
    	document.getElementById("answerSpan").style.display = 'inline';
    	document.getElementById("completedSpan").style.display = 'none';
    }
    
};
  
  
function addOptionsTable(line)
{
	var iSpace = line.indexOf(" ");
	var effect = line.substring(0, iSpace);
	var params = line.substring(iSpace+1);
	
	//var params = line;
	var optionsTable = document.getElementById('optionsTable');
	
	//var optionsTable = document.getElementById('showCheckBoxes');
	while (optionsTable.childNodes.length > 0) {
        	optionsTable.removeChild(optionsTable.firstChild);
    }
	
	
	var items = params.split(' ');
	for (l in items) {
		var word = items[l];
		
		
		if(word == ""){
			continue;
		}
		
		var row = optionsTable.insertRow(optionsTable.rows.length);
		
		row.setAttribute("id", "option_"+word);
		var checkboxCell = row.insertCell(0);
		var checkbox = document.createElement("input");
		checkbox.setAttribute("type", "checkbox"); // TODO: Now forced to checkbox (actually, it should be 'effect' variable )
		checkbox.setAttribute("id", word);
		checkbox.setAttribute("name", word); 
		checkbox.setAttribute("class", "OPTIONSLIST");
		
		checkbox.checked = false;	
		checkboxCell.appendChild(checkbox);
		var nameCell = row.insertCell(1);
		nameCell.innerHTML = word;
		
	}

}  
          
   


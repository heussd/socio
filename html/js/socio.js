//var $currentUrl = "http://fbi.h-da.de/";

function addNewTag($tag) {
  if ($tag == "") return;
  $.ajax({ url: "http://localhost:8080/socio/rest/addTag?uri=" + $currentUrl + "&tag='" + $tag + "'"})
}


function displayUrl() {
    $('#currentUrl').text($currentUrl);
   	$("#currentUrl").addClass("unknown");

	// Ask for general knowledge
  	$.ajax({
       url:"http://localhost:8080/socio/rest/knows?uri=" + $currentUrl,
       success: function(data, status) { 
       if (data == "true" ) {
         $("#currentUrl").removeClass("unknown");
       	$("#currentUrl").addClass("known");
       }
       else {
        $("#currentUrl").removeClass("known");
       	$("#currentUrl").addClass("unknown");
       }
       }
     })
}

function buildTagList() {
  $.getJSON('http://localhost:8080/socio/rest/queryUri?uri=' + $currentUrl, function(data) {
	  var items = [];
	  
	  $.each(data, function(key, val) {
	  		  	
	    items.push('<li id="' + key + '"><button class="cupid-green"><a href="#" class="tag">' + val + '</a></button></li>');
	  });
	  
	$('<ul/>', {
		'class': 'allmytags',
		html: items.join('')}
	).replaceAll($('.allmytags'));
	
  });
  
  $.getJSON('http://localhost:8080/socio/rest/queryUri?own=false&uri=' + $currentUrl, function(data) {
	  var items = [];
	  
	  $.each(data, function(key, val) {
	  		  	
	    items.push('<li id="' + key + '"><button class="clean-gray"><a href="javascript:addNewTag("' + val + '")">' + val + '</a></button></li>');
	  });
	  
	$('<ul/>', {
		'class': 'allforeigntags',
		html: items.join('')}
	).replaceAll($('.allforeigntags'));
	
  });

}


function openUrl(url) {
	try {
		chrome.tabs.create({'url': url}, function(tab) {
		  // Tab opened.
		});
	} catch(err) {
		// Fallback option, hardcode your URL here
		window.open(url);
	}	
}

function buildRelatedList() {
	$.getJSON('http://localhost:8080/socio/rest/queryRelated?uri=' + $currentUrl, function(data) {
		 var items = [];
	  
	  $.each(data, function(key, val) {
	    items.push('<li id="' + key + '"><a href="' + key + '" class="relatedurl" onclick="openUrl(\'' + key + '\')">' + key + '</a> <div class="progress"><div style="width:' + ( val  * 100 ) + '%; ">&nbsp;</div></div></li>');
	  });
	  
	$('<ol/>', {
		'class': 'related',
		html: items.join('')}
	).replaceAll($('.related'));
	
  });

}

function addXmppUser($user) {
    if ($user == "") return;
    $.ajax({ url: "http://localhost:8080/socio/rest/addUser?xmpp='" + $user + "'"});
}



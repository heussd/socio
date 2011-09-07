//var $currentUrl = "http://fbi.h-da.de/";

function addNewTag($tag) {
  if ($tag == "") return;
  if ($tag == " ") return;
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
	  		  	
	    items.push('<li id="' + key + '"><button class="clean-gray" onclick="addNewTag(\'' + val + '\');setTimeout(500, buildTagList());">' + val + '</button></li>');
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
	    items.push('<li id="' + key + '"><span class="hiddenvalue">' + val + '</span><a href="' + key + '" class="relatedurl" onclick="openUrl(\'' + key + '\'); setTimeout(1000, buildRelatedList());">' + key + '</a> <div class="progress"><div style="width:' + ( val  * 100 ) + '%; ">&nbsp;</div></div></li>');
	  });
	  
	  
	  
	  
	  items.sort(function(a, b) {
	  	
	  	//alert($(a).children().html);
	  	//alert( $(a).text() );
	  	return $(a).text() < $(b).text();
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






jQuery.fn.sortElements = (function(){
 
    var sort = [].sort;
 
    return function(comparator, getSortable) {
 
        getSortable = getSortable || function(){return this;};
 
        var placements = this.map(function(){
 
            var sortElement = getSortable.call(this),
                parentNode = sortElement.parentNode,
 
                // Since the element itself will change position, we have
                // to have some way of storing its original position in
                // the DOM. The easiest way is to have a 'flag' node:
                nextSibling = parentNode.insertBefore(
                    document.createTextNode(''),
                    sortElement.nextSibling
                );
 
            return function() {
 
                if (parentNode === this) {
                    throw new Error(
                        "You can't sort elements if any one is a descendant of another."
                    );
                }
 
                // Insert before flag:
                parentNode.insertBefore(this, nextSibling);
                // Remove flag:
                parentNode.removeChild(nextSibling);
 
            };
 
        });
 
        return sort.call(this, comparator).each(function(i){
            placements[i].call(getSortable.call(this));
        });
 
    };
 
})();


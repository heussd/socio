//var $currentUrl = "http://fbi.h-da.de/";



function indexLoad(){
      
     $(function() {
			$( '#newtagform' ).submit(function() {
				addNewTag($('#newTag').val());
				$('#newTag').val("");
				buildTagList();
				setTimeout(buildTagList,1000);
				return false;
			});

			$( "#newTag" ).autocomplete({
				source: "http://localhost:8080/socio/rest/queryTag",
				minLength: 2,
				select: function( event, ui ) {
				addNewTag(this.value);
			}
			});
  		});
  		      addListeners();
  					self.port.emit("getTabToIndex","");
  					
}


function relatedLoad(){
    addListeners();
    self.port.emit("getTabToRelated","");
}


function peerLoad(){
   addListeners();
   $( '#newpeerform' ).submit(function() {
                addXmppUser($('#newPeer').val());
                $('#newPeer').val("");
                return false;
            });
}


self.port.on("sendTabToIndex",function(url){
          console.log("sendTabToIndex: " +url);
          $currentUrl = escape(url);
          updateAddressbarIcon($currentUrl);
          indexStart($currentUrl);
});

self.port.on("sendTabToRelated",function(url){
          $currentUrl = escape(url);
          updateAddressbarIcon($currentUrl);
          relatedStart($currentUrl);
});

  function indexStart(url) {

      buildTagList();
      setTimeout(buildTagList,1000);
      
      $('#newTag').focus();
  }



function relatedStart(url) {
  			
  			buildRelatedList();
  		}

function addListeners(){



document.getElementById("related").addEventListener('click',function (e) {

  $.ajax({
  url: "../related.html",
  context: document.body,
  mimeType:  "text/plain",
  isLocal: true,
  success: function(data, textStatus, jqXHR){
    this.removeEventListener('click',arguments.callee,false);
    document.body.innerHTML=data;
    relatedLoad();
  },
  error: function(jqXHR, textStatus, errorThrown){
  document.body.innerHTML=textStatus;
  }
  
});

},false);

document.getElementById("index").addEventListener('click',function (e) {

  loadIndexPage();
  },false);

document.getElementById("peer").addEventListener('click',function (e) {

  $.ajax({
  url: "../peer.html",
  context: document.body,
  mimeType:  "text/plain",
  isLocal: true,
  success: function(data, textStatus, jqXHR){
    this.removeEventListener('click',arguments.callee,false)
    document.body.innerHTML=data;
    peerLoad();
  },
  error: function(jqXHR, textStatus, errorThrown){
  document.body.innerHTML=textStatus;
  }
  
});

},false);


}


function loadIndexPage(){
    $.ajax({
  url: "../index.html",
  context: document.body,
  mimeType:  "text/plain",
  isLocal: true,
  success: function(data, textStatus, jqXHR){
    this.removeEventListener('click',arguments.callee,false)
    document.body.innerHTML=data;
    indexLoad();
  },
  error: function(jqXHR, textStatus, errorThrown){
  document.body.innerHTML=textStatus;
  }
  
});

}

self.port.on("startOver",function(text){
          loadIndexPage();
});

function addNewTag($tag) {
  if ($tag == "") return;
  if ($tag == " ") return;
  $.ajax({ url: "http://localhost:8080/socio/rest/addTag?uri=" + $currentUrl + "&tag='" + $tag + "'", success: function(data, textStatus, jqXHR){
    self.port.emit("addBookmark",$tag);
    updateAddressbarIcon($currentUrl);
  }});
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
  console.log("buildTagList(): " + $currentUrl);
  $.getJSON('http://localhost:8080/socio/rest/queryUri?uri=' + $currentUrl, function(data) {
	  var items = [];
	  
	  $.each(data, function(key, val) {
	  		  	
	    items.push('<li id="' + key + '"><button class="cupid-green"><a class="tag">' + val + '</a></button></li>');
	  });
	  
	$('<ul/>', {
		'class': 'allmytags',
		html: items.join('')}
	).replaceAll($('.allmytags'));
	
	
		$.each(data, function(key, val) {
		  document.getElementById(key).addEventListener('click',function (e) {
        openUrl("http://localhost:8080/socio/rest/activity?tag=" + val);
      },false);
    }); 
	
  });
  
  $.getJSON('http://localhost:8080/socio/rest/queryUri?own=false&uri=' + $currentUrl, function(data) {
	  var items = [];
	  
	  $.each(data, function(key, val) {
	  		  	
	    items.push('<li id="' + key + '"><button class="clean-gray">' + val + '</button></li>');
	  });
	  
	$('<ul/>', {
		'class': 'allforeigntags',
		html: items.join('')}
	).replaceAll($('.allforeigntags'));
	
	$.each(data, function(key, val) {
		  document.getElementById(key).addEventListener('click',function (e) {
        addNewTag(val);
        setTimeout(500, buildTagList());
      },false);
    });
	
	
  });
  
}


function openUrl(url) {
	self.port.emit("openTab",url);
}

function buildRelatedList() {
	$.getJSON('http://localhost:8080/socio/rest/queryRelated?uri=' + $currentUrl, function(data) {
	  
	  var items = [];
	  
	  $.each(data, function(key, val) {
	    items.push('<li id="' + key + '"><span class="hiddenvalue">' + val + '</span><a title="' + key + '" class="relatedurl">' + shortenUrl(key) + '</a> <div class="progress"><div style="width:' + ( val  * 100 ) + '%; ">&nbsp;</div></div></li>');
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
	
		$.each(data, function(key, val) {
		  document.getElementById(key).addEventListener('click',function (e) {
        openUrl(key);
      },false);
    });
	
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


function updateAddressbarIcon(url) {
console.log("updateAdressbarIcon(): " + url);
    /*
    Fire a "What do you know about ...?"-request. The response is
    a classification of SocIOs knowledge about the given resource:
     "none"     -   resource is not known
     "own"      -   the user has already assigned tags to the resource
     "foreign"  -   other users have already assigned tags to the resource
     "both"     -   "own" + "foreign"
    */
    $.ajax({
      url:"http://localhost:8080/socio/rest/knows?uri=" + url,
      success: function(data, status) {
          self.port.emit("changeIcon",data);
      },
      error: function(jqXHR, textStatus, errorThrown){
          self.port.emit("changeIcon","none");
  }
   });
}

function shortenUrl($string) {
  $string = $string.replace("http://", "");
  $string = $string.replace("https://", "");
  if($string.length > 33) {
    return $string.substring(0,32)+"...";
  }
  return $string;
}
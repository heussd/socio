var data = require("self").data         
var tabs = require("tabs");
var {Cc,Ci} = require("chrome");
var ss = require("simple-storage");
var pref = Cc['@mozilla.org/preferences-service;1'].getService(Ci.nsIPrefBranch);  
  
  
try{
    console.log("Check if socIO.addBookmark preference exists");
     //You can change this value in about:config
     pref.getBoolPref('socIO.addBookmark');
  }catch(e){
      pref.setBoolPref('socIO.addBookmark',true);
}  


// docs: https://developer.mozilla.org/En/Places_Developer_Guide
var bookmarks = Cc["@mozilla.org/browser/nav-bookmarks-service;1"]
                .getService(Ci.nsINavBookmarksService);

var socioPanel = require("panel").Panel({
  width:400,
  height:300,         
  contentScriptFile:[data.url("jquery/jquery-1.6.2.min.js"), data.url("jquery/jquery-ui-1.8.16.custom.min.js"), data.url("js/socio.js"),],
  //contentScript: "indexLoad();",
  contentURL: data.url("index.html"),
  contentScriptWhen: "end"
});
 
var socioWidget = require("widget").Widget({
  id: "socio-icon",
  label: "socIO",
  contentURL: data.url("images/none.png"),
  panel: socioPanel
}); 

socioPanel.port.on("getTabToIndex", function() {
  socioPanel.port.emit("sendTabToIndex",tabs.activeTab.url);
});
            
socioPanel.port.on("getTabToRelated", function() {
  socioPanel.port.emit("sendTabToRelated",tabs.activeTab.url);
});          
            
socioPanel.port.on("openTab", function(url) {
  tabs.open({
      url: url
      });
      socioPanel.hide();
}); 

socioPanel.port.on("changeIcon", function(imgData) {
  console.log("changeIcon " + imgData);
  socioWidget.contentURL = data.url("images/" + imgData +".png");
}); 

socioPanel.on("show",function(){
   console.log("show");
   socioPanel.port.emit("startOver",""); 
   //socioPanel.port.emit("sendTabToIndex",tabs.activeTab.url);
});

tabs.on("activate",function(tab) {
   console.log("activate");
   socioPanel.port.emit("sendTabToIndex",tab.url);
});   

tabs.on("ready",function(tab){
   console.log("ready");
   if(tab == tabs.activeTab){
      socioPanel.port.emit("sendTabToIndex",tab.url);
   }
});           

socioPanel.port.on("addBookmark", function(tag) {
  console.log("addBookmark: " + tag);
  var getsBookmarked = pref.getBoolPref('socIO.addBookmark');
  
  if(getsBookmarked == false){
    return;
  }
  //Bookmark already exists?
  var nsUri = makeURI(tabs.activeTab.url);
  var title = tabs.activeTab.title;
  var count = 0;
  var bookmarkIds = bookmarks.getBookmarkIdsForURI(nsUri);
  if(bookmarkIds.length == 0){
    //Add new Bookmark and tag it
    var bId = bookmarks.insertBookmark(0, nsUri, bookmarks.DEFAULT_INDEX, title);
    bookmarks.setKeywordForBookmark(bId,tag);
  }else{
    //Add new Tag 
    for(var i=0; i<bookmarkIds.length; ++i){
         bookmarks.setKeywordForBookmark(bookmarkIds[i],tag);
    }
  }
});

function makeURI(aURL) {  
      var ioService = Cc["@mozilla.org/network/io-service;1"]  
                      .getService(Ci.nsIIOService);  
      return ioService.newURI(aURL, null,null);  
    }  
      
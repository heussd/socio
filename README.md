SocIO
=====

* Author:    Timm Heuss (Timm.Heuss AT web.de)
* Date:      September, 2011
* Last mod.: September, 2011
* Website:   <http://users.fbihome.de/~Heuss/srp/>
* GitHub:    <https://github.com/Heussd/socio/>


This software called SocIO implements the basic ideas of the concept [Social Resource Promotion](http://users.fbihome.de/~Heuss/srp/srp_heuss.pdf>) and utilizes semantic, REST and XMPP technologies in Java.


What does it do?
----------------
SocIO allows you to...

* assign tags to webpages and store them transparently in your personal RDF store.
* connect and exchange taggings with other Jabber-IDs (try <socio@jabber.ccc.de>).
* confirm tags that others gave to webpages.
* display related webpages, based on the tags assigned to the current page.


Requirements
------------

* A Jabber account. I suggest to use a dedicated account for SocIO.
* For maximum confort: [Chromium](http://www.chromium.org/) or [Google Chrome](http://www.google.de/chrome) browser (included HTML pages require some minor hacking to work with other compliant browsers, see notice below).


Usage
-----
To use SocIO, follow these steps:

1. Build the maven project or download the jar file from <http://users.fbihome.de/~Heuss/srp/socio.jar>.
2. Start the Java program on command line, it will ask for the Jabber credentials:
    ``java -jar socio.jar``
3. Use "Load unpackaged extension"-option in Chrome to load the folder `html` as extension or download the crx-file from <http://users.fbihome.de/~Heuss/srp/socio.crx> and load it into Chrome.
 
Browser dependance
------------------
SocIO includes a GUI in form of a Chromium or Chrome extension. Because these extensions basically consist of HTML, JavaScript and CSS, they should work in other compliant browsers, too. The only exception are the two Chrome-specific API-calls for the functionalities:

1. Retrieve the currently displayed webpage (used in the files `index.html` and `related.html`).
2. Open a specific webpage (used in the file `socio.js`).

These calls are encapsulated in a try-catch-clauses and thus work in other browsers. In case of the URL-retrival, the code falls back to <https://www.fbi.h-da.de/>, which could be manually overwritten to use it with other webapges:

	  try {
        chrome.tabs.getSelected(null,function(tab) {
          start(tab.url);
        });
      } catch(err) {
        // Fallback option, hardcode your URL here
        start("https://www.fbi.h-da.de/");
      }


What do you think?
------------------

Feel free to mail the author your feedback: Timm.Heuss AT web.de.


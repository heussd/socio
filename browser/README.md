How to install browser extensions
=================================


Google Chrome / Chromium
------------------------

1. Go to <chrome://extensions/>
2. Click `Load unpacked extension...`
3. Select folder `chrome/`


Mozilla Firefox 
------------------------

1. You need the Add-on SDK to build the Add-On (https://addons.mozilla.org/en-US/developers/builder)
2. Follow the instructions of Add-on SDK to install and start Add-on SDK
3. Change to the directory where you checked out the socio firefox extension directory
4. Run 'cfx xpi' to build the XPI package
5. Drag & Drop the generated xpi package into the Firefox window, and choose install




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



Building and installation instructions 
------------------------

1. You need the Add-on SDK to build the Add-On (https://addons.mozilla.org/en-US/developers/builder)
2. Follow the instructions of Add-on SDK to install and start Add-on SDK
3. Change to the directory where the firefox directory is
4. Run 'cfx xpi' to build the XPI package
5. Drag & Drop the generated xpi package into the Firefox window, and choose install

Automatically adding tagged URLs to the bookmarks
------------------------
The addon automatically adds tagged URLs to the Firefox bookmark database.
If you don't want this behaviour, go to <about:config> and change the value of "socIO.addBookmark" to "false".

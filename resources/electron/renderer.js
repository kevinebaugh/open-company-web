// The renderer process of the Carrot electron app
// https://electronjs.org/docs/tutorial/application-architecture#main-and-renderer-processes
var electron = require('electron');
var ipcRenderer = electron.ipcRenderer;
var package = require('../package.json');

console.log("Carrot desktop engage!");

// By attaching fields and functions to the window object,
// we allow the web page to access a controlled set of native
// desktop funcitonality.

// NOTE: you need to add these fields/functions to externs.js
// in order for them to avoid munging in production builds!

window.OCCarrotDesktop = {};

window.OCCarrotDesktop.showDesktopWindow = function() {
  console.log("Sending show-desktop-window IPC");
  ipcRenderer.send('show-desktop-window');
}

window.OCCarrotDesktop.setBadgeCount = function(count) {
  console.log("Sending set-badge-count IPC: " + count);
  ipcRenderer.send('set-badge-count', count);
};

window.OCCarrotDesktop.windowHasFocus = function() {
  console.log("Determining focus state of desktop window");
  return ipcRenderer.sendSync('window-has-focus?');
};

window.OCCarrotDesktop.getElectronAppVersion = function() {
  console.log("DBG Get electorn app version");
  return package.version;
}

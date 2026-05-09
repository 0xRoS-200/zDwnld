// zDwnld Helper — Background Service Worker
// Currently minimal; reserved for future context-menu integration.

chrome.runtime.onInstalled.addListener(() => {
  console.log('[zDwnld Helper] Extension installed. Make sure zDwnld is running.');
});

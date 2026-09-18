chrome.runtime.onInstalled.addListener(()=>console.info("Matkahelpotin Browser Bridge installed"));
chrome.sidePanel.setPanelBehavior({openPanelOnActionClick:true}).catch(console.error);
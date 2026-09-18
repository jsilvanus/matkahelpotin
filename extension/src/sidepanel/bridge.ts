import type {ContentRequest,ContentResponse,TargetSelection} from "../content";
async function ensureContentScript(tabId:number):Promise<void>{await chrome.scripting.executeScript({target:{tabId},files:["assets/content.js"]})}
async function send(tabId:number,message:ContentRequest):Promise<TargetSelection|null>{
  await ensureContentScript(tabId);
  const response=(await chrome.tabs.sendMessage(tabId,message)) as ContentResponse;
  if(!response.ok) throw new Error(response.error);
  return response.result;
}
export function selectTarget(tabId:number,selection:TargetSelection){return send(tabId,{type:"selectTarget",selection})}
export function detectTarget(tabId:number){return send(tabId,{type:"detect"})}
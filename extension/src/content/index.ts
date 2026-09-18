export type TargetKind="verottaja"|"visma_m2";
export type DatasetKind="commute"|"business_trip";
export interface TargetSelection{target:TargetKind;dataset:DatasetKind}
export type ContentRequest={type:"selectTarget";selection:TargetSelection}|{type:"detect"};
export type ContentResponse={ok:true;result:TargetSelection|null}|{ok:false;error:string};

interface TargetDomAdapter{selectDataset(dataset:DatasetKind):Promise<void>}

class StubDomAdapter implements TargetDomAdapter{
  constructor(private readonly target:TargetKind,private readonly document:Document){}
  async selectDataset(dataset:DatasetKind):Promise<void>{
    // TODO: Replace this with the real target DOM operations once selectors are supplied.
    console.debug("[Matkahelpotin stub]",this.target,dataset,this.document.title);
  }
}

function adapterFor(target:TargetKind):TargetDomAdapter{return new StubDomAdapter(target,document)}

function detectTarget():TargetKind|null{
  const host=location.hostname.toLowerCase();
  if(host.includes("vero")) return "verottaja";
  if(host.includes("visma")) return "visma_m2";
  return null;
}

async function handle(message:ContentRequest):Promise<ContentResponse>{
  if(message.type==="detect"){
    const target=detectTarget();
    return {ok:true,result:target?{target,dataset:"commute"}:null};
  }
  try{
    await adapterFor(message.selection.target).selectDataset(message.selection.dataset);
    return {ok:true,result:message.selection};
  }catch(error){
    return {ok:false,error:error instanceof Error?error.message:String(error)};
  }
}

declare global{interface Window{__matkahelpotinContentLoaded?:boolean}}
if(!window.__matkahelpotinContentLoaded){
  window.__matkahelpotinContentLoaded=true;
  chrome.runtime.onMessage.addListener((message:ContentRequest,_sender,sendResponse:(response:ContentResponse)=>void)=>{
    handle(message).then(sendResponse).catch(error=>sendResponse({ok:false,error:error instanceof Error?error.message:String(error)}));
    return true;
  });
}
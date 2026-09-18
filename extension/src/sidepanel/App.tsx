import {useEffect,useState} from "react";
import type {DatasetKind,TargetKind,TargetSelection} from "../content";
import {detectTarget,selectTarget} from "./bridge";

const TARGETS:Array<{kind:TargetKind;label:string}>=[{kind:"verottaja",label:"Verottaja"},{kind:"visma_m2",label:"Visma M2"}];
const DATASETS:Array<{kind:DatasetKind;label:string}>=[{kind:"commute",label:"Työmatka"},{kind:"business_trip",label:"Virkamatka"}];

async function activeTab():Promise<chrome.tabs.Tab>{
  const [tab]=await chrome.tabs.query({active:true,currentWindow:true});
  if(!tab?.id) throw new Error("No active tab.");
  return tab;
}

export function App(){
  const [selection,setSelection]=useState<TargetSelection>({target:"verottaja",dataset:"commute"});
  const [currentUrl,setCurrentUrl]=useState("");
  const [message,setMessage]=useState("Valitse kohdejärjestelmä.");
  const [busy,setBusy]=useState(false);

  useEffect(()=>{
    chrome.tabs.query({active:true,currentWindow:true}).then(async([tab])=>{
      setCurrentUrl(tab?.url??"");
      if(!tab?.id)return;
      try{const detected=await detectTarget(tab.id);if(detected){setSelection(detected);setMessage("Kohde tunnistettu.");}}catch{}
    });
  },[]);

  async function apply(next:TargetSelection){
    setSelection(next);setBusy(true);setMessage("");
    try{const tab=await activeTab();await selectTarget(tab.id!,next);setMessage((next.target==="verottaja"?"Verottaja":"Visma M2")+" — "+(next.dataset==="commute"?"työmatka":"virkamatka")+" valittu.");}
    catch(error){setMessage(error instanceof Error?error.message:"Kohteen vaihto epäonnistui.");}
    finally{setBusy(false);}
  }

  return <main>
    <h1>Matkahelpotin</h1>
    <p className="muted">Selainliityntä</p>
    <section><h2>Kohdejärjestelmä</h2><div className="row">
      {TARGETS.map(({kind,label})=><button key={kind} className={selection.target===kind?"target selected":"target"} disabled={busy} onClick={()=>void apply({...selection,target:kind})}>{label}</button>)}
    </div></section>
    <section><h2>Matkan tyyppi</h2><div className="row">
      {DATASETS.map(({kind,label})=><button key={kind} className={selection.dataset===kind?"choice selected":"choice"} disabled={busy} onClick={()=>void apply({...selection,dataset:kind})}>{label}</button>)}
    </div></section>
    <section className="card"><div className="status">{message}</div><div className="muted">Nykyinen sivu: {currentUrl||"tuntematon"}</div></section>
    <p className="muted stub">Verottajan ja Visma M2:n DOM-operaatiot ovat toistaiseksi stubattu. Kun todelliset DOM-rakenteet ovat tiedossa, vain kohdeadapterit täydennetään.</p>
  </main>;
}
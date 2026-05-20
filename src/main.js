const app = document.getElementById("app");
let screen = "dashboard";
let menuOpen = false;

const species = ["Raccoon","Grey Squirrel","Red Squirrel","Flying Squirrel","Bat","Skunk","Groundhog","Bird","Snake","Opossum","Rodent","Rat","Mouse","Carpenter Bee","Other"];

let db = JSON.parse(localStorage.getItem("ww_fieldops") || '{"jobs":[],"photos":[],"visits":[],"repairs":[]}');

function save(){ localStorage.setItem("ww_fieldops", JSON.stringify(db)); render(); }
function id(){ return Date.now().toString(36)+Math.random().toString(36).slice(2); }
function esc(v){ return String(v||"").replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m])); }

window.go = v => { screen=v; menuOpen=false; render(); };
window.toggleMenu = () => { menuOpen=!menuOpen; render(); };

window.addJob = () => {
  const job = {
    id:id(),
    customer: customer.value,
    phone: phone.value,
    address: address.value,
    town: town.value,
    species: speciesSelect.value,
    title: title.value || speciesSelect.value + " job",
    notes: notes.value,
    status:"Active",
    created:new Date().toLocaleString()
  };
  if(!job.customer || !job.address) return alert("Customer and address required.");
  db.jobs.unshift(job);
  save();
};

function layout(content){
  app.innerHTML = `
    <header>
      <div><h1>Wildlife Whisperer FieldOps</h1><div class="sub">${screen}</div></div>
      <button class="menuBtn" onclick="toggleMenu()">☰</button>
    </header>
    ${menuOpen ? `<div class="drawer">
      <button onclick="go('dashboard')">🏠 Dashboard</button>
      <button onclick="go('jobs')">🦝 Jobs</button>
      <button onclick="go('heatmap')">🗺️ Heat Map</button>
      <button onclick="go('estimate')">💵 Estimator</button>
      <button onclick="go('ai')">🧠 AI Assistant</button>
    </div>` : ""}
    <main>${content}</main>
  `;
}

function jobCards(list){
  return list.length ? list.map(j=>`
    <div class="card">
      <h3>${esc(j.title)}</h3>
      <p>${esc(j.customer)} · <a href="tel:${esc(j.phone)}">${esc(j.phone)}</a></p>
      <p>${esc(j.address)}</p>
      <span>${esc(j.species)}</span><span>${esc(j.status)}</span>
      <button onclick="openJob('${j.id}')">Open</button>
    </div>
  `).join("") : `<div class="card">No jobs yet.</div>`;
}

window.openJob = jobId => {
  const j = db.jobs.find(x=>x.id===jobId);
  layout(`<section class="card">
    <h2>${esc(j.title)}</h2>
    <p>${esc(j.customer)} · <a href="tel:${esc(j.phone)}">${esc(j.phone)}</a></p>
    <p>${esc(j.address)}</p>
    <textarea id="editNotes">${esc(j.notes)}</textarea>
    <button onclick="saveJobNotes('${j.id}')">Save Notes</button>
    <button onclick="go('jobs')">Back</button>
  </section>`);
};

window.saveJobNotes = jobId => {
  const j = db.jobs.find(x=>x.id===jobId);
  j.notes = editNotes.value;
  save();
};

function dashboard(){
  layout(`
    <section class="grid">
      <div class="card"><h2>${db.jobs.filter(j=>j.status==="Active").length}</h2><p>Active jobs</p></div>
      <div class="card"><h2>${db.photos.length}</h2><p>Photos</p></div>
      <div class="card"><h2>${db.visits.length}</h2><p>Visits</p></div>
      <div class="card"><h2>${db.repairs.length}</h2><p>Repairs</p></div>
    </section>
    <h2>Fast Create Job</h2>
    <section class="card form">
      <input id="customer" placeholder="Customer name">
      <input id="phone" placeholder="Phone">
      <input id="address" placeholder="Address">
      <input id="town" placeholder="Town / area">
      <select id="speciesSelect">${species.map(s=>`<option>${s}</option>`).join("")}</select>
      <input id="title" placeholder="Job title">
      <textarea id="notes" placeholder="Notes / scope"></textarea>
      <button onclick="addJob()">Create Job</button>
    </section>
    <h2>Recent Jobs</h2>
    ${jobCards(db.jobs.slice(0,5))}
  `);
}

function jobs(){ layout(`<h2>Jobs</h2>${jobCards(db.jobs)}`); }

function heatmap(){
  const towns = db.jobs.reduce((a,j)=>{a[j.town||"Unknown"]=(a[j.town||"Unknown"]||0)+1; return a;},{});
  layout(`<h2>Heat Map</h2>${Object.entries(towns).map(([t,c])=>`<div class="card"><h3>${esc(t)}</h3><p>${c} job(s)</p></div>`).join("") || `<div class="card">No map data.</div>`}`);
}

function estimate(){
  layout(`<h2>Estimator</h2><div class="card form">
    <select id="estSpecies">${species.map(s=>`<option>${s}</option>`).join("")}</select>
    <select id="severity"><option>Low</option><option>Medium</option><option>High</option><option>Critical</option></select>
    <button onclick="calcEstimate()">Calculate</button>
    <textarea id="estimateOut"></textarea>
  </div>`);
}

window.calcEstimate = () => {
  const base = {"Raccoon":650,"Grey Squirrel":550,"Red Squirrel":575,"Flying Squirrel":750,"Bat":950,"Skunk":450,"Groundhog":450,"Rat":350,"Mouse":325,"Carpenter Bee":350}[estSpecies.value] || 500;
  const mult = {Low:1,Medium:1.35,High:1.8,Critical:2.4}[severity.value];
  estimateOut.value = `Recommended estimate: $${Math.round(base*mult)}`;
};

function ai(){
  layout(`<h2>AI Species Assistant</h2><div class="card form">
    <select id="aiSpecies">${species.map(s=>`<option>${s}</option>`).join("")}</select>
    <button onclick="speciesAdvice()">Suggest</button>
    <textarea id="aiOut"></textarea>
  </div>`);
}

window.speciesAdvice = () => {
  const advice = {
    "Flying Squirrel":"Night activity, colony nesting, wall/attic movement. Inspect high gaps, vents, soffit returns.",
    "Red Squirrel":"Aggressive chewing, cone caches, repeated soffit penetration.",
    "Grey Squirrel":"Larger attic nesting, roofline travel, soffit/fascia/gable vent entry.",
    "Raccoon":"Inspect roof access, chimney, attic latrine areas, soffit damage, insulation compression.",
    "Bat":"Confirm legal exclusion window, guano accumulation, staining, roost points, one-way device placement."
  };
  aiOut.value = advice[aiSpecies.value] || "Track entry behavior, seasonality, recurrence, and secondary entry points.";
};

function render(){
  if(screen==="dashboard") dashboard();
  if(screen==="jobs") jobs();
  if(screen==="heatmap") heatmap();
  if(screen==="estimate") estimate();
  if(screen==="ai") ai();
}

render();
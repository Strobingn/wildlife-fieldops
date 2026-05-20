import { createClient } from "@supabase/supabase-js";

const SUPABASE_URL = "https://hgdzmwfcghtilyqagjak.supabase.co";
const SUPABASE_ANON_KEY = "sb_publishable_ExD5HM7IkieB_ZWItda83w_rFwR3nrB";

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const style = document.createElement("style");

style.textContent = `
:root{
  --bg:#070b10;
  --panel:#101822;
  --panel2:#162231;
  --text:#edf5ff;
  --muted:#9fb0c3;
  --accent:#61e28d;
  --line:#26384d;
  --blue:#7ab7ff;
}

*{
  box-sizing:border-box;
}

body{
  margin:0;
  background:radial-gradient(circle at top,#102033 0,#070b10 50%);
  color:var(--text);
  font-family:system-ui,sans-serif;
}

header{
  position:sticky;
  top:0;
  z-index:1000;
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:18px;
  border-bottom:1px solid var(--line);
  background:#070b10ee;
}

h1{
  margin:0;
  font-size:24px;
}

h2{
  margin:24px 0 12px;
}

.sub{
  color:var(--muted);
  margin-top:4px;
  text-transform:capitalize;
}

main{
  max-width:900px;
  margin:auto;
  padding:18px;
}

.grid{
  display:grid;
  grid-template-columns:1fr 1fr;
  gap:14px;
}

.card{
  background:#101822ee;
  border:1px solid var(--line);
  border-radius:24px;
  padding:18px;
  margin-bottom:14px;
}

.card h2{
  margin:0;
  font-size:34px;
}

.card p{
  color:var(--muted);
}

.form{
  display:grid;
  gap:12px;
}

input,
select,
textarea,
button{
  font:inherit;
}

input,
select,
textarea{
  width:100%;
  background:#0d1520;
  border:1px solid var(--line);
  color:var(--text);
  border-radius:18px;
  padding:16px;
  font-size:18px;
}

textarea{
  min-height:140px;
  resize:vertical;
}

button{
  border:0;
  border-radius:18px;
  padding:16px;
  font-weight:900;
  font-size:18px;
  background:var(--accent);
  color:#041108;
}

.menuBtn{
  width:68px;
  height:68px;
  border-radius:24px;
  background:var(--panel2);
  border:1px solid var(--line);
  color:white;
  font-size:34px;
}

.drawer{
  position:fixed;
  top:100px;
  left:20px;
  right:20px;
  z-index:9999;
  background:#101822;
  border:1px solid var(--line);
  border-radius:24px;
  padding:14px;
  display:grid;
  gap:10px;
}

.drawer button{
  background:#203044;
  color:white;
  text-align:left;
}

.tag{
  display:inline-block;
  background:#203044;
  border:1px solid var(--line);
  color:var(--muted);
  border-radius:999px;
  padding:4px 10px;
  margin-right:5px;
  font-size:13px;
}

a{
  color:var(--blue);
}
`;

document.head.appendChild(style);

const app = document.getElementById("app");

const species = [
  "Raccoon",
  "Grey Squirrel",
  "Red Squirrel",
  "Flying Squirrel",
  "Bat",
  "Skunk",
  "Groundhog",
  "Bird",
  "Snake",
  "Opossum",
  "Rodent",
  "Rat",
  "Mouse",
  "Carpenter Bee",
  "Other"
];

let db = {
  jobs: []
};

let screen = "dashboard";
let menuOpen = false;
let isLoading = false;

function esc(value) {
  return String(value || "").replace(/[&<>"']/g, character => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[character]));
}

function go(view) {
  screen = view;
  menuOpen = false;
  render();
}

window.go = go;

window.toggleMenu = function () {
  menuOpen = !menuOpen;
  render();
};

async function loadJobs() {
  isLoading = true;
  render();

  const { data, error } = await supabase
    .from("jobs")
    .select("*")
    .order("created_at", { ascending: false });

  isLoading = false;

  if (error) {
    alert("Supabase load error: " + error.message);
    render();
    return;
  }

  db.jobs = data || [];
  render();
}

window.addJob = async function () {
  const customerInput = document.getElementById("customer");
  const phoneInput = document.getElementById("phone");
  const addressInput = document.getElementById("address");
  const townInput = document.getElementById("town");
  const speciesInput = document.getElementById("speciesSelect");
  const titleInput = document.getElementById("title");
  const notesInput = document.getElementById("notes");

  const payload = {
    customer_name: customerInput.value.trim(),
    customer_phone: phoneInput.value.trim(),
    address: addressInput.value.trim(),
    town: townInput.value.trim(),
    species: speciesInput.value,
    title: titleInput.value.trim() || speciesInput.value + " job",
    scope: notesInput.value.trim(),
    status: "Active"
  };

  if (!payload.customer_name || !payload.address) {
    alert("Customer and address required.");
    return;
  }

  const { error } = await supabase
    .from("jobs")
    .insert(payload);

  if (error) {
    alert("Supabase save error: " + error.message);
    return;
  }

  await loadJobs();
};

function layout(content) {
  app.innerHTML = `
    <header>
      <div>
        <h1>Wildlife Whisperer FieldOps</h1>
        <div class="sub">
          ${esc(screen)} ${isLoading ? "· syncing..." : ""}
        </div>
      </div>

      <button class="menuBtn" onclick="toggleMenu()">☰</button>
    </header>

    ${
      menuOpen
        ? `
          <div class="drawer">
            <button onclick="go('dashboard')">🏠 Dashboard</button>
            <button onclick="go('jobs')">🦝 Jobs</button>
            <button onclick="go('heatmap')">🗺️ Heat Map</button>
            <button onclick="go('estimate')">💵 Estimator</button>
            <button onclick="go('ai')">🧠 AI Assistant</button>
          </div>
        `
        : ""
    }

    <main>
      ${content}
    </main>
  `;
}

function dashboard() {
  layout(`
    <section class="grid">
      <div class="card">
        <h2>${db.jobs.filter(job => job.status === "Active").length}</h2>
        <p>Active jobs</p>
      </div>

      <div class="card">
        <h2>${db.jobs.length}</h2>
        <p>Total jobs</p>
      </div>
    </section>

    <h2>Fast Create Job</h2>

    <section class="card form">
      <input id="customer" placeholder="Customer">
      <input id="phone" placeholder="Phone">
      <input id="address" placeholder="Address">
      <input id="town" placeholder="Town">

      <select id="speciesSelect">
        ${species.map(item => `<option>${item}</option>`).join("")}
      </select>

      <input id="title" placeholder="Job title">
      <textarea id="notes" placeholder="Notes / scope"></textarea>

      <button onclick="addJob()">Create Cloud Job</button>
    </section>

    <h2>Recent Jobs</h2>

    ${jobCards(db.jobs.slice(0, 5))}
  `);
}

function jobCards(list) {
  if (!list.length) {
    return `
      <div class="card">
        No jobs yet.
      </div>
    `;
  }

  return list.map(job => `
    <div class="card">
      <h3>${esc(job.title)}</h3>
      <p>${esc(job.customer_name)}</p>
      <p>${esc(job.address)}</p>

      <p>
        <span class="tag">${esc(job.species)}</span>
        <span class="tag">${esc(job.status)}</span>
      </p>
    </div>
  `).join("");
}

function jobs() {
  layout(`
    <h2>Jobs</h2>
    ${jobCards(db.jobs)}
  `);
}

function heatmap() {
  const towns = {};

  db.jobs.forEach(job => {
    const town = job.town || "Unknown";
    towns[town] = (towns[town] || 0) + 1;
  });

  const townCards = Object.entries(towns)
    .map(([town, count]) => `
      <div class="card">
        <h3>${esc(town)}</h3>
        <p>${count} jobs</p>
      </div>
    `)
    .join("");

  layout(`
    <h2>Heat Map</h2>
    ${townCards || `<div class="card">No map data yet.</div>`}
  `);
}

function estimate() {
  layout(`
    <h2>Estimator</h2>

    <div class="card form">
      <select id="estSpecies">
        ${species.map(item => `<option>${item}</option>`).join("")}
      </select>

      <select id="severity">
        <option>Low</option>
        <option>Medium</option>
        <option>High</option>
        <option>Critical</option>
      </select>

      <button onclick="calcEstimate()">Calculate</button>

      <textarea id="estimateOut"></textarea>
    </div>
  `);
}

window.calcEstimate = function () {
  const selectedSpecies = document.getElementById("estSpecies").value;
  const selectedSeverity = document.getElementById("severity").value;

  const basePrices = {
    Raccoon: 650,
    "Grey Squirrel": 550,
    "Red Squirrel": 575,
    "Flying Squirrel": 750,
    Bat: 950,
    Skunk: 450,
    Groundhog: 450,
    Rat: 350,
    Mouse: 325,
    "Carpenter Bee": 350
  };

  const severityMultipliers = {
    Low: 1,
    Medium: 1.35,
    High: 1.8,
    Critical: 2.4
  };

  const price = Math.round(
    (basePrices[selectedSpecies] || 500) *
    (severityMultipliers[selectedSeverity] || 1)
  );

  document.getElementById("estimateOut").value =
    "Recommended estimate: $" + price;
};

function ai() {
  layout(`
    <h2>AI Species Assistant</h2>

    <div class="card form">
      <select id="aiSpecies">
        ${species.map(item => `<option>${item}</option>`).join("")}
      </select>

      <button onclick="speciesAdvice()">Suggest</button>

      <textarea id="aiOut"></textarea>
    </div>
  `);
}

window.speciesAdvice = function () {
  const selectedSpecies = document.getElementById("aiSpecies").value;

  const advice = {
    "Flying Squirrel": "Inspect soffits, returns, attic runs, vents, roofline gaps, and night movement paths.",
    "Red Squirrel": "Inspect fascia, roof edge, ridge vent chewing, cone caches, and repeat entry points.",
    "Grey Squirrel": "Inspect attic travel routes, soffit/fascia openings, gable vents, and roof edge chewing.",
    Raccoon: "Inspect roof returns, soffits, chimney caps, attic latrine areas, and insulation compression.",
    Bat: "Check legal exclusion windows, guano zones, staining, roost gaps, and one-way device locations.",
    Rat: "Inspect foundation gaps, crawlspace trails, burrows, grease marks, droppings, and food/water sources.",
    Mouse: "Inspect utility penetrations, sill plates, garage corners, basement gaps, and food sources."
  };

  document.getElementById("aiOut").value =
    advice[selectedSpecies] || "Inspect entry points, recurrence patterns, seasonal behavior, and secondary openings.";
};

function render() {
  if (screen === "dashboard") {
    dashboard();
  }

  if (screen === "jobs") {
    jobs();
  }

  if (screen === "heatmap") {
    heatmap();
  }

  if (screen === "estimate") {
    estimate();
  }

  if (screen === "ai") {
    ai();
  }
}

loadJobs();select,
textarea{
  width:100%;
  background:#0d1520;
  border:1px solid var(--line);
  color:var(--text);
  border-radius:18px;
  padding:16px;
  font-size:18px;
}

textarea{
  min-height:140px;
  resize:vertical;
}

button{
  border:0;
  border-radius:18px;
  padding:16px;
  font-weight:900;
  font-size:18px;
  background:var(--accent);
  color:#041108;
}

.menuBtn{
  width:68px;
  height:68px;
  border-radius:24px;
  background:var(--panel2);
  border:1px solid var(--line);
  color:white;
  font-size:34px;
}

.drawer{
  position:fixed;
  top:100px;
  left:20px;
  right:20px;
  z-index:9999;
  background:#101822;
  border:1px solid var(--line);
  border-radius:24px;
  padding:14px;
  display:grid;
  gap:10px;
}

.drawer button{
  background:#203044;
  color:white;
  text-align:left;
}

.tag{
  display:inline-block;
  background:#203044;
  border:1px solid var(--line);
  color:var(--muted);
  border-radius:999px;
  padding:4px 10px;
  margin-right:5px;
  font-size:13px;
}

a{
  color:var(--blue);
}

@media(max-width:700px){
  .grid{
    grid-template-columns:1fr 1fr;
  }

  h1{
    font-size:22px;
  }
}
`;

document.head.appendChild(style);

const app = document.getElementById("app");

const species = [
  "Raccoon",
  "Grey Squirrel",
  "Red Squirrel",
  "Flying Squirrel",
  "Bat",
  "Skunk",
  "Groundhog",
  "Bird",
  "Snake",
  "Opossum",
  "Rodent",
  "Rat",
  "Mouse",
  "Carpenter Bee",
  "Other"
];

let db = {
  jobs:[]
};

let screen = "dashboard";
let menuOpen = false;

function esc(v){
  return String(v || "").replace(/[&<>"']/g,m=>({
    "&":"&amp;",
    "<":"&lt;",
    ">":"&gt;",
    '"':"&quot;",
    "'":"&#039;"
  }[m]));
}

function go(view){
  screen = view;
  menuOpen = false;
  render();
}

window.go = go;

window.toggleMenu = function(){
  menuOpen = !menuOpen;
  render();
};

async function loadJobs(){

  const { data,error } = await supabase
    .from("jobs")
    .select("*")
    .order("created_at",{ ascending:false });

  if(error){
    alert(error.message);
    return;
  }

  db.jobs = data || [];

  render();
}

window.addJob = async function(){

  const payload = {
    customer_name:
      document.getElementById("customer").value,

    customer_phone:
      document.getElementById("phone").value,

    address:
      document.getElementById("address").value,

    town:
      document.getElementById("town").value,

    species:
      document.getElementById("speciesSelect").value,

    title:
      document.getElementById("title").value ||

      document.getElementById("speciesSelect").value + " job",

    scope:
      document.getElementById("notes").value,

    status:"Active"
  };

  if(
    !payload.customer_name ||
    !payload.address
  ){
    alert("Customer and address required.");
    return;
  }

  const { error } = await supabase
    .from("jobs")
    .insert(payload);

  if(error){
    alert(error.message);
    return;
  }

  await loadJobs();
};

function layout(content){

  app.innerHTML = `
    <header>
      <div>
        <h1>Wildlife Whisperer FieldOps</h1>
        <div class="sub">${screen}</div>
      </div>

      <button
        class="menuBtn"
        onclick="toggleMenu()"
      >
        ☰
      </button>
    </header>

    ${
      menuOpen
      ?
      `
      <div class="drawer">

        <button onclick="go('dashboard')">
          🏠 Dashboard
        </button>

        <button onclick="go('jobs')">
          🦝 Jobs
        </button>

        <button onclick="go('heatmap')">
          🗺️ Heat Map
        </button>

        <button onclick="go('estimate')">
          💵 Estimator
        </button>

        <button onclick="go('ai')">
          🧠 AI Assistant
        </button>

      </div>
      `
      :
      ""
    }

    <main>
      ${content}
    </main>
  `;
}

function dashboard(){

  layout(`

    <section class="grid">

      <div class="card">
        <h2>
          ${
            db.jobs.filter(
              j => j.status === "Active"
            ).length
          }
        </h2>

        <p>Active jobs</p>
      </div>

      <div class="card">
        <h2>${db.jobs.length}</h2>
        <p>Total jobs</p>
      </div>

    </section>

    <h2>Fast Create Job</h2>

    <section class="card form">

      <input
        id="customer"
        placeholder="Customer"
      >

      <input
        id="phone"
        placeholder="Phone"
      >

      <input
        id="address"
        placeholder="Address"
      >

      <input
        id="town"
        placeholder="Town"
      >

      <select id="speciesSelect">

        ${
          species.map(
            s => `<option>${s}</option>`
          ).join("")
        }

      </select>

      <input
        id="title"
        placeholder="Job title"
      >

      <textarea
        id="notes"
        placeholder="Notes / scope"
      ></textarea>

      <button onclick="addJob()">
        Create Cloud Job
      </button>

    </section>

    <h2>Recent Jobs</h2>

    ${
      jobCards(
        db.jobs.slice(0,5)
      )
    }

  `);
}

function jobCards(list){

  if(!list.length){

    return `
      <div class="card">
        No jobs yet.
      </div>
    `;
  }

  return list.map(j=>`

    <div class="card">

      <h3>${esc(j.title)}</h3>

      <p>
        ${esc(j.customer_name)}
      </p>

      <p>
        ${esc(j.address)}
      </p>

      <p>

        <span class="tag">
          ${esc(j.species)}
        </span>

        <span class="tag">
          ${esc(j.status)}
        </span>

      </p>

    </div>

  `).join("");
}

function jobs(){

  layout(`

    <h2>Jobs</h2>

    ${
      jobCards(db.jobs)
    }

  `);
}

function heatmap(){

  const towns = {};

  db.jobs.forEach(j=>{

    const town = j.town || "Unknown";

    towns[town] = (towns[town] || 0) + 1;

  });

  layout(`

    <h2>Heat Map</h2>

    ${
      Object.entries(towns).map(
        ([town,count]) => `

        <div class="card">

          <h3>${esc(town)}</h3>

          <p>${count} jobs</p>

        </div>

      `
      ).join("")
    }

  `);
}

function estimate(){

  layout(`

    <h2>Estimator</h2>

    <div class="card form">

      <select id="estSpecies">

        ${
          species.map(
            s => `<option>${s}</option>`
          ).join("")
        }

      </select>

      <select id="severity">

        <option>Low</option>
        <option>Medium</option>
        <option>High</option>
        <option>Critical</option>

      </select>

      <button onclick="calcEstimate()">
        Calculate
      </button>

      <textarea id="estimateOut"></textarea>

    </div>

  `);
}

window.calcEstimate = function(){

  const base = {
    "Raccoon":650,
    "Grey Squirrel":550,
    "Red Squirrel":575,
    "Flying Squirrel":750,
    "Bat":950
  }[
    document.getElementById(
      "estSpecies"
    ).value
  ] || 500;

  const mult = {
    Low:1,
    Medium:1.35,
    High:1.8,
    Critical:2.4
  }[
    document.getElementById(
      "severity"
    ).value
  ];

  document.getElementById(
    "estimateOut"
  ).value =
    "Recommended estimate: $" +
    Math.round(base * mult);
};

function ai(){

  layout(`

    <h2>AI Species Assistant</h2>

    <div class="card form">

      <select id="aiSpecies">

        ${
          species.map(
            s => `<option>${s}</option>`
          ).join("")
        }

      </select>

      <button onclick="speciesAdvice()">
        Suggest
      </button>

      <textarea id="aiOut"></textarea>

    </div>

  `);
}

window.speciesAdvice = function(){

  const advice = {
    "Flying Squirrel":
      "Inspect soffits, returns, attic runs, vents.",

    "Red Squirrel":
      "Inspect fascia, roof edge, ridge vent chewing.",

    "Grey Squirrel":
      "Inspect attic travel routes and entry gaps.",

    "Raccoon":
      "Inspect roof returns, soffits, chimney caps.",

    "Bat":
      "Check exclusion windows and guano zones."
  };

  const species =
    document.getElementById(
      "aiSpecies"
    ).value;

  document.getElementById(
    "aiOut"
  ).value =
    advice[species] ||
    "Inspect entry points and recurrence.";
};

function render(){

  if(screen === "dashboard"){
    dashboard();
  }

  if(screen === "jobs"){
    jobs();
  }

  if(screen === "heatmap"){
    heatmap();
  }

  if(screen === "estimate"){
    estimate();
  }

  if(screen === "ai"){
    ai();
  }
}

loadJobs();    .from("jobs")
    .update({
      scope: notes,
      updated_at: new Date().toISOString()
    })
    .eq("id", jobId);

  if (error) {
    alert("Supabase update error: " + error.message);
    return;
  }

  await loadJobs();
  go("jobs");
}

function go(view) {
  screen = view;
  menuOpen = false;
  render();
}

function toggleMenu() {
  menuOpen = !menuOpen;
  render();
}

window.go = go;
window.toggleMenu = toggleMenu;
window.addJob = addJob;
window.saveJobNotes = saveJobNotes;

function layout(content) {
  app.innerHTML = `
    <style>
      :root {
        --bg: #070b10;
        --panel: #101822;
        --panel2: #162231;
        --text: #edf5ff;
        --muted: #9fb0c3;
        --accent: #61e28d;
        --blue: #7ab7ff;
        --line: #26384d;
        --danger: #ff6b6b;
      }

      * {
        box-sizing: border-box;
      }

      body {
        margin: 0;
        background: radial-gradient(circle at top, #102033 0, #070b10 50%);
        color: var(--text);
        font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      }

      header {
        position: sticky;
        top: 0;
        z-index: 1000;
        background: #070b10ee;
        border-bottom: 1px solid var(--line);
        padding: 18px 22px;
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      h1 {
        margin: 0;
        font-size: 22px;
      }

      h2 {
        margin: 24px 0 12px;
        font-size: 22px;
      }

      h3 {
        margin: 0 0 8px;
      }

      .sub {
        color: var(--muted);
        font-size: 16px;
        margin-top: 4px;
        text-transform: capitalize;
      }

      main {
        padding: 18px 22px 40px;
        max-width: 900px;
        margin: auto;
      }

      .grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 14px;
      }

      .card {
        background: #101822ee;
        border: 1px solid var(--line);
        border-radius: 22px;
        padding: 18px;
        box-shadow: 0 12px 30px rgba(0,0,0,.22);
        margin-bottom: 14px;
      }

      .card h2 {
        margin: 0;
        font-size: 32px;
      }

      .card p {
        color: var(--muted);
        margin: 8px 0;
      }

      .form {
        display: grid;
        gap: 12px;
      }

      input,
      select,
      textarea,
      button {
        font: inherit;
      }

      input,
      select,
      textarea {
        width: 100%;
        background: #0d1520;
        color: var(--text);
        border: 1px solid var(--line);
        border-radius: 16px;
        padding: 15px;
        font-size: 18px;
      }

      textarea {
        min-height: 130px;
        resize: vertical;
      }

      button {
        border: 0;
        border-radius: 16px;
        padding: 15px 18px;
        background: var(--accent);
        color: #051108;
        font-weight: 900;
        font-size: 18px;
      }

      .menuBtn {
        width: 64px;
        height: 64px;
        background: var(--panel2);
        color: var(--text);
        border: 1px solid var(--line);
        border-radius: 22px;
        font-size: 36px;
        line-height: 1;
      }

      .drawer {
        position: fixed;
        top: 100px;
        right: 20px;
        left: 20px;
        z-index: 9999;
        background: #101822;
        border: 1px solid var(--line);
        border-radius: 22px;
        padding: 14px;
        display: grid;
        gap: 10px;
        box-shadow: 0 20px 80px rgba(0,0,0,.55);
      }

      .drawer button {
        width: 100%;
        text-align: left;
        background: #203044;
        color: var(--text);
        border: 1px solid var(--line);
      }

      a {
        color: var(--blue);
      }

      span {
        display: inline-block;
        background: #203044;
        border: 1px solid var(--line);
        color: var(--muted);
        border-radius: 999px;
        padding: 4px 9px;
        margin-right: 5px;
        font-size: 13px;
      }

      .danger {
        background: var(--danger);
        color: #240808;
      }

      .loading {
        opacity: .7;
        font-size: 14px;
        color: var(--muted);
      }
    </style>

    <header>
      <div>
        <h1>Wildlife Whisperer FieldOps</h1>
        <div class="sub">${esc(screen)} ${loading ? "· syncing..." : ""}</div>
      </div>

      <button class="menuBtn" onclick="toggleMenu()">☰</button>
    </header>

    ${menuOpen ? `
      <div class="drawer">
        <button onclick="go('dashboard')">🏠 Dashboard</button>
        <button onclick="go('jobs')">🦝 Jobs</button>
        <button onclick="go('heatmap')">🗺️ Heat Map</button>
        <button onclick="go('estimate')">💵 Estimator</button>
        <button onclick="go('ai')">🧠 AI Assistant</button>
      </div>
    ` : ""}

    <main>
      ${content}
    </main>
  `;
}

function dashboard() {
  layout(`
    <section class="grid">
      <div class="card">
        <h2>${db.jobs.filter(j => j.status === "Active").length}</h2>
        <p>Active jobs</p>
      </div>

      <div class="card">
        <h2>${db.jobs.length}</h2>
        <p>Total cloud jobs</p>
      </div>

      <div class="card">
        <h2>${db.photos.length}</h2>
        <p>Photos</p>
      </div>

      <div class="card">
        <h2>${db.repairs.length}</h2>
        <p>Repairs</p>
      </div>
    </section>

    <h2>Fast Create Job</h2>

    <section class="card form">
      <input id="customer" placeholder="Customer name">
      <input id="phone" placeholder="Phone">
      <input id="address" placeholder="Address">
      <input id="town" placeholder="Town / area">

      <select id="speciesSelect">
        ${species.map(s => `<option>${s}</option>`).join("")}
      </select>

      <input id="title" placeholder="Job title">
      <textarea id="notes" placeholder="Notes / scope"></textarea>

      <button onclick="addJob()">Create Cloud Job</button>
    </section>

    <h2>Recent Jobs</h2>
    ${jobCards(db.jobs.slice(0, 5))}
  `);
}

function jobCards(list) {
  if (!list.length) {
    return `<div class="card">No jobs yet.</div>`;
  }

  return list.map(j => `
    <div class="card">
      <h3>${esc(j.title)}</h3>
      <p>${esc(j.customer_name)} · <a href="tel:${esc(j.customer_phone)}">${esc(j.customer_phone)}</a></p>
      <p>${esc(j.address)}</p>
      <p>
        <span>${esc(j.species)}</span>
        <span>${esc(j.status)}</span>
      </p>
      <button onclick="openJob('${j.id}')">Open</button>
    </div>
  `).join("");
}

window.openJob = function(jobId) {
  const j = db.jobs.find(x => x.id === jobId);

  layout(`
    <section class="card">
      <h2>${esc(j.title)}</h2>
      <p>${esc(j.customer_name)} · <a href="tel:${esc(j.customer_phone)}">${esc(j.customer_phone)}</a></p>
      <p>${esc(j.address)}</p>
      <p>${esc(j.species)}</p>

      <textarea id="editNotes">${esc(j.scope)}</textarea>

      <button onclick="saveJobNotes('${j.id}')">Save Notes To Supabase</button>
      <button onclick="go('jobs')">Back</button>
    </section>
  `);
};

function jobs() {
  layout(`
    <h2>Jobs</h2>
    ${jobCards(db.jobs)}
  `);
}

function heatmap() {
  const towns = db.jobs.reduce((acc, j) => {
    acc[j.town || "Unknown"] = (acc[j.town || "Unknown"] || 0) + 1;
    return acc;
  }, {});

  layout(`
    <h2>Heat Map</h2>
    ${
      Object.entries(towns).map(([town, count]) => `
        <div class="card">
          <h3>${esc(town)}</h3>
          <p>${count} job(s)</p>
        </div>
      `).join("") || `<div class="card">No map data yet.</div>`
    }
  `);
}

function estimate() {
  layout(`
    <h2>Estimator</h2>

    <div class="card form">
      <select id="estSpecies">
        ${species.map(s => `<option>${s}</option>`).join("")}
      </select>

      <select id="severity">
        <option>Low</option>
        <option>Medium</option>
        <option>High</option>
        <option>Critical</option>
      </select>

      <button onclick="calcEstimate()">Calculate</button>
      <textarea id="estimateOut"></textarea>
    </div>
  `);
}

window.calcEstimate = function() {
  const base = {
    Raccoon: 650,
    "Grey Squirrel": 550,
    "Red Squirrel": 575,
    "Flying Squirrel": 750,
    Bat: 950,
    Skunk: 450,
    Groundhog: 450,
    Rat: 350,
    Mouse: 325,
    "Carpenter Bee": 350
  }[document.getElementById("estSpecies").value] || 500;

  const mult = {
    Low: 1,
    Medium: 1.35,
    High: 1.8,
    Critical: 2.4
  }[document.getElementById("severity").value];

  document.getElementById("estimateOut").value =
    `Recommended estimate: $${Math.round(base * mult)}`;
};

function ai() {
  layout(`
    <h2>AI Species Assistant</h2>

    <div class="card form">
      <select id="aiSpecies">
        ${species.map(s => `<option>${s}</option>`).join("")}
      </select>

      <button onclick="speciesAdvice()">Suggest</button>
      <textarea id="aiOut"></textarea>
    </div>
  `);
}

window.speciesAdvice = function() {
  const s = document.getElementById("aiSpecies").value;

  const advice = {
    "Flying Squirrel": "Night activity, colony nesting, wall/attic movement. Inspect high gaps, vents, and soffit returns.",
    "Red Squirrel": "Aggressive chewing, cone caches, repeated soffit penetration. Check fascia, ridge gaps, and wiring risk.",
    "Grey Squirrel": "Larger attic nesting, roofline travel, soffit/fascia/gable vent entry.",
    Raccoon: "Inspect roof access, chimney, attic latrine areas, soffit damage, and insulation compression.",
    Bat: "Confirm legal exclusion window, guano accumulation, staining, roost points, and one-way device placement.",
    Rat: "Inspect foundation gaps, crawlspace trails, grease marks, droppings, food/water source, and burrow activity."
  };

  document.getElementById("aiOut").value =
    advice[s] || "Track entry behavior, seasonality, recurrence, and secondary entry points.";
};

function render() {
  if (screen === "dashboard") dashboard();
  if (screen === "jobs") jobs();
  if (screen === "heatmap") heatmap();
  if (screen === "estimate") estimate();
  if (screen === "ai") ai();
}

loadJobs();

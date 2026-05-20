import { createClient } from "@supabase/supabase-js";

const SUPABASE_URL =
  import.meta.env.VITE_SUPABASE_URL ||
  "https://hgdzmwfcghtilyqagjak.supabase.co";

const SUPABASE_ANON_KEY =
  import.meta.env.VITE_SUPABASE_ANON_KEY ||
  "PASTE_YOUR_SUPABASE_PUBLISHABLE_KEY_HERE";

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

const app = document.getElementById("app");

const style = document.createElement("style");
style.textContent = `
:root{
  --bg:#05080d;
  --panel:#0d1520;
  --panel2:#111e2d;
  --panel3:#17283b;
  --text:#edf5ff;
  --muted:#93a7bd;
  --accent:#5ce083;
  --blue:#74b8ff;
  --yellow:#f4c15d;
  --red:#ff6b6b;
  --line:#26384d;
}

*{box-sizing:border-box}

body{
  margin:0;
  background:
    radial-gradient(circle at top left,#102033 0,#05080d 46%),
    linear-gradient(180deg,#07101a,#05080d);
  color:var(--text);
  font-family:system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;
}

button,input,select,textarea{font:inherit}

button{
  border:0;
  border-radius:18px;
  padding:15px 16px;
  background:var(--accent);
  color:#031007;
  font-weight:900;
  font-size:16px;
}

button.secondary{
  background:var(--panel3);
  color:var(--text);
  border:1px solid var(--line);
}

button.danger{
  background:var(--red);
  color:#260808;
}

input,select,textarea{
  width:100%;
  background:#09111c;
  border:1px solid var(--line);
  color:var(--text);
  border-radius:18px;
  padding:15px;
  font-size:16px;
  outline:none;
}

textarea{
  min-height:118px;
  resize:vertical;
}

header{
  position:sticky;
  top:0;
  z-index:1000;
  display:flex;
  justify-content:space-between;
  align-items:center;
  gap:12px;
  padding:14px 16px;
  border-bottom:1px solid var(--line);
  background:#05080dec;
  backdrop-filter:blur(14px);
}

.logoRow{
  display:flex;
  align-items:center;
  gap:12px;
}

.logoMark{
  width:46px;
  height:46px;
  border-radius:16px;
  background:
    radial-gradient(circle at 40% 40%,#5ce083 0,#1b5f32 30%,#0d1520 70%);
  border:1px solid var(--line);
  display:grid;
  place-items:center;
  font-size:24px;
}

h1{
  margin:0;
  font-size:19px;
  line-height:1.1;
}

.sub{
  color:var(--muted);
  font-size:13px;
  margin-top:3px;
  text-transform:capitalize;
}

.menuBtn{
  width:58px;
  height:58px;
  border-radius:20px;
  background:var(--panel2);
  border:1px solid var(--line);
  color:white;
  font-size:30px;
}

.drawer{
  position:fixed;
  top:82px;
  left:12px;
  right:12px;
  z-index:9999;
  background:#0d1520f8;
  border:1px solid var(--line);
  border-radius:24px;
  padding:12px;
  display:grid;
  gap:9px;
  box-shadow:0 24px 90px rgba(0,0,0,.55);
}

.drawer button{
  background:#17283b;
  color:white;
  text-align:left;
  border:1px solid var(--line);
}

main{
  max-width:960px;
  margin:auto;
  padding:14px;
  padding-bottom:94px;
}

.bottomNav{
  position:fixed;
  bottom:0;
  left:0;
  right:0;
  z-index:900;
  background:#05080df3;
  border-top:1px solid var(--line);
  display:grid;
  grid-template-columns:repeat(5,1fr);
  gap:4px;
  padding:8px;
}

.bottomNav button{
  border-radius:16px;
  padding:10px 6px;
  font-size:12px;
  background:transparent;
  color:var(--muted);
}

.bottomNav button.active{
  background:#17283b;
  color:white;
  border:1px solid var(--line);
}

.hero{
  background:
    linear-gradient(135deg,#101822,#0b1320),
    radial-gradient(circle at top right,#244769 0,transparent 35%);
  border:1px solid var(--line);
  border-radius:26px;
  padding:18px;
  margin-bottom:14px;
}

.hero h2{
  margin:0;
  font-size:24px;
}

.hero p{
  color:var(--muted);
  margin:8px 0 0;
}

.grid{
  display:grid;
  grid-template-columns:1fr 1fr;
  gap:12px;
}

.card{
  background:#0d1520e8;
  border:1px solid var(--line);
  border-radius:24px;
  padding:16px;
  margin-bottom:12px;
  box-shadow:0 10px 28px rgba(0,0,0,.22);
}

.metric h2{
  margin:0;
  font-size:34px;
}

.metric p{
  margin:6px 0 0;
  color:var(--muted);
  font-size:14px;
}

.sectionTitle{
  display:flex;
  justify-content:space-between;
  align-items:center;
  margin:22px 0 10px;
}

.sectionTitle h2{
  margin:0;
  font-size:22px;
}

.form{
  display:grid;
  gap:10px;
}

.row2{
  display:grid;
  grid-template-columns:1fr 1fr;
  gap:10px;
}

.tag{
  display:inline-block;
  background:#17283b;
  border:1px solid var(--line);
  color:var(--muted);
  border-radius:999px;
  padding:4px 10px;
  margin:3px 4px 3px 0;
  font-size:12px;
}

.jobCard h3{
  margin:0 0 8px;
  font-size:20px;
}

.jobCard p{
  color:var(--muted);
  margin:6px 0;
}

.aiBox{
  border-left:4px solid var(--accent);
  background:#0a121d;
}

.aiBox h3{
  margin:0 0 8px;
}

.aiText{
  white-space:pre-wrap;
  line-height:1.45;
  color:#dcecff;
}

.alert{
  background:#1d1720;
  border:1px solid #5b3544;
  color:#ffd9df;
}

.good{
  color:var(--accent);
}

.warn{
  color:var(--yellow);
}

.search{
  margin-bottom:12px;
}

@media(min-width:760px){
  main{padding:22px 22px 96px}
  .grid{grid-template-columns:repeat(4,1fr)}
  .row2{grid-template-columns:1fr 1fr}
}

@media(max-width:420px){
  .row2{grid-template-columns:1fr}
  .grid{grid-template-columns:1fr 1fr}
  h1{font-size:17px}
}
`;
document.head.appendChild(style);

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
let searchQuery = "";

function esc(value) {
  return String(value || "").replace(/[&<>"']/g, character => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[character]));
}

function money(value) {
  return "$" + Number(value || 0).toLocaleString();
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
  const payload = {
    customer_name: document.getElementById("customer").value.trim(),
    customer_phone: document.getElementById("phone").value.trim(),
    address: document.getElementById("address").value.trim(),
    town: document.getElementById("town").value.trim(),
    species: document.getElementById("speciesSelect").value,
    title:
      document.getElementById("title").value.trim() ||
      document.getElementById("speciesSelect").value + " job",
    scope: document.getElementById("notes").value.trim(),
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

window.updateSearch = function (value) {
  searchQuery = value;
  render();
};

window.closeJob = async function (jobId) {
  const { error } = await supabase
    .from("jobs")
    .update({
      status: "Closed",
      updated_at: new Date().toISOString()
    })
    .eq("id", jobId);

  if (error) {
    alert("Close error: " + error.message);
    return;
  }

  await loadJobs();
};

window.reopenJob = async function (jobId) {
  const { error } = await supabase
    .from("jobs")
    .update({
      status: "Active",
      updated_at: new Date().toISOString()
    })
    .eq("id", jobId);

  if (error) {
    alert("Reopen error: " + error.message);
    return;
  }

  await loadJobs();
};

function layout(content) {
  app.innerHTML = `
    <header>
      <div class="logoRow">
        <div class="logoMark">🦝</div>
        <div>
          <h1>Wildlife Whisperer FieldOps</h1>
          <div class="sub">${esc(screen)} ${isLoading ? "· syncing..." : ""}</div>
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
            <button onclick="go('create')">➕ Create Job</button>
            <button onclick="go('ai')">🧠 AI Field Assistant</button>
            <button onclick="go('estimate')">💵 Smart Estimator</button>
            <button onclick="go('heatmap')">🗺️ Heat Map</button>
          </div>
        `
        : ""
    }

    <main>${content}</main>

    <nav class="bottomNav">
      <button class="${screen === "dashboard" ? "active" : ""}" onclick="go('dashboard')">🏠<br>Home</button>
      <button class="${screen === "jobs" ? "active" : ""}" onclick="go('jobs')">🦝<br>Jobs</button>
      <button class="${screen === "create" ? "active" : ""}" onclick="go('create')">➕<br>New</button>
      <button class="${screen === "ai" ? "active" : ""}" onclick="go('ai')">🧠<br>AI</button>
      <button class="${screen === "estimate" ? "active" : ""}" onclick="go('estimate')">💵<br>Price</button>
    </nav>
  `;
}

function dashboard() {
  const active = db.jobs.filter(job => job.status === "Active");
  const closed = db.jobs.filter(job => job.status === "Closed");

  layout(`
    <section class="hero">
      <h2>Field Command Center</h2>
      <p>Jobs, estimates, species behavior, exclusion planning, and property history in one place.</p>
    </section>

    <section class="grid">
      <div class="card metric">
        <h2>${active.length}</h2>
        <p>Active jobs</p>
      </div>

      <div class="card metric">
        <h2>${db.jobs.length}</h2>
        <p>Total jobs</p>
      </div>

      <div class="card metric">
        <h2>${closed.length}</h2>
        <p>Closed</p>
      </div>

      <div class="card metric">
        <h2>${new Set(db.jobs.map(job => job.town || "Unknown")).size}</h2>
        <p>Service areas</p>
      </div>
    </section>

    <div class="sectionTitle">
      <h2>Quick Actions</h2>
    </div>

    <section class="grid">
      <button onclick="go('create')">➕ New Job</button>
      <button onclick="go('ai')" class="secondary">🧠 AI Plan</button>
      <button onclick="go('estimate')" class="secondary">💵 Estimate</button>
      <button onclick="go('heatmap')" class="secondary">🗺️ Heat Map</button>
    </section>

    <div class="sectionTitle">
      <h2>Recent Jobs</h2>
      <button class="secondary" onclick="go('jobs')">View All</button>
    </div>

    ${jobCards(db.jobs.slice(0, 5))}
  `);
}

function createJob() {
  layout(`
    <section class="hero">
      <h2>Create Field Job</h2>
      <p>Designed for exclusion-heavy, photo-heavy, multi-visit wildlife work.</p>
    </section>

    <section class="card form">
      <div class="row2">
        <input id="customer" placeholder="Customer name">
        <input id="phone" placeholder="Phone">
      </div>

      <input id="address" placeholder="Service address">

      <div class="row2">
        <input id="town" placeholder="Town / service area">

        <select id="speciesSelect">
          ${species.map(item => `<option>${item}</option>`).join("")}
        </select>
      </div>

      <input id="title" placeholder="Job title">

      <textarea id="notes" placeholder="Notes / scope: entry points, attic/crawlspace, exclusion repairs, warranty notes"></textarea>

      <button onclick="addJob()">Create Cloud Job</button>
    </section>
  `);
}

function filteredJobs() {
  const q = searchQuery.toLowerCase().trim();

  if (!q) {
    return db.jobs;
  }

  return db.jobs.filter(job => {
    const haystack = [
      job.title,
      job.customer_name,
      job.customer_phone,
      job.address,
      job.town,
      job.species,
      job.status,
      job.scope
    ].join(" ").toLowerCase();

    return haystack.includes(q);
  });
}

function jobCards(list) {
  if (!list.length) {
    return `<div class="card">No jobs yet.</div>`;
  }

  return list.map(job => `
    <div class="card jobCard">
      <h3>${esc(job.title || job.species + " job")}</h3>

      <p><strong>${esc(job.customer_name)}</strong> · <a href="tel:${esc(job.customer_phone)}">${esc(job.customer_phone)}</a></p>
      <p>${esc(job.address)}</p>

      <p>
        <span class="tag">${esc(job.species)}</span>
        <span class="tag">${esc(job.status)}</span>
        <span class="tag">${esc(job.town || "No town")}</span>
      </p>

      <p>${esc(job.scope || "No notes yet.")}</p>

      <div class="row2">
        ${
          job.status === "Closed"
            ? `<button onclick="reopenJob('${job.id}')">Reopen</button>`
            : `<button onclick="closeJob('${job.id}')" class="secondary">Close Job</button>`
        }

        <button onclick="openAIForJob('${job.id}')" class="secondary">AI Plan</button>
      </div>
    </div>
  `).join("");
}

window.openAIForJob = function (jobId) {
  const job = db.jobs.find(item => item.id === jobId);

  if (!job) {
    return;
  }

  screen = "ai";
  menuOpen = false;
  render();

  setTimeout(() => {
    const speciesInput = document.getElementById("aiSpecies");
    const notesInput = document.getElementById("aiNotes");

    if (speciesInput) {
      speciesInput.value = job.species || "Raccoon";
    }

    if (notesInput) {
      notesInput.value = `${job.title || ""}\n${job.address || ""}\n${job.scope || ""}`;
    }

    generateAIPlan();
  }, 50);
};

function jobs() {
  layout(`
    <section class="hero">
      <h2>Job Board</h2>
      <p>Search jobs, review active work, and generate AI field plans.</p>
    </section>

    <input
      class="search"
      placeholder="Search customer, address, species, town..."
      value="${esc(searchQuery)}"
      oninput="updateSearch(this.value)"
    >

    ${jobCards(filteredJobs())}
  `);
}

function heatmap() {
  const towns = {};

  db.jobs.forEach(job => {
    const town = job.town || "Unknown";
    towns[town] = (towns[town] || 0) + 1;
  });

  const cards = Object.entries(towns)
    .sort((a, b) => b[1] - a[1])
    .map(([town, count]) => `
      <div class="card">
        <h3>${esc(town)}</h3>
        <p>${count} job(s)</p>
        <span class="tag">${count >= 3 ? "Hot zone" : "Normal"}</span>
      </div>
    `)
    .join("");

  layout(`
    <section class="hero">
      <h2>Wildlife Heat Map</h2>
      <p>Town-based activity clustering for marketing, routing, and recurring issue tracking.</p>
    </section>

    ${cards || `<div class="card">No map data yet.</div>`}
  `);
}

function estimate() {
  layout(`
    <section class="hero">
      <h2>Smart Estimator</h2>
      <p>Pricing logic for exclusion-heavy wildlife jobs. Tune the numbers as your market data grows.</p>
    </section>

    <section class="card form">
      <select id="estSpecies">
        ${species.map(item => `<option>${item}</option>`).join("")}
      </select>

      <select id="severity">
        <option>Low</option>
        <option>Medium</option>
        <option>High</option>
        <option>Critical</option>
      </select>

      <div class="row2">
        <input id="linearFeet" type="number" placeholder="Repair linear feet">
        <input id="visits" type="number" value="3" placeholder="Visits">
      </div>

      <select id="warranty">
        <option value="0">No warranty</option>
        <option value="150">Basic warranty</option>
        <option value="300">Extended warranty</option>
      </select>

      <button onclick="calcEstimate()">Calculate Estimate</button>

      <textarea id="estimateOut" placeholder="Estimate output"></textarea>
    </section>
  `);
}

window.calcEstimate = function () {
  const selectedSpecies = document.getElementById("estSpecies").value;
  const selectedSeverity = document.getElementById("severity").value;
  const feet = Number(document.getElementById("linearFeet").value || 0);
  const visits = Number(document.getElementById("visits").value || 3);
  const warranty = Number(document.getElementById("warranty").value || 0);

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

  const base = basePrices[selectedSpecies] || 500;
  const severity = severityMultipliers[selectedSeverity] || 1;
  const repair = feet * 22;
  const visitCost = visits * 85;
  const total = Math.round(base * severity + repair + visitCost + warranty);

  document.getElementById("estimateOut").value =
`Recommended Estimate: ${money(total)}

Breakdown:
Base Species Rate: ${money(base)}
Severity Multiplier: ${selectedSeverity} x${severity}
Exclusion / Repair: ${feet} ft x $22 = ${money(repair)}
Visit Allowance: ${visits} visits x $85 = ${money(visitCost)}
Warranty Add-On: ${money(warranty)}

Suggested Positioning:
This is an exclusion-focused job, not just removal. Price should include inspection, entry point correction, behavior monitoring, and warranty boundaries.`;
};

function ai() {
  layout(`
    <section class="hero">
      <h2>AI Field Assistant</h2>
      <p>Rule-based field intelligence for nuisance wildlife jobs. Works without paid AI API keys.</p>
    </section>

    <section class="card form">
      <select id="aiSpecies">
        ${species.map(item => `<option>${item}</option>`).join("")}
      </select>

      <select id="aiStructure">
        <option>Attic</option>
        <option>Soffit / Fascia</option>
        <option>Crawlspace</option>
        <option>Basement</option>
        <option>Roofline</option>
        <option>Chimney</option>
        <option>Deck / Shed</option>
        <option>Exterior Only</option>
      </select>

      <select id="aiSeason">
        <option>Spring</option>
        <option>Summer</option>
        <option>Fall</option>
        <option>Winter</option>
      </select>

      <textarea id="aiNotes" placeholder="Observed signs: noises, droppings, chewing, tracks, entry holes, damage, customer concern"></textarea>

      <button onclick="generateAIPlan()">Generate Field Plan</button>
    </section>

    <section class="card aiBox">
      <h3>Recommended Plan</h3>
      <div id="aiOut" class="aiText">Pick species + structure, add notes, then generate.</div>
    </section>
  `);
}

window.generateAIPlan = function () {
  const selectedSpecies = document.getElementById("aiSpecies").value;
  const structure = document.getElementById("aiStructure").value;
  const season = document.getElementById("aiSeason").value;
  const notes = document.getElementById("aiNotes").value.trim();

  const speciesPlans = {
    Raccoon: {
      behavior: "Strong climber, roofline access common, can damage soffits, vents, chimneys, and attic insulation.",
      inspect: "Roof returns, soffits, chimney caps, attic latrine zones, insulation compression, greasy rub marks, large entry holes.",
      exclusion: "One-way door only if no dependent young are trapped inside. Reinforce entry with metal flashing, hardware cloth, or structural repair.",
      risk: "Possible latrine contamination, roundworm risk, aggressive female with young."
    },
    "Grey Squirrel": {
      behavior: "Daytime attic activity, chewing damage, common soffit/fascia/gable vent entry.",
      inspect: "Soffits, fascia corners, gable vents, ridge vent edges, attic nesting, chewed wiring risk.",
      exclusion: "Use positive set or one-way exclusion after confirming no juveniles are trapped. Seal secondary gaps.",
      risk: "Electrical/fire risk from chewing, repeat entry if weak trim remains."
    },
    "Red Squirrel": {
      behavior: "Aggressive chewer, territorial, can create repeat entry points and stash food.",
      inspect: "Fascia lines, roof edge, soffit returns, ridge gaps, cone/nut caches.",
      exclusion: "Trap or exclude, then overbuild vulnerable edges. Red squirrels often test repairs.",
      risk: "High recurrence if exclusion materials are weak."
    },
    "Flying Squirrel": {
      behavior: "Nocturnal, colony-prone, quiet but persistent, often uses small high gaps.",
      inspect: "Night activity, attic trails, wall voids, gable/soffit gaps, droppings in corners.",
      exclusion: "Seal all secondary gaps after eviction. One-way devices may need longer monitoring.",
      risk: "Colony behavior and multiple hidden entries."
    },
    Bat: {
      behavior: "Roosting species. Legal timing and maternity season matter.",
      inspect: "Guano, staining, urine marks, ridge caps, fascia gaps, chimney flashing, gable vents.",
      exclusion: "Use bat valves/cones only during legal exclusion window. Seal all non-primary gaps first.",
      risk: "Legal restrictions, rabies concern, guano cleanup, maternity colony risk."
    },
    Rat: {
      behavior: "Ground-level structural penetration, food/water source driven, high reproduction.",
      inspect: "Foundation gaps, crawlspace trails, burrows, grease marks, droppings, utility penetrations.",
      exclusion: "Seal gaps with rodent-proof materials. Pair exclusion with sanitation and population reduction.",
      risk: "Reinfestation if food/water source remains."
    },
    Mouse: {
      behavior: "Small-gap entry, often utility penetrations and garage/basement routes.",
      inspect: "Sill plate gaps, garage corners, basement, pipe penetrations, droppings along edges.",
      exclusion: "Seal quarter-inch gaps and larger. Use metal mesh/foam combo where appropriate.",
      risk: "Fast reinfestation if small gaps remain."
    },
    "Carpenter Bee": {
      behavior: "Bores into exposed wood, recurrence common without residual treatment and sealing.",
      inspect: "Fascia, soffits, decks, trim boards, exposed rafters, old galleries.",
      exclusion: "Treat galleries, plug holes after activity drops, consider painting/staining exposed wood.",
      risk: "Woodpecker damage after larvae develop."
    }
  };

  const plan = speciesPlans[selectedSpecies] || {
    behavior: "General nuisance wildlife behavior depends on food, shelter, entry opportunity, and season.",
    inspect: "Inspect entry points, tracks, droppings, nesting, damage, rub marks, and travel routes.",
    exclusion: "Remove or evict animal, seal primary and secondary entry points, document with photos.",
    risk: "Recurrence if entry points or attractants remain."
  };

  const seasonWarning =
    season === "Spring"
      ? "Spring: watch for dependent young before exclusion."
      : season === "Winter"
        ? "Winter: animals may be using structure as shelter; inspect warm voids and attic insulation."
        : season === "Fall"
          ? "Fall: overwintering pressure increases; reinforce weak points."
          : "Summer: heat can change movement patterns; check vents, shaded entry routes, and odor issues.";

  document.getElementById("aiOut").textContent =
`Species: ${selectedSpecies}
Structure: ${structure}
Season: ${season}

Behavior Read:
${plan.behavior}

Inspection Priority:
${plan.inspect}

Exclusion Strategy:
${plan.exclusion}

Risk Notes:
${plan.risk}

Seasonal Warning:
${seasonWarning}

Field Checklist:
1. Photograph every entry point before work.
2. Confirm whether young are present.
3. Identify primary entry and secondary weak points.
4. Choose trap, one-way exclusion, or direct removal strategy.
5. Repair with chew-resistant / weather-resistant material.
6. Document warranty boundaries clearly.
7. Schedule follow-up if activity continues.

Customer Explanation:
This job should be explained as animal removal plus structural exclusion. The goal is not only removing the current animal, but stopping the building from being re-entered.

Your Notes:
${notes || "No field notes entered."}`;
};

function render() {
  if (screen === "dashboard") dashboard();
  if (screen === "create") createJob();
  if (screen === "jobs") jobs();
  if (screen === "heatmap") heatmap();
  if (screen === "estimate") estimate();
  if (screen === "ai") ai();
}

loadJobs();

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

let db = JSON.parse(localStorage.getItem("ww_fieldops") || `{
  "jobs": [],
  "photos": [],
  "visits": [],
  "repairs": []
}`);

function save() {
  localStorage.setItem("ww_fieldops", JSON.stringify(db));
  render();
}

function id() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2);
}

function esc(v) {
  return String(v || "").replace(/[&<>"']/g, m => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[m]));
}

let screen = "dashboard";
let menuOpen = false;

function go(view) {
  screen = view;
  menuOpen = false;
  render();
}

window.go = go;

function addJob() {
  const job = {
    id: id(),
    customer: document.getElementById("customer").value,
    phone: document.getElementById("phone").value,
    address: document.getElementById("address").value,
    town: document.getElementById("town").value,
    species: document.getElementById("species").value,
    title: document.getElementById("title").value || document.getElementById("species").value + " job",
    notes: document.getElementById("notes").value,
    status: "Active",
    created: new Date().toLocaleString()
  };

  if (!job.customer || !job.address) {
    alert("Customer and address are required.");
    return;
  }

  db.jobs.unshift(job);
  save();
}

window.addJob = addJob;

function renderLayout(content) {
  app.innerHTML = `
    <header>
      <div>
        <h1>Wildlife Whisperer FieldOps</h1>
        <div class="sub">${screen}</div>
      </div>
      <button class="menuBtn" onclick="window.toggleMenu()">☰</button>
    </header>

    ${menuOpen ? `
      <div class="drawer">
        <button onclick="go('dashboard')">🏠 Dashboard</button>
        <button onclick="go('jobs')">🦝 Jobs</button>
        <button onclick="go('visits')">📝 Visits</button>
        <button onclick="go('repairs')">🔨 Repairs</button>
        <button onclick="go('photos')">📸 Photos</button>
        <button onclick="go('heatmap')">🗺️ Heat Map</button>
        <button onclick="go('estimate')">💵 Estimator</button>
        <button onclick="go('ai')">🧠 AI Assistant</button>
      </div>
    ` : ""}

    <main>${content}</main>
  `;
}

window.toggleMenu = function () {
  menuOpen = !menuOpen;
  render();
};

function dashboard() {
  renderLayout(`
    <section class="grid">
      <div class="card"><h2>${db.jobs.filter(j => j.status === "Active").length}</h2><p>Active jobs</p></div>
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

      <select id="species">
        ${species.map(s => `<option>${s}</option>`).join("")}
      </select>

      <input id="title" placeholder="Job title">
      <textarea id="notes" placeholder="Notes / scope"></textarea>

      <button onclick="addJob()">Create Job</button>
    </section>

    <h2>Recent Jobs</h2>
    ${jobCards(db.jobs.slice(0, 5))}
  `);
}

function jobCards(list) {
  if (!list.length) return `<div class="card">No jobs yet.</div>`;

  return list.map(j => `
    <div class="card">
      <h3>${esc(j.title)}</h3>
      <p>${esc(j.customer)} · ${esc(j.phone)}</p>
      <p>${esc(j.address)}</p>
      <p>
        <span>${esc(j.species)}</span>
        <span>${esc(j.status)}</span>
      </p>
      <button onclick="openJob('${j.id}')">Open</button>
    </div>
  `).join("");
}

window.openJob = function (jobId) {
  const j = db.jobs.find(x => x.id === jobId);

  renderLayout(`
    <section class="card">
      <h2>${esc(j.title)}</h2>
      <p>${esc(j.customer)} · <a href="tel:${esc(j.phone)}">${esc(j.phone)}</a></p>
      <p>${esc(j.address)}</p>
      <p>${esc(j.species)}</p>
      <textarea id="editNotes">${esc(j.notes)}</textarea>
      <button onclick="saveJobNotes('${j.id}')">Save Notes</button>
      <button onclick="go('jobs')">Back</button>
    </section>
  `);
};

window.saveJobNotes = function (jobId) {
  const j = db.jobs.find(x => x.id === jobId);
  j.notes = document.getElementById("editNotes").value;
  save();
};

function jobs() {
  renderLayout(`
    <h2>Jobs</h2>
    ${jobCards(db.jobs)}
  `);
}

function visits() {
  renderLayout(`<h2>Visits</h2><div class="card">Visit tracking comes next.</div>`);
}

function repairs() {
  renderLayout(`<h2>Repairs</h2><div class="card">Exclusion and structural repair tracker comes next.</div>`);
}

function photos() {
  renderLayout(`<h2>Photos</h2><div class="card">Photo upload comes next.</div>`);
}

function heatmap() {
  renderLayout(`
    <h2>Heat Map</h2>
    ${Object.entries(
      db.jobs.reduce((a, j) => {
        a[j.town || "Unknown"] = (a[j.town || "Unknown"] || 0) + 1;
        return a;
      }, {})
    ).map(([town, count]) => `
      <div class="card">
        <h3>${esc(town)}</h3>
        <p>${count} job(s)</p>
      </div>
    `).join("") || `<div class="card">No map data yet.</div>`}
  `);
}

function estimate() {
  renderLayout(`
    <h2>Estimator</h2>
    <div class="card form">
      <select id="estSpecies">${species.map(s => `<option>${s}</option>`).join("")}</select>
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

  document.getElementById("estimateOut").value = `Recommended estimate: $${Math.round(base * mult)}`;
};

function ai() {
  renderLayout(`
    <h2>AI Species Assistant</h2>
    <div class="card form">
      <select id="aiSpecies">${species.map(s => `<option>${s}</option>`).join("")}</select>
      <button onclick="speciesAdvice()">Suggest</button>
      <textarea id="aiOut"></textarea>
    </div>
  `);
}

window.speciesAdvice = function () {
  const s = document.getElementById("aiSpecies").value;

  const advice = {
    "Flying Squirrel": "Night activity, colony nesting, wall/attic movement. Inspect high gaps, vents, and soffit returns.",
    "Red Squirrel": "Aggressive chewing, cone caches, repeated soffit penetration. Check fascia, ridge gaps, and wiring risk.",
    "Grey Squirrel": "Larger attic nesting, roofline travel, soffit/fascia/gable vent entry.",
    Raccoon: "Inspect roof access, chimney, attic latrine areas, soffit damage, and insulation compression.",
    Bat: "Confirm legal exclusion window, guano accumulation, staining, roost points, and one-way device placement.",
    Rat: "Inspect foundation gaps, crawlspace trails, grease marks, droppings, food/water source, and burrow activity."
  };

  document.getElementById("aiOut").value = advice[s] || "Track entry behavior, seasonality, recurrence, and secondary entry points.";
};

function render() {
  if (screen === "dashboard") dashboard();
  if (screen === "jobs") jobs();
  if (screen === "visits") visits();
  if (screen === "repairs") repairs();
  if (screen === "photos") photos();
  if (screen === "heatmap") heatmap();
  if (screen === "estimate") estimate();
  if (screen === "ai") ai();
} 

import { createClient } from "@supabase/supabase-js";
import { Geolocation } from "@capacitor/geolocation";

const SUPABASE_URL = "https://hgdzmwfcghtilyqagjak.supabase.co";
const SUPABASE_KEY = "PASTE_YOUR_SUPABASE_PUBLISHABLE_KEY_HERE";

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

const SERVICES = [
  { name: "One-way set / one-way door", price: 225 },
  { name: "Bird gel", price: 125 },
  { name: "Sheet metal work", price: 35 },
  { name: "Caulking", price: 12 },
  { name: "Stainless steel mesh", price: 45 },
  { name: "Hardware cloth", price: 35 },
  { name: "Exclusion repair", price: 150 },
  { name: "Soffit / fascia repair", price: 225 },
  { name: "Ridge vent guard", price: 300 },
  { name: "Chimney cap", price: 350 },
  { name: "Gable vent screening", price: 175 },
  { name: "Foundation gap sealing", price: 95 },
  { name: "Cleanup / sanitation", price: 250 },
  { name: "Warranty follow-up", price: 125 }
];

const SPECIES = [
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
  "Mouse",
  "Rat",
  "Carpenter Bee",
  "Other"
];

const app = document.getElementById("app");
const menu = document.getElementById("menu");

let screen = "dashboard";
let jobs = [];
let techs = [];
let services = [];
let selectedJob = null;

function money(n) {
  return "$" + Number(n || 0).toLocaleString();
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

function go(page) {
  screen = page;
  menu.classList.remove("open");
  render();
}

window.go = go;

window.toggleMenu = function () {
  menu.classList.toggle("open");
};

async function loadData() {
  const [jobsRes, techsRes, servicesRes] = await Promise.all([
    supabase.from("jobs").select("*").order("created_at", { ascending: false }),
    supabase.from("techs").select("*").order("created_at", { ascending: false }),
    supabase.from("services").select("*")
  ]);

  jobs = jobsRes.data || [];
  techs = techsRes.data || [];
  services = servicesRes.data || [];

  render();
}

function nav() {
  menu.innerHTML = `
    <button onclick="go('dashboard')">🏠 Dashboard</button>
    <button onclick="go('jobs')">🦝 Jobs</button>
    <button onclick="go('new')">➕ New Job</button>
    <button onclick="go('estimate')">💵 Estimator</button>
    <button onclick="go('techs')">👷 Techs</button>
    <button onclick="go('ai')">🧠 AI Assistant</button>
  `;
}

function shell(content) {
  app.innerHTML = `
    <div class="top">
      <div>
        <strong>Wildlife Whisperer FieldOps</strong>
        <div class="tiny">${screen}</div>
      </div>
      <button class="action" style="width:auto;margin:0" onclick="toggleMenu()">☰</button>
    </div>
    <div class="wrap">${content}</div>
  `;
}

function dashboard() {
  const active = jobs.filter(j => j.status !== "Closed");
  const total = jobs.reduce((sum, j) => sum + Number(j.estimate || 0), 0);

  shell(`
    <div class="grid">
      <div class="card"><div class="stat">${active.length}</div><div class="tiny">Active jobs</div></div>
      <div class="card"><div class="stat">${jobs.length}</div><div class="tiny">Total jobs</div></div>
      <div class="card"><div class="stat">${techs.length}</div><div class="tiny">Techs</div></div>
      <div class="card"><div class="stat">${money(total)}</div><div class="tiny">Quoted value</div></div>
    </div>

    <button class="action" onclick="go('new')">➕ Create New Job</button>
    <button class="action" onclick="go('estimate')">💵 Smart Estimator</button>

    <h2 style="margin:18px 0 10px">Recent Jobs</h2>
    ${jobs.slice(0,5).map(jobCard).join("") || `<div class="card">No jobs yet.</div>`}
  `);
}

function jobCard(j) {
  return `
    <div class="card job">
      <h3>${esc(j.customer)}</h3>
      <div>${esc(j.address)}</div>
      <div class="tiny">${esc(j.species)} · ${esc(j.status || "Active")} · ${money(j.estimate)}</div>
      <span class="pill">${esc(j.town)}</span>
      <span class="pill">${esc(j.assigned_tech || "Unassigned")}</span>

      <button class="action" onclick="openJob('${j.id}')">Open Job</button>
      <a class="mapbtn" href="${directionsUrl(j)}" target="_blank">🚗 Google Maps Directions</a>
    </div>
  `;
}

function jobsPage() {
  shell(`
    <button class="action" onclick="go('new')">➕ New Job</button>
    ${jobs.map(jobCard).join("") || `<div class="card">No jobs yet.</div>`}
  `);
}

window.openJob = function (id) {
  selectedJob = jobs.find(j => j.id === id);
  screen = "detail";
  render();
};

function detailPage() {
  if (!selectedJob) {
    shell(`<div class="card">No job selected.</div>`);
    return;
  }

  const jobServices = services.filter(s => s.job_id === selectedJob.id);
  const totalServices = jobServices.reduce((sum, s) => sum + Number(s.total || 0), 0);

  shell(`
    <div class="card">
      <h2>${esc(selectedJob.customer)}</h2>
      <div>${esc(selectedJob.address)}</div>
      <div class="tiny">${esc(selectedJob.phone)} · ${esc(selectedJob.species)}</div>

      <a class="mapbtn" href="${mapUrl(selectedJob)}" target="_blank">📍 Open In Google Maps</a>
      <a class="mapbtn" href="${directionsUrl(selectedJob)}" target="_blank">🚗 Start Directions</a>
      <button class="action" onclick="saveGps('${selectedJob.id}')">📌 Save Current GPS To Job</button>
    </div>

    <div class="card">
      <h3>Add Service</h3>
      <select id="serviceName" onchange="servicePrice.value=this.selectedOptions[0].dataset.price">
        ${SERVICES.map(s => `<option data-price="${s.price}">${s.name}</option>`).join("")}
      </select>
      <input id="serviceQty" type="number" value="1" placeholder="Qty / feet / units">
      <input id="servicePrice" type="number" value="${SERVICES[0].price}" placeholder="Unit price">
      <button class="action" onclick="addService('${selectedJob.id}')">Save Service</button>
    </div>

    <div class="card">
      <h3>Services Total: ${money(totalServices)}</h3>
      ${jobServices.map(s => `
        <div class="service">
          <strong>${esc(s.service)}</strong><br>
          ${s.qty} × ${money(s.unit_price)} = ${money(s.total)}
        </div>
      `).join("") || `<div class="tiny">No services added yet.</div>`}
    </div>
  `);
}

function newJobPage() {
  shell(`
    <div class="card">
      <h2>New Job</h2>
      <input id="customer" placeholder="Customer name">
      <input id="phone" placeholder="Phone">
      <input id="address" placeholder="Address">
      <input id="town" placeholder="Town">
      <select id="species">${SPECIES.map(s => `<option>${s}</option>`).join("")}</select>
      <select id="assignedTech">
        <option value="">Unassigned</option>
        ${techs.map(t => `<option>${esc(t.name)}</option>`).join("")}
      </select>
      <textarea id="notes" placeholder="Notes / scope"></textarea>
      <button class="action" onclick="createJob()">Save Job</button>
    </div>
  `);
}

window.createJob = async function () {
  const payload = {
    customer: customer.value.trim(),
    phone: phone.value.trim(),
    address: address.value.trim(),
    town: town.value.trim(),
    species: species.value,
    status: "Active",
    assigned_tech: assignedTech.value,
    notes: notes.value.trim(),
    estimate: 0
  };

  if (!payload.customer || !payload.address) {
    alert("Customer and address required.");
    return;
  }

  const { error } = await supabase.from("jobs").insert(payload);

  if (error) {
    alert(error.message);
    return;
  }

  await loadData();
  go("jobs");
};

window.addService = async function (jobId) {
  const qty = Number(serviceQty.value || 1);
  const price = Number(servicePrice.value || 0);
  const total = qty * price;

  const { error } = await supabase.from("services").insert({
    job_id: jobId,
    service: serviceName.value,
    qty,
    unit_price: price,
    total
  });

  if (error) {
    alert(error.message);
    return;
  }

  await updateJobEstimate(jobId);
  await loadData();
  selectedJob = jobs.find(j => j.id === jobId) || selectedJob;
};

async function updateJobEstimate(jobId) {
  const { data } = await supabase.from("services").select("*").eq("job_id", jobId);
  const total = (data || []).reduce((sum, s) => sum + Number(s.total || 0), 0);
  await supabase.from("jobs").update({ estimate: total }).eq("id", jobId);
}

function estimatePage() {
  shell(`
    <div class="card">
      <h2>Smart Estimator</h2>
      <select id="estService" onchange="estPrice.value=this.selectedOptions[0].dataset.price; calcEstimate()">
        ${SERVICES.map(s => `<option data-price="${s.price}">${s.name}</option>`).join("")}
      </select>
      <input id="estQty" type="number" value="1" oninput="calcEstimate()" placeholder="Qty / feet / units">
      <input id="estPrice" type="number" value="${SERVICES[0].price}" oninput="calcEstimate()" placeholder="Unit price">
      <textarea id="estimateOut" readonly></textarea>
      <button class="action" onclick="calcEstimate()">Calculate</button>
      <button class="action" onclick="emailEstimate()">Open Gmail Estimate</button>
    </div>
  `);
  setTimeout(() => calcEstimate(), 50);
}

window.calcEstimate = function () {
  const total = Number(estQty.value || 0) * Number(estPrice.value || 0);
  estimateOut.value =
`Wildlife Whisperer LLC Estimate

Service: ${estService.value}
Quantity: ${estQty.value}
Unit Price: ${money(estPrice.value)}

Recommended Total: ${money(total)}

Scope includes professional nuisance wildlife inspection, exclusion work, repair materials, and service documentation.`;
};

window.emailEstimate = function () {
  if (!estimateOut.value) calcEstimate();
  const subject = encodeURIComponent("Wildlife Whisperer LLC Estimate");
  const body = encodeURIComponent(estimateOut.value);
  window.location.href = `mailto:?subject=${subject}&body=${body}`;
};

function techsPage() {
  shell(`
    <div class="card">
      <h2>Add Tech</h2>
      <input id="techName" placeholder="Name">
      <input id="techPhone" placeholder="Phone">
      <input id="techRole" placeholder="Role">
      <button class="action" onclick="addTech()">Save Tech</button>
    </div>

    ${techs.map(t => `
      <div class="card">
        <strong>${esc(t.name)}</strong>
        <div class="tiny">${esc(t.phone)} · ${esc(t.role)}</div>
      </div>
    `).join("")}
  `);
}

window.addTech = async function () {
  const { error } = await supabase.from("techs").insert({
    name: techName.value.trim(),
    phone: techPhone.value.trim(),
    role: techRole.value.trim()
  });

  if (error) {
    alert(error.message);
    return;
  }

  await loadData();
  go("techs");
};

function aiPage() {
  shell(`
    <div class="card">
      <h2>AI Field Assistant</h2>
      <select id="aiSpecies">${SPECIES.map(s => `<option>${s}</option>`).join("")}</select>
      <textarea id="aiObs" placeholder="Observed signs: noises, droppings, chewing, attic, soffit, roofline..."></textarea>
      <button class="action" onclick="aiSuggest()">Suggest Plan</button>
      <textarea id="aiOut" readonly></textarea>
    </div>
  `);
}

window.aiSuggest = function () {
  const s = aiSpecies.value;
  const o = aiObs.value.toLowerCase();

  let tips = [
    `Species: ${s}`,
    "Photograph entry points before work.",
    "Check secondary entry points before sealing.",
    "Document warranty boundaries clearly."
  ];

  if (s.includes("Squirrel")) tips.push("Inspect soffits, fascia, gable vents, roof returns, and chewing damage.");
  if (s === "Raccoon") tips.push("Inspect attic latrine areas, chimney, soffits, and roofline access.");
  if (s === "Bat") tips.push("Check guano, staining, legal exclusion timing, and roost gaps.");
  if (o.includes("soffit")) tips.push("Prioritize soffit/fascia repair and stainless mesh reinforcement.");
  if (o.includes("bird")) tips.push("Consider bird gel, mesh, and ledge exclusion.");
  if (o.includes("gap")) tips.push("Use caulking, sheet metal, or stainless steel mesh depending on gap size.");

  aiOut.value = tips.map(t => "• " + t).join("\n");
};

function mapUrl(j) {
  const q = j.latitude && j.longitude
    ? `${j.latitude},${j.longitude}`
    : j.address;

  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`;
}

function directionsUrl(j) {
  const q = j.latitude && j.longitude
    ? `${j.latitude},${j.longitude}`
    : j.address;

  return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(q)}&travelmode=driving`;
}

window.saveGps = async function (jobId) {
  try {
    const pos = await Geolocation.getCurrentPosition({
      enableHighAccuracy: true,
      timeout: 12000
    });

    const latitude = String(pos.coords.latitude);
    const longitude = String(pos.coords.longitude);

    const { error } = await supabase
      .from("jobs")
      .update({ latitude, longitude })
      .eq("id", jobId);

    if (error) {
      alert(error.message);
      return;
    }

    alert("GPS saved to job.");
    await loadData();
    selectedJob = jobs.find(j => j.id === jobId) || selectedJob;
  } catch (err) {
    alert("GPS error: " + err.message);
  }
};

function render() {
  nav();

  if (screen === "dashboard") dashboard();
  if (screen === "jobs") jobsPage();
  if (screen === "detail") detailPage();
  if (screen === "new") newJobPage();
  if (screen === "estimate") estimatePage();
  if (screen === "techs") techsPage();
  if (screen === "ai") aiPage();
}

loadData();

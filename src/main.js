import { supabase } from "./auth/supabaseClient.js";
import { Geolocation } from "@capacitor/geolocation";
import { jsPDF } from "jspdf";

/* ─── API KEYS ─── */
const GOOGLE_MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY";
const GOOGLE_CALENDAR_CLIENT_ID = "YOUR_GOOGLE_CALENDAR_CLIENT_ID";
const OPENWEATHER_API_KEY = "YOUR_OPENWEATHER_API_KEY";

/* ─── CONSTANTS ─── */
const SERVICES = [
  { name: "Inspection", price: 125 },
  { name: "Inspection photography", price: 75 },
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
  "Raccoon", "Grey Squirrel", "Red Squirrel", "Flying Squirrel", "Bat", "Skunk",
  "Groundhog", "Bird", "Snake", "Opossum", "Rodent", "Mouse", "Rat", "Carpenter Bee", "Other"
];

const SPECIES_ICONS = {
  "Raccoon": "🦝",
  "Grey Squirrel": "🐿️",
  "Red Squirrel": "🐿️",
  "Flying Squirrel": "🦇",
  "Bat": "🦇",
  "Skunk": "🦨",
  "Groundhog": "🦫",
  "Bird": "🐦",
  "Snake": "🐍",
  "Opossum": "🦡",
  "Rodent": "🐁",
  "Mouse": "🐁",
  "Rat": "🐀",
  "Carpenter Bee": "🐝",
  "Other": "🐾"
};

const STATUS_STYLES = {
  "Active": "active",
  "Scheduled": "scheduled",
  "Closed": "closed",
  "Trapping": "trapping",
  "Repair": "repair",
  "Waiting On Customer": "scheduled",
  "Exclusion": "active",
  "Warranty": "active"
};

/* ─── STATE ─── */
const app = document.getElementById("app");
const menuEl = document.getElementById("menu");
let screen = "dashboard";
let jobs = [];
let techs = [];
let services = [];
let inspections = [];
let photos = [];
let selectedJob = null;
let map = null;
let markers = [];
let gapiLoaded = false;
let gisLoaded = false;
let tokenClient = null;
let searchDebounce = null;

/* ─── UTILS ─── */
function money(n) { return "$" + Number(n || 0).toLocaleString(); }
function esc(v) {
  return String(v || "").replace(/[&<>"']/g, m => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" }[m]));
}
function go(page) {
  screen = page;
  menuEl.classList.remove("open");
  updateBottomNav(page);
  render();
}
window.go = go;
window.toggleMenu = () => menuEl.classList.toggle("open");

function debounce(fn, ms) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

/* ─── DATA ─── */
async function loadData() {
  try {
    const [j, t, s, i, p] = await Promise.all([
      supabase.from("jobs").select("*").order("created_at", { ascending: false }),
      supabase.from("techs").select("*").order("created_at", { ascending: false }),
      supabase.from("services").select("*"),
      supabase.from("inspections").select("*").order("created_at", { ascending: false }),
      supabase.from("photos").select("*").order("created_at", { ascending: false })
    ]);

    jobs = j.data || [];
    techs = t.data || [];
    services = s.data || [];
    inspections = i.data || [];
    photos = p.data || [];

    localStorage.setItem("ww_fieldops_cache", JSON.stringify({
      jobs, techs, services, inspections, photos,
      cachedAt: new Date().toISOString()
    }));
  } catch (err) {
    console.error("Sync failed, using cached data:", err);
    const cached = JSON.parse(localStorage.getItem("ww_fieldops_cache") || "{}");
    jobs = cached.jobs || [];
    techs = cached.techs || [];
    services = cached.services || [];
    inspections = cached.inspections || [];
    photos = cached.photos || [];
  }

  if (selectedJob) selectedJob = jobs.find(x => x.id === selectedJob.id) || selectedJob;
  render();
}

/* ─── GOOGLE MAPS ─── */
function loadGoogleMaps() {
  if (window.google?.maps || GOOGLE_MAPS_API_KEY.includes("YOUR_")) return;
  const script = document.createElement("script");
  script.src = `https://maps.googleapis.com/maps/api/js?key=${GOOGLE_MAPS_API_KEY}&libraries=places&callback=onGoogleMapsLoaded`;
  script.async = true;
  script.defer = true;
  document.head.appendChild(script);
}
window.onGoogleMapsLoaded = function () {
  if (screen === "dashboard" || screen === "detail") render();
};

function initMap(containerId) {
  if (!window.google?.maps || GOOGLE_MAPS_API_KEY.includes("YOUR_")) {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = '<div class="card tiny">Add Google Maps API key to enable interactive map.</div>';
    return null;
  }
  const defaultCenter = { lat: 40.7128, lng: -74.0060 };
  const m = new google.maps.Map(document.getElementById(containerId), {
    zoom: 12,
    center: defaultCenter,
    mapTypeId: "roadmap",
    styles: [
      { elementType: "geometry", stylers: [{ color: "#242f3e" }] },
      { elementType: "labels.text.stroke", stylers: [{ color: "#242f3e" }] },
      { elementType: "labels.text.fill", stylers: [{ color: "#746855" }] },
      { featureType: "administrative.locality", elementType: "labels.text.fill", stylers: [{ color: "#d59563" }] },
      { featureType: "poi", elementType: "labels.text.fill", stylers: [{ color: "#d59563" }] },
      { featureType: "poi.park", elementType: "geometry", stylers: [{ color: "#263c3f" }] },
      { featureType: "poi.park", elementType: "labels.text.fill", stylers: [{ color: "#6b9a76" }] },
      { featureType: "road", elementType: "geometry", stylers: [{ color: "#38414e" }] },
      { featureType: "road", elementType: "geometry.stroke", stylers: [{ color: "#212a37" }] },
      { featureType: "road", elementType: "labels.text.fill", stylers: [{ color: "#9ca5b3" }] },
      { featureType: "road.highway", elementType: "geometry", stylers: [{ color: "#746855" }] },
      { featureType: "road.highway", elementType: "geometry.stroke", stylers: [{ color: "#1f2835" }] },
      { featureType: "road.highway", elementType: "labels.text.fill", stylers: [{ color: "#f3d19c" }] },
      { featureType: "transit", elementType: "geometry", stylers: [{ color: "#2f3948" }] },
      { featureType: "transit.station", elementType: "labels.text.fill", stylers: [{ color: "#d59563" }] },
      { featureType: "water", elementType: "geometry", stylers: [{ color: "#17263c" }] },
      { featureType: "water", elementType: "labels.text.fill", stylers: [{ color: "#515c6d" }] },
      { featureType: "water", elementType: "labels.text.stroke", stylers: [{ color: "#17263c" }] }
    ]
  });
  return m;
}

function renderMap() {
  const container = document.getElementById("dashMap");
  if (!container || !window.google?.maps) return;

  const mappedJobs = jobs.filter(j => j.latitude && j.longitude);
  if (!mappedJobs.length) {
    container.innerHTML = '<div class="card tiny">No GPS jobs yet.</div>';
    return;
  }

  if (!map) map = initMap("dashMap");
  if (!map) return;

  markers.forEach(m => m.setMap(null));
  markers = [];

  const bounds = new google.maps.LatLngBounds();
  mappedJobs.forEach(job => {
    const pos = { lat: parseFloat(job.latitude), lng: parseFloat(job.longitude) };
    const marker = new google.maps.Marker({
      position: pos,
      map,
      title: `${esc(job.species)} - ${esc(job.customer)}`,
      animation: google.maps.Animation.DROP
    });
    marker.addListener("click", () => {
      selectedJob = job;
      screen = "detail";
      updateBottomNav("detail");
      render();
    });
    markers.push(marker);
    bounds.extend(pos);
  });

  if (markers.length > 1) map.fitBounds(bounds);
  else if (markers.length === 1) { map.setCenter(markers[0].getPosition()); map.setZoom(15); }
}

function navigateToJob(lat, lng, address) {
  if (!lat || !lng) {
    if (address) {
      window.open(`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(address)}&travelmode=driving`, "_blank");
      return;
    }
    return alert("No GPS data for this job.");
  }
  window.open(`https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}&travelmode=driving`, "_blank");
}
window.navigateToJob = navigateToJob;

/* ─── GOOGLE CALENDAR ─── */
function loadGoogleCalendarAPI() {
  if (gapiLoaded || GOOGLE_CALENDAR_CLIENT_ID.includes("YOUR_")) return;

  const gapiScript = document.createElement("script");
  gapiScript.src = "https://apis.google.com/js/api.js";
  gapiScript.onload = () => {
    window.gapi.load("client", async () => {
      await window.gapi.client.init({
        apiKey: "YOUR_GOOGLE_API_KEY",
        discoveryDocs: ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"]
      });
      gapiLoaded = true;
    });
  };
  document.head.appendChild(gapiScript);

  const gisScript = document.createElement("script");
  gisScript.src = "https://accounts.google.com/gsi/client";
  gisScript.onload = () => {
    tokenClient = google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CALENDAR_CLIENT_ID,
      scope: "https://www.googleapis.com/auth/calendar.events",
      callback: () => {}
    });
    gisLoaded = true;
  };
  document.head.appendChild(gisScript);
}

window.createCalendarEvent = async function (job) {
  if (!gapiLoaded || !gisLoaded || GOOGLE_CALENDAR_CLIENT_ID.includes("YOUR_")) {
    alert("Google Calendar not configured. Add your Client ID and API Key.");
    return;
  }

  return new Promise((resolve) => {
    tokenClient.callback = async (resp) => {
      if (resp.error) { alert("Auth error: " + resp.error); resolve(); return; }
      const event = {
        summary: `Wildlife Job: ${job.customer} - ${job.species}`,
        description: `Customer: ${job.customer}\nPhone: ${job.phone}\nAddress: ${job.address}\nSpecies: ${job.species}\nScope: ${job.notes || ""}`,
        start: { dateTime: new Date().toISOString(), timeZone: "America/New_York" },
        end: { dateTime: new Date(Date.now() + 3600000).toISOString(), timeZone: "America/New_York" },
        location: job.address
      };
      try {
        const response = await window.gapi.client.calendar.events.insert({ calendarId: "primary", resource: event });
        alert(`Event created: ${response.result.htmlLink}`);
      } catch (err) {
        alert("Failed to create event: " + err.message);
      }
      resolve();
    };
    tokenClient.requestAccessToken({ prompt: "consent" });
  });
};

/* ─── WEATHER ─── */
async function getWeather(lat, lng) {
  if (OPENWEATHER_API_KEY.includes("YOUR_")) return null;
  try {
    const url = `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lng}&appid=${OPENWEATHER_API_KEY}&units=imperial`;
    const res = await fetch(url);
    if (!res.ok) return null;
    const data = await res.json();
    return {
      temp: Math.round(data.main.temp),
      condition: data.weather[0].main,
      description: data.weather[0].description,
      icon: `https://openweathermap.org/img/wn/${data.weather[0].icon}@2x.png`
    };
  } catch (e) { console.error("Weather error:", e); return null; }
}

/* ─── VOICE COMMANDS ─── */
window.dictate = function (el) {
  const R = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!R) return alert("Speech recognition not supported. Try Chrome.");
  const r = new R();
  r.lang = "en-US";
  r.continuous = true;
  r.interimResults = true;
  r.onresult = e => {
    const transcript = e.results[e.results.length - 1][0].transcript;
    el.value += (el.value ? " " : "") + transcript;
    // Command parsing
    const lower = transcript.toLowerCase();
    if (lower.includes("add job") || lower.includes("new job")) {
      const speciesMatch = transcript.match(/for (a |an )?(\w+(?:\s+\w+)?)/i);
      const addressMatch = transcript.match(/at (.+?)(?:\s+in\s+(.+))?$/i);
      if (speciesMatch && addressMatch && typeof customer !== "undefined") {
        const sp = speciesMatch[2].trim();
        const addr = addressMatch[1].trim();
        const tn = addressMatch[2] ? addressMatch[2].trim() : "";
        if (SPECIES.some(s => s.toLowerCase() === sp.toLowerCase())) {
          if (document.getElementById("species")) document.getElementById("species").value = sp;
        }
        if (document.getElementById("address")) document.getElementById("address").value = addr;
        if (document.getElementById("town") && tn) document.getElementById("town").value = tn;
        alert(`Detected: ${sp} at ${addr}${tn ? ", " + tn : ""}. Fill customer name and tap Save Job.`);
      }
    }
  };
  r.start();
};

/* ─── LAZY LOADING ─── */
function lazyLoadImages() {
  const imgs = document.querySelectorAll("img.lazy");
  if (!imgs.length) return;
  const obs = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const img = entry.target;
        img.src = img.dataset.src;
        img.classList.add("loaded");
        img.classList.remove("lazy");
        obs.unobserve(img);
      }
    });
  });
  imgs.forEach(img => obs.observe(img));
}

/* ─── NAV / SHELL ─── */
function nav() {
  menuEl.innerHTML = `
    <div style="font-weight:700;font-size:18px;margin-bottom:18px;padding-left:4px;">FieldOps</div>
    <button onclick="go('dashboard')">🏠 Dashboard</button>
    <button onclick="go('jobs')">🦝 Jobs</button>
    <button onclick="go('new')">➕ New Job</button>
    <button onclick="go('estimate')">💵 Estimator</button>
    <button onclick="go('techs')">👷 Techs</button>
    <button onclick="go('ai')">🧠 AI Assistant</button>
    <button onclick="exportData()">💾 Export JSON</button>
    <button onclick="importDataPrompt()">📥 Import JSON</button>
  `;
}

function updateBottomNav(page) {
  const map = { dashboard: 0, jobs: 1, detail: 2, new: 2, estimate: 3, ai: 3 };
  const idx = map[page] ?? -1;
  document.querySelectorAll(".bottom-nav button").forEach((b, i) => {
    b.classList.toggle("active", i === idx);
  });
}

function shell(content) {
  const hasMap = screen === "dashboard";
  app.innerHTML = `
    <div class="top">
      <div>
        <strong style="font-size:16px;">Wildlife Whisperer FieldOps</strong>
        <div class="tiny" style="margin-top:2px;">${esc(screen)}</div>
      </div>
      <div class="sync-indicator">
        ${hasMap ? '<span class="sync-dot"></span><span>Live</span>' : ""}
        <button class="menuButton" onclick="toggleMenu()" style="margin-left:8px;">☰</button>
      </div>
    </div>
    <div class="wrap">${content}</div>
    <div class="bottom-nav">
      <button onclick="go('dashboard')" title="Dashboard">🏠</button>
      <button onclick="go('jobs')" title="Jobs">🦝</button>
      <button onclick="go('new')" title="New Job">➕</button>
      <button onclick="go('estimate')" title="Estimate">💵</button>
      <button onclick="go('ai')" title="AI">🧠</button>
    </div>
    <button class="fab" onclick="go('new')" title="New Job">+</button>
  `;
  setTimeout(() => {
    updateBottomNav(screen);
    if (screen === "dashboard") renderMap();
    lazyLoadImages();
  }, 50);
}

/* ─── SCORING ─── */
function jobScore(job) {
  const v = inspections.some(i => i.job_id === job.id);
  const p = photos.some(p => p.job_id === job.id);
  const sv = services.filter(s => s.job_id === job.id);
  const s = sv.length > 0;
  return Math.min(100, (v ? 25 : 0) + (p ? 25 : 0) + (s ? 25 : 0) + (job.latitude ? 25 : 0));
}

/* ─── PAGES ─── */
function dashboard() {
  const active = jobs.filter(j => j.status !== "Closed");
  const total = jobs.reduce((sum, j) => sum + Number(j.grand_total || j.estimate || 0), 0);

  shell(`
    <div class="grid">
      <div class="card"><div class="stat">${active.length}</div><div class="tiny">Active jobs</div></div>
      <div class="card"><div class="stat">${jobs.length}</div><div class="tiny">Total jobs</div></div>
      <div class="card"><div class="stat">${techs.length}</div><div class="tiny">Techs</div></div>
      <div class="card"><div class="stat">${money(total)}</div><div class="tiny">Quoted value</div></div>
    </div>

    <div id="dashMap" style="height:320px;width:100%;border-radius:16px;margin-bottom:18px;border:1px solid var(--border);background:var(--card);"></div>

    <button class="action" onclick="go('new')">➕ Create New Job</button>
    <button class="action dark" onclick="go('estimate')">💵 Smart Estimator</button>

    <h2 style="margin:18px 0 10px">Recent Jobs</h2>
    ${jobs.slice(0, 5).map(jobCard).join("") || `<div class="card">No jobs yet.</div>`}
  `);
}

function jobCard(j) {
  const icon = SPECIES_ICONS[j.species] || "🐾";
  const statusClass = STATUS_STYLES[j.status] || "active";
  const s = jobScore(j);
  const jobServices = services.filter(s => s.job_id === j.id);
  const totalServices = jobServices.reduce((sum, s) => sum + Number(s.total || 0), 0);

  return `
    <div class="card job">
      <div class="job-header">
        <span class="species-icon">${icon}</span>
        <h3 style="margin:0;flex:1;">${esc(j.customer)}</h3>
        <span class="status-pill ${statusClass}">${esc(j.status || "Active")}</span>
      </div>
      <div>${esc(j.address)}</div>
      <div class="tiny">${esc(j.species)} · ${esc(j.town || "")} · ${money(j.grand_total || j.estimate)}</div>
      <div style="margin-top:6px;">
        <span class="pill">${esc(j.town || "Unsorted")}</span>
        <span class="pill muted">${esc(j.assigned_tech || "Unassigned")}</span>
        <span class="pill info">${jobServices.length} services</span>
        ${j.latitude ? '<span class="pill">📍 GPS</span>' : ""}
      </div>
      <div class="prog"><div class="bar" style="width:${s}%"></div></div>
      <div class="tiny">Score ${s}% · Est ${money(j.estimate || 0)} · Services ${money(totalServices)}</div>
      <div class="job-actions">
        <button class="primary" onclick="openJob('${j.id}')">Open</button>
        <button class="secondary" onclick="navigateToJob(${j.latitude || "null"}, ${j.longitude || "null"}, '${esc(j.address)}')">Navigate</button>
      </div>
    </div>
  `;
}

function jobsPage() {
  shell(`
    <div class="search-box">
      <input id="searchInput" placeholder="🔍 Search jobs, customers, addresses, species..." oninput="onSearchInput(this.value)">
    </div>
    <button class="action" onclick="go('new')">➕ New Job</button>
    <div id="jobList">${jobs.map(jobCard).join("") || `<div class="card">No jobs yet.</div>`}</div>
  `);
}

window.onSearchInput = debounce(function (q) {
  const list = document.getElementById("jobList");
  if (!list) return;
  const term = q.toLowerCase();
  const filtered = jobs.filter(j =>
    (j.customer + j.address + j.town + j.species + j.status + (j.notes || "")).toLowerCase().includes(term)
  );
  list.innerHTML = filtered.map(jobCard).join("") || `<div class="card">No matching jobs.</div>`;
  lazyLoadImages();
}, 250);

window.openJob = function (id) {
  selectedJob = jobs.find(j => j.id === id);
  screen = "detail";
  updateBottomNav("detail");
  render();
};

function detailPage() {
  if (!selectedJob) { shell(`<div class="card">No job selected.</div>`); return; }

  const jobServices = services.filter(s => s.job_id === selectedJob.id);
  const jobInspections = inspections.filter(i => i.job_id === selectedJob.id);
  const jobPhotos = photos.filter(p => p.job_id === selectedJob.id);
  const totalServices = jobServices.reduce((sum, s) => sum + Number(s.total || 0), 0);
  const icon = SPECIES_ICONS[selectedJob.species] || "🐾";
  const statusClass = STATUS_STYLES[selectedJob.status] || "active";
  const s = jobScore(selectedJob);

  shell(`
    <div class="card stack">
      <div class="job-header">
        <span class="species-icon">${icon}</span>
        <h2 style="margin:0;flex:1;">${esc(selectedJob.customer)}</h2>
        <span class="status-pill ${statusClass}">${esc(selectedJob.status || "Active")}</span>
      </div>
      <div>${esc(selectedJob.address)}${selectedJob.town ? ", " + esc(selectedJob.town) : ""}</div>
      <div class="tiny">${esc(selectedJob.phone)} · ${esc(selectedJob.species)} · ${esc(selectedJob.email || "")}</div>
      <div class="tiny">Estimate: ${money(selectedJob.estimate)} · Tax: ${money(selectedJob.tax_amount)} · Total: ${money(selectedJob.grand_total)}</div>
      <div class="prog"><div class="bar" style="width:${s}%"></div></div>
      <div class="tiny">Job Score ${s}%</div>

      <div class="job-actions" style="margin-top:14px;">
        <button class="primary" onclick="saveGps('${selectedJob.id}')">📌 Save GPS</button>
        <button class="secondary" onclick="navigateToJob(${selectedJob.latitude || "null"}, ${selectedJob.longitude || "null"}, '${esc(selectedJob.address)}')">🚗 Navigate</button>
      </div>
      <button class="action dark" style="margin-top:8px;" onclick="createCalendarEvent(${JSON.stringify(selectedJob).replace(/"/g, '&quot;')})">📅 Add to Google Calendar</button>
      <button class="action dark" style="margin-top:8px;" onclick="generateJobPDF()">📄 Download Job PDF</button>
    </div>

    <div id="weatherBox"></div>

    <div id="detailMap" style="height:280px;width:100%;border-radius:16px;margin-top:12px;border:1px solid var(--border);background:var(--card);"></div>

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
      <h3>Tax</h3>
      <input id="jobTaxRate" type="number" value="${selectedJob.tax_rate || 0}" placeholder="Tax rate %">
      <button class="action" onclick="applyTax('${selectedJob.id}')">Apply Tax To Estimate</button>
    </div>

    <div class="card">
      <h3>Inspection Notes</h3>
      <select id="inspectionType">
        <option>Initial inspection</option>
        <option>Roofline inspection</option>
        <option>Attic inspection</option>
        <option>Crawlspace inspection</option>
        <option>Exterior inspection</option>
        <option>Warranty inspection</option>
      </select>
      <textarea id="inspectionNotes" placeholder="Inspection notes"></textarea>
      <button class="action" onclick="saveInspection('${selectedJob.id}')">Save Inspection Notes</button>
      <button class="action dark" style="margin-top:8px;" onclick="dictate(inspectionNotes)">🎙️ Dictate Notes</button>
    </div>

    <div class="card">
      <h3>Inspection Photography</h3>
      <input id="photoFile" type="file" accept="image/*">
      <select id="photoTag">
        <option>Inspection photo</option>
        <option>Entry point</option>
        <option>Damage</option>
        <option>Droppings / evidence</option>
        <option>Repair before</option>
        <option>Repair after</option>
      </select>
      <textarea id="photoNotes" placeholder="Photo notes"></textarea>
      <button class="action" onclick="saveInspectionPhoto('${selectedJob.id}')">Save Inspection Photo</button>
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

    <div class="card">
      <h3>Inspection History</h3>
      ${jobInspections.map(i => `
        <div class="service">
          <strong>${esc(i.inspection_type)}</strong>
          <div class="tiny">${esc(i.created_at)}</div>
          <div>${esc(i.notes)}</div>
        </div>
      `).join("") || `<div class="tiny">No inspections yet.</div>`}
    </div>

    <div class="card">
      <h3>Photos</h3>
      ${jobPhotos.map(p => `
        <div class="service">
          <strong>${esc(p.tag)}</strong>
          <div class="tiny">${esc(p.notes)}</div>
          ${p.image_url ? `<img class="photo lazy" data-src="${p.image_url}" alt="${esc(p.tag)}">` : ""}
        </div>
      `).join("") || `<div class="tiny">No photos yet.</div>`}
    </div>
  `);

  setTimeout(async () => {
    if (selectedJob.latitude && selectedJob.longitude) {
      if (window.google?.maps) {
        const dmap = initMap("detailMap");
        if (dmap) {
          const pos = { lat: parseFloat(selectedJob.latitude), lng: parseFloat(selectedJob.longitude) };
          dmap.setCenter(pos);
          dmap.setZoom(16);
          new google.maps.Marker({ position: pos, map: dmap, title: esc(selectedJob.customer) });
        }
      }

      const weather = await getWeather(selectedJob.latitude, selectedJob.longitude);
      const wbox = document.getElementById("weatherBox");
      if (wbox && weather) {
        wbox.innerHTML = `
          <div class="card">
            <div class="weather-card">
              <img src="${weather.icon}" alt="${esc(weather.description)}" style="width:48px;height:48px;">
              <div>
                <div style="font-weight:700;font-size:18px;">${weather.temp}°F</div>
                <div class="tiny">${esc(weather.condition)} · ${esc(weather.description)}</div>
              </div>
            </div>
          </div>
        `;
      }
    }
  }, 100);
}

function newJobPage() {
  shell(`
    <div class="card">
      <h2>New Job</h2>
      <input id="customer" placeholder="Customer name">
      <input id="phone" placeholder="Phone">
      <input id="email" placeholder="Email">
      <input id="address" placeholder="Address">
      <input id="town" placeholder="Town">
      <select id="species">${SPECIES.map(s => `<option>${s}</option>`).join("")}</select>
      <select id="assignedTech">
        <option value="">Unassigned</option>
        ${techs.map(t => `<option>${esc(t.name)}</option>`).join("")}
      </select>
      <textarea id="notes" placeholder="Notes / scope"></textarea>
      <button class="action" onclick="createJob()">Save Job</button>
      <button class="action dark" onclick="dictate(notes)">🎙️ Dictate Notes</button>
    </div>
  `);
}

window.createJob = async function () {
  const payload = {
    customer: customer.value.trim(),
    phone: phone.value.trim(),
    email: email.value.trim(),
    address: address.value.trim(),
    town: town.value.trim(),
    species: species.value,
    status: "Active",
    assigned_tech: assignedTech.value,
    notes: notes.value.trim(),
    estimate: 0,
    tax_rate: 0,
    tax_amount: 0,
    grand_total: 0
  };

  if (!payload.customer || !payload.address) { alert("Customer and address required."); return; }

  const { error } = await supabase.from("jobs").insert(payload);
  if (error) { alert(error.message); return; }

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

  if (error) { alert(error.message); return; }

  await updateJobEstimate(jobId);
  await loadData();
};

async function updateJobEstimate(jobId) {
  const { data } = await supabase.from("services").select("*").eq("job_id", jobId);
  const subtotal = (data || []).reduce((sum, s) => sum + Number(s.total || 0), 0);
  const job = jobs.find(j => j.id === jobId);
  const taxRate = Number(job?.tax_rate || 0);
  const taxAmount = +(subtotal * (taxRate / 100)).toFixed(2);
  const grandTotal = +(subtotal + taxAmount).toFixed(2);

  await supabase.from("jobs").update({
    estimate: subtotal,
    tax_amount: taxAmount,
    grand_total: grandTotal
  }).eq("id", jobId);
}

window.applyTax = async function (jobId) {
  const taxRate = Number(jobTaxRate.value || 0);
  const job = jobs.find(j => j.id === jobId);
  const subtotal = Number(job?.estimate || 0);
  const taxAmount = +(subtotal * (taxRate / 100)).toFixed(2);
  const grandTotal = +(subtotal + taxAmount).toFixed(2);

  const { error } = await supabase.from("jobs").update({
    tax_rate: taxRate,
    tax_amount: taxAmount,
    grand_total: grandTotal
  }).eq("id", jobId);

  if (error) { alert(error.message); return; }
  await loadData();
};

window.saveInspection = async function (jobId) {
  const { error } = await supabase.from("inspections").insert({
    job_id: jobId,
    inspection_type: inspectionType.value,
    notes: inspectionNotes.value
  });

  if (error) { alert(error.message); return; }
  await loadData();
};

window.saveInspectionPhoto = async function (jobId) {
  const file = photoFile.files[0];
  if (!file) { alert("Choose a photo."); return; }

  const reader = new FileReader();
  reader.onload = async () => {
    const { error } = await supabase.from("photos").insert({
      job_id: jobId,
      image_url: reader.result,
      tag: photoTag.value,
      notes: photoNotes.value
    });

    if (error) { alert(error.message); return; }
    await loadData();
  };
  reader.readAsDataURL(file);
};

function estimatePage() {
  shell(`
    <div class="card">
      <h2>Smart Estimator</h2>

      <select id="estService" onchange="estPrice.value=this.selectedOptions[0].dataset.price; calcEstimate()">
        ${SERVICES.map(s => `<option data-price="${s.price}">${s.name}</option>`).join("")}
      </select>

      <input id="estQty" type="number" value="1" oninput="calcEstimate()" placeholder="Qty / feet / units">
      <input id="estPrice" type="number" value="${SERVICES[0].price}" oninput="calcEstimate()" placeholder="Unit price">
      <input id="estTax" type="number" value="0" oninput="calcEstimate()" placeholder="Tax rate %">

      <textarea id="estimateOut" readonly></textarea>

      <button class="action" onclick="calcEstimate()">Calculate</button>
      <button class="action dark" onclick="emailEstimate()">Open Gmail Estimate</button>
      <button class="action" onclick="generateEstimatePDF()">📄 Download PDF</button>
    </div>
  `);
  setTimeout(() => calcEstimate(), 50);
}

window.calcEstimate = function () {
  const subtotal = Number(estQty.value || 0) * Number(estPrice.value || 0);
  const taxRate = Number(estTax.value || 0);
  const taxAmount = +(subtotal * (taxRate / 100)).toFixed(2);
  const total = +(subtotal + taxAmount).toFixed(2);

  estimateOut.value =
    `Wildlife Whisperer LLC Estimate\n\n` +
    `Service: ${estService.value}\n` +
    `Quantity: ${estQty.value}\n` +
    `Unit Price: ${money(estPrice.value)}\n\n` +
    `Subtotal: ${money(subtotal)}\n` +
    `Tax Rate: ${taxRate}%\n` +
    `Tax Amount: ${money(taxAmount)}\n\n` +
    `Recommended Total: ${money(total)}\n\n` +
    `Scope includes professional nuisance wildlife inspection, inspection photography when selected, exclusion work, repair materials, service documentation, and warranty boundaries.`;
};

window.emailEstimate = function () {
  if (!estimateOut.value) calcEstimate();
  const subject = encodeURIComponent("Wildlife Whisperer LLC Estimate");
  const body = encodeURIComponent(estimateOut.value);
  window.location.href = `mailto:?subject=${subject}&body=${body}`;
};

window.generateEstimatePDF = function () {
  if (!estimateOut.value) calcEstimate();
  const doc = new jsPDF();
  doc.setFontSize(18);
  doc.text("Wildlife Whisperer LLC", 20, 20);
  doc.setFontSize(14);
  doc.text("Estimate", 20, 30);
  doc.setFontSize(11);
  const lines = doc.splitTextToSize(estimateOut.value, 170);
  doc.text(lines, 20, 45);
  doc.save("wildlife-estimate.pdf");
};

window.generateJobPDF = function () {
  if (!selectedJob) return alert("No job selected.");
  const doc = new jsPDF();
  doc.setFontSize(18);
  doc.text("Wildlife Whisperer LLC", 20, 20);
  doc.setFontSize(14);
  doc.text("Job Report", 20, 30);
  doc.setFontSize(11);

  const jobServices = services.filter(s => s.job_id === selectedJob.id);
  const totalServices = jobServices.reduce((sum, s) => sum + Number(s.total || 0), 0);

  let y = 45;
  const addLine = (label, value) => {
    doc.setFont(undefined, "bold");
    doc.text(label + ":", 20, y);
    doc.setFont(undefined, "normal");
    const lines = doc.splitTextToSize(String(value || "N/A"), 120);
    doc.text(lines, 70, y);
    y += 6 * lines.length;
    if (y > 270) { doc.addPage(); y = 20; }
  };

  addLine("Customer", selectedJob.customer);
  addLine("Address", selectedJob.address);
  addLine("Phone", selectedJob.phone);
  addLine("Species", selectedJob.species);
  addLine("Status", selectedJob.status);
  addLine("Town", selectedJob.town);
  addLine("Assigned Tech", selectedJob.assigned_tech);
  addLine("Estimate", money(selectedJob.estimate));
  addLine("Tax", money(selectedJob.tax_amount));
  addLine("Total", money(selectedJob.grand_total));
  addLine("Services Total", money(totalServices));
  addLine("Notes", selectedJob.notes);

  doc.save(`job-${selectedJob.customer.replace(/[^a-z0-9]/gi, "_")}.pdf`);
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

  if (error) { alert(error.message); return; }

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
      <button class="action dark" onclick="dictate(aiObs)">🎙️ Dictate</button>
      <textarea id="aiOut" readonly></textarea>
    </div>
  `);
}

window.aiSuggest = function () {
  const s = aiSpecies.value;
  const o = aiObs.value.toLowerCase();

  let tips = [
    `Species: ${s}`,
    "Photograph all inspection findings.",
    "Write inspection notes before pricing.",
    "Check secondary entry points before sealing.",
    "Document warranty boundaries clearly."
  ];

  if (s.includes("Squirrel")) tips.push("Inspect soffits, fascia, gable vents, roof returns, chewing damage, and stainless mesh needs.");
  if (s === "Raccoon") tips.push("Inspect attic latrine areas, chimney, soffits, and roofline access.");
  if (s === "Bat") tips.push("Check guano, staining, legal exclusion timing, and roost gaps.");
  if (o.includes("soffit")) tips.push("Prioritize soffit/fascia repair and stainless mesh reinforcement.");
  if (o.includes("bird")) tips.push("Consider bird gel, mesh, ledge exclusion, and inspection photography.");
  if (o.includes("gap")) tips.push("Use caulking, sheet metal, or stainless steel mesh depending on gap size.");

  aiOut.value = tips.map(t => "• " + t).join("\n");
};

window.saveGps = async function (jobId) {
  try {
    const pos = await Geolocation.getCurrentPosition({ enableHighAccuracy: true, timeout: 12000 });
    const latitude = String(pos.coords.latitude);
    const longitude = String(pos.coords.longitude);

    const { error } = await supabase.from("jobs").update({ latitude, longitude }).eq("id", jobId);
    if (error) { alert(error.message); return; }

    alert("GPS saved to job.");
    await loadData();
  } catch (err) {
    alert("GPS error: " + err.message);
  }
};

/* ─── EXPORT / IMPORT ─── */
window.exportData = function () {
  const data = { jobs, techs, services, inspections, photos, exportedAt: new Date().toISOString() };
  const a = document.createElement("a");
  a.href = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }));
  a.download = `wildlife-fieldops-backup-${new Date().toISOString().slice(0,10)}.json`;
  a.click();
};

window.importDataPrompt = function () {
  const raw = prompt("Paste JSON backup data:");
  if (!raw) return;
  importData(raw);
};

window.importData = async function (raw) {
  try {
    const data = JSON.parse(raw);
    if (!data.jobs) { alert("Invalid format: missing jobs array."); return; }

    // Merge strategy: upsert all tables
    if (data.jobs?.length) {
      const { error } = await supabase.from("jobs").upsert(data.jobs);
      if (error) console.error("Jobs import error:", error);
    }
    if (data.techs?.length) {
      const { error } = await supabase.from("techs").upsert(data.techs);
      if (error) console.error("Techs import error:", error);
    }
    if (data.services?.length) {
      const { error } = await supabase.from("services").upsert(data.services);
      if (error) console.error("Services import error:", error);
    }
    if (data.inspections?.length) {
      const { error } = await supabase.from("inspections").upsert(data.inspections);
      if (error) console.error("Inspections import error:", error);
    }
    if (data.photos?.length) {
      const { error } = await supabase.from("photos").upsert(data.photos);
      if (error) console.error("Photos import error:", error);
    }

    alert("Import complete. Reloading data...");
    await loadData();
  } catch (e) {
    alert("Import failed: " + e.message);
  }
};

/* ─── RENDER ─── */
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

/* ─── INIT ─── */
loadGoogleMaps();
loadGoogleCalendarAPI();
loadData();

/* ─── SERVICE WORKER UPDATE ─── */
if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("sw.js").then(reg => {
    reg.addEventListener("updatefound", () => {
      const newWorker = reg.installing;
      newWorker.addEventListener("statechange", () => {
        if (newWorker.state === "installed" && navigator.serviceWorker.controller) {
          if (confirm("App update available. Reload now?")) location.reload();
        }
      });
    });
  }).catch(() => {});
}

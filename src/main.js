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

*{
  box-sizing:border-box;
}

body{
  margin:0;
  background:
    radial-gradient(circle at top left,#102033 0,#05080d 46%),
    linear-gradient(180deg,#07101a,#05080d);
  color:var(--text);
  font-family:system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;
}

button,
input,
select,
textarea{
  font:inherit;
}

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

input,
select,
textarea{
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
  padding-bottom:96px;
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
  gap:10px;
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

.search{
  margin-bottom:12px;
}

.profileCard{
  border-left:4px solid var(--blue);
}

.good{
  color:var(--accent);
}

.warn{
  color:var(--yellow);
}

a{
  color:var(--blue);
}

.photoThumb{
  width:90px;
  height:90px;
  object-fit:cover;
  border-radius:14px;
  border:1px solid var(--line);
}

@media(min-width:760px){
  main{
    padding:22px 22px 96px;
  }

  .grid{
    grid-template-columns:repeat(4,1fr);
  }
}

@media(max-width:420px){
  .row2{
    grid-template-columns:1fr;
  }

  .grid{
    grid-template-columns:1fr 1fr;
  }

  h1{
    font-size:17px;
  }
}
`;

document.head.appendChild(style);

const speciesList = [
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

let state = {
  session: null,
  user: null,
  profile: null,
  profiles: [],
  jobs: [],
  properties: [],
  visits: [],
  repairs: [],
  warranties: [],
  estimates: [],
  photos: [],
  aiPlans: [],
  screen: "dashboard",
  menuOpen: false,
  loading: false,
  search: "",
  activeJobId: null
};

function esc(value){
  return String(value || "").replace(/[&<>"']/g, character => ({
    "&":"&amp;",
    "<":"&lt;",
    ">":"&gt;",
    '"':"&quot;",
    "'":"&#039;"
  }[character]));
}

function money(value){
  return "$" + Number(value || 0).toLocaleString();
}

function setLoading(value){
  state.loading = value;
  render();
}

function getJob(jobId){
  return state.jobs.find(job => job.id === jobId);
}

function getProfile(profileId){
  return state.profiles.find(profile => profile.id === profileId);
}

function go(screen){
  state.screen = screen;
  state.menuOpen = false;
  render();
}

window.go = go;

window.toggleMenu = function(){
  state.menuOpen = !state.menuOpen;
  render();
};

async function init(){
  const { data } = await supabase.auth.getSession();

  state.session = data.session;
  state.user = data.session?.user || null;

  if(!state.user){
    renderLogin();
    return;
  }

  await ensureProfile();
  await loadAll();
}

async function ensureProfile(){
  const { data,error } = await supabase
    .from("profiles")
    .select("*")
    .eq("id",state.user.id)
    .maybeSingle();

  if(error){
    alert("Profile load error: " + error.message);
    return;
  }

  if(data){
    state.profile = data;
    return;
  }

  const { data:created,error:createError } = await supabase
    .from("profiles")
    .insert({
      id:state.user.id,
      email:state.user.email,
      name:state.user.email,
      role:"owner",
      active:true
    })
    .select()
    .single();

  if(createError){
    alert("Profile create error: " + createError.message);
    return;
  }

  state.profile = created;
}

async function loadAll(){
  setLoading(true);

  const [
    profilesResult,
    propertiesResult,
    jobsResult,
    visitsResult,
    repairsResult,
    warrantiesResult,
    estimatesResult,
    photosResult,
    aiResult
  ] = await Promise.all([
    supabase.from("profiles").select("*").order("created_at",{ ascending:false }),
    supabase.from("properties").select("*").order("created_at",{ ascending:false }),
    supabase.from("jobs").select("*").order("created_at",{ ascending:false }),
    supabase.from("visits").select("*").order("created_at",{ ascending:false }),
    supabase.from("repairs").select("*").order("created_at",{ ascending:false }),
    supabase.from("warranties").select("*").order("created_at",{ ascending:false }),
    supabase.from("estimates").select("*").order("created_at",{ ascending:false }),
    supabase.from("job_photos").select("*").order("created_at",{ ascending:false }),
    supabase.from("ai_plans").select("*").order("created_at",{ ascending:false })
  ]);

  setLoading(false);

  const possibleError =
    profilesResult.error ||
    propertiesResult.error ||
    jobsResult.error ||
    visitsResult.error ||
    repairsResult.error ||
    warrantiesResult.error ||
    estimatesResult.error ||
    photosResult.error ||
    aiResult.error;

  if(possibleError){
    alert("Supabase load error: " + possibleError.message);
    return;
  }

  state.profiles = profilesResult.data || [];
  state.properties = propertiesResult.data || [];
  state.jobs = jobsResult.data || [];
  state.visits = visitsResult.data || [];
  state.repairs = repairsResult.data || [];
  state.warranties = warrantiesResult.data || [];
  state.estimates = estimatesResult.data || [];
  state.photos = photosResult.data || [];
  state.aiPlans = aiResult.data || [];

  render();
}

function renderLogin(){
  app.innerHTML = `
    <main>
      <section class="hero">
        <h2>Wildlife Whisperer FieldOps</h2>
        <p>Login to sync jobs, technicians, estimates, photos, repairs, and AI field plans.</p>
      </section>

      <section class="card form">
        <input id="loginEmail" placeholder="Email">
        <input id="loginPassword" type="password" placeholder="Password">

        <button onclick="login()">Login</button>
        <button class="secondary" onclick="signup()">Create Account</button>
      </section>
    </main>
  `;
}

window.login = async function(){
  const email = document.getElementById("loginEmail").value.trim();
  const password = document.getElementById("loginPassword").value;

  const { error } = await supabase.auth.signInWithPassword({
    email,
    password
  });

  if(error){
    alert("Login error: " + error.message);
    return;
  }

  await init();
};

window.signup = async function(){
  const email = document.getElementById("loginEmail").value.trim();
  const password = document.getElementById("loginPassword").value;

  const { error } = await supabase.auth.signUp({
    email,
    password
  });

  if(error){
    alert("Signup error: " + error.message);
    return;
  }

  alert("Account created. If Supabase email confirmation is enabled, confirm the email before logging in.");
};

window.logout = async function(){
  await supabase.auth.signOut();
  state.session = null;
  state.user = null;
  state.profile = null;
  renderLogin();
};

function layout(content){
  app.innerHTML = `
    <header>
      <div class="logoRow">
        <div class="logoMark">🦝</div>
        <div>
          <h1>Wildlife Whisperer FieldOps</h1>
          <div class="sub">
            ${esc(state.screen)}
            ${state.loading ? "· syncing..." : ""}
            ${state.profile ? "· " + esc(state.profile.role) : ""}
          </div>
        </div>
      </div>

      <button class="menuBtn" onclick="toggleMenu()">☰</button>
    </header>

    ${
      state.menuOpen
      ?
      `
      <div class="drawer">
        <button onclick="go('dashboard')">🏠 Dashboard</button>
        <button onclick="go('jobs')">🦝 Jobs</button>
        <button onclick="go('create')">➕ Create Job</button>
        <button onclick="go('techs')">👷 Techs</button>
        <button onclick="go('estimate')">💵 Estimates</button>
        <button onclick="go('ai')">🧠 AI Assistant</button>
        <button onclick="go('heatmap')">🗺️ Heat Map</button>
        <button onclick="go('photos')">📸 Photos</button>
        <button onclick="go('settings')">⚙️ Settings</button>
      </div>
      `
      :
      ""
    }

    <main>${content}</main>

    <nav class="bottomNav">
      <button class="${state.screen === "dashboard" ? "active" : ""}" onclick="go('dashboard')">🏠<br>Home</button>
      <button class="${state.screen === "jobs" ? "active" : ""}" onclick="go('jobs')">🦝<br>Jobs</button>
      <button class="${state.screen === "create" ? "active" : ""}" onclick="go('create')">➕<br>New</button>
      <button class="${state.screen === "techs" ? "active" : ""}" onclick="go('techs')">👷<br>Techs</button>
      <button class="${state.screen === "estimate" ? "active" : ""}" onclick="go('estimate')">💵<br>Price</button>
    </nav>
  `;
}

function dashboard(){
  const activeJobs = state.jobs.filter(job => job.status !== "Closed");
  const openRepairs = state.repairs.filter(repair => repair.status !== "Complete" && repair.status !== "Closed");
  const activeTechs = state.profiles.filter(profile => profile.active);

  layout(`
    <section class="hero">
      <h2>Field Command Center</h2>
      <p>Cloud jobs, tech assignment, estimates, repairs, warranties, photos, and AI field plans powered by Supabase.</p>
    </section>

    <section class="grid">
      <div class="card metric">
        <h2>${activeJobs.length}</h2>
        <p>Active jobs</p>
      </div>

      <div class="card metric">
        <h2>${activeTechs.length}</h2>
        <p>Active techs</p>
      </div>

      <div class="card metric">
        <h2>${openRepairs.length}</h2>
        <p>Open repairs</p>
      </div>

      <div class="card metric">
        <h2>${state.estimates.length}</h2>
        <p>Estimates</p>
      </div>
    </section>

    <div class="sectionTitle">
      <h2>Quick Actions</h2>
    </div>

    <section class="grid">
      <button onclick="go('create')">➕ New Job</button>
      <button class="secondary" onclick="go('techs')">👷 Techs</button>
      <button class="secondary" onclick="go('estimate')">💵 Estimate</button>
      <button class="secondary" onclick="go('ai')">🧠 AI Plan</button>
    </section>

    <div class="sectionTitle">
      <h2>Recent Jobs</h2>
      <button class="secondary" onclick="go('jobs')">View All</button>
    </div>

    ${jobCards(state.jobs.slice(0,5))}
  `);
}

function createJob(){
  layout(`
    <section class="hero">
      <h2>Create Field Job</h2>
      <p>Save customer, property, species, tech assignment, and scope directly to Supabase.</p>
    </section>

    <section class="card form">
      <div class="row2">
        <input id="customer" placeholder="Customer name">
        <input id="phone" placeholder="Phone">
      </div>

      <input id="email" placeholder="Customer email">
      <input id="address" placeholder="Service address">

      <div class="row2">
        <input id="town" placeholder="Town / service area">

        <select id="speciesSelect">
          ${speciesList.map(item => `<option>${item}</option>`).join("")}
        </select>
      </div>

      <div class="row2">
        <select id="assignedTo">
          <option value="">Unassigned</option>
          ${state.profiles.map(profile => `
            <option value="${profile.id}">
              ${esc(profile.name || profile.email)} — ${esc(profile.role)}
            </option>
          `).join("")}
        </select>

        <select id="priority">
          <option>Normal</option>
          <option>High</option>
          <option>Emergency</option>
        </select>
      </div>

      <input id="title" placeholder="Job title">

      <textarea id="notes" placeholder="Scope: entry points, attic/crawlspace, exclusion repairs, warranty notes"></textarea>

      <button onclick="addJob()">Create Cloud Job</button>
    </section>
  `);
}

window.addJob = async function(){
  const customer = document.getElementById("customer").value.trim();
  const phone = document.getElementById("phone").value.trim();
  const email = document.getElementById("email").value.trim();
  const address = document.getElementById("address").value.trim();
  const town = document.getElementById("town").value.trim();
  const selectedSpecies = document.getElementById("speciesSelect").value;
  const assignedTo = document.getElementById("assignedTo").value || null;
  const priority = document.getElementById("priority").value;
  const title = document.getElementById("title").value.trim() || selectedSpecies + " job";
  const scope = document.getElementById("notes").value.trim();

  if(!customer || !address){
    alert("Customer and address required.");
    return;
  }

  const { data:property,error:propertyError } = await supabase
    .from("properties")
    .insert({
      address,
      town,
      created_by:state.user.id
    })
    .select()
    .single();

  if(propertyError){
    alert("Property save error: " + propertyError.message);
    return;
  }

  const { error:jobError } = await supabase
    .from("jobs")
    .insert({
      property_id:property.id,
      customer_name:customer,
      customer_phone:phone,
      customer_email:email,
      address,
      town,
      species:selectedSpecies,
      title,
      scope,
      priority,
      assigned_to:assignedTo,
      created_by:state.user.id,
      status:"Active"
    });

  if(jobError){
    alert("Job save error: " + jobError.message);
    return;
  }

  await loadAll();
  go("jobs");
};

function filteredJobs(){
  const query = state.search.toLowerCase().trim();

  if(!query){
    return state.jobs;
  }

  return state.jobs.filter(job => {
    const haystack = [
      job.title,
      job.customer_name,
      job.customer_phone,
      job.customer_email,
      job.address,
      job.town,
      job.species,
      job.status,
      job.priority,
      job.scope,
      getProfile(job.assigned_to)?.name,
      getProfile(job.assigned_to)?.email
    ].join(" ").toLowerCase();

    return haystack.includes(query);
  });
}

window.updateSearch = function(value){
  state.search = value;
  render();
};

function jobCards(jobs){
  if(!jobs.length){
    return `<div class="card">No jobs yet.</div>`;
  }

  return jobs.map(job => {
    const tech = getProfile(job.assigned_to);

    return `
      <div class="card jobCard">
        <h3>${esc(job.title || job.species + " job")}</h3>

        <p>
          <strong>${esc(job.customer_name)}</strong>
          · <a href="tel:${esc(job.customer_phone)}">${esc(job.customer_phone || "No phone")}</a>
        </p>

        <p>${esc(job.address)}</p>

        <p>
          <span class="tag">${esc(job.species)}</span>
          <span class="tag">${esc(job.status)}</span>
          <span class="tag">${esc(job.priority || "Normal")}</span>
          <span class="tag">${esc(job.town || "No town")}</span>
        </p>

        <p>
          Assigned:
          <span class="tag">${esc(tech?.name || tech?.email || "Unassigned")}</span>
        </p>

        <p>${esc(job.scope || "No notes yet.")}</p>

        <div class="row2">
          <button onclick="openJob('${job.id}')">Open Job</button>
          <button class="secondary" onclick="openAIForJob('${job.id}')">AI Plan</button>
        </div>
      </div>
    `;
  }).join("");
}

function jobs(){
  layout(`
    <section class="hero">
      <h2>Job Board</h2>
      <p>Search, assign, review, estimate, and document jobs.</p>
    </section>

    <input
      class="search"
      placeholder="Search customer, address, species, tech, town..."
      value="${esc(state.search)}"
      oninput="updateSearch(this.value)"
    >

    ${jobCards(filteredJobs())}
  `);
}

window.openJob = function(jobId){
  state.activeJobId = jobId;
  state.screen = "jobDetail";
  state.menuOpen = false;
  render();
};

function jobDetail(){
  const job = getJob(state.activeJobId);

  if(!job){
    go("jobs");
    return;
  }

  const tech = getProfile(job.assigned_to);
  const visits = state.visits.filter(item => item.job_id === job.id);
  const repairs = state.repairs.filter(item => item.job_id === job.id);
  const warranties = state.warranties.filter(item => item.job_id === job.id);
  const photos = state.photos.filter(item => item.job_id === job.id);
  const estimates = state.estimates.filter(item => item.job_id === job.id);
  const aiPlans = state.aiPlans.filter(item => item.job_id === job.id);

  layout(`
    <section class="hero">
      <h2>${esc(job.title)}</h2>
      <p>${esc(job.customer_name)} · ${esc(job.species)} · ${esc(job.status)}</p>
    </section>

    <section class="card">
      <p><strong>Phone:</strong> <a href="tel:${esc(job.customer_phone)}">${esc(job.customer_phone || "No phone")}</a></p>
      <p><strong>Email:</strong> ${esc(job.customer_email || "No email")}</p>
      <p><strong>Address:</strong> ${esc(job.address)}</p>
      <p><strong>Assigned:</strong> ${esc(tech?.name || tech?.email || "Unassigned")}</p>
      <p>${esc(job.scope || "No scope notes.")}</p>

      <div class="row2">
        <button class="secondary" onclick="closeJob('${job.id}')">Close Job</button>
        <button onclick="openAIForJob('${job.id}')">AI Plan</button>
      </div>
    </section>

    <section class="card form">
      <h3>Add Visit</h3>
      <select id="visitType">
        <option>Inspection</option>
        <option>Trap Set</option>
        <option>Trap Check</option>
        <option>Exclusion</option>
        <option>Repair</option>
        <option>Warranty Follow-Up</option>
      </select>
      <input id="animalsRemoved" type="number" placeholder="Animals removed">
      <textarea id="visitNotes" placeholder="Visit notes"></textarea>
      <button onclick="addVisit('${job.id}')">Save Visit</button>
    </section>

    <section class="card form">
      <h3>Add Exclusion / Repair</h3>
      <input id="repairLocation" placeholder="Location: left rear soffit, gable vent, chimney...">
      <select id="repairStatus">
        <option>Open</option>
        <option>Needs Repair</option>
        <option>Complete</option>
        <option>Warranty Covered</option>
      </select>
      <select id="repairSeverity">
        <option>Low</option>
        <option>Medium</option>
        <option>High</option>
        <option>Critical</option>
      </select>
      <input id="repairMaterials" placeholder="Materials">
      <textarea id="repairNotes" placeholder="Repair notes"></textarea>
      <button onclick="addRepair('${job.id}')">Save Repair</button>
    </section>

    <section class="card form">
      <h3>Upload Photo</h3>
      <input id="photoFile" type="file" accept="image/*" capture="environment">
      <select id="photoTag">
        <option>Before</option>
        <option>Entry Point</option>
        <option>Damage</option>
        <option>Trap Placement</option>
        <option>Droppings / Evidence</option>
        <option>After</option>
      </select>
      <textarea id="photoNotes" placeholder="Photo notes"></textarea>
      <button onclick="uploadJobPhoto('${job.id}')">Upload Photo</button>
    </section>

    <div class="sectionTitle"><h2>Visits</h2></div>
    ${visits.map(visit => `
      <div class="card">
        <h3>${esc(visit.visit_type)}</h3>
        <p>${esc(visit.notes)}</p>
        <span class="tag">Animals: ${esc(visit.animals_removed)}</span>
      </div>
    `).join("") || `<div class="card">No visits yet.</div>`}

    <div class="sectionTitle"><h2>Repairs</h2></div>
    ${repairs.map(repair => `
      <div class="card">
        <h3>${esc(repair.location)}</h3>
        <span class="tag">${esc(repair.status)}</span>
        <span class="tag">${esc(repair.severity)}</span>
        <p>${esc(repair.materials)}</p>
        <p>${esc(repair.notes)}</p>
      </div>
    `).join("") || `<div class="card">No repairs yet.</div>`}

    <div class="sectionTitle"><h2>Photos</h2></div>
    ${photos.map(photo => `
      <div class="card">
        ${photo.public_url ? `<img class="photoThumb" src="${photo.public_url}">` : ""}
        <h3>${esc(photo.tag)}</h3>
        <p>${esc(photo.notes)}</p>
      </div>
    `).join("") || `<div class="card">No photos yet.</div>`}

    <div class="sectionTitle"><h2>Estimates</h2></div>
    ${estimates.map(estimate => `
      <div class="card">
        <h3>${money(estimate.total)}</h3>
        <p>${esc(estimate.customer_email)}</p>
        <p>${esc(estimate.body)}</p>
      </div>
    `).join("") || `<div class="card">No estimates yet.</div>`}

    <div class="sectionTitle"><h2>AI Plans</h2></div>
    ${aiPlans.map(plan => `
      <div class="card aiBox">
        <h3>${esc(plan.species)} · ${esc(plan.structure_area)}</h3>
        <div class="aiText">${esc(plan.plan)}</div>
      </div>
    `).join("") || `<div class="card">No AI plans yet.</div>`}
  `);
}

window.closeJob = async function(jobId){
  const { error } = await supabase
    .from("jobs")
    .update({
      status:"Closed",
      updated_at:new Date().toISOString()
    })
    .eq("id",jobId);

  if(error){
    alert("Close job error: " + error.message);
    return;
  }

  await loadAll();
};

window.addVisit = async function(jobId){
  const { error } = await supabase
    .from("visits")
    .insert({
      job_id:jobId,
      technician_id:state.user.id,
      visit_type:document.getElementById("visitType").value,
      animals_removed:Number(document.getElementById("animalsRemoved").value || 0),
      notes:document.getElementById("visitNotes").value
    });

  if(error){
    alert("Visit save error: " + error.message);
    return;
  }

  await loadAll();
};

window.addRepair = async function(jobId){
  const { error } = await supabase
    .from("repairs")
    .insert({
      job_id:jobId,
      location:document.getElementById("repairLocation").value,
      status:document.getElementById("repairStatus").value,
      severity:document.getElementById("repairSeverity").value,
      materials:document.getElementById("repairMaterials").value,
      notes:document.getElementById("repairNotes").value
    });

  if(error){
    alert("Repair save error: " + error.message);
    return;
  }

  await loadAll();
};

window.uploadJobPhoto = async function(jobId){
  const file = document.getElementById("photoFile").files[0];

  if(!file){
    alert("Choose a photo first.");
    return;
  }

  const path = `${jobId}/${Date.now()}-${file.name}`;

  const { error:uploadError } = await supabase.storage
    .from("job-photos")
    .upload(path,file);

  if(uploadError){
    alert("Photo upload error: " + uploadError.message);
    return;
  }

  const { data:urlData } = supabase.storage
    .from("job-photos")
    .getPublicUrl(path);

  const { error:insertError } = await supabase
    .from("job_photos")
    .insert({
      job_id:jobId,
      path,
      public_url:urlData.publicUrl,
      tag:document.getElementById("photoTag").value,
      notes:document.getElementById("photoNotes").value,
      uploaded_by:state.user.id
    });

  if(insertError){
    alert("Photo record error: " + insertError.message);
    return;
  }

  await loadAll();
};

function techs(){
  layout(`
    <section class="hero">
      <h2>Technicians</h2>
      <p>Every tech signs up with email/password. Manage roles, names, and active status here.</p>
    </section>

    ${state.profiles.map(profile => `
      <div class="card profileCard form">
        <h3>${esc(profile.email)}</h3>

        <input id="techName-${profile.id}" value="${esc(profile.name || "")}" placeholder="Name">
        <input id="techPhone-${profile.id}" value="${esc(profile.phone || "")}" placeholder="Phone">

        <select id="techRole-${profile.id}">
          <option ${profile.role === "owner" ? "selected" : ""}>owner</option>
          <option ${profile.role === "admin" ? "selected" : ""}>admin</option>
          <option ${profile.role === "technician" ? "selected" : ""}>technician</option>
        </select>

        <select id="techActive-${profile.id}">
          <option value="true" ${profile.active ? "selected" : ""}>active</option>
          <option value="false" ${!profile.active ? "selected" : ""}>inactive</option>
        </select>

        <button onclick="updateTech('${profile.id}')">Save Tech</button>
      </div>
    `).join("")}
  `);
}

window.updateTech = async function(profileId){
  const { error } = await supabase
    .from("profiles")
    .update({
      name:document.getElementById(`techName-${profileId}`).value,
      phone:document.getElementById(`techPhone-${profileId}`).value,
      role:document.getElementById(`techRole-${profileId}`).value,
      active:document.getElementById(`techActive-${profileId}`).value === "true"
    })
    .eq("id",profileId);

  if(error){
    alert("Tech update error: " + error.message);
    return;
  }

  await loadAll();
};

function estimate(){
  layout(`
    <section class="hero">
      <h2>Smart Estimator</h2>
      <p>Create an estimate, save it to Supabase, and open Gmail with the customer copy ready.</p>
    </section>

    <section class="card form">
      <select id="estimateJob">
        <option value="">No linked job</option>
        ${state.jobs.map(job => `
          <option value="${job.id}">
            ${esc(job.title)} — ${esc(job.customer_name)}
          </option>
        `).join("")}
      </select>

      <input id="estCustomer" placeholder="Customer name">
      <input id="estEmail" placeholder="Customer email">
      <input id="estAddress" placeholder="Service address">

      <select id="estSpecies">
        ${speciesList.map(item => `<option>${item}</option>`).join("")}
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

      <select id="warrantyAdd">
        <option value="0">No warranty</option>
        <option value="150">Basic warranty</option>
        <option value="300">Extended warranty</option>
      </select>

      <textarea id="estimateNotes" placeholder="Scope notes"></textarea>

      <button onclick="calcEstimate()">Calculate</button>
      <button class="secondary" onclick="saveEstimate()">Save Estimate</button>
      <button class="secondary" onclick="sendEstimateEmail()">Open Gmail Estimate</button>

      <textarea id="estimateOut" placeholder="Estimate output"></textarea>
    </section>
  `);
}

window.calcEstimate = function(){
  const linkedJob = getJob(document.getElementById("estimateJob").value);

  if(linkedJob){
    document.getElementById("estCustomer").value ||= linkedJob.customer_name || "";
    document.getElementById("estEmail").value ||= linkedJob.customer_email || "";
    document.getElementById("estAddress").value ||= linkedJob.address || "";
    document.getElementById("estSpecies").value = linkedJob.species || "Raccoon";
  }

  const selectedSpecies = document.getElementById("estSpecies").value;
  const selectedSeverity = document.getElementById("severity").value;
  const feet = Number(document.getElementById("linearFeet").value || 0);
  const visits = Number(document.getElementById("visits").value || 3);
  const warranty = Number(document.getElementById("warrantyAdd").value || 0);
  const notes = document.getElementById("estimateNotes").value.trim();

  const basePrices = {
    Raccoon:650,
    "Grey Squirrel":550,
    "Red Squirrel":575,
    "Flying Squirrel":750,
    Bat:950,
    Skunk:450,
    Groundhog:450,
    Rat:350,
    Mouse:325,
    "Carpenter Bee":350
  };

  const severityMultipliers = {
    Low:1,
    Medium:1.35,
    High:1.8,
    Critical:2.4
  };

  const base = basePrices[selectedSpecies] || 500;
  const severity = severityMultipliers[selectedSeverity] || 1;
  const repair = feet * 22;
  const visitCost = visits * 85;
  const total = Math.round(base * severity + repair + visitCost + warranty);

  const body =
`Wildlife Whisperer LLC Estimate

Customer: ${document.getElementById("estCustomer").value || "Customer"}
Address: ${document.getElementById("estAddress").value || "Service Address"}
Species: ${selectedSpecies}
Severity: ${selectedSeverity}

Recommended Estimate: ${money(total)}

Breakdown:
Base Species Rate: ${money(base)}
Severity Multiplier: ${selectedSeverity} x${severity}
Exclusion / Repair: ${feet} ft x $22 = ${money(repair)}
Visit Allowance: ${visits} visits x $85 = ${money(visitCost)}
Warranty Add-On: ${money(warranty)}

Scope:
${notes || "Inspection, nuisance wildlife removal/exclusion, entry-point correction, and follow-up recommendations."}

Notes:
This estimate is for nuisance wildlife service and exclusion-focused work. Final pricing may change if hidden damage, additional entry points, contamination, or structural repairs are found during service.

Thank you,
Wildlife Whisperer LLC`;

  document.getElementById("estimateOut").value = body;
  document.getElementById("estimateOut").dataset.total = String(total);
};

window.saveEstimate = async function(){
  if(!document.getElementById("estimateOut").value.trim()){
    calcEstimate();
  }

  const { error } = await supabase
    .from("estimates")
    .insert({
      job_id:document.getElementById("estimateJob").value || null,
      customer_name:document.getElementById("estCustomer").value,
      customer_email:document.getElementById("estEmail").value,
      species:document.getElementById("estSpecies").value,
      severity:document.getElementById("severity").value,
      total:Number(document.getElementById("estimateOut").dataset.total || 0),
      body:document.getElementById("estimateOut").value,
      created_by:state.user.id
    });

  if(error){
    alert("Estimate save error: " + error.message);
    return;
  }

  alert("Estimate saved to Supabase.");
  await loadAll();
};

window.sendEstimateEmail = function(){
  if(!document.getElementById("estimateOut").value.trim()){
    calcEstimate();
  }

  const email = document.getElementById("estEmail").value.trim();

  if(!email){
    alert("Enter customer email first.");
    return;
  }

  const subject = encodeURIComponent("Wildlife Whisperer LLC Estimate");
  const body = encodeURIComponent(document.getElementById("estimateOut").value);

  window.location.href = `mailto:${email}?subject=${subject}&body=${body}`;
};

function ai(){
  layout(`
    <section class="hero">
      <h2>AI Field Assistant</h2>
      <p>Rule-based wildlife field intelligence saved to Supabase.</p>
    </section>

    <section class="card form">
      <select id="aiJob">
        <option value="">No linked job</option>
        ${state.jobs.map(job => `
          <option value="${job.id}">
            ${esc(job.title)} — ${esc(job.customer_name)}
          </option>
        `).join("")}
      </select>

      <select id="aiSpecies">
        ${speciesList.map(item => `<option>${item}</option>`).join("")}
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

      <textarea id="aiNotes" placeholder="Observed signs"></textarea>

      <button onclick="generateAIPlan()">Generate Field Plan</button>
      <button class="secondary" onclick="saveAIPlan()">Save AI Plan</button>
    </section>

    <section class="card aiBox">
      <h3>Recommended Plan</h3>
      <div id="aiOut" class="aiText">Pick species + structure, add notes, then generate.</div>
    </section>
  `);
}

window.openAIForJob = function(jobId){
  state.screen = "ai";
  render();

  setTimeout(() => {
    const job = getJob(jobId);

    if(!job){
      return;
    }

    document.getElementById("aiJob").value = job.id;
    document.getElementById("aiSpecies").value = job.species || "Raccoon";
    document.getElementById("aiNotes").value = `${job.title || ""}\n${job.address || ""}\n${job.scope || ""}`;
    generateAIPlan();
  },50);
};

function getSpeciesPlan(selectedSpecies){
  const plans = {
    Raccoon:{
      behavior:"Strong climber. Roofline, chimney, and soffit access are common.",
      inspect:"Roof returns, soffits, chimney caps, attic latrine zones, insulation compression, rub marks.",
      exclusion:"Confirm no dependent young. Use one-way door or removal strategy. Reinforce with metal/flashing/hardware cloth.",
      risk:"Roundworm, contamination, aggressive female with young, repeat entry if structure remains weak."
    },
    "Grey Squirrel":{
      behavior:"Daytime attic activity. Chewing damage. Common soffit/fascia/gable vent entry.",
      inspect:"Soffits, fascia corners, gable vents, ridge vent edges, attic nesting, chewed wiring.",
      exclusion:"Positive set or one-way exclusion after juvenile check. Seal secondary gaps.",
      risk:"Electrical/fire risk and repeat chewing."
    },
    "Red Squirrel":{
      behavior:"Aggressive chewer. Territorial. Can create repeat entry points.",
      inspect:"Fascia, roof edge, soffit returns, ridge gaps, cone/nut caches.",
      exclusion:"Trap or exclude, then overbuild vulnerable edges.",
      risk:"High recurrence if materials are weak."
    },
    "Flying Squirrel":{
      behavior:"Nocturnal, colony-prone, quiet but persistent.",
      inspect:"Night activity, attic trails, wall voids, gable/soffit gaps, droppings.",
      exclusion:"Seal secondary gaps after eviction. One-way devices may need longer monitoring.",
      risk:"Colony behavior and hidden entries."
    },
    Bat:{
      behavior:"Roosting species. Legal timing matters.",
      inspect:"Guano, staining, urine marks, ridge caps, fascia gaps, chimney flashing, gable vents.",
      exclusion:"Use bat valves/cones during legal window. Seal non-primary gaps first.",
      risk:"Legal restrictions, rabies concern, guano cleanup, maternity colony."
    }
  };

  return plans[selectedSpecies] || {
    behavior:"General nuisance wildlife behavior depends on shelter, food, access, and season.",
    inspect:"Entry points, tracks, droppings, nesting, damage, rub marks, travel routes.",
    exclusion:"Remove/evict animal, seal primary and secondary entries, document with photos.",
    risk:"Recurrence if openings or attractants remain."
  };
}

window.generateAIPlan = function(){
  const selectedSpecies = document.getElementById("aiSpecies").value;
  const structure = document.getElementById("aiStructure").value;
  const season = document.getElementById("aiSeason").value;
  const notes = document.getElementById("aiNotes").value.trim();
  const plan = getSpeciesPlan(selectedSpecies);

  const seasonal =
    season === "Spring"
    ? "Spring: confirm dependent young before exclusion."
    : season === "Winter"
    ? "Winter: inspect warm voids and insulation compression."
    : season === "Fall"
    ? "Fall: overwintering pressure increases. Reinforce weak points."
    : "Summer: inspect vents, shaded routes, odor, and heat-driven movement.";

  document.getElementById("aiOut").textContent =
`Species: ${selectedSpecies}
Structure: ${structure}
Season: ${season}

Behavior:
${plan.behavior}

Inspection:
${plan.inspect}

Exclusion Strategy:
${plan.exclusion}

Risk:
${plan.risk}

Seasonal Warning:
${seasonal}

Field Checklist:
1. Photograph all evidence and entry points.
2. Confirm whether young are present.
3. Identify primary and secondary openings.
4. Choose trap, direct removal, or one-way exclusion.
5. Repair with wildlife-resistant materials.
6. Define warranty boundaries clearly.
7. Schedule follow-up if activity continues.

Customer Explanation:
This is not only animal removal. This is structural wildlife exclusion designed to prevent re-entry.

Field Notes:
${notes || "No notes entered."}`;
};

window.saveAIPlan = async function(){
  const plan = document.getElementById("aiOut").textContent;

  const { error } = await supabase
    .from("ai_plans")
    .insert({
      job_id:document.getElementById("aiJob").value || null,
      species:document.getElementById("aiSpecies").value,
      structure_area:document.getElementById("aiStructure").value,
      season:document.getElementById("aiSeason").value,
      field_notes:document.getElementById("aiNotes").value,
      plan,
      created_by:state.user.id
    });

  if(error){
    alert("AI plan save error: " + error.message);
    return;
  }

  alert("AI plan saved.");
  await loadAll();
};

function heatmap(){
  const towns = {};

  state.jobs.forEach(job => {
    const town = job.town || "Unknown";
    towns[town] = (towns[town] || 0) + 1;
  });

  const cards = Object.entries(towns)
    .sort((a,b) => b[1] - a[1])
    .map(([town,count]) => `
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
      <p>Town-based activity clustering from Supabase job history.</p>
    </section>

    ${cards || `<div class="card">No heat map data yet.</div>`}
  `);
}

function photos(){
  layout(`
    <section class="hero">
      <h2>Photo Library</h2>
      <p>Photos uploaded to Supabase Storage from job pages.</p>
    </section>

    ${state.photos.map(photo => `
      <div class="card">
        ${photo.public_url ? `<img class="photoThumb" src="${photo.public_url}">` : ""}
        <h3>${esc(photo.tag)}</h3>
        <p>${esc(photo.notes)}</p>
      </div>
    `).join("") || `<div class="card">No photos yet.</div>`}
  `);
}

function settings(){
  layout(`
    <section class="hero">
      <h2>Settings</h2>
      <p>Supabase-backed Wildlife Whisperer FieldOps.</p>
    </section>

    <section class="card">
      <p><strong>Email:</strong> ${esc(state.user.email)}</p>
      <p><strong>Role:</strong> ${esc(state.profile?.role)}</p>
      <p><strong>Profile ID:</strong> ${esc(state.user.id)}</p>

      <button class="danger" onclick="logout()">Logout</button>
    </section>
  `);
}

function render(){
  if(!state.user){
    renderLogin();
    return;
  }

  if(state.screen === "dashboard") dashboard();
  if(state.screen === "create") createJob();
  if(state.screen === "jobs") jobs();
  if(state.screen === "jobDetail") jobDetail();
  if(state.screen === "techs") techs();
  if(state.screen === "estimate") estimate();
  if(state.screen === "ai") ai();
  if(state.screen === "heatmap") heatmap();
  if(state.screen === "photos") photos();
  if(state.screen === "settings") settings();
}

init();

import { getJobs, searchJobs, closeJob } from '../data/jobs.js';
import { esc, money } from '../utils/helpers.js';

export function renderJobs() {
  const app = document.getElementById('app');
  app.innerHTML = `
    <header>
      <div class="logoRow">
        <div class="logoMark">🦝</div>
        <div>
          <h1>Wildlife Whisperer FieldOps</h1>
          <div class="sub">Jobs</div>
        </div>
      </div>
      <button class="menuBtn" onclick="toggleMenu()">☰</button>
    </header>
    <main>
      <section class="hero">
        <h2>Job Board</h2>
        <p>Search, assign, review, estimate, and document jobs.</p>
      </section>
      <input
        class="search"
        id="jobSearch"
        placeholder="Search customer, address, species, tech, town..."
        oninput="updateJobSearch(this.value)"
      >
      <div id="jobsList"></div>
    </main>
    <nav class="bottomNav">
      <button onclick="go('dashboard')">🏠<br>Home</button>
      <button class="active" onclick="go('jobs')">🦝<br>Jobs</button>
      <button onclick="go('create')">➕<br>New</button>
      <button onclick="go('techs')">👷<br>Techs</button>
      <button onclick="go('estimate')">💵<br>Price</button>
    </nav>
  `;

  // Load and render jobs
  loadAndRenderJobs();
}

async function loadAndRenderJobs(query = '') {
  const jobs = query ? await searchJobs(query) : await getJobs();
  const jobsList = document.getElementById('jobsList');
  jobsList.innerHTML = jobs.map(jobCard).join('') || '<div class="card">No jobs yet.</div>';
}

function jobCard(job) {
  return `
    <div class="card jobCard">
      <h3>${esc(job.title || job.species + ' job')}</h3>
      <p>
        <strong>${esc(job.customer_name)}</strong>
        · <a href="tel:${esc(job.customer_phone)}">${esc(job.customer_phone || 'No phone')}</a>
      </p>
      <p>${esc(job.address)}</p>
      <p>
        <span class="tag">${esc(job.species)}</span>
        <span class="tag">${esc(job.status)}</span>
        <span class="tag">${esc(job.priority || 'Normal')}</span>
        <span class="tag">${esc(job.town || 'No town')}</span>
      </p>
      <p>Assigned: <span class="tag">${esc(job.profiles?.name || job.assigned_to || 'Unassigned')}</span></p>
      <p>${esc(job.scope || 'No notes yet.')}</p>
      <div class="row2">
        <button onclick="openJob('${job.id}')">Open Job</button>
        <button class="secondary" onclick="openAIForJob('${job.id}')">AI Plan</button>
      </div>
    </div>
  `;
}

export async function updateJobSearch(query) {
  await loadAndRenderJobs(query);
}

window.openJob = function(jobId) {
  // Navigate to job detail (implement in src/ui/jobDetail.js)
  go('jobDetail', { jobId });
};

window.closeJob = async function(jobId) {
  await closeJob(jobId);
  await loadAndRenderJobs();
};

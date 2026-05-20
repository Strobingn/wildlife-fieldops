// Add a "synced" indicator to job cards
function jobCard(job) {
  const syncedIndicator = job.synced === false ?
    '<span class="tag warn">⏳ Unsynced</span>' :
    '<span class="tag good">✓ Synced</span>';

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
        ${syncedIndicator}
      </p>
      <p>Assigned: <span class="tag">${esc(job.profiles?.name || job.assigned_to || 'Unassigned')}</span></p>
      <p>${esc(job.scope || 'No notes yet.')}</p>
      <div class="row2">
        <button onclick="openJob('${job.id}')">Open Job</button>
        <button class="secondary" onclick="forceSync()">Sync Now</button>
      </div>
    </div>
  `;
}

// Add a global sync button
window.forceSync = async function() {
  await processSyncQueue();
  // Refresh the current view
  window.go(state.screen);
};

import { supabase } from '../auth/supabaseClient.js';

// Queue for offline actions
let syncQueue = JSON.parse(localStorage.getItem('syncQueue') || '[]');

// Add an action to the sync queue
export function queueAction(action) {
  syncQueue.push(action);
  localStorage.setItem('syncQueue', JSON.stringify(syncQueue));
}

// Process the sync queue (call this when online)
export async function processSyncQueue() {
  if (syncQueue.length === 0) return;

  const queue = [...syncQueue]; // Copy to avoid race conditions
  syncQueue = []; // Clear the queue
  localStorage.setItem('syncQueue', JSON.stringify(syncQueue));

  for (const action of queue) {
    try {
      await processAction(action);
    } catch (error) {
      console.error('Sync error:', error);
      // Re-add failed actions to the queue
      syncQueue.unshift(action);
      localStorage.setItem('syncQueue', JSON.stringify(syncQueue));
    }
  }
}

// Process a single action
async function processAction(action) {
  const { type, payload } = action;

  switch (type) {
    case 'CREATE_JOB':
      await supabase.from('jobs').insert(payload);
      break;
    case 'UPDATE_JOB':
      await supabase.from('jobs').update(payload).eq('id', payload.id);
      break;
    case 'CREATE_VISIT':
      await supabase.from('visits').insert(payload);
      break;
    case 'CREATE_REPAIR':
      await supabase.from('repairs').insert(payload);
      break;
    case 'UPLOAD_PHOTO':
      const { jobId, file, tag, notes } = payload;
      const path = `${jobId}/${Date.now()}-${file.name}`;
      const { error: uploadError } = await supabase.storage
        .from('job-photos')
        .upload(path, file);
      if (uploadError) throw uploadError;

      const { data: urlData } = supabase.storage
        .from('job-photos')
        .getPublicUrl(path);

      await supabase.from('job_photos').insert({
        job_id: jobId,
        path,
        public_url: urlData.publicUrl,
        tag,
        notes,
      });
      break;
    default:
      console.warn('Unknown action type:', type);
  }
}

// Check online status and sync
export function setupSync() {
  window.addEventListener('online', processSyncQueue);
  // Sync every 5 minutes as a fallback
  setInterval(processSyncQueue, 5 * 60 * 1000);
}

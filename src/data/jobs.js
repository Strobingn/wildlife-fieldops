import { supabase } from '../auth/supabaseClient.js';
import { queueAction } from './sync.js';

// Fetch all jobs for the current user
export async function getJobs() {
  const { data, error } = await supabase
    .from('jobs')
    .select('*, profiles(name, email)')
    .order('created_at', { ascending: false });

  if (error) throw error;
  return data;
}

// Create a new job (offline-first)
export async function createJob(jobData) {
  // Generate a temporary ID for offline use
  const tempId = `temp-${Date.now()}`;
  const jobWithTempId = { ...jobData, id: tempId };

  // Queue the action for sync
  queueAction('CREATE_JOB', jobWithTempId);

  // Return the job with temp ID (will be replaced with real ID after sync)
  return { ...jobWithTempId, synced: false };
}

// Update a job (offline-first)
export async function updateJob(jobId, updates) {
  // Queue the action for sync
  queueAction('UPDATE_JOB', { id: jobId, ...updates });
  return { success: true, synced: false };
}

import { supabase } from '../auth/supabaseClient.js';

// Fetch all jobs for the current user
export async function getJobs() {
  const { data, error } = await supabase
    .from('jobs')
    .select('*, profiles(name, email)') // Join with profiles for assigned tech
    .order('created_at', { ascending: false });

  if (error) throw error;
  return data;
}

// Create a new job
export async function createJob(jobData) {
  const { error } = await supabase
    .from('jobs')
    .insert(jobData);
  if (error) throw error;
  return { success: true };
}

// Update a job
export async function updateJob(jobId, updates) {
  const { error } = await supabase
    .from('jobs')
    .update(updates)
    .eq('id', jobId);
  if (error) throw error;
  return { success: true };
}

// Close a job
export async function closeJob(jobId) {
  return updateJob(jobId, { status: 'Closed', updated_at: new Date().toISOString() });
}

// Search jobs
export async function searchJobs(query) {
  const { data, error } = await supabase
    .from('jobs')
    .select('*')
    .ilike('customer_name', `%${query}%`)
    .or(`address.ilike.%${query}%,species.ilike.%${query}%`)
    .order('created_at', { ascending: false });

  if (error) throw error;
  return data;
}

import { createClient } from '@supabase/supabase-js';

// Configure Supabase client
const supabaseUrl = 'https://hgdzmwfcghtilyqagjak.supabase.co';
const supabaseKey = 'YOUR_SUPABASE_SERVICE_ROLE_KEY'; // Use a service role key
const supabase = createClient(supabaseUrl, supabaseKey);

export default {
  async fetch(request, env) {
    // Handle CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response('', { headers: cors() });
    }

    // Only allow POST
    if (request.method !== 'POST') {
      return new Response('Method not allowed', {
        status: 405,
        headers: cors(),
      });
    }

    try {
      const body = await request.json();
      const { device, db, queue } = body;

      if (!queue || !Array.isArray(queue)) {
        return new Response(
          JSON.stringify({ ok: false, error: 'Invalid queue format' }),
          { status: 400, headers: cors() }
        );
      }

      // Process each action in the queue
      const results = [];
      for (const action of queue) {
        const result = await processAction(action, db, device);
        results.push(result);
      }

      // Return sync summary
      return new Response(
        JSON.stringify({
          ok: true,
          syncedActions: results.filter(r => r.ok).length,
          failedActions: results.filter(r => !r.ok).length,
          failures: results.filter(r => !r.ok),
          at: new Date().toISOString(),
        }),
        { headers: { ...cors(), 'content-type': 'application/json' } }
      );
    } catch (error) {
      return new Response(
        JSON.stringify({ ok: false, error: error.message }),
        { status: 500, headers: cors() }
      );
    }
  },
};

// Process a single action
async function processAction(action, db, device) {
  const { id, type, at, payload } = action;

  try {
    switch (type) {
      case 'CREATE_JOB':
        return await handleCreateJob(payload, device);
      case 'UPDATE_JOB':
        return await handleUpdateJob(payload, device);
      case 'CREATE_VISIT':
        return await handleCreateVisit(payload, device);
      case 'CREATE_REPAIR':
        return await handleCreateRepair(payload, device);
      case 'UPLOAD_PHOTO':
        return await handleUploadPhoto(payload, device);
      case 'CREATE_ESTIMATE':
        return await handleCreateEstimate(payload, device);
      case 'CREATE_AI_PLAN':
        return await handleCreateAIPlan(payload, device);
      case 'CREATE_SIGNATURE':
        return await handleCreateSignature(payload, device);
      default:
        return { ok: false, error: `Unknown action type: ${type}`, action };
    }
  } catch (error) {
    return { ok: false, error: error.message, action };
  }
}

// Handlers for each action type
async function handleCreateJob(payload, device) {
  // Check if job already exists (conflict resolution)
  const { data: existingJob, error: fetchError } = await supabase
    .from('jobs')
    .select('id, updated_at')
    .eq('id', payload.id)
    .single();

  if (fetchError && fetchError.code !== 'PGRST116') { // PGRST116 = no rows found
    throw fetchError;
  }

  if (existingJob) {
    // Conflict: compare timestamps (last-write-wins)
    const payloadTime = new Date(payload.updated_at || payload.created_at);
    const existingTime = new Date(existingJob.updated_at);
    if (payloadTime <= existingTime) {
      return { ok: true, skipped: true, reason: 'Older version already exists' };
    }
  }

  // Insert or update
  const { error } = await supabase
    .from('jobs')
    .upsert(payload, { onConflict: 'id' });

  if (error) throw error;
  return { ok: true, type: 'CREATE_JOB', id: payload.id };
}

async function handleUpdateJob(payload, device) {
  const { id, ...updates } = payload;
  const { error } = await supabase
    .from('jobs')
    .update(updates)
    .eq('id', id);

  if (error) throw error;
  return { ok: true, type: 'UPDATE_JOB', id };
}

async function handleCreateVisit(payload, device) {
  const { error } = await supabase
    .from('visits')
    .upsert(payload, { onConflict: 'id' });

  if (error) throw error;
  return { ok: true, type: 'CREATE_VISIT', id: payload.id };
}

async function handleCreateRepair(payload, device) {
  const { error } = await supabase
    .from('repairs')
    .upsert(payload, { onConflict: 'id' });

  if (error) throw error;
  return { ok: true, type: 'CREATE_REPAIR', id: payload.id };
}

async function handleUploadPhoto(payload, device) {
  const { jobId, file, tag, notes, uploaded_by } = payload;

  // Generate a unique path
  const path = `${jobId}/${Date.now()}-${file.name}`;

  // Upload to Supabase Storage
  const { error: uploadError } = await supabase.storage
    .from('job-photos')
    .upload(path, file);

  if (uploadError) throw uploadError;

  // Get public URL
  const { data: urlData } = supabase.storage
    .from('job-photos')
    .getPublicUrl(path);

  // Save photo metadata to database
  const { error: dbError } = await supabase
    .from('job_photos')
    .insert({
      job_id: jobId,
      path,
      public_url: urlData.publicUrl,
      tag,
      notes,
      uploaded_by,
    });

  if (dbError) throw dbError;
  return { ok: true, type: 'UPLOAD_PHOTO', id: payload.id, path };
}

async function handleCreateEstimate(payload, device) {
  const { error } = await supabase
    .from('estimates')
    .upsert(payload, { onConflict: 'id' });

  if (error) throw error;
  return { ok: true, type: 'CREATE_ESTIMATE', id: payload.id };
}

async function handleCreateAIPlan(payload, device) {
  const { error } = await supabase
    .from('ai_plans')
    .upsert(payload, { onConflict: 'id' });

  if (error) throw error;
  return { ok: true, type: 'CREATE_AI_PLAN', id: payload.id };
}

async function handleCreateSignature(payload, device) {
  const { error } = await supabase
    .from('signatures')
    .upsert(payload, { onConflict: 'id' });

  if (error) throw error;
  return { ok: true, type: 'CREATE_SIGNATURE', id: payload.id };
}

// CORS headers
function cors() {
  return {
    'access-control-allow-origin': '*',
    'access-control-allow-methods': 'POST, OPTIONS',
    'access-control-allow-headers': 'content-type, authorization',
  };
}

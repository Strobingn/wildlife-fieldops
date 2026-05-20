import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || 'https://hgdzmwfcghtilyqagjak.supabase.co';
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || 'sb_publishable_ExD5HM7IkieB_ZWItda83w_rFwR3nrB';

// Configure Supabase with offline support
const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
  localStorage: {
    getItem: (key) => localStorage.getItem(key),
    setItem: (key, value) => localStorage.setItem(key, value),
    removeItem: (key) => localStorage.removeItem(key),
  },
  // Enable realtime
  realtime: {
    params: {
      eventsPerSecond: 10,
    },
  },
});

export { supabase };

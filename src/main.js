import { supabase } from './src/auth/supabaseClient.js';
import { setupSync } from './src/data/sync.js';
import { renderDashboard } from './src/ui/dashboard.js';
import { renderJobs } from './src/ui/jobs.js';
import { renderCreateJob } from './src/ui/forms.js';
import './styles.css';

// Global state
const state = {
  user: null,
  profile: null,
  screen: 'dashboard',
  menuOpen: false,
};

// Initialize the app
async function init() {
  // Check for existing session
  const { data: { session } } = await supabase.auth.getSession();
  state.user = session?.user || null;

  if (!state.user) {
    renderLogin();
    return;
  }

  // Load user profile
  await loadProfile();
  setupSync(); // Initialize sync
  renderDashboard(); // Render initial screen
}

// Load user profile
async function loadProfile() {
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', state.user.id)
    .single();

  if (error && error.code !== 'PGRST116') { // PGRST116 = no rows found
    console.error('Profile load error:', error);
    return;
  }

  if (!data) {
    // Create profile if it doesn't exist
    const { data: newProfile, error: createError } = await supabase
      .from('profiles')
      .insert({
        id: state.user.id,
        email: state.user.email,
        name: state.user.email.split('@')[0],
        role: 'owner',
        active: true,
      })
      .select()
      .single();

    if (createError) {
      console.error('Profile create error:', createError);
      return;
    }
    state.profile = newProfile;
  } else {
    state.profile = data;
  }
}

// Render login screen
function renderLogin() {
  document.getElementById('app').innerHTML = `
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

// Login
window.login = async function() {
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;

  const { error } = await supabase.auth.signInWithPassword({
    email,
    password,
  });

  if (error) {
    alert('Login error: ' + error.message);
    return;
  }

  await init();
};

// Signup
window.signup = async function() {
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;

  const { error } = await supabase.auth.signUp({
    email,
    password,
  });

  if (error) {
    alert('Signup error: ' + error.message);
    return;
  }

  alert('Account created. Check your email for confirmation.');
};

// Navigation
window.go = function(screen, params = {}) {
  state.screen = screen;
  state.menuOpen = false;

  switch (screen) {
    case 'dashboard':
      renderDashboard();
      break;
    case 'jobs':
      renderJobs();
      break;
    case 'create':
      renderCreateJob();
      break;
    // Add other screens here
    default:
      renderDashboard();
  }
};

// Toggle menu
window.toggleMenu = function() {
  state.menuOpen = !state.menuOpen;
  // Re-render the current screen to update the menu state
  window.go(state.screen);
};

// Start the app
init();

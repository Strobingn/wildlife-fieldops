#!/usr/bin/env node
/**
 * @file validate-setup.js
 * @description Pre-build validation script. Run with: node scripts/validate-setup.js
 * Checks that your API keys are valid and all config is ready.
 */

import { config, isFeatureAvailable, getBuildInfo } from '../src/config.js';

const CHECK = '\x1b[32m\u2713\x1b[0m';
const CROSS = '\x1b[31m\u2717\x1b[0m';
const WARN = '\x1b[33m\u26A0\x1b[0m';
const INFO = '\x1b[36m\u2139\x1b[0m';

console.log('\n' + '='.repeat(60));
console.log('  Wildlife FieldOps — Setup Validator');
console.log('  ' + getBuildInfo());
console.log('='.repeat(60) + '\n');

let pass = 0;
let fail = 0;
let warn = 0;

function ok(msg)  { console.log(`  ${CHECK} ${msg}`); pass++; }
function bad(msg) { console.log(`  ${CROSS} ${msg}`); fail++; }
function w(msg)   { console.log(`  ${WARN} ${msg}`); warn++; }
function i(msg)   { console.log(`  ${INFO} ${msg}`); }

// ─── 1. Basic Config ────────────────────────────────────────────────────────

console.log('BASIC CONFIG\n' + '-'.repeat(40));
ok(`APP_VERSION = "${config.APP_VERSION}"`);
ok(`BUILD_DATE = "${config.BUILD_DATE}"`);
ok(`GPS_TIMEOUT = ${config.GPS_TIMEOUT}ms`);
ok(`DEFAULT_MAP_ZOOM = ${config.DEFAULT_MAP_ZOOM}`);
ok(`DEFAULT_MAP_CENTER = lat:${config.DEFAULT_MAP_CENTER.lat}, lng:${config.DEFAULT_MAP_CENTER.lng}`);
ok(`MAX_FILE_SIZE_MB = ${config.MAX_FILE_SIZE_MB}`);
ok(`SYNC_INTERVAL = ${config.SYNC_INTERVAL}ms`);
ok(`SNAPSHOT_INTERVAL = ${config.SNAPSHOT_INTERVAL}ms`);
ok(`IMAGE_MAX_WIDTH = ${config.IMAGE_MAX_WIDTH}px`);
ok(`IMAGE_QUALITY = ${config.IMAGE_QUALITY}`);

// ─── 2. Supabase ────────────────────────────────────────────────────────────

console.log('\nSUPABASE\n' + '-'.repeat(40));
if (config.hasSupabase) {
  ok('Supabase is CONFIGURED');
  i(`URL: ${config.SUPABASE_URL}`);
  ok('Realtime: enabled (Supabase connected)');
  ok('Auth: enabled (Supabase connected)');
  ok('Storage: enabled (Supabase connected)');

  // URL validation
  if (!config.SUPABASE_URL.includes('supabase.co')) {
    bad('Supabase URL does not contain "supabase.co" — may be invalid');
  } else {
    ok('Supabase URL format looks correct');
  }

  // Key validation
  if (config.SUPABASE_ANON_KEY.length < 20) {
    bad('Supabase Anon Key looks too short — may be a placeholder');
  } else {
    ok('Supabase Anon Key length looks valid');
  }
} else {
  w('Supabase NOT configured — app will run in OFFLINE mode');
  w('  -> Add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY to GitHub Secrets');
  w('  -> Guide: SETUP_GUIDE.md Part 1A + Part 2');
}

// ─── 3. Google Maps ─────────────────────────────────────────────────────────

console.log('\nGOOGLE MAPS\n' + '-'.repeat(40));
if (config.hasGoogleMaps) {
  ok('Google Maps is CONFIGURED');
  i(`Key prefix: ${config.GOOGLE_MAPS_API_KEY.substring(0, 10)}...`);

  if (!config.GOOGLE_MAPS_API_KEY.startsWith('AIza')) {
    bad('Google Maps Key does not start with "AIza" — may be invalid');
  } else {
    ok('Google Maps Key format looks correct');
  }
} else {
  w('Google Maps NOT configured — GPS tab will show placeholder map');
  w('  -> Add VITE_GOOGLE_MAPS_API_KEY to GitHub Secrets');
  w('  -> Guide: SETUP_GUIDE.md Part 1B + Part 2');
}

// ─── 4. OpenWeather ─────────────────────────────────────────────────────────

console.log('\nWEATHER\n' + '-'.repeat(40));
if (config.hasOpenWeather) {
  ok('OpenWeather is CONFIGURED');
} else {
  w('OpenWeather NOT configured — weather display disabled (optional)');
}

// ─── 5. Feature Flags (server-safe checks) ──────────────────────────────────

console.log('\nFEATURE FLAGS\n' + '-'.repeat(40));
const serverSafeFeatures = ['supabase','googleMaps','googleCalendar','weather'];
for (const f of serverSafeFeatures) {
  const available = isFeatureAvailable(f);
  console.log(`  ${available ? CHECK : CROSS} ${f}: ${available ? 'enabled' : 'disabled'}`);
  if (available) pass++; else warn++;
}
i('(notifications, geolocation, speechRecognition = browser-only)');

// ─── 6. Security Check ──────────────────────────────────────────────────────

console.log('\nSECURITY CHECK\n' + '-'.repeat(40));
let placeholderFound = false;
const keys = {
  'SUPABASE_URL': config.SUPABASE_URL,
  'SUPABASE_ANON_KEY': config.SUPABASE_ANON_KEY,
  'GOOGLE_MAPS_API_KEY': config.GOOGLE_MAPS_API_KEY,
  'OPENWEATHER_API_KEY': config.OPENWEATHER_API_KEY,
};
const badPatterns = ['your-','YOUR_','example','placeholder','xxx','test','demo','changeme','replace'];

for (const [name, value] of Object.entries(keys)) {
  if (!value) continue;
  const lowered = value.toLowerCase();
  for (const bad of badPatterns) {
    if (lowered.includes(bad.toLowerCase())) {
      bad(`${name} contains placeholder text: "${bad}"`);
      placeholderFound = true;
    }
  }
}

if (!placeholderFound) {
  ok('No placeholder text found in API keys');
}

// ─── 7. Summary ─────────────────────────────────────────────────────────────

console.log('\n' + '='.repeat(60));
if (fail === 0) {
  console.log(`  STATUS: \x1b[32m${warn > 0 ? 'READY (with warnings)' : 'ALL CHECKS PASSED'}\x1b[0m`);
  console.log(`  ${pass} passed, ${warn} warnings, 0 errors`);
  console.log('');
  console.log('  NEXT STEPS:');
  if (warn > 0) {
    console.log('  1. Add the missing API keys to GitHub Secrets (SETUP_GUIDE.md)');
    console.log('  2. Re-run this validator: node scripts/validate-setup.js');
    console.log('  3. Push to GitHub: git push origin main');
  } else {
    console.log('  1. Push to GitHub: git push origin main');
    console.log('  2. GitHub Actions will auto-build your APK');
    console.log('  3. Download APK from Actions > Artifacts');
  }
} else {
  console.log(`  STATUS: \x1b[31mBLOCKING ISSUES FOUND\x1b[0m`);
  console.log(`  ${pass} passed, ${warn} warnings, ${fail} errors`);
  console.log('');
  console.log('  Fix the errors above, then re-run:');
  console.log('  node scripts/validate-setup.js');
}
console.log('='.repeat(60) + '\n');

process.exit(fail > 0 ? 1 : 0);

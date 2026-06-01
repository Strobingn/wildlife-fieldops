/**
 * Wildlife Whisperer FieldOps — Service Worker
 * Version: 3.0.0
 * Strategy: Cache-First for static, Network-First for API, Background Sync for mutations
 */

const CACHE_VERSION = 'v5';
const CACHE_NAME = `ww-fieldops-${CACHE_VERSION}`;
const OFFLINE_PAGE = './offline.html';

// Static assets to cache on install
const STATIC_ASSETS = [
  './',
  './index.html',
  './offline.html',
  './manifest.webmanifest',
  './src/main.js',
  './src/config.js',
  './src/constants.js',
  './src/state.js',
  './src/router.js',
  './src/utils.js',
  './src/errors.js',
  './src/styles.css',
  './src/api/supabaseClient.js',
  './src/api/jobs.js',
  './src/api/customers.js',
  './src/api/photos.js',
  './src/api/sync.js',
  './src/api/weather.js',
  './src/api/maps.js',
  './src/api/calendar.js',
  './src/api/pdf.js',
  './src/components/AppShell.js',
  './src/components/Dashboard.js',
  './src/components/JobList.js',
  './src/components/JobDetail.js',
  './src/components/JobForm.js',
  './src/components/CustomerList.js',
  './src/components/CustomerForm.js',
  './src/components/EstimateCalc.js',
  './src/components/PhotoGallery.js',
  './src/components/SignaturePad.js',
  './src/components/GPSTracker.js',
  './src/components/SettingsPage.js',
  './src/components/MetricsPage.js',
  './src/components/AIModal.js',
  './assets/icon.png',
  './assets/logo.png',
  './assets/monochrome.svg'
];

// API origins that should use Network-First strategy
const API_ORIGINS = [
  'supabase.co',
  'supabase.in',
  'api.openweathermap.org',
  'maps.googleapis.com',
  'www.googleapis.com'
];

// Google-managed resources to bypass
const GOOGLE_RESOURCES = [
  'googleapis.com',
  'gstatic.com',
  'accounts.google.com',
  'gsi.googleapis.com',
  'maps.google.com'
];

// Check if a URL matches any pattern
function matchesAny(url, patterns) {
  return patterns.some(pattern => url.includes(pattern));
}

// =========================
// INSTALL
// =========================
self.addEventListener('install', event => {
  console.log(`[SW ${CACHE_VERSION}] Installing...`);

  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        // Cache static assets with individual fallback
        const cachePromises = STATIC_ASSETS.map(asset => {
          return cache.add(asset).catch(err => {
            console.warn(`[SW] Failed to cache: ${asset}`, err.message);
            // Continue even if one asset fails
          });
        });
        return Promise.all(cachePromises);
      })
      .then(() => {
        console.log(`[SW ${CACHE_VERSION}] Static assets cached`);
      })
      .catch(err => {
        console.error(`[SW ${CACHE_VERSION}] Install failed:`, err);
      })
  );

  // Activate immediately
  self.skipWaiting();
});

// =========================
// ACTIVATE
// =========================
self.addEventListener('activate', event => {
  console.log(`[SW ${CACHE_VERSION}] Activating...`);

  event.waitUntil(
    caches.keys()
      .then(cacheNames => {
        return Promise.all(
          cacheNames
            .filter(name => name.startsWith('ww-fieldops-') && name !== CACHE_NAME)
            .map(name => {
              console.log(`[SW] Deleting old cache: ${name}`);
              return caches.delete(name);
            })
        );
      })
      .then(() => self.clients.claim())
      .then(() => {
        console.log(`[SW ${CACHE_VERSION}] Activated and controlling clients`);
        // Notify all clients about the update
        return self.clients.matchAll({ type: 'window' }).then(clients => {
          clients.forEach(client => {
            client.postMessage({ type: 'SW_ACTIVATED', version: CACHE_VERSION });
          });
        });
      })
  );
});

// =========================
// FETCH
// =========================
self.addEventListener('fetch', event => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET requests for cache (except for API mutations handled by background sync)
  if (request.method !== 'GET') {
    // Allow non-GET to pass through normally - no caching
    return;
  }

  // Skip Google-managed resources (they handle their own caching)
  if (matchesAny(url.href, GOOGLE_RESOURCES)) {
    return;
  }

  // Skip chrome-extension and blob URLs
  if (url.protocol === 'chrome-extension:' || url.protocol === 'blob:') {
    return;
  }

  // Network-First strategy for API calls
  if (matchesAny(url.href, API_ORIGINS)) {
    event.respondWith(networkFirstStrategy(request));
    return;
  }

  // Cache-First strategy for static assets
  event.respondWith(cacheFirstStrategy(request));
});

// Cache-First: Try cache first, fall back to network, cache the result
async function cacheFirstStrategy(request) {
  const cache = await caches.open(CACHE_NAME);
  const cached = await cache.match(request);

  if (cached) {
    // Return cached response immediately
    // Refresh cache in background (stale-while-revalidate)
    fetch(request)
      .then(response => {
        if (response && response.status === 200 && response.type === 'basic') {
          cache.put(request, response.clone());
        }
      })
      .catch(() => { /* Ignore background refresh errors */ });

    return cached;
  }

  // Not in cache - fetch from network
  try {
    const networkResponse = await fetch(request);
    if (networkResponse && networkResponse.status === 200 && networkResponse.type === 'basic') {
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  } catch (error) {
    // Network failed - try to return the offline page for navigation requests
    if (request.mode === 'navigate') {
      const offlinePage = await cache.match(OFFLINE_PAGE);
      if (offlinePage) return offlinePage;
    }
    // Return a minimal error response
    return new Response(
      JSON.stringify({ error: 'Offline', message: 'Resource not available offline' }),
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }
}

// Network-First: Try network first, fall back to cache
async function networkFirstStrategy(request) {
  const cache = await caches.open(CACHE_NAME);

  try {
    const networkResponse = await fetch(request);
    // Cache successful GET API responses for offline fallback
    if (request.method === 'GET' && networkResponse && networkResponse.ok) {
      cache.put(request, networkResponse.clone());
    }
    return networkResponse;
  } catch (error) {
    // Network failed - try cache
    const cached = await cache.match(request);
    if (cached) {
      return cached;
    }

    // Return offline error response for API calls
    return new Response(
      JSON.stringify({
        error: 'Offline',
        message: 'You are currently offline. Data will sync when connection is restored.',
        offline: true
      }),
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }
}

// =========================
// BACKGROUND SYNC
// =========================
self.addEventListener('sync', event => {
  console.log(`[SW] Background sync triggered: ${event.tag}`);

  if (event.tag === 'ww-sync') {
    event.waitUntil(
      self.clients.matchAll({ type: 'window' })
        .then(clients => {
          clients.forEach(client => {
            client.postMessage({
              type: 'SYNC_NOW',
              tag: event.tag,
              timestamp: Date.now()
            });
          });
        })
        .catch(err => console.error('[SW] Background sync failed:', err))
    );
  }

  if (event.tag === 'ww-sync-photos') {
    event.waitUntil(
      self.clients.matchAll({ type: 'window' })
        .then(clients => {
          clients.forEach(client => {
            client.postMessage({
              type: 'SYNC_PHOTOS',
              tag: event.tag,
              timestamp: Date.now()
            });
          });
        })
        .catch(err => console.error('[SW] Photo sync failed:', err))
    );
  }
});

// =========================
// PUSH NOTIFICATIONS
// =========================
self.addEventListener('push', event => {
  console.log('[SW] Push received:', event);

  let payload = {};
  try {
    payload = event.data ? event.data.json() : {};
  } catch (e) {
    payload = { title: 'FieldOps', body: event.data ? event.data.text() : 'New notification' };
  }

  const title = payload.title || 'Wildlife Whisperer FieldOps';
  const options = {
    body: payload.body || 'You have a new notification',
    icon: './assets/icon.png',
    badge: './assets/monochrome.svg',
    tag: payload.tag || 'default',
    requireInteraction: payload.requireInteraction || false,
    actions: payload.actions || [],
    data: payload.data || {},
    timestamp: Date.now()
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

// Notification click handler
self.addEventListener('notificationclick', event => {
  event.notification.close();

  const notificationData = event.notification.data || {};
  const action = event.action;

  let targetUrl = './';
  if (notificationData.jobId) targetUrl = `./#/jobs/${notificationData.jobId}`;
  if (notificationData.page) targetUrl = `./#/${notificationData.page}`;

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then(clients => {
        // If a window is already open, focus it and navigate
        for (const client of clients) {
          if (client.url.includes(self.registration.scope) && 'focus' in client) {
            client.focus();
            client.postMessage({
              type: 'NOTIFICATION_CLICK',
              action,
              data: notificationData
            });
            client.navigate(targetUrl);
            return;
          }
        }
        // Otherwise open a new window
        if (self.clients.openWindow) {
          return self.clients.openWindow(targetUrl);
        }
      })
  );
});

// Push subscription change
self.addEventListener('pushsubscriptionchange', event => {
  console.log('[SW] Push subscription changed');
  event.waitUntil(
    self.clients.matchAll({ type: 'window' })
      .then(clients => {
        clients.forEach(client => {
          client.postMessage({ type: 'PUSH_SUBSCRIPTION_CHANGED' });
        });
      })
  );
});

// =========================
// MESSAGE HANDLING (from main thread)
// =========================
self.addEventListener('message', event => {
  if (!event.data) return;

  switch (event.data.type) {
    case 'SKIP_WAITING':
      console.log('[SW] Skip waiting command received');
      self.skipWaiting();
      break;

    case 'GET_VERSION':
      event.source.postMessage({ type: 'SW_VERSION', version: CACHE_VERSION });
      break;

    case 'CACHE_ASSETS':
      if (event.data.assets && Array.isArray(event.data.assets)) {
        event.waitUntil(
          caches.open(CACHE_NAME)
            .then(cache => {
              return Promise.all(
                event.data.assets.map(asset =>
                  cache.add(asset).catch(err =>
                    console.warn(`[SW] Failed to cache runtime asset: ${asset}`, err)
                  )
                )
              );
            })
            .then(() => {
              event.source.postMessage({ type: 'ASSETS_CACHED', success: true });
            })
        );
      }
      break;

    case 'CLEAR_CACHE':
      event.waitUntil(
        caches.delete(CACHE_NAME)
          .then(() => {
            event.source.postMessage({ type: 'CACHE_CLEARED', success: true });
          })
      );
      break;

    default:
      break;
  }
});

// =========================
// PERIODIC BACKGROUND SYNC (if supported)
// =========================
self.addEventListener('periodicsync', event => {
  if (event.tag === 'ww-periodic-sync') {
    event.waitUntil(
      self.clients.matchAll({ type: 'window' })
        .then(clients => {
          clients.forEach(client => {
            client.postMessage({
              type: 'PERIODIC_SYNC',
              timestamp: Date.now()
            });
          });
        })
    );
  }
});

console.log(`[SW ${CACHE_VERSION}] Service Worker loaded`);

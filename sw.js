const C = 'ww-fieldops-v3';
const ASSETS = [
  './',
  './index.html',
  './manifest.webmanifest',
  './src/main.js',
  './src/style.css',
  './src/auth/supabaseClient.js'
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(C).then(cache => cache.addAll(ASSETS)).catch(() => {})
  );
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== C).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  // Network-first for API calls
  if (e.request.url.includes('supabase.co') || e.request.url.includes('googleapis.com')) {
    e.respondWith(fetch(e.request).catch(() => caches.match(e.request)));
    return;
  }

  // Cache-first for static assets
  e.respondWith(
    caches.match(e.request).then(cached => {
      if (cached) return cached;
      return fetch(e.request).then(response => {
        if (!response || response.status !== 200 || response.type !== 'basic') return response;
        const clone = response.clone();
        caches.open(C).then(cache => cache.put(e.request, clone));
        return response;
      });
    }).catch(() => caches.match('./index.html'))
  );
});

// Background sync for offline queue
self.addEventListener('sync', e => {
  if (e.tag === 'ww-sync') {
    e.waitUntil(self.clients.matchAll().then(clients => {
      clients.forEach(client => client.postMessage({ type: 'SYNC_NOW' }));
    }));
  }
});

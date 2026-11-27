const CACHE_VERSION = 'odyssey-pwa-v4.0';
const STATIC_CACHE = `${CACHE_VERSION}-static`;
const RUNTIME_CACHE = `${CACHE_VERSION}-runtime`;

const PRECACHE_ASSETS = [
  '/',
  '/offline.html',
  '/manifest.webmanifest',
  '/css/odyssey-travela.css',
  '/img/favicon.svg',
  '/img/favicon-192x192.png',
  '/img/chatbot-icon.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then((cache) => cache.addAll(PRECACHE_ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== STATIC_CACHE && key !== RUNTIME_CACHE)
          .map((key) => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (
    url.pathname.startsWith('/api/v1/ai/stream') ||
    url.pathname.startsWith('/admin') ||
    url.pathname.startsWith('/auth') ||
    url.pathname.startsWith('/login') ||
    url.pathname.startsWith('/logout') ||
    url.pathname.startsWith('/actuator')
  ) {
    return;
  }

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone();
          caches.open(RUNTIME_CACHE).then((cache) => cache.put(request, copy));
          return response;
        })
        .catch(() =>
          caches.match(request).then((cached) => cached || caches.match('/offline.html'))
        )
    );
    return;
  }

  if (
    url.pathname.startsWith('/css/') ||
    url.pathname.startsWith('/js/') ||
    url.pathname.startsWith('/img/') ||
    url.pathname.startsWith('/api/v1/lugares') ||
    url.pathname.startsWith('/api/v1/clima')
  ) {
    event.respondWith(
      caches.match(request).then((cached) => {
        const networkFetch = fetch(request)
          .then((response) => {
            if (response && response.status === 200) {
              const copy = response.clone();
              caches.open(RUNTIME_CACHE).then((cache) => cache.put(request, copy));
            }
            return response;
          })
          .catch(() => cached);
        return cached || networkFetch;
      })
    );
  }
});

// ── Web Push API + Notificaciones Nativas PWA (Odyssey v4.0) ──
self.addEventListener('push', (event) => {
  let payload = {
    title: '⛵ ODYSSEY Cartagena',
    body: 'Nueva actualización cultural disponible en tus sitios favoritos.',
    url: '/eventos'
  };
  if (event.data) {
    try {
      const parsed = event.data.json();
      payload = {
        title: parsed.title || parsed.titulo || payload.title,
        body: parsed.body || parsed.cuerpo || payload.body,
        url: parsed.url || parsed.urlDestino || payload.url
      };
    } catch (_) {
      payload.body = event.data.text() || payload.body;
    }
  }

  event.waitUntil(
    self.registration.showNotification(payload.title, {
      body: payload.body,
      icon: '/img/favicon-192x192.png',
      badge: '/img/favicon-32x32.png',
      vibrate: [120, 60, 120],
      data: { url: payload.url }
    })
  );
});

self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'ODYSSEY_LOCAL_NOTIFY') {
    self.registration.showNotification(event.data.title || '⛵ ODYSSEY Cartagena', {
      body: event.data.body || 'Alertas inteligentes activadas en este dispositivo.',
      icon: '/img/favicon-192x192.png',
      badge: '/img/favicon-32x32.png',
      data: { url: event.data.url || '/eventos' }
    });
  }
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = (event.notification.data && event.notification.data.url) || '/eventos';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (const client of windowClients) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          client.navigate(targetUrl);
          return client.focus();
        }
      }
      return clients.openWindow ? clients.openWindow(targetUrl) : null;
    })
  );
});

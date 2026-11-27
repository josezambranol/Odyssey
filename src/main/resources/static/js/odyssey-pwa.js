(function () {
  if (!document.querySelector('link[rel="manifest"]')) {
    const manifestLink = document.createElement('link');
    manifestLink.rel = 'manifest';
    manifestLink.href = '/manifest.webmanifest';
    document.head.appendChild(manifestLink);
  }
  if (!document.querySelector('meta[name="theme-color"]')) {
    const metaTheme = document.createElement('meta');
    metaTheme.name = 'theme-color';
    metaTheme.content = '#0b2d6b';
    document.head.appendChild(metaTheme);
  }

  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').catch(() => {});
    });
  }

  let deferredInstallPrompt = null;
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredInstallPrompt = e;
    document.querySelectorAll('[data-pwa-install]').forEach((btn) => {
      btn.style.display = 'inline-flex';
    });
  });

  window.instalarOdysseyPwa = async function () {
    if (!deferredInstallPrompt) return;
    deferredInstallPrompt.prompt();
    await deferredInstallPrompt.userChoice;
    deferredInstallPrompt = null;
    document.querySelectorAll('[data-pwa-install]').forEach((btn) => {
      btn.style.display = 'none';
    });
  };

  function mostrarBannerRed(online) {
    let toast = document.getElementById('odysseyNetToast');
    if (online) {
      if (toast) toast.remove();
      return;
    }
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'odysseyNetToast';
      toast.style.cssText =
        'position:fixed;bottom:18px;left:18px;z-index:2000;background:#071630;color:#fff;' +
        'border:1px solid #d4a64a;border-radius:14px;padding:10px 15px;font-size:.78rem;' +
        'box-shadow:0 12px 30px rgba(0,0,0,.35);display:flex;align-items:center;gap:10px;';
      toast.innerHTML =
        '<span>📡 Sin conexión · Caché Offline activa</span>' +
        '<a href="/offline.html" style="color:#f3c86b;font-weight:700;text-decoration:none;">Guía SOS →</a>';
      document.body.appendChild(toast);
    }
  }

  window.addEventListener('offline', () => mostrarBannerRed(false));
  window.addEventListener('online', () => mostrarBannerRed(true));

  window.odysseyStreamAi = function (mensaje, callbacks) {
    const { onToken, onDone, onError } = callbacks;
    if (!window.EventSource) {
      fallbackPostAi(mensaje, callbacks);
      return;
    }
    let recibioAlgo = false;
    const es = new EventSource('/api/v1/ai/stream?q=' + encodeURIComponent(mensaje));
    es.addEventListener('token', (ev) => {
      try {
        const payload = JSON.parse(ev.data);
        if (payload.chunk) {
          recibioAlgo = true;
          onToken(payload.chunk);
        }
      } catch (_) {}
    });
    es.addEventListener('done', (ev) => {
      es.close();
      try { onDone(JSON.parse(ev.data)); } catch (_) { onDone({}); }
    });
    es.onerror = () => {
      es.close();
      if (!recibioAlgo) fallbackPostAi(mensaje, callbacks);
      else if (onDone) onDone({});
    };
  };

  async function fallbackPostAi(mensaje, { onToken, onDone, onError }) {
    try {
      const res = await fetch('/chatbot/respuesta', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
        body: JSON.stringify({ mensaje })
      });
      const data = await res.json();
      if (data && data.respuesta) {
        onToken(data.respuesta);
        if (onDone) onDone({ motor: data.motor || 'Odyssey AI' });
      } else if (onError) onError();
    } catch (err) {
      if (onError) onError(err);
    }
  }
})();

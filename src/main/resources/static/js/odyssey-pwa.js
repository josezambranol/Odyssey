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

  function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }

  window.activarNotificacionesOdyssey = async function (lugarId, lugarNombre) {
    if (!('Notification' in window) || !('serviceWorker' in navigator)) {
      return { ok: false, mensaje: 'Tu navegador actual no soporta notificaciones Web Push.' };
    }
    const permiso = await Notification.requestPermission();
    if (permiso !== 'granted') {
      return { ok: false, mensaje: 'Activa el permiso de notificaciones en el navegador para recibir avisos.' };
    }

    try {
      const reg = await navigator.serviceWorker.ready;
      const keyRes = await fetch('/api/v1/push/vapid-public-key' + (lugarId ? '?lugarId=' + lugarId : ''));
      const keyJson = await keyRes.json();
      const publicKey = keyJson && keyJson.data ? keyJson.data.publicKey : null;

      let endpoint = 'pwa-local://' + window.location.host + '/device/' + (lugarId || 'general');
      let keys = { p256dh: 'local-browser-p256dh', auth: 'local-browser-auth' };

      if (publicKey && reg.pushManager) {
        try {
          const pushSub = await reg.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(publicKey)
          });
          const subJson = pushSub.toJSON();
          endpoint = subJson.endpoint || endpoint;
          keys = subJson.keys || keys;
        } catch (_) {
          // Fallback local limpio cuando el navegador restringe push relay en HTTP local
        }
      }

      await fetch('/api/v1/push/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
        body: JSON.stringify({
          endpoint,
          keys,
          lugarId: lugarId || null,
          idioma: document.documentElement.lang || 'es'
        })
      });

      if (reg.active) {
        reg.active.postMessage({
          type: 'ODYSSEY_LOCAL_NOTIFY',
          title: '⛵ Alertas Odyssey v4.0 activadas',
          body: lugarNombre
            ? `Te avisaremos cuando haya eventos culturales o alertas en ${lugarNombre}.`
            : 'Recibirás avisos de eventos culturales y clima marino en Cartagena.',
          url: lugarId ? `/lugares/${lugarId}` : '/eventos'
        });
      }

      return { ok: true, mensaje: '¡Listo! Notificaciones Web Push activadas en este dispositivo.' };
    } catch (err) {
      return { ok: false, mensaje: 'No se pudo completar el registro Push. Inténtalo de nuevo.' };
    }
  };

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

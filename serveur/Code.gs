/**
 * YRO — Filet serveur (dead man's switch) — multi-appareils
 * ---------------------------------------------------------
 * Ce script tourne sur les serveurs de Google : il envoie l'e-mail d'alerte
 * même si votre téléphone est éteint, verrouillé, sans SIM ou perdu.
 *
 * Un SEUL script / compte Google peut servir PLUSIEURS téléphones ou applications :
 * chaque installation possède un identifiant unique (id) et est suivie séparément.
 *
 * Fonctionnement :
 *  - Chaque appareil envoie un "check-in" (je suis là) à chaque validation.
 *  - Un déclencheur horaire appelle check() toutes les 5 min.
 *  - Pour chaque appareil, si plus aucun check-in n'arrive avant son échéance → e-mail.
 *
 * INSTALLATION : voir GUIDE.md
 */

// 🔒 Jeton secret LONG et aléatoire, identique à celui saisi dans l'app.
const TOKEN = 'CHANGEZ-MOI-mettez-un-jeton-secret-long-et-aleatoire';

const PROPS = PropertiesService.getScriptProperties();

function getDevices() {
  const s = PROPS.getProperty('devices');
  return s ? JSON.parse(s) : {};
}
function saveDevices(d) {
  PROPS.setProperty('devices', JSON.stringify(d));
}

function doPost(e) {
  try {
    const body = JSON.parse(e.postData.contents);
    if (body.token !== TOKEN) return json({ ok: false, error: 'bad token' });

    const id = body.id;
    if (!id) return json({ ok: false, error: 'missing id' });

    const devices = getDevices();

    if (body.action === 'checkin') {
      devices[id] = {
        label: body.label || '',
        armed: true,
        fireAt: Number(body.fireAt || 0),
        to: body.to || '',
        subject: body.subject || 'Alerte',
        message: body.message || '',
        position: body.position || ''
      };
      saveDevices(devices);
      return json({ ok: true });
    }

    if (body.action === 'disarm') {
      if (devices[id]) { devices[id].armed = false; saveDevices(devices); }
      return json({ ok: true });
    }

    return json({ ok: false, error: 'unknown action' });
  } catch (err) {
    return json({ ok: false, error: String(err) });
  }
}

// Appelée par le déclencheur horaire (toutes les 5 min)
function check() {
  const devices = getDevices();
  const now = Date.now();
  let changed = false;

  for (const id in devices) {
    const d = devices[id];
    if (!d || !d.armed) continue;
    if (!d.fireAt || now <= Number(d.fireAt)) continue;

    if (d.to) {
      let body = d.message || '';
      if (d.position) body += '\n\nPosition : ' + d.position;
      MailApp.sendEmail(d.to, d.subject || 'Alerte', body);
    }
    d.armed = false; // évite les envois répétés
    changed = true;
  }

  if (changed) saveDevices(devices);
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

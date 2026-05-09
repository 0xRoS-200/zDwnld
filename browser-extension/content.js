/**
 * zDwnld Helper — Content Script
 * Detects <video>, <audio>, and direct download links on every page.
 * Shows a floating "⬇ Download with zDwnld" button on hover.
 */

const ZDWNLD_URL = 'http://127.0.0.1:6868';
const BADGE_ID   = 'zdwnld-badge';

// --- Badge element -----------------------------------------------------------
function createBadge() {
  if (document.getElementById(BADGE_ID)) return document.getElementById(BADGE_ID);
  const badge = document.createElement('div');
  badge.id = BADGE_ID;
  badge.innerText = '⬇ Download with zDwnld';
  Object.assign(badge.style, {
    position:        'fixed',
    bottom:          '24px',
    right:           '24px',
    zIndex:          '2147483647',
    background:      'linear-gradient(135deg, #ff8c00, #e05000)',
    color:           '#fff',
    fontFamily:      'Segoe UI, sans-serif',
    fontSize:        '14px',
    fontWeight:      'bold',
    padding:         '10px 20px',
    borderRadius:    '30px',
    boxShadow:       '0 4px 20px rgba(255,140,0,0.5)',
    cursor:          'pointer',
    transition:      'transform 0.15s, opacity 0.15s',
    opacity:         '0',
    pointerEvents:   'none',
    userSelect:      'none',
  });
  badge.addEventListener('mouseenter', () => badge.style.transform = 'scale(1.05)');
  badge.addEventListener('mouseleave', () => badge.style.transform = 'scale(1.0)');
  document.body.appendChild(badge);
  return badge;
}

let currentUrl = null;
let hideTimer   = null;

function showBadge(url) {
  currentUrl = url;
  const badge = createBadge();
  clearTimeout(hideTimer);
  badge.style.opacity      = '1';
  badge.style.pointerEvents = 'auto';
  badge.onclick = () => sendToZDwnld(url);
}

function scheduledHide() {
  hideTimer = setTimeout(() => {
    const badge = document.getElementById(BADGE_ID);
    if (badge) { badge.style.opacity = '0'; badge.style.pointerEvents = 'none'; }
    currentUrl = null;
  }, 800);
}

function sendToZDwnld(url) {
  fetch(ZDWNLD_URL + '/download', {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify({ url }),
  })
  .then(r => r.json())
  .then(() => { /* zDwnld received it */ })
  .catch(() => {
    alert('zDwnld is not running. Please open zDwnld first.');
  });
}

// --- Attach hover listeners to media / download elements --------------------
const DOWNLOAD_EXTS = /\.(mp4|mkv|avi|mov|flv|webm|mp3|m4a|aac|flac|ogg|wav|zip|rar|7z|tar|gz|iso|pdf|exe|msi|apk|dmg)(\?|$)/i;

function attachToElement(el) {
  if (el.dataset.zdwnldAttached) return;
  el.dataset.zdwnldAttached = 'true';

  el.addEventListener('mouseenter', () => {
    let url = null;
    if (el.tagName === 'VIDEO' || el.tagName === 'AUDIO') {
      url = el.src || el.querySelector('source')?.src;
    } else if (el.tagName === 'A') {
      url = el.href;
    }
    if (url && url.startsWith('http')) showBadge(url);
  });

  el.addEventListener('mouseleave', scheduledHide);
}

function scanPage() {
  document.querySelectorAll('video, audio').forEach(attachToElement);
  document.querySelectorAll('a[href]').forEach(a => {
    if (DOWNLOAD_EXTS.test(a.href)) attachToElement(a);
  });
}

// Initial scan + watch for dynamic content
scanPage();
const observer = new MutationObserver(scanPage);
observer.observe(document.body, { childList: true, subtree: true });

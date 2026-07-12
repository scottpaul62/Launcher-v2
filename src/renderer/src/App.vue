<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import logo from './assets/logo.png'
import planet from './assets/planet.png'
import sky from './assets/sky.jpg'
import comet from './assets/comet.png'

/* ===================== Traceability helper ===================== */
function ulog (msg) { try { if (window.hw && window.hw.uiLog) window.hw.uiLog(msg) } catch (_) {} }

/* ===================== Identity / config ===================== */
const CONFIG = reactive({
  pseudo: 'StevenO2LP',
  uuid: null,
  actus: [
    { tag: 'Nouveauté', date: '8 juillet 2026', titre: '⚡ La Hache de Zeus est arrivée',
      texte: "Un artefact légendaire forgé dans la foudre du maître de l'Olympe. Effets de foudre en jeu, texture animée — saurez-vous la mériter ?" },
    { tag: 'Mise à jour', date: '9 juillet 2026', titre: '🏛️ Interface Olympe v2',
      texte: "Menus marbre et or, nouveau scoreboard aux lauriers, monnaie Drachme : le serveur prend les couleurs de l'Olympe." },
    { tag: 'Événement', date: '10 juillet 2026', titre: '🌿 Ouverture publique en préparation',
      texte: "Le launcher, le domaine play.heroesworld.fr et les derniers préparatifs avancent. L'Olympe ouvrira bientôt ses portes." }
  ]
})
const APP_VERSION = '2.5.1'
const comet_ = comet // conserve l'import (utilisé comme sprite décoratif potentiel)

/* ===================== Réglages persistés ===================== */
const settings = reactive(Object.assign(
  { ram: 4, res: '1920 × 1080', dir: '%APPDATA%/.heroesworld', java: '' },
  JSON.parse(localStorage.getItem('hwSettings') || '{}')
))
function saveSettings () { localStorage.setItem('hwSettings', JSON.stringify(settings)) }
watch(settings, saveSettings, { deep: true })

const nameColor = ref(localStorage.getItem('hwNameColor') || '#FFD700')
function pickColor (e) { nameColor.value = e.target.value; localStorage.setItem('hwNameColor', e.target.value) }

/* ===================== i18n ===================== */
function normalizeLang (v) {
  const c = ('' + (v || '')).toLowerCase()
  if (c.startsWith('en')) return 'en'
  return 'fr'
}
const lang = ref(normalizeLang(localStorage.getItem('hwLang')))
const messages = {
  fr: {
    'nav.accueil': 'Accueil', 'nav.actus': 'Actualités', 'nav.cosmetiques': 'Cosmétiques', 'nav.amis': 'Amis',
    'nav.mods': 'Mods', 'nav.langues': 'Langues', 'nav.themes': 'Thèmes', 'nav.settings': 'Paramètres',
    'window.minimize': 'Réduire', 'window.maximize': 'Agrandir', 'window.close': 'Fermer',
    'account.loginMs': 'Se connecter avec Microsoft', 'account.myAccount': 'Mon compte Microsoft',
    'account.changeSkin': 'Changer de skin', 'account.switchAccount': 'Changer de compte',
    'account.screenshots': "Captures d'écran", 'account.settings': 'Paramètres du compte',
    'account.logout': 'Déconnexion', 'account.online': 'En ligne', 'account.offline': 'Hors session',
    'home.tagline': "L'OLYMPE VOUS ATTEND", 'home.launch': 'LANCER',
    'home.onlineChip': 'En ligne · {online}/{max} joueurs', 'home.offlineChip': 'Hors ligne',
    'home.seeAll': 'Tout voir ›',
    'actus.subtitle': 'Toutes les annonces du serveur HEROES-WORLD.', 'actus.prev': 'Précédent', 'actus.next': 'Suivant',
    'cosmetics.subtitle': 'Aperçu de ton personnage. Le catalogue arrive avec la boutique.',
    'cosmetics.notice': 'Les cosmétiques (capes, auras, compagnons…) arriveront avec la boutique HEROES-WORLD.',
    'cosmetics.previewSub': 'Aperçu de ton skin actuel.', 'cosmetics.searchPlaceholder': 'Rechercher un cosmétique…',
    'cosmetics.soonBadge': 'Bientôt',
    'friends.subtitle': 'Ta liste d\'amis HEROES-WORLD.', 'friends.tabOnline': 'En ligne', 'friends.tabOffline': 'Hors ligne',
    'friends.tabRequests': 'Demandes', 'friends.emptyTitle': 'Aucun ami pour le moment.',
    'friends.emptySub': "La liste d'amis sera disponible avec l'hébergement du serveur HEROES-WORLD.",
    'mods.subtitle': 'Fabric 0.19.3 · Minecraft 1.20.6', 'mods.searchPlaceholder': 'Rechercher un mod…',
    'mods.openFolderBtn': 'Ouvrir le dossier des mods', 'mods.restartHint': 'Redémarrage du launcher requis pour appliquer les changements.',
    'mods.groupClient': 'Client', 'mods.groupMinecraft': 'Minecraft', 'mods.groupLibraries': 'Bibliothèques',
    'mods.alwaysActive': 'Toujours actif', 'mods.emptyList': 'Aucun mod personnalisé détecté dans le dossier du jeu.',
    'mods.version': 'Version', 'mods.author': 'Auteur', 'mods.size': 'Taille', 'mods.group': 'Groupe',
    'mods.selectPrompt': 'Sélectionne un mod pour voir ses détails.',
    'langues.subtitle': "Choisis la langue de l'interface du launcher.", 'langues.searchPlaceholder': 'Rechercher une langue…',
    'common.comingSoon': 'À venir',
    'themes.subtitle': "Change l'ambiance visuelle du launcher.", 'themes.active': 'Actif',
    'theme.olympe.name': 'Olympe', 'theme.olympe.desc': 'Foudre dorée',
    'theme.jupiter.name': 'Jupiter', 'theme.jupiter.desc': 'Espace & planète',
    'theme.nuit.name': 'Nuit étoilée', 'theme.nuit.desc': 'Ciel profond',
    'theme.aurore.name': 'Aurore', 'theme.aurore.desc': 'Lueurs polaires',
    'theme.sombre.name': 'Sombre', 'theme.sombre.desc': 'Minimal & épuré',
    'settings.memoryTitle': 'Mémoire', 'settings.ramLabel': 'RAM allouée', 'settings.ramUnit': 'Go',
    'settings.ramNote': 'Recommandé : 4 Go.', 'settings.displayTitle': 'Affichage', 'settings.resolutionLabel': 'Résolution',
    'settings.folderTitle': 'Dossier du jeu', 'settings.openFolderBtn': 'Ouvrir le dossier du jeu',
    'settings.javaTitle': 'Java (optionnel)', 'settings.javaPlaceholder': 'Chemin vers javaw.exe (auto si vide)',
    'settings.accountTitle': 'Compte', 'settings.connectedAs': 'Connecté : {name}.', 'settings.logoutBtn': 'Déconnexion',
    'settings.loginBtn': 'Se connecter', 'settings.launcherTitle': 'Launcher', 'settings.versionLabel': 'Version {v}',
    'settings.checkUpdateBtn': 'Vérifier les mises à jour',
    'launch.phase.prepare': 'Préparation…', 'launch.phase.meta': 'Vérification des fichiers…',
    'launch.phase.install': 'Téléchargement de Minecraft…', 'launch.phase.fabric': 'Installation de Fabric…',
    'launch.phase.deps': 'Installation des dépendances…', 'launch.phase.ready': 'Prêt',
    'launch.phase.auth': 'Authentification…', 'launch.phase.launch': 'Lancement du jeu…',
    'launch.phase.done': 'Olympe rejoint !', 'launch.phase.warn': 'Avertissement', 'launch.phase.error': 'Erreur',
    'launch.phase.exit': 'Jeu fermé', 'launch.phase.default': 'Démarrage…',
    'launch.failedTitle': 'Le lancement a échoué', 'launch.showLogs': 'Afficher les journaux',
    'launch.hideLogs': 'Masquer les journaux', 'launch.openFolder': 'Ouvrir le dossier', 'launch.close': 'Fermer',
    'launch.cancel': 'Annuler', 'launch.liveLog': 'Journal en direct', 'launch.copy': 'Copier', 'launch.clear': 'Vider',
    'launch.waiting': 'En attente de données…',
    'update.updating': 'Mise à jour en cours… {percent}%', 'update.ready': 'Mise à jour {version} prête.',
    'update.installBtn': 'Installer & redémarrer',
    'toast.langSaved': 'Langue enregistrée', 'toast.loginUnavailable': 'Connexion indisponible',
    'toast.loginWindow': 'Fenêtre de connexion Microsoft…', 'toast.connectedPrefix': 'Connecté : ',
    'toast.loginCancelled': 'Connexion annulée', 'toast.loginError': 'Erreur connexion : ',
    'toast.loggedOut': 'Déconnecté', 'toast.launchUnavailable': 'Lancement indisponible',
    'toast.folderOpened': 'Dossier ouvert', 'toast.modsFolderOpened': 'Dossier des mods ouvert',
    'toast.folderOpenFailed': "Impossible d'ouvrir le dossier", 'toast.logCopied': 'Journal copié',
    'toast.logCopyFailed': 'Copie impossible', 'toast.updateChecking': 'Recherche de mise à jour…'
  },
  en: {
    'nav.accueil': 'Home', 'nav.actus': 'News', 'nav.cosmetiques': 'Cosmetics', 'nav.amis': 'Friends',
    'nav.mods': 'Mods', 'nav.langues': 'Languages', 'nav.themes': 'Themes', 'nav.settings': 'Settings',
    'window.minimize': 'Minimize', 'window.maximize': 'Maximize', 'window.close': 'Close',
    'account.loginMs': 'Log in with Microsoft', 'account.myAccount': 'My Microsoft account',
    'account.changeSkin': 'Change skin', 'account.switchAccount': 'Switch account',
    'account.screenshots': 'Screenshots', 'account.settings': 'Account settings',
    'account.logout': 'Log out', 'account.online': 'Online', 'account.offline': 'Signed out',
    'home.tagline': 'THE OLYMPUS AWAITS YOU', 'home.launch': 'LAUNCH',
    'home.onlineChip': 'Online · {online}/{max} players', 'home.offlineChip': 'Offline',
    'home.seeAll': 'See all ›',
    'actus.subtitle': 'All HEROES-WORLD server announcements.', 'actus.prev': 'Previous', 'actus.next': 'Next',
    'cosmetics.subtitle': 'Preview of your character. The catalogue is coming with the shop.',
    'cosmetics.notice': 'Cosmetics (capes, auras, companions…) will arrive with the HEROES-WORLD shop.',
    'cosmetics.previewSub': 'Preview of your current skin.', 'cosmetics.searchPlaceholder': 'Search a cosmetic…',
    'cosmetics.soonBadge': 'Soon',
    'friends.subtitle': 'Your HEROES-WORLD friends list.', 'friends.tabOnline': 'Online', 'friends.tabOffline': 'Offline',
    'friends.tabRequests': 'Requests', 'friends.emptyTitle': 'No friends yet.',
    'friends.emptySub': 'The friends list will be available once the HEROES-WORLD server is hosted.',
    'mods.subtitle': 'Fabric 0.19.3 · Minecraft 1.20.6', 'mods.searchPlaceholder': 'Search a mod…',
    'mods.openFolderBtn': 'Open mods folder', 'mods.restartHint': 'Restart the launcher to apply the changes.',
    'mods.groupClient': 'Client', 'mods.groupMinecraft': 'Minecraft', 'mods.groupLibraries': 'Libraries',
    'mods.alwaysActive': 'Always active', 'mods.emptyList': 'No custom mod detected in the game folder.',
    'mods.version': 'Version', 'mods.author': 'Author', 'mods.size': 'Size', 'mods.group': 'Group',
    'mods.selectPrompt': 'Select a mod to see its details.',
    'langues.subtitle': 'Choose the launcher interface language.', 'langues.searchPlaceholder': 'Search a language…',
    'common.comingSoon': 'Coming soon',
    'themes.subtitle': "Change the launcher's visual atmosphere.", 'themes.active': 'Active',
    'theme.olympe.name': 'Olympe', 'theme.olympe.desc': 'Golden lightning',
    'theme.jupiter.name': 'Jupiter', 'theme.jupiter.desc': 'Space & planet',
    'theme.nuit.name': 'Starry night', 'theme.nuit.desc': 'Deep sky',
    'theme.aurore.name': 'Aurora', 'theme.aurore.desc': 'Polar lights',
    'theme.sombre.name': 'Dark', 'theme.sombre.desc': 'Minimal & clean',
    'settings.memoryTitle': 'Memory', 'settings.ramLabel': 'Allocated RAM', 'settings.ramUnit': 'GB',
    'settings.ramNote': 'Recommended: 4 GB.', 'settings.displayTitle': 'Display', 'settings.resolutionLabel': 'Resolution',
    'settings.folderTitle': 'Game folder', 'settings.openFolderBtn': 'Open game folder',
    'settings.javaTitle': 'Java (optional)', 'settings.javaPlaceholder': 'Path to javaw.exe (auto if empty)',
    'settings.accountTitle': 'Account', 'settings.connectedAs': 'Connected: {name}.', 'settings.logoutBtn': 'Log out',
    'settings.loginBtn': 'Log in', 'settings.launcherTitle': 'Launcher', 'settings.versionLabel': 'Version {v}',
    'settings.checkUpdateBtn': 'Check for updates',
    'launch.phase.prepare': 'Preparing…', 'launch.phase.meta': 'Checking files…',
    'launch.phase.install': 'Downloading Minecraft…', 'launch.phase.fabric': 'Installing Fabric…',
    'launch.phase.deps': 'Installing dependencies…', 'launch.phase.ready': 'Ready',
    'launch.phase.auth': 'Authenticating…', 'launch.phase.launch': 'Launching the game…',
    'launch.phase.done': 'Olympus joined!', 'launch.phase.warn': 'Warning', 'launch.phase.error': 'Error',
    'launch.phase.exit': 'Game closed', 'launch.phase.default': 'Starting…',
    'launch.failedTitle': 'Launch failed', 'launch.showLogs': 'Show logs',
    'launch.hideLogs': 'Hide logs', 'launch.openFolder': 'Open folder', 'launch.close': 'Close',
    'launch.cancel': 'Cancel', 'launch.liveLog': 'Live log', 'launch.copy': 'Copy', 'launch.clear': 'Clear',
    'launch.waiting': 'Waiting for data…',
    'update.updating': 'Update in progress… {percent}%', 'update.ready': 'Update {version} ready.',
    'update.installBtn': 'Install & restart',
    'toast.langSaved': 'Language saved', 'toast.loginUnavailable': 'Login unavailable',
    'toast.loginWindow': 'Opening Microsoft login window…', 'toast.connectedPrefix': 'Connected: ',
    'toast.loginCancelled': 'Login cancelled', 'toast.loginError': 'Login error: ',
    'toast.loggedOut': 'Logged out', 'toast.launchUnavailable': 'Launch unavailable',
    'toast.folderOpened': 'Folder opened', 'toast.modsFolderOpened': 'Mods folder opened',
    'toast.folderOpenFailed': 'Could not open the folder', 'toast.logCopied': 'Log copied',
    'toast.logCopyFailed': 'Copy failed', 'toast.updateChecking': 'Checking for updates…'
  }
}
function t (key) {
  const dict = messages[lang.value] || messages.fr
  return dict[key] || messages.fr[key] || key
}
function setLang (code) {
  if (code !== 'fr' && code !== 'en') return
  if (lang.value === code) return
  lang.value = code
  localStorage.setItem('hwLang', code)
  toast(t('toast.langSaved'))
  ulog('langue -> ' + code)
}

/* ===================== Thème ===================== */
const themesList = [
  { id: 'olympe' }, { id: 'jupiter' }, { id: 'nuit' }, { id: 'aurore' }, { id: 'sombre' }
]
const theme = ref(localStorage.getItem('hwTheme') || 'jupiter')
const themeSwitching = ref(false)
function setTheme (id) {
  if (theme.value === id || themeSwitching.value) return
  ulog('theme -> ' + id)
  themeSwitching.value = true
  setTimeout(() => { theme.value = id; localStorage.setItem('hwTheme', id) }, 260)
  setTimeout(() => { themeSwitching.value = false }, 620)
}

/* ===================== Navigation ===================== */
const navItems = [
  { id: 'accueil', icon: 'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z' },
  { id: 'actus', icon: 'M4 11h9v2H4zM4 7h13v2H4zM4 15h6v2H4zM19 5H2v14h17a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z' },
  { id: 'cosmetiques', icon: 'M12 2l2.4 3.6L18 4l-.6 3.9L21 10l-3.6 1.4L18 15l-3.9-.6L12 18l-2.1-3.6L6 15l.6-3.9L3 10l3.6-1.4L6 4l3.9 1.6z' },
  { id: 'amis', icon: 'M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z' },
  { id: 'mods', icon: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z' },
  { id: 'langues', icon: 'M12 2a10 10 0 1 0 .001 20.001A10 10 0 0 0 12 2zm7.94 9h-3.05a15.7 15.7 0 0 0-1.32-5.47A8.03 8.03 0 0 1 19.94 11zM12 4.06c.98 1.4 1.72 3.24 2.02 5.94H9.98c.3-2.7 1.04-4.54 2.02-5.94zM4.06 13h3.05c.16 2.05.62 3.9 1.32 5.47A8.03 8.03 0 0 1 4.06 13zm3.05-2H4.06a8.03 8.03 0 0 1 4.37-5.47A15.7 15.7 0 0 0 7.11 11zM12 19.94c-.98-1.4-1.72-3.24-2.02-5.94h4.04c-.3 2.7-1.04 4.54-2.02 5.94zm2.63-1.47c.7-1.57 1.16-3.42 1.32-5.47h3.05a8.03 8.03 0 0 1-4.37 5.47z' },
  { id: 'settings', icon: 'M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54A.48.48 0 0 0 14 2h-4a.48.48 0 0 0-.48.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.49.49 0 0 0-.59.22L2.65 8.47a.49.49 0 0 0 .12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h4c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z' }
]
const MC_VER = '1.20.6'
const LOADER_VER = '0.19.3'
const page = ref('accueil')
const pageLabel = computed(() => t('nav.' + page.value))
function go (p) { page.value = p; acctOpen.value = false; ulog('page -> ' + p) }

/* ===================== Serveur ===================== */
const serverOnline = ref(false)
const players = reactive({ online: 0, max: 0 })
let srvTimer = null

/* ===================== Compte (Microsoft réel) ===================== */
const acctOpen = ref(false)
const account = reactive({ online: false })
const headSrc = computed(() => `https://mc-heads.net/head/${CONFIG.uuid || CONFIG.pseudo}`)
const bodySrc = computed(() => `https://mc-heads.net/body/${CONFIG.uuid || CONFIG.pseudo}`)
function toggleAcct () { acctOpen.value = !acctOpen.value }
function ext (url) { if (window.hw && window.hw.openExternal) window.hw.openExternal(url); acctOpen.value = false }
async function refreshAccount () {
  if (!(window.hw && window.hw.getAccount)) return
  try {
    const a = await window.hw.getAccount()
    if (a && a.name) { CONFIG.pseudo = a.name; CONFIG.uuid = a.uuid; account.online = true }
    else { account.online = false }
  } catch (_) {}
}
async function login () {
  ulog('login click')
  if (!(window.hw && window.hw.login)) { toast(t('toast.loginUnavailable')); return }
  toast(t('toast.loginWindow'))
  try {
    const r = await window.hw.login()
    if (r && r.ok) {
      CONFIG.pseudo = r.name; CONFIG.uuid = r.uuid; account.online = true; acctOpen.value = false
      toast(t('toast.connectedPrefix') + r.name); ulog('login ok -> ' + r.name)
    } else {
      toast(t('toast.loginCancelled') + (r && r.error ? ' (' + r.error + ')' : '')); ulog('login cancelled -> ' + JSON.stringify(r))
    }
  } catch (e) { toast(t('toast.loginError') + e); ulog('login error -> ' + e) }
}
async function logout () {
  ulog('logout click')
  acctOpen.value = false
  try { if (window.hw && window.hw.logout) await window.hw.logout() } catch (_) {}
  account.online = false; CONFIG.uuid = null; toast(t('toast.loggedOut'))
}
async function openFolder (sub) {
  if (!(window.hw && window.hw.openGameFolder)) return { ok: false, error: 'unavailable' }
  const r = await window.hw.openGameFolder(settings.dir, sub)
  ulog('open folder' + (sub ? ' (' + sub + ')' : '') + ' -> ' + JSON.stringify(r))
  return r
}
async function openGameFolderBtn () {
  const r = await openFolder()
  toast(r && r.ok ? t('toast.folderOpened') : t('toast.folderOpenFailed'))
}
function onDocDown (e) { if (acctOpen.value && !e.target.closest('.acct-chip')) acctOpen.value = false }

/* ===================== Journal / logs ===================== */
const logLines = ref([])
const logsOpen = ref(false)
function scrollLog () { nextTick(() => { const el = document.querySelector('.ov-logpre'); if (el) el.scrollTop = el.scrollHeight }) }
function pushLog (s) { logLines.value.push(s); if (logLines.value.length > 900) logLines.value.splice(0, logLines.value.length - 900); if (logsOpen.value) scrollLog() }
function lineClass (s) {
  const l = ('' + s).toLowerCase()
  if (l.includes('error') || l.includes('exception') || l.includes('crash') || l.includes('caused by') || l.includes('échec') || l.includes('erreur') || l.includes('fatal') || l.includes('failed')) return 'lg-err'
  if (l.includes('warn') || l.includes('avertiss') || l.includes('deprecat')) return 'lg-warn'
  if (l.includes('[mc:game]') || l.includes('[game]')) return 'lg-game'
  return ''
}
function copyLog () { try { navigator.clipboard.writeText(logLines.value.join('\n')); toast(t('toast.logCopied')) } catch (_) { toast(t('toast.logCopyFailed')) } }
function clearLog () { logLines.value = [] }

/* ===================== Lancement ===================== */
const launchInfo = reactive({ active: false, phase: '', percent: 0, text: '', error: '' })
const launchPhaseLabel = computed(() => {
  const map = {
    prepare: t('launch.phase.prepare'), meta: t('launch.phase.meta'), install: t('launch.phase.install'),
    fabric: t('launch.phase.fabric'), deps: t('launch.phase.deps'), ready: t('launch.phase.ready'),
    auth: t('launch.phase.auth'), launch: t('launch.phase.launch'), done: t('launch.phase.done'),
    warn: t('launch.phase.warn'), error: t('launch.phase.error'), exit: t('launch.phase.exit')
  }
  return map[launchInfo.phase] || launchInfo.text || t('launch.phase.default')
})
function launch () {
  ulog('LANCER cliqué')
  if (launchInfo.active) return
  if (!window.hw || !window.hw.launch) { toast(t('toast.launchUnavailable')); return }
  launchInfo.active = true; launchInfo.phase = 'prepare'; launchInfo.percent = 0; launchInfo.text = ''; launchInfo.error = ''
  window.hw.launch({ dir: settings.dir, ram: settings.ram, name: CONFIG.pseudo })
    .then((r) => { if (!r || !r.ok) { launchInfo.phase = 'error'; launchInfo.error = (r && r.error) || 'Échec inconnu' } })
    .catch((e) => { launchInfo.phase = 'error'; launchInfo.error = '' + e })
}
async function cancelLaunch () {
  ulog('cancel launch')
  try { if (window.hw && window.hw.cancelLaunch) await window.hw.cancelLaunch() } catch (_) {}
  launchInfo.active = false; launchInfo.phase = ''; launchInfo.percent = 0
}
function closeLaunchOverlay () { launchInfo.active = false; launchInfo.phase = '' }
if (typeof window !== 'undefined' && window.hw && window.hw.onMcStatus) {
  window.hw.onMcStatus((d) => {
    if (d.phase) launchInfo.phase = d.phase
    if (d.text) launchInfo.text = d.text
    if (typeof d.percent === 'number') launchInfo.percent = d.percent
    ulog('mc status -> ' + (d.phase || '') + (d.text ? (' ' + d.text) : ''))
    if (d.phase === 'error' || d.phase === 'exit') launchInfo.error = d.text || 'Le lancement a été interrompu.'
    else if (d.phase === 'done') setTimeout(() => { launchInfo.active = false; launchInfo.phase = '' }, 1800)
  })
}

/* ===================== Mods ===================== */
const MOD_META = {
  'heroworld-titlescreen': { name: 'HeroesWorld Title Screen', version: '1.0.0', author: 'HEROES-WORLD', desc: "Menu principal personnalisé du client : écran titre sans Solo, connexion directe, habillage Olympe." }
}
const MOD_GROUP_LABELS = { Client: 'mods.groupClient', Minecraft: 'mods.groupMinecraft', 'Bibliothèques': 'mods.groupLibraries' }
const rawMods = ref([])
const modsSearch = ref('')
const modToggles = reactive(JSON.parse(localStorage.getItem('hwModToggles') || '{}'))
const restartNeeded = ref(false)
const selectedMod = ref(null)
async function loadMods () {
  if (!(window.hw && window.hw.modsList)) return
  try { rawMods.value = (await window.hw.modsList(settings.dir)) || [] } catch (_) { rawMods.value = [] }
}
function fmtSize (b) { if (b === undefined || b === null) return ''; if (b < 1024) return b + ' o'; if (b < 1048576) return (b / 1024).toFixed(1) + ' Ko'; return (b / 1048576).toFixed(1) + ' Mo' }
const modsMerged = computed(() => {
  const client = rawMods.value.map(m => {
    const base = m.file.replace(/\.jar$/i, '')
    const meta = MOD_META[base] || {}
    return {
      id: base, name: meta.name || base, version: meta.version || '—', author: meta.author || 'Inconnu',
      desc: meta.desc || 'Mod client détecté dans le dossier mods.', group: 'Client', size: fmtSize(m.size), toggleable: true
    }
  })
  const core = [
    { id: 'minecraft', name: 'Minecraft', version: '1.20.6', author: 'Mojang Studios', desc: 'Version de jeu utilisée par HEROES-WORLD.', group: 'Minecraft', size: '', toggleable: false },
    { id: 'fabric-loader', name: 'Fabric Loader', version: '0.19.3', author: 'FabricMC', desc: 'Chargeur de mods officiel Fabric pour Minecraft 1.20.6.', group: 'Bibliothèques', size: '', toggleable: false }
  ]
  return [...client, ...core]
})
const modsFiltered = computed(() => {
  const q = modsSearch.value.trim().toLowerCase()
  const list = q ? modsMerged.value.filter(m => m.name.toLowerCase().includes(q)) : modsMerged.value
  const groups = { Client: [], Minecraft: [], 'Bibliothèques': [] }
  list.forEach(m => { groups[m.group].push(m) })
  return groups
})
const selectedModObj = computed(() => modsMerged.value.find(m => m.id === selectedMod.value) || null)
function isModOn (m) { return modToggles[m.id] !== false }
function toggleMod (m) {
  if (!m.toggleable) return
  modToggles[m.id] = !isModOn(m)
  localStorage.setItem('hwModToggles', JSON.stringify(modToggles))
  restartNeeded.value = true
}
async function openModsFolderBtn () {
  const r = await openFolder('mods')
  toast(r && r.ok ? t('toast.modsFolderOpened') : t('toast.folderOpenFailed'))
}

/* ===================== Cosmétiques (aperçu honnête, rien n'équipe réellement) ===================== */
const cosmeticCategories = ['Capes', 'Casques', 'Ailes', 'Compagnons', 'Auras', 'Costumes', 'Emotes', 'Sprays', 'Skins', 'Exclusifs HW']
const mythSuffixes = ["de Zeus", "d'Athéna", "d'Apollon", "de Poséidon", "d'Hadès", "d'Hermès", "d'Arès", "d'Artémis", "d'Héra", "de Cronos", "de l'Olympe", "des Titans", "de Perséphone", "d'Héphaïstos"]
function buildCosmetics () {
  const store = {}
  cosmeticCategories.forEach((cat, ci) => {
    const singular = cat.endsWith('s') ? cat.slice(0, -1) : cat
    const count = 6 + (ci % 4)
    const items = []
    for (let i = 0; i < count; i++) {
      items.push({ id: cat + '-' + i, name: singular + ' ' + mythSuffixes[(i + ci * 3) % mythSuffixes.length] })
    }
    store[cat] = items
  })
  return store
}
const cosmetics = reactive(buildCosmetics())
const cosCategory = ref('Capes')
const cosSearch = ref('')
const cosItemsFiltered = computed(() => {
  let list = cosmetics[cosCategory.value] || []
  if (cosSearch.value.trim()) { const q = cosSearch.value.trim().toLowerCase(); list = list.filter(i => i.name.toLowerCase().includes(q)) }
  return [...list].sort((a, b) => a.name.localeCompare(b.name))
})

/* ===================== Amis (aucune donnée fictive) ===================== */
const friendsTab = ref('online')

/* ===================== Langues ===================== */
const languages = [
  { code: 'fr', name: 'Français', enabled: true }, { code: 'en', name: 'English', enabled: true },
  { code: 'es', name: 'Español', enabled: false }, { code: 'de', name: 'Deutsch', enabled: false },
  { code: 'it', name: 'Italiano', enabled: false }, { code: 'pt', name: 'Português', enabled: false },
  { code: 'nl', name: 'Nederlands', enabled: false }, { code: 'pl', name: 'Polski', enabled: false },
  { code: 'ru', name: 'Русский', enabled: false }, { code: 'ja', name: '日本語', enabled: false },
  { code: 'zh', name: '简体中文', enabled: false }, { code: 'ko', name: '한국어', enabled: false }
]
const langSearch = ref('')
const languagesFiltered = computed(() => {
  const q = langSearch.value.trim().toLowerCase()
  return q ? languages.filter(l => l.name.toLowerCase().includes(q)) : languages
})

/* ===================== Ambiance (ciel / météores) ===================== */
function starField (n, tile, r, op) {
  const g = []
  for (let i = 0; i < n; i++) {
    const x = Math.round(Math.random() * tile), y = Math.round(Math.random() * tile)
    const o = (op * (0.5 + Math.random() * 0.5)).toFixed(2)
    g.push(`radial-gradient(${r}px ${r}px at ${x}px ${y}px, rgba(255,255,255,${o}), transparent 60%)`)
  }
  return { backgroundImage: g.join(','), backgroundSize: `${tile}px ${tile}px` }
}
const stars1 = starField(40, 340, 1.4, 0.7)
const stars2 = starField(28, 560, 1.0, 0.5)
const meteors = ref([])
let meteorId = 0, meteorTimer = null
function spawnMeteor () {
  const id = ++meteorId
  const fromLeft = Math.random() > 0.5
  const sx = fromLeft ? -15 : 115, ex = fromLeft ? 115 : -15
  const sy = 3 + Math.random() * 24, ey = sy + 20 + Math.random() * 22
  const ang = Math.atan2(ey - sy, ex - sx) * 180 / Math.PI
  const dur = 2.6 + Math.random() * 1.8, sc = 0.4 + Math.random() * 0.4
  const style = { '--sx': sx + 'vw', '--sy': sy + 'vh', '--ex': ex + 'vw', '--ey': ey + 'vh', '--ang': ang + 'deg', '--sc': sc, animationDuration: dur + 's' }
  meteors.value.push({ id, style })
  setTimeout(() => { meteors.value = meteors.value.filter(m => m.id !== id) }, dur * 1000 + 400)
}
function scheduleMeteor () { meteorTimer = setTimeout(() => { if (!document.hidden) spawnMeteor(); scheduleMeteor() }, 11000 + Math.random() * 15000) }

/* ===================== Mise à jour / toast ===================== */
const upd = reactive({ state: null, percent: 0, version: '' })
if (typeof window !== 'undefined' && window.hw && window.hw.onUpdate) {
  window.hw.onUpdate((d) => {
    if (d.type === 'available') { upd.state = 'downloading'; upd.version = d.version || '' }
    else if (d.type === 'progress') { upd.state = 'downloading'; upd.percent = d.percent || 0 }
    else if (d.type === 'ready') { upd.state = 'ready'; upd.version = d.version || '' }
    else if (d.type === 'error') { upd.state = null }
  })
}
function installUpdate () { if (window.hw && window.hw.installUpdate) { window.hw.installUpdate(); ulog('install update') } }
function checkUpdate () { if (window.hw && window.hw.checkUpdate) { window.hw.checkUpdate(); toast(t('toast.updateChecking')); ulog('check update') } }

const toastMsg = ref('')
let toastTimer = null
function toast (m) { toastMsg.value = m; clearTimeout(toastTimer); toastTimer = setTimeout(() => (toastMsg.value = ''), 2400) }

/* ===================== Actus (lecteur) ===================== */
const actuIndex = ref(0)

/* ===================== Clavier / fenêtre / cycle de vie ===================== */
function onKey (e) {
  if (e.key !== 'Escape') return
  if (acctOpen.value) { acctOpen.value = false; return }
  if (launchInfo.active && (launchInfo.phase === 'error' || launchInfo.phase === 'exit')) { closeLaunchOverlay(); return }
  if (page.value !== 'accueil') go('accueil')
}
function winMin () { if (window.hw) window.hw.minimize() }
function winMax () { if (window.hw) window.hw.maximize() }
function winClose () { if (window.hw) window.hw.close() }

function onGlobalError (e) { ulog('JS ERROR: ' + (e.message || '') + ' @' + (e.filename || '') + ':' + (e.lineno || '')) }
function onUnhandledRejection (e) { ulog('PROMISE REJECT: ' + ((e.reason && (e.reason.message || e.reason)) || '')) }

onMounted(() => {
  document.addEventListener('keydown', onKey)
  document.addEventListener('mousedown', onDocDown)
  window.addEventListener('error', onGlobalError)
  window.addEventListener('unhandledrejection', onUnhandledRejection)
  meteorTimer = setTimeout(scheduleMeteor, 5000)
  refreshAccount()
  loadMods()
  if (window.hw && window.hw.onLogLine) window.hw.onLogLine(pushLog)
  const pingSrv = async () => {
    try {
      if (window.hw && window.hw.serverStatus) {
        const s = await window.hw.serverStatus()
        serverOnline.value = !!(s && s.online)
        if (s && s.players) { players.online = s.players.online || 0; players.max = s.players.max || 0 }
      } else if (window.hw && window.hw.serverOnline) {
        serverOnline.value = !!(await window.hw.serverOnline())
      }
    } catch (_) {}
  }
  pingSrv(); srvTimer = setInterval(pingSrv, 5000)
  ulog('App.vue monté')
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKey)
  document.removeEventListener('mousedown', onDocDown)
  window.removeEventListener('error', onGlobalError)
  window.removeEventListener('unhandledrejection', onUnhandledRejection)
  clearTimeout(meteorTimer)
  clearInterval(srvTimer)
})
</script>

<template>
  <div class="titlebar" @dblclick="winMax">
    <span class="tb-title">⚡ HEROES-WORLD</span>
    <div class="tb-controls">
      <button @click="winMin" :aria-label="t('window.minimize')">─</button>
      <button @click="winMax" :aria-label="t('window.maximize')">▢</button>
      <button class="close" @click="winClose" :aria-label="t('window.close')">✕</button>
    </div>
  </div>

  <div class="bg" :class="['theme-' + theme, { switching: themeSwitching }]">
    <div class="sky" :style="{ backgroundImage: `url(${sky})` }"></div>
    <div class="stars" :style="stars1"></div>
    <div class="stars s2" :style="stars2"></div>
    <div v-for="m in meteors" :key="m.id" class="meteor" :style="m.style"></div>
    <div class="lightning"></div>
    <div class="planet-glow"></div>
    <img class="planet" :src="planet" alt="" />
    <div class="planet-veil"></div>
    <div class="scrim"></div>
  </div>

  <div class="shell">
    <aside class="sidebar">
      <div class="side-brand"><img :src="logo" alt="HEROES-WORLD" /></div>
      <nav class="side-nav">
        <button v-for="n in navItems" :key="n.id" class="side-item" :class="{ active: page === n.id }" :title="t('nav.' + n.id)" @click="go(n.id)">
          <svg class="side-ic" viewBox="0 0 24 24"><path :d="n.icon" /></svg>
          <span class="side-label">{{ t('nav.' + n.id) }}</span>
        </button>
      </nav>
      <div class="side-foot">
        <span class="sf-dot" :class="{ on: serverOnline }"></span>
        <span class="side-label sf-txt">Fabric {{ LOADER_VER }} · MC {{ MC_VER }}</span>
      </div>
    </aside>

    <div class="main">
      <div class="topbar">
        <div class="tb-crumb">{{ pageLabel }}</div>
        <div class="acct-chip" :class="{ open: acctOpen }" @click="toggleAcct">
          <span class="acct-dot" :class="{ on: account.online }"></span>
          <span class="acct-name" :style="{ color: nameColor }">{{ CONFIG.pseudo }}</span>
          <img class="acct-head" :src="headSrc" draggable="false" alt="" />
          <transition name="pop">
            <div v-if="acctOpen" class="acct-pop" @click.stop>
              <div class="ap-head">
                <img :src="headSrc" alt="" />
                <div>
                  <div class="ap-name" :style="{ color: nameColor }">{{ CONFIG.pseudo }}</div>
                  <div class="ap-status" :class="{ on: account.online }"><i></i>{{ account.online ? t('account.online') : t('account.offline') }}</div>
                </div>
              </div>
              <button v-if="!account.online" class="ap-item login" @click="login">{{ t('account.loginMs') }}</button>
              <template v-else>
                <button class="ap-item" @click="ext('https://account.microsoft.com')">{{ t('account.myAccount') }}</button>
                <button class="ap-item" @click="ext('https://www.minecraft.net/fr-fr/msaprofile/mygames/editskin')">{{ t('account.changeSkin') }}</button>
                <button class="ap-item" @click="login">{{ t('account.switchAccount') }}</button>
                <button class="ap-item" @click="openGameFolderBtn">{{ t('account.screenshots') }}</button>
                <button class="ap-item" @click="go('settings')">{{ t('account.settings') }}</button>
                <div class="ap-sep"></div>
                <button class="ap-item danger" @click="logout">{{ t('account.logout') }}</button>
              </template>
            </div>
          </transition>
        </div>
      </div>

      <div class="content">
        <!-- ACCUEIL -->
        <section v-if="page === 'accueil'" class="pg pg-home">
          <div class="home-center">
            <img class="brand-logo" :src="logo" alt="HEROES-WORLD" draggable="false" />
            <div class="brand-tagline">{{ t('home.tagline') }}</div>
            <button class="btn-launch" :class="{ busy: launchInfo.active }" :disabled="launchInfo.active" @click="launch">
              <template v-if="!launchInfo.active">{{ t('home.launch') }}</template>
              <template v-else>{{ launchPhaseLabel }}</template>
            </button>
            <div class="home-chips">
              <div class="chip" :class="{ on: serverOnline }">
                <span class="chip-dot"></span>
                <span v-if="serverOnline">{{ t('home.onlineChip').replace('{online}', players.online).replace('{max}', players.max) }}</span>
                <span v-else>{{ t('home.offlineChip') }}</span>
              </div>
            </div>
          </div>
          <div class="home-news">
            <div class="home-news-head"><span>{{ t('nav.actus') }}</span><button @click="go('actus')">{{ t('home.seeAll') }}</button></div>
            <div class="home-news-grid">
              <article v-for="(a, i) in CONFIG.actus.slice(0, 3)" :key="i" class="news-card" @click="go('actus')">
                <div class="nc-tag">{{ a.tag }}</div>
                <h3>{{ a.titre }}</h3>
                <p>{{ a.texte }}</p>
                <div class="nc-date">{{ a.date }}</div>
              </article>
            </div>
          </div>
        </section>

        <!-- ACTUALITÉS -->
        <section v-else-if="page === 'actus'" class="pg">
          <div class="pg-head"><h1>{{ t('nav.actus') }}</h1><p>{{ t('actus.subtitle') }}</p></div>
          <div class="actus-body">
            <aside class="actus-list">
              <button v-for="(a, i) in CONFIG.actus" :key="i" class="actus-item" :class="{ active: actuIndex === i }" @click="actuIndex = i">
                <span class="ai-tag">{{ a.tag }}</span>
                <span class="ai-title">{{ a.titre }}</span>
                <span class="ai-date">{{ a.date }}</span>
              </button>
            </aside>
            <article class="actus-reader panel">
              <div class="ar-tag">{{ CONFIG.actus[actuIndex].tag }}</div>
              <h2>{{ CONFIG.actus[actuIndex].titre }}</h2>
              <div class="ar-meta">{{ CONFIG.actus[actuIndex].date }} · HEROES-WORLD</div>
              <p>{{ CONFIG.actus[actuIndex].texte }}</p>
              <div class="ar-pager">
                <button :disabled="actuIndex === 0" @click="actuIndex--">‹ {{ t('actus.prev') }}</button>
                <span>{{ actuIndex + 1 }} / {{ CONFIG.actus.length }}</span>
                <button :disabled="actuIndex === CONFIG.actus.length - 1" @click="actuIndex++">{{ t('actus.next') }} ›</button>
              </div>
            </article>
          </div>
        </section>

        <!-- COSMÉTIQUES -->
        <section v-else-if="page === 'cosmetiques'" class="pg">
          <div class="pg-head"><h1>{{ t('nav.cosmetiques') }}</h1><p>{{ t('cosmetics.subtitle') }}</p></div>
          <div class="cos-notice panel">{{ t('cosmetics.notice') }}</div>
          <div class="cos-body">
            <aside class="cos-cats">
              <button v-for="c in cosmeticCategories" :key="c" class="cos-cat" :class="{ active: cosCategory === c }" @click="cosCategory = c">{{ c }}</button>
            </aside>
            <div class="cos-preview panel">
              <img :src="bodySrc" alt="Aperçu du skin" class="cos-body-img" draggable="false" />
              <div class="cos-preview-name" :style="{ color: nameColor }">{{ CONFIG.pseudo }}</div>
              <div class="cos-preview-sub">{{ t('cosmetics.previewSub') }}</div>
            </div>
            <div class="cos-grid-wrap">
              <div class="cos-controls">
                <input class="ipt" type="text" v-model="cosSearch" :placeholder="t('cosmetics.searchPlaceholder')" />
              </div>
              <div class="cos-grid">
                <div v-for="it in cosItemsFiltered" :key="it.id" class="cos-item soon">
                  <div class="ci-name">{{ it.name }}</div>
                  <span class="ci-badge soon">{{ t('cosmetics.soonBadge') }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- AMIS -->
        <section v-else-if="page === 'amis'" class="pg">
          <div class="pg-head"><h1>{{ t('nav.amis') }}</h1><p>{{ t('friends.subtitle') }}</p></div>
          <div class="friends-tabs">
            <button :class="{ active: friendsTab === 'online' }" @click="friendsTab = 'online'">{{ t('friends.tabOnline') }}</button>
            <button :class="{ active: friendsTab === 'offline' }" @click="friendsTab = 'offline'">{{ t('friends.tabOffline') }}</button>
            <button :class="{ active: friendsTab === 'demandes' }" @click="friendsTab = 'demandes'">{{ t('friends.tabRequests') }}</button>
          </div>
          <div class="friends-list">
            <div class="friends-empty panel">
              <p class="fe-title">{{ t('friends.emptyTitle') }}</p>
              <p class="fe-sub">{{ t('friends.emptySub') }}</p>
            </div>
          </div>
        </section>

        <!-- MODS -->
        <section v-else-if="page === 'mods'" class="pg">
          <div class="pg-head"><h1>{{ t('nav.mods') }}</h1><p>{{ t('mods.subtitle') }}</p></div>
          <div class="mods-toolbar">
            <input class="ipt" type="text" v-model="modsSearch" :placeholder="t('mods.searchPlaceholder')" />
            <button class="btn-sm ghost" @click="openModsFolderBtn">{{ t('mods.openFolderBtn') }}</button>
          </div>
          <div v-if="restartNeeded" class="mods-hint">{{ t('mods.restartHint') }}</div>
          <div class="mods-body">
            <div class="mods-list">
              <template v-for="grp in ['Client', 'Minecraft', 'Bibliothèques']" :key="grp">
                <div v-if="modsFiltered[grp] && modsFiltered[grp].length" class="mods-group">
                  <div class="mg-title">{{ t(MOD_GROUP_LABELS[grp]) }}</div>
                  <div v-for="m in modsFiltered[grp]" :key="m.id" class="mod-row" :class="{ active: selectedMod === m.id }" @click="selectedMod = m.id">
                    <div class="mr-main"><div class="mr-name">{{ m.name }}</div><div class="mr-sub">v{{ m.version }} · {{ m.author }}</div></div>
                    <label v-if="m.toggleable" class="switch" @click.stop>
                      <input type="checkbox" :checked="isModOn(m)" @change="toggleMod(m)" />
                      <span class="switch-track"></span>
                    </label>
                    <span v-else class="mr-static">{{ t('mods.alwaysActive') }}</span>
                  </div>
                </div>
              </template>
              <p v-if="!rawMods.length" class="mods-empty">{{ t('mods.emptyList') }}</p>
            </div>
            <div class="mods-detail panel">
              <template v-if="selectedModObj">
                <h3>{{ selectedModObj.name }}</h3>
                <div class="md-row"><span>{{ t('mods.version') }}</span><b>{{ selectedModObj.version }}</b></div>
                <div class="md-row"><span>{{ t('mods.author') }}</span><b>{{ selectedModObj.author }}</b></div>
                <div class="md-row" v-if="selectedModObj.size"><span>{{ t('mods.size') }}</span><b>{{ selectedModObj.size }}</b></div>
                <div class="md-row"><span>{{ t('mods.group') }}</span><b>{{ selectedModObj.group }}</b></div>
                <p class="md-desc">{{ selectedModObj.desc }}</p>
              </template>
              <p v-else class="md-empty">{{ t('mods.selectPrompt') }}</p>
            </div>
          </div>
        </section>

        <!-- LANGUES -->
        <section v-else-if="page === 'langues'" class="pg">
          <div class="pg-head"><h1>{{ t('nav.langues') }}</h1><p>{{ t('langues.subtitle') }}</p></div>
          <input class="ipt lang-search" type="text" v-model="langSearch" :placeholder="t('langues.searchPlaceholder')" />
          <div class="lang-list">
            <button v-for="l in languagesFiltered" :key="l.code" class="lang-row" :class="{ active: lang === l.code, disabled: !l.enabled }" :disabled="!l.enabled" @click="l.enabled && setLang(l.code)">
              <span>{{ l.name }}</span>
              <span v-if="!l.enabled" class="lang-soon">{{ t('common.comingSoon') }}</span>
              <svg v-else-if="lang === l.code" class="lang-check" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5" /></svg>
            </button>
          </div>
        </section>

        <!-- PARAMÈTRES -->
        <section v-else-if="page === 'settings'" class="pg">
          <div class="pg-head"><h1>{{ t('nav.settings') }}</h1></div>
          <div class="set-grid">
            <div class="panel set-block">
              <h3>{{ t('settings.memoryTitle') }}</h3>
              <div class="set-row"><label>{{ t('settings.ramLabel') }}</label><input type="range" min="2" max="12" v-model.number="settings.ram" /><span class="set-val">{{ settings.ram }} {{ t('settings.ramUnit') }}</span></div>
              <p class="set-note">{{ t('settings.ramNote') }}</p>
            </div>
            <div class="panel set-block">
              <h3>{{ t('settings.displayTitle') }}</h3>
              <div class="set-row"><label>{{ t('settings.resolutionLabel') }}</label>
                <select v-model="settings.res"><option>1920 × 1080</option><option>2560 × 1440</option><option>1600 × 900</option><option>1366 × 768</option></select>
              </div>
            </div>
            <div class="panel set-block">
              <h3>{{ t('settings.folderTitle') }}</h3>
              <div class="set-row"><input class="ipt full" type="text" v-model="settings.dir" spellcheck="false" /></div>
              <div class="set-row"><button class="btn-sm ghost" @click="openGameFolderBtn">{{ t('settings.openFolderBtn') }}</button></div>
            </div>
            <div class="panel set-block">
              <h3>{{ t('settings.javaTitle') }}</h3>
              <div class="set-row"><input class="ipt full" type="text" v-model="settings.java" :placeholder="t('settings.javaPlaceholder')" spellcheck="false" /></div>
            </div>
            <div class="panel set-block">
              <h3>{{ t('settings.accountTitle') }}</h3>
              <p class="set-note">{{ t('settings.connectedAs').replace('{name}', CONFIG.pseudo) }}</p>
              <div class="set-row"><button v-if="account.online" class="btn-sm danger" @click="logout">{{ t('settings.logoutBtn') }}</button><button v-else class="btn-sm" @click="login">{{ t('settings.loginBtn') }}</button></div>
            </div>
            <div class="panel set-block">
              <h3>{{ t('settings.launcherTitle') }}</h3>
              <p class="set-note">{{ t('settings.versionLabel').replace('{v}', APP_VERSION) }}</p>
              <div class="set-row"><button class="btn-sm ghost" @click="checkUpdate">{{ t('settings.checkUpdateBtn') }}</button></div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>

  <!-- Overlay de lancement -->
  <div v-if="launchInfo.active" class="launch-ov">
    <div class="lo-bg"></div>
    <div class="lo-card">
      <img class="lo-logo" :src="logo" alt="" />
      <div class="lo-profile">{{ CONFIG.pseudo }} · Fabric 0.19.3 · MC 1.20.6</div>
      <template v-if="launchInfo.phase !== 'error' && launchInfo.phase !== 'exit'">
        <div class="lo-spinner"></div>
        <div class="lo-text">{{ launchPhaseLabel }}</div>
        <div class="lo-bar"><i :style="{ width: launchInfo.percent + '%' }"></i></div>
        <div class="lo-percent">{{ launchInfo.percent }}%</div>
      </template>
      <template v-else>
        <div class="lo-error">
          <div class="lo-error-ic">⚠</div>
          <div class="lo-error-title">{{ t('launch.failedTitle') }}</div>
          <p class="lo-error-msg">{{ launchInfo.error }}</p>
        </div>
      </template>
      <div class="lo-actions">
        <button class="btn-sm ghost" @click="logsOpen = !logsOpen">{{ logsOpen ? t('launch.hideLogs') : t('launch.showLogs') }}</button>
        <button class="btn-sm ghost" @click="openGameFolderBtn">{{ t('launch.openFolder') }}</button>
        <button v-if="launchInfo.phase === 'error' || launchInfo.phase === 'exit'" class="btn-sm" @click="closeLaunchOverlay">{{ t('launch.close') }}</button>
        <button v-else class="btn-sm danger" @click="cancelLaunch">{{ t('launch.cancel') }}</button>
      </div>
      <transition name="pop">
        <div v-if="logsOpen" class="lo-logs">
          <div class="lo-logs-bar"><b>{{ t('launch.liveLog') }}</b><span class="sp"></span><button @click="copyLog">{{ t('launch.copy') }}</button><button @click="clearLog">{{ t('launch.clear') }}</button></div>
          <div class="ov-logpre">
            <div v-for="(l, i) in logLines" :key="i" :class="lineClass(l)">{{ l }}</div>
            <div v-if="!logLines.length" class="lg-dim">{{ t('launch.waiting') }}</div>
          </div>
        </div>
      </transition>
    </div>
  </div>

  <div v-if="upd.state" class="upd" :class="{ ready: upd.state === 'ready' }">
    <span class="upd-ic">⬇</span>
    <span v-if="upd.state === 'downloading'">{{ t('update.updating').replace('{percent}', upd.percent) }}</span>
    <template v-else><span>{{ t('update.ready').replace('{version}', upd.version) }}</span><button @click="installUpdate">{{ t('update.installBtn') }}</button></template>
  </div>

  <div class="toast" :class="{ show: toastMsg }">{{ toastMsg }}</div>
</template>

<style scoped>
/* ===== Titlebar ===== */
.titlebar { position: fixed; top: 0; left: 0; right: 0; height: 34px; z-index: 100; display: flex; align-items: center; padding-left: 14px; -webkit-app-region: drag; background: rgba(10,11,17,.86); border-bottom: 1px solid rgba(232,197,106,.22); }
.tb-title { font-family: var(--serif, 'Palatino Linotype', Georgia, serif); font-weight: 700; letter-spacing: 3px; font-size: 12px; color: #CFC7B2; }
.tb-controls { margin-left: auto; height: 100%; display: flex; -webkit-app-region: no-drag; }
.tb-controls button { width: 44px; height: 100%; border: none; background: none; color: #9A94A8; cursor: pointer; font-size: 13px; }
.tb-controls button:hover { background: rgba(232,197,106,.16); color: var(--gold, #E8C56A); }
.tb-controls .close:hover { background: #E05555; color: #fff; }

/* ===== Fond animé / thèmes ===== */
.bg { position: fixed; inset: 0; z-index: 0; overflow: hidden; background: #07070C; transition: filter .5s ease, opacity .5s ease; --tint-r: 90; --tint-g: 169; --tint-b: 230; }
.bg.switching { filter: brightness(.18) saturate(.5); opacity: .9; }
.bg.theme-olympe { --tint-r: 232; --tint-g: 197; --tint-b: 106; }
.bg.theme-jupiter { --tint-r: 90; --tint-g: 169; --tint-b: 230; }
.bg.theme-nuit { --tint-r: 120; --tint-g: 140; --tint-b: 255; }
.bg.theme-aurore { --tint-r: 124; --tint-g: 203; --tint-b: 110; }
.bg.theme-sombre { --tint-r: 150; --tint-g: 150; --tint-b: 160; }
.sky { position: absolute; inset: -5%; background-size: cover; background-position: center; animation: skydrift 75s ease-in-out infinite alternate; will-change: transform; }
@keyframes skydrift { from { transform: scale(1.06) translate(0,0) } to { transform: scale(1.13) translate(-2%,-1.2%) } }
.stars { position: absolute; inset: -60px; background-repeat: repeat; opacity: .7; }
.stars.s2 { opacity: .5; }
.bg.theme-sombre .stars { opacity: .18; }
.planet-glow { position: absolute; right: -14vw; top: 50%; width: min(70vw, 1000px); aspect-ratio: 1; transform: translateY(-50%); border-radius: 50%; background: radial-gradient(circle at 50% 50%, rgba(var(--tint-r),var(--tint-g),var(--tint-b),.22), transparent 70%); pointer-events: none; transition: opacity .5s; }
.planet, .planet-veil { position: absolute; right: -7vw; top: 50%; transform: translateY(-50%); width: min(52vw, 780px); transition: opacity .5s; }
.planet { opacity: .98; filter: brightness(.8) saturate(1.02); -webkit-mask-image: radial-gradient(circle at 50% 50%, #000 66%, rgba(0,0,0,.55) 80%, transparent 92%); mask-image: radial-gradient(circle at 50% 50%, #000 66%, rgba(0,0,0,.55) 80%, transparent 92%); animation: float 120s ease-in-out infinite alternate; will-change: transform; }
.planet-veil { aspect-ratio: 1; border-radius: 50%; pointer-events: none; background: radial-gradient(circle at 50% 50%, rgba(4,5,9,.28) 55%, rgba(4,5,9,.12) 74%, transparent 90%); }
@keyframes float { from { transform: translateY(-50%) } to { transform: translateY(-53%) } }
.bg.theme-sombre .planet, .bg.theme-sombre .planet-glow, .bg.theme-sombre .planet-veil { opacity: .12; }
.scrim { position: absolute; inset: 0; background: radial-gradient(ellipse 78% 70% at 40% 46%, rgba(5,6,10,.14), rgba(5,6,10,.55) 90%); }
.lightning { position: absolute; inset: 0; background: radial-gradient(circle at 72% 18%, rgba(232,197,106,.4), transparent 42%); opacity: 0; pointer-events: none; }
.bg.theme-olympe .lightning { animation: lightningFlash 9s infinite; }
@keyframes lightningFlash { 0%, 92%, 100% { opacity: 0 } 93% { opacity: .85 } 94% { opacity: .15 } 95% { opacity: .55 } 96% { opacity: 0 } }
.meteor { position: absolute; top: 0; left: 0; width: 170px; height: 2px; border-radius: 3px; pointer-events: none; opacity: 0; transform-origin: right center; background: linear-gradient(90deg, rgba(190,205,255,0), rgba(205,220,255,.5) 60%, rgba(255,255,255,.95)); animation-name: meteorGo; animation-timing-function: linear; animation-fill-mode: forwards; will-change: transform; }
.meteor::after { content: ''; position: absolute; right: -1px; top: 50%; width: 5px; height: 5px; margin-top: -2.5px; border-radius: 50%; background: #fff; box-shadow: 0 0 9px 2px rgba(205,220,255,.9); }
@keyframes meteorGo { 0% { transform: translate(var(--sx), var(--sy)) rotate(var(--ang)) scale(var(--sc)); opacity: 0 } 15% { opacity: .6 } 80% { opacity: .55 } 100% { transform: translate(var(--ex), var(--ey)) rotate(var(--ang)) scale(var(--sc)); opacity: 0 } }

/* ===== Coquille / barre latérale ===== */
.shell { position: fixed; inset: 34px 0 0 0; z-index: 10; display: flex; }
.sidebar { width: 200px; flex: 0 0 auto; display: flex; flex-direction: column; background: rgba(9,10,16,.86); border-right: 1px solid rgba(255,255,255,.07); padding: 16px 0; }
.side-brand { display: flex; justify-content: center; padding: 4px 0 18px; }
.side-brand img { width: 38px; height: 38px; object-fit: contain; filter: drop-shadow(0 4px 10px rgba(0,0,0,.5)); }
.side-nav { display: flex; flex-direction: column; gap: 2px; padding: 0 10px; }
.side-item { position: relative; display: flex; align-items: center; gap: 12px; padding: 11px 12px; border: none; background: none; color: #A9A4B8; cursor: pointer; border-radius: 10px; font-size: 13.5px; text-align: left; transition: background .15s, color .15s; }
.side-item:hover { background: rgba(255,255,255,.06); color: #EDE8DA; }
.side-item.active { color: var(--gold, #E8C56A); background: rgba(232,197,106,.1); }
.side-item.active::before { content: ''; position: absolute; left: -10px; top: 8px; bottom: 8px; width: 3px; border-radius: 3px; background: var(--gold, #E8C56A); box-shadow: 0 0 8px var(--gold, #E8C56A); }
.side-ic { width: 19px; height: 19px; flex: 0 0 auto; }
.side-ic path { fill: currentColor; }
.side-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.side-foot { margin-top: auto; display: flex; align-items: center; gap: 8px; padding: 12px 16px 4px; border-top: 1px solid rgba(255,255,255,.06); font-size: 11.5px; color: #6E6980; }
.sf-dot { width: 8px; height: 8px; border-radius: 50%; background: #E05555; box-shadow: 0 0 6px #E05555; flex: 0 0 auto; }
.sf-dot.on { background: #7CCB6E; box-shadow: 0 0 6px #7CCB6E; }
.sf-txt { font-size: 11.5px; }

/* ===== Zone principale / compte ===== */
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; position: relative; }
.topbar { height: 58px; flex: 0 0 auto; display: flex; align-items: center; padding: 0 clamp(16px, 2vw, 32px); gap: 16px; border-bottom: 1px solid rgba(255,255,255,.06); background: rgba(7,7,12,.28); }
.tb-crumb { font-family: var(--serif, Georgia, serif); font-size: 15px; letter-spacing: 1px; color: #CFC7B2; }
.acct-chip { position: relative; margin-left: auto; display: flex; align-items: center; gap: 10px; padding: 6px 12px 6px 14px; border-radius: 999px; background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); cursor: pointer; transition: background .15s, border-color .15s; }
.acct-chip:hover, .acct-chip.open { background: rgba(232,197,106,.1); border-color: rgba(232,197,106,.3); }
.acct-dot { width: 8px; height: 8px; border-radius: 50%; background: #6b6b78; }
.acct-dot.on { background: #7CCB6E; box-shadow: 0 0 7px #7CCB6E; }
.acct-name { font-weight: 700; font-size: 13.5px; }
.acct-head { width: 32px; height: 32px; image-rendering: pixelated; border-radius: 6px; }
.acct-pop { position: absolute; top: 52px; right: 0; width: 250px; z-index: 90; background: rgba(10,11,17,.96); border: 1px solid rgba(232,197,106,.35); border-radius: 12px; padding: 8px; box-shadow: 0 18px 44px rgba(0,0,0,.6); backdrop-filter: blur(14px); cursor: default; }
.ap-head { display: flex; align-items: center; gap: 10px; padding: 8px 8px 12px; border-bottom: 1px solid rgba(255,255,255,.08); margin-bottom: 6px; }
.ap-head img { width: 40px; height: 40px; image-rendering: pixelated; border-radius: 8px; }
.ap-name { font-family: var(--serif, Georgia, serif); font-weight: 700; font-size: 15px; }
.ap-status { font-size: 11px; color: #9A94A8; display: flex; align-items: center; gap: 6px; }
.ap-status i { width: 7px; height: 7px; border-radius: 50%; background: #6b6b78; }
.ap-status.on { color: #7CCB6E; } .ap-status.on i { background: #7CCB6E; box-shadow: 0 0 6px #7CCB6E; }
.ap-item { display: block; width: 100%; text-align: left; background: none; border: none; color: #E7E2D2; font-size: 13px; padding: 9px 10px; border-radius: 8px; cursor: pointer; }
.ap-item:hover { background: rgba(232,197,106,.14); color: var(--gold, #E8C56A); }
.ap-item.login { background: rgba(90,169,230,.14); color: #8fd6fb; font-weight: 700; } .ap-item.login:hover { background: rgba(90,169,230,.22); }
.ap-item.danger { color: #E58A8A; } .ap-item.danger:hover { background: rgba(224,85,85,.15); color: #ff9a9a; }
.ap-sep { height: 1px; background: rgba(255,255,255,.08); margin: 6px 0; }
.pop-enter-active, .pop-leave-active { transition: opacity .15s ease, transform .15s ease; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== Contenu / pages génériques ===== */
.content { flex: 1; min-height: 0; overflow-y: auto; padding: clamp(18px, 2.2vw, 36px); contain: layout paint; }
.content::-webkit-scrollbar { width: 9px; }
.content::-webkit-scrollbar-thumb { background: rgba(255,255,255,.12); border-radius: 6px; }
.pg { animation: pgIn .28s ease; }
@keyframes pgIn { from { opacity: 0; transform: translateY(6px) } to { opacity: 1; transform: translateY(0) } }
.pg-head { margin-bottom: 20px; }
.pg-head h1 { font-family: var(--serif, Georgia, serif); font-size: clamp(22px, 2.4vw, 30px); color: #EDE8DA; letter-spacing: .5px; }
.pg-head p { font-size: 13px; color: #9A94A8; margin-top: 4px; }
.panel { background: rgba(9,10,16,.84); border: 1px solid rgba(255,255,255,.07); border-radius: 14px; contain: content; }
.ipt { background: rgba(5,6,10,.55); border: 1px solid rgba(255,255,255,.14); color: #EDE8DA; border-radius: 9px; padding: 9px 13px; font-size: 13.5px; }
.ipt.full { width: 100%; }
.ipt:focus { outline: none; border-color: rgba(232,197,106,.5); }
.btn-sm { background: linear-gradient(180deg, var(--gold, #E8C56A), #D4AF37); color: #1c1607; border: none; border-radius: 9px; padding: 9px 16px; font-weight: 700; font-size: 13px; cursor: pointer; transition: transform .12s, filter .15s; }
.btn-sm:hover { filter: brightness(1.08); }
.btn-sm:active { transform: scale(.97); }
.btn-sm.ghost { background: rgba(255,255,255,.07); color: #EDE8DA; border: 1px solid rgba(255,255,255,.14); }
.btn-sm.ghost:hover { background: rgba(232,197,106,.14); color: var(--gold, #E8C56A); border-color: rgba(232,197,106,.4); }
.btn-sm.danger { background: rgba(224,85,85,.85); color: #fff; }
.btn-sm:disabled { opacity: .4; cursor: not-allowed; filter: none; }

/* ===== Accueil ===== */
.pg-home { display: flex; flex-direction: column; align-items: center; gap: clamp(24px, 4vh, 48px); min-height: 100%; justify-content: center; }
.home-center { display: flex; flex-direction: column; align-items: center; gap: 16px; text-align: center; }
.brand-logo { width: min(360px, 32vw); filter: drop-shadow(0 14px 34px rgba(0,0,0,.55)); }
.brand-tagline { font-family: var(--serif, Georgia, serif); letter-spacing: 7px; font-size: clamp(12px, 1.1vw, 15px); color: #CFC7B2; }
.btn-launch { font-family: var(--serif, Georgia, serif); font-weight: 700; letter-spacing: 8px; font-size: clamp(17px, 1.6vw, 22px); color: #F0D585; border: 1px solid rgba(233,204,116,.38); border-radius: 13px; padding: 15px clamp(40px, 6vw, 74px); cursor: pointer; background: linear-gradient(180deg, rgba(255,255,255,.15), rgba(255,255,255,.06)); box-shadow: inset 0 1px 0 rgba(255,255,255,.28), 0 8px 26px rgba(0,0,0,.4); text-shadow: 0 1px 8px rgba(0,0,0,.55); transition: transform .14s ease, box-shadow .2s, color .2s; margin-top: 8px; }
.btn-launch:hover:not(:disabled) { transform: scale(1.04); color: #FFE9A8; box-shadow: inset 0 1px 0 rgba(255,255,255,.35), 0 0 24px rgba(240,213,133,.3), 0 10px 30px rgba(0,0,0,.45); border-color: rgba(233,204,116,.6); }
.btn-launch:active:not(:disabled) { transform: scale(.99); }
.btn-launch.busy, .btn-launch:disabled { cursor: default; letter-spacing: 2px; opacity: .85; }
.home-chips { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 6px; }
.chip { display: flex; align-items: center; gap: 8px; border-radius: 999px; padding: 8px 16px; font-size: 13px; color: #CFC7B2; background: rgba(9,10,16,.65); border: 1px solid rgba(255,255,255,.08); }
.chip-dot { width: 8px; height: 8px; border-radius: 50%; background: #E05555; box-shadow: 0 0 7px #E05555; }
.chip.on .chip-dot { background: #7CCB6E; box-shadow: 0 0 7px #7CCB6E; }
.home-news { width: min(1000px, 94%); }
.home-news-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.home-news-head span { font-family: var(--serif, Georgia, serif); color: #CFC7B2; letter-spacing: 1px; font-size: 14px; }
.home-news-head button { background: none; border: none; color: var(--gold, #E8C56A); cursor: pointer; font-size: 13px; }
.home-news-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; }
.news-card { background: rgba(9,10,16,.84); border: 1px solid rgba(255,255,255,.07); border-radius: 14px; padding: 16px 18px; cursor: pointer; transition: border-color .15s, transform .15s; contain: content; }
.news-card:hover { border-color: rgba(232,197,106,.4); transform: translateY(-3px); }
.nc-tag { font-size: 11px; letter-spacing: 1.5px; text-transform: uppercase; color: #5AA9E6; margin-bottom: 8px; }
.news-card h3 { font-family: var(--serif, Georgia, serif); font-size: 15.5px; color: var(--gold, #E8C56A); margin-bottom: 8px; line-height: 1.3; }
.news-card p { font-size: 12.5px; color: #9A94A8; line-height: 1.55; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.nc-date { font-size: 11px; color: #6b6b78; margin-top: 10px; }

/* ===== Actualités ===== */
.actus-body { display: flex; gap: 20px; align-items: flex-start; }
.actus-list { width: 260px; flex: 0 0 auto; display: flex; flex-direction: column; gap: 6px; }
.actus-item { display: flex; flex-direction: column; gap: 3px; text-align: left; background: rgba(9,10,16,.55); border: 1px solid rgba(255,255,255,.06); border-radius: 10px; padding: 10px 12px; cursor: pointer; color: #C9C4D6; contain: content; }
.actus-item:hover { border-color: rgba(232,197,106,.3); }
.actus-item.active { border-color: rgba(232,197,106,.55); background: rgba(232,197,106,.08); }
.ai-tag { font-size: 10.5px; letter-spacing: 1px; text-transform: uppercase; color: #5AA9E6; }
.ai-title { font-size: 13px; color: #EDE8DA; font-weight: 600; }
.ai-date { font-size: 11px; color: #6b6b78; }
.actus-reader { flex: 1; padding: 30px 34px; min-width: 0; }
.ar-tag { font-size: 12px; letter-spacing: 2px; text-transform: uppercase; color: #5AA9E6; margin-bottom: 12px; }
.actus-reader h2 { font-family: var(--serif, Georgia, serif); font-size: clamp(22px, 2.6vw, 32px); color: var(--gold, #E8C56A); line-height: 1.2; margin-bottom: 8px; }
.ar-meta { font-size: 13px; color: #8A85A0; margin-bottom: 18px; }
.actus-reader p { font-size: 15px; line-height: 1.7; color: #D8D3E0; }
.ar-pager { display: flex; align-items: center; gap: 18px; margin-top: 24px; }
.ar-pager button { background: rgba(255,255,255,.06); border: 1px solid rgba(255,255,255,.1); color: #EDE8DA; border-radius: 9px; padding: 9px 16px; cursor: pointer; font-size: 13px; }
.ar-pager button:disabled { opacity: .3; cursor: not-allowed; }
.ar-pager span { font-size: 13px; color: #8A85A0; }

/* ===== Cosmétiques ===== */
.cos-notice { padding: 12px 16px; margin-bottom: 16px; font-size: 12.5px; color: #CFC7B2; border-color: rgba(232,197,106,.25); }
.cos-body { display: grid; grid-template-columns: 170px 260px 1fr; gap: 18px; align-items: start; }
.cos-cats { display: flex; flex-direction: column; gap: 4px; }
.cos-cat { text-align: left; background: rgba(9,10,16,.5); border: 1px solid rgba(255,255,255,.06); color: #B9B4C6; border-radius: 9px; padding: 9px 12px; cursor: pointer; font-size: 12.5px; }
.cos-cat:hover { border-color: rgba(232,197,106,.3); }
.cos-cat.active { color: var(--gold, #E8C56A); background: rgba(232,197,106,.1); border-color: rgba(232,197,106,.4); }
.cos-preview { padding: 20px; display: flex; flex-direction: column; align-items: center; text-align: center; gap: 6px; }
.cos-body-img { width: 120px; image-rendering: pixelated; filter: drop-shadow(0 10px 20px rgba(0,0,0,.5)); }
.cos-preview-name { font-weight: 700; font-size: 14px; margin-top: 4px; }
.cos-preview-sub { font-size: 11.5px; color: #8A85A0; }
.cos-grid-wrap { min-width: 0; }
.cos-controls { display: flex; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.cos-controls .ipt { flex: 1; min-width: 140px; }
.cos-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
.cos-item { position: relative; background: rgba(9,10,16,.65); border: 1px solid rgba(255,255,255,.07); border-radius: 12px; padding: 14px 14px 20px; min-height: 80px; contain: content; }
.cos-item.soon { opacity: .55; }
.ci-name { font-size: 13px; color: #EDE8DA; font-weight: 600; line-height: 1.3; }
.ci-badge { position: absolute; top: 10px; right: 10px; font-size: 9.5px; padding: 2px 7px; border-radius: 999px; }
.ci-badge.soon { background: rgba(255,255,255,.1); color: #9A94A8; text-transform: uppercase; letter-spacing: .5px; }

/* ===== Amis ===== */
.friends-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.friends-tabs button { background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); color: #B9B4C6; border-radius: 999px; padding: 8px 16px; cursor: pointer; font-size: 12.5px; }
.friends-tabs button.active { background: rgba(232,197,106,.14); color: var(--gold, #E8C56A); border-color: rgba(232,197,106,.4); }
.friends-list { display: flex; flex-direction: column; gap: 8px; max-width: 640px; }
.friends-empty { padding: 30px; text-align: center; }
.fe-title { color: #EDE8DA; font-size: 14px; font-weight: 700; }
.fe-sub { color: #8A85A0; font-size: 12.5px; margin-top: 8px; line-height: 1.55; }

/* ===== Mods ===== */
.mods-toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.mods-toolbar .ipt { flex: 1; }
.mods-hint { background: rgba(232,197,106,.1); border: 1px solid rgba(232,197,106,.3); color: var(--gold, #E8C56A); border-radius: 9px; padding: 9px 14px; font-size: 12.5px; margin-bottom: 12px; }
.mods-body { display: grid; grid-template-columns: 1fr 280px; gap: 16px; align-items: start; }
.mods-list { display: flex; flex-direction: column; gap: 14px; min-width: 0; }
.mg-title { font-size: 11px; letter-spacing: 1.5px; text-transform: uppercase; color: #6b6b78; margin-bottom: 6px; }
.mod-row { display: flex; align-items: center; gap: 12px; background: rgba(9,10,16,.55); border: 1px solid rgba(255,255,255,.06); border-radius: 10px; padding: 11px 14px; margin-bottom: 6px; cursor: pointer; contain: content; }
.mod-row:hover { border-color: rgba(232,197,106,.25); }
.mod-row.active { border-color: rgba(232,197,106,.5); background: rgba(232,197,106,.06); }
.mr-main { flex: 1; min-width: 0; }
.mr-name { font-size: 13px; font-weight: 700; color: #EDE8DA; }
.mr-sub { font-size: 11px; color: #8A85A0; margin-top: 2px; }
.mr-static { font-size: 11px; color: #7CCB6E; }
.mods-empty { color: #8A85A0; font-size: 13px; }
.switch { position: relative; width: 36px; height: 20px; flex: 0 0 auto; display: inline-block; }
.switch input { opacity: 0; width: 0; height: 0; position: absolute; }
.switch-track { position: absolute; inset: 0; background: rgba(255,255,255,.15); border-radius: 999px; transition: background .15s; }
.switch-track::before { content: ''; position: absolute; left: 2px; top: 2px; width: 16px; height: 16px; border-radius: 50%; background: #EDE8DA; transition: transform .15s; }
.switch input:checked + .switch-track { background: #7CCB6E; }
.switch input:checked + .switch-track::before { transform: translateX(16px); }
.mods-detail { padding: 18px; }
.mods-detail h3 { font-family: var(--serif, Georgia, serif); color: var(--gold, #E8C56A); font-size: 16px; margin-bottom: 10px; }
.md-row { display: flex; justify-content: space-between; font-size: 12px; color: #B9B4C6; margin-bottom: 6px; }
.md-desc { font-size: 12.5px; color: #9A94A8; line-height: 1.55; margin-top: 10px; }
.md-empty { color: #6b6b78; font-size: 12.5px; }

/* ===== Langues ===== */
.lang-search { width: min(360px, 100%); margin-bottom: 14px; }
.lang-list { display: flex; flex-direction: column; gap: 4px; max-width: 420px; }
.lang-row { display: flex; align-items: center; justify-content: space-between; background: rgba(9,10,16,.5); border: 1px solid rgba(255,255,255,.06); border-radius: 9px; padding: 11px 14px; cursor: pointer; color: #C9C4D6; font-size: 13px; contain: content; }
.lang-row:hover { border-color: rgba(232,197,106,.3); }
.lang-row.active { color: var(--gold, #E8C56A); border-color: rgba(232,197,106,.45); background: rgba(232,197,106,.08); }
.lang-row.disabled { opacity: .45; cursor: not-allowed; }
.lang-row.disabled:hover { border-color: rgba(255,255,255,.06); }
.lang-soon { font-size: 9.5px; letter-spacing: .5px; text-transform: uppercase; padding: 2px 8px; border-radius: 999px; background: rgba(255,255,255,.08); color: #9A94A8; }
.lang-check { width: 16px; height: 16px; fill: none; stroke: currentColor; stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; }

/* ===== Thèmes ===== */
.themes-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; max-width: 900px; }
.theme-card { position: relative; display: flex; flex-direction: column; gap: 8px; background: rgba(9,10,16,.6); border: 1px solid rgba(255,255,255,.07); border-radius: 14px; padding: 12px; cursor: pointer; text-align: left; contain: content; }
.theme-card:hover { border-color: rgba(232,197,106,.35); }
.theme-card.active { border-color: rgba(232,197,106,.6); }
.tc-preview { height: 90px; border-radius: 10px; }
.tc-preview.prev-olympe { background: radial-gradient(circle at 70% 30%, #E8C56A, #241a06 72%); }
.tc-preview.prev-jupiter { background: radial-gradient(circle at 65% 40%, #5AA9E6, #0E1018 65%); }
.tc-preview.prev-nuit { background: linear-gradient(155deg, #090a16, #1b1f3a); }
.tc-preview.prev-aurore { background: linear-gradient(155deg, #0a1a14, #1c3a2c 45%, #123a3a 85%); }
.tc-preview.prev-sombre { background: linear-gradient(155deg, #0a0a0e, #17171d); }
.tc-name { font-family: var(--serif, Georgia, serif); font-size: 14px; color: #EDE8DA; }
.tc-desc { font-size: 11.5px; color: #8A85A0; }
.tc-badge { position: absolute; top: 10px; right: 10px; background: rgba(124,203,110,.2); color: #7CCB6E; font-size: 10px; padding: 2px 8px; border-radius: 999px; }

/* ===== Paramètres ===== */
.set-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 14px; max-width: 1000px; }
.set-block { padding: 18px 20px; }
.set-block h3 { font-family: var(--serif, Georgia, serif); font-size: 13px; letter-spacing: 2px; text-transform: uppercase; color: #CFC7B2; margin-bottom: 12px; }
.set-row { display: flex; align-items: center; gap: 12px; margin: 8px 0; }
.set-row label { width: 110px; font-size: 13px; flex: 0 0 auto; }
.set-row select, .set-row input[type=range] { flex: 1; }
.set-row input[type=range] { accent-color: var(--gold-dark, #D4AF37); }
.set-val { width: 56px; text-align: right; color: var(--gold, #E8C56A); font-weight: 700; font-size: 13px; }
.set-note { font-size: 11.5px; color: #8A85A0; margin-top: 6px; }

/* ===== Overlay de lancement ===== */
.launch-ov { position: fixed; inset: 0; z-index: 500; display: grid; place-items: center; }
.lo-bg { position: absolute; inset: 0; background: radial-gradient(ellipse at 50% 40%, rgba(20,22,34,.96), rgba(4,5,9,.99)); }
.lo-card { position: relative; width: min(560px, 92vw); max-height: 88vh; display: flex; flex-direction: column; align-items: center; gap: 14px; background: rgba(9,10,16,.9); border: 1px solid rgba(232,197,106,.3); border-radius: 18px; padding: 34px 30px; box-shadow: 0 30px 80px rgba(0,0,0,.6); overflow: auto; }
.lo-logo { width: 90px; filter: drop-shadow(0 8px 20px rgba(0,0,0,.5)); }
.lo-profile { font-size: 12px; color: #8A85A0; letter-spacing: .5px; }
.lo-spinner { width: 34px; height: 34px; border-radius: 50%; border: 3px solid rgba(232,197,106,.2); border-top-color: var(--gold, #E8C56A); animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg) } }
.lo-text { font-family: var(--serif, Georgia, serif); font-size: 15px; color: #EDE8DA; }
.lo-bar { width: 100%; height: 8px; border-radius: 999px; background: rgba(255,255,255,.1); overflow: hidden; }
.lo-bar i { display: block; height: 100%; background: linear-gradient(90deg, #5AA9E6, var(--gold, #E8C56A)); transition: width .3s ease; }
.lo-percent { font-size: 12px; color: #8A85A0; }
.lo-error { display: flex; flex-direction: column; align-items: center; gap: 6px; text-align: center; }
.lo-error-ic { font-size: 30px; color: #E05555; }
.lo-error-title { font-family: var(--serif, Georgia, serif); font-size: 16px; color: #E58A8A; }
.lo-error-msg { font-size: 12.5px; color: #C9A0A0; max-width: 440px; word-break: break-word; }
.lo-actions { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; }
.lo-logs { width: 100%; background: #0A0B11; border: 1px solid rgba(255,255,255,.1); border-radius: 10px; overflow: hidden; }
.lo-logs-bar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-bottom: 1px solid rgba(255,255,255,.08); color: #EDE8DA; font-size: 12.5px; }
.lo-logs-bar .sp { flex: 1; }
.lo-logs-bar button { background: rgba(232,197,106,.14); color: var(--gold, #E8C56A); border: 1px solid rgba(232,197,106,.3); border-radius: 7px; padding: 5px 10px; cursor: pointer; font-size: 11.5px; }
.ov-logpre { max-height: 180px; overflow: auto; padding: 10px 12px; font-family: Consolas, monospace; font-size: 11.5px; line-height: 1.5; color: #9FB0C4; white-space: pre-wrap; word-break: break-word; }
.lg-err { color: #ff6b6b; } .lg-warn { color: #ffc04f; } .lg-game { color: #C9D2E0; } .lg-dim { color: #6b7180; font-style: italic; }

/* ===== Mise à jour / toast ===== */
.upd { position: fixed; top: 44px; left: 50%; transform: translateX(-50%); z-index: 250; display: flex; align-items: center; gap: 12px; background: rgba(9,10,16,.94); border: 1px solid rgba(232,197,106,.5); border-radius: 12px; padding: 9px 16px; font-size: 13px; color: #EDE8DA; box-shadow: 0 10px 30px rgba(0,0,0,.5); }
.upd.ready { border-color: #7CCB6E; }
.upd-ic { color: var(--gold, #E8C56A); }
.upd button { background: linear-gradient(180deg, var(--gold, #E8C56A), #D4AF37); color: #1c1607; border: none; border-radius: 8px; padding: 6px 14px; font-weight: 800; cursor: pointer; }
.toast { position: fixed; bottom: 26px; left: 50%; transform: translate(-50%,12px); z-index: 600; opacity: 0; pointer-events: none; transition: .2s; background: #0A0B11; color: #EDE8DA; border: 1px solid var(--gold, #E8C56A); border-radius: 10px; padding: 11px 20px; font-size: 13px; }
.toast.show { opacity: 1; transform: translate(-50%,0); }

/* ===== Responsive ===== */
@media (max-width: 980px) {
  .sidebar { width: 62px; }
  .side-nav { padding: 0 6px; }
  .side-item { justify-content: center; padding: 12px 6px; gap: 0; }
  .side-label { display: none; }
}
@media (max-width: 1100px) {
  .cos-body { grid-template-columns: 130px 1fr; }
  .cos-preview { display: none; }
  .mods-body { grid-template-columns: 1fr; }
  .mods-detail { display: none; }
}
@media (max-width: 900px) {
  .actus-body { flex-direction: column; }
  .actus-list { width: 100%; flex-direction: row; overflow-x: auto; }
}
@media (min-width: 1500px) {
  .content { padding: 44px 56px; }
}
</style>

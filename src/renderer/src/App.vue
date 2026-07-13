<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import logo from './assets/logo.png'
import planet from './assets/planet.png'
import sky from './assets/sky.jpg'
import comet from './assets/comet.png'
import CharacterViewer from './components/CharacterViewer.vue'

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
const APP_VERSION = ref('…')
const comet_ = comet // conserve l'import (utilisé comme sprite décoratif potentiel)

/* ===================== Réglages persistés ===================== */
const settings = reactive(Object.assign(
  {
    ram: 4, res: '1920 × 1080', dir: '%APPDATA%/.heroesworld', java: '',
    updateFreq: 'auto', autoDownload: true, minimizeOnLaunch: true,
    notifEnabled: true, animations: true
  },
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
    'cosmetics.subtitle': 'Le vestiaire de tes cosmétiques : parcours le catalogue et prévisualise ton personnage en 3D.',
    'cosmetics.notice': "Ceci est une vitrine : parcours le catalogue, prévisualise en 3D et marque tes favoris. L'équipement réel de tes cosmétiques se fait EN JEU, dans le menu HEROES-WORLD.",
    'cosmetics.previewSub': 'Aperçu de ton skin actuel.', 'cosmetics.searchPlaceholder': 'Rechercher un cosmétique (toutes catégories)…',
    'cosmetics.categories': 'Catégories',
    'cosmetics.cat.capes': 'Capes', 'cosmetics.cat.ailes': 'Ailes', 'cosmetics.cat.auras': 'Auras',
    'cosmetics.cat.casques': 'Casques / Chapeaux', 'cosmetics.cat.masques': 'Masques', 'cosmetics.cat.compagnons': 'Compagnons',
    'cosmetics.cat.effets': 'Effets', 'cosmetics.cat.emotes': 'Emotes', 'cosmetics.cat.sprays': 'Sprays',
    'cosmetics.cat.skins': 'Skins', 'cosmetics.cat.exclusifs': 'Exclusifs HW',
    'cosmetics.filterAllTypes': 'Tous les types', 'cosmetics.filterAllRarities': 'Toutes les raretés',
    'cosmetics.filterAllOwned': 'Tous', 'cosmetics.filterOwned': 'Possédés', 'cosmetics.filterLocked': 'Non possédés',
    'cosmetics.sortName': 'Nom', 'cosmetics.sortNew': 'Nouveauté', 'cosmetics.sortRarity': 'Rareté', 'cosmetics.sortEquipped': 'Équipé',
    'cosmetics.rarity.commun': 'Commun', 'cosmetics.rarity.rare': 'Rare', 'cosmetics.rarity.epique': 'Épique',
    'cosmetics.rarity.legendaire': 'Légendaire', 'cosmetics.rarity.mythique': 'Mythique',
    'cosmetics.owned': 'Possédé', 'cosmetics.locked': 'Non possédé', 'cosmetics.equippedBadge': 'Équipé',
    'cosmetics.equipBtn': 'Équiper (aperçu)', 'cosmetics.unequipBtn': 'Retirer (aperçu)',
    'cosmetics.equipNote': "L'équipement réel se fait EN JEU — ceci n'est qu'un aperçu visuel dans le launcher.",
    'cosmetics.favToggle': 'Ajouter/retirer des favoris', 'cosmetics.favorites': 'Favoris',
    'cosmetics.noResults': 'Aucun cosmétique ne correspond à ta recherche.',
    'friends.subtitle': 'Ta liste d\'amis HEROES-WORLD.', 'friends.tabOnline': 'En ligne', 'friends.tabOffline': 'Hors ligne',
    'friends.tabRequests': 'Demandes', 'friends.emptyTitle': 'Aucun ami pour le moment.',
    'friends.emptySub': "La liste d'amis sera disponible avec l'hébergement du serveur HEROES-WORLD.",
    'friends.addPlaceholder': 'Pseudo…', 'friends.addBtn': 'Ajouter',
    'friends.addSent': "Demande d'ami envoyée (bientôt disponible)", 'friends.searchPlaceholder': 'Rechercher un joueur…',
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
    'settings.searchPlaceholder': 'Rechercher un réglage…',
    'settings.cat.compte': 'Compte', 'settings.cat.jeu': 'Jeu', 'settings.cat.minecraft': 'Minecraft', 'settings.cat.java': 'Java',
    'settings.cat.launcher': 'Launcher', 'settings.cat.notifications': 'Notifications', 'settings.cat.apparence': 'Apparence',
    'settings.cat.performances': 'Performances', 'settings.cat.apropos': 'À propos',
    'settings.updateFreqLabel': 'Fréquence des mises à jour', 'settings.updateFreqAuto': 'Automatique',
    'settings.updateFreqDaily': 'Quotidienne', 'settings.updateFreqManual': 'Manuelle',
    'settings.autoDownloadLabel': 'Téléchargement automatique', 'settings.minimizeOnLaunchLabel': 'Réduire le launcher au lancement',
    'settings.notifEnabledLabel': 'Activer les notifications', 'settings.animationsLabel': 'Animations du décor',
    'settings.bgQualityLabel': 'Qualité des arrière-plans', 'settings.bgQualityHigh': 'Élevée',
    'settings.bgQualityMedium': 'Moyenne', 'settings.bgQualityLow': 'Faible',
    'settings.fpsLimitLabel': 'Limite FPS interface', 'settings.fpsUnlimited': 'Illimitée',
    'settings.themeTitle': 'Thème visuel', 'settings.noResults': 'Aucun réglage ne correspond à ta recherche.',
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
    'toast.logCopyFailed': 'Copie impossible', 'toast.updateChecking': 'Recherche de mise à jour…',
    'launch.repair': 'Réparer', 'launch.copyLogs': 'Copier les logs',
    'launch.hint.cancelled': 'Lancement annulé par toi.', 'launch.hint.java': "Java introuvable — vérifie l'installation de Java 21.",
    'launch.hint.network': 'Problème réseau — vérifie ta connexion.', 'launch.hint.crash': 'Le jeu a planté — ouvre la console pour le détail.',
    'launch.hint.generic': 'Une erreur est survenue au lancement.',
    'notif.title': 'Notifications', 'notif.markAllRead': 'Tout marquer comme lu', 'notif.empty': 'Aucune notification pour le moment.',
    'notif.update.title': 'Mise à jour disponible', 'notif.error.title': 'Échec du lancement',
    'notif.event1.title': 'Événement', 'notif.event1.text': 'Ouverture publique en préparation.',
    'notif.cosmetic1.title': 'Nouveau cosmétique', 'notif.cosmetic1.text': 'Ailes de foudre bientôt disponibles.',
    'home.playingAs': 'Connecté en tant que {name}'
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
    'cosmetics.subtitle': 'Your cosmetics locker: browse the catalogue and preview your character in 3D.',
    'cosmetics.notice': "This is a showcase: browse, preview in 3D and star your favorites. Actually equipping cosmetics happens IN-GAME, from the HEROES-WORLD menu.",
    'cosmetics.previewSub': 'Preview of your current skin.', 'cosmetics.searchPlaceholder': 'Search a cosmetic (all categories)…',
    'cosmetics.categories': 'Categories',
    'cosmetics.cat.capes': 'Capes', 'cosmetics.cat.ailes': 'Wings', 'cosmetics.cat.auras': 'Auras',
    'cosmetics.cat.casques': 'Helmets / Hats', 'cosmetics.cat.masques': 'Masks', 'cosmetics.cat.compagnons': 'Companions',
    'cosmetics.cat.effets': 'Effects', 'cosmetics.cat.emotes': 'Emotes', 'cosmetics.cat.sprays': 'Sprays',
    'cosmetics.cat.skins': 'Skins', 'cosmetics.cat.exclusifs': 'HW Exclusives',
    'cosmetics.filterAllTypes': 'All types', 'cosmetics.filterAllRarities': 'All rarities',
    'cosmetics.filterAllOwned': 'All', 'cosmetics.filterOwned': 'Owned', 'cosmetics.filterLocked': 'Not owned',
    'cosmetics.sortName': 'Name', 'cosmetics.sortNew': 'Newest', 'cosmetics.sortRarity': 'Rarity', 'cosmetics.sortEquipped': 'Equipped',
    'cosmetics.rarity.commun': 'Common', 'cosmetics.rarity.rare': 'Rare', 'cosmetics.rarity.epique': 'Epic',
    'cosmetics.rarity.legendaire': 'Legendary', 'cosmetics.rarity.mythique': 'Mythic',
    'cosmetics.owned': 'Owned', 'cosmetics.locked': 'Not owned', 'cosmetics.equippedBadge': 'Equipped',
    'cosmetics.equipBtn': 'Equip (preview)', 'cosmetics.unequipBtn': 'Unequip (preview)',
    'cosmetics.equipNote': 'Actual equipping happens IN-GAME — this is only a visual preview in the launcher.',
    'cosmetics.favToggle': 'Add/remove from favorites', 'cosmetics.favorites': 'Favorites',
    'cosmetics.noResults': 'No cosmetic matches your search.',
    'friends.subtitle': 'Your HEROES-WORLD friends list.', 'friends.tabOnline': 'Online', 'friends.tabOffline': 'Offline',
    'friends.tabRequests': 'Requests', 'friends.emptyTitle': 'No friends yet.',
    'friends.emptySub': 'The friends list will be available once the HEROES-WORLD server is hosted.',
    'friends.addPlaceholder': 'Username…', 'friends.addBtn': 'Add',
    'friends.addSent': 'Friend request sent (coming soon)', 'friends.searchPlaceholder': 'Search a player…',
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
    'settings.searchPlaceholder': 'Search a setting…',
    'settings.cat.compte': 'Account', 'settings.cat.jeu': 'Game', 'settings.cat.minecraft': 'Minecraft', 'settings.cat.java': 'Java',
    'settings.cat.launcher': 'Launcher', 'settings.cat.notifications': 'Notifications', 'settings.cat.apparence': 'Appearance',
    'settings.cat.performances': 'Performance', 'settings.cat.apropos': 'About',
    'settings.updateFreqLabel': 'Update frequency', 'settings.updateFreqAuto': 'Automatic',
    'settings.updateFreqDaily': 'Daily', 'settings.updateFreqManual': 'Manual',
    'settings.autoDownloadLabel': 'Automatic download', 'settings.minimizeOnLaunchLabel': 'Minimize launcher on launch',
    'settings.notifEnabledLabel': 'Enable notifications', 'settings.animationsLabel': 'Background animations',
    'settings.bgQualityLabel': 'Background quality', 'settings.bgQualityHigh': 'High',
    'settings.bgQualityMedium': 'Medium', 'settings.bgQualityLow': 'Low',
    'settings.fpsLimitLabel': 'Interface FPS limit', 'settings.fpsUnlimited': 'Unlimited',
    'settings.themeTitle': 'Visual theme', 'settings.noResults': 'No setting matches your search.',
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
    'toast.logCopyFailed': 'Copy failed', 'toast.updateChecking': 'Checking for updates…',
    'launch.repair': 'Repair', 'launch.copyLogs': 'Copy logs',
    'launch.hint.cancelled': 'Launch cancelled by you.', 'launch.hint.java': 'Java not found — check your Java 21 installation.',
    'launch.hint.network': 'Network issue — check your connection.', 'launch.hint.crash': 'The game crashed — open the console for details.',
    'launch.hint.generic': 'An error occurred during launch.',
    'notif.title': 'Notifications', 'notif.markAllRead': 'Mark all as read', 'notif.empty': 'No notifications yet.',
    'notif.update.title': 'Update available', 'notif.error.title': 'Launch failed',
    'notif.event1.title': 'Event', 'notif.event1.text': 'Public opening in preparation.',
    'notif.cosmetic1.title': 'New cosmetic', 'notif.cosmetic1.text': 'Lightning wings coming soon.',
    'home.playingAs': 'Playing as {name}'
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
  { id: 'amis', icon: 'M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z' },
  { id: 'mods', icon: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z' },
  { id: 'langues', icon: 'M12 2a10 10 0 1 0 .001 20.001A10 10 0 0 0 12 2zm7.94 9h-3.05a15.7 15.7 0 0 0-1.32-5.47A8.03 8.03 0 0 1 19.94 11zM12 4.06c.98 1.4 1.72 3.24 2.02 5.94H9.98c.3-2.7 1.04-4.54 2.02-5.94zM4.06 13h3.05c.16 2.05.62 3.9 1.32 5.47A8.03 8.03 0 0 1 4.06 13zm3.05-2H4.06a8.03 8.03 0 0 1 4.37-5.47A15.7 15.7 0 0 0 7.11 11zM12 19.94c-.98-1.4-1.72-3.24-2.02-5.94h4.04c-.3 2.7-1.04 4.54-2.02 5.94zm2.63-1.47c.7-1.57 1.16-3.42 1.32-5.47h3.05a8.03 8.03 0 0 1-4.37 5.47z' },
  { id: 'settings', icon: 'M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54A.48.48 0 0 0 14 2h-4a.48.48 0 0 0-.48.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.49.49 0 0 0-.59.22L2.65 8.47a.49.49 0 0 0 .12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h4c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z' }
]
const MC_VER = '1.20.6'
const LOADER_VER = '0.19.3'
const page = ref('accueil')
const pageLabel = computed(() => t('nav.' + page.value))
function go (p) { page.value = p; acctOpen.value = false; notifOpen.value = false; ulog('page -> ' + p) }

/* ===================== Serveur ===================== */
const serverOnline = ref(false)
const players = reactive({ online: 0, max: 0 })
let srvTimer = null

/* ===================== Compte (Microsoft réel) ===================== */
const acctOpen = ref(false)
const account = reactive({ online: false })
const headSrc = computed(() => `https://mc-heads.net/head/${CONFIG.uuid || CONFIG.pseudo}`)
const bodySrc = computed(() => `https://mc-heads.net/body/${CONFIG.uuid || CONFIG.pseudo}`)
function toggleAcct () { acctOpen.value = !acctOpen.value; if (acctOpen.value) notifOpen.value = false }
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
function onDocDown (e) {
  if (acctOpen.value && !e.target.closest('.acct-chip')) acctOpen.value = false
  if (notifOpen.value && !e.target.closest('.bell-chip')) notifOpen.value = false
}

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
let minimizedThisLaunch = false
function launch () {
  ulog('LANCER cliqué')
  if (launchInfo.active) return
  if (!window.hw || !window.hw.launch) { toast(t('toast.launchUnavailable')); return }
  minimizedThisLaunch = false
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
function closeLaunchOverlay () { launchInfo.active = false; launchInfo.phase = ''; launchInfo.error = '' }
function repairLaunch () {
  ulog('repair click')
  launchInfo.active = false; launchInfo.phase = ''; launchInfo.error = ''
  nextTick(() => launch())
}
function errorHint (msg) {
  const m = ('' + (msg || '')).toLowerCase()
  if (m.includes('annulé') || m.includes('annule')) return t('launch.hint.cancelled')
  if (m.includes('java')) return t('launch.hint.java')
  if (m.includes('réseau') || m.includes('reseau') || m.includes('network') || m.includes('timeout') || m.includes('econnrefused') || m.includes('enotfound')) return t('launch.hint.network')
  if (m.includes('0xc0000005') || m.includes('crash') || m.includes('planté') || m.includes('plante')) return t('launch.hint.crash')
  return t('launch.hint.generic')
}
const launchErrorHint = computed(() => errorHint(launchInfo.error))
let _lastPhase = ''
if (typeof window !== 'undefined' && window.hw && window.hw.onMcStatus) {
  window.hw.onMcStatus((d) => {
    if (d.phase) launchInfo.phase = d.phase
    if (d.text) launchInfo.text = d.text
    if (typeof d.percent === 'number') launchInfo.percent = d.percent
    if (d.phase && d.phase !== _lastPhase) { _lastPhase = d.phase; ulog('mc status -> ' + d.phase + (d.text ? (' ' + d.text) : '')) }
    if (d.phase === 'launch' && settings.minimizeOnLaunch && !minimizedThisLaunch) { minimizedThisLaunch = true; winMin() }
    if (d.phase === 'error') {
      launchInfo.error = d.text || 'Le lancement a été interrompu.'
      logsOpen.value = true
      addNotif('error', t('notif.error.title'), launchInfo.error)
    } else if (d.phase === 'exit') {
      // Fermeture normale du jeu (code 0) : pas d'écran d'erreur, on referme discrètement l'overlay.
      launchInfo.active = false; launchInfo.phase = ''; launchInfo.error = ''
    } else if (d.phase === 'done') {
      setTimeout(() => { launchInfo.active = false; launchInfo.phase = '' }, 1800)
    }
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

/* ===================== Cosmétiques (vitrine + aperçu 3D — l'équipement réel se fait EN JEU) ===================== */
const cosmeticCategories = [
  { id: 'capes', key: 'cosmetics.cat.capes', label: 'Cape' },
  { id: 'ailes', key: 'cosmetics.cat.ailes', label: 'Aile' },
  { id: 'auras', key: 'cosmetics.cat.auras', label: 'Aura' },
  { id: 'casques', key: 'cosmetics.cat.casques', label: 'Casque' },
  { id: 'masques', key: 'cosmetics.cat.masques', label: 'Masque' },
  { id: 'compagnons', key: 'cosmetics.cat.compagnons', label: 'Compagnon' },
  { id: 'effets', key: 'cosmetics.cat.effets', label: 'Effet' },
  { id: 'emotes', key: 'cosmetics.cat.emotes', label: 'Emote' },
  { id: 'sprays', key: 'cosmetics.cat.sprays', label: 'Spray' },
  { id: 'skins', key: 'cosmetics.cat.skins', label: 'Skin' },
  { id: 'exclusifs', key: 'cosmetics.cat.exclusifs', label: 'Exclusif' }
]
const mythSuffixes = ["de Zeus", "d'Athéna", "d'Apollon", "de Poséidon", "d'Hadès", "d'Hermès", "d'Arès", "d'Artémis", "d'Héra", "de Cronos", "de l'Olympe", "des Titans", "de Perséphone", "d'Héphaïstos"]
const RARITY_LIST = ['commun', 'rare', 'epique', 'legendaire', 'mythique']
const RARITY_COLORS = { commun: '#9A94A8', rare: '#5AA9E6', epique: '#B07CE8', legendaire: '#E8C56A', mythique: '#FF5C7A' }
const RARITY_CYCLE = ['commun', 'commun', 'rare', 'commun', 'rare', 'epique', 'commun', 'rare', 'legendaire', 'mythique', 'commun', 'rare']
function rarityColor (r) { return RARITY_COLORS[r] || RARITY_COLORS.commun }
function rarityBadgeStyle (r) { const c = rarityColor(r); return { color: c, borderColor: c, background: c + '22' } }
function buildCosmetics () {
  const store = {}
  let newCounter = 0
  cosmeticCategories.forEach((cat, ci) => {
    const count = 8 + (ci % 5)
    const items = []
    for (let i = 0; i < count; i++) {
      const rarity = cat.id === 'exclusifs' ? 'mythique' : RARITY_CYCLE[(i + ci * 2) % RARITY_CYCLE.length]
      const owned = cat.id === 'exclusifs' ? (i === 0) : ((i + ci) % 3 !== 0)
      items.push({
        id: cat.id + '-' + i,
        name: cat.label + ' ' + mythSuffixes[(i + ci * 3) % mythSuffixes.length],
        category: cat.id,
        rarity,
        owned,
        newIndex: newCounter++
      })
    }
    store[cat.id] = items
  })
  return store
}
const cosmetics = reactive(buildCosmetics())
const cosCategory = ref('capes')
const cosSearch = ref('')
const cosFilterType = ref('all')
const cosFilterRarity = ref('all')
const cosFilterOwned = ref('all')
const cosSort = ref('name')
const cosSelected = ref(null)
const favCosmetics = reactive(JSON.parse(localStorage.getItem('hwFavCosmetics') || '{}'))
const equippedPreview = reactive(JSON.parse(localStorage.getItem('hwEquippedPreview') || '{}'))
function isFav (id) { return !!favCosmetics[id] }
function toggleFav (it) {
  if (favCosmetics[it.id]) delete favCosmetics[it.id]; else favCosmetics[it.id] = true
  localStorage.setItem('hwFavCosmetics', JSON.stringify(favCosmetics))
}
function isEquipped (it) { return !!it && equippedPreview[it.category] === it.id }
function toggleEquip (it) {
  if (!it || !it.owned) return
  if (equippedPreview[it.category] === it.id) delete equippedPreview[it.category]; else equippedPreview[it.category] = it.id
  localStorage.setItem('hwEquippedPreview', JSON.stringify(equippedPreview))
  ulog('equip preview -> ' + JSON.stringify(equippedPreview))
}
function selectCosCategory (id) { cosCategory.value = id; cosSearch.value = '' }
function selectCosmetic (it) { cosSelected.value = it.id; ulog('cosmetic select -> ' + it.id) }
function selectFromShowcase (it) { cosCategory.value = it.category; cosSearch.value = ''; cosSelected.value = it.id }
const cosAllItems = computed(() => cosmeticCategories.flatMap(c => cosmetics[c.id] || []))
const cosSelectedObj = computed(() => cosAllItems.value.find(i => i.id === cosSelected.value) || null)
const cosEquippedList = computed(() => Object.entries(equippedPreview).map(([, id]) => cosAllItems.value.find(i => i.id === id)).filter(Boolean))
const favShowcase = computed(() => cosAllItems.value.filter(i => isFav(i.id)).slice(0, 10))
const cosItemsFiltered = computed(() => {
  const q = cosSearch.value.trim().toLowerCase()
  let list = q ? cosAllItems.value.filter(i => i.name.toLowerCase().includes(q)) : (cosmetics[cosCategory.value] || [])
  if (cosFilterType.value !== 'all') list = list.filter(i => i.category === cosFilterType.value)
  if (cosFilterRarity.value !== 'all') list = list.filter(i => i.rarity === cosFilterRarity.value)
  if (cosFilterOwned.value === 'owned') list = list.filter(i => i.owned)
  else if (cosFilterOwned.value === 'locked') list = list.filter(i => !i.owned)
  const rarityRank = { commun: 0, rare: 1, epique: 2, legendaire: 3, mythique: 4 }
  const sorted = [...list]
  if (cosSort.value === 'name') sorted.sort((a, b) => a.name.localeCompare(b.name))
  else if (cosSort.value === 'new') sorted.sort((a, b) => b.newIndex - a.newIndex)
  else if (cosSort.value === 'rarity') sorted.sort((a, b) => rarityRank[b.rarity] - rarityRank[a.rarity])
  else if (cosSort.value === 'equipped') sorted.sort((a, b) => (isEquipped(b) ? 1 : 0) - (isEquipped(a) ? 1 : 0))
  return sorted
})

/* ===================== Amis (aucune donnée fictive) ===================== */
const friendsTab = ref('online')
const friendAddName = ref('')
const friendSearch = ref('')
function addFriend () {
  const name = friendAddName.value.trim()
  if (!name) return
  toast(t('friends.addSent'))
  ulog('add friend -> ' + name)
  friendAddName.value = ''
}

/* ===================== Langues ===================== */
const languages = [
  { code: 'fr', name: 'Français', region: 'FR', enabled: true }, { code: 'en', name: 'English', region: 'GB', enabled: true },
  { code: 'es', name: 'Español', region: 'ES', enabled: false }, { code: 'de', name: 'Deutsch', region: 'DE', enabled: false },
  { code: 'it', name: 'Italiano', region: 'IT', enabled: false }, { code: 'pt', name: 'Português', region: 'PT', enabled: false },
  { code: 'nl', name: 'Nederlands', region: 'NL', enabled: false }, { code: 'pl', name: 'Polski', region: 'PL', enabled: false },
  { code: 'ru', name: 'Русский', region: 'RU', enabled: false }, { code: 'ja', name: '日本語', region: 'JP', enabled: false },
  { code: 'zh', name: '简体中文', region: 'CN', enabled: false }, { code: 'ko', name: '한국어', region: 'KR', enabled: false }
]
const langSearch = ref('')
const languagesFiltered = computed(() => {
  const q = langSearch.value.trim().toLowerCase()
  if (!q) return languages
  return languages.filter(l => l.name.toLowerCase().includes(q) || l.code.includes(q) || (l.region || '').toLowerCase().includes(q))
})

/* ===================== Paramètres (catégories + recherche) ===================== */
const settingsCategories = [
  { id: 'jeu', key: 'settings.cat.jeu' },
  { id: 'launcher', key: 'settings.cat.launcher' },
  { id: 'apropos', key: 'settings.cat.apropos' }
]
const settingsSelectedCat = ref('jeu')
const settingsSearch = ref('')
const settingsFieldIndex = [
  { cat: 'jeu', labelKey: 'settings.memoryTitle' }, { cat: 'jeu', labelKey: 'settings.ramLabel' },
  { cat: 'jeu', labelKey: 'settings.displayTitle' }, { cat: 'jeu', labelKey: 'settings.resolutionLabel' },
  { cat: 'jeu', labelKey: 'settings.folderTitle' }, { cat: 'jeu', labelKey: 'settings.javaTitle' },
  { cat: 'launcher', labelKey: 'settings.updateFreqLabel' }, { cat: 'launcher', labelKey: 'settings.autoDownloadLabel' },
  { cat: 'launcher', labelKey: 'settings.minimizeOnLaunchLabel' }, { cat: 'launcher', labelKey: 'settings.notifEnabledLabel' },
  { cat: 'launcher', labelKey: 'settings.animationsLabel' },
  { cat: 'apropos', labelKey: 'settings.launcherTitle' }, { cat: 'apropos', labelKey: 'settings.checkUpdateBtn' },
  { cat: 'apropos', labelKey: 'settings.openFolderBtn' }
]
const settingsSearchActive = computed(() => settingsSearch.value.trim().length > 0)
const settingsMatchedCats = computed(() => {
  const set = new Set()
  if (!settingsSearchActive.value) return set
  const q = settingsSearch.value.trim().toLowerCase()
  settingsCategories.forEach(c => { if (t(c.key).toLowerCase().includes(q)) set.add(c.id) })
  settingsFieldIndex.forEach(f => { if (t(f.labelKey).toLowerCase().includes(q)) set.add(f.cat) })
  return set
})
const settingsNoResults = computed(() => settingsSearchActive.value && settingsMatchedCats.value.size === 0)
function categoryNameMatches (catId) {
  if (!settingsSearchActive.value) return false
  const q = settingsSearch.value.trim().toLowerCase()
  const c = settingsCategories.find(x => x.id === catId)
  return c ? t(c.key).toLowerCase().includes(q) : false
}
function catVisible (id) { return !settingsSearchActive.value ? settingsSelectedCat.value === id : settingsMatchedCats.value.has(id) }
function fieldVisible (labelKey, catId) {
  if (!settingsSearchActive.value) return true
  if (catId && categoryNameMatches(catId)) return true
  return t(labelKey).toLowerCase().includes(settingsSearch.value.trim().toLowerCase())
}
function selectSettingsCat (id) { settingsSelectedCat.value = id; settingsSearch.value = '' }

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
function scheduleMeteor () { meteorTimer = setTimeout(() => { if (!document.hidden && settings.animations) spawnMeteor(); scheduleMeteor() }, 11000 + Math.random() * 15000) }

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

/* ===================== Notifications ===================== */
const notifications = reactive([])
let notifId = 0
function nowLabel () {
  try { return new Date().toLocaleTimeString(lang.value === 'en' ? 'en-US' : 'fr-FR', { hour: '2-digit', minute: '2-digit' }) } catch (_) { return '' }
}
function addNotif (type, title, text) {
  if (!settings.notifEnabled) return
  notifications.unshift({ id: ++notifId, type, title, text, time: nowLabel(), read: false })
  if (notifications.length > 30) notifications.length = 30
}
const notifUnread = computed(() => notifications.filter(n => !n.read).length)
function notifIcon (type) { return { update: '⬇', event: '🌿', error: '⚠', cosmetic: '✨', friend: '👥' }[type] || '🔔' }
function markAllRead () { notifications.forEach(n => { n.read = true }) }
const notifOpen = ref(false)
function toggleNotif () {
  notifOpen.value = !notifOpen.value
  if (notifOpen.value) { acctOpen.value = false; markAllRead() }
  ulog('notif -> ' + notifOpen.value)
}
watch(() => upd.state, (v, old) => {
  if (v === 'ready' && old !== 'ready') addNotif('update', t('notif.update.title'), t('update.ready').replace('{version}', upd.version))
})

const toastMsg = ref('')
let toastTimer = null
function toast (m) { toastMsg.value = m; clearTimeout(toastTimer); toastTimer = setTimeout(() => (toastMsg.value = ''), 2400) }

/* ===================== Actus (lecteur) ===================== */
const actuIndex = ref(0)

/* ===================== Clavier / fenêtre / cycle de vie ===================== */
function onKey (e) {
  if (e.key !== 'Escape') return
  if (notifOpen.value) { notifOpen.value = false; return }
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
  try { if (window.hw && window.hw.appVersion) window.hw.appVersion().then(v => { if (v) APP_VERSION.value = v }) } catch (_) {}
  addNotif('event', t('notif.event1.title'), t('notif.event1.text'))
  addNotif('cosmetic', t('notif.cosmetic1.title'), t('notif.cosmetic1.text'))
  if (settings.updateFreq !== 'manual' && window.hw && window.hw.checkUpdate) { window.hw.checkUpdate() }
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

  <div class="bg" :class="['theme-' + theme, { switching: themeSwitching, 'anim-off': !settings.animations }]">
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

  <div class="shell" :class="{ 'anim-off': !settings.animations }">
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
        <div class="tb-right">
          <div class="bell-chip" :class="{ open: notifOpen }" @click="toggleNotif" :title="t('notif.title')">
            <svg class="bell-ic" viewBox="0 0 24 24"><path d="M12 22c1.1 0 2-.9 2-2h-4a2 2 0 0 0 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4a1.5 1.5 0 0 0-3 0v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z" /></svg>
            <span v-if="notifUnread" class="bell-badge">{{ notifUnread > 9 ? '9+' : notifUnread }}</span>
            <transition name="pop">
              <div v-if="notifOpen" class="notif-pop" @click.stop>
                <div class="np-head">
                  <b>{{ t('notif.title') }}</b>
                  <button v-if="notifications.length" class="np-mark" @click="markAllRead">{{ t('notif.markAllRead') }}</button>
                </div>
                <div class="np-list">
                  <div v-for="n in notifications" :key="n.id" class="np-item" :class="{ unread: !n.read }">
                    <span class="np-ic" :class="'ic-' + n.type">{{ notifIcon(n.type) }}</span>
                    <div class="np-body">
                      <div class="np-title">{{ n.title }}</div>
                      <div class="np-text">{{ n.text }}</div>
                      <div class="np-time">{{ n.time }}</div>
                    </div>
                  </div>
                  <div v-if="!notifications.length" class="np-empty">{{ t('notif.empty') }}</div>
                </div>
              </div>
            </transition>
          </div>
          <div class="acct-chip" :class="{ open: acctOpen }" @click="toggleAcct">
            <span class="acct-dot" :class="{ on: account.online }"></span>
            <span class="acct-name" :style="{ color: nameColor }">{{ CONFIG.pseudo }}</span>
            <img class="acct-head" :src="`https://mc-heads.net/avatar/${CONFIG.uuid || CONFIG.pseudo}/64`" alt="" draggable="false" />
            <transition name="pop">
              <div v-if="acctOpen" class="acct-pop" @click.stop>
                <div class="ap-head">
                  <img class="ap-head-img" :src="`https://mc-heads.net/avatar/${CONFIG.uuid || CONFIG.pseudo}/100`" alt="" draggable="false" @click="toggleAcct" />
                  <div class="ap-name" :style="{ color: nameColor }">{{ CONFIG.pseudo }}</div>
                  <div class="ap-status" :class="{ on: account.online }"><i></i>{{ account.online ? t('account.online') : t('account.offline') }}</div>
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
            <div class="home-profile">{{ t('home.playingAs').replace('{name}', CONFIG.pseudo) }}</div>
            <div class="home-chips">
              <div class="chip" :class="{ on: serverOnline }">
                <span class="chip-dot"></span>
                <span v-if="serverOnline">{{ t('home.onlineChip').replace('{online}', players.online).replace('{max}', players.max) }}</span>
                <span v-else>{{ t('home.offlineChip') }}</span>
              </div>
              <div class="chip chip-ghost">
                <span>Fabric {{ LOADER_VER }} · MC {{ MC_VER }}</span>
              </div>
            </div>
          </div>
          <div class="home-news">
            <div class="home-news-head"><span>{{ t('nav.actus') }}</span><button @click="go('actus')">{{ t('home.seeAll') }}</button></div>
            <div class="home-news-grid">
              <article v-for="(a, i) in CONFIG.actus.slice(0, 3)" :key="i" class="news-card" @click="go('actus')">
                <div class="nc-tag" :class="{ 'nc-tag-event': a.tag === 'Événement' }">{{ a.tag }}</div>
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
                <span class="ai-tag" :class="{ 'ai-tag-event': a.tag === 'Événement' }">{{ a.tag }}</span>
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
        <section v-else-if="page === 'cosmetiques'" class="pg pg-cos">
          <div class="pg-head"><h1>{{ t('nav.cosmetiques') }}</h1><p>{{ t('cosmetics.subtitle') }}</p></div>
          <div class="cos-notice panel">{{ t('cosmetics.notice') }}</div>
          <div class="cos-body">
            <aside class="cos-cats panel">
              <div class="cos-cats-title">{{ t('cosmetics.categories') }}</div>
              <button v-for="c in cosmeticCategories" :key="c.id" class="cos-cat" :class="{ active: cosCategory === c.id && !cosSearch.trim() }" @click="selectCosCategory(c.id)">
                <span>{{ t(c.key) }}</span>
                <span class="cos-cat-count">{{ (cosmetics[c.id] || []).length }}</span>
              </button>
            </aside>

            <div class="cos-preview panel">
              <div class="cos-viewer"><CharacterViewer :name="CONFIG.uuid || CONFIG.pseudo" :size="200" /></div>
              <div class="cos-preview-name" :style="{ color: nameColor }">{{ CONFIG.pseudo }}</div>
              <div class="cos-preview-sub">{{ t('cosmetics.previewSub') }}</div>

              <div v-if="cosEquippedList.length" class="cos-equipped-list">
                <span v-for="e in cosEquippedList" :key="e.id" class="cos-equipped-chip" :style="{ borderColor: rarityColor(e.rarity), color: rarityColor(e.rarity) }">{{ e.name }}</span>
              </div>

              <div v-if="cosSelectedObj" class="cos-selected-card">
                <div class="cos-selected-name" :style="{ color: rarityColor(cosSelectedObj.rarity) }">{{ cosSelectedObj.name }}</div>
                <div class="cos-selected-meta">
                  <span class="cos-rarity-badge" :style="rarityBadgeStyle(cosSelectedObj.rarity)">{{ t('cosmetics.rarity.' + cosSelectedObj.rarity) }}</span>
                  <span class="cos-owned-badge" :class="cosSelectedObj.owned ? 'owned' : 'locked'">{{ cosSelectedObj.owned ? t('cosmetics.owned') : t('cosmetics.locked') }}</span>
                </div>
                <div class="cos-selected-actions">
                  <button class="btn-sm" :class="{ ghost: !cosSelectedObj.owned }" :disabled="!cosSelectedObj.owned" @click="toggleEquip(cosSelectedObj)">
                    {{ isEquipped(cosSelectedObj) ? t('cosmetics.unequipBtn') : t('cosmetics.equipBtn') }}
                  </button>
                  <button class="btn-fav" :class="{ active: isFav(cosSelectedObj.id) }" @click="toggleFav(cosSelectedObj)" :title="t('cosmetics.favToggle')">★</button>
                </div>
                <p class="cos-equip-note">{{ t('cosmetics.equipNote') }}</p>
              </div>
            </div>

            <div class="cos-grid-wrap">
              <div class="cos-controls panel">
                <input class="ipt" type="text" v-model="cosSearch" :placeholder="t('cosmetics.searchPlaceholder')" />
                <select class="ipt" v-model="cosFilterType">
                  <option value="all">{{ t('cosmetics.filterAllTypes') }}</option>
                  <option v-for="c in cosmeticCategories" :key="c.id" :value="c.id">{{ t(c.key) }}</option>
                </select>
                <select class="ipt" v-model="cosFilterRarity">
                  <option value="all">{{ t('cosmetics.filterAllRarities') }}</option>
                  <option v-for="r in RARITY_LIST" :key="r" :value="r">{{ t('cosmetics.rarity.' + r) }}</option>
                </select>
                <select class="ipt" v-model="cosFilterOwned">
                  <option value="all">{{ t('cosmetics.filterAllOwned') }}</option>
                  <option value="owned">{{ t('cosmetics.filterOwned') }}</option>
                  <option value="locked">{{ t('cosmetics.filterLocked') }}</option>
                </select>
                <select class="ipt" v-model="cosSort">
                  <option value="name">{{ t('cosmetics.sortName') }}</option>
                  <option value="new">{{ t('cosmetics.sortNew') }}</option>
                  <option value="rarity">{{ t('cosmetics.sortRarity') }}</option>
                  <option value="equipped">{{ t('cosmetics.sortEquipped') }}</option>
                </select>
              </div>

              <div v-if="favShowcase.length && !cosSearch.trim()" class="cos-fav-strip">
                <span class="cos-fav-label">★ {{ t('cosmetics.favorites') }}</span>
                <button v-for="f in favShowcase" :key="'fav-' + f.id" class="cos-fav-chip" :style="{ borderColor: rarityColor(f.rarity), color: rarityColor(f.rarity) }" @click="selectFromShowcase(f)">{{ f.name }}</button>
              </div>

              <div class="cos-grid">
                <div v-for="it in cosItemsFiltered" :key="it.id" class="cos-item" :class="{ locked: !it.owned, active: cosSelected === it.id, equipped: isEquipped(it) }" @click="selectCosmetic(it)">
                  <button class="cos-fav-star" :class="{ active: isFav(it.id) }" @click.stop="toggleFav(it)" :title="t('cosmetics.favToggle')">★</button>
                  <div class="ci-name">{{ it.name }}</div>
                  <div class="ci-badges">
                    <span class="ci-badge ci-rarity" :style="rarityBadgeStyle(it.rarity)">{{ t('cosmetics.rarity.' + it.rarity) }}</span>
                    <span v-if="!it.owned" class="ci-badge locked">{{ t('cosmetics.locked') }}</span>
                    <span v-else-if="isEquipped(it)" class="ci-badge equipped">{{ t('cosmetics.equippedBadge') }}</span>
                  </div>
                </div>
                <p v-if="!cosItemsFiltered.length" class="cos-empty">{{ t('cosmetics.noResults') }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- AMIS -->
        <section v-else-if="page === 'amis'" class="pg">
          <div class="pg-head"><h1>{{ t('nav.amis') }}</h1><p>{{ t('friends.subtitle') }}</p></div>
          <div class="friends-add panel">
            <input class="ipt" type="text" v-model="friendAddName" :placeholder="t('friends.addPlaceholder')" @keyup.enter="addFriend" />
            <button class="btn-sm" @click="addFriend">{{ t('friends.addBtn') }}</button>
          </div>
          <div class="friends-tabs">
            <button :class="{ active: friendsTab === 'online' }" @click="friendsTab = 'online'">{{ t('friends.tabOnline') }}</button>
            <button :class="{ active: friendsTab === 'offline' }" @click="friendsTab = 'offline'">{{ t('friends.tabOffline') }}</button>
            <button :class="{ active: friendsTab === 'demandes' }" @click="friendsTab = 'demandes'">{{ t('friends.tabRequests') }}</button>
          </div>
          <input class="ipt friends-search" type="text" v-model="friendSearch" :placeholder="t('friends.searchPlaceholder')" />
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
          <div class="lang-wrap">
            <input class="ipt lang-search" type="text" v-model="langSearch" :placeholder="t('langues.searchPlaceholder')" />
            <div class="lang-list">
              <button v-for="l in languagesFiltered" :key="l.code" class="lang-row" :class="{ active: lang === l.code, disabled: !l.enabled }" :disabled="!l.enabled" @click="l.enabled && setLang(l.code)">
                <span class="lang-flag" :class="'flag-' + l.code" :title="l.region"></span>
                <span class="lang-name">{{ l.name }}</span>
                <span class="lang-right">
                  <span v-if="!l.enabled" class="lang-soon">{{ t('common.comingSoon') }}</span>
                  <svg v-else-if="lang === l.code" class="lang-check" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5" /></svg>
                </span>
              </button>
            </div>
          </div>
        </section>

        <!-- PARAMÈTRES -->
        <section v-else-if="page === 'settings'" class="pg pg-settings">
          <div class="pg-head"><h1>{{ t('nav.settings') }}</h1></div>
          <input class="ipt set-search" type="text" v-model="settingsSearch" :placeholder="t('settings.searchPlaceholder')" />
          <div class="set-body">
            <aside class="set-cats panel">
              <button v-for="c in settingsCategories" :key="c.id" class="set-cat" :class="{ active: !settingsSearchActive && settingsSelectedCat === c.id }" @click="selectSettingsCat(c.id)">{{ t(c.key) }}</button>
            </aside>

            <div class="set-panels">
              <div v-if="catVisible('jeu')" class="panel set-block">
                <h3>{{ t('settings.cat.jeu') }}</h3>
                <div v-show="fieldVisible('settings.ramLabel', 'jeu')" class="set-row">
                  <label>{{ t('settings.ramLabel') }}<span class="set-hint">{{ t('settings.ramNote') }}</span></label>
                  <div class="set-control"><input type="range" min="2" max="12" v-model.number="settings.ram" /><span class="set-val">{{ settings.ram }} {{ t('settings.ramUnit') }}</span></div>
                </div>
                <div v-show="fieldVisible('settings.resolutionLabel', 'jeu')" class="set-row">
                  <label>{{ t('settings.resolutionLabel') }}</label>
                  <div class="set-control"><select v-model="settings.res"><option>1920 × 1080</option><option>2560 × 1440</option><option>1600 × 900</option><option>1366 × 768</option></select></div>
                </div>
                <div v-show="fieldVisible('settings.folderTitle', 'jeu')" class="set-row set-row-stack">
                  <label>{{ t('settings.folderTitle') }}</label>
                  <input class="ipt full" type="text" v-model="settings.dir" spellcheck="false" />
                </div>
                <div v-show="fieldVisible('settings.javaTitle', 'jeu')" class="set-row set-row-stack">
                  <label>{{ t('settings.javaTitle') }}</label>
                  <input class="ipt full" type="text" v-model="settings.java" :placeholder="t('settings.javaPlaceholder')" spellcheck="false" />
                </div>
              </div>

              <div v-if="catVisible('launcher')" class="panel set-block">
                <h3>{{ t('settings.cat.launcher') }}</h3>
                <div v-show="fieldVisible('settings.updateFreqLabel', 'launcher')" class="set-row">
                  <label>{{ t('settings.updateFreqLabel') }}</label>
                  <div class="set-control">
                    <select v-model="settings.updateFreq">
                      <option value="auto">{{ t('settings.updateFreqAuto') }}</option>
                      <option value="daily">{{ t('settings.updateFreqDaily') }}</option>
                      <option value="manual">{{ t('settings.updateFreqManual') }}</option>
                    </select>
                  </div>
                </div>
                <div v-show="fieldVisible('settings.autoDownloadLabel', 'launcher')" class="set-row set-row-toggle">
                  <label>{{ t('settings.autoDownloadLabel') }}</label>
                  <label class="switch"><input type="checkbox" v-model="settings.autoDownload" /><span class="switch-track"></span></label>
                </div>
                <div v-show="fieldVisible('settings.minimizeOnLaunchLabel', 'launcher')" class="set-row set-row-toggle">
                  <label>{{ t('settings.minimizeOnLaunchLabel') }}</label>
                  <label class="switch"><input type="checkbox" v-model="settings.minimizeOnLaunch" /><span class="switch-track"></span></label>
                </div>
                <div v-show="fieldVisible('settings.notifEnabledLabel', 'launcher')" class="set-row set-row-toggle">
                  <label>{{ t('settings.notifEnabledLabel') }}</label>
                  <label class="switch"><input type="checkbox" v-model="settings.notifEnabled" /><span class="switch-track"></span></label>
                </div>
                <div v-show="fieldVisible('settings.animationsLabel', 'launcher')" class="set-row set-row-toggle">
                  <label>{{ t('settings.animationsLabel') }}</label>
                  <label class="switch"><input type="checkbox" v-model="settings.animations" /><span class="switch-track"></span></label>
                </div>
              </div>

              <div v-if="catVisible('apropos')" class="panel set-block">
                <h3>{{ t('settings.launcherTitle') }}</h3>
                <div v-show="fieldVisible('settings.checkUpdateBtn', 'apropos')" class="set-row">
                  <label>{{ t('settings.launcherTitle') }}<span class="set-hint">{{ t('settings.versionLabel').replace('{v}', APP_VERSION) }}</span></label>
                  <div class="set-control"><button class="btn-sm ghost" @click="checkUpdate">{{ t('settings.checkUpdateBtn') }}</button></div>
                </div>
                <div v-show="fieldVisible('settings.openFolderBtn', 'apropos')" class="set-row">
                  <label>{{ t('settings.folderTitle') }}</label>
                  <div class="set-control"><button class="btn-sm ghost" @click="openGameFolderBtn">{{ t('settings.openFolderBtn') }}</button></div>
                </div>
              </div>

              <p v-if="settingsNoResults" class="set-empty">{{ t('settings.noResults') }}</p>
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
          <p class="lo-error-hint">{{ launchErrorHint }}</p>
        </div>
      </template>
      <div class="lo-actions">
        <template v-if="launchInfo.phase === 'error' || launchInfo.phase === 'exit'">
          <button class="btn-sm" @click="repairLaunch">{{ t('launch.repair') }}</button>
          <button class="btn-sm ghost" @click="copyLog">{{ t('launch.copyLogs') }}</button>
          <button class="btn-sm ghost" @click="logsOpen = !logsOpen">{{ logsOpen ? t('launch.hideLogs') : t('launch.showLogs') }}</button>
          <button class="btn-sm ghost" @click="openGameFolderBtn">{{ t('launch.openFolder') }}</button>
          <button class="btn-sm ghost" @click="closeLaunchOverlay">{{ t('launch.close') }}</button>
        </template>
        <template v-else>
          <button class="btn-sm ghost" @click="logsOpen = !logsOpen">{{ logsOpen ? t('launch.hideLogs') : t('launch.showLogs') }}</button>
          <button class="btn-sm ghost" @click="openGameFolderBtn">{{ t('launch.openFolder') }}</button>
          <button class="btn-sm danger" @click="cancelLaunch">{{ t('launch.cancel') }}</button>
        </template>
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
.bg.anim-off .sky, .bg.anim-off .lightning, .bg.anim-off .planet { animation: none !important; }
.bg.anim-off .meteor { display: none; }
.bg.bg-quality-medium .stars.s2 { opacity: .22; }
.bg.bg-quality-low .stars.s2 { display: none; }
.bg.bg-quality-low .planet-glow { opacity: .5; }
.bg.bg-quality-low .lightning { display: none; }
.shell.anim-off .pg { animation: none; }

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
.topbar { height: 78px; flex: 0 0 auto; display: flex; align-items: center; padding: 0 clamp(16px, 2vw, 32px); gap: 16px; border-bottom: 1px solid rgba(255,255,255,.06); background: rgba(7,7,12,.28); }
.tb-crumb { font-family: var(--serif, Georgia, serif); font-size: 15px; letter-spacing: 1px; color: #CFC7B2; }
.tb-right { margin-left: auto; display: flex; align-items: center; gap: 14px; }
.bell-chip { position: relative; display: flex; align-items: center; justify-content: center; width: 40px; height: 40px; flex: 0 0 auto; border-radius: 999px; background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); cursor: pointer; transition: background .15s, border-color .15s; }
.bell-chip:hover, .bell-chip.open { background: rgba(232,197,106,.1); border-color: rgba(232,197,106,.3); }
.bell-ic { width: 18px; height: 18px; }
.bell-ic path { fill: #CFC7B2; transition: fill .15s; }
.bell-chip:hover .bell-ic path, .bell-chip.open .bell-ic path { fill: var(--gold, #E8C56A); }
.bell-badge { position: absolute; top: -3px; right: -3px; min-width: 16px; height: 16px; padding: 0 4px; border-radius: 999px; background: #E05555; color: #fff; font-size: 9.5px; font-weight: 800; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 0 2px rgba(9,10,16,.9); }
.notif-pop { position: absolute; top: 50px; right: 0; width: 320px; max-height: 420px; z-index: 90; display: flex; flex-direction: column; background: rgba(10,11,17,.96); border: 1px solid rgba(232,197,106,.35); border-radius: 12px; padding: 10px; box-shadow: 0 18px 44px rgba(0,0,0,.6); backdrop-filter: blur(14px); cursor: default; }
.np-head { display: flex; align-items: center; justify-content: space-between; padding: 4px 6px 10px; border-bottom: 1px solid rgba(255,255,255,.08); margin-bottom: 6px; }
.np-head b { font-family: var(--serif, Georgia, serif); font-size: 13.5px; color: #EDE8DA; letter-spacing: .5px; }
.np-mark { background: none; border: none; color: var(--gold, #E8C56A); font-size: 11px; cursor: pointer; }
.np-mark:hover { text-decoration: underline; }
.np-list { overflow-y: auto; max-height: 340px; display: flex; flex-direction: column; gap: 4px; }
.np-item { display: flex; gap: 10px; padding: 9px 8px; border-radius: 9px; }
.np-item.unread { background: rgba(232,197,106,.08); }
.np-ic { flex: 0 0 auto; width: 30px; height: 30px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 14px; background: rgba(255,255,255,.06); }
.np-ic.ic-update { background: rgba(90,169,230,.15); }
.np-ic.ic-error { background: rgba(224,85,85,.15); }
.np-ic.ic-cosmetic { background: rgba(232,197,106,.15); }
.np-ic.ic-event { background: rgba(124,203,110,.15); }
.np-ic.ic-friend { background: rgba(160,140,230,.15); }
.np-body { min-width: 0; flex: 1; }
.np-title { font-size: 12.5px; font-weight: 700; color: #EDE8DA; }
.np-text { font-size: 11.5px; color: #9A94A8; margin-top: 2px; line-height: 1.4; }
.np-time { font-size: 10px; color: #6b6b78; margin-top: 4px; }
.np-empty { padding: 30px 10px; text-align: center; color: #6b6b78; font-size: 12.5px; }
.acct-chip { position: relative; display: flex; align-items: center; gap: 10px; padding: 6px 14px 6px 16px; border-radius: 999px; background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); cursor: pointer; transition: background .15s, border-color .15s; }
.acct-chip:hover, .acct-chip.open { background: rgba(232,197,106,.1); border-color: rgba(232,197,106,.3); }
.acct-dot { width: 8px; height: 8px; border-radius: 50%; background: #6b6b78; }
.acct-dot.on { background: #7CCB6E; box-shadow: 0 0 7px #7CCB6E; }
.acct-name { font-weight: 700; font-size: 13.5px; }
/* Live 3D character bust used as the account-chip trigger (replaces the old flat head PNG) */
.acct-head-3d { width: 54px; height: 54px; flex: 0 0 auto; border-radius: 10px; overflow: hidden; background: radial-gradient(ellipse at 50% 30%, rgba(232,197,106,.1), transparent 72%); }
.acct-pop { position: absolute; top: 74px; right: 0; width: 250px; z-index: 90; background: rgba(10,11,17,.96); border: 1px solid rgba(232,197,106,.35); border-radius: 12px; padding: 8px; box-shadow: 0 18px 44px rgba(0,0,0,.6); backdrop-filter: blur(14px); cursor: default; }
.ap-head { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 16px 10px 14px; border-bottom: 1px solid rgba(255,255,255,.08); margin-bottom: 6px; }
/* Larger 3D preview inside the open profile panel (the "profile card") */
.ap-head-3d { width: 128px; height: 148px; border-radius: 12px; overflow: hidden; margin-bottom: 8px; background: radial-gradient(ellipse at 50% 26%, rgba(232,197,106,.12), transparent 72%); }
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
.content::-webkit-scrollbar, .mods-list::-webkit-scrollbar, .mods-detail::-webkit-scrollbar, .lang-list::-webkit-scrollbar, .friends-list::-webkit-scrollbar, .cos-grid::-webkit-scrollbar, .cos-cats::-webkit-scrollbar, .actus-list::-webkit-scrollbar, .np-list::-webkit-scrollbar { width: 8px; }
.content::-webkit-scrollbar-thumb, .mods-list::-webkit-scrollbar-thumb, .mods-detail::-webkit-scrollbar-thumb, .lang-list::-webkit-scrollbar-thumb, .friends-list::-webkit-scrollbar-thumb, .cos-grid::-webkit-scrollbar-thumb, .cos-cats::-webkit-scrollbar-thumb, .actus-list::-webkit-scrollbar-thumb, .np-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,.12); border-radius: 6px; }
.pg { max-width: 1600px; width: 100%; margin: 0 auto; animation: pgIn .28s ease; }
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
.home-profile { font-size: 12px; color: #9A94A8; letter-spacing: .3px; margin-top: -6px; }
.home-chips { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 6px; }
.chip { display: flex; align-items: center; gap: 8px; border-radius: 999px; padding: 8px 16px; font-size: 13px; color: #CFC7B2; background: rgba(9,10,16,.65); border: 1px solid rgba(255,255,255,.08); }
.chip-dot { width: 8px; height: 8px; border-radius: 50%; background: #E05555; box-shadow: 0 0 7px #E05555; }
.chip.on .chip-dot { background: #7CCB6E; box-shadow: 0 0 7px #7CCB6E; }
.chip-ghost { color: #8A85A0; background: rgba(255,255,255,.03); }
.home-news { width: min(1000px, 94%); }
.home-news-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.home-news-head span { font-family: var(--serif, Georgia, serif); color: #CFC7B2; letter-spacing: 1px; font-size: 14px; }
.home-news-head button { background: none; border: none; color: var(--gold, #E8C56A); cursor: pointer; font-size: 13px; }
.home-news-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; }
.news-card { background: rgba(9,10,16,.84); border: 1px solid rgba(255,255,255,.07); border-radius: 14px; padding: 16px 18px; cursor: pointer; transition: border-color .15s, transform .15s; contain: content; }
.news-card:hover { border-color: rgba(232,197,106,.4); transform: translateY(-3px); }
.nc-tag { font-size: 11px; letter-spacing: 1.5px; text-transform: uppercase; color: #5AA9E6; margin-bottom: 8px; }
.nc-tag-event, .ai-tag-event { color: var(--gold, #E8C56A); }
.news-card h3 { font-family: var(--serif, Georgia, serif); font-size: 15.5px; color: var(--gold, #E8C56A); margin-bottom: 8px; line-height: 1.3; }
.news-card p { font-size: 12.5px; color: #9A94A8; line-height: 1.55; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.nc-date { font-size: 11px; color: #6b6b78; margin-top: 10px; }

/* ===== Actualités ===== */
.actus-body { display: flex; gap: 20px; align-items: flex-start; }
.actus-list { width: 260px; flex: 0 0 auto; display: flex; flex-direction: column; gap: 6px; max-height: calc(100vh - 300px); overflow-y: auto; padding-right: 4px; }
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
.cos-body { display: grid; grid-template-columns: 200px 260px 1fr; gap: 18px; align-items: start; }
.cos-cats { display: flex; flex-direction: column; gap: 4px; padding: 10px; max-height: calc(100vh - 300px); overflow-y: auto; }
.cos-cats-title { font-size: 11px; letter-spacing: 1.5px; text-transform: uppercase; color: #6b6b78; margin: 2px 4px 6px; }
.cos-cat { display: flex; align-items: center; justify-content: space-between; gap: 8px; text-align: left; background: rgba(9,10,16,.5); border: 1px solid rgba(255,255,255,.06); color: #B9B4C6; border-radius: 9px; padding: 9px 12px; cursor: pointer; font-size: 12.5px; contain: content; }
.cos-cat:hover { border-color: rgba(232,197,106,.3); }
.cos-cat.active { color: var(--gold, #E8C56A); background: rgba(232,197,106,.1); border-color: rgba(232,197,106,.4); }
.cos-cat-count { font-size: 10.5px; color: #6b6b78; }
.cos-cat.active .cos-cat-count { color: var(--gold, #E8C56A); }
.cos-preview { padding: 20px; display: flex; flex-direction: column; align-items: center; text-align: center; gap: 6px; max-height: calc(100vh - 300px); overflow-y: auto; }
.cos-body-img { width: 120px; image-rendering: pixelated; filter: drop-shadow(0 10px 20px rgba(0,0,0,.5)); }
.cos-viewer { width: 200px; height: 260px; margin: 0 auto; flex: 0 0 auto; }
.acct-head { width: 38px; height: 38px; border-radius: 8px; image-rendering: pixelated; flex: 0 0 auto; }
.ap-head-img { width: 64px; height: 64px; border-radius: 10px; image-rendering: pixelated; cursor: pointer; align-self: center; display: block; }
.cos-preview-name { font-weight: 700; font-size: 14px; margin-top: 4px; }
.cos-preview-sub { font-size: 11.5px; color: #8A85A0; }
.cos-equipped-list { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; margin-top: 8px; }
.cos-equipped-chip { font-size: 10px; padding: 3px 9px; border-radius: 999px; border: 1px solid; background: rgba(255,255,255,.04); contain: content; }
.cos-selected-card { width: 100%; margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(255,255,255,.08); display: flex; flex-direction: column; align-items: center; gap: 8px; }
.cos-selected-name { font-weight: 700; font-size: 14px; }
.cos-selected-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: center; }
.cos-rarity-badge { font-size: 10px; padding: 3px 9px; border-radius: 999px; border: 1px solid; font-weight: 700; }
.cos-owned-badge { font-size: 9.5px; padding: 2px 8px; border-radius: 999px; text-transform: uppercase; letter-spacing: .5px; }
.cos-owned-badge.owned { background: rgba(124,203,110,.16); color: #7CCB6E; }
.cos-owned-badge.locked { background: rgba(255,255,255,.08); color: #9A94A8; }
.cos-selected-actions { display: flex; align-items: center; gap: 8px; }
.btn-fav { width: 34px; height: 34px; border-radius: 9px; background: rgba(255,255,255,.06); border: 1px solid rgba(255,255,255,.12); color: #6b6b78; cursor: pointer; font-size: 15px; line-height: 1; transition: color .15s, border-color .15s, background .15s; }
.btn-fav:hover { border-color: rgba(232,197,106,.4); color: var(--gold, #E8C56A); }
.btn-fav.active { color: var(--gold, #E8C56A); background: rgba(232,197,106,.14); border-color: rgba(232,197,106,.45); }
.cos-equip-note { font-size: 10.5px; color: #6b6b78; line-height: 1.5; max-width: 220px; }
.cos-grid-wrap { min-width: 0; }
.cos-controls { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; padding: 12px 14px; }
.cos-controls .ipt { flex: 1; min-width: 140px; }
.cos-fav-strip { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.cos-fav-label { font-size: 11.5px; color: var(--gold, #E8C56A); font-weight: 700; margin-right: 2px; }
.cos-fav-chip { font-size: 11px; padding: 5px 11px; border-radius: 999px; border: 1px solid; background: rgba(255,255,255,.04); cursor: pointer; contain: content; }
.cos-fav-chip:hover { background: rgba(232,197,106,.1); }
.cos-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; max-height: calc(100vh - 400px); overflow-y: auto; align-content: start; padding-right: 4px; }
.cos-item { position: relative; background: rgba(9,10,16,.65); border: 1px solid rgba(255,255,255,.07); border-radius: 12px; padding: 14px 40px 34px 14px; min-height: 84px; cursor: pointer; transition: border-color .15s, transform .15s; contain: content; }
.cos-item:hover { border-color: rgba(232,197,106,.3); transform: translateY(-2px); }
.cos-item.active { border-color: rgba(232,197,106,.55); background: rgba(232,197,106,.06); }
.cos-item.locked { opacity: .6; }
.cos-item.equipped { border-color: rgba(124,203,110,.5); }
.cos-fav-star { position: absolute; top: 8px; right: 8px; width: 24px; height: 24px; border-radius: 7px; background: rgba(0,0,0,.25); border: none; color: #6b6b78; cursor: pointer; font-size: 13px; line-height: 1; }
.cos-fav-star:hover { color: #CFC7B2; }
.cos-fav-star.active { color: var(--gold, #E8C56A); }
.ci-name { font-size: 13px; color: #EDE8DA; font-weight: 600; line-height: 1.3; padding-right: 20px; }
.ci-badges { position: absolute; left: 14px; bottom: 10px; display: flex; gap: 5px; flex-wrap: wrap; }
.ci-badge { font-size: 9.5px; padding: 2px 7px; border-radius: 999px; }
.ci-badge.ci-rarity { border: 1px solid; font-weight: 700; }
.ci-badge.locked { background: rgba(255,255,255,.1); color: #9A94A8; text-transform: uppercase; letter-spacing: .5px; }
.ci-badge.equipped { background: rgba(124,203,110,.18); color: #7CCB6E; text-transform: uppercase; letter-spacing: .5px; }
.cos-empty { grid-column: 1 / -1; color: #8A85A0; font-size: 13px; padding: 20px 0; }

/* ===== Amis ===== */
.friends-add { display: flex; gap: 10px; padding: 14px 16px; margin-bottom: 16px; max-width: 640px; }
.friends-add .ipt { flex: 1; }
.friends-search { width: min(320px, 100%); margin-bottom: 14px; }
.friends-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.friends-tabs button { background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); color: #B9B4C6; border-radius: 999px; padding: 8px 16px; cursor: pointer; font-size: 12.5px; }
.friends-tabs button.active { background: rgba(232,197,106,.14); color: var(--gold, #E8C56A); border-color: rgba(232,197,106,.4); }
.friends-list { display: flex; flex-direction: column; gap: 8px; max-width: 640px; max-height: calc(100vh - 300px); overflow-y: auto; padding-right: 4px; }
.friends-empty { padding: 30px; text-align: center; }
.fe-title { color: #EDE8DA; font-size: 14px; font-weight: 700; }
.fe-sub { color: #8A85A0; font-size: 12.5px; margin-top: 8px; line-height: 1.55; }

/* ===== Mods ===== */
.mods-toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.mods-toolbar .ipt { flex: 1; }
.mods-hint { background: rgba(232,197,106,.1); border: 1px solid rgba(232,197,106,.3); color: var(--gold, #E8C56A); border-radius: 9px; padding: 9px 14px; font-size: 12.5px; margin-bottom: 12px; }
.mods-body { display: grid; grid-template-columns: 1fr 280px; gap: 16px; align-items: start; }
.mods-list { display: flex; flex-direction: column; gap: 14px; min-width: 0; max-height: calc(100vh - 300px); overflow-y: auto; padding-right: 4px; }
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
.mods-detail { padding: 18px; position: sticky; top: 0; max-height: calc(100vh - 300px); overflow-y: auto; }
.mods-detail h3 { font-family: var(--serif, Georgia, serif); color: var(--gold, #E8C56A); font-size: 16px; margin-bottom: 10px; }
.md-row { display: flex; justify-content: space-between; font-size: 12px; color: #B9B4C6; margin-bottom: 6px; }
.md-desc { font-size: 12.5px; color: #9A94A8; line-height: 1.55; margin-top: 10px; }
.md-empty { color: #6b6b78; font-size: 12.5px; }

/* ===== Langues ===== */
.lang-wrap { max-width: 560px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
.lang-search { width: 100%; margin-bottom: 0; padding: 11px 15px; font-size: 14px; }
.lang-list { display: flex; flex-direction: column; gap: 8px; max-height: calc(100vh - 340px); overflow-y: auto; padding: 2px 4px 2px 2px; }
.lang-row { display: flex; align-items: center; gap: 14px; width: 100%; background: linear-gradient(180deg, rgba(20,22,32,.8), rgba(10,11,18,.8)); border: 1px solid rgba(255,255,255,.07); border-radius: 12px; padding: 13px 16px; cursor: pointer; color: #C9C4D6; font-size: 14px; text-align: left; transition: border-color .15s ease, background .15s ease, transform .12s ease; contain: content; }
.lang-row:hover:not(.disabled) { border-color: rgba(232,197,106,.4); background: linear-gradient(180deg, rgba(232,197,106,.1), rgba(20,22,32,.8)); transform: translateY(-1px); }
.lang-row.active { border-color: rgba(232,197,106,.65); background: linear-gradient(180deg, rgba(232,197,106,.18), rgba(232,197,106,.05)); box-shadow: inset 0 0 0 1px rgba(232,197,106,.25), 0 6px 18px rgba(0,0,0,.35); }
.lang-row.disabled { opacity: .42; cursor: not-allowed; }
.lang-row.disabled:hover { border-color: rgba(255,255,255,.07); transform: none; }
.lang-flag { position: relative; flex: 0 0 auto; display: inline-block; width: 22px; height: 15px; border-radius: 3px; overflow: hidden; background-repeat: no-repeat; box-shadow: 0 0 0 1px rgba(0,0,0,.4), inset 0 0 0 1px rgba(255,255,255,.08); }
.lang-row.active .lang-flag { box-shadow: 0 0 0 1px rgba(232,197,106,.6), inset 0 0 0 1px rgba(255,255,255,.12); }
.flag-fr { background-image: linear-gradient(90deg, #0055A4 0 33.33%, #FFFFFF 33.33% 66.66%, #EF4135 66.66% 100%); }
.flag-en { background-color: #00247D; background-image: linear-gradient(#fff,#fff), linear-gradient(#fff,#fff), linear-gradient(#CF142B,#CF142B), linear-gradient(#CF142B,#CF142B); background-size: 100% 5px, 6px 100%, 100% 3px, 4px 100%; background-position: center, center, center, center; }
.flag-es { background-image: linear-gradient(180deg, #AA151B 0 25%, #F1BF00 25% 75%, #AA151B 75% 100%); }
.flag-de { background-image: linear-gradient(180deg, #000000 0 33.33%, #DD0000 33.33% 66.66%, #FFCE00 66.66% 100%); }
.flag-it { background-image: linear-gradient(90deg, #009246 0 33.33%, #FFFFFF 33.33% 66.66%, #CE2B37 66.66% 100%); }
.flag-pt { background-image: linear-gradient(90deg, #046A38 0 40%, #DA291C 40% 100%); }
.flag-nl { background-image: linear-gradient(180deg, #AE1C28 0 33.33%, #FFFFFF 33.33% 66.66%, #21468B 66.66% 100%); }
.flag-pl { background-image: linear-gradient(180deg, #FFFFFF 0 50%, #DC143C 50% 100%); }
.flag-ru { background-image: linear-gradient(180deg, #FFFFFF 0 33.33%, #0039A6 33.33% 66.66%, #D52B1E 66.66% 100%); }
.flag-ja { background-color: #fff; background-image: radial-gradient(circle at 50% 50%, #BC002D 0 32%, transparent 33%); }
.flag-zh { background-color: #DE2910; background-image: radial-gradient(circle at 28% 30%, #FFDE00 0 22%, transparent 23%); }
.flag-ko { background-color: #fff; }
.flag-ko::after { content: ''; position: absolute; inset: 0; margin: auto; width: 9px; height: 9px; border-radius: 50%; background: conic-gradient(#CD2E3A 0deg 180deg, #0047A0 180deg 360deg); }
.lang-name { flex: 1; font-weight: 600; color: #EDE8DA; letter-spacing: .2px; }
.lang-row.active .lang-name { color: #FBEBB8; }
.lang-right { flex: 0 0 auto; display: flex; align-items: center; }
.lang-soon { font-size: 9.5px; letter-spacing: .5px; text-transform: uppercase; padding: 3px 9px; border-radius: 999px; background: rgba(255,255,255,.07); color: #8A85A0; border: 1px solid rgba(255,255,255,.08); }
.lang-check { width: 18px; height: 18px; fill: none; stroke: var(--gold, #E8C56A); stroke-width: 2.6; stroke-linecap: round; stroke-linejoin: round; }

/* ===== Paramètres ===== */
.pg-settings { margin-top: -12px; }
.pg-settings .pg-head { margin-bottom: 10px; }
.set-search { width: min(420px, 100%); margin-bottom: 12px; }
.set-body { display: flex; gap: 18px; align-items: flex-start; max-width: 1100px; }
.set-cats { width: 190px; flex: 0 0 auto; display: flex; flex-direction: column; gap: 4px; padding: 10px; max-height: calc(100vh - 300px); overflow-y: auto; }
.set-cat { text-align: left; background: rgba(9,10,16,.5); border: 1px solid rgba(255,255,255,.06); color: #B9B4C6; border-radius: 9px; padding: 10px 12px; cursor: pointer; font-size: 12.5px; contain: content; }
.set-cat:hover { border-color: rgba(232,197,106,.3); }
.set-cat.active { color: var(--gold, #E8C56A); background: rgba(232,197,106,.1); border-color: rgba(232,197,106,.4); }
.set-panels { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 16px; max-width: 720px; max-height: calc(100vh - 300px); overflow-y: auto; padding-right: 4px; }
.set-block { padding: 20px 22px; }
.set-block h3 { font-family: var(--serif, Georgia, serif); font-size: 13px; letter-spacing: 2px; text-transform: uppercase; color: #CFC7B2; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid rgba(232,197,106,.15); }
.set-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin: 10px 0; padding: 14px 16px; background: rgba(15,17,26,.7); border: 1px solid rgba(255,255,255,.06); border-radius: 10px; flex-wrap: wrap; transition: border-color .15s, background .15s; }
.set-row:first-of-type { margin-top: 0; }
.set-row:hover { border-color: rgba(232,197,106,.22); }
.set-row label { display: flex; flex-direction: column; gap: 4px; min-width: 180px; flex: 1 1 220px; font-size: 13px; color: #EDE8DA; font-weight: 600; }
.set-hint { font-size: 11px; color: #8A85A0; font-weight: 400; }
.set-control { flex: 1 1 200px; display: flex; align-items: center; justify-content: flex-end; gap: 10px; min-width: 140px; }
.set-control select, .set-control input[type=range] { flex: 1; min-width: 120px; }
.set-control input[type=range] { accent-color: var(--gold-dark, #D4AF37); }
.set-row-stack { flex-direction: column; align-items: stretch; justify-content: flex-start; gap: 8px; }
.set-row-stack label { margin-bottom: 2px; }
.set-row-toggle { justify-content: space-between; }
.set-val { width: 56px; text-align: right; color: var(--gold, #E8C56A); font-weight: 700; font-size: 13px; flex: 0 0 auto; }
.set-note { font-size: 11.5px; color: #8A85A0; margin-top: 6px; }
.set-empty { color: #8A85A0; font-size: 13px; padding: 20px 0; }

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
.lo-error-hint { font-size: 12px; color: #CFC7B2; max-width: 440px; margin-top: 2px; }
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

/* ===== Accessibilité ===== */
button:focus-visible, .side-item:focus-visible, .acct-chip:focus-visible, .bell-chip:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid rgba(232,197,106,.65); outline-offset: 2px; border-radius: 6px; }

/* ===== Responsive ===== */
@media (max-width: 980px) {
  .sidebar { width: 62px; }
  .side-nav { padding: 0 6px; }
  .side-item { justify-content: center; padding: 12px 6px; gap: 0; }
  .side-label { display: none; }
  .topbar { height: 64px; }
  .acct-head-3d { width: 40px; height: 40px; }
  .acct-pop { top: 60px; }
  .notif-pop { top: 56px; width: 280px; }
  .bell-chip { width: 36px; height: 36px; }
}
@media (max-width: 620px) {
  .acct-name { display: none; }
  .notif-pop { width: min(88vw, 320px); }
}
@media (max-width: 1100px) {
  .cos-body { grid-template-columns: 130px 1fr; }
  .cos-preview { display: none; }
  .cos-viewer { width: 150px; height: 220px; }
}
@media (max-width: 900px) {
  .set-body { flex-direction: column; }
  .set-cats { width: 100%; flex-direction: row; flex-wrap: wrap; max-height: none; }
  .set-panels { max-height: none; max-width: none; }
}
@media (max-width: 700px) {
  .cos-controls { flex-direction: column; align-items: stretch; }
  .cos-controls .ipt, .cos-controls select { width: 100%; }
}
</style>

import { app, BrowserWindow, ipcMain, shell, Tray, Menu } from 'electron'
import { join } from 'path'
import { readFileSync, writeFileSync, unlinkSync, existsSync, appendFileSync, mkdirSync, readdirSync, statSync } from 'fs'
import { spawn } from 'child_process'
import net from 'net'

// Accélération GPU RÉACTIVÉE (v2.6.1) : la désactiver rendait l'UI saccadée en plein écran.
// (Si un jour l'UI est noire au démarrage sur une machine, remettre app.disableHardwareAcceleration().)
let win
let LOG_PATH = null
// Journal miroir dans le dossier de travail (accessible pour diagnostic à distance).
const MIRROR_LOG = 'C:\\Users\\scott\\OneDrive\\Bureau\\Serveur + IA\\HeroWorld-Launcher\\launcher-live.log'
{
  const _log = console.log.bind(console)
  const emit = (a) => {
    const s = a.map((x) => typeof x === 'string' ? x : (() => { try { return JSON.stringify(x) } catch (_) { return String(x) } })()).join(' ')
    const line = '[' + new Date().toISOString() + '] ' + s + '\n'
    try { if (LOG_PATH) appendFileSync(LOG_PATH, line) } catch (_) {}
    try { appendFileSync(MIRROR_LOG, line) } catch (_) {}
    try { if (win && !win.isDestroyed()) win.webContents.send('log:line', s) } catch (_) {}
  }
  console.log = (...a) => { _log(...a); emit(a) }
  console.error = (...a) => { _log(...a); emit(a) }
}

function createWindow () {
  win = new BrowserWindow({
    width: 1280,
    height: 800,
    resizable: false,
    maximizable: true,
    fullscreenable: true,
    frame: false,
    backgroundColor: '#0D0D12',
    show: false,
    title: 'HEROES-WORLD',
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
      contextIsolation: true
    }
  })

  win.on('ready-to-show', () => win.show())

  // Dev : serveur Vite ; Prod : fichier construit.
  if (process.env.ELECTRON_RENDERER_URL) {
    win.loadURL(process.env.ELECTRON_RENDERER_URL)
  } else {
    win.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

// ============ MISE À JOUR AUTOMATIQUE (electron-updater) ============
// Fonctionne uniquement dans l'app installée (.exe), jamais en dev.
// Envoie les évènements au renderer -> bandeau "Mise à jour disponible".
function sendUpd (payload) { if (win && !win.isDestroyed()) win.webContents.send('update:event', payload) }
function setupUpdater () {
  if (!app.isPackaged) return
  let autoUpdater
  try { ({ autoUpdater } = require('electron-updater')) } catch (e) { return }
  autoUpdater.autoDownload = true
  autoUpdater.autoInstallOnAppQuit = true
  autoUpdater.on('update-available', (i) => sendUpd({ type: 'available', version: i.version }))
  autoUpdater.on('download-progress', (p) => sendUpd({ type: 'progress', percent: Math.round(p.percent) }))
  autoUpdater.on('update-downloaded', (i) => sendUpd({ type: 'ready', version: i.version }))
  autoUpdater.on('error', (err) => sendUpd({ type: 'error', message: String(err && err.message || err) }))
  ipcMain.on('update:install', () => { try { autoUpdater.quitAndInstall() } catch (e) {} })
  ipcMain.on('update:check', () => { try { autoUpdater.checkForUpdates() } catch (e) {} })
  try { autoUpdater.checkForUpdatesAndNotify() } catch (e) {}
}

app.whenReady().then(() => {
  try { LOG_PATH = join(app.getPath('userData'), 'heroesworld-debug.log'); appendFileSync(LOG_PATH, '\n===== BOOT ' + new Date().toISOString() + ' v' + app.getVersion() + ' =====\n') } catch (_) {}
  console.log('[BOOT] userData=' + app.getPath('userData'))
  createWindow()
  setupUpdater()
  startLocalServer()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  stopLocalServer()
  if (process.platform !== 'darwin') app.quit()
})

// Contrôles de fenêtre (barre de titre custom)
ipcMain.on('win:minimize', () => win && win.minimize())
ipcMain.on('win:maximize', () => {
  if (!win) return
  if (win.isMaximized()) win.unmaximize()
  else win.maximize()
})
ipcMain.on('win:close', () => win && win.close())

// Ouvre une URL dans le navigateur externe (jamais dans la fenêtre Electron)
ipcMain.handle('open:external', (e, url) => {
  try { if (typeof url === 'string' && /^https?:\/\//.test(url)) shell.openExternal(url) } catch (err) {}
})

// ============ SERVEUR LOCAL (TEMPORAIRE, pour tester — à remplacer par l'hébergement 24/7) ============
const SERVER_DIR = 'C:\\Users\\scott\\OneDrive\\Bureau\\Serveur + IA\\Serveur - claude'
const SERVER_BAT = SERVER_DIR + '\\start.bat'
let serverStarted = false
function isPortOpen (port, host = '127.0.0.1') {
  return new Promise((resolve) => {
    const sock = net.connect({ port, host })
    const t = setTimeout(() => { try { sock.destroy() } catch (_) {} ; resolve(false) }, 700)
    sock.on('connect', () => { clearTimeout(t); try { sock.destroy() } catch (_) {} ; resolve(true) })
    sock.on('error', () => { clearTimeout(t); resolve(false) })
  })
}

// ---- Ping Minecraft réel (Server List Ping) : nombre de joueurs en ligne / max ----
function writeVarInt (value) {
  const bytes = []
  let v = value >>> 0
  do { let temp = v & 0x7f; v >>>= 7; if (v !== 0) temp |= 0x80; bytes.push(temp) } while (v !== 0)
  return Buffer.from(bytes)
}
function readVarInt (buf, offset) {
  let numRead = 0, result = 0, read
  do {
    if (offset + numRead >= buf.length) return null
    read = buf[offset + numRead]
    result |= (read & 0x7f) << (7 * numRead)
    numRead++
    if (numRead > 5) throw new Error('VarInt trop long')
  } while ((read & 0x80) !== 0)
  return { value: result, size: numRead }
}
function mcPing (host = '127.0.0.1', port = 25565, timeout = 2500) {
  return new Promise((resolve) => {
    let done = false
    const finish = (r) => { if (!done) { done = true; try { sock.destroy() } catch (_) {} ; resolve(r) } }
    const sock = net.connect({ host, port })
    sock.setTimeout(timeout)
    sock.on('timeout', () => finish({ online: false }))
    sock.on('error', () => finish({ online: false }))
    sock.on('connect', () => {
      try {
        const hostBuf = Buffer.from(host, 'utf8')
        const portBuf = Buffer.alloc(2); portBuf.writeUInt16BE(port)
        const payload = Buffer.concat([writeVarInt(0x00), writeVarInt(765), writeVarInt(hostBuf.length), hostBuf, portBuf, writeVarInt(1)])
        sock.write(Buffer.concat([writeVarInt(payload.length), payload]))
        sock.write(Buffer.concat([writeVarInt(1), writeVarInt(0x00)]))
      } catch (_) { finish({ online: false }) }
    })
    let buf = Buffer.alloc(0)
    sock.on('data', (chunk) => {
      buf = Buffer.concat([buf, chunk])
      try {
        const len = readVarInt(buf, 0); if (!len) return
        if (buf.length < len.size + len.value) return // paquet incomplet
        let off = len.size
        const pid = readVarInt(buf, off); if (!pid) return; off += pid.size
        const jl = readVarInt(buf, off); if (!jl) return; off += jl.size
        if (buf.length < off + jl.value) return
        const obj = JSON.parse(buf.slice(off, off + jl.value).toString('utf8'))
        const p = obj.players || {}
        const motd = typeof obj.description === 'string' ? obj.description : ((obj.description && (obj.description.text || '')) || '')
        finish({ online: true, players: { online: p.online || 0, max: p.max || 0 }, version: (obj.version && obj.version.name) || '', motd })
      } catch (_) { /* attendre plus de données */ }
    })
  })
}

let serverProc = null
async function startLocalServer () {
  try {
    if (process.platform !== 'win32') return
    if (!existsSync(SERVER_BAT)) { console.log('[SRV] start.bat introuvable :', SERVER_BAT); return }
    if (serverProc && serverProc.exitCode === null) { console.log('[SRV] déjà lancé par le launcher'); return }
    if (await isPortOpen(25565)) { console.log('[SRV] serveur déjà en ligne'); serverStarted = true; return }
    console.log('[SRV] démarrage du serveur local…')
    // cmd /c "chemin\\start.bat" — Node protège les espaces ; le .bat gère le "+" via un lien temporaire.
    serverProc = spawn('cmd.exe', ['/c', SERVER_BAT], { cwd: SERVER_DIR })
    serverStarted = true
    serverProc.on('exit', () => { serverProc = null; serverStarted = false })
    serverProc.on('error', (e) => { console.log('[SRV] erreur', e); serverProc = null; serverStarted = false })
  } catch (e) { console.log('[SRV]', e) }
}
function stopLocalServer () {
  try { if (serverProc && serverProc.pid) spawn('taskkill', ['/pid', String(serverProc.pid), '/t', '/f']) } catch (_) {}
  serverProc = null; serverStarted = false
}
ipcMain.handle('server:start', () => startLocalServer())
ipcMain.handle('server:stop', () => stopLocalServer())
ipcMain.handle('log:open', () => { try { if (LOG_PATH) shell.openPath(LOG_PATH) } catch (_) {} })
ipcMain.handle('server:online', () => isPortOpen(25565))
ipcMain.handle('server:status', () => mcPing('127.0.0.1', 25565))
ipcMain.handle('log:read', () => { try { return readFileSync(LOG_PATH, 'utf8').slice(-16000) } catch (e) { return 'Journal vide ou illisible : ' + String(e) } })

// ---- Tray : le launcher se cache pendant le jeu, revient à la fermeture ----
let tray = null
function iconPath () {
  const cands = [join(process.resourcesPath || '', 'icon.png'), join(app.getAppPath(), 'build', 'icon.png'), join(app.getAppPath(), 'resources', 'icon.png')]
  return cands.find((p) => { try { return p && existsSync(p) } catch (_) { return false } }) || ''
}
function restoreLauncher () {
  try { if (win) { win.show(); win.focus() } } catch (_) {}
  if (tray) { try { tray.destroy() } catch (_) {} ; tray = null }
}
function goToTray () {
  const ic = iconPath()
  if (!ic) { try { win.minimize() } catch (_) {} ; return }
  try {
    if (!tray) {
      tray = new Tray(ic)
      tray.setToolTip('HEROES-WORLD — en jeu (clique pour rouvrir)')
      tray.setContextMenu(Menu.buildFromTemplate([
        { label: 'Ouvrir HEROES-WORLD', click: () => restoreLauncher() },
        { label: 'Quitter', click: () => { stopLocalServer(); app.quit() } }
      ]))
      tray.on('click', () => restoreLauncher())
    }
    win.hide()
  } catch (_) { try { win.minimize() } catch (__) {} }
}

// ============ AUTH MICROSOFT ============
const AZURE_CLIENT_ID = '1ce6e35a-126f-48fd-97fb-54d143ac6d45'
const REDIRECT = 'https://login.microsoftonline.com/common/oauth2/nativeclient'
const AUTHORIZE = `https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?prompt=select_account&client_id=${AZURE_CLIENT_ID}&response_type=code&scope=XboxLive.signin%20offline_access&redirect_uri=${REDIRECT}`
const TOKEN_URL = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/token'
const accountFile = () => join(app.getPath('userData'), 'hw-account.json')
function readAccount () { try { return JSON.parse(readFileSync(accountFile(), 'utf8')) } catch (_) { return null } }
function writeAccount (a) { try { writeFileSync(accountFile(), JSON.stringify(a)) } catch (_) {} }

async function msToken (body) {
  const res = await fetch(TOKEN_URL, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams(body).toString() })
  if (!res.ok) { let d = ''; try { d = (await res.text()).slice(0, 160) } catch (_) {} ; throw new Error('Jeton Microsoft ' + res.status + ' ' + d) }
  return res.json()
}
async function fullAuthFromMs (msAccessToken) {
  const { MicrosoftAuthenticator } = await import('@xmcl/user')
  const auth = new MicrosoftAuthenticator({ fetch })
  const { minecraftXstsResponse } = await auth.acquireXBoxToken(msAccessToken)
  const uhs = minecraftXstsResponse.DisplayClaims.xui[0].uhs
  const mc = await auth.loginMinecraftWithXBox(uhs, minecraftXstsResponse.Token)
  const pr = await fetch('https://api.minecraftservices.com/minecraft/profile', { headers: { Authorization: 'Bearer ' + mc.access_token } })
  if (!pr.ok) throw new Error('Ce compte ne possède pas Minecraft (profil ' + pr.status + ')')
  const profile = await pr.json()
  return { name: profile.name, uuid: profile.id, mcToken: mc.access_token }
}

ipcMain.handle('auth:get', () => { const a = readAccount(); return a ? { name: a.name, uuid: a.uuid } : null })
ipcMain.handle('auth:logout', () => { try { unlinkSync(accountFile()) } catch (_) {} ; return { ok: true } })
ipcMain.handle('auth:login', async () => new Promise((resolve) => {
  const authWin = new BrowserWindow({ width: 500, height: 660, parent: win, modal: true, show: true, autoHideMenuBar: true, title: 'Connexion Microsoft', webPreferences: { nodeIntegration: false, contextIsolation: true } })
  let done = false
  let started = false
  const finish = (r) => { if (done) return; done = true; try { authWin.close() } catch (_) {} ; resolve(r) }
  const handle = async (uri) => {
    if (!uri || uri.indexOf(REDIRECT + '?') !== 0) return
    let u
    try { u = new URL(uri) } catch (_) { return }
    const err = u.searchParams.get('error')
    if (err) return finish({ ok: false, error: err })
    const code = u.searchParams.get('code')
    if (!code || started) return
    started = true // anti-doublon : le code n'est échangé qu'une fois
    try {
      const tok = await msToken({ client_id: AZURE_CLIENT_ID, code, grant_type: 'authorization_code', redirect_uri: REDIRECT, scope: 'XboxLive.signin offline_access' })
      const acc = await fullAuthFromMs(tok.access_token)
      writeAccount({ name: acc.name, uuid: acc.uuid, refreshToken: tok.refresh_token })
      console.log('[AUTH] connecté :', acc.name)
      finish({ ok: true, name: acc.name, uuid: acc.uuid })
    } catch (e) { console.error('[AUTH]', e); finish({ ok: false, error: String(e && e.message ? e.message : e) }) }
  }
  authWin.webContents.on('will-redirect', (e, uri) => handle(uri))
  authWin.webContents.on('did-navigate', (e, uri) => handle(uri))
  authWin.webContents.on('will-navigate', (e, uri) => handle(uri))
  authWin.on('closed', () => { if (!done) { done = true; resolve({ ok: false, error: 'Fenêtre fermée' }) } })
  authWin.loadURL(AUTHORIZE)
}))

// ============ LANCEMENT DU JEU via XMCL ============
// Télécharge Minecraft 1.20.6 + Fabric 0.19.3 puis lance (hors ligne pour ce premier jet,
// SANS auto-connexion : le jeu s'ouvre sur le menu). Le vrai login Microsoft viendra ensuite.
let launching = false
let cancelRequested = false
let activeTask = null
function resolveRoot (dir) {
  const appData = process.env.APPDATA || app.getPath('appData')
  let root = String(dir || '').replace(/%APPDATA%/i, appData).trim()
  if (!root) root = join(appData, '.heroesworld')
  return root
}
ipcMain.handle('mc:launch', async (event, opts = {}) => {
  if (launching) return { ok: false, error: 'Lancement déjà en cours' }
  launching = true
  cancelRequested = false
  activeTask = null
  let lastLog = ''
  const send = (d) => { try { win && !win.isDestroyed() && win.webContents.send('mc:status', d) } catch (_) {} ; const t = d.text || JSON.stringify(d); if (t !== lastLog) { lastLog = t; console.log('[MC]', t) } }
  try {
    const path = await import('path')
    const fs = await import('fs')
    const installer = await import('@xmcl/installer')
    const core = await import('@xmcl/core')

    const MC = '1.20.6'
    const LOADER = '0.19.3'
    const appData = process.env.APPDATA || app.getPath('appData')
    let root = String(opts.dir || '').replace(/%APPDATA%/i, appData).trim()
    if (!root) root = path.join(appData, '.heroesworld')
    fs.mkdirSync(root, { recursive: true })
    startLocalServer()
    send({ phase: 'prepare', text: 'Préparation…', percent: 2 })

    // 1) Minecraft vanilla 1.20.6 avec PROGRESSION RÉELLE (installTask + startAndWait)
    send({ phase: 'meta', text: 'Récupération de la liste des versions…', percent: 4 })
    const list = await installer.getVersionList()
    const meta = list.versions.find((v) => v.id === MC)
    if (!meta) throw new Error('Version ' + MC + ' introuvable')
    send({ phase: 'install', text: 'Téléchargement de Minecraft ' + MC + '…', percent: 8 })
    if (typeof installer.installTask === 'function') {
      const vTask = installer.installTask(meta, root)
      activeTask = vTask
      let last = 0
      await vTask.startAndWait({
        onUpdate () {
          try {
            if (vTask.total > 0) {
              const p = 8 + Math.min(62, Math.round((vTask.progress / vTask.total) * 62))
              if (p !== last) { last = p; send({ phase: 'install', text: 'Téléchargement de Minecraft ' + MC + '…  ' + Math.round((vTask.progress / vTask.total) * 100) + '%', percent: p }) }
            }
          } catch (_) {}
        }
      })
    } else {
      await installer.install(meta, root)
    }

    activeTask = null
    if (cancelRequested) throw new Error('Lancement annulé')

    // 2) Fabric loader
    send({ phase: 'fabric', text: 'Installation de Fabric ' + LOADER + '…', percent: 72 })
    let versionId = await installer.installFabric({ minecraftVersion: MC, version: LOADER, minecraft: root })
    if (!versionId || typeof versionId !== 'string') versionId = 'fabric-loader-' + LOADER + '-' + MC

    // 3) Dépendances Fabric avec progression
    send({ phase: 'deps', text: 'Finalisation des fichiers…', percent: 74 })
    const resolved = await core.Version.parse(root, versionId)
    // Ne garder que les natives de l'architecture courante (Windows x64). @xmcl inclut x86+arm64 aussi,
    // ce qui corrompt l'extraction des DLL graphiques -> crash LWJGL 0xC0000005. On les retire.
    try {
      const bad = (lib) => /natives-windows-(x86|arm64)|natives-(linux|osx|macos)|-natives-linux|-natives-macos/i.test(String(lib && lib.name) + '|' + String(lib && lib.download && lib.download.path) + '|' + String(lib && lib.path))
      const before = resolved.libraries.length
      resolved.libraries = resolved.libraries.filter((lib) => !bad(lib))
      console.log('[MC] natives filtrees (x64) : ' + before + ' -> ' + resolved.libraries.length + ' libs')
    } catch (e) { console.log('[MC] filtre natives : ' + String(e)) }
    try {
      if (typeof installer.installDependenciesTask === 'function') {
        const dTask = installer.installDependenciesTask(resolved)
        activeTask = dTask
        await dTask.startAndWait({
          onUpdate () {
            try { if (dTask.total > 0) { const p = 74 + Math.min(18, Math.round((dTask.progress / dTask.total) * 18)); send({ phase: 'deps', text: 'Finalisation des fichiers…', percent: p }) } } catch (_) {}
          }
        })
      } else {
        await installer.installDependencies(resolved)
      }
    } catch (e) { console.log('[MC] installDependencies', e) }
    activeTask = null
    if (cancelRequested) throw new Error('Lancement annulé')
    send({ phase: 'ready', text: 'Fichiers prêts', percent: 93 })

    // 3b) Mod HEROES-WORLD (menu custom) -> mods/
    try {
      const modsDir = path.join(root, 'mods')
      fs.mkdirSync(modsDir, { recursive: true })
      const modName = 'heroworld-titlescreen-1.0.0.jar'
      const cands = [
        path.join(process.resourcesPath || '', 'mods', modName),
        path.join(app.getAppPath(), 'resources', 'mods', modName),
        path.join(app.getAppPath(), '..', 'resources', 'mods', modName),
        path.join(__dirname, '..', '..', 'resources', 'mods', modName)
      ]
      const src = cands.find((p) => { try { return p && fs.existsSync(p) } catch (_) { return false } })
      if (src) { fs.copyFileSync(src, path.join(modsDir, modName)); console.log('[MC] mod copié depuis', src) }
      else { console.log('[MC] mod introuvable, candidats:', cands) ; send({ phase: 'warn', text: 'Menu custom introuvable (jeu lancé sans le mod)', percent: 95 }) }
    } catch (e) { console.log('[MC] copie mod', e) }

    // 3c) Adresse du bouton "Rejoindre" -> serveur local (TEMPORAIRE : localhost)
    try {
      const cfgDir = path.join(root, 'config')
      fs.mkdirSync(cfgDir, { recursive: true })
      fs.writeFileSync(path.join(cfgDir, 'heroworld.json'), JSON.stringify({ address: 'localhost:25565' }, null, 2))
    } catch (e) { console.log('[MC] config heroworld', e) }

    // 4) Recherche robuste de Java (préférence Java 21)
    const jexe = process.platform === 'win32' ? 'javaw.exe' : 'java'
    function findJava () {
      const cands = []
      if (opts.java) cands.push(opts.java)
      if (process.env.JAVA_HOME) cands.push(path.join(process.env.JAVA_HOME, 'bin', jexe))
      const bases = [process.env['ProgramFiles'], process.env['ProgramW6432'], process.env['ProgramFiles(x86)'], process.env['LOCALAPPDATA'] ? path.join(process.env['LOCALAPPDATA'], 'Programs') : null].filter(Boolean)
      for (const base of bases) {
        for (const vendor of ['Eclipse Adoptium', 'Java', 'Microsoft', 'Zulu', 'Amazon Corretto', 'BellSoft', 'AdoptOpenJDK']) {
          const dir = path.join(base, vendor)
          try { for (const d of fs.readdirSync(dir)) { const jp = path.join(dir, d, 'bin', jexe); if (fs.existsSync(jp)) cands.push(jp) } } catch (_) {}
        }
      }
      cands.sort((a, b) => (String(b).includes('-21') || String(b).includes('jdk-21') ? 1 : 0) - (String(a).includes('-21') || String(a).includes('jdk-21') ? 1 : 0))
      for (const c of cands) { try { if (c && fs.existsSync(c)) return c } catch (_) {} }
      return jexe
    }
    const javaPath = findJava()
    console.log('[MC] javaPath =', javaPath)
    send({ phase: 'launch', text: 'Lancement du jeu…', percent: 97 })

    // 5) Compte : Microsoft si connecté (rafraîchi), sinon hors ligne
    let launchName = String(opts.name || 'Player').slice(0, 16)
    let launchUuid = '0'.repeat(32)
    let launchToken = '0'
    const savedAcc = readAccount()
    if (savedAcc && savedAcc.refreshToken) {
      try {
        send({ phase: 'auth', text: 'Connexion à ton compte Microsoft…', percent: 96 })
        const tok = await msToken({ client_id: AZURE_CLIENT_ID, refresh_token: savedAcc.refreshToken, grant_type: 'refresh_token', redirect_uri: REDIRECT, scope: 'XboxLive.signin offline_access' })
        const acc = await fullAuthFromMs(tok.access_token)
        writeAccount({ name: acc.name, uuid: acc.uuid, refreshToken: tok.refresh_token || savedAcc.refreshToken })
        launchName = acc.name; launchUuid = String(acc.uuid).replace(/-/g, ''); launchToken = acc.mcToken
        console.log('[MC] compte Microsoft OK :', launchName)
      } catch (e) { console.log('[MC] refresh auth échec -> hors ligne', e); send({ phase: 'warn', text: 'Compte non rafraîchi — lancement hors ligne' }) }
    }

    // 6) Lancement + capture des logs pour diagnostic
    const ram = Math.max(2, Math.min(16, Number(opts.ram) || 4))
    console.log('[MC] LANCEMENT version=' + versionId + ' java=' + javaPath + ' ram=' + ram + 'G name=' + launchName + ' online=' + (launchToken !== '0') + ' root=' + root)
    try {
      const natDir = path.join(root, 'versions', versionId, versionId + '-natives')
      if (fs.existsSync(natDir)) { fs.rmSync(natDir, { recursive: true, force: true }); console.log('[MC] dossier natives reinitialise pour extraction propre') }
    } catch (e) { console.log('[MC] clean natives : ' + String(e)) }
    const launchOpt = {
      gamePath: root,
      javaPath,
      version: resolved,
      minMemory: 1024,
      maxMemory: ram * 1024,
      gameProfile: { name: launchName, id: launchUuid },
      accessToken: launchToken,
      userType: 'msa',
      launcherName: 'HEROES-WORLD',
      launcherBrand: 'HeroesWorld',
      extraExecOption: { cwd: root }
    }
    try { const args = await core.generateArguments(launchOpt); console.log('[MC] COMMANDE: ' + args.join(' ')) } catch (ge) { console.log('[MC] generateArguments: ' + String(ge)) }
    const proc = await core.launch(launchOpt)
    let tail = ''
    const cap = (buf) => { try { tail = (tail + buf.toString()).slice(-4000); console.log('[MC:game]', buf.toString().trim()) } catch (_) {} }
    if (proc.stdout) proc.stdout.on('data', cap)
    if (proc.stderr) proc.stderr.on('data', cap)
    const started = Date.now()
    send({ phase: 'done', text: 'Jeu lancé — initialisation…', percent: 100 })
    const watcher = core.createMinecraftProcessWatcher(proc)
    watcher.on('minecraft-window-ready', () => { console.log('[MC] fenêtre du jeu prête -> launcher au tray'); goToTray() })
    watcher.on('error', (err) => { console.log('[MC] watcher error : ' + String(err)); send({ phase: 'error', text: 'Lancement : ' + String(err) }); launching = false; restoreLauncher(); stopLocalServer() })
    watcher.on('minecraft-exit', (e) => {
      launching = false
      restoreLauncher()
      stopLocalServer()
      console.log('[MC] sortie code=' + e.code + ' signal=' + e.signal)
      try { if (e.crashReport) console.log('[CRASH] rapport MC (' + e.crashReportLocation + ') :\n' + String(e.crashReport).slice(0, 1500)) } catch (_) {}
      // Rapport de crash natif JVM (hs_err_pid) — nomme la DLL qui plante
      try {
        const errs = fs.readdirSync(root).filter((n) => /^hs_err_pid.*\.log$/.test(n)).map((n) => ({ n, t: fs.statSync(path.join(root, n)).mtimeMs })).sort((a, b) => b.t - a.t)
        if (errs.length) { console.log('[CRASH] ' + errs[0].n + ' :\n' + fs.readFileSync(path.join(root, errs[0].n), 'utf8').split(/\r?\n/).slice(0, 34).join('\n')) }
        else console.log('[CRASH] aucun hs_err_pid dans ' + root)
      } catch (er) { console.log('[CRASH] lecture hs_err : ' + String(er)) }
      // État des fichiers natifs (glfw/opengl/lwjgl)
      try {
        const nat = path.join(root, 'versions', versionId, versionId + '-natives')
        console.log('[NATIVES] ' + nat + ' existe=' + fs.existsSync(nat) + ' -> ' + (fs.existsSync(nat) ? fs.readdirSync(nat).join(', ') : 'RIEN'))
      } catch (_) {}
      if (e.code) { send({ phase: 'error', text: 'Le jeu a planté (code ' + e.code + '). Ouvre la console (📋) pour le détail.' }) }
      else { send({ phase: 'exit', text: 'Jeu fermé' }) }
    })
    return { ok: true }
  } catch (err) {
    console.error('[MC] échec', err)
    send({ phase: 'error', text: 'Erreur : ' + (err && err.message ? err.message : String(err)) })
    launching = false
    activeTask = null
    return { ok: false, error: String(err && err.message ? err.message : err) }
  }
})

// Annuler un lancement en cours (pendant le téléchargement / la préparation)
ipcMain.handle('mc:cancel', () => {
  cancelRequested = true
  try { if (activeTask && typeof activeTask.cancel === 'function') activeTask.cancel() } catch (_) {}
  console.log('[MC] annulation demandée')
  return { ok: true }
})

// Ouvrir un dossier du jeu (racine, mods, screenshots…) dans l'explorateur Windows
ipcMain.handle('game:open-folder', async (e, dir, sub) => {
  try {
    const root = resolveRoot(dir)
    const target = sub ? join(root, String(sub)) : root
    mkdirSync(target, { recursive: true })
    const err = await shell.openPath(target)
    if (err) { console.log('[UI] ouverture dossier ÉCHEC (' + target + ') : ' + err); return { ok: false, error: err, path: target } }
    console.log('[UI] dossier ouvert : ' + target)
    return { ok: true, path: target }
  } catch (ex) { console.log('[UI] ouverture dossier exception : ' + String(ex)); return { ok: false, error: String(ex) } }
})

// Traçabilité : le renderer envoie ses interactions/erreurs -> journal de debug (+ diffusé au live log)
ipcMain.handle('ui:log', (e, msg) => { try { console.log('[UI] ' + String(msg)) } catch (_) {} ; return { ok: true } })

// Version réelle de l'application (pour l'afficher dans les Paramètres)
ipcMain.handle('app:version', () => { try { return app.getVersion() } catch (_) { return '' } })

// Lister les .jar présents dans le dossier mods/
ipcMain.handle('mods:list', (e, dir) => {
  try {
    const root = resolveRoot(dir)
    const modsDir = join(root, 'mods')
    if (!existsSync(modsDir)) return []
    return readdirSync(modsDir).filter((f) => /\.jar$/i.test(f)).map((f) => {
      let size = 0; try { size = statSync(join(modsDir, f)).size } catch (_) {}
      return { file: f, size }
    })
  } catch (_) { return [] }
})

import { app, BrowserWindow, ipcMain, shell, Tray, Menu } from 'electron'
import { join } from 'path'
import { readFileSync, writeFileSync, unlinkSync, existsSync, appendFileSync } from 'fs'
import { spawn } from 'child_process'
import net from 'net'

let win
let LOG_PATH = null
{
  const _log = console.log.bind(console)
  const emit = (a) => {
    const s = a.map((x) => typeof x === 'string' ? x : (() => { try { return JSON.stringify(x) } catch (_) { return String(x) } })()).join(' ')
    try { if (LOG_PATH) appendFileSync(LOG_PATH, '[' + new Date().toISOString() + '] ' + s + '\n') } catch (_) {}
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
ipcMain.handle('mc:launch', async (event, opts = {}) => {
  if (launching) return { ok: false, error: 'Lancement déjà en cours' }
  launching = true
  const send = (d) => { try { win && !win.isDestroyed() && win.webContents.send('mc:status', d) } catch (_) {} ; console.log('[MC]', d.text || JSON.stringify(d)) }
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

    // 2) Fabric loader
    send({ phase: 'fabric', text: 'Installation de Fabric ' + LOADER + '…', percent: 72 })
    let versionId = await installer.installFabric({ minecraftVersion: MC, version: LOADER, minecraft: root })
    if (!versionId || typeof versionId !== 'string') versionId = 'fabric-loader-' + LOADER + '-' + MC

    // 3) Dépendances Fabric avec progression
    send({ phase: 'deps', text: 'Finalisation des fichiers…', percent: 74 })
    const resolved = await core.Version.parse(root, versionId)
    try {
      if (typeof installer.installDependenciesTask === 'function') {
        const dTask = installer.installDependenciesTask(resolved)
        await dTask.startAndWait({
          onUpdate () {
            try { if (dTask.total > 0) { const p = 74 + Math.min(18, Math.round((dTask.progress / dTask.total) * 18)); send({ phase: 'deps', text: 'Finalisation des fichiers…', percent: p }) } } catch (_) {}
          }
        })
      } else {
        await installer.installDependencies(resolved)
      }
    } catch (e) { console.log('[MC] installDependencies', e) }
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
    const proc = await core.launch({
      gamePath: root,
      javaPath,
      version: versionId,
      minMemory: 1024,
      maxMemory: ram * 1024,
      gameProfile: { name: launchName, id: launchUuid },
      accessToken: launchToken,
      userType: 'msa',
      launcherName: 'HEROES-WORLD',
      launcherBrand: 'HeroesWorld'
    })
    let tail = ''
    const cap = (buf) => { try { tail = (tail + buf.toString()).slice(-4000); console.log('[MC:game]', buf.toString().trim()) } catch (_) {} }
    if (proc.stdout) proc.stdout.on('data', cap)
    if (proc.stderr) proc.stderr.on('data', cap)
    const started = Date.now()
    send({ phase: 'done', text: 'Jeu lancé !', percent: 100 })
    goToTray()
    proc.on('error', (err) => { send({ phase: 'error', text: 'Java introuvable / lancement : ' + err.message }); launching = false; restoreLauncher(); stopLocalServer() })
    proc.on('exit', (code) => {
      launching = false
      restoreLauncher()
      stopLocalServer()
      if (Date.now() - started < 12000 && code) {
        const reason = tail.split(/\r?\n/).map((l) => l.trim()).filter(Boolean).slice(-5).join('  |  ').slice(0, 320)
        send({ phase: 'error', text: 'Le jeu s\'est fermé (code ' + code + '). ' + (reason || 'voir logs') })
      } else {
        send({ phase: 'exit', text: 'Jeu fermé (code ' + code + ')' })
      }
    })
    return { ok: true }
  } catch (err) {
    console.error('[MC] échec', err)
    send({ phase: 'error', text: 'Erreur : ' + (err && err.message ? err.message : String(err)) })
    launching = false
    return { ok: false, error: String(err && err.message ? err.message : err) }
  }
})

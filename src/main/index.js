import { app, BrowserWindow, ipcMain, shell } from 'electron'
import { join } from 'path'

let win

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
  createWindow()
  setupUpdater()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
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

    // 4) Java
    const jexe = process.platform === 'win32' ? 'javaw.exe' : 'java'
    const javaPath = opts.java || (process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', jexe) : (process.platform === 'win32' ? 'javaw' : 'java'))

    // 5) Lancement (hors ligne, sans serveur -> menu custom)
    const ram = Math.max(2, Math.min(16, Number(opts.ram) || 4))
    send({ phase: 'launch', text: 'Lancement du jeu…', percent: 97 })
    const proc = await core.launch({
      gamePath: root,
      javaPath,
      version: versionId,
      minMemory: 1024,
      maxMemory: ram * 1024,
      gameProfile: { name: String(opts.name || 'Player').slice(0, 16), id: '0'.repeat(32) },
      accessToken: '0',
      userType: 'msa',
      launcherName: 'HEROES-WORLD',
      launcherBrand: 'HeroesWorld',
      extraExecOption: { detached: true }
    })
    send({ phase: 'done', text: 'Jeu lancé !', percent: 100 })
    proc.on('error', (err) => { send({ phase: 'error', text: 'Erreur process : ' + err.message }); launching = false })
    proc.on('exit', (code) => { send({ phase: 'exit', text: 'Jeu fermé (code ' + code + ')' }); launching = false })
    return { ok: true }
  } catch (err) {
    console.error('[MC] échec', err)
    send({ phase: 'error', text: 'Erreur : ' + (err && err.message ? err.message : String(err)) })
    launching = false
    return { ok: false, error: String(err && err.message ? err.message : err) }
  }
})

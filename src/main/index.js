import { app, BrowserWindow, ipcMain } from 'electron'
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

// TODO (prochaine étape) : IPC de lancement via XMCL Core
//  ipcMain.handle('mc:launch', async (e, opts) => { ... @xmcl/core launch ... })

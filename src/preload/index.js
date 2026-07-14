import { contextBridge, ipcRenderer, webFrame } from 'electron'

// Pont sécurisé renderer <-> main. On n'expose que le strict nécessaire.
contextBridge.exposeInMainWorld('hw', {
  minimize: () => ipcRenderer.send('win:minimize'),
  maximize: () => ipcRenderer.send('win:maximize'),
  close: () => ipcRenderer.send('win:close'),
  // Mises à jour automatiques
  onUpdate: (cb) => ipcRenderer.on('update:event', (_e, data) => cb(data)),
  installUpdate: () => ipcRenderer.send('update:install'),
  checkUpdate: () => ipcRenderer.send('update:check'),
  openExternal: (url) => ipcRenderer.invoke('open:external', url),
  launch: (opts) => ipcRenderer.invoke('mc:launch', opts),
  cancelLaunch: () => ipcRenderer.invoke('mc:cancel'),
  openGameFolder: (dir, sub) => ipcRenderer.invoke('game:open-folder', dir, sub),
  modsList: (dir) => ipcRenderer.invoke('mods:list', dir),
  uiLog: (msg) => ipcRenderer.invoke('ui:log', msg),
  appVersion: () => ipcRenderer.invoke('app:version'),
  onMcStatus: (cb) => ipcRenderer.on('mc:status', (_e, d) => cb(d)),
  login: () => ipcRenderer.invoke('auth:login'),
  getAccount: () => ipcRenderer.invoke('auth:get'),
  logout: () => ipcRenderer.invoke('auth:logout'),
  listAccounts: () => ipcRenderer.invoke('auth:list'),
  switchAccount: (uuid) => ipcRenderer.invoke('auth:switch', uuid),
  removeAccount: (uuid) => ipcRenderer.invoke('auth:remove', uuid),
  setZoom: (f) => { try { webFrame.setZoomFactor(Number(f) || 1) } catch (_) {} },
  logOpen: () => ipcRenderer.invoke('log:open'),
  serverOnline: () => ipcRenderer.invoke('server:online'),
  serverStatus: () => ipcRenderer.invoke('server:status'),
  logRead: () => ipcRenderer.invoke('log:read'),
  onLogLine: (cb) => ipcRenderer.on('log:line', (_e, s) => cb(s))
})

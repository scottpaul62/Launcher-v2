import { contextBridge, ipcRenderer } from 'electron'

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
  onMcStatus: (cb) => ipcRenderer.on('mc:status', (_e, d) => cb(d)),
  login: () => ipcRenderer.invoke('auth:login'),
  getAccount: () => ipcRenderer.invoke('auth:get'),
  logout: () => ipcRenderer.invoke('auth:logout'),
  logOpen: () => ipcRenderer.invoke('log:open'),
  serverOnline: () => ipcRenderer.invoke('server:online'),
  logRead: () => ipcRenderer.invoke('log:read')
})

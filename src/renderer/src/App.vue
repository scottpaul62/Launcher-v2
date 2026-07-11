<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import logo from './assets/logo.png'
import planet from './assets/planet.png'
import sky from './assets/sky.jpg'

/* ================== CONTENU ÉDITABLE ================== */
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
  ],
  mods: [
    { nom: 'HeroesWorld Title Screen', version: '1.0.0', auteur: 'HEROES-WORLD', etat: 'Actif',
      desc: "Menu principal personnalisé du client : écran titre sans Solo, connexion directe, habillage Olympe." },
    { nom: 'Fabric Loader', version: '0.19.3', auteur: 'FabricMC', etat: 'Actif',
      desc: "Chargeur de mods officiel Fabric pour Minecraft 1.20.6." }
  ]
})

/* ================== ROUTEUR ================== */
const page = ref('accueil')
const go = (p) => { if (!editMode.value) page.value = p }

/* ================== ÉTAT ================== */
const serverOnline = ref(false)
const players = ref({ online: 0, max: 20 })
const nameColor = ref(localStorage.getItem('hwNameColor') || '#FFD700')
const actuIndex = ref(0)

const settings = reactive(JSON.parse(localStorage.getItem('hwSettings') || '{"ram":4,"res":"1920 × 1080","dir":"%APPDATA%/.heroesworld"}'))
function saveSettings () { localStorage.setItem('hwSettings', JSON.stringify(settings)) }

/* Tête qui suit le curseur */
const headTilt = reactive({ rx: 0, ry: 0 })
const headSrc = computed(() => `https://mc-heads.net/avatar/${CONFIG.uuid || CONFIG.pseudo}/256`)
function onMouseMove (e) {
  const cx = window.innerWidth - 70, cy = 64
  headTilt.ry = Math.max(-26, Math.min(26, (e.clientX - cx) / window.innerWidth * 90))
  headTilt.rx = Math.max(-18, Math.min(18, -(e.clientY - cy) / window.innerHeight * 60))
  if (drag.id) onMove(e)
}
const headStyle = computed(() => ({ transform: `perspective(320px) rotateY(${headTilt.ry.toFixed(1)}deg) rotateX(${headTilt.rx.toFixed(1)}deg)` }))
function pickColor (e) { nameColor.value = e.target.value; localStorage.setItem('hwNameColor', e.target.value) }

/* Étoiles scintillantes (par-dessus la photo 4K) */
function starField (n, tile, r, op) {
  const g = []
  for (let i = 0; i < n; i++) {
    const x = Math.round(Math.random() * tile), y = Math.round(Math.random() * tile)
    const o = (op * (0.5 + Math.random() * 0.5)).toFixed(2)
    g.push(`radial-gradient(${r}px ${r}px at ${x}px ${y}px, rgba(255,255,255,${o}), transparent 60%)`)
  }
  return { backgroundImage: g.join(','), backgroundSize: `${tile}px ${tile}px` }
}
const stars1 = starField(40, 300, 1.4, 0.9)
const stars2 = starField(28, 520, 1.0, 0.6)

/* Lancement (honnête tant que XMCL pas câblé) */
function launch () { toast('Lancement via XMCL — câblage en cours (utilise le launcher actuel pour jouer)') }

/* Toast */
const toastMsg = ref('')
let toastTimer = null
function toast (m) { toastMsg.value = m; clearTimeout(toastTimer); toastTimer = setTimeout(() => (toastMsg.value = ''), 2400) }

/* Molette + Échap */
let wheelLock = false
function onWheel (e) {
  if (wheelLock || editMode.value) return
  if (page.value === 'accueil' && e.deltaY > 40) { wheelLock = true; go('actus'); setTimeout(() => (wheelLock = false), 700) }
  else if (page.value === 'actus' && e.deltaY < -40) { wheelLock = true; go('accueil'); setTimeout(() => (wheelLock = false), 700) }
}
function onKey (e) { if (e.key === 'Escape' && page.value !== 'accueil' && !editMode.value) go('accueil') }

/* ============ MODE ÉDITION : déplacer + redimensionner (sauvegarde en %) ============ */
const editMode = ref(false)
const layout = reactive(JSON.parse(localStorage.getItem('hwLayoutV2') || '{}'))
const drag = reactive({ id: null, mode: null, ox: 0, oy: 0, sx: 0, ow: 0 })
const W = () => window.innerWidth, H = () => window.innerHeight

function styleFor (id) {
  const p = layout[id]; if (!p) return {}
  const s = { position: 'absolute', left: p.left, top: p.top, right: 'auto', bottom: 'auto', transform: 'none', margin: 0 }
  if (p.w) s.width = p.w + 'px'
  return s
}
function captureLayout () {
  document.querySelectorAll('[data-ed]').forEach(el => {
    const id = el.dataset.ed
    if (layout[id]) return
    const r = el.getBoundingClientRect()
    layout[id] = { left: (r.left / W() * 100).toFixed(2) + '%', top: (r.top / H() * 100).toFixed(2) + '%', w: Math.round(r.width) }
  })
}
function startMove (id, e) {
  if (!editMode.value) return
  e.preventDefault(); e.stopPropagation()
  const r = e.currentTarget.getBoundingClientRect()
  drag.id = id; drag.mode = 'move'; drag.ox = e.clientX - r.left; drag.oy = e.clientY - r.top
}
function startResize (id, e) {
  if (!editMode.value) return
  e.preventDefault(); e.stopPropagation()
  const host = e.currentTarget.parentElement
  const r = host.getBoundingClientRect()
  drag.id = id; drag.mode = 'resize'; drag.sx = e.clientX; drag.ow = r.width
  layout[id] = { ...(layout[id] || {}), left: (r.left / W() * 100).toFixed(2) + '%', top: (r.top / H() * 100).toFixed(2) + '%' }
}
function onMove (e) {
  if (!drag.id) return
  if (drag.mode === 'move') {
    const x = e.clientX - drag.ox, y = e.clientY - drag.oy
    layout[drag.id] = { ...layout[drag.id], left: (Math.max(0, x) / W() * 100).toFixed(2) + '%', top: (Math.max(34, y) / H() * 100).toFixed(2) + '%' }
  } else {
    const w = Math.max(90, Math.min(drag.ow + (e.clientX - drag.sx), W() * 0.9))
    layout[drag.id] = { ...layout[drag.id], w: Math.round(w) }
  }
}
function endDrag () { drag.id = null; drag.mode = null }
function toggleEdit () { editMode.value = true; page.value = 'accueil'; nextTick(captureLayout) }
function applyEdit () { localStorage.setItem('hwLayoutV2', JSON.stringify(layout)); editMode.value = false }
function resetEdit () { Object.keys(layout).forEach(k => delete layout[k]); localStorage.removeItem('hwLayoutV2'); editMode.value = false }
function closeEdit () { editMode.value = false }

let rzT = null
function onResize () {
  document.body.classList.add('resizing')
  clearTimeout(rzT); rzT = setTimeout(() => document.body.classList.remove('resizing'), 200)
}
onMounted(() => {
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', endDrag)
  window.addEventListener('wheel', onWheel, { passive: true })
  window.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('mouseup', endDrag)
  window.removeEventListener('wheel', onWheel)
  window.removeEventListener('keydown', onKey)
})

/* ============ MISE À JOUR AUTO (bandeau) ============ */
const upd = reactive({ state: null, percent: 0, version: '' })
if (typeof window !== 'undefined' && window.hw && window.hw.onUpdate) {
  window.hw.onUpdate((d) => {
    if (d.type === 'available') { upd.state = 'downloading'; upd.version = d.version || '' }
    else if (d.type === 'progress') { upd.state = 'downloading'; upd.percent = d.percent || 0 }
    else if (d.type === 'ready') { upd.state = 'ready'; upd.version = d.version || '' }
    else if (d.type === 'error') { upd.state = null }
  })
}
function installUpdate () { if (window.hw && window.hw.installUpdate) window.hw.installUpdate() }

const dock = [
  { id: 'accueil', tip: 'Accueil' }, { id: 'mods', tip: 'Mods' }, { id: 'boutique', tip: 'Boutique' },
  { id: 'amis', tip: 'Amis' }, { id: 'settings', tip: 'Paramètres' }, { id: 'aide', tip: 'Aide' }
]
function winMin () { if (window.hw) window.hw.minimize() }
function winMax () { if (window.hw) window.hw.maximize() }
function winClose () { if (window.hw) window.hw.close() }
</script>

<template>
  <div class="titlebar">
    <span class="tb-title">⚡ HEROES-WORLD</span>
    <div class="tb-controls">
      <button @click="winMin">─</button><button @click="winMax">▢</button><button class="close" @click="winClose">✕</button>
    </div>
  </div>

  <!-- Décor : vraie photo 4K + étoiles + planète fondue -->
  <div class="bg">
    <div class="sky" :style="{ backgroundImage: `url(${sky})` }"></div>
    <div class="stars" :style="stars1"></div>
    <div class="stars s2" :style="stars2"></div>
    <div class="planet-glow"></div>
    <img class="planet" :src="planet" alt="" />
    <div class="scrim" :class="{ soft: editMode }"></div>
  </div>

  <div class="app" :class="{ edit: editMode }">
    <!-- Barre du mode édition -->
    <div class="editbar" v-if="editMode">
      <span>🖌 Glisse pour déplacer · tire le coin ◢ pour agrandir</span>
      <button class="reset" @click="resetEdit">Réinitialiser</button>
      <button class="cancel" @click="closeEdit">Fermer</button>
      <button class="apply" @click="applyEdit">Appliquer</button>
    </div>
    <button v-else class="brush" @click="toggleEdit" title="Personnaliser l'interface">🖌</button>

    <!-- Statut : UNIQUEMENT sur l'accueil -->
    <div v-if="page === 'accueil'" class="status editable" :class="{ on: serverOnline }" data-ed="status"
         :style="styleFor('status')" @mousedown="startMove('status',$event)">
      <span class="dot"></span>
      <span v-if="serverOnline">En ligne · {{ players.online }}/{{ players.max }} joueurs</span>
      <span v-else>Hors ligne</span>
    </div>

    <!-- Compte -->
    <div class="account editable" data-ed="account" :style="styleFor('account')" @mousedown="startMove('account',$event)">
      <input type="color" class="name-color" :value="nameColor" @input="pickColor" title="Couleur du pseudo" />
      <span class="pseudo" :style="{ color: nameColor }">{{ CONFIG.pseudo }}</span>
      <div class="avatar"><img class="head" :src="headSrc" :style="headStyle" draggable="false" alt="" /></div>
      <span v-if="editMode" class="ed-h" @mousedown="startResize('account',$event)"></span>
    </div>

    <section v-if="page === 'accueil'" class="home">
      <div class="brand editable" data-ed="brand" :style="styleFor('brand')" @mousedown="startMove('brand',$event)">
        <img class="logo" :src="logo" alt="HEROES-WORLD" draggable="false" />
        <div class="tagline">L'OLYMPE VOUS ATTEND</div>
        <span v-if="editMode" class="ed-h" @mousedown="startResize('brand',$event)"></span>
      </div>

      <button class="launch editable" data-ed="launch" @click="launch" :style="styleFor('launch')" @mousedown="startMove('launch',$event)">
        LANCER
        <span v-if="editMode" class="ed-h" @mousedown="startResize('launch',$event)"></span>
      </button>

      <div class="widget quest editable" data-ed="quest" :style="styleFor('quest')" @mousedown="startMove('quest',$event)">
        <div class="w-head"><span class="d gold"></span> OBJECTIF DU JOUR</div>
        <div class="w-title">⚔️ Rejoindre l'Olympe</div>
        <div class="bar"><i style="width:35%"></i></div>
        <div class="w-foot"><span>Récompense : 100 ₯</span><button @click="!editMode && toast('Bientôt disponible')">VOIR</button></div>
        <span v-if="editMode" class="ed-h" @mousedown="startResize('quest',$event)"></span>
      </div>

      <div class="widget announce editable" data-ed="announce" @click="go('actus')" :style="styleFor('announce')" @mousedown="startMove('announce',$event)">
        <div class="w-head"><span class="d cyan"></span> NOUVEAUTÉ</div>
        <div class="w-title small">⚡ La Hache de Zeus est arrivée</div>
        <div class="w-text">Un artefact légendaire forgé dans la foudre. Clique pour lire les actus.</div>
        <span v-if="editMode" class="ed-h" @mousedown="startResize('announce',$event)"></span>
      </div>
    </section>

    <section v-else-if="page === 'actus'" class="page">
      <div class="page-head"><button class="back" @click="go('accueil')">‹ Accueil</button><h1>Actualités</h1></div>
      <div class="page-body actus">
        <article class="glass">
          <div class="a-tag">{{ CONFIG.actus[actuIndex].tag }}</div>
          <h2>{{ CONFIG.actus[actuIndex].titre }}</h2>
          <div class="a-meta">{{ CONFIG.actus[actuIndex].date }} · HEROES-WORLD</div>
          <p>{{ CONFIG.actus[actuIndex].texte }}</p>
          <div class="pager">
            <button :disabled="actuIndex===0" @click="actuIndex--">‹</button>
            <span>{{ actuIndex + 1 }} / {{ CONFIG.actus.length }}</span>
            <button :disabled="actuIndex===CONFIG.actus.length-1" @click="actuIndex++">›</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="page === 'mods'" class="page">
      <div class="page-head"><button class="back" @click="go('accueil')">‹ Accueil</button><h1>Mods</h1></div>
      <div class="page-body mods">
        <div class="mod glass" v-for="m in CONFIG.mods" :key="m.nom">
          <div class="mod-top"><span class="d gold"></span><b>{{ m.nom }}</b><span class="mod-ver">v{{ m.version }} · {{ m.etat }}</span></div>
          <p>{{ m.desc }}</p><div class="mod-sub">{{ m.auteur }}</div>
        </div>
      </div>
    </section>

    <section v-else-if="page === 'boutique'" class="page">
      <div class="page-head"><button class="back" @click="go('accueil')">‹ Accueil</button><h1>Boutique</h1></div>
      <div class="page-body center"><div class="info-card glass"><div class="icon">🛒</div><p>La boutique ouvrira avec le serveur public : grades, cosmétiques et coffres mythologiques en Drachmes.</p><button disabled>Bientôt disponible</button></div></div>
    </section>
    <section v-else-if="page === 'amis'" class="page">
      <div class="page-head"><button class="back" @click="go('accueil')">‹ Accueil</button><h1>Amis</h1></div>
      <div class="page-body center"><div class="info-card glass"><div class="icon">👥</div><p>Le système d'amis arrive avec l'ouverture publique. Retrouve la communauté sur le Discord du serveur.</p><button disabled>Discord — bientôt</button></div></div>
    </section>
    <section v-else-if="page === 'aide'" class="page">
      <div class="page-head"><button class="back" @click="go('accueil')">‹ Accueil</button><h1>Aide & Support</h1></div>
      <div class="page-body faq">
        <div class="q glass"><b>Comment jouer ?</b><p>Clique sur LANCER : le jeu se télécharge puis s'ouvre sur le menu HEROES-WORLD.</p></div>
        <div class="q glass"><b>Le serveur est hors ligne ?</b><p>Tu ne pourras rejoindre qu'une fois la pastille verte affichée.</p></div>
        <div class="q glass"><b>Un souci technique ?</b><p>Le launcher journalise tout pour faciliter le diagnostic.</p></div>
      </div>
    </section>

    <section v-else-if="page === 'settings'" class="page">
      <div class="page-head"><button class="back" @click="go('accueil')">‹ Accueil</button><h1>Paramètres</h1></div>
      <div class="page-body settings">
        <div class="set-grid">
          <div class="set-block glass">
            <h3>Mémoire</h3>
            <div class="row"><label>RAM allouée</label><input type="range" min="2" max="12" v-model.number="settings.ram" @change="saveSettings"><span class="val">{{ settings.ram }} Go</span></div>
            <p class="note">Recommandé : 4 Go. Appliqué au lancement (câblage XMCL en cours).</p>
          </div>
          <div class="set-block glass">
            <h3>Affichage</h3>
            <div class="row"><label>Résolution</label><select v-model="settings.res" @change="saveSettings"><option>1920 × 1080</option><option>2560 × 1440</option><option>1600 × 900</option><option>1366 × 768</option></select></div>
          </div>
          <div class="set-block glass">
            <h3>Dossier du jeu</h3>
            <div class="row"><input type="text" v-model="settings.dir" @change="saveSettings" spellcheck="false"></div>
            <p class="note">Où sont installés Minecraft, Java et les mods.</p>
          </div>
          <div class="set-block glass">
            <h3>Compte</h3>
            <p class="note">Connecté : {{ CONFIG.pseudo }}. Gestion Microsoft — câblage XMCL en cours.</p>
          </div>
        </div>
      </div>
    </section>

    <nav class="dock editable" data-ed="dock" :style="styleFor('dock')" @mousedown="startMove('dock',$event)">
      <button v-for="d in dock" :key="d.id" :class="{ active: page === d.id }" :data-tip="d.tip" @click="go(d.id)">
        <span class="ic" :data-i="d.id"></span>
      </button>
    </nav>

    <div v-if="upd.state" class="upd" :class="{ ready: upd.state === 'ready' }">
      <span class="upd-ic">⬇</span>
      <span v-if="upd.state === 'downloading'">Mise à jour en cours… {{ upd.percent }}%</span>
      <template v-else>
        <span>Mise à jour {{ upd.version }} prête.</span>
        <button @click="installUpdate">Installer &amp; redémarrer</button>
      </template>
    </div>

    <div class="toast" :class="{ show: toastMsg }">{{ toastMsg }}</div>
  </div>
</template>

<style scoped>
.titlebar { position: fixed; top: 0; left: 0; right: 0; height: 34px; z-index: 100; display: flex; align-items: center; padding-left: 14px; -webkit-app-region: drag; background: rgba(10,11,16,.55); border-bottom: 1px solid rgba(212,175,55,.28); }
.tb-title { font-family: var(--serif); font-weight: 700; letter-spacing: 3px; font-size: 12px; color: #CFC7B2; }
.tb-controls { margin-left: auto; height: 100%; display: flex; -webkit-app-region: no-drag; }
.tb-controls button { width: 44px; height: 100%; border: none; background: none; color: #9A94A8; cursor: pointer; }
.tb-controls button:hover { background: rgba(212,175,55,.18); color: var(--gold); }
.tb-controls .close:hover { background: #E05555; color: #fff; }

/* Décor */
.bg { position: fixed; inset: 0; z-index: 0; overflow: hidden; background: #05060A; }
.sky { position: absolute; inset: -5%; background-size: cover; background-position: center; will-change: transform; animation: skydrift 75s ease-in-out infinite alternate; }
@keyframes skydrift { from { transform: scale(1.06) translate(0,0) } to { transform: scale(1.13) translate(-2%, -1.2%) } }
.stars { position: absolute; inset: -60px; background-repeat: repeat; opacity: .8; animation: tw 6s ease-in-out infinite alternate; }
.stars.s2 { animation: tw 9s ease-in-out -3s infinite alternate; }
@keyframes tw { from { opacity: .35 } to { opacity: .85 } }
/* Planète : plus grande, fondue dans le noir (pas de coupure franche) */
.planet-glow { position: absolute; right: -14vw; top: 50%; width: min(70vw, 1000px); aspect-ratio: 1; transform: translateY(-50%); border-radius: 50%; background: radial-gradient(circle at 50% 50%, rgba(0,0,0,.55) 30%, transparent 70%); pointer-events: none; }
.planet { position: absolute; right: -7vw; top: 50%; transform: translateY(-50%); width: min(52vw, 780px); opacity: .98; -webkit-mask-image: radial-gradient(circle at 50% 50%, #000 66%, rgba(0,0,0,.55) 80%, transparent 92%); mask-image: radial-gradient(circle at 50% 50%, #000 66%, rgba(0,0,0,.55) 80%, transparent 92%); filter: drop-shadow(0 0 70px rgba(0,0,0,.6)); animation: float 120s ease-in-out infinite alternate; }
@keyframes float { from { transform: translateY(-50%) } to { transform: translateY(-53%) } }
.scrim { position: absolute; inset: 0; background: radial-gradient(ellipse 78% 70% at 40% 46%, rgba(5,6,10,.12), rgba(5,6,10,.5) 90%); transition: background .2s; }
.scrim.soft { background: rgba(5,6,10,.14); }

.app { position: fixed; inset: 0; z-index: 10; }
.glass { background: rgba(9,10,16,.82); border: 1px solid rgba(255,255,255,.07); }

/* Mode édition */
.brush { position: fixed; right: 18px; bottom: 70px; z-index: 70; width: 42px; height: 42px; border-radius: 12px; cursor: pointer; background: rgba(12,13,19,.85); border: 1px solid rgba(255,255,255,.1); color: #EDE8DA; font-size: 18px; }
.brush:hover { background: rgba(212,175,55,.2); border-color: rgba(212,175,55,.5); }
.editbar { position: fixed; left: 50%; bottom: 72px; transform: translateX(-50%); z-index: 300; display: flex; align-items: center; gap: 10px; background: rgba(8,9,13,.95); border: 1px solid rgba(212,175,55,.45); border-radius: 14px; padding: 10px 16px; color: #EDE8DA; font-size: 13px; }
.editbar button { border: none; border-radius: 8px; padding: 8px 15px; cursor: pointer; font-weight: 700; font-size: 13px; }
.editbar .apply { background: linear-gradient(180deg,var(--gold),#D4AF37); color: #1c1607; }
.editbar .cancel { background: rgba(255,255,255,.1); color: #EDE8DA; }
.editbar .reset { background: rgba(224,85,85,.22); color: #E05555; }
.app.edit .editable { outline: 1px dashed rgba(212,175,55,.65); outline-offset: 5px; cursor: move; }
.ed-h { position: absolute; right: -7px; bottom: -7px; width: 16px; height: 16px; border-radius: 4px; background: var(--gold); border: 2px solid #1c1607; cursor: nwse-resize; z-index: 5; box-shadow: 0 0 8px rgba(0,0,0,.5); }

/* Half-stage = tout reste dans une largeur max centrée (adaptatif petit ↔ plein écran) */
.status { position: absolute; top: 46px; left: max(20px, calc(50vw - 780px)); display: flex; align-items: center; gap: 8px; border-radius: 999px; padding: 7px 15px; font-size: 13px; color: #CFC7B2; background: rgba(9,10,16,.78); border: 1px solid rgba(255,255,255,.07); }
.status .dot { width: 9px; height: 9px; border-radius: 50%; background: #E05555; box-shadow: 0 0 8px #E05555; } .status.on .dot { background: #7CCB6E; box-shadow: 0 0 8px #7CCB6E; }
.account { position: absolute; top: 40px; right: max(20px, calc(50vw - 780px)); display: flex; align-items: center; gap: 10px; }
.name-color { width: 18px; height: 18px; border: none; background: none; padding: 0; border-radius: 5px; cursor: pointer; opacity: .55; } .name-color:hover { opacity: 1; }
.pseudo { font-weight: 700; }
.avatar { width: clamp(50px,4.6vw,64px); height: clamp(50px,4.6vw,64px); border-radius: 50%; box-shadow: 0 0 0 2px rgba(212,175,55,.5); display: grid; place-items: center; background: rgba(0,0,0,.3); }
.head { width: 78%; height: 78%; border-radius: 14%; image-rendering: pixelated; transition: transform .12s ease-out; filter: drop-shadow(0 4px 10px rgba(0,0,0,.5)); }

/* Accueil : bloc centré, logo+tagline groupés, redimensionnables */
.home { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: clamp(28px, 6vh, 74px); padding-bottom: 5vh; }
.brand { position: relative; display: flex; flex-direction: column; align-items: center; gap: 8px; width: clamp(240px, 30vw, 500px); }
.logo { width: 100%; filter: drop-shadow(0 12px 34px rgba(0,0,0,.55)); }
.tagline { font-family: var(--serif); letter-spacing: 7px; font-size: clamp(10px,.85vw,14px); color: #CFC7B2; }
.launch { position: relative; font-family: var(--serif); font-weight: 900; letter-spacing: 8px; font-size: clamp(17px,1.5vw,23px); color: #1c1607; border: none; border-radius: 11px; padding: clamp(11px,1.3vh,17px) clamp(44px,5.5vw,86px); cursor: pointer; background: linear-gradient(180deg,#FFE98A,#FFD700 30%,#D4AF37 75%,#8B6914); box-shadow: 0 6px 0 #6e5310, 0 12px 34px rgba(255,215,0,.35), inset 0 1px 0 rgba(255,255,255,.6); transition: filter .15s, transform .12s; }
.launch:hover { filter: brightness(1.08); transform: translateY(-2px); }
.app.edit .launch:hover { transform: none; }

.widget { position: absolute; width: clamp(230px, 21vw, 310px); border-radius: 14px; padding: 14px 16px; color: #EDE8DA; box-shadow: 0 14px 36px rgba(0,0,0,.45); background: rgba(9,10,16,.82); border: 1px solid rgba(255,255,255,.07); }
.widget.quest { left: max(20px, calc(50vw - 780px)); bottom: 96px; } .widget.announce { left: max(20px, calc(50vw - 780px)); bottom: 300px; cursor: pointer; }
.w-head { font-size: 10px; letter-spacing: 2px; color: #9A94A8; display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.d { width: 8px; height: 8px; border-radius: 50%; } .d.gold { background: var(--gold); box-shadow: 0 0 8px var(--gold); } .d.cyan { background: #4FC3F7; box-shadow: 0 0 8px #4FC3F7; }
.w-title { font-family: var(--serif); font-size: 15px; color: var(--gold); margin-bottom: 8px; } .w-title.small { font-size: 14px; }
.w-text { font-size: 12px; color: #9A94A8; line-height: 1.5; }
.bar { height: 7px; border-radius: 999px; background: rgba(255,255,255,.08); overflow: hidden; margin-bottom: 10px; } .bar i { display: block; height: 100%; background: linear-gradient(90deg,#4FC3F7,var(--gold)); }
.w-foot { display: flex; align-items: center; justify-content: space-between; font-size: 11px; color: #9A94A8; }
.w-foot button { background: linear-gradient(180deg,var(--gold),#D4AF37); color: #1c1607; border: none; border-radius: 7px; padding: 5px 12px; font-weight: 800; cursor: pointer; }

/* Pages secondaires */
.page { position: absolute; inset: 0; display: flex; flex-direction: column; }
.page-head { display: flex; align-items: center; gap: 20px; padding: 48px clamp(20px,4vw,54px) 6px max(clamp(20px,4vw,54px), calc(50vw - 780px)); }
.back { background: rgba(9,10,16,.8); border: 1px solid rgba(255,255,255,.09); color: #EDE8DA; border-radius: 10px; padding: 8px 16px; cursor: pointer; } .back:hover { background: rgba(212,175,55,.18); color: var(--gold); }
.page-head h1 { font-family: var(--serif); font-size: clamp(20px,2.4vw,30px); color: #EDE8DA; letter-spacing: 1px; text-shadow: 0 2px 12px rgba(0,0,0,.6); }
.page-body { flex: 1; overflow-y: auto; padding: clamp(10px,2vh,20px) clamp(20px,4vw,54px) 96px; }
.page-body.center { display: flex; align-items: center; justify-content: center; }
.actus { display: flex; justify-content: center; }
.actus article { max-width: 760px; border-radius: 16px; padding: 30px 34px; } .a-tag { font-size: 11px; letter-spacing: 2px; text-transform: uppercase; color: #4FC3F7; margin-bottom: 12px; }
.actus h2 { font-family: var(--serif); font-size: clamp(22px,2.8vw,36px); color: var(--gold); line-height: 1.15; margin-bottom: 8px; } .a-meta { font-size: 12px; color: #8A85A0; margin-bottom: 18px; } .actus p { font-size: clamp(14px,1.2vw,16px); line-height: 1.7; color: #D8D3E0; }
.pager { display: flex; align-items: center; justify-content: center; gap: 18px; margin-top: 22px; }
.pager button { width: 38px; height: 38px; border-radius: 10px; border: 1px solid rgba(255,255,255,.1); background: rgba(255,255,255,.05); color: #EDE8DA; font-size: 19px; cursor: pointer; } .pager button:disabled { opacity: .3; cursor: not-allowed; }
.mods { max-width: 860px; margin: 0 auto; display: flex; flex-direction: column; gap: 12px; }
.mod { border-radius: 12px; padding: 16px 20px; } .mod-top { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; } .mod-top b { color: #EDE8DA; } .mod-ver { margin-left: auto; font-size: 12px; color: #8A85A0; } .mod p { font-size: 14px; color: #C9C4D6; line-height: 1.6; } .mod-sub { font-size: 12px; color: #8A85A0; margin-top: 6px; }
.info-card { max-width: 520px; text-align: center; border-radius: 16px; padding: 40px 34px; } .info-card .icon { font-size: 42px; margin-bottom: 14px; } .info-card p { font-size: 15px; line-height: 1.7; color: #D8D3E0; margin-bottom: 22px; } .info-card button { background: linear-gradient(180deg,var(--gold),#D4AF37); color: #1c1607; border: none; border-radius: 10px; padding: 10px 24px; font-weight: 700; } .info-card button:disabled { filter: grayscale(.5) brightness(.7); }
.faq { max-width: 720px; margin: 0 auto; display: flex; flex-direction: column; gap: 12px; } .faq .q { border-radius: 12px; padding: 16px 20px; } .faq .q b { color: var(--gold); font-family: var(--serif); } .faq .q p { font-size: 14px; color: #C9C4D6; margin-top: 8px; line-height: 1.6; }
.settings .set-grid { max-width: 760px; margin: 0 auto; display: flex; flex-direction: column; gap: 14px; }
.set-block { border-radius: 12px; padding: 18px 22px; }
.set-block h3 { font-family: var(--serif); font-size: 14px; letter-spacing: 2px; text-transform: uppercase; color: #CFC7B2; margin-bottom: 12px; }
.set-block .row { display: flex; align-items: center; gap: 16px; margin: 8px 0; } .set-block label { width: 130px; font-size: 14px; }
.set-block input[type=range] { flex: 1; accent-color: var(--gold-dark); } .set-block .val { width: 60px; text-align: right; color: var(--gold); font-weight: 700; }
.set-block select, .set-block input[type=text] { flex: 1; background: rgba(5,6,10,.6); border: 1px solid rgba(255,255,255,.16); color: #EDE8DA; border-radius: 8px; padding: 9px 12px; }
.set-block .note { font-size: 12px; color: #8A85A0; margin-top: 8px; }

/* Dock : couleurs par icône + animation au survol */
.dock { position: absolute; left: 50%; bottom: 14px; transform: translateX(-50%); z-index: 40; display: flex; gap: 4px; background: rgba(8,9,13,.72); border: 1px solid rgba(255,255,255,.08); border-radius: 16px; padding: 6px 8px; }
.dock button { position: relative; width: 46px; height: 46px; border: none; border-radius: 11px; background: none; cursor: pointer; display: grid; place-items: center; transition: transform .16s ease, background .16s; } .dock button:hover { background: rgba(255,255,255,.08); transform: translateY(-5px); }
.dock button.active::after { content: ''; position: absolute; left: 24%; right: 24%; bottom: 4px; height: 2.5px; border-radius: 2px; background: currentColor; box-shadow: 0 0 7px currentColor; }
.dock button::before { content: attr(data-tip); position: absolute; top: -32px; left: 50%; transform: translateX(-50%); opacity: 0; transition: .15s; background: #0A0B11; color: #EDE8DA; border: 1px solid rgba(212,175,55,.3); border-radius: 7px; padding: 3px 9px; font-size: 11px; white-space: nowrap; pointer-events: none; } .dock button:hover::before { opacity: 1; }
.ic { width: 22px; height: 22px; display: inline-block; background: #B9B4C6; transition: background .16s, transform .16s; -webkit-mask-size: contain; -webkit-mask-repeat: no-repeat; -webkit-mask-position: center; mask-size: contain; mask-repeat: no-repeat; mask-position: center; } .dock button:hover .ic { transform: scale(1.14); }
/* couleurs signature par onglet */
.dock button[data-tip=Accueil] { color: #FFD700; } .dock button[data-tip=Accueil]:hover .ic, .dock button[data-tip=Accueil].active .ic { background: #FFD700; }
.dock button[data-tip=Mods] { color: #4FC3F7; } .dock button[data-tip=Mods]:hover .ic, .dock button[data-tip=Mods].active .ic { background: #4FC3F7; }
.dock button[data-tip=Boutique] { color: #7CCB6E; } .dock button[data-tip=Boutique]:hover .ic, .dock button[data-tip=Boutique].active .ic { background: #7CCB6E; }
.dock button[data-tip=Amis] { color: #C792EA; } .dock button[data-tip=Amis]:hover .ic, .dock button[data-tip=Amis].active .ic { background: #C792EA; }
.dock button[data-tip=Paramètres] { color: #FFB74D; } .dock button[data-tip=Paramètres]:hover .ic, .dock button[data-tip=Paramètres].active .ic { background: #FFB74D; }
.dock button[data-tip=Aide] { color: #64B5F6; } .dock button[data-tip=Aide]:hover .ic, .dock button[data-tip=Aide].active .ic { background: #64B5F6; }
.ic[data-i=accueil]{ -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Cpath d='M3 11 12 3l9 8v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Cpath d='M3 11 12 3l9 8v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z'/%3E%3C/svg%3E"); }
.ic[data-i=mods]{ -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Cpath d='M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Cpath d='M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z'/%3E%3C/svg%3E"); }
.ic[data-i=boutique]{ -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Cpath d='M6 6h15l-2 9H8zM6 6 5 3H2'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Cpath d='M6 6h15l-2 9H8zM6 6 5 3H2'/%3E%3C/svg%3E"); }
.ic[data-i=amis]{ -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Ccircle cx='9' cy='8' r='4'/%3E%3Cpath d='M2 21a7 7 0 0 1 14 0'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Ccircle cx='9' cy='8' r='4'/%3E%3Cpath d='M2 21a7 7 0 0 1 14 0'/%3E%3C/svg%3E"); }
.ic[data-i=settings]{ -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Ccircle cx='12' cy='12' r='3.2'/%3E%3Cpath d='M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Ccircle cx='12' cy='12' r='3.2'/%3E%3Cpath d='M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2'/%3E%3C/svg%3E"); }
.ic[data-i=aide]{ -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Ccircle cx='12' cy='12' r='9'/%3E%3Cpath d='M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.7.4-1 .7-1 1.7M12 17h.01'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23fff'%3E%3Ccircle cx='12' cy='12' r='9'/%3E%3Cpath d='M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.7.4-1 .7-1 1.7M12 17h.01'/%3E%3C/svg%3E"); }

.upd { position: fixed; top: 44px; left: 50%; transform: translateX(-50%); z-index: 250; display: flex; align-items: center; gap: 12px; background: rgba(9,10,16,.92); border: 1px solid rgba(212,175,55,.5); border-radius: 12px; padding: 9px 16px; font-size: 13px; color: #EDE8DA; box-shadow: 0 10px 30px rgba(0,0,0,.5); }
.upd.ready { border-color: #7CCB6E; }
.upd-ic { color: var(--gold); }
.upd button { background: linear-gradient(180deg,var(--gold),#D4AF37); color: #1c1607; border: none; border-radius: 8px; padding: 6px 14px; font-weight: 800; cursor: pointer; }
.toast { position: fixed; bottom: 78px; left: 50%; transform: translate(-50%,12px); z-index: 200; opacity: 0; pointer-events: none; transition: .2s; background: #0A0B11; color: #EDE8DA; border: 1px solid var(--gold); border-radius: 10px; padding: 11px 20px; font-size: 13px; } .toast.show { opacity: 1; transform: translate(-50%,0); }
</style>

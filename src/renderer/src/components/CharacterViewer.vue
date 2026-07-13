<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { SkinViewer, IdleAnimation } from 'skinview3d'

/* ===================== Props / events ===================== */
const props = defineProps({
  name: { type: String, default: '' }, // uuid or pseudo
  size: { type: Number, default: 140 } // fallback box size (px) if the parent forgets to size the wrapper
})
const emit = defineEmits(['click'])

/* ===================== Identity / textures ===================== */
const FALLBACK_NAME = 'MHF_Steve'
const skinUrl = (id) => `https://mc-heads.net/skin/${encodeURIComponent(id)}`
const capeUrl = (uuid) => `https://crafatar.com/capes/${encodeURIComponent(uuid)}`
const isLikelyUuid = (s) => typeof s === 'string' && /^[0-9a-f]{32}$/i.test(String(s).replace(/-/g, ''))

/* ===================== Cursor-follow tuning ===================== */
// Amplitude limits (radians). Head has the largest amplitude, body follows
// less, legs barely move at all.
const HEAD_YAW_MAX = 0.7
const HEAD_PITCH_MAX = 0.4
const BODY_YAW_RATIO = 0.4
const BODY_PITCH_RATIO = 0.15
const LEG_YAW_RATIO = 0.06
// Exponential smoothing factor used every frame: cur += (target - cur) * min(1, dt * LERP_SPEED)
const LERP_SPEED = 8

/* ===================== DOM refs ===================== */
const root = ref(null)
const canvasEl = ref(null)

/* ===================== skinview3d instances (kept out of Vue's reactivity: perf + they hold Three.js/WebGL objects) ===================== */
let viewer = null
let cursorAnim = null
let resizeObserver = null
let loadToken = 0 // guards against out-of-order async skin/cape loads (e.g. rapid account switching)

/**
 * Custom idle animation: plays the stock skinview3d breathing / arm-swing /
 * cape-sway animation untouched, then layers a smoothed "look at the cursor"
 * pose on top of the head, torso and legs every frame. Because IdleAnimation
 * never touches head/body/leg rotations, this never fights the base pose.
 */
class CursorIdleAnimation extends IdleAnimation {
  constructor () {
    super()
    this.targetNX = 0 // normalized cursor X, roughly [-1, 1]
    this.targetNY = 0 // normalized cursor Y, roughly [-1, 1]
    this.curHeadY = 0
    this.curHeadX = 0
    this.curBodyY = 0
    this.curBodyX = 0
    this.curLegY = 0
  }

  setCursor (nx, ny) {
    this.targetNX = Math.max(-1, Math.min(1, nx))
    this.targetNY = Math.max(-1, Math.min(1, ny))
  }

  /** Cursor left the window: ease back to neutral instead of snapping. */
  resetCursor () {
    this.targetNX = 0
    this.targetNY = 0
  }

  // `delta` is the real elapsed time (seconds) since the previous frame,
  // provided by skinview3d's own render loop (three.js Clock) - so the
  // follow motion is time-based, not frame-count-based, and stays smooth
  // regardless of the actual FPS.
  animate (player, delta) {
    super.animate(player)

    const k = Math.min(1, Math.max(0, delta) * LERP_SPEED)

    const headYawTarget = this.targetNX * HEAD_YAW_MAX
    const headPitchTarget = this.targetNY * HEAD_PITCH_MAX
    const bodyYawTarget = headYawTarget * BODY_YAW_RATIO
    const bodyPitchTarget = headPitchTarget * BODY_PITCH_RATIO
    const legYawTarget = headYawTarget * LEG_YAW_RATIO

    this.curHeadY += (headYawTarget - this.curHeadY) * k
    this.curHeadX += (headPitchTarget - this.curHeadX) * k
    this.curBodyY += (bodyYawTarget - this.curBodyY) * k
    this.curBodyX += (bodyPitchTarget - this.curBodyX) * k
    this.curLegY += (legYawTarget - this.curLegY) * k

    player.skin.head.rotation.y = this.curHeadY
    player.skin.head.rotation.x = this.curHeadX
    player.skin.body.rotation.y = this.curBodyY
    player.skin.body.rotation.x = this.curBodyX
    player.skin.rightLeg.rotation.y = this.curLegY
    player.skin.leftLeg.rotation.y = this.curLegY
  }
}

/* ===================== Cursor tracking ===================== */
function onPointerMove (e) {
  if (!root.value || !cursorAnim) return
  const rect = root.value.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  // Normalize against a generous radius (not just the viewport's own box) so
  // the head can reach its full range without requiring the cursor to hover
  // exactly over the (small) character preview.
  const radius = Math.max(240, Math.min(window.innerWidth, window.innerHeight) * 0.5)
  const nx = (e.clientX - cx) / radius
  const ny = (e.clientY - cy) / radius
  cursorAnim.setCursor(nx, ny)
}
function onPointerLeaveWindow () {
  if (cursorAnim) cursorAnim.resetCursor()
}

/* ===================== Visibility (perf) ===================== */
function onVisibilityChange () {
  if (viewer) viewer.renderPaused = document.hidden
}

/* ===================== Sizing (responsive, no distortion) ===================== */
function applySize () {
  if (!viewer || !root.value) return
  const w = Math.max(1, Math.round(root.value.clientWidth))
  const h = Math.max(1, Math.round(root.value.clientHeight))
  viewer.setSize(w, h)
}

/* ===================== Skin / cape loading ===================== */
async function applyIdentity (id) {
  if (!viewer) return
  const myToken = ++loadToken
  const wanted = (id && String(id).trim()) || FALLBACK_NAME

  try {
    await viewer.loadSkin(skinUrl(wanted))
  } catch (err) {
    if (myToken !== loadToken) return // a newer identity has since started loading
    console.warn('[CharacterViewer] skin load failed for "' + wanted + '", falling back to Steve.', err)
    if (wanted !== FALLBACK_NAME && viewer) {
      try {
        await viewer.loadSkin(skinUrl(FALLBACK_NAME))
      } catch (err2) {
        console.warn('[CharacterViewer] fallback skin failed too.', err2)
      }
    }
  }
  if (myToken !== loadToken || !viewer) return

  // Best-effort cape. mc-heads.net does not expose a reliable cape endpoint,
  // and most players don't have a cape anyway, so this is opportunistic and
  // silently skipped on any failure (no user-facing error).
  try { viewer.loadCape(null) } catch (_) { /* ignore */ }
  if (isLikelyUuid(wanted)) {
    try {
      await viewer.loadCape(capeUrl(wanted))
    } catch (_) {
      /* no cape available: skip gracefully */
    }
  }
}

/* ===================== Build / teardown ===================== */
function buildViewer () {
  if (!canvasEl.value || !root.value || viewer) return
  const w = Math.max(1, root.value.clientWidth || props.size)
  const h = Math.max(1, root.value.clientHeight || props.size)

  viewer = new SkinViewer({
    canvas: canvasEl.value,
    width: w,
    height: h,
    zoom: 0.9,
    fov: 40
  })

  // We drive rotation ourselves via the cursor; disable all mouse controls.
  viewer.controls.enableZoom = false
  viewer.controls.enablePan = false
  viewer.controls.enableRotate = false

  // Transparent background so the model sits directly over the launcher's
  // space/marble backdrop (this is skinview3d's default, set explicitly for clarity).
  viewer.background = null

  cursorAnim = new CursorIdleAnimation()
  viewer.animation = cursorAnim
  viewer.renderPaused = document.hidden

  applyIdentity(props.name)

  resizeObserver = new ResizeObserver(() => applySize())
  resizeObserver.observe(root.value)
}

function disposeViewer () {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (viewer) {
    try { viewer.dispose() } catch (_) { /* ignore */ }
    viewer = null
  }
  cursorAnim = null
}

function onCanvasClick (e) {
  emit('click', e)
}

/* ===================== Lifecycle ===================== */
onMounted(async () => {
  await nextTick()
  buildViewer()
  window.addEventListener('mousemove', onPointerMove, { passive: true })
  window.addEventListener('blur', onPointerLeaveWindow)
  document.addEventListener('mouseleave', onPointerLeaveWindow)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onPointerMove)
  window.removeEventListener('blur', onPointerLeaveWindow)
  document.removeEventListener('mouseleave', onPointerLeaveWindow)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  disposeViewer()
})

watch(() => props.name, (n) => { applyIdentity(n) })
</script>

<template>
  <div
    ref="root"
    class="cv-root"
    :style="{ minWidth: size + 'px', minHeight: size + 'px' }"
    @click="onCanvasClick"
  >
    <canvas ref="canvasEl" class="cv-canvas"></canvas>
  </div>
</template>

<style scoped>
.cv-root {
  position: relative;
  width: 100%;
  height: 100%;
  cursor: pointer;
  user-select: none;
}
.cv-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
}
</style>

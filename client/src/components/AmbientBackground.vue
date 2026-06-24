<template>
  <div ref="containerRef" class="ambient-background" aria-hidden="true"></div>
</template>

<script setup>
import { Mesh, Program, Renderer, Triangle } from 'ogl'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  timeSpeed: {
    type: Number,
    default: 0.18,
  },
  colorBalance: {
    type: Number,
    default: -0.08,
  },
  warpStrength: {
    type: Number,
    default: 0.78,
  },
  warpFrequency: {
    type: Number,
    default: 4.1,
  },
  warpSpeed: {
    type: Number,
    default: 1.3,
  },
  warpAmplitude: {
    type: Number,
    default: 58,
  },
  blendAngle: {
    type: Number,
    default: -18,
  },
  blendSoftness: {
    type: Number,
    default: 0.16,
  },
  rotationAmount: {
    type: Number,
    default: 260,
  },
  noiseScale: {
    type: Number,
    default: 1.2,
  },
  grainAmount: {
    type: Number,
    default: 0.06,
  },
  grainScale: {
    type: Number,
    default: 1.75,
  },
  grainAnimated: {
    type: Boolean,
    default: false,
  },
  contrast: {
    type: Number,
    default: 1.16,
  },
  gamma: {
    type: Number,
    default: 1,
  },
  saturation: {
    type: Number,
    default: 0.95,
  },
  centerX: {
    type: Number,
    default: 0.02,
  },
  centerY: {
    type: Number,
    default: -0.08,
  },
  zoom: {
    type: Number,
    default: 0.84,
  },
  color1: {
    type: String,
    default: '#f3edff',
  },
  color2: {
    type: String,
    default: '#b88cff',
  },
  color3: {
    type: String,
    default: '#05070b',
  },
})

const vertex = `#version 300 es
in vec2 position;
void main() {
  gl_Position = vec4(position, 0.0, 1.0);
}
`

const fragment = `#version 300 es
precision highp float;
uniform vec2 iResolution;
uniform float iTime;
uniform float uTimeSpeed;
uniform float uColorBalance;
uniform float uWarpStrength;
uniform float uWarpFrequency;
uniform float uWarpSpeed;
uniform float uWarpAmplitude;
uniform float uBlendAngle;
uniform float uBlendSoftness;
uniform float uRotationAmount;
uniform float uNoiseScale;
uniform float uGrainAmount;
uniform float uGrainScale;
uniform float uGrainAnimated;
uniform float uContrast;
uniform float uGamma;
uniform float uSaturation;
uniform vec2 uCenterOffset;
uniform float uZoom;
uniform vec3 uColor1;
uniform vec3 uColor2;
uniform vec3 uColor3;
out vec4 fragColor;

#define S(a,b,t) smoothstep(a,b,t)

mat2 Rot(float a) {
  float s = sin(a);
  float c = cos(a);
  return mat2(c, -s, s, c);
}

vec2 hash(vec2 p) {
  p = vec2(dot(p, vec2(2127.1, 81.17)), dot(p, vec2(1269.5, 283.37)));
  return fract(sin(p) * 43758.5453);
}

float noise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  vec2 u = f * f * (3.0 - 2.0 * f);
  float n = mix(
    mix(
      dot(-1.0 + 2.0 * hash(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0)),
      dot(-1.0 + 2.0 * hash(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)),
      u.x
    ),
    mix(
      dot(-1.0 + 2.0 * hash(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)),
      dot(-1.0 + 2.0 * hash(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)),
      u.x
    ),
    u.y
  );
  return 0.5 + 0.5 * n;
}

void mainImage(out vec4 o, vec2 C) {
  float t = iTime * uTimeSpeed;
  vec2 uv = C / iResolution.xy;
  float ratio = iResolution.x / iResolution.y;
  vec2 tuv = uv - 0.5 + uCenterOffset;
  tuv /= max(uZoom, 0.001);

  float degree = noise(vec2(t * 0.1, tuv.x * tuv.y) * uNoiseScale);
  tuv.y *= 1.0 / ratio;
  tuv *= Rot(radians((degree - 0.5) * uRotationAmount + 180.0));
  tuv.y *= ratio;

  float frequency = uWarpFrequency;
  float ws = max(uWarpStrength, 0.001);
  float amplitude = uWarpAmplitude / ws;
  float warpTime = t * uWarpSpeed;
  tuv.x += sin(tuv.y * frequency + warpTime) / amplitude;
  tuv.y += sin(tuv.x * (frequency * 1.5) + warpTime) / (amplitude * 0.5);

  vec3 colLav = uColor1;
  vec3 colOrg = uColor2;
  vec3 colDark = uColor3;
  float b = uColorBalance;
  float s = max(uBlendSoftness, 0.0);
  mat2 blendRot = Rot(radians(uBlendAngle));
  float blendX = (tuv * blendRot).x;
  float edge0 = -0.3 - b - s;
  float edge1 = 0.2 - b + s;
  float v0 = 0.5 - b + s;
  float v1 = -0.3 - b - s;
  vec3 layer1 = mix(colDark, colOrg, S(edge0, edge1, blendX));
  vec3 layer2 = mix(colOrg, colLav, S(edge0, edge1, blendX));
  vec3 col = mix(layer1, layer2, S(v0, v1, tuv.y));

  vec2 grainUv = uv * max(uGrainScale, 0.001);
  if (uGrainAnimated > 0.5) {
    grainUv += vec2(iTime * 0.05);
  }
  float grain = fract(sin(dot(grainUv, vec2(12.9898, 78.233))) * 43758.5453);
  col += (grain - 0.5) * uGrainAmount;

  col = (col - 0.5) * uContrast + 0.5;
  float luma = dot(col, vec3(0.2126, 0.7152, 0.0722));
  col = mix(vec3(luma), col, uSaturation);
  col = pow(max(col, 0.0), vec3(1.0 / max(uGamma, 0.001)));
  col = clamp(col, 0.0, 1.0);

  o = vec4(col, 1.0);
}

void main() {
  vec4 o = vec4(0.0);
  mainImage(o, gl_FragCoord.xy);
  fragColor = o;
}
`

const containerRef = ref(null)

let renderer = null
let program = null
let mesh = null
let resizeObserver = null
let intersectionObserver = null
let animationFrame = 0
let isVisible = true
let isPageVisible = true
let startTime = 0
let canvas = null

function hexToRgb(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (!result) {
    return [1, 1, 1]
  }

  return [
    Number.parseInt(result[1], 16) / 255,
    Number.parseInt(result[2], 16) / 255,
    Number.parseInt(result[3], 16) / 255,
  ]
}

function syncUniforms() {
  if (!program) {
    return
  }

  const uniforms = program.uniforms
  uniforms.uTimeSpeed.value = props.timeSpeed
  uniforms.uColorBalance.value = props.colorBalance
  uniforms.uWarpStrength.value = props.warpStrength
  uniforms.uWarpFrequency.value = props.warpFrequency
  uniforms.uWarpSpeed.value = props.warpSpeed
  uniforms.uWarpAmplitude.value = props.warpAmplitude
  uniforms.uBlendAngle.value = props.blendAngle
  uniforms.uBlendSoftness.value = props.blendSoftness
  uniforms.uRotationAmount.value = props.rotationAmount
  uniforms.uNoiseScale.value = props.noiseScale
  uniforms.uGrainAmount.value = props.grainAmount
  uniforms.uGrainScale.value = props.grainScale
  uniforms.uGrainAnimated.value = props.grainAnimated ? 1 : 0
  uniforms.uContrast.value = props.contrast
  uniforms.uGamma.value = props.gamma
  uniforms.uSaturation.value = props.saturation
  uniforms.uCenterOffset.value[0] = props.centerX
  uniforms.uCenterOffset.value[1] = props.centerY
  uniforms.uZoom.value = props.zoom

  const color1 = hexToRgb(props.color1)
  const color2 = hexToRgb(props.color2)
  const color3 = hexToRgb(props.color3)

  uniforms.uColor1.value[0] = color1[0]
  uniforms.uColor1.value[1] = color1[1]
  uniforms.uColor1.value[2] = color1[2]
  uniforms.uColor2.value[0] = color2[0]
  uniforms.uColor2.value[1] = color2[1]
  uniforms.uColor2.value[2] = color2[2]
  uniforms.uColor3.value[0] = color3[0]
  uniforms.uColor3.value[1] = color3[1]
  uniforms.uColor3.value[2] = color3[2]
}

function renderFrame(time) {
  if (!renderer || !program || !mesh) {
    animationFrame = 0
    return
  }

  program.uniforms.iTime.value = (time - startTime) * 0.001
  renderer.render({ scene: mesh })
  animationFrame = window.requestAnimationFrame(renderFrame)
}

function startLoop() {
  if (!isVisible || !isPageVisible || animationFrame) {
    return
  }
  animationFrame = window.requestAnimationFrame(renderFrame)
}

function stopLoop() {
  if (!animationFrame) {
    return
  }
  window.cancelAnimationFrame(animationFrame)
  animationFrame = 0
}

function setSize() {
  if (!containerRef.value || !renderer || !program || !mesh) {
    return
  }

  const rect = containerRef.value.getBoundingClientRect()
  const width = Math.max(1, Math.floor(rect.width))
  const height = Math.max(1, Math.floor(rect.height))
  renderer.setSize(width, height)
  program.uniforms.iResolution.value[0] = renderer.gl.drawingBufferWidth
  program.uniforms.iResolution.value[1] = renderer.gl.drawingBufferHeight
  renderer.render({ scene: mesh })
}

function handleVisibilityChange() {
  isPageVisible = !document.hidden
  if (isPageVisible) {
    startLoop()
  } else {
    stopLoop()
  }
}

onMounted(() => {
  const container = containerRef.value
  if (!container) {
    return
  }

  renderer = new Renderer({
    webgl: 2,
    alpha: true,
    antialias: false,
    dpr: Math.min(window.devicePixelRatio || 1, 2),
  })

  canvas = renderer.gl.canvas
  canvas.style.width = '100%'
  canvas.style.height = '100%'
  canvas.style.display = 'block'
  container.appendChild(canvas)

  program = new Program(renderer.gl, {
    vertex,
    fragment,
    uniforms: {
      iTime: { value: 0 },
      iResolution: { value: new Float32Array([1, 1]) },
      uTimeSpeed: { value: props.timeSpeed },
      uColorBalance: { value: props.colorBalance },
      uWarpStrength: { value: props.warpStrength },
      uWarpFrequency: { value: props.warpFrequency },
      uWarpSpeed: { value: props.warpSpeed },
      uWarpAmplitude: { value: props.warpAmplitude },
      uBlendAngle: { value: props.blendAngle },
      uBlendSoftness: { value: props.blendSoftness },
      uRotationAmount: { value: props.rotationAmount },
      uNoiseScale: { value: props.noiseScale },
      uGrainAmount: { value: props.grainAmount },
      uGrainScale: { value: props.grainScale },
      uGrainAnimated: { value: props.grainAnimated ? 1 : 0 },
      uContrast: { value: props.contrast },
      uGamma: { value: props.gamma },
      uSaturation: { value: props.saturation },
      uCenterOffset: { value: new Float32Array([props.centerX, props.centerY]) },
      uZoom: { value: props.zoom },
      uColor1: { value: new Float32Array(hexToRgb(props.color1)) },
      uColor2: { value: new Float32Array(hexToRgb(props.color2)) },
      uColor3: { value: new Float32Array(hexToRgb(props.color3)) },
    },
  })

  mesh = new Mesh(renderer.gl, {
    geometry: new Triangle(renderer.gl),
    program,
  })

  resizeObserver = new ResizeObserver(() => {
    setSize()
  })
  resizeObserver.observe(container)
  setSize()
  syncUniforms()

  intersectionObserver = new IntersectionObserver(
    ([entry]) => {
      isVisible = entry?.isIntersecting ?? true
      if (isVisible) {
        startLoop()
      } else {
        stopLoop()
      }
    },
    { threshold: 0 }
  )
  intersectionObserver.observe(container)

  isPageVisible = !document.hidden
  document.addEventListener('visibilitychange', handleVisibilityChange)

  startTime = performance.now()
  startLoop()
})

watch(
  () => [
    props.timeSpeed,
    props.colorBalance,
    props.warpStrength,
    props.warpFrequency,
    props.warpSpeed,
    props.warpAmplitude,
    props.blendAngle,
    props.blendSoftness,
    props.rotationAmount,
    props.noiseScale,
    props.grainAmount,
    props.grainScale,
    props.grainAnimated,
    props.contrast,
    props.gamma,
    props.saturation,
    props.centerX,
    props.centerY,
    props.zoom,
    props.color1,
    props.color2,
    props.color3,
  ],
  () => {
    syncUniforms()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  stopLoop()
  resizeObserver?.disconnect()
  intersectionObserver?.disconnect()
  document.removeEventListener('visibilitychange', handleVisibilityChange)

  if (containerRef.value && canvas?.parentNode === containerRef.value) {
    containerRef.value.removeChild(canvas)
  }

  const loseContext = renderer?.gl?.getExtension('WEBGL_lose_context')
  loseContext?.loseContext?.()

  mesh = null
  program = null
  renderer = null
  resizeObserver = null
  intersectionObserver = null
  canvas = null
})
</script>

<style scoped>
.ambient-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
</style>

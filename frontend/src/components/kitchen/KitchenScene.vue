<template>
  <div ref="sceneHost" class="kitchen-scene" aria-label="AI 智能厨房场景">
    <div class="scene-loading" v-if="loading">正在准备厨房……</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { AnimatedSprite, Application, Assets, Container, Graphics, Sprite, Text, TextStyle } from 'pixi.js'

const emit = defineEmits(['select-station'])

const sceneHost = ref(null)
const loading = ref(true)

const SCENE_HEIGHT = 760
const MIN_SCENE_WIDTH = 1080
const SPRITE_FRAMES = [1, 2, 3]
const SPRITE_NUMS = [1, 2, 3, 4, 5, 6, 7, 8, 9]

const stations = [
  {
    id: 'chef',
    title: '主厨料理大厅',
    name: '阿灶',
    role: 'AI 主厨',
    icon: '✦',
    accent: 0xd6a43b,
    floor: 0x2a211e,
    floorAlt: 0x342923,
    description: '输入现有食材，生成并保存专属菜谱。',
    room: 'hero',
    spriteNum: 1,
    characters: [
      { id: 'chef', name: '阿灶', role: 'AI 主厨', spriteNum: 1, action: 'cook' },
      { id: 'chef-helper', name: '小灶', role: '备餐助手', spriteNum: 2, stationId: 'pantry', action: 'prep' },
      { id: 'chef-recipes', name: '小谱', role: '菜谱策划', spriteNum: 4, stationId: 'recipes', action: 'prep' },
      { id: 'chef-nutrition', name: '小衡', role: '营养助手', spriteNum: 5, stationId: 'nutrition', action: 'prep' }
    ]
  },
  {
    id: 'pantry',
    title: '食材储藏室',
    name: '小仓',
    role: '食材管家',
    icon: '▣',
    accent: 0x68a873,
    floor: 0x283d37,
    floorAlt: 0x304b42,
    description: '查看库存、临期食材和入库记录。',
    room: 'standard',
    spriteNum: 3,
    characters: [{ id: 'pantry', name: '小仓', role: '食材管家', spriteNum: 3, action: 'work' }]
  },
  {
    id: 'recipes',
    title: '菜谱书房',
    name: '阿笺',
    role: '菜谱管理员',
    icon: '▤',
    accent: 0xb083c7,
    floor: 0x33283c,
    floorAlt: 0x42314c,
    description: '整理已保存菜谱、收藏夹和分享记录。',
    room: 'standard',
    spriteNum: 4,
    characters: [{ id: 'recipes', name: '阿笺', role: '菜谱管理员', spriteNum: 4, action: 'work' }]
  },
  {
    id: 'nutrition',
    title: '营养咨询室',
    name: '衡衡',
    role: '营养师',
    icon: '♥',
    accent: 0xe2816c,
    floor: 0x3e2c31,
    floorAlt: 0x51363a,
    description: '维护健康档案和每日营养目标。',
    room: 'standard',
    spriteNum: 5,
    characters: [{ id: 'nutrition', name: '衡衡', role: '营养师', spriteNum: 5, action: 'work' }]
  },
  {
    id: 'weekly',
    title: '菜单计划室',
    name: '周周',
    role: '菜单规划师',
    icon: '▦',
    accent: 0x6d9cc3,
    floor: 0x293746,
    floorAlt: 0x314559,
    description: '安排一周餐次，并汇总采购清单。',
    room: 'wide',
    spriteNum: 6,
    characters: [
      { id: 'weekly', name: '周周', role: '菜单规划师', spriteNum: 6, action: 'work' },
      { id: 'weekly-nutrition', name: '小衡', role: '营养助手', spriteNum: 7, stationId: 'nutrition', action: 'work' },
      { id: 'weekly-recipes', name: '阿谱', role: '菜谱策划', spriteNum: 8, stationId: 'recipes', action: 'work' }
    ]
  },
  {
    id: 'hot',
    title: '美食情报站',
    name: '椒椒',
    role: '市场观察员',
    icon: '♨',
    accent: 0xd48c52,
    floor: 0x3b3026,
    floorAlt: 0x4b392b,
    description: '查看全站热门食材和搜索趋势。',
    room: 'standard',
    spriteNum: 9,
    characters: [{ id: 'hot', name: '椒椒', role: '市场观察员', spriteNum: 9, action: 'work' }]
  }
]

let app
let resizeObserver
let animationTick
let characterVisuals = []
let workstationVisuals = []
let cookingVisuals = []
const spriteTextures = new Map()

onMounted(async () => {
  if (!sceneHost.value) return

  app = new Application()
  await app.init({
    width: Math.max(sceneHost.value.clientWidth, MIN_SCENE_WIDTH),
    height: SCENE_HEIGHT,
    backgroundAlpha: 0,
    antialias: false,
    resolution: Math.min(window.devicePixelRatio || 1, 2),
    autoDensity: true
  })

  await loadSpriteTextures()

  const canvas = app.canvas
  canvas.className = 'kitchen-scene-canvas'
  canvas.setAttribute('role', 'img')
  canvas.setAttribute('aria-label', '点击厨房中的人物打开对应功能')
  sceneHost.value.appendChild(canvas)
  loading.value = false

  const draw = () => {
    if (!app || app.destroyed) return
    const width = Math.max(sceneHost.value?.clientWidth || MIN_SCENE_WIDTH, MIN_SCENE_WIDTH)
    app.renderer.resize(width, SCENE_HEIGHT)
    app.stage.removeChildren().forEach((child) => child.destroy({ children: true }))
    characterVisuals = []
    workstationVisuals = []
    cookingVisuals = []
    drawKitchen(app.stage, width)
  }

  draw()
  resizeObserver = new ResizeObserver(draw)
  resizeObserver.observe(sceneHost.value)

  animationTick = 0
  app.ticker.add(() => {
    animationTick += app.ticker.deltaTime
    characterVisuals.forEach((item, index) => {
      const idle = Math.sin(animationTick * 0.08 + index * 0.9) * (item.action === 'cook' ? 1.8 : 1.35)
      item.container.position.y = item.baseY + idle
      item.container.position.x = item.baseX + (item.action === 'cook' ? Math.sin(animationTick * 0.12 + index) * 1.4 : 0)
      item.container.rotation = item.action === 'cook' ? Math.sin(animationTick * 0.12 + index) * 0.025 : 0
      item.glow.alpha = item.hovered ? 0.94 : 0.2 + (Math.sin(animationTick * 0.06 + index) + 1) * 0.06
      item.container.scale.set(item.pressed ? 0.98 : item.hovered ? 1.045 : 1)
    })
    workstationVisuals.forEach((item, index) => {
      item.screen.alpha = 0.65 + (Math.sin(animationTick * 0.08 + index * 0.8) + 1) * 0.12
      item.light.alpha = 0.35 + (Math.sin(animationTick * 0.1 + index) + 1) * 0.2
    })
    cookingVisuals.forEach((item) => {
      const motion = Math.sin(animationTick * 0.16) * 1.6
      item.pan.position.x = item.baseX + motion
      item.pan.rotation = Math.sin(animationTick * 0.12) * 0.08
      item.steam.alpha = 0.35 + (Math.sin(animationTick * 0.14) + 1) * 0.16
    })
  })
})

async function loadSpriteTextures() {
  const requests = []
  SPRITE_NUMS.forEach((spriteNum) => {
    SPRITE_FRAMES.forEach((frame) => {
      const fileName = `${spriteNum}-D-${frame}.png`
      requests.push(
        Assets.load(`/sprites/${fileName}`)
          .then((texture) => spriteTextures.set(`${spriteNum}-D-${frame}`, texture))
          .catch(() => null)
      )
    })
  })
  await Promise.all(requests)
}

function getSpriteFrames(spriteNum) {
  return SPRITE_FRAMES.map((frame) => spriteTextures.get(`${spriteNum}-D-${frame}`)).filter(Boolean)
}

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  if (app) {
    app.destroy(true)
    app = null
  }
})

function drawKitchen(stage, width) {
  const outer = new Graphics()
  outer.rect(0, 0, width, SCENE_HEIGHT).fill(0xe6d2a8)
  stage.addChild(outer)

  drawPixelGrid(stage, 0, 0, width, SCENE_HEIGHT, 24, 0xe6d2a8, 0xd8c196)
  drawFrame(stage, 12, 12, width - 24, SCENE_HEIGHT - 24)

  const innerX = 22
  const innerW = width - 44
  drawHeroRoom(stage, innerX, 24, innerW, 136)

  const gap = 12
  const columns = 3
  const colW = (innerW - gap * (columns - 1)) / columns
  const rowY = 176
  const rowH = 208
  drawRoom(stage, stations[1], innerX, rowY, colW, rowH)
  drawRoom(stage, stations[2], innerX + colW + gap, rowY, colW, rowH)
  drawRoom(stage, stations[3], innerX + (colW + gap) * 2, rowY, colW, rowH)

  const lowerY = 396
  const wideW = colW * 2 + gap
  drawRoom(stage, stations[4], innerX, lowerY, wideW, 204)
  drawRoom(stage, stations[5], innerX + wideW + gap, lowerY, colW, 204)

  drawBreakRoom(stage, innerX, 614, innerW, 124)
  drawSceneStats(stage, width)
  drawOfficeHeader(stage, width)
}

function drawFrame(parent, x, y, w, h) {
  const frame = new Graphics()
  frame.rect(x, y, w, h).fill({ color: 0x221d1a, alpha: 0.92 })
  frame.rect(x, y, w, h).stroke({ width: 4, color: 0x70533d, alpha: 0.95 })
  frame.rect(x + 5, y + 5, w - 10, h - 10).stroke({ width: 1, color: 0xb68c55, alpha: 0.6 })
  parent.addChild(frame)

  const topBand = new Graphics()
  topBand.rect(x + 7, y + 7, w - 14, 12).fill(0x43362c)
  for (let i = 0; i < Math.floor((w - 30) / 26); i += 1) {
    topBand.moveTo(x + 18 + i * 26, y + 20)
      .lineTo(x + 24 + i * 26, y + 20)
      .lineTo(x + 21 + i * 26, y + 25)
      .fill(i % 2 ? 0x936d42 : 0xc5964e)
  }
  parent.addChild(topBand)
}

function drawOfficeHeader(parent, width) {
  const header = new Graphics()
  header.roundRect(20, 14, width - 40, 14, 4).fill({ color: 0x302621, alpha: 0.96 })
  header.roundRect(20, 14, width - 40, 14, 4).stroke({ width: 1, color: 0xb68c55, alpha: 0.9 })
  header.rect(28, 18, 5, 5).fill(0xd6a43b)
  header.rect(width - 44, 18, 5, 5).fill(0x68a873)
  parent.addChild(header)
  addText(parent, '像素厨房 · 办公区', 40, 17, 7, 0xf4dfb3, true)
  addText(parent, '在线 · 点击人物开始工作', width - 210, 17, 7, 0xc6e0c7, true)
}

function drawHeroRoom(parent, x, y, w, h) {
  const room = new Graphics()
  room.rect(x, y, w, h).fill(0x191719)
  drawPixelGrid(room, x, y, w, h, 24, 0x191719, 0x242126)
  room.rect(x, y, w, 26).fill(0x3e302a)
  room.rect(x, y + 25, w, 2).fill(0xb78a4d)
  room.rect(x, y, w, h).stroke({ width: 2, color: 0x865c3b })
  parent.addChild(room)

  addRoomLabel(parent, x + 10, y + 7, '主厨料理大厅', '今日 AI 菜谱工作台', 0xd6a43b)
  addText(parent, '准备好食材了吗？', x + 38, y + 48, 13, 0xf3dfb9, true)
  addText(parent, '点击主厨，输入食材并生成一份只属于你的菜谱', x + 38, y + 70, 9, 0xc8b496, false)

  const island = new Graphics()
  island.roundRect(x + w * 0.56, y + 62, w * 0.25, 39, 10).fill(0xa8753c)
  island.roundRect(x + w * 0.56 + 4, y + 66, w * 0.25 - 8, 28, 7).fill(0xd2a25d)
  island.roundRect(x + w * 0.56 + 14, y + 70, w * 0.25 - 28, 13, 4).fill(0x6d4a2e)
  island.roundRect(x + w * 0.56 + 21, y + 72, 26, 8, 3).fill(0x222225)
  island.roundRect(x + w * 0.56 + 58, y + 72, 26, 8, 3).fill(0x222225)
  parent.addChild(island)

  const chefPositions = [0.34, 0.445, 0.555, 0.66].map((ratio) => x + w * ratio)
  stations[0].characters.forEach((character, index) => {
    const visual = drawCharacter(parent, stations[0], chefPositions[index], y + 114, 0.94, character)
    characterVisuals.push(visual)
  })
  addText(parent, '点击人物开始料理', x + w * 0.5, y + 32, 8, 0xf1c36a, true, true)
  addSpeechBubble(parent, x + w * 0.69, y + 44, '今天想吃什么？', 104)

  drawPantryShelf(parent, x + 24, y + 95, 84, 22)
  const cooking = drawCookingPan(parent, x + w * 0.76, y + 76)
  cookingVisuals.push(cooking)
}

function drawRoom(parent, station, x, y, w, h) {
  const room = new Graphics()
  room.rect(x, y, w, h).fill(station.floor)
  drawPixelGrid(room, x, y, w, h, 20, station.floor, station.floorAlt)
  room.rect(x, y, w, 30).fill({ color: station.accent, alpha: 0.28 })
  room.rect(x, y + 28, w, 2).fill(station.accent)
  room.rect(x, y, w, h).stroke({ width: 2, color: station.accent, alpha: 0.78 })
  parent.addChild(room)

  addRoomLabel(parent, x + w / 2, y + 8, station.title, station.role, station.accent, true)
  drawWallDecor(parent, station.id, x, y, w)

  const characters = station.characters || [{ id: station.id, name: station.name, role: station.role, spriteNum: station.spriteNum }]
  const positions = characters.length === 3 ? [0.32, 0.5, 0.68] : characters.length === 2 ? [0.4, 0.6] : [0.5]
  characters.forEach((character, index) => {
    const char = drawCharacter(parent, station, x + w * positions[index], y + h - 52, 0.94, character)
    characterVisuals.push(char)
    // Keep the workbench below the character's feet so it never overlaps the face
    // when the scene is scaled down in a smaller desktop viewport.
    const desk = drawWorkstation(parent, x + w * positions[index], y + h - 46, station.accent)
    workstationVisuals.push(desk)
  })

  if (station.id === 'pantry') {
    drawFridge(parent, x + 18, y + h - 86)
    drawCrate(parent, x + w - 72, y + h - 72, station.accent)
  } else if (station.id === 'recipes') {
    drawBookcase(parent, x + 20, y + h - 87, station.accent)
    drawBookcase(parent, x + w - 66, y + h - 87, station.accent)
  } else if (station.id === 'nutrition') {
    drawScale(parent, x + 20, y + h - 63, station.accent)
    drawPlant(parent, x + w - 44, y + h - 74)
  } else if (station.id === 'weekly') {
    drawCalendarBoard(parent, x + 24, y + 60, station.accent)
    drawTable(parent, x + w - 142, y + h - 75, 116, station.accent)
  } else if (station.id === 'hot') {
    drawBulletin(parent, x + 18, y + 58, station.accent)
    drawChiliCrate(parent, x + w - 75, y + h - 70, station.accent)
  }
}

function drawBreakRoom(parent, x, y, w, h) {
  const room = new Graphics()
  room.rect(x, y, w, h).fill(0x211d1a)
  drawPixelGrid(room, x, y, w, h, 24, 0x211d1a, 0x2a2420)
  room.rect(x, y, w, 24).fill(0x514135)
  room.rect(x, y + 23, w, 2).fill(0xb98a4c)
  room.rect(x, y, w, h).stroke({ width: 2, color: 0x816044 })
  parent.addChild(room)
  addRoomLabel(parent, x + 12, y + 6, '厨房休息区', '让推荐灵感稍微喘口气', 0x9bb58a)
  drawTable(parent, x + w * 0.42, y + 51, Math.min(260, w * 0.24), 0xb98a4c)
  drawPlant(parent, x + 34, y + 64)
  drawPlant(parent, x + w - 42, y + 64)
  addText(parent, '今天也要好好吃饭。', x + w * 0.09, y + 74, 11, 0xe8d7b7, true)
}

function drawSceneStats(parent, width) {
  const stats = [
    ['菜谱', '等待你的第一道'],
    ['库存', '登录后自动匹配'],
    ['本周', '开始安排菜单']
  ]
  const boxW = 130
  stats.forEach((stat, index) => {
    const x = width - 32 - (stats.length - index) * (boxW + 8)
    const box = new Graphics()
    box.roundRect(x, 38, boxW, 38, 5).fill({ color: 0xefe0bb, alpha: 0.96 })
    box.roundRect(x, 38, boxW, 38, 5).stroke({ width: 1, color: 0x8b6e4e })
    parent.addChild(box)
    addText(parent, stat[0], x + 10, 47, 8, 0x775b3d, true)
    addText(parent, stat[1], x + 10, 60, 8, 0x4a392c, false)
  })
}

function drawCharacter(parent, station, x, feetY, scale, character = null) {
  const actor = character || station
  const targetHeight = 58 * scale
  const spriteNum = actor.spriteNum || station.spriteNum
  const spriteFrames = getSpriteFrames(spriteNum)
  const container = new Container()
  container.position.set(x, feetY)
  container.eventMode = 'static'
  container.cursor = 'pointer'

  const glow = new Graphics()
  glow.ellipse(0, 4, 30 * scale, 7 * scale).fill({ color: station.accent, alpha: 0.2 })
  glow.ellipse(0, 4, 24 * scale, 4 * scale).fill({ color: 0x090807, alpha: 0.5 })
  container.addChild(glow)

  let animatedSprite = null
  if (spriteFrames.length) {
    const sprite = spriteFrames.length > 1 ? new AnimatedSprite(spriteFrames) : new Sprite(spriteFrames[0])
    sprite.anchor.set(0.5, 1)
    const fitSpriteToHeight = () => {
      if (sprite.texture?.height) {
        sprite.scale.set(targetHeight / sprite.texture.height)
      }
    }
    // The source frames use different transparent canvas sizes. Refit each
    // frame to the same height so the character does not pulse while animating.
    fitSpriteToHeight()
    if (sprite instanceof AnimatedSprite) {
      sprite.onFrameChange = fitSpriteToHeight
      sprite.animationSpeed = 0.08
      sprite.play()
      animatedSprite = sprite
    }
    container.addChild(sprite)
  }

  if (!spriteFrames.length) {
    const body = new Graphics()
    body.rect(-13 * scale, -42 * scale, 26 * scale, 27 * scale).fill(station.id === 'chef' ? 0xf0e2bf : 0xb7d0c3)
    body.rect(-13 * scale, -42 * scale, 26 * scale, 27 * scale).stroke({ width: 1.4 * scale, color: 0x2d201a })
    body.rect(-8 * scale, -16 * scale, 6 * scale, 16 * scale).fill(0x5e493b)
    body.rect(2 * scale, -16 * scale, 6 * scale, 16 * scale).fill(0x5e493b)
    body.rect(-18 * scale, -37 * scale, 5 * scale, 15 * scale).fill(0xc68d68)
    body.rect(13 * scale, -37 * scale, 5 * scale, 15 * scale).fill(0xc68d68)
    container.addChild(body)

    const head = new Graphics()
    head.rect(-12 * scale, -67 * scale, 24 * scale, 24 * scale).fill(0xd99a78)
    head.rect(-12 * scale, -67 * scale, 24 * scale, 24 * scale).stroke({ width: 1.4 * scale, color: 0x2d201a })
    head.rect(-8 * scale, -57 * scale, 3 * scale, 3 * scale).fill(0x2b211e)
    head.rect(5 * scale, -57 * scale, 3 * scale, 3 * scale).fill(0x2b211e)
    head.rect(-4 * scale, -48 * scale, 8 * scale, 2 * scale).fill(0x7d4539)
    head.rect(-14 * scale, -71 * scale, 28 * scale, 8 * scale).fill(station.id === 'chef' ? 0xd6a43b : station.accent)
    head.rect(-10 * scale, -74 * scale, 20 * scale, 4 * scale).fill(station.id === 'chef' ? 0xf2c966 : station.accent)

    if (station.id === 'chef') {
      head.rect(-3 * scale, -78 * scale, 6 * scale, 5 * scale).fill(0xffe7a3)
      head.rect(-8 * scale, -82 * scale, 16 * scale, 4 * scale).fill(0xf0c362)
    } else if (station.id === 'nutrition') {
      head.rect(-14 * scale, -64 * scale, 4 * scale, 15 * scale).fill(0x2a2524)
      head.rect(10 * scale, -64 * scale, 4 * scale, 15 * scale).fill(0x2a2524)
    } else if (station.id === 'pantry') {
      head.rect(-16 * scale, -72 * scale, 32 * scale, 5 * scale).fill(0x6d9b72)
    }
    container.addChild(head)
  }

  const labelY = -targetHeight - 15
  const nameBg = new Graphics()
  nameBg.roundRect(-31 * scale, labelY, 62 * scale, 15 * scale, 3).fill(0xf3ead4)
  nameBg.roundRect(-31 * scale, labelY, 62 * scale, 15 * scale, 3).stroke({ width: 1, color: station.accent })
  container.addChild(nameBg)
  const name = new Text({
    text: actor.name || station.name,
    style: new TextStyle({ fontFamily: 'Microsoft YaHei, sans-serif', fontSize: 9 * scale, fontWeight: '900', fill: 0x392b24 })
  })
  name.anchor.set(0.5, 0.5)
  name.position.set(0, labelY + 7.5 * scale)
  container.addChild(name)

  const glowFrame = new Graphics()
  glowFrame.roundRect(-36 * scale, labelY - 5 * scale, 72 * scale, targetHeight + 25 * scale, 5).stroke({ width: 2, color: station.accent, alpha: 0.2 })
  glowFrame.visible = false
  container.addChild(glowFrame)

  const visual = {
    container,
    baseX: x,
    baseY: feetY,
    glow: glowFrame,
    hovered: false,
    pressed: false,
    action: actor.action || 'work',
    sprite: animatedSprite
  }
  container.on('pointerover', () => {
    visual.hovered = true
    glowFrame.visible = true
  })
  container.on('pointerout', () => {
    visual.hovered = false
    glowFrame.visible = false
  })
  container.on('pointerdown', () => {
    visual.pressed = true
    glowFrame.visible = true
    emit('select-station', actor.stationId || station.id)
  })
  container.on('pointerup', () => {
    visual.pressed = false
  })
  parent.addChild(container)
  return visual
}

function addRoomLabel(parent, x, y, title, subtitle, accent, centered = false) {
  const label = new Graphics()
  const labelW = Math.max(136, title.length * 12 + 28)
  const labelX = centered ? x - labelW / 2 : x
  label.roundRect(labelX, y, labelW, 27, 5).fill(accent)
  label.roundRect(labelX + 2, y + 2, labelW - 4, 23, 4).fill({ color: 0x281f1b, alpha: 0.3 })
  parent.addChild(label)
  addText(parent, title, centered ? x : x + 10, y + 4, 10, 0xfff8e7, true, centered)
  addText(parent, subtitle, centered ? x : x + 10, y + 16, 7, 0xf1dfbd, false, centered)
}

function addSpeechBubble(parent, x, y, text, width) {
  const bubble = new Graphics()
  bubble.roundRect(x, y, width, 22, 5).fill(0xf8f3e7)
  bubble.roundRect(x, y, width, 22, 5).stroke({ width: 1.2, color: 0xb08350 })
  bubble.moveTo(x + 8, y + 22).lineTo(x + 14, y + 28).lineTo(x + 20, y + 22).fill(0xf8f3e7)
  parent.addChild(bubble)
  addText(parent, text, x + 10, y + 7, 8, 0x4a392c, true)
}

function addText(parent, text, x, y, size, fill, bold = false, centered = false) {
  const node = new Text({
    text,
    style: new TextStyle({
      fontFamily: 'Microsoft YaHei, sans-serif',
      fontSize: size,
      fontWeight: bold ? '900' : '600',
      fill,
      letterSpacing: 0.3
    })
  })
  if (centered) node.anchor.set(0.5, 0)
  node.position.set(x, y)
  parent.addChild(node)
}

function drawPixelGrid(parent, x, y, w, h, tile, colorA, colorB) {
  const target = typeof parent.rect === 'function' ? parent : new Graphics()
  for (let ty = y; ty < y + h; ty += tile) {
    for (let tx = x; tx < x + w; tx += tile) {
      target.rect(tx, ty, tile, tile).fill(((tx - x) / tile + (ty - y) / tile) % 2 === 0 ? colorA : colorB)
      target.rect(tx, ty, tile, 1).fill({ color: 0xffffff, alpha: 0.06 })
      target.rect(tx, ty + tile - 1, tile, 1).fill({ color: 0x000000, alpha: 0.08 })
    }
  }
  if (target !== parent) parent.addChild(target)
}

function drawWallDecor(parent, id, x, y, w) {
  if (id === 'pantry') drawPantryShelf(parent, x + w - 102, y + 53, 76, 28)
  if (id === 'recipes') drawPictureFrame(parent, x + w / 2 - 22, y + 49, 44, 28, 0xb083c7)
  if (id === 'nutrition') drawPictureFrame(parent, x + w / 2 - 22, y + 49, 44, 28, 0xe2816c)
  if (id === 'weekly') drawPictureFrame(parent, x + w - 88, y + 49, 50, 30, 0x6d9cc3)
  if (id === 'hot') drawPictureFrame(parent, x + w / 2 - 24, y + 48, 48, 30, 0xd48c52)
}

function drawPictureFrame(parent, x, y, w, h, accent) {
  const g = new Graphics()
  g.rect(x, y, w, h).fill(0xeee0bb)
  g.rect(x, y, w, h).stroke({ width: 2, color: accent })
  g.rect(x + 5, y + 5, w - 10, h - 10).fill({ color: accent, alpha: 0.36 })
  g.rect(x + 9, y + 10, 7, 7).fill(0xf4d578)
  g.rect(x + 20, y + 8, 12, 11).fill(0x77a978)
  parent.addChild(g)
}

function drawPantryShelf(parent, x, y, w, h) {
  const g = new Graphics()
  g.rect(x, y, w, h).fill(0x9a673d)
  g.rect(x, y + h / 2, w, 2).fill(0x513423)
  ;[0xd3544b, 0x6ea466, 0xd5a33e, 0xb57b9e].forEach((color, index) => {
    g.rect(x + 8 + index * 17, y + 5, 10, 10).fill(color)
  })
  parent.addChild(g)
}

function drawWorkstation(parent, x, y, accent) {
  const desk = new Container()
  desk.position.set(x, y)

  const body = new Graphics()
  body.roundRect(-30, 0, 60, 20, 4).fill(0xb77d45)
  body.roundRect(-27, 3, 54, 13, 3).fill(0xd5a363)
  body.roundRect(-27, 3, 54, 13, 3).stroke({ width: 1, color: accent, alpha: 0.85 })
  body.rect(-22, 18, 5, 9).fill(0x805738)
  body.rect(17, 18, 5, 9).fill(0x805738)
  desk.addChild(body)

  const screen = new Graphics()
  screen.roundRect(-9, -16, 18, 13, 2).fill(0x1e2a2a)
  screen.roundRect(-7, -14, 14, 8, 1).fill({ color: accent, alpha: 0.88 })
  screen.rect(-4, -2, 8, 2).fill(0x77553a)
  desk.addChild(screen)

  const light = new Graphics()
  light.circle(24, 7, 2.5).fill(accent)
  desk.addChild(light)
  parent.addChild(desk)
  return { screen, light }
}

function drawCookingPan(parent, x, y) {
  const pan = new Container()
  pan.position.set(x, y)

  const body = new Graphics()
  body.ellipse(0, 0, 31, 9).fill(0x2a2420)
  body.ellipse(0, -2, 25, 6).fill(0x5e4533)
  body.ellipse(-2, -3, 18, 4).fill(0xd65343)
  body.circle(-8, -3, 3).fill(0xe7b24b)
  body.circle(4, -2, 3).fill(0x78a96f)
  body.rect(26, -3, 22, 5).fill(0x7e5a3d)
  body.rect(43, -4, 12, 7).fill(0x342923)
  pan.addChild(body)
  parent.addChild(pan)

  const steam = new Graphics()
  steam.moveTo(x - 10, y - 10).quadraticCurveTo(x - 15, y - 20, x - 9, y - 28).stroke({ width: 2, color: 0xf0dbb2, alpha: 0.64 })
  steam.moveTo(x + 3, y - 10).quadraticCurveTo(x - 2, y - 19, x + 5, y - 27).stroke({ width: 2, color: 0xf0dbb2, alpha: 0.52 })
  parent.addChild(steam)
  return { pan, steam, baseX: x }
}

function drawFridge(parent, x, y) {
  const g = new Graphics()
  g.roundRect(x, y, 45, 74, 4).fill(0xbcc6c2)
  g.roundRect(x, y, 45, 74, 4).stroke({ width: 2, color: 0x2a302f })
  g.rect(x + 2, y + 34, 41, 2).fill(0x65716f)
  g.rect(x + 35, y + 9, 3, 13).fill(0x384441)
  g.rect(x + 35, y + 43, 3, 13).fill(0x384441)
  parent.addChild(g)
}

function drawCrate(parent, x, y, accent) {
  const g = new Graphics()
  g.rect(x, y, 52, 30).fill(0xa97943)
  g.rect(x, y, 52, 30).stroke({ width: 2, color: accent })
  g.moveTo(x + 8, y + 6).lineTo(x + 44, y + 24).stroke({ width: 1, color: 0x724726 })
  g.moveTo(x + 44, y + 6).lineTo(x + 8, y + 24).stroke({ width: 1, color: 0x724726 })
  parent.addChild(g)
}

function drawBookcase(parent, x, y, accent) {
  const g = new Graphics()
  g.rect(x, y, 44, 75).fill(0x8e5d43)
  g.rect(x, y, 44, 75).stroke({ width: 2, color: accent })
  g.rect(x + 4, y + 20, 36, 3).fill(0xe0b35d)
  g.rect(x + 4, y + 45, 36, 3).fill(0xe0b35d)
  ;[0xe67b63, 0x70a6c6, 0xd4a14a, 0xb180bc, 0x78a878].forEach((color, index) => {
    g.rect(x + 7 + (index % 4) * 8, y + 7 + Math.floor(index / 4) * 25, 6, 14).fill(color)
  })
  parent.addChild(g)
}

function drawScale(parent, x, y, accent) {
  const g = new Graphics()
  g.rect(x, y + 18, 46, 8).fill(0x8f9ea5)
  g.roundRect(x + 10, y, 26, 23, 4).fill(0xe9e2cb)
  g.roundRect(x + 10, y, 26, 23, 4).stroke({ width: 2, color: accent })
  g.circle(x + 23, y + 11, 6).fill(0x80b08d)
  parent.addChild(g)
}

function drawPlant(parent, x, y) {
  const g = new Graphics()
  g.rect(x - 8, y + 20, 16, 11).fill(0xd08058)
  g.rect(x - 6, y + 18, 12, 4).fill(0xe6b16d)
  g.rect(x - 2, y + 4, 4, 17).fill(0x659b68)
  g.circle(x - 7, y + 7, 6).fill(0x79ad73)
  g.circle(x + 6, y + 3, 6).fill(0x5f9563)
  parent.addChild(g)
}

function drawCalendarBoard(parent, x, y, accent) {
  const g = new Graphics()
  g.rect(x, y, 116, 68).fill(0xede2c8)
  g.rect(x, y, 116, 68).stroke({ width: 2, color: accent })
  g.rect(x, y, 116, 14).fill(accent)
  for (let row = 0; row < 3; row += 1) {
    for (let col = 0; col < 5; col += 1) {
      g.rect(x + 10 + col * 20, y + 23 + row * 13, 12, 7).fill((row + col) % 3 === 0 ? 0xe8b844 : 0xa8c3c0)
    }
  }
  parent.addChild(g)
}

function drawTable(parent, x, y, w, accent) {
  const g = new Graphics()
  g.roundRect(x, y, w, 24, 6).fill(0xb77d45)
  g.roundRect(x + 3, y + 3, w - 6, 15, 4).fill(0xd5a363)
  g.rect(x + 14, y + 22, 7, 12).fill(0x805738)
  g.rect(x + w - 21, y + 22, 7, 12).fill(0x805738)
  g.rect(x + w / 2 - 16, y + 8, 32, 3).fill(accent)
  parent.addChild(g)
}

function drawBulletin(parent, x, y, accent) {
  const g = new Graphics()
  g.rect(x, y, 118, 70).fill(0xeadfbd)
  g.rect(x, y, 118, 70).stroke({ width: 2, color: accent })
  g.rect(x + 8, y + 10, 48, 6).fill(accent)
  g.rect(x + 8, y + 24, 94, 4).fill(0x9b8063)
  g.rect(x + 8, y + 36, 75, 4).fill(0x9b8063)
  g.rect(x + 8, y + 48, 88, 4).fill(0x9b8063)
  g.circle(x + 96, y + 18, 7).fill(0xd65343)
  parent.addChild(g)
}

function drawChiliCrate(parent, x, y, accent) {
  const g = new Graphics()
  g.rect(x, y, 54, 30).fill(0x9d673d)
  g.rect(x, y, 54, 30).stroke({ width: 2, color: accent })
  ;[0xd65343, 0xe59e43, 0x76a56d].forEach((color, index) => {
    g.circle(x + 15 + index * 12, y + 15, 6).fill(color)
  })
  parent.addChild(g)
}

function drawSteam(parent, x, y) {
  const g = new Graphics()
  g.moveTo(x, y).quadraticCurveTo(x - 5, y - 8, x, y - 16).stroke({ width: 2, color: 0xf0dbb2, alpha: 0.7 })
  g.moveTo(x + 10, y + 2).quadraticCurveTo(x + 5, y - 6, x + 10, y - 13).stroke({ width: 2, color: 0xf0dbb2, alpha: 0.55 })
  parent.addChild(g)
}
</script>

<style scoped>
.kitchen-scene {
  position: relative;
  min-height: 760px;
  overflow: hidden;
  border: 1px solid #8b6e4e;
  background: #e6d2a8;
  box-shadow: 0 16px 30px rgba(53, 35, 23, 0.2);
}

.kitchen-scene :deep(.kitchen-scene-canvas) {
  display: block;
  width: 100%;
  height: 760px;
  image-rendering: pixelated;
}

.scene-loading {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: grid;
  place-items: center;
  color: #59432f;
  background: #e6d2a8;
  font-weight: 800;
}
</style>

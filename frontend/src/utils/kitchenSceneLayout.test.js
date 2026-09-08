import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const sceneSource = fs.readFileSync(new URL('../components/kitchen/KitchenScene.vue', import.meta.url), 'utf8')
const worldSource = fs.readFileSync(new URL('../views/KitchenWorldView.vue', import.meta.url), 'utf8')
const windowSource = fs.readFileSync(new URL('../components/kitchen/SceneWindow.vue', import.meta.url), 'utf8')

test('厨房场景外部几何常量保持不变', () => {
  assert.match(sceneSource, /const SCENE_HEIGHT = 760/)
  assert.match(sceneSource, /const MIN_SCENE_WIDTH = 1080/)
  assert.match(sceneSource, /height: SCENE_HEIGHT/)
  assert.match(worldSource, /max-width: 1520px/)
  assert.match(windowSource, /height: min\(760px/)
})

test('七个厨房区域使用稳定的站点和人物映射', () => {
  for (const stationId of ['chef', 'pantry', 'recipes', 'nutrition', 'weekly', 'hot']) {
    assert.match(sceneSource, new RegExp(`id: '${stationId}'`))
  }

  for (const characterId of ['chef-helper', 'chef-recipes', 'chef-nutrition', 'pantry', 'recipes', 'weekly', 'review', 'account', 'hot']) {
    assert.match(sceneSource, new RegExp(`id: '${characterId}'`))
  }

  for (const roomTitle of ['主厨接待大厅', '食材储藏室', '菜谱书房', '营养咨询室', '厨房协作区', '美食情报站', '厨房休息区']) {
    assert.match(sceneSource, new RegExp(roomTitle))
  }
})

test('像素布景配置覆盖七个区域的关键家具', () => {
  for (const helper of [
    'drawChalkboard',
    'drawWallClock',
    'drawSpiceJars',
    'drawDoubleFridge',
    'drawRack',
    'drawBookcase',
    'drawOvenStove',
    'drawSinkCabinet',
    'drawDeskComputer',
    'drawSmallDiningSet',
    'drawHangingPots',
    'drawDisplayCabinet',
    'drawPetRestCorner',
    'drawTeaCart'
  ]) {
    assert.match(sceneSource, new RegExp(`function ${helper}`))
  }

  assert.match(sceneSource, /drawHeroRoom\(staticLayer, characterLayer, effectsLayer, innerX, 30, innerW, 166\)/)
  assert.match(sceneSource, /drawBreakRoom\(staticLayer, innerX, 606, innerW, 132\)/)
  assert.match(sceneSource, /drawPixelGrid\(staticLayer, 0, 0, width, SCENE_HEIGHT, 32/)
})

test('六处截图标记按三删三增落实', () => {
  assert.doesNotMatch(sceneSource, /\['菜谱', '等待你的第一道'\]/)
  assert.doesNotMatch(sceneSource, /function drawCalendarBoard/)
  assert.doesNotMatch(sceneSource, /drawWallClock\(parent, x \+ 83, y \+ 61/)

  assert.match(sceneSource, /drawDisplayCabinet\(parent, x \+ Math\.round\(w \* 0\.23\)/)
  assert.match(sceneSource, /drawPetRestCorner\(parent, x \+ Math\.round\(w \* 0\.22\)/)
  assert.match(sceneSource, /drawTeaCart\(parent, x \+ Math\.round\(w \* 0\.68\)/)
})

test('Pixi 场景按静态、特效、人物和界面分层并限制事件命中', () => {
  for (const label of [
    'kitchen-static-layer',
    'kitchen-effects-layer',
    'kitchen-character-layer',
    'kitchen-ui-layer'
  ]) {
    assert.match(sceneSource, new RegExp(label))
  }

  assert.match(sceneSource, /new GraphicsContext\(\)/)
  assert.match(sceneSource, /room\.cacheAsTexture\(\{ resolution: 1, antialias: false \}\)/)
  assert.match(sceneSource, /layer\.eventMode = eventMode/)
  assert.match(sceneSource, /container\.eventMode = 'static'/)
  assert.match(sceneSource, /container\.hitArea = new Rectangle/)
})

test('所有人物固定原地踏步且全身不被工作台遮挡', () => {
  assert.doesNotMatch(sceneSource, /function updateHeroWanderers/)
  assert.doesNotMatch(sceneSource, /\bmotionX\b|\bmotionY\b|\bwander\b|\bwalking\b/)
  assert.match(sceneSource, /item\.container\.position\.set\(item\.baseX, item\.baseY\)/)
  assert.match(sceneSource, /drawCharacter\(characterLayer, station, x \+ w \* positions\[index\], y \+ h - 12/)
  assert.match(sceneSource, /drawWorkstation\(effectsLayer, x \+ w \* positions\[index\], y \+ h - 48/)
  assert.match(sceneSource, /drawPrepTable\(parent, x \+ w \* 0\.5, y \+ h - 57/)
})

test('工作台两侧提供炒锅、砧板和蔬菜且人物中线保持留空', () => {
  assert.match(sceneSource, /function drawWorktopFood/)
  assert.match(sceneSource, /middle of the worktop clear/)
  assert.ok((sceneSource.match(/drawWorktopFood\(/g) || []).length >= 5)
  assert.match(sceneSource, /ingredients\.ellipse\(-20, -2/)
  assert.match(sceneSource, /screen\.roundRect\(8, 2, 18, 13/)
})

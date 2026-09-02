import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const componentSource = await readFile(
  new URL('../components/ShoppingChecklistTable.vue', import.meta.url),
  'utf8'
)

test('采购清单在窄容器下使用分层卡片并保留交互区域', () => {
  assert.match(componentSource, /container-type:\s*inline-size/)
  assert.match(componentSource, /@container shopping-checklist \(max-width: 760px\)/)
  assert.match(componentSource, /class="mobile-card-status-row"/)
  assert.match(componentSource, /class="status-button mobile-status-button"/)
  assert.match(componentSource, /class="purchase-links mobile-purchase-links"/)
  assert.doesNotMatch(componentSource, /fixed="right"/)
})

import assert from 'node:assert/strict'
import test from 'node:test'
import {
  FINISHED_DISH_REVIEW_MAX_FILE_SIZE,
  buildFinishedDishReviewRequest,
  validateFinishedDishReviewImage
} from './finishedDishReview.js'

test('成品图评价请求会保留菜谱上下文并限制传输数据', () => {
  const request = buildFinishedDishReviewRequest({
    title: '  青椒炒蛋  ',
    ingredients: [
      { name: '鸡蛋', amount: '2 个' },
      { name: '青椒', amount: '1 个' },
      { name: '', amount: '' }
    ],
    steps: [
      { title: '处理食材', description: '洗净切好' },
      { title: '下锅翻炒', instruction: '炒至熟透' }
    ]
  }, 8)

  assert.deepEqual(request, {
    recipeId: 8,
    recipeTitle: '青椒炒蛋',
    ingredients: ['鸡蛋 2 个', '青椒 1 个'],
    steps: ['处理食材：洗净切好', '下锅翻炒：炒至熟透']
  })
})

test('成品图评价会拒绝不支持格式、空文件和过大文件', () => {
  assert.equal(validateFinishedDishReviewImage(), '请先选择一张成品图')
  assert.equal(
    validateFinishedDishReviewImage({ type: 'image/gif', size: 100 }),
    '仅支持 JPG、PNG 或 WEBP 格式的成品图'
  )
  assert.equal(
    validateFinishedDishReviewImage({ type: 'image/jpeg', size: 0 }),
    '图片文件无效，请重新选择'
  )
  assert.equal(
    validateFinishedDishReviewImage({ type: 'image/png', size: FINISHED_DISH_REVIEW_MAX_FILE_SIZE + 1 }),
    '成品图不能超过 5MB'
  )
  assert.equal(validateFinishedDishReviewImage({ type: 'image/webp', size: 1024 }), '')
})

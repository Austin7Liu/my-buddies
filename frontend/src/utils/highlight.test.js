import { describe, expect, it } from 'vitest'
import { parseHighlight } from './highlight.js'

describe('parseHighlight', () => {
  it('keeps only em semantics and strips other html tags', () => {
    expect(parseHighlight('一起<em>网球</em><img src=x onerror=alert(1)>')).toEqual([
      { text: '一起', highlight: false },
      { text: '网球', highlight: true },
      { text: '', highlight: false },
    ])
  })
})

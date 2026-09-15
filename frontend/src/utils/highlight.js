export function parseHighlight(text = '') {
  return text.split(/(<em>|<\/em>)/i).reduce((result, part) => {
    if (/^<em>$/i.test(part)) result.highlight = true
    else if (/^<\/em>$/i.test(part)) result.highlight = false
    else if (part) result.items.push({ text: part.replace(/<[^>]*>/g, ''), highlight: result.highlight })
    return result
  }, { highlight: false, items: [] }).items
}

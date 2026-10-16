/**
 * Evaluates gradient boosted trees exported by
 * `Training Data (headstart)/export_ondevice_models.py`.
 *
 * The exporter verifies that this walking strategy reproduces XGBoost's own
 * `predict()` output before writing a model, so the traversal rules here
 * (missing -> `missing` branch, `value < split_condition` -> `yes`) must stay
 * in sync with `walk_tree` in that script.
 */

export const MODEL_FORMAT = 'solene-xgb-json-v1'

function isMissing(value) {
  return value === undefined || value === null || Number.isNaN(value)
}

function compileNode(node, path) {
  if (Object.prototype.hasOwnProperty.call(node, 'leaf')) {
    return { isLeaf: true, value: Number(node.leaf) }
  }

  const childrenById = {}
  const children = node.children || []
  children.forEach((child) => {
    childrenById[child.nodeid] = compileNode(child, `${path}/${child.nodeid}`)
  })

  const yes = childrenById[node.yes]
  const no = childrenById[node.no]
  const missing = childrenById[node.missing]

  if (!yes || !no || !missing) {
    throw new Error(`Malformed booster node at ${path}: unresolved child id`)
  }

  return {
    isLeaf: false,
    missing,
    no,
    split: node.split,
    threshold: Number(node.split_condition),
    yes,
  }
}

/**
 * Turns the exported JSON into a structure with child references already
 * resolved, so evaluation never has to search `children` by node id.
 */
export function compileBooster(payload) {
  if (!payload || payload.format !== MODEL_FORMAT) {
    throw new Error(
      `Unsupported model format: ${payload && payload.format}. Expected ${MODEL_FORMAT}`
    )
  }
  if (!Array.isArray(payload.trees) || payload.trees.length === 0) {
    throw new Error('Booster contains no trees')
  }

  return {
    baseScore: Number(payload.base_score),
    featureNames: payload.feature_names || [],
    trees: payload.trees.map((tree, i) => compileNode(tree, `tree${i}`)),
  }
}

function evaluateTree(root, features) {
  let node = root
  while (!node.isLeaf) {
    const value = features[node.split]
    if (isMissing(value)) {
      node = node.missing
    } else if (Number(value) < node.threshold) {
      node = node.yes
    } else {
      node = node.no
    }
  }
  return node.value
}

export function predict(booster, features) {
  return booster.trees.reduce(
    (total, tree) => total + evaluateTree(tree, features),
    booster.baseScore
  )
}

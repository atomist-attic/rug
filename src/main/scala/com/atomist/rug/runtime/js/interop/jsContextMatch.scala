package com.atomist.rug.runtime.js.interop

/**
  * Fronts JavaScript Context object
  */
case class jsContextMatch(root: Object,
                          matches: _root_.java.util.List[jsSafeCommittingProxy],
                          teamId: String) {
}

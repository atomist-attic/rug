
/**
 * Parameters superclass for all project operations.
 */
interface Parameters {

  // TODO should return a detailed failure? Or exception...
  validate(): boolean
}

/**
 * Convenient superclass for implementations, which validates
 * all parameters OK.
 */
abstract class ParametersSupport implements Parameters {

  validate() { return true; }
}

export {Parameters}
export {ParametersSupport}

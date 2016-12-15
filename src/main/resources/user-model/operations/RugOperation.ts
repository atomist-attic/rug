interface RugOperation {
  name: string
  tags?: string[]
  description: string
  parameters?: Parameter[]
}

class ParameterPattern {
  //TODO - add more from rug lib
  public static java_package: string ="@java_package"
}

interface Parameter {
  required?: boolean
  name: string
  description?: string
  displayName?: string
  validInputDescription?: string
  default?: any
  pattern: string
  maxLength?: number
  minLength?: number
}

/**
 * Status of an operation.
 */
enum Status {
    Success,
    NoChange,
    Error
}

/**
 * Result of running an editor
 */
class Result {
    constructor(
        public status: Status,
        public message: string = "") {}
}

export {RugOperation, Parameter, ParameterPattern, Result, Status}

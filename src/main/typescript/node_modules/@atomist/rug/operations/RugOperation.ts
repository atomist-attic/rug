interface RugOperation {
  name: string
  tags?: string[]
  description: string
  parameters?: Parameter[]
}

abstract class Pattern {
  public static url: string ="@java_package"
  public static any: string ="@any"
  public static group_id: string ="@group_id"
  public static java_class: string ="@java_class"
  public static java_identifier: string ="@java_identifier"
  public static java_package: string ="@java_package"
  public static project_name: string ="@project_name"
  public static port: string ="@port"
  public static ruby_class: string ="@ruby_class"
  public static ruby_identifier: string ="@ruby_identifier"
  public static semantic_version: string ="@semantic_version"
  public static uuid: string ="@uuid"
}

interface Parameter {
  required?: boolean
  name: string
  description?: string
  displayName?: string
  validInput?: string
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

export {RugOperation, Parameter, Pattern, Result, Status}

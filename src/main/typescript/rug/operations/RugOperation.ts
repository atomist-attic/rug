import { PathExpression, PathExpressionEngine } from "../tree/PathExpression";

/**
 * Superinterface for all Rug operations, enabling cataloging.
 */
interface RugOperation {
  readonly name: string;
  readonly tags?: string[];
  readonly description: string;
  readonly parameters?: Parameter[];
}

/**
 * Well-known patterns used in operation parameters.
 */
abstract class Pattern {
  public static url: string = "@java_package";
  public static any: string = "@any";
  // tslint:disable-next-line:variable-name
  public static group_id: string = "@group_id";
  // tslint:disable-next-line:variable-name
  public static artifact_id: string = "@artifact_id";
  // tslint:disable-next-line:variable-name
  public static java_class: string = "@java_class";
  // tslint:disable-next-line:variable-name
  public static java_identifier: string = "@java_identifier";
  // tslint:disable-next-line:variable-name
  public static java_package: string = "@java_package";
  // tslint:disable-next-line:variable-name
  public static project_name: string = "@project_name";
  // tslint:disable-next-line:variable-name
  public static port: string = "@port";
  // tslint:disable-next-line:variable-name
  public static ruby_class: string = "@ruby_class";
  // tslint:disable-next-line:variable-name
  public static ruby_identifier: string = "@ruby_identifier";
  // tslint:disable-next-line:variable-name
  public static semantic_version: string = "@semantic_version";
  // tslint:disable-next-line:variable-name
  public static version_range: string = "@version_range";
  public static uuid: string = "@uuid";
}

interface BaseParameter {
  readonly pattern: string;
  readonly required?: boolean;
  readonly description?: string;
  readonly displayName?: string;
  readonly validInput?: string;
  readonly displayable?: boolean;
  readonly maxLength?: number;
  readonly minLength?: number;
  readonly tags?: string[];
}

interface Parameter extends BaseParameter {
  readonly name: string;
  readonly default?: string;
}

/**
 * Status of an operation.
 */
enum Status {
  Success,
  NoChange,
  Error,
}

/**
 * Result of running an editor
 */
class Result {
  constructor(
    public status: Status,
    public message: string = "") { }
}

/**
 * Severity of a comment from a review
 */
enum Severity {
  Fine,
  Polish,
  Major,
  Broken,
}

/**
 * A comment from a review
 */
class ReviewComment {
  constructor(
    public projectName: string,
    public comment: string,
    public severity: Severity,
    public fileName?: string,
    public line?: number,
    public column?: number) { }
}

export { RugOperation, Parameter, Pattern, Result, Status, ReviewComment, Severity, BaseParameter };

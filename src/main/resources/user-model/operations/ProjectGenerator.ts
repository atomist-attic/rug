import {Project} from "../model/Core"
import {ParametersSupport} from "./Parameters"
import {parameter} from "../support/Metadata"


abstract class GeneratorParameters extends ParametersSupport {

  // TODO need standard regex for package name etc as in Rug
  @parameter({description: "Name of the new project", 
        displayName: "Java Package", 
        pattern: ".*", 
        maxLength: 21})
    project_name: string
}

/**
 * Top level interface for all project generators
 */
interface ProjectGenerator<P extends GeneratorParameters> {

    populate(emptyProject: Project, parameters: P)

}

/**
 * The commonest case. We want to customize a new project
 */
abstract class CustomizingProjectGenerator<P extends GeneratorParameters> 
    implements ProjectGenerator<P> {

    populate(emptyProject: Project, parameters: P) {
        emptyProject.copyEditorBackingFilesPreservingPath("")
        //this.customize(emptyProject, parameters)
    }

   abstract customize(project: Project, parameters: P): void

}

export {GeneratorParameters}
export {ProjectGenerator}
export {CustomizingProjectGenerator}

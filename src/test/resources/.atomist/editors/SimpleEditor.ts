import {ProjectEditor} from 'user-model/operations/ProjectEditor'
import {Parameters} from 'user-model/operations/ProjectEditor'
import {Project} from 'user-model/model/Core'

/**
  Simple editor with no parameters
*/
class SimpleEditor implements ProjectEditor<Parameters> {

    edit(project: Project, p: Parameters) {
        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
        return `Edited Project now containing ${project.fileCount()} files: \n`;
    }
}

// Example of the code we need to add to run this in Nashorn
//let editor : ProjectEditor<Parameters> = new SimpleEditor()

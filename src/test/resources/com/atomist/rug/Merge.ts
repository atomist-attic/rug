import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'


class Merge implements ProjectEditor {

    name: string = "Merge"
    
    description: string = "Merge file"
    
    edit(project: Project) {     
        project.merge("template.vm", "dude.txt", {})
    }

}
export let editor_dude = new Merge();

import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'

@Editor("Edits")
class Merge  {
    
    edit(project: Project) {     
        project.merge("template.mustache", "dude.txt", {})
    }

}
export let editor_dude = new Merge();

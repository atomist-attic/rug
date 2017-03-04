import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Project} from '@atomist/rug/model/Core'

class AlwaysFails implements ProjectEditor {
    name = "Walter"
    description = "angry"
        
    edit(project: Project) {     
        throw new Error("I have buddies who died face down in the muck")
    }

}
export let walter = new AlwaysFails();

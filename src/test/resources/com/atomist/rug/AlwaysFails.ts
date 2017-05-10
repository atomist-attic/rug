import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'

@Editor("Walkter", "angry")
class AlwaysFails  {

    edit(project: Project) {     
        throw new Error("I have buddies who died face down in the muck")
    }

}
export let walter = new AlwaysFails();

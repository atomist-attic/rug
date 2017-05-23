import {Project} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'

@Editor("deliberate npe")
class DeliberateNpe{
    edit(project: Project) {
        let x = null
        x.throwNpe()
    }
  }

export let editor = new DeliberateNpe();

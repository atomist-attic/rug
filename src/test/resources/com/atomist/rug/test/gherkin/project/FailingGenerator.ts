import {Project} from '@atomist/rug/model/Core'
import {Generator} from '@atomist/rug/operations/Decorators'

@Generator("FailingGenerator","My failing Generator")
export class FailingGenerator {

     populate(project: Project) {
        throw Error(`I hate the world`)
    }
}
export let gen = new FailingGenerator()
import {Project} from '@atomist/rug/model/Core'
import {Generator} from '@atomist/rug/operations/Decorators'

@Generator("SimpleGenerator","My simple Generator")
export class SimpleGenerator {

     populate(project: Project) {
         console.log("In SimpleGenerator")
        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
    }
}
export let gen = new SimpleGenerator()
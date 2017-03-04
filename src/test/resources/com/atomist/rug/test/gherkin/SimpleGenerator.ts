import {Project} from '@atomist/rug/model/Core'
import {Generator} from '@atomist/rug/operations/Decorators'

@Generator("SimpleGenerator","My simple Generator")
export class SimpleGenerator {

     content: string = "woot"

     populate(project: Project) {
        let len: number = this.content.length;
        if(project.name() != "woot"){
           throw Error(`Project name should be woot, but was ${project.name()}`)
        }
        project.addFile("src/from/typescript", "Anders Hjelsberg is God");
    }
}
export let gen = new SimpleGenerator()
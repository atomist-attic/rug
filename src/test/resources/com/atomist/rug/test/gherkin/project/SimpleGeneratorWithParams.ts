import {Project} from '@atomist/rug/model/Core'
import {Generator} from '@atomist/rug/operations/Decorators'
import {Parameter} from '@atomist/rug/operations/Decorators'

@Generator("SimpleGeneratorWithParams","My simple Generator")
export class SimpleGeneratorWithParams {

    @Parameter({description: "text", pattern: "^[a-zA-Z ]*$"})
    text: string

     populate(project: Project) {
        project.addFile("src/from/typescript", this.text);
    }
}
export let gen = new SimpleGeneratorWithParams()
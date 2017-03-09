import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Parameter} from '@atomist/rug/operations/Decorators'

export class AlpEditor implements ProjectEditor {
    name: string = "AlpEditor"
    description: string = "ALP history"

    @Parameter({description: "Bold PM", pattern: "^.*$"})
    heir: string

    edit(project: Project) {
     project.addFile(this.heir, "Can a souffle rise twice?")
    }
}

export let xx_editor = new AlpEditor()
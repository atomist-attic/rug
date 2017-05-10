import {Project} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {Parameter} from '@atomist/rug/operations/Decorators'

@Editor("edits")
export class AlpEditor  {

    edit(project: Project) {
        project.addFile("Paul", "Can a souffle rise twice?");
    }
}

export const alpEditor = new AlpEditor();

@Editor("edits")
export class AlpEditorWithParameters  {

    @Parameter({description: "Bold PM", pattern: "^[A-Za-z]*$"})
    heir: string;

    edit(project: Project) {
        project.addFile(this.heir, "Can a souffle rise twice?");
    }
}

export const alpEditorWithParameters = new AlpEditorWithParameters();

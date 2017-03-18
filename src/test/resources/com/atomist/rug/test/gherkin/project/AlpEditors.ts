import {Project} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {Parameter} from '@atomist/rug/operations/Decorators'


export class AlpEditor implements ProjectEditor {
    name: string = "AlpEditor";
    description: string = "ALP history";

    edit(project: Project) {
        project.addFile("Paul", "Can a souffle rise twice?");
    }
}

export const alpEditor = new AlpEditor();


export class AlpEditorWithParameters implements ProjectEditor {
    name: string = "AlpEditorWithParameters"
    description: string = "ALP history";

    @Parameter({description: "Bold PM", pattern: "^.*$"})
    heir: string;

    edit(project: Project) {
        project.addFile(this.heir, "Can a souffle rise twice?");
    }
}

export const alpEditorWithParameters = new AlpEditorWithParameters();

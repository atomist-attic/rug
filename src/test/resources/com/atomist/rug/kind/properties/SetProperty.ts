import {Editor} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'
import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from "@atomist/rug/operations/Decorators";
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Properties} from '@atomist/rug/model/Core'

@Editor("PropertiesEdit")
class SetProperty  {

    @Parameter({description: "Change comment to this", pattern: "^[\\s\\S]*$"})
    value: string
    
    edit(project: Project) {
    
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        
            eng.with<Properties>(project, '//Properties()', p => {
                if (p.path == "src/main/resources/application.properties") {
                    p.setProperty(this.value, "8181")
                }
            })
    
    }

}
export let editor_propertiesEdit = new SetProperty();

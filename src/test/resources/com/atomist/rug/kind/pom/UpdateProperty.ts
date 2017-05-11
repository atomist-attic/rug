import {Editor} from '@atomist/rug/operations/Decorators'
import {Project,Pom} from '@atomist/rug/model/Core'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'

@Editor("UpdatePropertyEditor", "Updates properties")
class UpdateProperty {

 edit(project: Project) {
   project.context.pathExpressionEngine.with<Pom>(project, `/Pom()`, p => {
     if (p.path == "pom.xml")
       p.setGroupId("mygroup")
   })
 }
}

export let pe = new UpdateProperty()

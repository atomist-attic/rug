import { Project } from '@atomist/rug/model/Core'
import { Generator } from '@atomist/rug/operations/Decorators'

@Generator("JavaScriptContextTypeScriptTest", "Uses JavaScriptContext from TypeScript")
class JavaScriptContextTypeScriptTest {

    populate(project: Project) {

        let pom = project.addFile('hello.txt', 'hello yo');

    }
}
export let generator = new JavaScriptContextTypeScriptTest();
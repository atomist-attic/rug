import {Project} from '@atomist/rug/model/Core'
import {Editor} from '@atomist/rug/operations/Decorators'
import {TreeNode, TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'


class BananaType implements TypeProvider {

    typeName = "banana";

    find(context: TreeNode): TreeNode[] {
        return [new Banana()];
    }

}

class Banana implements TreeNode {

    parent() {
        return null;
    }

    nodeName(): string {
        return "banana";
    }

    nodeTags(): string[] {
        return [this.nodeName()];
    }

    value(): string {
        return "yellow";
    }

    children() {
        return [];
    }

}

@Editor("Constructed", "Uses single dynamic grammar")
class SimpleBanana {
    edit(project: Project) {
        let mg = new BananaType();
        let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg);

        let i = 0;
        eng.with<Banana>(project, "//File()/banana()", n => {
            //console.log("Checking color of banana")
            if (n.value() != "yellow")
                throw new Error(`Banana is not yellow but [${n.value()}]. Sad.`);
            i++
        });
        if (i == 0)
            throw new Error("No bananas tested. Sad.");
    }
}
export let editor = new SimpleBanana();

import {Project, File} from '@atomist/rug/model/Core'
import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
import {PathExpression,TreeNode,TypeProvider} from '@atomist/rug/tree/PathExpression'
import {PathExpressionEngine} from '@atomist/rug/tree/PathExpression'
import {Match} from '@atomist/rug/tree/PathExpression'
import {Parameter} from '@atomist/rug/operations/RugOperation'


class FruitererType implements TypeProvider {

 typeName = "fruiterer"

 private isFile(f: TreeNode): f is File {
   return true
 }

 find(context: TreeNode): TreeNode[] {
   if (this.isFile(context)) {
     let f = context as File
     if (f.isJava)
      return [ new Fruiterer(f) ]
      else return []
   }
   else 
    return []
 }
}

class Fruiterer implements TreeNode {

  constructor(public file: File) {}

  parent() { return this.file }

  nodeName: string = "fruiterer";

  nodeTags: string[] = [ this.nodeName];

  children() { return [ new MutatingBanana(this.file), new Pear() ] }

}

class MutatingBanana implements TreeNode {

  constructor(public file: File) {}

  parent() { return this.file }

  nodeName: string = "mutatingBanana";

  nodeTags: string[] = [ this.nodeName];

  children() { return [] }

  mutate(): void { 
    this.file.prepend("I am evil")
  }
}

class Pear implements TreeNode {

  parent() { return null }

  nodeName: string = "pear";

  nodeTags: string[] = [ this.nodeName];

  children() { return [] }

}

class TwoLevel implements ProjectEditor {

    name: string = "Constructed"
    description: string = "Uses single microgrammar"

    edit(project: Project) {
      let mg = new FruitererType()
      let eng: PathExpressionEngine = project.context.pathExpressionEngine.addType(mg)

      eng.with<MutatingBanana>(project, "//File()/fruiterer()/mutatingBanana()", n => {
        n.mutate()
      })
    }
  }

export let editor = new TwoLevel();
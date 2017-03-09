import { Project } from '@atomist/rug/model/Core'
import { Pattern } from '@atomist/rug/operations/RugOperation'
import { Editor, Parameter } from '@atomist/rug/operations/Decorators'

/*
 * Create a test, so you can make an editor and run it from Scala
 */

@Editor("AddTypeScriptTest", "Add a test")
class AddTypeScriptTest {

    @Parameter({
        pattern: Pattern.any
    })
    class_under_test: string;

    edit(project: Project) {

        let portions = this.class_under_test.split(".");
        let after_the_last_dot = portions[portions.length - 1];
        let class_name = after_the_last_dot;

        let packageName = this.class_under_test.replace(/\.[^.]*$/, "")

        let sample_ts_under_resources = "com/atomist/rug/ts/SampleTypeScriptTest.ts"
        let sample_ts_file_path = "src/test/resources/" + sample_ts_under_resources

        let ts_under_resources = this.class_under_test.replace(/\./g, '/') + "TypeScriptTest.ts"
        let ts_file_path = "src/test/resources/" + ts_under_resources
        let scala_file_path = "src/test/scala/" + this.class_under_test.replace(/\./g, '/') + "TypeScriptTest.scala"

        project.copyEditorBackingFileOrFail(sample_ts_file_path, ts_file_path)
        project.copyEditorBackingFileOrFail("src/test/scala/com/atomist/rug/ts/SampleTypeScriptTest.scala", scala_file_path)

        let tsFile = project.findFile(ts_file_path)
        tsFile.replace("Sample", class_name)

        let scalaFile = project.findFile(scala_file_path)
        scalaFile.replace(sample_ts_under_resources, ts_under_resources)
        scalaFile.replace("package com.atomist.rug.ts", "package " + packageName)
        scalaFile.replace("Sample", class_name)

    }
}
export let editor = new AddTypeScriptTest();
import {EditProject} from '@atomist/rug/operations/ProjectEditor'
import {Project, Xml} from '@atomist/rug/model/Core'
import { Pattern, RugOperation } from '@atomist/rug/operations/RugOperation'
import {PathExpression,PathExpressionEngine,TextTreeNode} from '@atomist/rug/tree/PathExpression'
import { Editor, Tags, Parameter } from '@atomist/rug/operations/Decorators'

/*
    Return a path expression to match the version of a particular dependency, if found

    <dependencies> ...
		<dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>gherkin</artifactId>
            <version>4.0.0</version>
        </dependency>
*/
export function versionOfDependency(group: string, artifact: string) {
    return new PathExpression<TextTreeNode,TextTreeNode>(
        `/*[@name='pom.xml']/XmlFile()/project/dependencies/dependency
            [/groupId//TEXT[@value='${group}']]
            [/artifactId//TEXT[@value='${artifact}']]
            /version//TEXT
        `
    )
}

@Editor("UpgradeVersion", "Find and upgrade POM version")
export class UpgradeVersion implements EditProject {

    @Parameter({pattern: Pattern.group_id, description: "Group to match"})
    group: string

    @Parameter({pattern: Pattern.artifact_id, description: "Artifact to match"})
    artifact: string

    @Parameter({pattern: Pattern.semantic_version, description: "Version to upgrade to"})
    desiredVersion: string
    
    edit(project: Project) {
        let eng: PathExpressionEngine = project.context.pathExpressionEngine;
        let search = versionOfDependency(this.group, this.artifact)
        eng.with<TextTreeNode>(project, search, version => {
            if (version.value() != this.desiredVersion) {
                //console.log(`Updated to desired version ${this.desiredVersion}`)
                version.update(this.desiredVersion)
            }
        })
    }
}

export const uv = new UpgradeVersion();

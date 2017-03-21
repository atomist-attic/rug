import {ProjectEditor,EditProject} from '@atomist/rug/operations/ProjectEditor'
import {Project, Xml} from '@atomist/rug/model/Core'
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

    //TODO correct to use well-known pattern
    @Parameter({pattern: "^.*$$", description: "Group to match"})
    group: string

    @Parameter({pattern: "^.*$$", description: "Artifact to match"})
    artifact: string

    @Parameter({pattern: "^.*$$", description: "Version to upgrade to"})
    desiredVersion: string
    
    edit(project: Project) {
        let eng: PathExpressionEngine = project.context().pathExpressionEngine();
        let search = versionOfDependency(this.group, this.artifact)
        // TODO can we use OR to get rid of this expression
        eng.with<TextTreeNode>(project, search.expression, version => {
            console.log(`Found version ${version.value()}`)
            if (version.value() != this.desiredVersion) {
                console.log(`Updated to desired version ${this.desiredVersion}`)
                version.update(this.desiredVersion)
            }
        })
    }
}

export const uv = new UpgradeVersion();


export class ArtifactRange {

    constructor(public from: string, public to: string) {}

    satisfies(s: string): boolean {
        // TODO parse this properly using sem ver
        return s == this.from || s == this.to
    }
}


import * as node from "./Nodes"
import {queryByExample} from "@atomist/rug/tree/QueryByExample"

declare var glob 

let c = new node.Commit().withMadeBy(
       new node.Person("Ebony").withGitHubId(new node.GitHubId("gogirl"))
    )

glob = queryByExample(c)

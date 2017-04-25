import { HandleEvent, ChannelAddress, DirectedMessage, LifecycleMessage, MessageMimeTypes, ResponseMessage, EventPlan } from '@atomist/rug/operations/Handlers'
import { GraphNode, Match, PathExpression } from '@atomist/rug/tree/PathExpression'
import { EventHandler, Tags } from '@atomist/rug/operations/Decorators'
import { K8Pod } from "@atomist/rug/cortex/stub/K8Pod"
import { DockerImage } from "@atomist/rug/cortex/DockerImage"
import { Tag } from "@atomist/rug/cortex/Tag"
import { Commit } from "@atomist/rug/cortex/Commit"
import { Repo } from "@atomist/rug/cortex/Repo"
import { ChatChannel } from "@atomist/rug/cortex/ChatChannel"

@EventHandler("pod-container-image-pulled", "Handle Kubernetes Pod container image pulled",
     `/K8Pod()
        [@state='Pulled']
        [/images::DockerImage()
            [/tag::Tag()
                [/commit::Commit()
                    [/builds::Build()
                        [/push::Push()
                            [/commits::Commit()/author::GitHubId()
                                [/person::Person()/chatId::ChatId()]?]
                            [/repo::Repo()]]]]]]`
)
@Tags("kubernetes")
class Pulled implements HandleEvent<K8Pod, K8Pod> {

    handle(event: Match<K8Pod, K8Pod>): EventPlan {
        const pod: K8Pod = event.root as K8Pod
        const image: DockerImage = pod.images[0]
        const commit: Commit = image.tag.commit
        const repo: Repo = commit.repo

        const lifecycleId: string = "commit_event/" + repo.owner + "/" + repo.name + "/" + commit.sha
        let message: LifecycleMessage = new LifecycleMessage(pod, lifecycleId)

        return EventPlan.ofMessage(message)
    }
}

export const pulled = new Pulled()

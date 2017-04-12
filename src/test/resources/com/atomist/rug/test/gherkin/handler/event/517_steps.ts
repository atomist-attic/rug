import { Given, When, Then, EventHandlerScenarioWorld } from "@atomist/rug/test/handler/Core"
import { DirectedMessage, LifecycleMessage, PlanMessage } from "@atomist/rug/operations/Handlers"
import { Build } from "@atomist/rug/cortex/stub/Build"
import { Commit } from "@atomist/rug/cortex/stub/Commit"
import { Tag } from "@atomist/rug/cortex/stub/Tag"
import { DockerImage } from "@atomist/rug/cortex/stub/DockerImage"
import { K8Pod } from "@atomist/rug/cortex/stub/K8Pod"
import { K8Spec } from "@atomist/rug/cortex/stub/K8Spec"
import { Environment } from "@atomist/rug/cortex/stub/Environment"
import { Person } from "@atomist/rug/cortex/stub/Person"
import { ChatId } from "@atomist/rug/cortex/stub/ChatId"
import { GitHubId } from "@atomist/rug/cortex/stub/GitHubId"
import { Repo } from "@atomist/rug/cortex/stub/Repo"
import { Push } from "@atomist/rug/cortex/stub/Push"
import { ChatChannel } from "@atomist/rug/cortex/stub/ChatChannel"


function buildPodEvent(state: string, domain: string = "prod.atomist.services."): K8Pod {
    const chatId: ChatId = new ChatId
    chatId.withScreenName("me")

    const person: Person = new Person
    person.withChatId(chatId)

    const gitHubId: GitHubId = new GitHubId
    gitHubId.withPerson(person)

    const commit: Commit = new Commit
    const tag: Tag = new Tag
    commit.withAuthor(gitHubId)
    tag.withCommit(commit)
    commit.addTags(tag)
    commit.withSha("b20479edc0c2a202b18814cbd4e95463df04fb83")

    const build: Build = new Build
    commit.addBuilds(build)
    build.withCommit(commit)

    const push: Push = new Push
    push.addCommits(commit)
    push.addBuilds(build)
    build.withPush(push)
    commit.withPush(push)

    const repo: Repo = new Repo
    commit.withRepo(repo)
    push.withRepo(repo)
    repo.withOwner("atomist-rugs")
    repo.withName("myrugs")

    const channel: ChatChannel = new ChatChannel
    repo.addChannels(channel)

    const image: DockerImage = new DockerImage
    image.withTag(tag)

    const environment: Environment = new Environment
    environment.withName(domain)

    const spec: K8Spec = new K8Spec
    spec.withEnvironment(environment)
    image.withSpec(spec)
    spec.addImages(image)

    const pod: K8Pod = new K8Pod
    pod.withState(state)
    pod.withSpec(spec)
    pod.addImages(image)
    pod.withName("myservice")

    return pod
}


Given("pod container image pulled handler registered", (world: EventHandlerScenarioWorld) => {
    world.registerHandler("pod-container-image-pulled")
})

When("crash looping occurs", (world: EventHandlerScenarioWorld) => {
    const pod: K8Pod = buildPodEvent("BackOff")
    world.sendEvent(pod)
})

When("a deployment was successful", (world: EventHandlerScenarioWorld) => {
    const pod: K8Pod = buildPodEvent("Started")
    world.sendEvent(pod)
})

When("a container image was pulled", (world: EventHandlerScenarioWorld) => {
    const pod: K8Pod = buildPodEvent("Pulled")
    world.sendEvent(pod)
})

When("a pod is terminating", (world: EventHandlerScenarioWorld) => {
    const pod: K8Pod = buildPodEvent("Killing")
    world.sendEvent(pod)
})

When("a pod is unhealthy", (world: EventHandlerScenarioWorld) => {
    const pod: K8Pod = buildPodEvent("Unhealthy")
    world.sendEvent(pod)
})

Then("the handler is triggered", (world: EventHandlerScenarioWorld) => {
    return world.plan() != null
})

Then("the committer should receive a direct message", (world: EventHandlerScenarioWorld) => {
    const message = world.plan().messages[0] as DirectedMessage
    return message.channelNames.length == 1
})

Then("we should receive a message", (world: EventHandlerScenarioWorld) => {
    const lifecycleId = "commit_event/atomist-rugs/myrugs/b20479edc0c2a202b18814cbd4e95463df04fb83"
    const message = world.plan().messages[0] as LifecycleMessage
    return message.lifecycleId == lifecycleId
})
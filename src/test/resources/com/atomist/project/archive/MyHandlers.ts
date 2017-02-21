import {HandleResponse, HandleEvent, Respondable, HandleCommand, Respond, Instruction, Response, HandlerContext, Plan, Message, Execute} from '@atomist/rug/operations/Handlers'
import {TreeNode, Match, PathExpression} from '@atomist/rug//tree/PathExpression'
import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'


@EventHandler("ClosedIssueReopener","Reopens closed issues",  "/issue")
@Tags("github", "issues")
class SimpleHandler implements HandleEvent<Issue,Issue>{
  handle(match: Match<Issue,Issue>): Plan {
    let issue = match.root()
    let reopen = issue.reopen
    reopen.onSuccess = {name: ""}
  //  reopen.onSuccess = {text: `Issue ${issue.number} has been reopened successfully`}
  //  reopen.onError = {name: "sendFailureMessage", parameters: {issue: issue, who: "jess"}}
    return new Plan().add(reopen)
  }
}

export let simple = new SimpleHandler();

@ResponseHandler("IssueClosedResponder", "Logs failed issue reopen attempts")
class IssueReopenFailedResponder implements HandleResponse<Issue>{

  @Parameter({description: "Name of recipient", pattern: "^.*$"})
  who: string

  handle(response: Response<Issue>): Plan {
    let issue = response.body
    let msg = new Message(`Issue ${issue.number} was not reopened, trying again`)
    msg.channelId = this.who
    return new Plan()
      .add(msg)
  }
}

export let responder = new IssueReopenFailedResponder();

@CommandHandler("LicenseAdder","Runs the SetLicense editor on a bunch of my repos")
@Tags("github", "license")
@Intent("add license")
class LicenseAdder implements HandleCommand{

  @Parameter({description: "The name of the license", pattern: "^.*$"})
  license: string;

  handle(command: HandlerContext) : Plan {
    let result = new Plan()
    result.add({instruction: {kind: "execute",
                    name: "HTTP",
                    parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}},
                    onError: {text: "No kitties for you today!"}})
    return result;
  }
}

export let adder = new LicenseAdder();

@CommandHandler("ListIssuesHandler","Lists open github issues in slack")
@Tags("github", "issues")
@Intent("list issues")
class IssueLister implements HandleCommand{

  @Parameter({description: "Days", pattern: "^.*$", maxLength: 100, required: false })
  days = 1

  handle(ctx: HandlerContext) : Message {
    var match: Match<Issue,Issue>; // ctx.pathExpressionEngine().evaluate<Issue,Issue>("/Repo()/Issue[@raisedBy='kipz']")
    let issues = match.matches();
    if (issues.length > 0) {
              let attachments = `{"attachments": [` + issues.map(i => {
                 let text = JSON.stringify(`#${i.number}: ${i.title}`)
                 if (i.state == "closed") {
                     return `{
                   "fallback": ${text},
                   "author_icon": "http://images.atomist.com/rug/issue-closed.png",
                   "color": "#bd2c00",
                   "author_link": "${i.issueUrl}",
                   "author_name": ${text}
                }`
                 }
                 else {
                     return `{
                 "fallback": ${text},
                 "author_icon": "http://images.atomist.com/rug/issue-open.png",
                 "color": "#6cc644",
                 "author_link": "${i.issueUrl}",
                 "author_name": ${text}
              }`
                 }
             }).join(",") + "]}"
             return {text: attachments}
         }else{
            return {text: "You are not crushin' it right now!"}
         }
  }
}

export let lister = new IssueLister();

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties")
class KittieFetcher implements HandleCommand{
  handle(command: HandlerContext) : Plan {
    let result = new Plan()
    result.add({instruction: {kind: "execution",
                name: "HTTP",
                parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}},
                onSuccess: {kind: "respond", name: "Kitties"},
                onError: {text: "No kitties for you today!"}})
    return result;
  }
}

@ResponseHandler("Kitties", "Prints out kitty urls")
class KittiesResponder implements HandleResponse<Object>{
  handle(response: Response<Object>) : Message {
    let results = response.body as any;
    return new Message(results.urls.join(","))
  }
}

export let kittRes = new KittiesResponder();

interface Issue extends TreeNode {
  reopen: Respondable<Execute>
  title: string
  number: string
  state: string
  issueUrl: string
}

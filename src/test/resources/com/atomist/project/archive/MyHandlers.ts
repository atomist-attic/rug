import {HandleResponse, HandleEvent, EventRespondable, HandleCommand, Respond, Instruction, Response, HandlerContext, EventPlan, CommandPlan, Execute, DirectedMessage, UserAddress, ResponseMessage, ChannelAddress} from '@atomist/rug/operations/Handlers'
import {TreeNode, Match, PathExpression} from '@atomist/rug//tree/PathExpression'
import {EventHandler, ResponseHandler, CommandHandler, Parameter, Tags, Intent} from '@atomist/rug/operations/Decorators'
import {Project} from '@atomist/rug/model/Core'


@EventHandler("ClosedIssueReopener","Reopens closed issues",  "/issue")
@Tags("github", "issues")
class SimpleHandler implements HandleEvent<Issue,Issue>{
  handle(match: Match<Issue,Issue>): EventPlan {
    let issue = match.root()
    let reopen = issue.reopen
    reopen.onSuccess = new DirectedMessage("blah", new ChannelAddress("#blah"))
    return new EventPlan().add(reopen)
  }
}

export let simple = new SimpleHandler();

@ResponseHandler("IssueClosedResponder", "Logs failed issue reopen attempts")
class IssueReopenFailedResponder implements HandleResponse<Issue>{

  @Parameter({description: "Name of recipient", pattern: "^.*$"})
  who: string

  handle(response: Response<Issue>): CommandPlan {
    let issue = response.body
    let msg = new DirectedMessage(`Issue ${issue.number} was not reopened, trying again`, new ChannelAddress(this.who))
    return new CommandPlan()
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

  handle(command: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({instruction: {kind: "execute",
                    name: "HTTP",
                    parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}},
                    onError: {body: "No kitties for you today!", kind: "directed", contentType: "text/plain"}})
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

  handle(ctx: HandlerContext) {
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
             return new CommandPlan().add({body: attachments, contentType: "text/plain", kind: "directed"})
         }
         else{
            return new CommandPlan().add({body: "You are not crushin' it right now!", contentType: "text/plain", kind: "directed"})
         }
  }
}

export let lister = new IssueLister();

@CommandHandler("ShowMeTheKitties","Search Youtube for kitty videos and post results to slack")
@Tags("kitty", "youtube", "slack")
@Intent("show me kitties")
class KittieFetcher implements HandleCommand {

  handle(command: HandlerContext) : CommandPlan {
    let result = new CommandPlan()
    result.add({instruction: {kind: "execution",
                name: "HTTP",
                parameters: {method: "GET", url: "http://youtube.com?search=kitty&safe=true", as: "JSON"}},
                onSuccess: {kind: "respond", name: "Kitties"},
                onError: new ResponseMessage("No kitties for you today!")})
    return result;
  }
}

@ResponseHandler("Kitties", "Prints out kitty urls")
class KittiesResponder implements HandleResponse<Object> {
  handle(response: Response<Object>): CommandPlan {
    let results = response.body as any;
    return new CommandPlan().add(new DirectedMessage(results.urls.join(","), new UserAddress("woot")));
  }
}

export let kittRes = new KittiesResponder();

interface Issue extends TreeNode {
  reopen: EventRespondable<Execute>
  title: string
  number: string
  state: string
  issueUrl: string
}

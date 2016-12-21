package com.atomist.tree.content.text.microgrammar

import com.atomist.parse.java.ParsingTargets
import org.scalatest.{FlatSpec, Matchers}

class MatcherDSLUsageTest extends FlatSpec with Matchers {

  val mgp = new MatcherDSLDefinitionParser

  it should "match literal" in {
    val matcher = mgp.parse("def foo")
    matcher.matchPrefix(0, "def foo thing") match {
      case Some(pm) =>
    }
  }

  // TODO this is a debatable case. Why wouldn't we just match with a regex or literal string
  // if there's nothing dynamic in the content? No nodes are created
  it should "match literal using microgrammar" in pendingUntilFixed {
    val matcher = mgp.parse("def foo")
    //println(matcher)
    // Problem is that this is discarded as nothing is bound
    val mg = new MatcherMicrogrammar("deff", matcher)
    mg.findMatches("def foo bar").size should be(1)
  }

  it should "match regex using microgrammar" in {
    val matcher = mgp.parse("def $foo:§f.o§")
    //println(matcher)
    val mg = new MatcherMicrogrammar("deff", matcher)
    val input = "def foo bar"
    matcher.matchPrefix(0, input) match {
      case Some(pm) =>
    }
    mg.findMatches(input).size should be(1)
  }

  it should "match regex in Maven POM" in {
    val proj = ParsingTargets.NewStartSpringIoProject

    val mg = new MatcherMicrogrammar("gid",
      mgp.parse("<modelVersion>$modelVersion:§[a-zA-Z0-9_\\.]+§</modelVersion>"))
    println(mg.matcher)
    val pom = proj.findFile("pom.xml").get.content
    val matches = mg.findMatches(pom)
    println(matches)
    matches.size should be(1)
  }

  it should "parse Slack emoji html" in pendingUntilFixed {
    val html =
      """<tr class="emoji_row">
        |													<td class="align_middle"><span data-original="https://emoji.slack-edge.com/T024F4A92/666/5b9d8b4d571e51c5.jpg" class="lazy emoji-wrapper"></span></td>
        |												<td class="align_middle" style="white-space: normal; word-break: break-all; word-wrap: break-word; height: 100%; vertical-align:middle;">:666:
        |												</td>
        |													<td class="align_middle">Image</td>
        |												<td class="author_cell hide_on_mobile" style="white-space: normal;">
        |															<a href="/team/asf" class="display_flex align_items_center break_word bold">
        |									<span class="small_right_margin"><span class="member_image thumb_24" style="background-image: url('https://secure.gravatar.com/avatar/ea56481d511928f89ecbfba1578b675b.jpg?s=48&amp;d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0025-48.png')" data-thumb-size="24" data-member-id="U024F4XCH"></span></span>
        |									asf
        |								</a>
        |													</td>
        |						<td class="align_middle align_right bold">
        |							<ts-icon class="ts_icon_times_circle subtle_silver cursor_pointer candy_red_on_hover" data-emoji-type="emoji" data-emoji-name="666" data-action="emoji.remove"></ts-icon>
        |						</td>
        |					</tr>""".stripMargin

    val mg = new MatcherMicrogrammar("emoji", mgp.parse("""<tr class="emoji_row">.*<span data-original="$emojiUrl:§https://.*§".*:$name:§[a-z0-9_-]*§:.*</tr"""))

    val matches = mg.findMatches(html)
    matches.size should be(1)

    for (elem <- matches;
         field <- elem.fieldValues
    ) {
      println(field.nodeName + " = " + field.value)
    }

  }

}

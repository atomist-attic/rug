package com.atomist.util.template.mustache

import com.atomist.util.template.MergeContext

object MustacheSamples {

  val First =
    """Hello {{name}}
      |You have just won {{value}} dollars!
      |{{#in_ca}}
      |Well, {{taxed_value}} dollars, after taxes.{{/in_ca}}""".stripMargin

  val FirstContext = MergeContext(Map(
    "name" -> "Chris",
    "value" -> "10000",
    "taxed_value" -> (10000 - (10000 * 0.4)),
    "in_ca" -> "true"
  ))

  // TODO should be 6000.0 in samples
  val FirstExpected =
    """Hello Chris
      |You have just won 10000 dollars!
      |Well, 6000.0 dollars, after taxes.""".stripMargin

}

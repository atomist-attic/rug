package com.atomist.rug.kind.yml

import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}

object YmlUsageTestTargets {

  val xYml =
    """
      |group: queen
      |artifact: "A Night at the Opera"
      |dependencies:
      |- "Death on Two Legs (Dedicated to....)"
      |- "Lazing on a Sunday Afternoon"
      |- "I'm in Love with My Car"
      |- "You're My Best Friend"
      |- "'39"
      |- "Sweet Lady"
      |- "Seaside Rendezvous"
      |- "The Prophet's Song"
      |- "Love of My Life"
      |- "Good Company"
      |- "Bohemian Rhapsody"
      |- "God Save the Queen"
      |common: everywhere
    """.stripMargin

  val yYml =
    """
      |artist: Paul Simon
      |album: Graceland
      |songs:
      |- The Boy in the Bubble
      |- Graceland
      |- I Know What I Know
      |- Gumboots
      |- Diamonds on the Soles of Her Shoes
      |- You Can Call Me Al
      |- Under African Skies
      |- Homeless
      |- Crazy Love, Vol. II
      |- That Was Your Mother
      |- All Around the World or the Myth of Fingerprints
      |common: everywhere
    """.stripMargin

  val zYml =
    """
      |something: completely different
      |this:
      |  structure:
      |    is:
      |    - more
      |    - nested
      |that:
      |- is
      |- not
      |some:
      |  nesting: one level
      |common: everywhere
    """.stripMargin

  val singleAS = new SimpleFileBasedArtifactSource("single",
    Seq(StringFileArtifact("x.yml", xYml))
  )

  val yamlAS = new SimpleFileBasedArtifactSource("triple",
    Seq(
      StringFileArtifact("x.yml", xYml),
      StringFileArtifact("src/main/y.yml", yYml),
      StringFileArtifact("target/build/z.yml", zYml)
    )
  )

  val fullAS = new SimpleFileBasedArtifactSource("full",
    Seq(
      StringFileArtifact("README.md", "This is a README\n"),
      StringFileArtifact("x.yml", xYml),
      StringFileArtifact("src/main/y.yml", yYml),
      StringFileArtifact("src/main/not-yml.txt", "Not YAML file.\n\nWe should not find this.\n"),
      StringFileArtifact("target/build/z.yml", zYml)
    )
  )

  val allAS: Seq[(SimpleFileBasedArtifactSource, Int)] =
    Seq((singleAS, 1), (yamlAS, 3), (fullAS, 3))

  // --- !clarkevans.com/^invoice
  val YamlOrgStart =
    """
      |invoice: 34843
      |date   : 2001-01-23
      |bill-to: &id001
      |    given  : Chris
      |    family : Dumars
      |    address:
      |        lines: |
      |            458 Walkman Dr.
      |            Suite #292
      |        city    : Royal Oak
      |        state   : MI
      |        postal  : 48046
      |ship-to: *id001
      |product:
      |    - sku         : BL394D
      |      quantity    : 4
      |      description : Basketball
      |      price       : 450.00
      |    - sku         : BL4438H
      |      quantity    : 1
      |      description : Super Hoop
      |      price       : 2392.00
      |tax  : 251.42
      |total: 4443.52
      |comments: >
      |    Late afternoon is best.
      |    Backup contact is Nancy
      |    Billsmer @ 338-4338.
    """.stripMargin

}

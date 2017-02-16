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

}



package com.atomist.param

import org.scalatest.{FlatSpec, Matchers}

class ParameterValidationPatternsTest extends FlatSpec with Matchers {

  val ProjectNamePattern = ParameterValidationPatterns.ProjectName.r
  val GroupNamePattern = ParameterValidationPatterns.GroupName.r

  val JavaPackagePattern = ParameterValidationPatterns.JavaPackage.r
  val JavaClassPattern = ParameterValidationPatterns.JavaClass.r

  val NonNegativeIntPattern = ParameterValidationPatterns.NonNegativeInteger.r
  val Version = ParameterValidationPatterns.Version.r

  val UrlPattern = ParameterValidationPatterns.Url.r

  val UuidPattern = ParameterValidationPatterns.Uuid.r

  "project name regexp" should "match valid names" in {
    Seq("com", "dog-catcher", "dog_catcher", "dogCatcher", "_dog.catcher-", "8675309") foreach {
      case ProjectNamePattern(_*) =>
      case s => fail(s"<$s> did not match $ProjectNamePattern")
    }
  }

  it should "reject invalid names" in {
    Seq("@*", "7$44", "/what/in/gods/holy/name/are/you/blathering/about", "dog=catcher", "dog$catcher") foreach {
      case s@ProjectNamePattern(_*) => fail(s"<$s> matched $ProjectNamePattern")
      case s =>
    }
  }

  "group name regexp" should "match valid names" in {
    Seq("com", "dog_catcher", "dog.catcher", "com.dog.catcher", "atomist-project-templates", "common-editors") foreach {
      case GroupNamePattern(_*) =>
      case s => fail(s"<$s> did not match $GroupNamePattern")
    }
  }

  it should "reject invalid group names" in {
    Seq("@*", "744", "/what/in/gods/holy/name/are/you/blathering/about", "fiddle$stick") foreach {
      case s@GroupNamePattern(_*) => fail(s"<$s> matched $GroupNamePattern")
      case s =>
    }
  }

  "Java package regexp" should "match valid packages" in {
    Seq("", "com", "com.foo", "com.foo.bar", "com.my_co", "com.route66", "com.dollar$tr", "_foo.bar_", "$$$") foreach {
      case JavaPackagePattern(_*) =>
      case s => fail(s"<$s> did not match $JavaPackagePattern")
    }
  }

  it should "reject invalid packages" in {
    Seq("@*", "744", "/what/in/gods/holy/name/are/you/blathering/about") foreach {
      case s@JavaPackagePattern(_*) => fail(s"<$s> matched $JavaPackagePattern")
      case s =>
    }
  }

  "Java class regexp" should "match valid classes" in {
    Seq("Dog", "FavoriteDog", "Favorite_Dog", "Favorite$Dog", "Dog1") foreach {
      case JavaClassPattern(_*) =>
      case s => fail(s"<$s> did not match $JavaClassPattern")
    }
  }

  it should "reject invalid classes" in {
    Seq("", "@*", "744", "1Dog", "Favorite.Dog", "/what/in/gods/holy/name/are/you/blathering/about") foreach {
      case s@JavaClassPattern(_*) => fail(s"<$s> matched $JavaClassPattern")
      case s =>
    }
  }

  "NonNegativeInteger regexp" should "match valid ints" in {
    Seq("0", "1", "744", "3423") foreach {
      case NonNegativeIntPattern(_*) =>
      case s => fail(s"<$s> did not match $NonNegativeIntPattern")
    }
  }

  it should "reject invalid ints" in {
    Seq("a", "/what/in/gods/holy/name/are/you/blathering/about", "-1", "1.25", "+4", "00") foreach {
      case s@NonNegativeIntPattern(_*) => fail(s"<$s> matched $NonNegativeIntPattern")
      case s =>
    }
  }

  "URL regexp" should "match valid URLs" in {
    Seq("http://my.dog.com", "https://my.dog.com") foreach {
      case UrlPattern(_*) =>
      case s => fail(s"<$s> did not match $UrlPattern")
    }
  }

  it should "reject invalid URLs" in {
    Seq("/my.dog.com", "my.dog.com", "a", "/what/in/gods/holy/name/are/you/blathering/about", "-1", "1.25", "+4") foreach {
      case s@UrlPattern(_*) => fail(s"<$s> matched $UrlPattern")
      case _ =>
    }
  }

  val validVersions = Seq("0.0.0", "0.1.0", "1.0.0", "10.123.987654", "11.0.2")

  "version" should "accept valid versions with numbers" in {
    validVersions foreach {
      case Version(_*) =>
      case s => fail(s"<$s> did not match $Version")
    }
  }

  it should "accept valid versions with leading 'v'" in {
    validVersions.map(v => "v" + v) foreach {
      case Version(_*) =>
      case s => fail(s"<$s> did not match $Version")
    }
  }

  val invalidVersions = Seq("1", "1.0", "0.1.0x", "V1.0.0", "x10.123.987654", "11.0.2~1", "$1.21", "2016/06/19", "20160916")

  it should "reject invalid versions" in {
    invalidVersions foreach {
      case s@Version(_*) => fail(s"<$s> did match $Version")
      case _ =>
    }
  }

  val validPreRelease = Seq("SNAPSHOT", "20161108124123", "20161108T124123Z", "travis23.1", "alpha.42.21.beta", "-A-", "-0-", "-.-", "0beta1")

  it should "accept valid versions with valid pre-release suffixes" in {
    validVersions.flatMap(v => validPreRelease.map(p => v + "-" + p)) foreach {
      case Version(_*) =>
      case s => fail(s"<$s> did not match $Version")
    }
  }

  it should "reject invalid pre-release versions" in {
    val invalidPreReleases = Seq("", ".", "ugh.", ".bug", "01", "alpha.01", "no,commas", "no_underscores", "tragically..hip", "/what/in/gods/holy/name/are/you/blathering/about", "%%%")
    validVersions.flatMap(v => invalidPreReleases.map(p => v + "-" + p))foreach {
      case s@Version(_*) => fail(s"<$s> matched $Version")
      case _ =>
    }
  }

  val validBuildData = Seq("SNAPSHOT", "20161108124123", "20161108T124123Z", "travis23.1", "alpha.42.21.beta", "-A-", "-0-", "-.-", "0beta1", "000")

  it should "accept valid versions with valid build suffixes" in {
    validVersions.flatMap(v => validBuildData.map(b => v + "+" + b)) foreach {
      case Version(_*) =>
      case s => fail(s"<$s> did not match $Version")
    }
  }

  it should "accept valid versions with valid pre-release and build suffixes" in {
    validVersions.flatMap(v => validPreRelease.flatMap(p => validBuildData.map(b => v + "-" + p + "+" + b))) foreach {
      case Version(_*) =>
      case s => fail(s"<$s> did not match $Version")
    }
  }

  it should "reject versions with invalid build metadata" in {
    val invalidBuildData = Seq("", ".", "ugh.", ".bug", "no,commas", "no_underscores", "tragically..hip", "/what/in/gods/holy/name/are/you/blathering/about", "%%%")
    validVersions.flatMap(v => invalidBuildData.map(b => v + "+" + b)) foreach {
      case s@Version(_*) => fail(s"<$s> matched $Version")
      case _ =>
    }
  }

  "UUID" should "accept valid UUID" in {
    java.util.UUID.randomUUID().toString match {
      case UuidPattern(_*) =>
      case s => fail(s"<$s> did not match $UuidPattern")
    }
  }
  
  it should "reject invalid UUIDs" in {
    Seq("", "   ", "4234213423-234213412-2134123423") foreach {
      case s@UuidPattern(_*) => fail(s"<$s> should not have matched $UuidPattern")
      case s =>
    }
  }
}

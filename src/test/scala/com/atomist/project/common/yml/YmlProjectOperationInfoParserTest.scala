package com.atomist.project.common.yml

import com.atomist.tree.project.SimpleResourceSpecifier
import com.atomist.param.ParameterValidationPatterns
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._

object YmlProjectOperationInfoParserTest {

  val noParameters =
    """
      |name:
      |    test1
      |
      |description:
      |   descr1
    """.stripMargin

  val valid1 =
    """
      |name:
      |    test1
      |
      |description:
      |    descr1
      |
      |group:
      |    atomist
      |
      |version:
      |    1.0
      |
      |parameters:
      |
      |  - name: foo
      |    required: true
      |
      |  - name: bar
      |    required: false
      |
      |tags:
      |  - name: t1
      |    description: whatever
    """.stripMargin

  val bogusRegexp =
    """
      |name:
      |    test1
      |
      |description:
      |    descr1
      |
      |group:
      |    atomist
      |
      |version:
      |    1.0
      |
      |parameters:
      |
      |  - name: foo
      |    required: true
      |    pattern: (xxxxxxx
      |
    """.stripMargin

  def withOverrides(paramName: String, description: String, required: Boolean, pattern: String, default: String, validInputDescription: String) =
    s"""
       |name:
       |    test1
       |
       |description:
       |   descr1
       |
       |parameters:
       |
       |  - name: $paramName
       |    required: $required
       |    description: $description
       |    default_value: $default
       |    pattern: $pattern
       |    valid_input_description: $validInputDescription
       |
       |tags:
       |
       | - name: spring
       |   description: Spring Framework
       |
       | - name: spring-boot
       |   description: Spring Boot
       |
  """.stripMargin

  val invalid1 =

    """
      |namxe:
      |    test1
      |
      |description:
      |   descr1
      |
      |parameters:
      |    foo:
      |       required: true
      |    bar:
      |       required: false
    """.stripMargin
}

class YmlProjectOperationInfoParserTest extends FlatSpec with Matchers {

  import YmlProjectOperationInfoParserTest._

  it should "reject empty content" in {
    an[InvalidYmlDescriptorException] should be thrownBy {
      YmlProjectOperationInfoParser.parse("")
    }
  }

  it should "reject content without name" in {
    an[InvalidYmlDescriptorException] should be thrownBy {
      YmlProjectOperationInfoParser.parse(invalid1)
    }
  }

  it should "reject content with bogus regexp" in {
    an[InvalidYmlDescriptorException] should be thrownBy {
      YmlProjectOperationInfoParser.parse(bogusRegexp)
    }
  }

  it should "return empty parameters if no parameters specified" in {
    val poi = YmlProjectOperationInfoParser.parse(noParameters)
    poi.parameters.isEmpty should be(true)
  }

  it should "return empty tags if no tags specified" in {
    val poi = YmlProjectOperationInfoParser.parse(noParameters)
    poi.tags.isEmpty should be(true)
  }

  it should "return no GAV if version and group not specified" in {
    val poi = YmlProjectOperationInfoParser.parse(noParameters)
    poi.gav.isDefined should be(false)
  }

  it should "return GAV if version and group specified" in {
    val poi = YmlProjectOperationInfoParser.parse(valid1)
    poi.gav.isDefined should be(true)
    poi.gav.get should equal(SimpleResourceSpecifier("atomist", "test1", "1.0"))
  }

  it should "find name in valid content" in {
    val poi = YmlProjectOperationInfoParser.parse(valid1)
    poi.name should equal("test1")
    poi.description should equal("descr1")
    poi.parameters.size should equal(2)
  }

  it should "find parameters in valid content using default values" in {
    val poi = YmlProjectOperationInfoParser.parse(valid1)
    poi.name should equal("test1")
    poi.description should equal("descr1")
    poi.parameters.size should equal(2)
    poi.parameters(0).getName should equal("foo")
    poi.parameters(0).isRequired should equal(true)
    poi.parameters(1).getName should equal("bar")
    poi.parameters(1).isRequired should equal(false)
    poi.parameters(1).getPattern should equal(ParameterValidationPatterns.MatchAll)
  }

  it should "honor parameter overrides without strange characters" in {
    checkWithOverrides("name1", "description is good", required = true, ".+", "default_val", "You should input something")
  }

  it should "honor parameter overrides with strange characters" in {
    checkWithOverrides("name1", "description is good", required = true, ".+", "default_val", "You should've input something")
  }

  it should "respect tags" in {
    val poi = checkWithOverrides("name1", "description is good", required = true, ".+", "default_val", "You should've input something")
    poi.tags.size should equal(2)
    poi.tags(0).name should equal("spring")
    poi.tags(0).description should equal("Spring Framework")
    poi.tags(1).name should equal("spring-boot")
    poi.tags(1).description should equal("Spring Boot")
  }

  it should "accept multiline description" is pending

  private def checkWithOverrides(name: String, description: String, required: Boolean, pattern: String, default: String, validInputDescription: String) = {
    val yml = withOverrides(name, description, required, pattern, default, validInputDescription)
    val poi = YmlProjectOperationInfoParser.parse(yml)
    val p = poi.parameters.head
    p.getName should equal(name)
    p.getDescription should equal(description)
    p.isRequired should equal(required)
    p.getPattern should equal(pattern)
    p.getDefaultValue should equal(default)
    p.getValidInputDescription should equal(validInputDescription)
    poi
  }

  it should "parse YML with problematic pattern correctly escaped" in {
    val pattern = // "tommy"
      """"[a-zA-Z_$][a-zA-Z\\d_$]*""""
    val yml =
      s"""
         |name:
         |  flask-rest-service
         |type:
         |  python-flask
         |
         |description:
         |  Python Flask REST microservice
         |
         |parameters:
         |
         |  - name: package
         |    required: true
         |    description: The package name
         |    pattern: $pattern
         |    valid_input_description: A name for this service
      """.stripMargin
    val parsed = YmlProjectOperationInfoParser.parse(yml)
    parsed.parameters.head.getPattern should equal("""[a-zA-Z_$][a-zA-Z\d_$]*""")
  }
}

package com.atomist.project.common.yml

import com.atomist.tree.content.project.SimpleResourceSpecifier
import com.atomist.param.ParameterValidationPatterns
import org.scalatest.{FlatSpec, Matchers}

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
    assert(poi.parameters.isEmpty === true)
  }

  it should "return empty tags if no tags specified" in {
    val poi = YmlProjectOperationInfoParser.parse(noParameters)
    assert(poi.tags.isEmpty === true)
  }

  it should "return no GAV if version and group not specified" in {
    val poi = YmlProjectOperationInfoParser.parse(noParameters)
    assert(poi.gav.isDefined === false)
  }

  it should "return GAV if version and group specified" in {
    val poi = YmlProjectOperationInfoParser.parse(valid1)
    assert(poi.gav.isDefined === true)
    assert(poi.gav.get === SimpleResourceSpecifier("atomist", "test1", "1.0"))
  }

  it should "find name in valid content" in {
    val poi = YmlProjectOperationInfoParser.parse(valid1)
    assert(poi.name === "test1")
    assert(poi.description === "descr1")
    assert(poi.parameters.size === 2)
  }

  it should "find parameters in valid content using default values" in {
    val poi = YmlProjectOperationInfoParser.parse(valid1)
    assert(poi.name === "test1")
    assert(poi.description === "descr1")
    assert(poi.parameters.size === 2)
    assert(poi.parameters(0).getName === "foo")
    assert(poi.parameters(0).isRequired === true)
    assert(poi.parameters(1).getName === "bar")
    assert(poi.parameters(1).isRequired === false)
    assert(poi.parameters(1).getPattern === ParameterValidationPatterns.MatchAny)
  }

  it should "honor parameter overrides without strange characters" in {
    checkWithOverrides("name1", "description is good", required = true, ".+", "default_val", "You should input something")
  }

  it should "honor parameter overrides with strange characters" in {
    checkWithOverrides("name1", "description is good", required = true, ".+", "default_val", "You should've input something")
  }

  it should "respect tags" in {
    val poi = checkWithOverrides("name1", "description is good", required = true, ".+", "default_val", "You should've input something")
    assert(poi.tags.size === 2)
    assert(poi.tags(0).name === "spring")
    assert(poi.tags(0).description === "Spring Framework")
    assert(poi.tags(1).name === "spring-boot")
    assert(poi.tags(1).description === "Spring Boot")
  }

  it should "accept multiline description" is pending

  private  def checkWithOverrides(name: String, description: String, required: Boolean, pattern: String, default: String, validInputDescription: String) = {
    val yml = withOverrides(name, description, required, pattern, default, validInputDescription)
    val poi = YmlProjectOperationInfoParser.parse(yml)
    val p = poi.parameters.head
    assert(p.getName === name)
    assert(p.getDescription === description)
    assert(p.isRequired === required)
    assert(p.getPattern === pattern)
    assert(p.getDefaultValue === default)
    assert(p.getValidInputDescription === validInputDescription)
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
    assert(parsed.parameters.head.getPattern === """[a-zA-Z_$][a-zA-Z\d_$]*""")
  }
}

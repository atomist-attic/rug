package com.atomist.rug.parser

object DefaultIdentifierResolver extends IdentifierResolver {

  import com.atomist.param.ParameterValidationPatterns._

  val knownIds = Map(
    "artifact_id" -> ArtifactId,
    "group_id" -> GroupName,
    "java_class" -> JavaClass,
    "java_identifier" -> JavaIdentifier,
    "java_package" -> JavaPackage,
    "project_name" -> ProjectName,
    "port" -> Port,
    "ruby_class" -> "[A-Z][A-Za-z0-9_]*",
    "ruby_identifier" -> "[A-Za-z_][A-Za-z0-9_]*",
    "semantic_version" -> Version,
    "url" -> Url,
    "uuid" -> Uuid
  )

  override def resolve(identifier: String): Either[SourceDescription, String] =
     knownIds.get(identifier) match {
       case Some(value) => Right(value)
       case None => Left(s"the hard-coded map in ${getClass.getSimpleName}")
     }
}

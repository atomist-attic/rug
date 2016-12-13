package com.atomist.util.lang

import com.github.javaparser.ASTHelper
import com.github.javaparser.ast.{CompilationUnit, ImportDeclaration}
import com.github.javaparser.ast.`type`.{ClassOrInterfaceType, ReferenceType, VoidType, PrimitiveType => JavaParserPrimitiveType}
import com.github.javaparser.ast.body._
import com.github.javaparser.ast.expr._
import com.github.javaparser.ast.stmt.{BlockStmt, ReturnStmt, Statement, ThrowStmt}

import scala.collection.JavaConverters._

/**
  * Utility methods to simplify working with JavaParser.
  */
object JavaParserUtils {

  import scala.language.implicitConversions

  // From https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
  val reservedWords = Set(
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while"
  )

  // Make calling JavaParser methods less verbose
  implicit def stringToNameExpr(s: String): NameExpr = ASTHelper.createNameExpr(s)

  // TODO why doesn't a structural type seem to work here?
  def getAnnotation(bd: BodyDeclaration, name: String): Option[AnnotationExpr] =
    bd.getAnnotations.asScala.find(_.getName.getName equals name)

  def getAnnotationValueAsString(an: AnnotationExpr): Option[String] = {
    an match {
      case s: SingleMemberAnnotationExpr => s.getMemberValue match {
        case sl: StringLiteralExpr => Some(sl.getValue)
      }
      // TODO fragile
      case mm: NormalAnnotationExpr => Some(mm.getPairs.get(0).getValue.asInstanceOf[StringLiteralExpr].getValue)

      case _ => None
    }
  }

  def getAnnotation(p: Parameter, name: String): Option[AnnotationExpr] =
    p.getAnnotations.asScala.find(a => a.getName.getName equals name)

  import scala.language.reflectiveCalls

  // Structural type makes this work across method declarations and types
  def isPublic(modifiable: {def getModifiers: Int}) = ModifierSet.isPublic(modifiable.getModifiers)

  def getUnderlyingType(rt: ReferenceType): ClassOrInterfaceType =
    rt.getType match {
      case cit: ClassOrInterfaceType => cit
      case _ => ???
    }

  // Return a new throw new UnsupportedOperationException: Default method body
  // Sadly in Java we don't have Scala ???
  def implementMeBlock(message: String): BlockStmt = {
    val block = new BlockStmt()
    val arg = new StringLiteralExpr(message)
    val newUoe = new ObjectCreationExpr(null,
      new ClassOrInterfaceType("UnsupportedOperationException"), Seq(arg.asInstanceOf[Expression]).asJava)
    val throws = new ThrowStmt(newUoe)
    ASTHelper.addStmt(block, throws)
    block
  }

  def thisDotFieldEqualsField(fieldName: String): BlockStmt = {
    val block = new BlockStmt()
    val assign = new AssignExpr(s"this.$fieldName", fieldName, AssignExpr.Operator.assign)
    ASTHelper.addStmt(block, assign)
    block
  }

  def returnThisDotField(fieldName: String): BlockStmt = {
    val block = new BlockStmt()
    val returnStmt = new ReturnStmt(s"this.$fieldName")
    ASTHelper.addStmt(block, returnStmt)
    block
  }

  def isBeanGetter(md: MethodDeclaration): Boolean = {
    ModifierSet.isPublic(md.getModifiers) &&
      (md.getName.startsWith("get") || md.getName.startsWith("is")) &&
      md.getParameters.isEmpty && !md.getType.isInstanceOf[VoidType]
  }

  /**
    * Return Some if the method is a simple getter of a field,
    * such as "return age;"
    *
    * @param md the MethodDeclaration
    * @return field name if it's a simple getter
    */
  def simpleGetterOfField(md: MethodDeclaration): Option[String] = {
    val statements: List[Statement] = md.getBody.getStmts.asScala.toList
    statements match {
      case (r: ReturnStmt) :: Nil =>
        r.getChildrenNodes.asScala.toList match {
          case (n: NameExpr) :: Nil =>
            Some(n.getName)
          case _ => None
        }
      case _ => None
    }
  }

  /**
    * Is this a data member? If so, return the name that should be used for it.
    */
  def nonAnnotatedDataMemberWithName(bd: BodyDeclaration): Option[String] = bd match {
    case md: MethodDeclaration if isBeanGetter(md) && !shouldIgnoreAsCustomTypeMember(bd) =>
      val exposedNameFromMethod = JavaHelpers.getterNameToPropertyName(md.getName)
      Some(exposedNameFromMethod)
    case fd: FieldDeclaration if isPublicField(fd) && !shouldIgnoreAsCustomTypeMember(bd) =>
      Some(fd.getVariables.asScala.head.getId.getName)
    case _ => None
  }

  def annotatedDataMemberWithName(bd: BodyDeclaration): Option[String] = bd match {
    case md: MethodDeclaration if shouldExposeAsCustomTypeMember(md) =>
      val exposedNameFromMethod = JavaHelpers.getterNameToPropertyName(md.getName)
      Some(dataModelNameFromAnnotation(bd).getOrElse(exposedNameFromMethod))
    case fd: FieldDeclaration
      if shouldExposeAsCustomTypeMember(fd) =>
      Some(dataModelNameFromAnnotation(bd).getOrElse(fd.getVariables.asScala.head.getId.getName))
    case _ => None
  }

  // TODO handle non JSON annotations if we support any
  private def dataModelNameFromAnnotation(bd: BodyDeclaration): Option[String] = {
    bd.getAnnotations.asScala.find(a => JsonPropertyAnnotationName.equals(a.getName.getName))
      .flatMap(a => getAnnotationValueAsString(a))
  }

  def isPublicField(f: FieldDeclaration): Boolean = {
    ModifierSet.isPublic(f.getModifiers) && !ModifierSet.isStatic(f.getModifiers) &&
      f.getVariables.size == 1
  }

  def isReservedWord(name: String): Boolean = {
    name != null && name.length > 0 && reservedWords.contains(name)
  }

  /**
    * Annotations that cause a potential custom type field or property to be ignored.
    */
  private val IgnoreDataFieldAnnotations = Set(
    "JsonIgnore",
    "Transient"
  )

  val JsonPropertyAnnotationName = "JsonProperty"

  /**
    * Annotations that cause even a private field or method to be exposed.
    */
  private val ExposeDataFieldAnnotations = Set(
    JsonPropertyAnnotationName
  )

  def shouldIgnoreAsCustomTypeMember(bd: BodyDeclaration): Boolean =
    bd.getAnnotations.asScala.exists(a => IgnoreDataFieldAnnotations.contains(a.getName.getName))

  def shouldExposeAsCustomTypeMember(bd: BodyDeclaration): Boolean =
    bd.getAnnotations.asScala.exists(a => ExposeDataFieldAnnotations.contains(a.getName.getName))

  def addImportsIfNeeded(fqns: Seq[String], cu: CompilationUnit): Unit = {
    fqns.foreach(fqn => {
      val importDefinition = cu.getImports.asScala.find(imp => imp.toString contains fqn)
      if (importDefinition.isEmpty) {
        cu.getImports.add(new ImportDeclaration(new NameExpr(fqn), false, false))
      }
    })
  }
}

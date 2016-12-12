package com.atomist.rug.kind.elm

import com.atomist.tree.content.text.{AbstractMutableContainerTreeNode, MutableTerminalTreeNode}
import com.atomist.rug.kind.elm.ElmModel.ElmDeclarationModels._
import com.atomist.rug.kind.elm.ElmModel.ElmExpressionModels._
import com.atomist.rug.kind.elm.ElmModel.ElmTypeModels._
import com.atomist.rug.kind.elm.ElmModel._
import com.atomist.util.scalaparsing.CommonTypesParser
import com.atomist.source.StringFileArtifact
import com.typesafe.scalalogging.LazyLogging

private[elm] object ElmParserCombinator
  extends CommonTypesParser
    with LazyLogging {

  import ElmTokens._

  val ElmMultilineComment = """\{\-([^*]|[\r\n|\n]|(\*+([^*/]|[\r\n|\n])))*\-+\}"""

  val DashDashLineComment = """\-\-.*[\r\n|\n]"""

  // this set is actually dynamic but we can just hard-code the ones we know about for now
  val ElmInfixOperators: Seq[Parser[String]] = Seq("++", "+", "!", "-", "//", "/", "*", "==", "::", "<<", ">>")

  val ElmCommentsWhiteSpace =
    ("""(\s|""" + DashDashLineComment + "|" + ElmMultilineComment + ")+").r

  val CommaThatMightGoLeft = opt(NewLineWithLessIndentation) ~ Comma

  /** Ignore Elm comments */
  override protected val whiteSpace = ElmCommentsWhiteSpace

  // Overridden because the superclass methods is surprisingly expensive
  override def skipWhitespace = true

  /* one step above ElmTokens */
  object ElmIdentifiers {
    private def variableReferenceStructure: Parser[ElmVariableReference] = opt(rep1sep(UppercaseVariableName, ".") <~ ".") ~ localVariableReference ^^ {
      case Some(qualifiers) ~ name => new ElmVariableReference(qualifiers, name)
      case None ~ name => new ElmVariableReference(Seq(), name)
    }

    def localVariableReference: Parser[MutableTerminalTreeNode] = mutableTerminalNode("var-ref", identifierRefString(ReservedWords, VariableName))

    def variableReference: Parser[ElmVariableReference] = positionedStructure(variableReferenceStructure)

    def recordFieldName: Parser[MutableTerminalTreeNode] = mutableTerminalNode("record-label", identifierRefString(ReservedWords, LowercaseVariableName))

    def variableDeclaration: Parser[MutableTerminalTreeNode] = mutableTerminalNode("var", identifierRefString(ReservedWords, LowercaseVariableName))
  }

  import ElmIdentifiers._

  def functionName: Parser[String] = identifierRef(ReservedWords, LowercaseVariableName) ^^ {
    ir: IdentifierRef => ir.name
  }

  def assignableIdentifier: Parser[String] = identifierRefString(ReservedWords, AssignableIdentifier)

  def functionNameAsIdentifierRef: Parser[ElmIdentifierRef] = mutableTerminalNode("function-name", fullyQualifiedFunctionName) ^^ {
    us: MutableTerminalTreeNode => new MutableTerminalTreeNode(us) with ElmIdentifierRef {
      override def id: String = value
    }
  }

  def elmIdentifierRef: Parser[ElmIdentifierRef] = mutableTerminalNode("some-identifier", identifierRefString(ReservedWords, SyntacticallyValidFunctionName)) ^^ {
    us: MutableTerminalTreeNode => new MutableTerminalTreeNode(us) with ElmIdentifierRef {
      override def id: String = value
    }
  }

  def fullyQualifiedFunctionName: Parser[String] = opt(rep1sep(TypeIdentifier, ".") <~ ".") ~ identifierRefString(ReservedWords, SyntacticallyValidFunctionName) ^^ {
    case None ~ last => last
    case Some(pathElements) ~ last =>
      pathElements.mkString(".") + "." + last
  }

  // TODO This probably has some wicked corner cases. This is not sufficient.
  val contentsOfStringToken = ".*".r

  def allExposing: Parser[AllExposing] = mutableTerminalNode("all-exposing", DotDot) ^^ (usf => new MutableTerminalTreeNode(usf.nodeName, usf.value, usf.startPosition) with AllExposing)

  def functionNamesExposing: Parser[FunctionNamesExposing] = rep1sep(localVariableReference, Comma) ^^ {
    fnames: Seq[MutableTerminalTreeNode] => new FunctionNamesExposing(fnames)
  }

  def exposing: Parser[Exposing] = allExposing | positionedStructure(functionNamesExposing)

  def exposings: Parser[Exposing] = ExposingKeywordToken ~> OpenParen ~> exposing <~ CloseParen

  def alias: Parser[String] = AsKeywordToken ~> TypeIdentifier

  def fqn: Parser[String] = rep1sep(TypeIdentifier, ".") ^^ (pathElements => pathElements.mkString("."))

  def importStructure: Parser[Import] = ImportKeywordToken ~> mutableTerminalNode("moduleName", fqn) ~ opt(alias) ~ opt(exposings) ^^ {
    case moduleNameField ~ alias ~ exposings => Import(moduleNameField, alias = alias, exposing = exposings)
  }

  def elmImport: Parser[Import] = NewLineAtTheVeryLeft ~> positionedStructure(importStructure)

  case class ModuleHeader(moduleName: MutableTerminalTreeNode, exposing: Exposing)

  def elmModuleHeader: Parser[ModuleHeader] = NewLineAtTheVeryLeft ~ opt(PortKeywordToken) ~ ModuleKeywordToken ~> mutableTerminalNode("module", TypeIdentifier) ~
    exposings ^^ {
    case moduleName ~ exposed => ModuleHeader(moduleName, exposed)
  }

  object TypeSpecifications {

    // TODO parentheses can be around these
    private def typeSpecificationStructure: Parser[ElmTypeSpecification] = rep1sep(typeSpecificationThatMightBePartOfAFunctionType, Arrow) ^^ {
      case Seq(one) => one
      case more => ElmFunctionType(more)
    }

    def typeSpecification: Parser[ElmTypeSpecification] = positionedStructure(typeSpecificationStructure)

    // rod, you might want to rename this one
    def typeSpecificationThatMightBePartOfAFunctionType: Parser[ElmTypeSpecification] =
      positionedStructure(tupleType) | typeWithParameters | positionedStructure(recordType) | typeParameterVariable

    def tupleType: Parser[ElmTupleType] = OpenParen ~> repsep(typeSpecification, CommaThatMightGoLeft) <~ CloseParen ^^ (elements => ElmTupleType(elements))

    def typeParameterVariable: Parser[ElmTypeParameterVariable] = mutableTerminalNode("type-variable", ElmTokens.TypeParameterIdentifier) ^^ {
      us: MutableTerminalTreeNode => new ElmTypeParameterVariable(us)
    }

    private def typeWithParametersStructure: Parser[ElmTypeWithParameters] =
      mutableTerminalNode("type-name", fqn) ~ rep(typeSpecification) ^^ {
        case name ~ params => ElmTypeWithParameters(typeNameField = name, parameters = params)
      }

    def typeWithParameters: Parser[ElmTypeWithParameters] = positionedStructure(typeWithParametersStructure)

    private def recordTypeStructure: Parser[ElmRecordType] = "{" ~> repsep(recordFieldType, ",") <~ "}" ^^ (fields => ElmRecordType(fields))

    def recordType: Parser[ElmRecordType] = positionedStructure(recordTypeStructure)

    private def recordFieldTypeStructure: Parser[ElmRecordFieldType] =
      recordFieldName ~ Colon ~ typeSpecification ^^ {
        case name ~ _ ~ value => ElmRecordFieldType(name, value)
      }

    def recordFieldType: Parser[ElmRecordFieldType] = positionedStructure(recordFieldTypeStructure)
  }

  import TypeSpecifications.{typeSpecification, typeWithParameters}

  object ElmExpressions {

    def expression: Parser[ElmExpression] = positionedStructure(infixExpression) | expressionExceptInfix

    def parentheticalExpression: Parser[ElmExpression] = OpenParen ~> expression <~ opt(NewLineWithLessIndentation) ~ CloseParen

    def expressionExceptInfix: Parser[ElmExpression] =
        parentheticalExpression |
        positionedStructure(tuple) |
        positionedStructure(let) |
        stringConstant | intConstant | listLiteral |
        RecordExpressions.recordSuchThat |
        positionedStructure(RecordExpressions.record) |
        positionedStructure(ifExpression) |
        caseExpression |
        recordFieldAccess |
        anonymousFunction |
        typicalFunctionApplication

    def elseExpression: Parser[ElmExpression] = opt(NewLineWithLessIndentation) ~ "else" ~> expression

    def ifExpression: Parser[ElmIf] =
      "if" ~ condition ~ "then" ~ expression ~ elseExpression ^^ {
        case _ ~ condition ~ _ ~ action ~ elseO =>
          new ElmIf(condition, action)
      }

    def condition: Parser[ElmCondition] = expressionExceptInfix ~ comparisonOperator ~ expressionExceptInfix ^^ {
      case l ~ op ~ r => new ElmCondition(l, op, r)
    }

    // TODO I don't know what others there are
    def comparisonOperator: Parser[String] = "=="

    // there's a oneOf method inherited that seems like it would do exactly this but it doesn't work
    def oneOfThese(these: Seq[Parser[String]]): Parser[String] = these.reduce(_ | _)

    private def infixExpressionStructure: Parser[ElmInfixFunctionApplication] = expressionExceptInfix ~ infixOperator ~ expression ^^ {
      case left ~ op ~ right => ElmInfixFunctionApplication(left, op, right)
    }

    def infixOperator: Parser[ElmInfixOperator] = mutableTerminalNode("infix-operator", oneOfThese(ElmInfixOperators)) ^^ {
      usfv: MutableTerminalTreeNode => new MutableTerminalTreeNode(usfv) with ElmInfixOperator {
        def id = value
      }
    }

    def infixExpression: Parser[ElmInfixFunctionApplication] = positionedStructure(infixExpressionStructure)

    def stringConstant: Parser[StringConstant] = "\"" ~> mutableTerminalNode("string-content", doubleQuotedStringContent) <~ "\"" ^^ {
      uf => new MutableTerminalTreeNode(uf) with StringConstant {
        override def s: String = value
      }
    }

    import ElmPatterns.elmPattern

    private def anonymousFunctionStructure: Parser[ElmAnonymousFunction] = "\\" ~> rep1(elmPattern) ~ "->" ~ expression ^^ {
      case params ~ arrow ~ body => new ElmAnonymousFunction(params, body)
    }

    def anonymousFunction: Parser[ElmAnonymousFunction] = positionedStructure(anonymousFunctionStructure)

    def tupleDeconstruction: Parser[String] = OpenParen ~ rep1sep(assignableIdentifier, Comma) ~ CloseParen ^^ {
      case s ~ t ~ r => t.mkString(s, ",", r)
    }

    import ElmFunctionDeclaration.elmFunctionDeclaration

    def thingsThatCanBeInTheAssignmentBlockOfALetExpression: Parser[ElmDeclaration] = ElmConstantDeclaration.elmConstantDeclaration | elmFunctionDeclaration

    def let: Parser[ElmLetExpression] = LetKeywordToken ~> rep1sep(thingsThatCanBeInTheAssignmentBlockOfALetExpression, NewLineWithLessIndentation) ~ NewLineWithLessIndentation ~ InKeywordToken ~ expression ^^ {
      case declarations ~ _ ~ _ ~ body => ElmLetExpression(declarations, body)
    }

    def intConstant: Parser[IntConstant] = mutableTerminalNode("int-constant", "[0-9]+".r) ^^ {
      uf => new MutableTerminalTreeNode(uf) with IntConstant {
        override def i: Int = value.toInt
      }
    }

    private def listElementSeparator: Parser[String] = opt(NewLineWithLessIndentation) ~> ","

    private def listLiteralStructure: Parser[ListLiteral] = "[" ~> repsep(expression, listElementSeparator) <~ opt(NewLineWithLessIndentation) ~ "]" ^^ (elements => ListLiteral(elements))

    def listLiteral: Parser[ListLiteral] = positionedStructure(listLiteralStructure)

    def tuple: Parser[ElmTuple] = OpenParen ~> repsep(expression, CommaThatMightGoLeft) <~ CloseParen ^^ (elements => ElmTuple(elements))

    private def caseExpressionStructure: Parser[ElmCase] = CaseKeywordToken ~> expression ~ OfKeywordToken ~ rep1sep(caseClause, NewLineWithLessIndentation) ^^ {
      case matchOn ~ _ ~ clauses => new ElmCase(matchOn, clauses)
    }

    def caseExpression: Parser[ElmCase] = positionedStructure(caseExpressionStructure)

    import ElmPatterns.elmPattern

    private def caseClauseStructure: Parser[ElmCaseClause] = elmPattern ~ Arrow ~ expression ^^ {
      case l ~ _ ~ r => new ElmCaseClause(l, r)
    }

    def caseClause: Parser[ElmCaseClause] = positionedStructure(caseClauseStructure)

    object RecordExpressions {

      def fieldStructure: Parser[ElmRecordField] = recordFieldName ~ EqualsToken ~ expression ^^ {
        case name ~ _ ~ value => new ElmRecordField(name, value)
      }

      def field: Parser[ElmRecordField] = positionedStructure(fieldStructure)

      private def recordElementSeparator: Parser[String] = opt(NewLineWithLessIndentation) ~> ","

      def record: Parser[ElmRecord] = "{" ~> repsep(field, recordElementSeparator) <~ opt(NewLineWithLessIndentation) ~ "}" ^^ (fields => new ElmRecord(fields))

      private def recordSuchThatStructure: Parser[ElmRecordUpdate] = "{" ~> functionName ~ "|" ~ rep1sep(field, CommaThatMightGoLeft) <~ opt(NewLineWithLessIndentation) ~ "}" ^^ {
        case startingRecord ~ _ ~ changedFields => ElmRecordUpdate(startingRecord, changedFields)
      }

      def recordSuchThat: Parser[ElmRecordUpdate] = positionedStructure(recordSuchThatStructure)
    }

    private def typicalFunctionApplicationStructure: Parser[ElmFunctionApplication] = ElmIdentifiers.variableReference ~ rep(expression) ^^ {
      case fqname ~ params => new ElmFunctionApplication(fqname, params)
    }

    def typicalFunctionApplication: Parser[ElmFunctionApplication] = positionedStructure(typicalFunctionApplicationStructure)

    private def recordFieldAccessStructure: Parser[ElmRecordFieldAccess] = ElmIdentifiers.variableReference ~ "." ~ rep1sep(ElmIdentifiers.recordFieldName, ".") ^^ {
      case record ~ _ ~ fields =>
        new ElmRecordFieldAccess(record, fields)
    }

    def recordFieldAccess: Parser[ElmRecordFieldAccess] = positionedStructure(recordFieldAccessStructure)
  }

  import ElmExpressions.expression
  import ElmPatterns.elmPattern

  object ElmFunctionDeclaration {
    private def functionNameInTypeDeclaration = mutableTerminalNode("functionNameFromTypeDeclaration", functionName)

    private def functionNameInBodyDeclaration = mutableTerminalNode("functionName", functionName)

    private def functionDeclarationStructure: Parser[ElmFunction] =
      opt(functionNameInTypeDeclaration ~ Colon ~ typeSpecification <~ NewLineAtTheVeryLeft) ~
        functionNameInBodyDeclaration ~ rep1(elmPattern) ~ EqualsToken ~ expression ^^ {
        case Some(nameFromTypeDecl ~ Colon ~ functionType) ~ name2 ~ params ~ EqualsToken ~ body =>
          new ElmFunction(name2, body, params, Some(nameFromTypeDecl, functionType))
        case None ~ name2 ~ params ~ EqualsToken ~ body =>
          new ElmFunction(name2, body, params, None)
      }

    // Q: what can we do to check that name1 == name2? throw exception?
    def elmFunctionDeclaration: Parser[ElmFunction] = positionedStructure(functionDeclarationStructure)
  }

  object ElmConstantDeclaration {

    def elmConstantDeclaration: Parser[ElmDeclaration] =
      positionedStructure(typedConstantDeclaration) |
        positionedStructure(simpleConstantDeclaration) |
        positionedStructure(patternedConstantDeclaration)

    private def constantNameInTypeDeclaration = ElmIdentifiers.variableDeclaration

    private def constantNameInBodyDeclaration = ElmIdentifiers.variableDeclaration

    private def typedConstantDeclaration: Parser[ElmSimpleConstantDeclaration] =
      constantNameInTypeDeclaration ~ Colon ~ typeSpecification ~ NewLineAtTheVeryLeft ~
        constantNameInBodyDeclaration ~ EqualsToken ~ expression ^^ {
        case nameFromTypeDecl ~ Colon ~ constantType ~ NewLineAtTheVeryLeft ~ name2 ~ EqualsToken ~ body =>
          new ElmSimpleConstantDeclaration(name2, body, Some(nameFromTypeDecl, constantType))
      }

    private def simpleConstantDeclaration: Parser[ElmSimpleConstantDeclaration] =
      constantNameInBodyDeclaration ~ EqualsToken ~ expression ^^ {
        case name2 ~ EqualsToken ~ body =>
          new ElmSimpleConstantDeclaration(name2, body, None)
      }

    private def patternedConstantDeclaration = (elmPattern ~ EqualsToken ~ expression) ^^ {
      case pattern ~ "=" ~ expression => new ElmPatternedConstantDeclaration(pattern, expression)
    }
  }
  private def elmPortDeclarationStructure = PortKeywordToken ~> mutableTerminalNode("portName", functionName) ~ Colon ~ typeSpecification ^^ {
    case name ~ _ ~ typeSpec => new ElmPortDeclaration(name, typeSpec)
  }

  def elmPortDeclaration: Parser[ElmDeclaration] = positionedStructure(elmPortDeclarationStructure)

  def elmTypeAliasStructure: Parser[ElmTypeAlias] = TypeKeywordToken ~ AliasToken ~>
    mutableTerminalNode("alias-name", TypeIdentifier) ~ EqualsToken ~ typeSpecification ^^ {
    case name ~ _ ~ t => new ElmTypeAlias(name, t)
  }

  def elmTypeAlias: Parser[ElmTypeAlias] = positionedStructure(elmTypeAliasStructure)

  def constructors: Parser[Seq[ElmTypeWithParameters]] =
    rep1sep(typeWithParameters, "|")

  def elmUnionType: Parser[ElmUnionType] =
    positionedStructure(TypeKeywordToken ~> TypeIdentifier ~ EqualsToken ~ constructors ^^ {
      case name ~ _ ~ options => new ElmUnionType(name, options)
    })

  import ElmConstantDeclaration.elmConstantDeclaration
  import ElmFunctionDeclaration.elmFunctionDeclaration

  def elmDeclaration: Parser[ElmDeclaration] = NewLineAtTheVeryLeft ~> (elmConstantDeclaration | elmFunctionDeclaration | elmPortDeclaration | elmTypeAlias | elmUnionType)

  private def elmModuleStructure(rawSource: String): Parser[ElmModule] = elmModuleHeader ~ rep(elmImport) ~ rep(elmDeclaration) ^^ {
    case moduleHeader ~ imports ~ fns =>
      new ElmModule(rawSource, nameField = moduleHeader.moduleName, initialExposing = moduleHeader.exposing,
        imports, declarations = fns)
  }

  private def elmModule(rawSource: String): Parser[ElmModule] = positionedStructure(elmModuleStructure(rawSource), topLevel = true)

  def parse(markedUpInput: String): ElmModule =
    parseTo(StringFileArtifact("<input>", markedUpInput), phrase(elmModule(markedUpInput) <~ eof))

  /**
    * Useful to test individual productions.
    */
  private[elm] def parseProduction[T](parser: Parser[T], input: String): T =
  parse(parser, input) match {
    case Success(matched, _) =>
      matched match {
        case soo: AbstractMutableContainerTreeNode => soo.pad(input)
        case _ =>
      }
      matched
    case Failure(msg, _) => throw new IllegalArgumentException(s"Failure: $msg\nInput: $input")
    case Error(msg, _) => throw new IllegalArgumentException(s"Error: $msg")
  }

  object ElmPatterns {

    def elmPattern: Parser[ElmPattern] = tuplePattern | patternInParens | recordPattern | unionTypeDeconstruction | simplePattern | discardTheThing

    private def simplePattern: Parser[ElmPattern] = ElmIdentifiers.variableDeclaration ^^ {
      s => MatchAnythingAndNameIt(s.value)
    }

    private def tuplePattern: Parser[ElmPattern] = OpenParen ~> rep1sep(elmPattern, Comma) <~ CloseParen ^^ {
      patterns: Seq[ElmPattern] => new TuplePattern(patterns)
    }

    private def recordPattern: Parser[ElmPattern] = OpenCurly ~> rep1sep(ElmIdentifiers.recordFieldName, Comma) <~ CloseCurly ^^ {
      s: Seq[MutableTerminalTreeNode] => new RecordPattern(s)
    }

    private def patternInParens: Parser[ElmPattern] = OpenParen ~> elmPattern <~ CloseParen

    private def unionTypeDeconstruction: Parser[UnionDeconstruction] = fqn ~ rep(elmPattern) ^^ {
      case constructor ~ parameters => UnionDeconstruction(constructor, parameters)
    }

    private def discardTheThing: Parser[ElmPattern] = "_".r ^^ {
      case underscore => MatchAnything
    }
  }
}
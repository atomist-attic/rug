grammar Rug;

rug_file
    : ( comment* annotation* rug+ )+ EOF
    ;

rug
    : type name ( annotation | comment | uses | precondition | let| param | with | editor_call )*
    ;

type
    : 'generator' | 'editor' | 'reviewer' | 'predicate'
    ;

precondition
    : 'predicondition' name
    ;

name
    : IDENTIFIER
    ;

editor_call
    : IDENTIFIER editor_call_arg*
    ;

editor_call_arg
    : editor_call_arg_name '=' editor_call_arg_value
    ;

editor_call_arg_name
    : IDENTIFIER
    ;

editor_call_arg_value
    : IDENTIFIER | STRING
    ;

annotation
    : annotation_name annotation_value?
    ;

annotation_name
    : ANNOTATION_NAME
    ;

annotation_value
    : INT | STRING
    ;

param
    : 'param' param_name ':' param_value
    ;

param_name
    : IDENTIFIER
    ;

param_value
    : STRING
    | ( '@any' | '@artifact_id' | '@group_id' | '@java_class' |
        '@java_identifier' | '@java_package' | '@project_name' |
        '@port' | '@ruby_class' | '@ruby_identifier' |
        '@semantic_version' | '@version_range' | '@url' | '@uuid' )
    | PARAM_PATTERN
    ;

uses
    : 'uses' other_rug
    ;

other_rug
    : ( ( group_id '.' artifact_id '.' editor_name ) | editor_name )
    ;

editor_name
    : IDENTIFIER
    ;

group_id
    : IDENTIFIER
    ;

artifact_id
    : IDENTIFIER
    ;

let
    : 'let' let_name '=' let_value
    ;

let_name
    : IDENTIFIER
    ;

let_value
    : STRING
    ;

with_type
    : ( file_type | project_type )
    ;

file_type
    : 'File' type_instance?
    ;

project_type
    : 'Project' type_instance?
    ;

rug_extension
    : IDENTIFIER
    ;

with
    : 'with' ( with_type | rug_extension ) type_instance? when? begin? comment* do_op* with* end?
    ;

when
    : 'when' ( type_instance '.' )? instance_property ( '.' property_method )? '='? ( STRING | IDENTIFIER )
    | 'when' JS_BLOCK
    ;

type_instance
    : IDENTIFIER
    ;

instance_property
    : IDENTIFIER
    ;

property_method
    : IDENTIFIER
    ;

do_op
    : 'do' function_call
    ;

function_call
    : function_name function_param*
    ;

function_name
    : ( IDENTIFIER | type_instance '.' instance_property ( '.' property_method )? )
    ;

function_param
    : ( IDENTIFIER | STRING | JS_BLOCK )
    ;

begin
    : 'begin' comment* do_op*
    ;

end
    : 'end'
    ;

comment
    : ( PY_COMMENT | C_COMMENT )
    ;

ANNOTATION_NAME: '@tag' | '@displayName' | '@validInput' | '@default' |
                 '@defaultRef' | '@generator' | '@maxLength' | '@minLength' |
                 '@description' | '@optional' | '@hide';
STRING: SINGLE_LINE_STRING | MULTI_LINE_STRING;
SINGLE_LINE_STRING: '"' (~'"')* '"';
MULTI_LINE_STRING: '"""' (~'"')* '"""';
JS_BLOCK: '{' .*? '}';
PY_COMMENT: '#' ~[\r\n]*;
C_COMMENT
     :   ( '//' ~[\r\n]* '\r'? '\n'
         | '/*' .*? '*/'
         )
     ;
fragment DIGIT: [0-9];
INT: DIGIT+;
IDENTIFIER : [a-zA-Z0-9_-]+;
PARAM_PATTERN: '^' .*? '$';
WS : ( '\t' | ' ' | '\r' | '\n' )+ -> skip;
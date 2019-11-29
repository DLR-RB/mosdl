// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0

grammar MOSDL;

area        : doc? AREA nvidentifier
              (typeImport)*
              (service | areaLevelDataType | error)*
              EOF;

typeImport  : IMPORT fullyQualifiedType ;

service     : doc? SERVICE nidentifier LBRACE (capability | operation | dataType | error)* RBRACE ;
capability  : doc? CAPABILITY (LBRACK NUMBER RBRACK)? LBRACE operation* RBRACE ;

operation   : doc? (sendOp | submitOp | requestOp | invokeOp | progressOp | pubsubOp) ;
sendOp      : SEND STAR? nidentifier sendMsg ;
submitOp    : SUBMIT STAR? nidentifier submitMsg (THROWS opErrors)? ;
requestOp   : REQUEST STAR? nidentifier requestMsg requestResponseMsg (THROWS opErrors)? ;
invokeOp    : INVOKE STAR? nidentifier invokeMsg invokeAckMsg invokeResponseMsg (THROWS opErrors)? ;
progressOp  : PROGRESS STAR? nidentifier progressMsg progressAckMsg progressUpdateMsg progressResponseMsg (THROWS opErrors)? ;
pubsubOp    : PUBSUB STAR? nidentifier pubsubMsg (THROWS opErrors)? ;

sendMsg             : cmsg;
submitMsg           : cmsg;
requestMsg          : cmsg;
requestResponseMsg  : pmsg;
invokeMsg           : cmsg;
invokeAckMsg        : pmsg;
invokeResponseMsg   : pmsg;
progressMsg         : cmsg;
progressAckMsg      : pmsg;
progressUpdateMsg   : pmsg STAR;
progressResponseMsg : pmsg;
pubsubMsg           : bmsg;

cmsg        : doc? parameters ;
pmsg        : doc? RARR parameters ;
bmsg        : doc? LARR parameters ;
parameters  : LPAREN (field (COMMA? field)*)? RPAREN ;

opErrors    : (error | errorRef) (COMMA (error | errorRef))*;
errorRef    : doc? nonNullableType (COLON extraInfo)? ;

areaLevelDataType : attribute
                  | fundamental
                  | dataType ;
attribute         : doc? ATTRIBUTE nidentifier ;
fundamental       : doc? FUNDAMENTAL ID (EXTENDS type)? ;

dataType    : composite
            | enumeration ;

composite   : doc? (ABSTRACT COMPOSITE ID | COMPOSITE nidentifier (EXTENDS type)?) LBRACE field* RBRACE ;
field       : doc? ID COLON nullableType;

enumeration : doc? ENUM nidentifier LBRACE enumItem (COMMA? enumItem)* RBRACE ;
enumItem    : doc? nidentifier ;

error       : doc? ERROR nidentifier (COLON extraInfo)? ;
extraInfo   : doc? nonNullableType ;

nullableType       : type QUEST? (LT nullableType GT)? ;
nonNullableType    : type (LT nonNullableType GT)? ;
type               : simpleType
                   | qualifiedType
                   | fullyQualifiedType;
simpleType         : ID ;
qualifiedType      : ID DOT ID ;
fullyQualifiedType : ID DBLCOLON (ID DOT)? ID ;
nidentifier        : ID (LBRACK NUMBER RBRACK)? ;
nvidentifier       : ID (LBRACK (NUMBER | DOT NUMBER | NUMBER DOT NUMBER) RBRACK)? ;
doc                : DOC
                   | LINE_DOC ;

AREA        : 'area' ;
SERVICE     : 'service' ;
COMPOSITE   : 'composite' ;
ENUM        : 'enum' ;
ATTRIBUTE   : 'attribute' ;
FUNDAMENTAL : 'fundamental' ;
ERROR       : 'error' ;
EXTENDS     : 'extends' ;
IMPORT      : 'import' ;
THROWS      : 'throws' ;
ABSTRACT    : 'abstract' ;
CAPABILITY  : 'capability' ;
SEND        : 'send' ;
SUBMIT      : 'submit' ;
REQUEST     : 'request' ;
INVOKE      : 'invoke' ;
PROGRESS    : 'progress' ;
PUBSUB      : 'pubsub' ;

DOC      : TRPLQUOT .*? TRPLQUOT ;
LINE_DOC : TRPLSLASH .*? '\r'?'\n';
ID       : LETTER (LETTER | '0'..'9')*
         | '"' LETTER (LETTER | '0'..'9')* '"' ;
NUMBER   : '0' [xX] [0-9a-fA-F]+
         | [0-9]+ ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACE   : '{' ;
RBRACE   : '}' ;
LBRACK   : '[' ;
RBRACK   : ']' ;
LT       : '<' ;
GT       : '>' ;
QUEST    : '?' ;
STAR     : '*' ;
DOT      : '.' ;
COMMA    : ',' ;
COLON    : ':' ;
DBLCOLON : '::' ;
RARR     : '->' ;
LARR     : '<-' ;
WS           : [ \r\t\n]+    -> skip ;
COMMENT      : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;

fragment LETTER    : [a-zA-Z\u0080-\u00FF_] ;
fragment TRPLQUOT  : '"""' ;
fragment TRPLSLASH : '///' ;

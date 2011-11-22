grammar ContextExpression;

options {
  language = Java;
}

@header {
package interdroid.contextdroid.contextexpressions;

import java.util.Map;
import java.util.HashMap;
import java.util.Stack;
}

@lexer::header {
package interdroid.contextdroid.contextexpressions;
}

@members {
public static final Expression parseExpression(final String expression) throws ExpressionParseException {
        if (expression == null || expression.trim().length() == 0)
            return null;

        CharStream stream = new ANTLRStringStream(expression);
        ContextExpressionLexer lexer = new ContextExpressionLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        ContextExpressionParser parser = new ContextExpressionParser(tokenStream);
        try {
            return parser.expression() /* .expression */ ;
        } catch (RecognitionException e) {
            throw new ExpressionParseException(e);
        }
}

public static final TypedValue parseTypedValue(final String expression) throws ExpressionParseException {
        if (expression == null || expression.trim().length() == 0)
            return null;

        CharStream stream = new ANTLRStringStream(expression);
        ContextExpressionLexer lexer = new ContextExpressionLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        ContextExpressionParser parser = new ContextExpressionParser(tokenStream);
        try {
            return parser.typed_value() /* .typed_value */ ;
        } catch (RecognitionException e) {
        	throw new ExpressionParseException(e);
        }
}

}

// Simple rules

configuration_options returns [Map<String, String> configuration]
@init {
	HashMap<String, String> config = new HashMap<String, String>();
}
	:	
	(id=ID val=CONFIG_VAL) 
		{config.put($id.getText(), $val.getText().substring(1));}
	(CONFIG_AND more_id=ID more_val=CONFIG_VAL 
		{config.put($more_id.getText(), $more_val.getText().substring(1));}
	)*
		{$configuration = config;}
	;

value_path	returns [String value_path]
@init {
	StringBuffer buf = new StringBuffer();
}
	:
	id=ID 
		{buf.append($id.getText());}
	('.' more_id=ID 
		{buf.append('.'); buf.append($more_id.getText());} 
	)*
		{$value_path = buf.toString();}
	;

// Various enumeration values

strategy	returns [Strategy strategy]
	:	ALL {$strategy = Strategy.ALL;}
	|	ANY {$strategy = Strategy.ANY;}
	;

comparator	returns [Comparator comparator]
	:	GT {$comparator = Comparator.GREATER_THAN;}
	|	LT {$comparator = Comparator.LESS_THAN;}
	|	GTEQ {$comparator = Comparator.GREATER_THAN_OR_EQUALS;}
	|	LTEQ {$comparator = Comparator.LESS_THAN_OR_EQUALS;}
	|	EQUALS {$comparator = Comparator.EQUALS;}
	|	NOTEQUALS {$comparator = Comparator.NOT_EQUALS;}
	|	REGEX {$comparator = Comparator.REGEX_MATCH;}
	|	CONTAINS {$comparator = Comparator.STRING_CONTAINS;}
	;

logic_operator returns [LogicOperator logic_operator]
	:	
		binary=binary_logic_operator 
			{$logic_operator = binary /* .logic_operator */ ;}
	|	unary=unary_logic_operator 
			{$logic_operator = unary /* .logic_operator */ ;}
	;

binary_logic_operator	returns [LogicOperator logic_operator]
	:	AND {$logic_operator = LogicOperator.AND;}
	|	OR {$logic_operator = LogicOperator.OR;}
	;

unary_logic_operator	returns [LogicOperator logic_operator]
	:	NOT {$logic_operator = LogicOperator.NOT;}
	;

math_operator	returns [MathOperator math_operator]
	: add=additive_math_operator {$math_operator=add /* .math_operator */ ;}
	| mult=multiplicative_math_operator {$math_operator=mult /* .math_operator */ ;}
	;

additive_math_operator returns [MathOperator math_operator]
	:	PLUS {$math_operator = MathOperator.PLUS;}
	|	MINUS {$math_operator = MathOperator.MINUS;}
	;
	
multiplicative_math_operator returns [MathOperator math_operator]
	:	MULT {$math_operator = MathOperator.TIMES;}
	|	DIV {$math_operator = MathOperator.DIVIDE;}
	|	MOD {$math_operator = MathOperator.MOD;}
	;

history_mode returns [HistoryReductionMode history_mode]
	:	NONE {$history_mode = HistoryReductionMode.NONE;}
	|	MAX {$history_mode = HistoryReductionMode.MAX;}
	|	MIN {$history_mode = HistoryReductionMode.MIN;}
	|	MEAN {$history_mode = HistoryReductionMode.MEAN;}
	|	MEDIAN {$history_mode = HistoryReductionMode.MEDIAN;}
	;


// Paser rules
context_typed_value returns [ContextTypedValue typed_value]
	:	entity=ID ':' path=value_path
			{$typed_value = new ContextTypedValue(entity.getText(), path /*.value_path */);}
	|	entity=ID ':' path=value_path '?' config=configuration_options
			{$typed_value = new ContextTypedValue(entity.getText(), path /* .value_path */, config /*.configuration */);}
	|	entity=ID ':' path=value_path '{' mode=history_mode ',' time=INT '}'
			{$typed_value = new ContextTypedValue(entity.getText(), path /* .value_path */, mode /*.history_mode */, Long.parseLong(time.getText()));}
	|	entity=ID ':' path=value_path '?' config=configuration_options '{' mode=history_mode ',' time=INT '}'
			{$typed_value = new ContextTypedValue(entity.getText(), path /* .value_path */ , config /*.configuration */ , mode /* .history_mode */ , Long.parseLong(time.getText()));}
	;

constant_typed_value returns [ConstantTypedValue typed_value]
	:	i=INT 
			{$typed_value = new ConstantTypedValue(Long.parseLong($i.getText()));}
	|	f=FLOAT 
			{$typed_value = new ConstantTypedValue(Double.parseDouble($f.getText()));}
	| 	raw=STRING 
			{$typed_value = new ConstantTypedValue($raw.getText());}
	;

typed_value	returns [TypedValue typed_value]
	:	constant=constant_typed_value 
			{$typed_value = constant /*.typed_value */ ;}
	| 	context=context_typed_value 
			{$typed_value = context /*.typed_value */ ;}
	;

parentheticalExpression returns [Expression expression]
	:	val=typed_value 
			{$expression = new TypedValueExpression(val /* .typed_value */ );}
	|	'(' WS* exp=orExpression WS* ')' 
			{$expression = exp /* .expression */ ;}
	;

comparativeExpression returns [Expression expression]
@init {
	Stack<Expression> rightStack = new Stack<Expression>();
	Stack<Strategy> strategyStack = new Stack<Strategy>();
	Stack<Comparator> compareStack = new Stack<Comparator>();
}
	: left=additiveExpression
	((WS* s=strategy)? WS* c=comparator WS* right=additiveExpression 
		{strategyStack.push(s /* .strategy */ ); compareStack.push(c /* .comparator */ ); rightStack.push(right /* .expression */ );}
	)?
{
	while(rightStack.size() > 1) {
		Expression temp = rightStack.pop();
		rightStack.push(new ComparisonExpression(rightStack.pop(), compareStack.pop(), strategyStack.pop(), temp));
	}
	if (rightStack.size() > 0) {
		$expression = new ComparisonExpression(left /* .expression */ , compareStack.pop(), strategyStack.pop(), rightStack.pop());
	} else {
		$expression = left /* .expression */ ;
	}
}
	;

multiplicativeExpression returns [Expression expression]
@init {
	Stack<Expression> rightStack = new Stack<Expression>();
	Stack<MathOperator> opStack = new Stack<MathOperator>();
}
	: left=parentheticalExpression
	(WS* op=multiplicative_math_operator WS* right=parentheticalExpression 
		{opStack.push(op /* .math_operator */ ); rightStack.push(right /* .expression */ );}
	)*
{
	while(rightStack.size() > 1) {
		Expression temp = rightStack.pop();
		rightStack.push(new MathExpression(rightStack.pop(), opStack.pop(), temp));
	}
	if (rightStack.size() > 0) {
		$expression = new MathExpression(left /* .expression */ , opStack.pop(), rightStack.pop());
	} else {
		$expression = left /* .expression */ ;
	}
}
	;

additiveExpression returns [Expression expression]
@init {
	Stack<Expression> rightStack = new Stack<Expression>();
	Stack<MathOperator> opStack = new Stack<MathOperator>();
}
	: left=multiplicativeExpression
	(op=additive_math_operator right=multiplicativeExpression 
		{opStack.push(op /* .math_operator */ ); rightStack.push(right /* .expression */ );}
	)*
{
	while(rightStack.size() > 1) {
		Expression temp = rightStack.pop();
		rightStack.push(new MathExpression(rightStack.pop(), opStack.pop(), temp));
	}
	if (rightStack.size() > 0) {
		$expression = new MathExpression(left /* .expression */ , opStack.pop(), rightStack.pop());
	} else {
		$expression = left /* .expression */ ;
	}
}
	;

unaryExpression returns [Expression expression]
	: op=unary_logic_operator exp=comparativeExpression 
		{$expression = new LogicExpression(op /* .logic_operator */ , exp /* .expression */ );}
	| exp=comparativeExpression 
		{$expression = exp /* .expression */ ;}
	;

andExpression returns [Expression expression]
@init {
	Stack<Expression> rightStack = new Stack<Expression>();
}
	: left=unaryExpression
	(AND right=unaryExpression 
		{rightStack.push(right /* .expression */ );}
	)*
{
	while(rightStack.size() > 1) {
		Expression temp = rightStack.pop();
		rightStack.push(new LogicExpression(rightStack.pop(), LogicOperator.AND, temp));
	}
	if (rightStack.size() > 0) {
		$expression = new LogicExpression(left /* .expression */ , LogicOperator.AND, rightStack.pop());
	} else {
		$expression = left /* .expression */ ;
	}
}
	;

orExpression returns [Expression expression]
@init {
	Stack<Expression> rightStack = new Stack<Expression>();
}
	: left=andExpression
	(OR right=andExpression 
		{rightStack.push(right /* .expression */ );}
	)*
{
	while(rightStack.size() > 1) {
		Expression temp = rightStack.pop();
		rightStack.push(new LogicExpression(rightStack.pop(), LogicOperator.OR, temp));
	}
	if (rightStack.size() > 0) {
		$expression = new LogicExpression(left /* .expression */ , LogicOperator.OR, rightStack.pop());
	} else {
		$expression = left /* .expression */ ;
	}
}
	;

expression returns [Expression expression]
	:	
	logic=orExpression EOF
		{$expression = logic /* .expression */ ;}
	;

// Lexar rules
// Binary
OR    :     '||' | 'or' | 'OR';
AND   :     '&&' | 'and' | 'AND';
// Unary
NOT   :    '!' | 'not' | 'NOT';

// Config
CONFIG_IS
	:	'=';
CONFIG_AND
	:	'&';

// Comparators
EQUALS
      :    '==' | '=';
NOTEQUALS
      :    '!=' | '<>';
LT    :    '<';
LTEQ  :    '<=';
GT    :    '>';
GTEQ  :    '>=';
PLUS  :    '+';
MINUS :    '-';
MULT  :    '*';
DIV   :    '/';
MOD   :    '%';
REGEX :
		'regex' | 'REGEX';
CONTAINS
	:	 'contains' | 'CONTAINS';

// Strategies
ALL 	:	('ALL'|'all');
ANY	:	('ANY'|'any');

// History Reduction
NONE 	:	('NONE'|'none');
MAX	:	('MAX'|'max');
MIN	:	('MIN'|'min');
MEAN	: 	('MEAN'|'mean');
MEDIAN 	:	('MEDIAN'|'median');

ID  :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
    ;

INT :	('-')? '0'..'9'+
    ;

FLOAT
    :   ('-')? ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   ('-')? '.' ('0'..'9')+ EXPONENT?
    |   ('-')? ('0'..'9')+ EXPONENT
    ;

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) {$channel=HIDDEN;}
    ;

STRING          
@init{StringBuilder lBuf = new StringBuilder();}
    :
    '\'' 
	( ESC_SEQ
		{lBuf.append(getText());} 
	| normal=~('\''|'\\')
		{lBuf.appendCodePoint(normal);} )* 
	'\''     
		{setText(lBuf.toString());}
    ;

fragment
ESC_SEQ
    :   '\\'
        (       'n'    {setText("\n");}
        |       'r'    {setText("\r");}
        |       't'    {setText("\t");}
        |       'b'    {setText("\b");}
        |       'f'    {setText("\f");}
        |       '"'    {setText("\"");}
        |       '\''   {setText("\'");}
        |       '/'    {setText("/");}
        |       '\\'   {setText("\\");}
        |       ('u')+ i=HEX_DIGIT j=HEX_DIGIT k=HEX_DIGIT l=HEX_DIGIT   {setText(String.valueOf((char) Integer.parseInt(i.getText() + j.getText() + k.getText() + l.getText(), 16)));}
        )
    ;

CONFIG_VAL
    :	'=' (STRING 
    		{
    		/* String uses setText which drops the '='. Put it back so it is the same as the other branch. */ 
    		setText("=" + getText());
    		}
    	| ('a'..'z'|'A'..'Z'|'0'..'9'|'.')*)
    ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

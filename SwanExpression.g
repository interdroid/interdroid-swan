grammar SwanExpression;

options {
  language = Java;
}

@header {
package interdroid.swan.swansong;

import android.os.Bundle;
import java.util.Stack;
}

@lexer::header {
package interdroid.swan.swansong;
}

@members {
public static final Expression parseExpression(final String expression) throws ExpressionParseException {
        if (expression == null || expression.trim().length() == 0)
            return null;

        CharStream stream = new ANTLRStringStream(expression);
        SwanExpressionLexer lexer = new SwanExpressionLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        SwanExpressionParser parser = new SwanExpressionParser(tokenStream);
        try {
            return parser.expression() /* .expression */ ;
        } catch (RecognitionException e) {
            throw new ExpressionParseException(e);
        }
}

public static final long convertTime(Token time, Token unit) {
	long unitFactor = 1;
	if (unit != null) {
		String unitText = unit.getText();
		if (unitText.equals("h") || unitText.equals("H")) {
			unitFactor = 60 * 60 * 1000;
		} else if (unitText.equals("m") || unitText.equals("M")) {
			unitFactor = 60 * 1000;
		} else if (unitText.equals("s") || unitText.equals("S")) {
			unitFactor = 1000;
		}
	}
	return Long.parseLong(time.getText()) * unitFactor;
}

}

// Simple rules

configuration_options returns [Bundle configuration]
@init {
	Bundle config = new Bundle();
}
	:	
	(id=ID val=CONFIG_VAL) 
		{config.putString($id.getText(), $val.getText().substring(1));}
	(CONFIG_AND more_id=ID more_val=CONFIG_VAL 
		{config.putString($more_id.getText(), $more_val.getText().substring(1));}
	)*
		{$configuration = config;}
	;

value_path	returns [String value_path]
@init {
	StringBuffer buf = new StringBuffer();
}
	:
	vp=v_p 
		{buf.append(vp);}
	('.' more_id=v_p 
		{buf.append('.'); buf.append(vp);} 
	)*
		{$value_path = buf.toString();}
	;

v_p		returns [String vp]
	:	id=ID	{ $vp = $id.getText(); }
	|	str=STRING { $vp = $str.getText(); }
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

binary_logic_operator	returns [BinaryLogicOperator logic_operator]
	:	AND {$logic_operator = BinaryLogicOperator.AND;}
	|	OR {$logic_operator = BinaryLogicOperator.OR;}
	;

unary_logic_operator	returns [UnaryLogicOperator logic_operator]
	:	NOT {$logic_operator = UnaryLogicOperator.NOT;}
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
	:	ALL {$history_mode = HistoryReductionMode.ALL;}
	|	MAX {$history_mode = HistoryReductionMode.MAX;}
	|	MIN {$history_mode = HistoryReductionMode.MIN;}
	|	MEAN {$history_mode = HistoryReductionMode.MEAN;}
	|	MEDIAN {$history_mode = HistoryReductionMode.MEDIAN;}
	|	ANY {$history_mode = HistoryReductionMode.ANY;}
	;


// Parser rules
sensor_value_expression returns [SensorValueExpression value_expression]
	:	location=ID '@' entity=ID ':' path=value_path
			{$value_expression = new SensorValueExpression(location.getText(), entity.getText(), path /*.value_path */, null, HistoryReductionMode.ANY, 0);}
	|	location=ID '@' entity=ID ':' path=value_path '?' config=configuration_options
			{$value_expression = new SensorValueExpression(location.getText(), entity.getText(), path /*.value_path */, config /*.configuration */, HistoryReductionMode.ANY, 0);}
	|	location=ID '@' entity=ID ':' path=value_path '{' ((mode=history_mode ',' time=time_value) | mode=history_mode | time=time_value) '}'
			{if (time == null) {
				$value_expression = new SensorValueExpression(location.getText(), entity.getText(), path /* .value_path */, null, mode /*.history_mode */, 0);
			} else {
				$value_expression = new SensorValueExpression(location.getText(), entity.getText(), path /* .value_path */, null, mode /*.history_mode */, time);
			}}
	|	location=ID '@' entity=ID ':' path=value_path '?' config=configuration_options '{' ((mode=history_mode ',' time=time_value) | mode=history_mode | time=time_value) '}'
			{if (time == null) {
				$value_expression = new SensorValueExpression(location.getText(), entity.getText(), path /* .value_path */ , config /*.configuration */ , mode /* .history_mode */, 0);
			} else {
				$value_expression = new SensorValueExpression(location.getText(), entity.getText(), path /* .value_path */ , config /*.configuration */ , mode /* .history_mode */ , time);
			}}
	;

constant_value_expression returns [ConstantValueExpression value_expression]
	:	i=INT 
			{$value_expression = new ConstantValueExpression(Long.parseLong($i.getText()));}
	|	f=FLOAT 
			{$value_expression = new ConstantValueExpression(Double.parseDouble($f.getText()));}
	| 	raw=STRING 
			{$value_expression = new ConstantValueExpression($raw.getText());}
	;

value_expression returns [ValueExpression value_expression]
	:	constant=constant_value_expression 
			{$value_expression = constant /* value expression */ ;}
	| 	sensor=sensor_value_expression 
			{$value_expression = sensor /* value expression */ ;}
	;
	
parentheticalExpression returns [Expression expression]
    :   val=value_expression 
            {$expression = val;}
    |   '(' WS* exp=orExpression WS* ')' 
            {$expression = exp /* .expression */ ;}
    ;
    
multiplicativeExpression returns [Expression expression]
@init {
    Stack<ValueExpression> rightStack = new Stack<ValueExpression>();
    Stack<MathOperator> opStack = new Stack<MathOperator>();
    Stack<String> locationStack = new Stack<String>();
}
    : left=parentheticalExpression
    (WS* (location=ID '@')? op=multiplicative_math_operator WS* right=parentheticalExpression 
        {locationStack.push(location == null ? Expression.LOCATION_INFER : location.getText()); opStack.push(op /* .math_operator */ ); rightStack.push((ValueExpression) right /* .expression */ );}
    )*
{
    while(rightStack.size() > 1) {
        ValueExpression temp = rightStack.pop();
        rightStack.push(new MathValueExpression(locationStack.pop(), rightStack.pop(), opStack.pop(), temp, HistoryReductionMode.DEFAULT_MODE));
    }
    if (rightStack.size() > 0) {
        $expression = new MathValueExpression(locationStack.pop(), (ValueExpression) left /* .expression */ , opStack.pop(), rightStack.pop(), HistoryReductionMode.DEFAULT_MODE);
    } else {
        $expression = left /* .expression */ ;
    }
}
    ;

additiveExpression returns [Expression expression]
@init {
    Stack<ValueExpression> rightStack = new Stack<ValueExpression>();
    Stack<MathOperator> opStack = new Stack<MathOperator>();
    Stack<String> locationStack = new Stack<String>();
}
    : left=multiplicativeExpression
    ((location=ID '@')? op=additive_math_operator right=multiplicativeExpression 
        {locationStack.push(location == null ? Expression.LOCATION_INFER : location.getText()); opStack.push(op /* .math_operator */ ); rightStack.push((ValueExpression) right /* .expression */ );}
    )*
{
    while(rightStack.size() > 1) {
        ValueExpression temp = rightStack.pop();
        rightStack.push(new MathValueExpression(locationStack.pop(), rightStack.pop(), opStack.pop(), temp, HistoryReductionMode.DEFAULT_MODE));
    }
    if (rightStack.size() > 0) {
        $expression = new MathValueExpression(locationStack.pop(), (ValueExpression) left /* .expression */ , opStack.pop(), rightStack.pop(), HistoryReductionMode.DEFAULT_MODE);
    } else {
        $expression = left /* .expression */ ;
    }
}
    ;

comparativeExpression returns [Expression expression]
@init {
    Stack<Expression> rightStack = new Stack<Expression>();
    Stack<Comparator> compareStack = new Stack<Comparator>();
    Stack<String> locationStack = new Stack<String>();
}
    : left=additiveExpression
    (WS* ((location=ID)? c=comparator) WS* right=additiveExpression 
        {locationStack.push(location == null ? Expression.LOCATION_INFER : location.getText()); compareStack.push(c /* .comparator */ ); rightStack.push(right /* .expression */ );}
    )?
{
    while(rightStack.size() > 1) {
        Expression temp = rightStack.pop();
        rightStack.push(new ComparisonExpression(locationStack.pop(), (ValueExpression) rightStack.pop(), compareStack.pop(), (ValueExpression) temp));
    }
    if (rightStack.size() > 0) {
        $expression = new ComparisonExpression(locationStack.pop(), (ValueExpression) left /* .expression */ , compareStack.pop(), (ValueExpression) rightStack.pop());
    } else {
        $expression = left /* .expression */ ;
    }
}
    ;

unaryExpression returns [Expression expression]
    : (location=ID)? NOT exp=comparativeExpression 
        {$expression = new LogicExpression(location == null ? Expression.LOCATION_INFER : location.getText(), UnaryLogicOperator.NOT /* .logic_operator */ , (TriStateExpression) exp /* .expression */ );}
    | exp=comparativeExpression 
        {$expression = exp /* .expression */ ;}
    ;

andExpression returns [Expression expression]
@init {
    Stack<Expression> rightStack = new Stack<Expression>();
    Stack<String> locationStack = new Stack<String>();
}
    : left=unaryExpression
    ((location=ID '@')? AND right=unaryExpression 
        {locationStack.push(location == null ? Expression.LOCATION_INFER : location.getText()); rightStack.push(right /* .expression */ );}
    )*
{
    while(rightStack.size() > 1) {
        Expression temp = rightStack.pop();
        rightStack.push(new LogicExpression(locationStack.pop(), (TriStateExpression) rightStack.pop(), BinaryLogicOperator.AND, (TriStateExpression) temp));
    }
    if (rightStack.size() > 0) {
        $expression = new LogicExpression(locationStack.pop(), (TriStateExpression) left /* .expression */ , BinaryLogicOperator.AND, (TriStateExpression) rightStack.pop());
    } else {
        $expression = left /* .expression */ ;
    }
}
    ;

orExpression returns [Expression expression]
@init {
    Stack<Expression> rightStack = new Stack<Expression>();
    Stack<String> locationStack = new Stack<String>();
}
    : left=andExpression
    ((location=ID '@')? OR right=andExpression 
        {locationStack.push(location == null ? Expression.LOCATION_INFER : location.getText()); rightStack.push(right /* .expression */ );}
    )*
{
    while(rightStack.size() > 1) {
        Expression temp = rightStack.pop();
        rightStack.push(new LogicExpression(locationStack.pop(), (TriStateExpression) rightStack.pop(), BinaryLogicOperator.OR, (TriStateExpression) temp));
    }
    if (rightStack.size() > 0) {
        $expression = new LogicExpression(locationStack.pop(), (TriStateExpression) left /* .expression */ , BinaryLogicOperator.OR, (TriStateExpression) rightStack.pop());
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

// Time
time_value returns [Long time]
    :
    val=INT unit=TIME_UNIT? 
    { long theTime = convertTime(val, unit); }
    (rep_val=INT rep_unit=TIME_UNIT? 
    { theTime += convertTime(rep_val, rep_unit);}
    )*
    {$time = new Long(theTime);}
    ;

// Lexer rules

TIME_UNIT 
    :   'h'|'H'|'m'|'M'|'s'|'S'|'ms';

// Binary
OR    :     '||' | 'or' | 'OR';
AND   :     '&&' | 'and' | 'AND';
// Unary
NOT   :    '!' | 'not' | 'NOT';

// Config
CONFIG_IS
    :   '=';
CONFIG_AND
    :   '&';

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
    :    'contains' | 'CONTAINS';

// Strategies
ALL     :   ('ALL'|'all');
ANY :   ('ANY'|'any');

// History Reduction
NONE    :   ('NONE'|'none');
MAX :   ('MAX'|'max');
MIN :   ('MIN'|'min');
MEAN    :   ('MEAN'|'mean');
MEDIAN  :   ('MEDIAN'|'median');

ID  :   ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
    ;

INT :   ('-')? '0'..'9'+
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
        {lBuf.appendCodePoint(normal);}
    )* 
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
    :   '=' (STRING 
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
	
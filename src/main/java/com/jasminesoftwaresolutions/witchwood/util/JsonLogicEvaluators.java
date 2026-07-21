package com.jasminesoftwaresolutions.witchwood.util;

import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicExpression;
import io.github.jamsesso.jsonlogic.evaluator.expressions.*;

import java.util.ArrayList;
import java.util.List;

public class JsonLogicEvaluators {
    private static final List<JsonLogicExpression> ALL_EXPRESSIONS = new ArrayList<>() {{
        add(MathExpression.ADD);
        add(MathExpression.SUBTRACT);
        add(MathExpression.MULTIPLY);
        add(MathExpression.DIVIDE);
        add(MathExpression.MODULO);
        add(MathExpression.MIN);
        add(MathExpression.MAX);
        add(NumericComparisonExpression.GT);
        add(NumericComparisonExpression.GTE);
        add(NumericComparisonExpression.LT);
        add(NumericComparisonExpression.LTE);
        add(IfExpression.IF);
        add(IfExpression.TERNARY);
        add(EqualityExpression.INSTANCE);
        add(InequalityExpression.INSTANCE);
        add(StrictEqualityExpression.INSTANCE);
        add(StrictInequalityExpression.INSTANCE);
        add(NotExpression.SINGLE);
        add(NotExpression.DOUBLE);
        add(LogicExpression.AND);
        add(LogicExpression.OR);
        add(LogExpression.STDOUT);
        add(MapExpression.INSTANCE);
        add(FilterExpression.INSTANCE);
        add(ReduceExpression.INSTANCE);
        add(AllExpression.INSTANCE);
        add(ArrayHasExpression.SOME);
        add(ArrayHasExpression.NONE);
        add(MergeExpression.INSTANCE);
        add(InExpression.INSTANCE);
        add(ConcatenateExpression.INSTANCE);
        add(SubstringExpression.INSTANCE);
        add(MissingExpression.ALL);
        add(MissingExpression.SOME);
    }};

    private static final JsonLogicEvaluator INSTANCE = new JsonLogicEvaluator(ALL_EXPRESSIONS);

    public static JsonLogicEvaluator withAllExpressions() {
        return INSTANCE;
    }
}

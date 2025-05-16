package team.idealstate.sugar.next.calculate.test;

import org.junit.jupiter.api.Test;
import team.idealstate.sugar.next.calculate.Expression;
import team.idealstate.sugar.next.calculate.exception.ExpressionException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionTest {

    @Test
    public void test() {
        Expression expression = new Expression("1 + 2 * 3");
        assertEquals(7, expression.calculate().intValue());
        expression = new Expression("(1 + 2) * 3");
        assertEquals(9, expression.calculate().intValue());
        expression = new Expression("(1 + 2 * (2 + 6)) * 3");
        assertEquals(51, expression.calculate().intValue());
        expression = new Expression("((1 + 2 * (2 + 6)) * 3)");
        assertEquals(51, expression.calculate().intValue());
        expression = new Expression("(1 + 2 * (2 + 6)) * (2 + 1)");
        assertEquals(51, expression.calculate().intValue());
        expression = new Expression("1 + 2 * 4 / 2");
        assertEquals(5, expression.calculate().intValue());
        expression = new Expression("1 + 2 * (4 / 2)");
        assertEquals(5, expression.calculate().intValue());
        expression = new Expression("1+2*(4 / 2) + 3");
        assertEquals(8, expression.calculate().intValue());
        expression = new Expression("1 == 1");
        assertTrue(expression.isTrue());
        expression = new Expression("1 == 2");
        assertTrue(expression.isFalse());
        expression = new Expression("1 >= 1");
        assertTrue(expression.isTrue());
        expression = new Expression("1 <= 1");
        assertTrue(expression.isTrue());
        expression = new Expression("1 >= 2");
        assertTrue(expression.isFalse());
        expression = new Expression("1 <= 2");
        assertTrue(expression.isTrue());
        expression = new Expression("1 > 2");
        assertTrue(expression.isFalse());
        expression = new Expression("1 < 2");
        assertTrue(expression.isTrue());
        expression = new Expression("1 + 2 == 1 * 3");
        assertTrue(expression.isTrue());
        expression = new Expression("a + 2 * 3");
        assertThrows(ExpressionException.class, expression::calculate);
        Map<String, Number> context = new HashMap<>();
        context.put("a", 1);
        context.put("b", 2);
        context.put("c", 3);
        expression = new Expression("a + 2 * 3");
        assertEquals(7, expression.calculate(context).intValue());
        expression = new Expression("(a + 2) * c");
        assertEquals(9, expression.calculate(context).intValue());
        expression = new Expression("(a + 2 * (b + 6)) * c");
        assertEquals(51, expression.calculate(context).intValue());
        expression = new Expression("((a + 2 * (b + 6)) * c)");
        assertEquals(51, expression.calculate(context).intValue());
        expression = new Expression("(a + 2 * (b + 6)) * (b + a)");
        assertEquals(51, expression.calculate(context).intValue());
        expression = new Expression("a + b * 4 / 2");
        assertEquals(5, expression.calculate(context).intValue());
        expression = new Expression("a + 2 * (4 / b)");
        assertEquals(5, expression.calculate(context).intValue());
        expression = new Expression("a+b*(4 / 2) + c");
        assertEquals(8, expression.calculate(context).intValue());
        expression = new Expression("a == 1");
        assertTrue(expression.isTrue(context));
        expression = new Expression("a == b");
        assertTrue(expression.isFalse(context));
        expression = new Expression("a >= 1");
        assertTrue(expression.isTrue(context));
        expression = new Expression("a <= 1");
        assertTrue(expression.isTrue(context));
        expression = new Expression("a >= b");
        assertTrue(expression.isFalse(context));
        expression = new Expression("a <= b");
        assertTrue(expression.isTrue(context));
        expression = new Expression("a > b");
        assertTrue(expression.isFalse(context));
        expression = new Expression("a < b");
        assertTrue(expression.isTrue(context));
        expression = new Expression("a + b == a * c");
        assertTrue(expression.isTrue(context));
        expression = new Expression("a + 2 * 3");
        Expression finalExpression = expression;
        assertDoesNotThrow(() -> finalExpression.calculate(context));
    }
}

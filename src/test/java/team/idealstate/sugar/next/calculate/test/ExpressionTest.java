/*
 *    Copyright 2025 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.sugar.next.calculate.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import team.idealstate.sugar.next.calculate.Expression;
import team.idealstate.sugar.next.calculate.exception.ExpressionException;

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

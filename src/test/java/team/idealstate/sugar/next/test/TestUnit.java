package team.idealstate.sugar.next.test;

import org.junit.jupiter.api.Test;
import team.idealstate.sugar.next.calculate.Expression;

import static org.junit.jupiter.api.Assertions.*;

public class TestUnit {

    @Test
    public void test0() {
        String input = "(-5 + 3) * 2 - -1 / 4.0";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(-3.75, result, 0.001);
    }

    // 一元运算符测试
    @Test
    public void testLogicalNotOperator() {
        String input = "!0";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "!5";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testBitwiseNotOperator() {
        String input = "~15";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(-16L, result);
        
        input = "~-1";
        expression = new Expression(input);
        result = expression.calculate().longValue();
        assertEquals(0L, result);
    }

    @Test
    public void testPositiveSignOperator() {
        String input = "+5";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(5.0, result, 0.001);
        
        input = "+(-3.14)";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(-3.14, result, 0.001);
    }

    @Test
    public void testMinusSignOperator() {
        String input = "-5";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(-5.0, result, 0.001);
        
        input = "-(-7.5)";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(7.5, result, 0.001);
    }

    // 二元运算符测试
    @Test
    public void testMultiplyOperator() {
        String input = "6.5 * 4";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(26.0, result, 0.001);
        
        input = "-3 * -8";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(24.0, result, 0.001);
    }

    @Test
    public void testDivideOperator() {
        String input = "20 / 4.0";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(5.0, result, 0.001);
        
        input = "-15.5 / -2";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(7.75, result, 0.001);
    }

    @Test
    public void testModuloOperator() {
        String input = "23 % 7";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(2.0, result, 0.001);
        
        input = "-17 % 5";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(-2.0, result, 0.001);
    }

    @Test
    public void testPowerOperator() {
        String input = "2 ** 8";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(256.0, result, 0.001);
        
        input = "4 ** 0.5";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(2.0, result, 0.001);
    }

    @Test
    public void testAddOperator() {
        String input = "15.5 + 23.7";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(39.2, result, 0.001);
        
        input = "-5 + 3";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(-2.0, result, 0.001);
    }

    @Test
    public void testSubtractOperator() {
        String input = "50.25 - 17.75";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(32.5, result, 0.001);
        
        input = "10 - -5";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(15.0, result, 0.001);
    }

    @Test
    public void testLeftShiftOperator() {
        String input = "16 << 2";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(64L, result);
        
        input = "-8 << 1";
        expression = new Expression(input);
        result = expression.calculate().longValue();
        assertEquals(-16L, result);
    }

    @Test
    public void testRightShiftOperator() {
        String input = "16 >> 2";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(4L, result);
        
        input = "-8 >> 1";
        expression = new Expression(input);
        result = expression.calculate().longValue();
        assertEquals(-4L, result);
    }

    @Test
    public void testUnsignedLeftShiftOperator() {
        String input = "16 <<< 2";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(64L, result);
    }

    @Test
    public void testUnsignedRightShiftOperator() {
        String input = "16 >>> 2";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(4L, result);
    }

    @Test
    public void testGreaterThanOperator() {
        String input = "15 > 10";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "5 > 10";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testLessThanOperator() {
        String input = "10 < 15";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "15 < 10";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testGreaterThanOrEqualsOperator() {
        String input = "15 >= 15";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "15 >= 10";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "10 >= 15";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testLessThanOrEqualsOperator() {
        String input = "10 <= 15";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "10 <= 10";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "15 <= 10";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testEqualsOperator() {
        String input = "15 == 15";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "15 == 10";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testNotEqualsOperator() {
        String input = "15 != 10";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
        
        input = "15 != 15";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(0.0, result, 0.001);
    }

    @Test
    public void testBitwiseAndOperator() {
        String input = "12 & 10";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(8L, result);

        input = "15 & 7";
        expression = new Expression(input);
        result = expression.calculate().longValue();
        assertEquals(7, result);
    }

    @Test
    public void testBitwiseXorOperator() {
        String input = "12 ^ 10";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(6L, result);
        
        input = "15 ^ 7";
        expression = new Expression(input);
        result = expression.calculate().longValue();
        assertEquals(8L, result);
    }

    @Test
    public void testBitwiseOrOperator() {
        String input = "12 | 10";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(14L, result);
        
        input = "8 | 3";
        expression = new Expression(input);
        result = expression.calculate().longValue();
        assertEquals(11L, result);
    }

    // 复合运算测试用例
    @Test
    public void testComplexExpression1() {
        String input = "(-5 + 3 * 2) ** 2 - 10 / 2";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(-4, result);
    }

    @Test
    public void testComplexExpression2() {
        String input = "~(8 & 3) << 2 | 15 + 6";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(-3, result);
    }

    @Test
    public void testComplexExpression3() {
        String input = "((15.5 + 4.5) * 2 - 10) / 3 ** 2";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(100.0, result, 0.1);
    }

    @Test
    public void testComplexExpression4() {
        String input = "!(10 > 5) | (3 <= 4)";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
    }

    @Test
    public void testComplexExpression5() {
        String input = "(17 % 5) * (8 >> 2) + ~(3 & 1)";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(2, result);
    }

    @Test
    public void testComplexExpression6() {
        String input = "2 ** (3 + 1) - 15 / (6 - 1) * 2";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(10.0, result, 0.001);
    }

    @Test
    public void testComplexExpression7() {
        String input = "((100.5 - 0.5) / 10 + 5) * 2 - 3 ** 2";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(21.0, result, 0.001);
    }

    @Test
    public void testComplexExpression8() {
        String input = "-(5 + 3) * ~(-2) >> 1 & 7 | 4";
        Expression expression = new Expression(input);
        long result = expression.calculate().longValue();
        assertEquals(4, result);
    }

    @Test
    public void testComplexExpression9() {
        String input = "(7 >= 5) | (3 < 2) & (10 != 5)";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(1.0, result, 0.001);
    }

    @Test
    public void testComplexExpression10() {
        String input = "((123.45 - 23.45) / 10) ** 1.5 + 5 * (7 - 3)";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(51.6, result, 0.1);
    }
    
    // 边界值测试
    @Test
    public void testBoundaryValues() {
        String input = "0 * 10000";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(0.0, result, 0.001);
        
        input = "10000 + 0";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(10000.0, result, 0.001);
        
        input = "-10000 - 0";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(-10000.0, result, 0.001);
    }
    
    // 括号优先级测试
    @Test
    public void testParenthesesPriority() {
        String input = "2 * (3 + 4) * 5";
        Expression expression = new Expression(input);
        double result = expression.calculate().doubleValue();
        assertEquals(70.0, result, 0.001);
        
        input = "((2 + 3) * (4 + 5))";
        expression = new Expression(input);
        result = expression.calculate().doubleValue();
        assertEquals(45.0, result, 0.001);
    }
}
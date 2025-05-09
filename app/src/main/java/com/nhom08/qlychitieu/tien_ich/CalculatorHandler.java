package com.nhom08.qlychitieu.tien_ich;

public class CalculatorHandler {
    private final StringBuilder input;

    public CalculatorHandler() {
        input = new StringBuilder();
    }

    public void appendNumber(String number) {
        input.append(number);
    }

    public void appendOperator(String operator) {
        if (!input.toString().isEmpty()) {
            try {
                Double.parseDouble(input.toString());
                input.append(operator);
            } catch (NumberFormatException e) {
                clear();
            }
        }
    }

    public void appendDot() {
        if (!input.toString().contains(".")) {
            input.append(".");
        }
    }

    public void append000() {
        if (!input.toString().isEmpty()) {
            input.append("000");
        }
    }

    public void clear() {
        input.setLength(0);
    }

    public String getCurrentInput() {
        return input.toString();
    }

    public double calculate() throws NumberFormatException {
        String expression = input.toString();
        if (expression.isEmpty()) return 0;

        if (expression.contains("+") || expression.contains("-")) {
            String[] parts = expression.split("[+\\-]");
            double result = Double.parseDouble(parts[0]);
            int i = 1;

            for (int j = 0; j < expression.length(); j++) {
                if (expression.charAt(j) == '+') {
                    result += Double.parseDouble(parts[i++]);
                } else if (expression.charAt(j) == '-') {
                    result -= Double.parseDouble(parts[i++]);
                }
            }
            return result;
        }

        return Double.parseDouble(expression);
    }
}

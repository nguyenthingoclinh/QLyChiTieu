package com.nhom08.qlychitieu.tien_ich;

import android.util.Log;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * CalculatorHandler - Lớp xử lý tính toán cho máy tính đơn giản trong ứng dụng

 * Class này cung cấp các chức năng cơ bản cho một máy tính:
 * - Nhập số và toán tử
 * - Thực hiện các phép tính cộng trừ
 * - Format kết quả theo định dạng số
 *
 * @author ThanhNB-NBT
 * @version 1.0
 * @since 2025-05-11
 */
public class CalculatorHandler {
    // TAG để sử dụng cho logging
    private static final String TAG = CalculatorHandler.class.getSimpleName();

    // Các ký tự đặc biệt trong phép tính
    private static final String DECIMAL_SEPARATOR = ".";    // Dấu phân cách thập phân
    private static final String THOUSAND_SEPARATOR = "000"; // Phím tắt thêm 3 số 0
    private static final String OPERATORS = "+-";          // Các toán tử được hỗ trợ

    // StringBuilder để lưu trữ và xử lý biểu thức
    private final StringBuilder input;

    // DecimalFormat để format số theo định dạng mong muốn
    private final DecimalFormat decimalFormat;

    /**
     * Constructor - Khởi tạo CalculatorHandler
     * Thiết lập StringBuilder để lưu biểu thức và DecimalFormat để format kết quả
     */
    public CalculatorHandler() {
        input = new StringBuilder();
        // Khởi tạo DecimalFormat với dấu chấm làm dấu phân cách thập phân
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#.##", symbols);
    }

    /**
     * Thêm một số vào biểu thức
     *
     * @param number Số cần thêm vào biểu thức (dạng String)
     */
    public void appendNumber(String number) {
        if (isValidNumber(number)) {
            input.append(number);
        }
    }

    /**
     * Thêm toán tử vào biểu thức
     * Chỉ thêm nếu phần tử cuối cùng là một số hợp lệ
     *
     * @param operator Toán tử cần thêm (+ hoặc -)
     */
    public void appendOperator(String operator) {
        if (canAppendOperator(operator)) {
            try {
                String currentInput = input.toString();
                // Kiểm tra ký tự cuối có phải là toán tử không
                if (!currentInput.isEmpty() &&
                        !OPERATORS.contains(String.valueOf(currentInput.charAt(currentInput.length() - 1)))) {
                    // Kiểm tra số cuối cùng có hợp lệ không
                    Double.parseDouble(getLastNumber());
                    input.append(operator);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error appending operator: " + e.getMessage());
                clear();
            }
        }
    }

    /**
     * Thêm dấu thập phân vào số hiện tại
     * Chỉ thêm nếu số hiện tại chưa có dấu thập phân
     */
    public void appendDot() {
        String lastNumber = getLastNumber();
        if (!lastNumber.contains(DECIMAL_SEPARATOR)) {
            input.append(DECIMAL_SEPARATOR);
        }
    }

    /**
     * Thêm ba số 0 vào số hiện tại
     * Chỉ thêm nếu biểu thức không rỗng và không kết thúc bằng toán tử
     */
    public void append000() {
        if (!input.toString().isEmpty() && !endsWithOperator()) {
            input.append(THOUSAND_SEPARATOR);
        }
    }

    /**
     * Xóa toàn bộ biểu thức hiện tại
     */
    public void clear() {
        input.setLength(0);
    }

    /**
     * Lấy biểu thức hiện tại
     *
     * @return Chuỗi biểu thức hiện tại
     */
    public String getCurrentInput() {
        return input.toString();
    }

    /**
     * Tính toán kết quả của biểu thức hiện tại
     *
     * @return Kết quả tính toán
     * @throws NumberFormatException nếu biểu thức không hợp lệ
     */
    public double calculate() throws NumberFormatException {
        String expression = input.toString();
        if (expression.isEmpty()) return 0;

        try {
            if (containsOperators(expression)) {
                return calculateExpression(expression);
            }
            return Double.parseDouble(expression);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error calculating expression: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy kết quả đã được format theo định dạng số
     *
     * @return Chuỗi kết quả đã được format
     */
    public String getFormattedResult() {
        try {
            double result = calculate();
            return decimalFormat.format(result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error formatting result: " + e.getMessage());
            return "0";
        }
    }

    // Các phương thức private helper

    /**
     * Kiểm tra chuỗi có phải là số hợp lệ không
     *
     * @param number Chuỗi cần kiểm tra
     * @return true nếu là số hợp lệ, false nếu không
     */
    private boolean isValidNumber(String number) {
        try {
            Double.parseDouble(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Kiểm tra toán tử có thể thêm vào biểu thức không
     *
     * @param operator Toán tử cần kiểm tra
     * @return true nếu là toán tử hợp lệ (+ hoặc -), false nếu không
     */
    private boolean canAppendOperator(String operator) {
        return operator != null && (operator.equals("+") || operator.equals("-"));
    }

    /**
     * Lấy số cuối cùng trong biểu thức
     *
     * @return Chuỗi chứa số cuối cùng trong biểu thức
     */
    private String getLastNumber() {
        String expression = input.toString();
        int lastOperatorIndex = Math.max(
                expression.lastIndexOf("+"),
                expression.lastIndexOf("-")
        );
        return lastOperatorIndex == -1 ?
                expression :
                expression.substring(lastOperatorIndex + 1);
    }

    /**
     * Kiểm tra biểu thức có kết thúc bằng toán tử không
     *
     * @return true nếu kết thúc bằng toán tử, false nếu không
     */
    private boolean endsWithOperator() {
        if (input.length() == 0) return false;
        char lastChar = input.charAt(input.length() - 1);
        return OPERATORS.indexOf(lastChar) != -1;
    }

    /**
     * Kiểm tra biểu thức có chứa toán tử không
     *
     * @param expression Biểu thức cần kiểm tra
     * @return true nếu có chứa toán tử, false nếu không
     */
    private boolean containsOperators(String expression) {
        return expression.contains("+") || expression.contains("-");
    }

    /**
     * Tính toán kết quả của biểu thức có chứa toán tử
     *
     * @param expression Biểu thức cần tính
     * @return Kết quả tính toán
     * @throws NumberFormatException nếu biểu thức không hợp lệ
     */
    private double calculateExpression(String expression) {
        // Tách biểu thức thành các phần số
        String[] parts = expression.split("[+\\-]");
        if (parts.length == 0) return 0;

        // Bắt đầu với số đầu tiên
        double result = Double.parseDouble(parts[0]);
        int partIndex = 1;

        // Duyệt qua từng ký tự trong biểu thức để tìm toán tử
        for (int i = 0; i < expression.length(); i++) {
            char operator = expression.charAt(i);
            if (operator == '+') {
                result += Double.parseDouble(parts[partIndex++]);
            } else if (operator == '-') {
                result -= Double.parseDouble(parts[partIndex++]);
            }
        }

        return result;
    }
}
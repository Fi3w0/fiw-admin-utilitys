package com.fiw.common;

public final class TextFormat {
    private static final String COLOR_CODES = "0123456789abcdefklmnorABCDEFKLMNOR";

    private TextFormat() {
    }

    public static String legacyColors(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '&' && index + 1 < value.length() && COLOR_CODES.indexOf(value.charAt(index + 1)) >= 0) {
                builder.append('\u00a7');
                builder.append(Character.toLowerCase(value.charAt(++index)));
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}

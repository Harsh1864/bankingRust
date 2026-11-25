package io.github.hossensyedriadh.cbs.mandate.internal.util;

public final class IbanCheck {
    private IbanCheck() {
    }

    public static boolean isValid(String iban) {
        if (iban == null) {
            return false;
        }

        String compact = iban.replace(" ", "").toUpperCase();
        if (!compact.matches("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}")) {
            return false;
        }

        String rearranged = compact.substring(4) + compact.substring(0, 4);
        StringBuilder numeric = new StringBuilder(rearranged.length() * 2);
        for (int i = 0; i < rearranged.length(); i++) {
            char character = rearranged.charAt(i);
            if (Character.isDigit(character)) {
                numeric.append(character);
            } else if (Character.isLetter(character)) {
                numeric.append(character - 'A' + 10);
            } else {
                return false;
            }
        }

        return mod97(numeric.toString()) == 1;
    }

    private static int mod97(String numeric) {
        int remainder = 0;
        for (int i = 0; i < numeric.length(); i++) {
            int digit = numeric.charAt(i) - '0';
            remainder = (remainder * 10 + digit) % 97;
        }
        return remainder;
    }
}

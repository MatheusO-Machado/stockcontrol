package br.com.matheus.stockcontrol.util;

import br.com.matheus.stockcontrol.model.DocumentType;

public final class BrDocumentValidator {
    private BrDocumentValidator() {}

    public static String onlyDigits(String s) {
        if (s == null) return null;
        return s.replaceAll("\\D+", "");
    }

    public static void validate(DocumentType type, String documentDigits) {
        if (type == null) throw new IllegalArgumentException("Tipo de documento é obrigatório.");
        String d = onlyDigits(documentDigits);
        if (d == null || d.isBlank()) throw new IllegalArgumentException("Documento é obrigatório.");

        if (type == DocumentType.CPF) {
            if (!isValidCPF(d)) throw new IllegalArgumentException("CPF inválido.");
        } else {
            if (!isValidCNPJ(d)) throw new IllegalArgumentException("CNPJ inválido.");
        }
    }

    // CPF: 11 dígitos
    public static boolean isValidCPF(String cpf) {
        cpf = onlyDigits(cpf);
        if (cpf == null || cpf.length() != 11) return false;
        if (allSameDigits(cpf)) return false;

        int d1 = calcCpfDigit(cpf, 9);
        int d2 = calcCpfDigit(cpf, 10);
        return cpf.charAt(9) - '0' == d1 && cpf.charAt(10) - '0' == d2;
    }

    private static int calcCpfDigit(String cpf, int len) {
        int sum = 0;
        int weight = len + 1; // 10 depois 11
        for (int i = 0; i < len; i++) {
            sum += (cpf.charAt(i) - '0') * (weight--);
        }
        int mod = (sum * 10) % 11;
        return (mod == 10) ? 0 : mod;
    }

    // CNPJ: 14 dígitos
    public static boolean isValidCNPJ(String cnpj) {
        cnpj = onlyDigits(cnpj);
        if (cnpj == null || cnpj.length() != 14) return false;
        if (allSameDigits(cnpj)) return false;

        int d1 = calcCnpjDigit(cnpj, 12);
        int d2 = calcCnpjDigit(cnpj, 13);
        return cnpj.charAt(12) - '0' == d1 && cnpj.charAt(13) - '0' == d2;
    }

    private static int calcCnpjDigit(String cnpj, int len) {
        int[] w = (len == 12)
                ? new int[]{5,4,3,2,9,8,7,6,5,4,3,2}
                : new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2};

        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (cnpj.charAt(i) - '0') * w[i];
        }

        int mod = sum % 11;
        return (mod < 2) ? 0 : (11 - mod);
    }

    private static boolean allSameDigits(String s) {
        char c = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != c) return false;
        }
        return true;
    }
}
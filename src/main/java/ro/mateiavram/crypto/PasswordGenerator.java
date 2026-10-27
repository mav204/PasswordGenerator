package ro.mateiavram.crypto;

import java.security.SecureRandom;

public class PasswordGenerator {

    private final SecureRandom secureRandom=new SecureRandom();
    public static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DIGITS = "0123456789";
    public static final String SYMBOLS = "!@#$%^&*-_=+?";

    public String generate(int length, boolean upper,boolean digits,boolean symbols){

        String alphabet=buildAlphabet(upper,digits,symbols);

        StringBuilder sb=new StringBuilder();
        for(int i=0;i<length;i++){
            int n=secureRandom.nextInt(alphabet.length());
            sb.append(alphabet.charAt(n));
        }

        return sb.toString();

    }



    public double entropyBits(int length,boolean upper, boolean digits, boolean symbols){
        int size=buildAlphabet(upper,digits,symbols).length();
        return length*Math.log(size)/Math.log(2);
    }

    public String buildAlphabet(boolean upper, boolean digits, boolean symbols) {
        String alphabet = LOWER;
        if (upper) {
            alphabet += UPPER;
        }
        if (digits) {
            alphabet += DIGITS;
        }
        if (symbols) {
            alphabet += SYMBOLS;
        }
        return alphabet;
    }
}

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class HugeInteger {
    private int sign_indicator; // -1, 0 or +1
    private int[] digits; // this is decimal number, each max number of element is 9

    public HugeInteger(String value) {
        if (value == null) {
            throw new IllegalArgumentException("the value is empty");
        }

        final int total_len = value.length();
        int pointer = 0;

        if (total_len == 0) { throw new NumberFormatException("This huge integer is zero length"); }

        // Check for at most one leading negative sign
        int sign = 1, index1 = value.lastIndexOf('-');//lastIndexOf() return the last ch position
        if (index1 >= 0) {
            if (index1 != 0) {
                throw new NumberFormatException("Illegal digit");// for example  "56-678"/"-56-78" there are all illegal
            }
            sign = -1;// the sign indicates this is negative
            pointer = 1;
        }

        if (pointer == total_len) { throw new NumberFormatException("Zero length HugeInteger"); }

        // Skip leading zeros, and also compute number of digits
        while (pointer < total_len && Character.digit(value.charAt(pointer), 10) == 0) {
            pointer++;
        }

        // Input is 0
        if (pointer == total_len) {
            sign_indicator = 0;
            digits = new int[0];
            return;
        }

        sign_indicator = sign;
        digits = new int[total_len - pointer];

        // Process each digit
        for (int j = 0; (j + pointer) < total_len; j++) {
            String digit = value.substring(j + pointer, j + pointer + 1);
            digits[j] = Integer.parseInt(digit);
            if (digits[j] < 0) {// this indicates [5 4 3 -2 7], -2 is illegal in this part
                throw new NumberFormatException("Illegal digit");
            }
        }
    }

    public HugeInteger(int n) {// it creates a random of n digits
        if (n < 1) { throw new IllegalArgumentException("n must be larger or equal to 1"); }

        // random generates the sign value -1 ~ 1
        sign_indicator = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        digits = new int[n];
        digits[0] = ThreadLocalRandom.current().nextInt(1, 10);// value 1 ~  9

        for (int i = 1; i < n; i++) {// loop through each digits
            digits[i] = ThreadLocalRandom.current().nextInt(0, 10);// value 0 ~ 9
        }
    }

    private HugeInteger(int[] digits, int signvalue) {
        this.digits = digits;
        this.sign_indicator = signvalue;
    }


    public HugeInteger add(HugeInteger h) {
        if (h.sign_indicator == 0) { return this; }
        if (sign_indicator == 0) { return h; }
        if (sign_indicator == h.sign_indicator) { return new HugeInteger(add(digits, h.digits), sign_indicator); }

        // if sign_indicator are opposite, then we need to find which one is bigger and which one is smaller, use the bigger one to minus the smaller one
        // Then we can create a new HugeInteger with a signum of 1 if this one was bigger, or -1 if h was bigger
        int compare = compareDigits(h);
        if (compare == 0) { return new HugeInteger(new int[0], 0); }

        int[] outcome = compare == 1 ? subtract(digits, h.digits) : subtract(h.digits, digits);
        // in here, if the outcome/compare result is 1 (means that digits is bigger than h), we could use digits minus h, otherwise vise versa.
        return new HugeInteger(outcome, compare == sign_indicator ? 1 : -1);
    }

    private static int[] add(int[] digits, int[] hDigits) {
        // Make digits be the small one by swapping
        if (digits.length > hDigits.length) {
            int[] tmp = hDigits;
            hDigits = digits;
            digits = tmp;
        }

        boolean carry_bit = false;
        int smallInd = digits.length, bigInd = hDigits.length;
        int[] result = new int[bigInd];

        // Start from the end and sum each pair of elements with the optional carry, until small number is done
        while (smallInd > 0) {
            int sum = digits[--smallInd] + hDigits[--bigInd] + (carry_bit ? 1 : 0);
            if (sum > 9) { // Can only have 1 digit per element so handle if the sum > 9
                carry_bit = true;
                sum -= 10;//the rest number is [sum - 10]
            } else {
                carry_bit = false;
            }
            result[bigInd] = sum;
        }

        // Sum remaining big number elements with optional carry
        while (bigInd > 0) {
            int sum = hDigits[--bigInd] + (carry_bit ? 1 : 0);
            if (sum > 9) {
                carry_bit = true;
                sum -= 10;
            } else {
                carry_bit = false;
            }
            result[bigInd] = sum;
        }

        // Grow result depending on possible last carry
        if (carry_bit) {
            int[] grown = new int[result.length + 1];
            System.arraycopy(result, 0, grown, 1, result.length);
            grown[0] = 1;
            return grown;
        }

        return result;
    }


    public HugeInteger subtract(HugeInteger h) {// this part is similar to addition, because it is the same principle
        if (h.sign_indicator == 0) { return this; }
        if (sign_indicator == 0) { return new HugeInteger(h.digits, -sign_indicator); }
        if (h.sign_indicator != sign_indicator) { return new HugeInteger(add(digits, h.digits), sign_indicator); }


        int compare = compareDigits(h);
        if (compare == 0) { return new HugeInteger(new int[0], 0); }

        int[] result = compare == 1 ? subtract(digits, h.digits) : subtract(h.digits, digits);

        return new HugeInteger(result, compare == sign_indicator ? 1 : -1);
    }

    private static int[] subtract(int[] big, int[] small) {
        boolean borrow_bit = false;
        int bigIndex = big.length, smallIndex = small.length;
        int[] result = new int[bigIndex];

        // Start from the end and subtract each pair of elements with the optional borrow, until small number is done
        while (smallIndex > 0) {
            int difference_result = big[--bigIndex] - small[--smallIndex] - (borrow_bit ? 1 : 0);
            if (difference_result < 0) { // Can only have 1 digit per element so handle if the difference < 0
                borrow_bit = true;
                difference_result += 10;
            } else {
                borrow_bit = false;
            }
            result[bigIndex] = difference_result;
        }

        // Subtract remaining big number elements with optional borrow
        while (bigIndex > 0) {
            int difference_result = big[--bigIndex] - (borrow_bit ? 1 : 0);
            if (difference_result < 0) {
                borrow_bit = true;
                difference_result += 10;
            } else {
                borrow_bit = false;
            }
            result[bigIndex] = difference_result;
        }

        return stripLeadingZeroes(result);
    }


    public HugeInteger multiply(HugeInteger h) {
        if (sign_indicator == 0 || h.sign_indicator == 0) {
            return new HugeInteger(new int[0], 0);
        }

        int a = sign_indicator == h.sign_indicator ? 1 : -1;
        int length = digits.length, hLength = h.digits.length;
        if (length == 1) {
            return multiplyByDigit(h, digits[0], a);
        }
        if (hLength == 1) {
            return multiplyByDigit(this, h.digits[0], a);
        }

        return karatsuba(this, h);
    }


    private HugeInteger multiplyn2(HugeInteger h, int signum) {
        int[] result_array = new int[h.digits.length + this.digits.length];
        int xstart = this.digits.length - 1;
        int ystart = h.digits.length - 1;
        int[] temp = h.digits, temp2 = this.digits;

        int carry_bit;
        for (int m = xstart; m >= 0; m--) {
            carry_bit = 0;
            for (int n = ystart, k = ystart + 1 + m; n >= 0; n--, k--) {
                int product_result = temp[n] * temp2[m] + result_array[k] + carry_bit;
                if (product_result > 9) {
                    result_array[k] = product_result % 10;
                    carry_bit = product_result / 10;
                } else {
                    result_array[k] = product_result;
                    carry_bit = 0;
                }
            }
            result_array[m] = carry_bit;
        }

        return new HugeInteger(stripLeadingZeroes(result_array), signum);
    }

    private static HugeInteger multiplyByDigit(HugeInteger z, int digit, int a) {
        int[] result = new int[z.digits.length + 1];
        int carry_bit = 0, outcome;
        int rIndex = result.length - 1;

        for (int i = z.digits.length - 1; i >= 0; i--) {
            outcome = z.digits[i] * digit + carry_bit;
            if (outcome > 9) {
                result[rIndex--] = outcome % 10;
                carry_bit = outcome / 10;
            } else {
                result[rIndex--] = outcome;
                carry_bit = 0;
            }
        }

        // If there is an empty space of the array, we should get rid of it
        if (carry_bit == 0) {
            result = Arrays.copyOfRange(result, 1, result.length);
        } else {
            result[rIndex] = carry_bit;
        }

        return new HugeInteger(result, a);
    }



    /**
     * I will explain the theory about karatsuba in my report, it is based on the recursive chapter we learned in the lecture
     * seen: https://www.youtube.com/watch?v=JCbZayFr9RE Karatsuba Multiplication

     */
    private HugeInteger karatsuba(HugeInteger num1, HugeInteger num2) {
        int Length1 = num1.digits.length, Length2 = num2.digits.length;

        int mid = (Math.max(Length1, Length2) + 1) / 2;

        // Divide both numbers into their upper and lower parts
        HugeInteger xUp = num1.getUpper(mid), xLow = num1.getLower(mid), yUp = num2.getUpper(mid), yLow = num2.getLower(mid);

        HugeInteger z0 = xLow.multiply(yLow);
        HugeInteger z1 = xUp.multiply(yUp);
        HugeInteger z2 = xLow.add(xUp).multiply(yLow.add(yUp));

        HugeInteger result = z1.shiftLeft(mid).add(z2.subtract(z1).subtract(z0)).shiftLeft(mid).add(z0);

        if (num1.sign_indicator != num2.sign_indicator) {
            return new HugeInteger(result.digits, -result.sign_indicator);
        } else {
            return result;
        }
    }


    private HugeInteger shiftLeft(int k) {
        if (sign_indicator == 0) {
            return new HugeInteger(new int[0], 0);
        }
        if (k > 0) {
            // Copy the digits array to a bigger one, starting at 0 to emulate the shift
            int[] outcome_array = new int[digits.length + k];
            System.arraycopy(digits, 0, outcome_array, 0, digits.length);
            return new HugeInteger(outcome_array, sign_indicator);
        } else if (k == 0) {
            return this;
        } else {
            throw new IllegalArgumentException("k must be bigger or equal to zero");
        }
    }

    /**
     * Returns a new HugeInteger representing n lower digits of the number.
     */
    private HugeInteger getLower(int m) {
        int len = digits.length;

        // Return the absolute value of this HugeInteger if the length is less than or equal to the number of digits in the lower half
        if (len <= m) {
            return sign_indicator >= 0 ? this : new HugeInteger(this.digits, -this.sign_indicator);
        }

        int[] lowerDigits = new int[m];
        System.arraycopy(digits, len - m, lowerDigits, 0, m);
        lowerDigits = stripLeadingZeroes(lowerDigits);
        return new HugeInteger(lowerDigits, lowerDigits.length == 0 ? 0 : 1); // just want to check if lower half is zero or not
    }

    /**
     * Returns a new HugeInteger representing digits.length-n upper digits of the number.
     */
    private HugeInteger getUpper(int n) {
        int len = digits.length;

        // Return 0 if the length is <= the number of digits in the upper half
        if (len <= n) {
            return new HugeInteger(new int[0], 0);
        }

        int upperLen = len - n;
        int[] upperDig = new int[upperLen];
        System.arraycopy(digits, 0, upperDig, 0, upperLen);
        return new HugeInteger(stripLeadingZeroes(upperDig), 1);
    }


    private static int[] stripLeadingZeroes(int[] val) {
        int nonZeroIndex;
        for (nonZeroIndex = 0; nonZeroIndex < val.length && val[nonZeroIndex] == 0; nonZeroIndex++) {}
        return nonZeroIndex != 0 ? Arrays.copyOfRange(val, nonZeroIndex, val.length) : val;
    }// return strip digits array


    public int compareTo(HugeInteger h) {
        // Compare signs first, then digits
        if (sign_indicator == h.sign_indicator) {
            switch (sign_indicator) {
                case 1:
                    return compareDigits(h);
                case -1:
                    return h.compareDigits(this);
                default:
                    return 0;
            }
        }

        return sign_indicator > h.sign_indicator ? 1 : -1;
    }// return -1, 0 or 1 if this HugeInteger is numerically less than, equal to, or greater than h

    private int compareDigits(HugeInteger h) {
        // In the beginning we need to compare the length, then we could compare for each digits
        int length1 = digits.length, length2 = h.digits.length;

        if (length1 < length2) { return -1; }
        if (length1 > length2) { return 1; }

        // each digits go through loop and compare them
        for (int j = 0; j < length1; j++) {
            int m = digits[j], n = h.digits[j];
            if (m != n) {
                return m < n ? -1 : 1;// if m is less than n, return -1, otherwise return 1
            }
        }

        return 0;
    }

    @Override //in here, override toString method
    public String toString() {
        if (sign_indicator == 0) { return "0"; }

        StringBuilder strBu = new StringBuilder(digits.length + 1);

        if (sign_indicator == -1) { strBu.append("-"); }

        for (int digit : digits) {
            strBu.append(digit);
        }

        return strBu.toString();
    }
}

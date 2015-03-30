package com.cm4j.test.guava.base;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import org.apache.commons.lang.StringUtils;

/**
 * Created by yanghao on 14-8-12.
 */
public abstract class StringMatcher implements Predicate<String> {

    public abstract boolean mataches(final String sequence);

    public static StringMatcher is(final String match) {
        return new StringMatcher() {
            @Override
            public boolean mataches(String sequence) {
                return StringUtils.contains(sequence, match);
            }
        };
    }

    @Override
    public boolean apply(String sequence) {
        return mataches(sequence);
    }

    public StringMatcher and(final StringMatcher matcher) {
        return new And(this, matcher);
    }

    public Or or(final StringMatcher matcher) {
        return new Or(this, matcher);
    }

    /*
     * ============== inner class =============
     */

    private static class And extends StringMatcher{
        final StringMatcher first;
        final StringMatcher second;

        private And(StringMatcher first, StringMatcher second) {
            this.first = Preconditions.checkNotNull(first);
            this.second = Preconditions.checkNotNull(second);
        }

        @Override
        public boolean mataches(String sequence) {
            return first.mataches(sequence) && second.mataches(sequence);
        }
    }

    private static class Or extends StringMatcher{
        final StringMatcher first;
        final StringMatcher second;

        private Or(StringMatcher first, StringMatcher second) {
            this.first = Preconditions.checkNotNull(first);
            this.second = Preconditions.checkNotNull(second);
        }

        @Override
        public boolean mataches(String sequence) {
            return first.mataches(sequence) || second.mataches(sequence);
        }

        public int countIn(String sequence) {
            int count = 0;
            if (first instanceof Or) {
                count += ((Or) first).countIn(sequence);
            } else if (first.mataches(sequence)) {
                count++;
            }
            if (second instanceof Or) {
                count += ((Or) second).countIn(sequence);
            } else if (second.mataches(sequence)) {
                count++;
            }
            return count;
        }
    }

    public static void main(String[] args) {
        System.out.println(StringMatcher.is("z2f").or(StringMatcher.is("mc"))
                .or(StringMatcher.is("gy")).countIn("zf_mc_lb_gy"));
    }
}

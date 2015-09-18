package com.cm4j.test.guava.base;

public final class Splitter {

    /*private final CharMatcher trimmer;
    private final boolean omitEmptyStrings;
    private final Strategy strategy;
    private final int limit;

    public Splitter(Strategy strategy) {
        this(strategy, CharMatcher.NONE, false, Integer.MAX_VALUE);
    }

    public Splitter(Strategy strategy, CharMatcher trimmer, boolean omitEmptyStrings, int limit) {
        this.trimmer = trimmer;
        this.omitEmptyStrings = omitEmptyStrings;
        this.strategy = strategy;
        this.limit = limit;
    }

    public static Splitter on(final CharMatcher separatorMatcher) {
        return new Splitter((splitter,toSplit) -> new SplittingIterator(splitter, toSplit) {
            @Override
            int separatorStart(int start) {
                return separatorMatcher.indexIn(toSplit, start);
            }

            @Override
            int separatorEnd(int separatorPosition) {
                return separatorPosition + 1;
            }
        });
    }

    public Iterable<String> split(CharSequence toSplit) {
        return () -> splittingIterator(toSplit);
    }

    private Iterator<String> splittingIterator(CharSequence toSplit) {
        return strategy.iterator(this, toSplit);
    }

    private interface Strategy {
        Iterator<String> iterator(Splitter splitter, CharSequence toSplit);
    }

    private abstract static class SplittingIterator extends AbstractIterator<String>{
        private final CharMatcher trimmer;
        private final boolean omitEmptyStrings;
        private CharSequence toSplit;

        int offset = 0;
        int limit;

        protected SplittingIterator(Splitter splitter, CharSequence toSplit) {
            this.trimmer = splitter.trimmer;
            this.omitEmptyStrings = splitter.omitEmptyStrings;
            this.limit = splitter.limit;
            this.toSplit = toSplit;
        }

        abstract int separatorStart(int start);
        abstract int separatorEnd(int separatorPosition);

        @Override
        protected String computeNext() {
            int nextStart = offset;
            while (offset != -1) {
                int start = nextStart;
                int end;

                int separatorPosition = separatorStart(offset);
                if (separatorPosition == -1) {
                    end = toSplit.length();
                    offset = -1;
                } else {
                    end = separatorPosition;
                    offset = separatorEnd(separatorPosition);
                }
                if (offset == nextStart) {
                    offset++;
                    if (offset >= toSplit.length()) {
                        offset = -1;
                    }
                    continue;
                }

                while (start < end && trimmer.matches(toSplit.charAt(start))) {
                    start++;
                }
                while (end > start && trimmer.matches(toSplit.charAt(end - 1))) {
                    end--;
                }

                if (omitEmptyStrings && start == end) {
                    nextStart = offset;
                    continue;
                }

                if (limit == 1) {
                    end = toSplit.length();
                    offset = -1;
                    while (end > start && trimmer.matches(toSplit.charAt(end - 1))) {
                        end--;
                    }
                } else {
                    limit--;
                }

                return toSplit.subSequence(start, end).toString();
            }
            return endOfData();
        }
    }

    public static void main(String[] args) {
        Iterable<String> split = Splitter.on(CharMatcher.DIGIT).split("a1b2c3");
        split.forEach(System.out::println);
    }*/
}

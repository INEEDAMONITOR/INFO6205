/*
  (c) Copyright 2018, 2019 Phasmid Software
 */
package edu.neu.coe.info6205.util;

import edu.neu.coe.info6205.sort.*;
import edu.neu.coe.info6205.sort.elementary.*;
import edu.neu.coe.info6205.sort.linearithmic.TimSort;
import edu.neu.coe.info6205.sort.linearithmic.*;

import java.io.*;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static edu.neu.coe.info6205.util.SortBenchmarkHelper.generateRandomLocalDateTimeArray;
import static edu.neu.coe.info6205.util.SortBenchmarkHelper.getWords;
import static edu.neu.coe.info6205.util.Utilities.formatWhole;

public class SortBenchmark {

    final static LazyLogger logger = new LazyLogger(SortBenchmark.class);
    final static Pattern regexLeipzig = Pattern.compile("[~\\t]*\\t(([\\s\\p{Punct}\\uFF0C]*\\p{L}+)*)");
    /**
     * For (basic) insertionsort, the number of array accesses is actually 6 times the number of comparisons.
     * That's because, for each inversion, there will typically be one swap (four array accesses) and (at least) one comparison (two array accesses).
     * Thus, in the case where comparisons are based on primitives,
     * the normalized time per run should approximate the time for one array access.
     */
    private final static TimeLogger[] timeLoggersQuadratic = {new TimeLogger("Raw time per run (mSec): ", (time, n) -> time), new TimeLogger("Normalized time per run (n^2): ", (time, n) -> time / meanInversions(n) / 6 * 1e6)};
    private static final double LgE = Utilities.lg(Math.E);
    /**
     * For mergesort, the number of array accesses is actually 6 times the number of comparisons.
     * That's because, in addition to each comparison, there will be approximately two copy operations.
     * Thus, in the case where comparisons are based on primitives,
     * the normalized time per run should approximate the time for one array access.
     */
    public final static TimeLogger[] timeLoggersLinearithmic = {new TimeLogger("Raw time per run (mSec): ", (time, n) -> time), new TimeLogger("Normalized time per run (n log n): ", (time, n) -> time / minComparisons(n) / 6 * 1e6)};
    private final Config config;

    public SortBenchmark(Config config) {
        this.config = config;
    }

    public static void main(String[] args) throws IOException {
        Config config = Config.load(SortBenchmark.class);

        List<Long> timeListHeap = new ArrayList<>();
        List<Long> timeListMerge = new ArrayList<>();
        List<Long> timeListQuick = new ArrayList<>();
        List<List<Integer>> heapList = new ArrayList<>();
        List<List<Integer>> mergeList = new ArrayList<>();
        List<List<Integer>> quickList = new ArrayList<>();

        for (int n = 10000; n <= 160000; n *= 1.5) {
            int comparesHeap = 0;
            int swapsHeap = 0;
            int hitsHeap = 0;
            long timeHeap = 0;

            int comparesMerge = 0;
            int swapsMerge = 0;
            int hitsMerge = 0;
            long timeMerge = 0;

            int comparesQuick = 0;
            int swapsQuick = 0;
            int hitsQuick = 0;
            long timeQuick = 0;

            int repeatTime = 50;
            System.out.println(n + "==========");
            // With InstrumentedHelper
            for (int i = 0; i < repeatTime; i++) {

                Helper<Integer> helper = HelperFactory.create("HeapSort", n, config);
                helper.init(n);
                Integer[] xs = helper.random(Integer.class, r -> r.nextInt(1000));

                final PrivateMethodTester privateMethodTester = new PrivateMethodTester(helper);
                final StatPack statPack = (StatPack) privateMethodTester.invokePrivate("getStatPack");
                SortWithHelper<Integer> sorter = new HeapSort<Integer>(helper);
                sorter.preProcess(xs);
                Integer[] ys = sorter.sort(xs);
                sorter.postProcess(ys);

                comparesHeap += (int) statPack.getStatistics(InstrumentedHelper.COMPARES).mean();
                swapsHeap += (int) statPack.getStatistics(InstrumentedHelper.SWAPS).mean();
                hitsHeap += (int) statPack.getStatistics(InstrumentedHelper.HITS).mean();


                // Merge Sort
                Helper<Integer> helperMerge = HelperFactory.create("Merge Sort", n, config);
                helperMerge.init(n);
                final PrivateMethodTester privateMethodTesterMerge = new PrivateMethodTester(helperMerge);
                final StatPack statPackMerge = (StatPack) privateMethodTesterMerge.invokePrivate("getStatPack");

                SortWithHelper<Integer> megerSorter = new MergeSort<>(helperMerge);
                megerSorter.preProcess(xs);
                Integer[] ysMerge = megerSorter.sort(xs);
                megerSorter.postProcess(ysMerge);


                comparesMerge += (int) statPackMerge.getStatistics(InstrumentedHelper.COMPARES).mean();
                swapsMerge += (int) statPackMerge.getStatistics(InstrumentedHelper.SWAPS).mean();
                hitsMerge += (int) statPackMerge.getStatistics(InstrumentedHelper.HITS).mean();


                // Quick Sort
                Helper<Integer> helperQ = HelperFactory.create("Merge Sort", n, config);
                helperQ.init(n);
                final PrivateMethodTester privateMethodTesterQ = new PrivateMethodTester(helperQ);
                final StatPack statPackQ = (StatPack) privateMethodTesterQ.invokePrivate("getStatPack");

                SortWithHelper<Integer> quickSort = new QuickSort_DualPivot<>(helperQ);
                quickSort.preProcess(xs);
                Integer[] ysQuick = quickSort.sort(xs);
                quickSort.postProcess(ysQuick);

                comparesQuick = (int) statPackQ.getStatistics(InstrumentedHelper.COMPARES).mean();
                swapsQuick = (int) statPackQ.getStatistics(InstrumentedHelper.SWAPS).mean();
                hitsQuick = (int) statPackQ.getStatistics(InstrumentedHelper.HITS).mean();
            }
            // Without InstrumentedHelper
            for (int i = 0; i < repeatTime; i++) {

                Helper<Integer> helper = HelperFactory.create("HeapSort", n, config);
                helper.init(n);
                Integer[] xs = helper.random(Integer.class, r -> r.nextInt(1000));

                long startTime = 0;
                long endTime = 0;

                // Heap
                startTime = System.currentTimeMillis();
                GenericSort<Integer> sHeap = new HeapSort(helper);
                Integer[] ysHeap = sHeap.sort(xs);
                endTime = System.currentTimeMillis();
                timeHeap += (endTime - startTime);

                // Merge
                startTime = System.currentTimeMillis();
                GenericSort<Integer> sMerge = new MergeSort(helper);
                Integer[] ysMerge = sMerge.sort(xs);
                endTime = System.currentTimeMillis();
                timeMerge += (endTime - startTime);

                // Quick
                startTime = System.currentTimeMillis();
                GenericSort<Integer> sQuick = new QuickSort_DualPivot(helper);
                Integer[] ysQuick = sQuick.sort(xs);
                endTime = System.currentTimeMillis();
                timeQuick += (endTime - startTime);


            }
            timeListHeap.add(timeHeap / repeatTime);
            timeListQuick.add(timeQuick / repeatTime);
            timeListMerge.add(timeMerge / repeatTime);

            heapList.add(Arrays.asList(new Integer[]{comparesHeap / repeatTime, swapsHeap / repeatTime, hitsHeap / repeatTime}));
            mergeList.add(Arrays.asList(new Integer[]{comparesMerge / repeatTime, swapsMerge / repeatTime, hitsMerge / repeatTime}));
            quickList.add(Arrays.asList(new Integer[]{comparesQuick / repeatTime, swapsQuick / repeatTime, hitsQuick / repeatTime}));

            System.out.printf("Heap: %d %d %d %d\n", comparesHeap / repeatTime, swapsHeap / repeatTime, hitsHeap / repeatTime, timeHeap / repeatTime);
            System.out.printf("Merge: %d %d %d %d\n", comparesMerge / repeatTime, swapsMerge / repeatTime, hitsMerge / repeatTime, timeMerge / repeatTime);
            System.out.printf("Quick: %d %d %d %d\n", comparesQuick / repeatTime, swapsQuick / repeatTime, hitsQuick / repeatTime, timeQuick / repeatTime);
        }
        System.out.println(Arrays.toString(heapList.toArray()));

        try {
            FileOutputStream fis = new FileOutputStream("./src/" + "heap_result.csv");
            OutputStreamWriter isr = new OutputStreamWriter(fis);
            BufferedWriter bw = new BufferedWriter(isr);
            bw.write("size \t n log n \t log n \t Compares\t swaps \t hits \t time \n");
            bw.flush();
            for (int i = 0, size = 10000; i < timeListHeap.size(); i++, size *= 1.5) {

                String content = size + "\t" + size * Math.log(size) + "\t" + Math.log(size) + "\t";
                for (int num : heapList.get(i)) {
                    content += num + "\t";

                }
                content += timeListHeap.get(i) + "\n";
                bw.write(content);
                bw.flush();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileOutputStream fis = new FileOutputStream("./src/" + "merge_result.csv");
            OutputStreamWriter isr = new OutputStreamWriter(fis);
            BufferedWriter bw = new BufferedWriter(isr);
            bw.write("size \t n log n \t log n \t Compares\t swaps \t hits \t time \n");
            bw.flush();
            for (int i = 0, size = 10000; i < timeListMerge.size(); i++, size *= 1.5) {

                String content = size + "\t" + size * Math.log(size) + "\t" + Math.log(size) + "\t";
                for (int num : mergeList.get(i)) {
                    content += num + "\t";

                }
                content += timeListMerge.get(i) + "\n";
                bw.write(content);
                bw.flush();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileOutputStream fis = new FileOutputStream("./src/" + "quick_result.csv");
            OutputStreamWriter isr = new OutputStreamWriter(fis);
            BufferedWriter bw = new BufferedWriter(isr);
            bw.write("size \t n log n \t log n \t Compares\t swaps \t hits \t time \n");
            bw.flush();
            for (int i = 0, size = 10000; i < timeListQuick.size(); i++, size *= 1.5) {

                String content = size + "\t" + size * Math.log(size) + "\t" + Math.log(size) + "\t";
                for (int num : quickList.get(i)) {
                    content += num + "\t";

                }
                content += timeListQuick.get(i) + "\n";
                bw.write(content);
                bw.flush();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to run a sorting benchmark, using an explicit preProcessor.
     *
     * @param words        an array of available words (to be chosen randomly).
     * @param nWords       the number of words to be sorted.
     * @param nRuns        the number of runs of the sort to be preformed.
     * @param sorter       the sorter to use--NOTE that this sorter will be closed at the end of this method.
     * @param preProcessor the pre-processor function, if any.
     * @param timeLoggers  a set of timeLoggers to be used.
     */
    static void runStringSortBenchmark(String[] words, int nWords, int nRuns, SortWithHelper<String> sorter, UnaryOperator<String[]> preProcessor, TimeLogger[] timeLoggers) {
        new SorterBenchmark<>(String.class, preProcessor, sorter, words, nRuns, timeLoggers).run(nWords);
        sorter.close();
    }

    /**
     * Method to run a sorting benchmark using the standard preProcess method of the sorter.
     *
     * @param words       an array of available words (to be chosen randomly).
     * @param nWords      the number of words to be sorted.
     * @param nRuns       the number of runs of the sort to be preformed.
     * @param sorter      the sorter to use--NOTE that this sorter will be closed at the end of this method.
     * @param timeLoggers a set of timeLoggers to be used.
     *                    <p>
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   NOTE: this method is public because it is referenced in a unit test of a different package
     */
    public static void runStringSortBenchmark(String[] words, int nWords, int nRuns, SortWithHelper<String> sorter, TimeLogger[] timeLoggers) {
        runStringSortBenchmark(words, nWords, nRuns, sorter, sorter::preProcess, timeLoggers);
    }

    /**
     * Method to run a sorting benchmark, using an explicit preProcessor.
     *
     * @param numbers      an array of available integers (to be chosen randomly).
     * @param n            the number of integers to be sorted.
     * @param nRuns        the number of runs of the sort to be preformed.
     * @param sorter       the sorter to use--NOTE that this sorter will be closed at the end of this method.
     * @param preProcessor the pre-processor function, if any.
     * @param timeLoggers  a set of timeLoggers to be used.
     */
    static void runIntegerSortBenchmark(Integer[] numbers, int n, int nRuns, SortWithHelper<Integer> sorter, UnaryOperator<Integer[]> preProcessor, TimeLogger[] timeLoggers) {
        new SorterBenchmark<>(Integer.class, preProcessor, sorter, numbers, nRuns, timeLoggers).run(n);
        sorter.close();
    }

    /**
     * This is based on log2(n!)
     *
     * @param n the number of elements.
     * @return the minimum number of comparisons possible to sort n randomly ordered elements.
     */
    static double minComparisons(int n) {
        double lgN = Utilities.lg(n);
        return n * (lgN - LgE) + lgN / 2 + 1.33;
    }

    /**
     * This is the mean number of inversions in a randomly ordered set of n elements.
     * For insertion sort, each (low-level) swap fixes one inversion, so on average there are this number of swaps.
     * The minimum number of comparisons is slightly higher.
     *
     * @param n the number of elements
     * @return one quarter n-squared more or less.
     */
    static double meanInversions(int n) {
        return 0.25 * n * (n - 1);
    }

    private static Collection<String> lineAsList(String line) {
        List<String> words = new ArrayList<>();
        words.add(line);
        return words;
    }

    public static Collection<String> getLeipzigWords(String line) {
        return getWords(regexLeipzig, line);
    }

    // CONSIDER: to be eliminated soon.
    private static Benchmark<LocalDateTime[]> benchmarkFactory(String description, Consumer<LocalDateTime[]> sorter, Consumer<LocalDateTime[]> checker) {
        return new Benchmark_Timer<>(description, (xs) -> Arrays.copyOf(xs, xs.length), sorter, checker);
    }

    private static void doPureBenchmark(String[] words, int nWords, int nRuns, Random random, Benchmark<String[]> benchmark) {
        // CONSIDER we should manage the space returned by fillRandomArray and deallocate it after use.
        final double time = benchmark.runFromSupplier(() -> Utilities.fillRandomArray(String.class, random, nWords, r -> words[r.nextInt(words.length)]), nRuns);
        for (TimeLogger timeLogger : timeLoggersLinearithmic) timeLogger.log(time, nWords);
    }

    public void sortLocalDateTimes(final int n, Config config) throws IOException {
        logger.info("Beginning LocalDateTime sorts");
        // CONSIDER why do we have localDateTimeSupplier IN ADDITION TO localDateTimes?
        Supplier<LocalDateTime[]> localDateTimeSupplier = () -> generateRandomLocalDateTimeArray(n);
        Helper<ChronoLocalDateTime<?>> helper = new BaseHelper<>("DateTimeHelper", config);
        final LocalDateTime[] localDateTimes = generateRandomLocalDateTimeArray(n);

        // CONSIDER finding the common ground amongst these sorts and get them all working together.

        // NOTE Test on date using pure tim sort.
        if (isConfigBenchmarkDateSorter("timsort"))
            logger.info(benchmarkFactory("Sort LocalDateTimes using Arrays::sort (TimSort)", Arrays::sort, null).runFromSupplier(localDateTimeSupplier, 100) + "ms");

        // NOTE this is supposed to match the previous benchmark run exactly. I don't understand why it takes rather less time.
        if (isConfigBenchmarkDateSorter("timsort")) {
            logger.info(benchmarkFactory("Repeat Sort LocalDateTimes using timSort::mutatingSort", new TimSort<>(helper)::mutatingSort, null).runFromSupplier(localDateTimeSupplier, 100) + "ms");
            // NOTE this is intended to replace the run two lines previous. It should take the exact same amount of time.
            runDateTimeSortBenchmark(LocalDateTime.class, localDateTimes, n, 100);
        }
    }

    /**
     * Method to run pure (non-instrumented) string sorter benchmarks.
     * <p>
     * NOTE: this is package-private because it is used by unit tests.
     *
     * @param words  the word source.
     * @param nWords the number of words to be sorted.
     * @param nRuns  the number of runs.
     */
    void benchmarkStringSorters(String[] words, int nWords, int nRuns) {
        logger.info("Testing pure sorts with " + formatWhole(nRuns) + " runs of sorting " + formatWhole(nWords) + " words");
        Random random = new Random();

        if (isConfigBenchmarkStringSorter("puresystemsort")) {
            Benchmark<String[]> benchmark = new Benchmark_Timer<>("SystemSort", null, Arrays::sort, null);
            doPureBenchmark(words, nWords, nRuns, random, benchmark);
        }

        if (isConfigBenchmarkStringSorter("mergesort")) {
            runMergeSortBenchmark(words, nWords, nRuns, false, false);
            runMergeSortBenchmark(words, nWords, nRuns, true, false);
            runMergeSortBenchmark(words, nWords, nRuns, false, true);
            runMergeSortBenchmark(words, nWords, nRuns, true, true);
        }

        if (isConfigBenchmarkStringSorter("quicksort3way"))
            runStringSortBenchmark(words, nWords, nRuns, new QuickSort_3way<>(nWords, config), timeLoggersLinearithmic);

        if (isConfigBenchmarkStringSorter("quicksortDualPivot"))
            runStringSortBenchmark(words, nWords, nRuns, new QuickSort_DualPivot<>(nWords, config), timeLoggersLinearithmic);

        if (isConfigBenchmarkStringSorter("quicksort"))
            runStringSortBenchmark(words, nWords, nRuns, new QuickSort_Basic<>(nWords, config), timeLoggersLinearithmic);

        if (isConfigBenchmarkStringSorter("introsort"))
            runStringSortBenchmark(words, nWords, nRuns, new IntroSort<>(nWords, config), timeLoggersLinearithmic);

        if (isConfigBenchmarkStringSorter("randomsort"))
            runStringSortBenchmark(words, nWords, nRuns, new RandomSort<>(nWords, config), timeLoggersLinearithmic);

        // NOTE: this is very slow of course, so recommendation is not to enable this option.
        if (isConfigBenchmarkStringSorter("insertionsort"))
            runStringSortBenchmark(words, nWords, nRuns / 10, new InsertionSort<>(nWords, config), timeLoggersQuadratic);

        // NOTE: this is very slow of course, so recommendation is not to enable this option.
        if (isConfigBenchmarkStringSorter("bubblesort"))
            runStringSortBenchmark(words, nWords, nRuns / 10, new BubbleSort<>(nWords, config), timeLoggersQuadratic);

        if (isConfigBenchmarkStringSorter("heapsort")) {
            Helper<String> helper = HelperFactory.create("Heapsort", nWords, config);
            runStringSortBenchmark(words, nWords, nRuns, new HeapSort<>(helper), timeLoggersLinearithmic);
        }
    }

    /**
     * Method to run instrumented string sorter benchmarks.
     * <p>
     * NOTE: this is package-private because it is used by unit tests.
     *
     * @param words  the word source.
     * @param nWords the number of words to be sorted.
     * @param nRuns  the number of runs.
     */
    void benchmarkStringSortersInstrumented(String[] words, int nWords, int nRuns) {
        logger.info("Testing with " + formatWhole(nRuns) + " runs of sorting " + formatWhole(nWords) + " words" + (config.isInstrumented() ? " and instrumented" : ""));
        Random random = new Random();


        if (isConfigBenchmarkStringSorter("puresystemsort")) {
            Benchmark<String[]> benchmark = new Benchmark_Timer<>("SystemSort", null, Arrays::sort, null);
            doPureBenchmark(words, nWords, nRuns, random, benchmark);
        }

        if (isConfigBenchmarkStringSorter("mergesort")) {
            runMergeSortBenchmark(words, nWords, nRuns, false, false);
            runMergeSortBenchmark(words, nWords, nRuns, true, false);
            runMergeSortBenchmark(words, nWords, nRuns, false, true);
            runMergeSortBenchmark(words, nWords, nRuns, true, true);
        }

        if (isConfigBenchmarkStringSorter("quicksort3way"))
            runStringSortBenchmark(words, nWords, nRuns, new QuickSort_3way<>(nWords, config), timeLoggersLinearithmic);

        if (isConfigBenchmarkStringSorter("quicksortDualPivot"))
            runStringSortBenchmark(words, nWords, nRuns, new QuickSort_DualPivot<>(nWords, config), timeLoggersLinearithmic);

        if (isConfigBenchmarkStringSorter("quicksort"))
            runStringSortBenchmark(words, nWords, nRuns, new QuickSort_Basic<>(nWords, config), timeLoggersLinearithmic);

        if (isConfigBenchmarkStringSorter("introsort"))
            runStringSortBenchmark(words, nWords, nRuns, new IntroSort<>(nWords, config), timeLoggersLinearithmic);

        if (isConfigBenchmarkStringSorter("randomsort"))
            runStringSortBenchmark(words, nWords, nRuns, new RandomSort<>(nWords, config), timeLoggersLinearithmic);

        // NOTE: this is very slow of course, so recommendation is not to enable this option.
        if (isConfigBenchmarkStringSorter("insertionsort"))
            runStringSortBenchmark(words, nWords, nRuns / 10, new InsertionSort<>(nWords, config), timeLoggersQuadratic);

        // NOTE: this is very slow of course, so recommendation is not to enable this option.
        if (isConfigBenchmarkStringSorter("bubblesort"))
            runStringSortBenchmark(words, nWords, nRuns / 10, new BubbleSort<>(nWords, config), timeLoggersQuadratic);
    }

    // CONSIDER generifying common code (but it's difficult if not impossible)
    private void sortIntegersByShellSort(final int n) {
        final Random random = new Random();

        // sort int[]
        final Supplier<int[]> intsSupplier = () -> {
            int[] result = (int[]) Array.newInstance(int.class, n);
            for (int i = 0; i < n; i++) result[i] = random.nextInt();
            return result;
        };

        final double t1 = new Benchmark_Timer<int[]>("intArraysorter", (xs) -> Arrays.copyOf(xs, xs.length), Arrays::sort, null).runFromSupplier(intsSupplier, 100);
        for (TimeLogger timeLogger : timeLoggersLinearithmic) timeLogger.log(t1, n);

        // sort Integer[]
        final Supplier<Integer[]> integersSupplier = () -> {
            Integer[] result = (Integer[]) Array.newInstance(Integer.class, n);
            for (int i = 0; i < n; i++) result[i] = random.nextInt();
            return result;
        };

        final double t2 = new Benchmark_Timer<Integer[]>("integerArraysorter", (xs) -> Arrays.copyOf(xs, xs.length), Arrays::sort, null).runFromSupplier(integersSupplier, 100);
        for (TimeLogger timeLogger : timeLoggersLinearithmic) timeLogger.log(t2, n);
    }

    // This was added by a Student. Need to figure out what to do with it. What's different from the method with int parameter??
    private void sortIntegersByShellSort() throws IOException {
        if (isConfigBenchmarkIntegerSorter("shellsort")) {
            final Random random = new Random();
            int N = 1000;
            for (int j = 0; j < 10; j++) {
                Integer[] numbers = new Integer[N];
                for (int i = 0; i < N; i++) numbers[i] = random.nextInt();

                SortWithHelper<Integer> sorter = new ShellSort<>(5);
                runIntegerSortBenchmark(numbers, N, 1000, sorter, sorter::preProcess, timeLoggersLinearithmic);
                N = N * 2;
            }
        }
    }

//    private void dateSortBenchmark(Supplier<LocalDateTime[]> localDateTimeSupplier, LocalDateTime[] localDateTimes, Sort<ChronoLocalDateTime<?>> dateHuskySortSystemSort, String s, int i) {
//        logger.info(benchmarkFactory(s, dateHuskySortSystemSort::sort, dateHuskySortSystemSort::postProcess).runFromSupplier(localDateTimeSupplier, 100) + "ms");
//        // NOTE: this is intended to replace the run in the previous line. It should take the exact same amount of time.
//        runDateTimeSortBenchmark(LocalDateTime.class, localDateTimes, 100000, 100, i);
//    }

    private void sortIntegersByHeapSort() throws IOException {
        if (isConfigBenchmarkIntegerSorter("heapsort")) {
            final Random random = new Random();
            int N = 1000;
            for (int j = 0; j < 10; j++) {
                Integer[] numbers = new Integer[N];
                for (int i = 0; i < N; i++) numbers[i] = random.nextInt();

                BaseHelper<Integer> helper = new BaseHelper<>("HeapSort", N, Config.load(SortBenchmark.class));
                SortWithHelper<Integer> sorter = new HeapSort<>(helper);
                runIntegerSortBenchmark(numbers, N, 1000, sorter, sorter::preProcess, timeLoggersLinearithmic);
                N = N * 2;
            }
        }
    }

    private void sortStrings(Stream<Integer> wordCounts) {
        logger.info("Beginning String sorts");

        // NOTE: common words benchmark
//        benchmarkStringSorters(getWords("3000-common-words.txt", SortBenchmark::lineAsList), config.getInt("benchmarkstringsorters", "words", 1000), config.getInt("benchmarkstringsorters", "runs", 1000));

        // NOTE: Leipzig English words benchmarks (according to command-line arguments)
        wordCounts.forEach(this::doLeipzigBenchmarkEnglish);

        // NOTE: Leipzig Chines words benchmarks (according to command-line arguments)
//        doLeipzigBenchmark("zho-simp-tw_web_2014_10K-sentences.txt", 5000, 1000);
    }

    private void doLeipzigBenchmarkEnglish(int x) {
        String resource = "eng-uk_web_2002_" + (x < 50000 ? "10K" : x < 200000 ? "100K" : "1M") + "-sentences.txt";
        try {
            doLeipzigBenchmark(resource, x, Utilities.round(100000000 / minComparisons(x)));
        } catch (FileNotFoundException e) {
            logger.warn("Unable to find resource: " + resource, e);
        }
    }

    private void runMergeSortBenchmark(String[] words, int nWords, int nRuns, Boolean insurance, Boolean noCopy) {
        Config x = config.copy(MergeSort.MERGESORT, MergeSort.INSURANCE, insurance.toString()).copy(MergeSort.MERGESORT, MergeSort.NOCOPY, noCopy.toString());
        runStringSortBenchmark(words, nWords, nRuns, new MergeSort<>(nWords, x), timeLoggersLinearithmic);
    }

    private void doLeipzigBenchmark(String resource, int nWords, int nRuns) throws FileNotFoundException {
        benchmarkStringSorters(getWords(resource, SortBenchmark::getLeipzigWords), nWords, nRuns);
        if (isConfigBoolean(Config.HELPER, BaseHelper.INSTRUMENT))
            benchmarkStringSortersInstrumented(getWords(resource, SortBenchmark::getLeipzigWords), nWords, nRuns);
    }

    @SuppressWarnings("SameParameterValue")
    private void runDateTimeSortBenchmark(Class<?> tClass, ChronoLocalDateTime<?>[] dateTimes, int N, int m) throws IOException {
        final SortWithHelper<ChronoLocalDateTime<?>> sorter = new TimSort<>();
        @SuppressWarnings("unchecked") final SorterBenchmark<ChronoLocalDateTime<?>> sorterBenchmark = new SorterBenchmark<>((Class<ChronoLocalDateTime<?>>) tClass, (xs) -> Arrays.copyOf(xs, xs.length), sorter, dateTimes, m, timeLoggersLinearithmic);
        sorterBenchmark.run(N);
    }

    private boolean isConfigBenchmarkStringSorter(String option) {
        return isConfigBoolean("benchmarkstringsorters", option);
    }

    private boolean isConfigBenchmarkDateSorter(String option) {
        return isConfigBoolean("benchmarkdatesorters", option);
    }

    private boolean isConfigBenchmarkIntegerSorter(String option) {
        return isConfigBoolean("benchmarkintegersorters", option);
    }

    private boolean isConfigBoolean(String section, String option) {
        return config.getBoolean(section, option);
    }
}

package com.demo;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BytesRefHashSortBech {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: java BytesRefHashSortBech <log_file> <test_round> <reorderIds>");
            return;
        }

        String filename = args[0];
        int round = Integer.parseInt(args[1]);
        boolean reorderIds = Boolean.parseBoolean(args[2]);

        System.out.printf("read term in %s, run %d round, %s reorder ids\n", filename, round, reorderIds ? "with" : "without");

        BytesRefHashSortBech test = new BytesRefHashSortBech();
        List<BytesRef> testData = test.loadTermsFromFile(filename);
        test.bench(testData, round, reorderIds);
    }

    private static void bench(List<BytesRef> testData, int round, boolean reorderIds) {
        BytesRefHash hash = new BytesRefHash();
        int uniqueCount = 0;
        long start = System.nanoTime();
        for (BytesRef ref : testData) {
            int pos = hash.add(ref);
            if (pos >= 0) {
                uniqueCount += 1;
            }
        }
        long insertTimeNs = System.nanoTime() - start;
        System.out.printf("Inserted %d terms in %.2f ms, unique term %d\n", testData.size(), insertTimeNs / 1_000_000.0, uniqueCount);

        // tricky to keep original ids
        final int termCount = hash.size();
        int[] ids = hash.compact();
        if (termCount != uniqueCount) {
            throw new RuntimeException("termCount " + termCount + " !=  uniqueCount " + uniqueCount);
        }
        final int[] originIds = Arrays.copyOf(ids, ids.length);
        System.out.printf("term count %d, ids.length %d, hash.size %d\n", termCount, ids.length, hash.size());

        for (int r = 0; r < round; r++) {
            if (reorderIds) {
                System.out.println("reorder ids");
                for (int i = 0; i < termCount; i++) {
                    ids[i] = i;
                }
                Arrays.fill(ids, termCount, ids.length, -1);
            } else {
                // restore ids
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = originIds[i];
                }
            }

            start = System.nanoTime();
            int[] sorted = hash.sort();
            long sortTimeNs = System.nanoTime() - start;

            System.out.printf("sort %d unique terms in %.2f ms\n", sorted.length, sortTimeNs / 1_000_000.0);

            BytesRef ref = new BytesRef();
            System.out.println("first 10 terms");
            for (int i = 0; i < Math.min(termCount, 10); i++) {
                hash.get(sorted[i], ref);
                System.out.printf("\t#%d, %d, %s\n", i, sorted[i], new String(ref.bytes, ref.offset, ref.length));
            }

            System.out.println("last 10 terms");
            for (int i = Math.max(termCount - 10, 0); i < termCount; i++) {
                hash.get(sorted[i], ref);
                System.out.printf("\t#%d, %d, %s\n", i, sorted[i], new String(ref.bytes, ref.offset, ref.length));
            }
        }
    }

    private List<BytesRef> loadTermsFromFile(String filename) throws IOException {
        File file = new File(filename);
        List<BytesRef> terms = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (String term : line.split("\\s+")) {
                    if (!term.isEmpty()) {
                        terms.add(new BytesRef(term.getBytes(StandardCharsets.UTF_8)));
                    }
                }
            }
        }

        System.out.println("Loaded " + terms.size() + " terms from file");
        return terms;
    }
}


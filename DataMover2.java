import java.util.stream.IntStream;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

class DataMover2Result {
    public int count = 0,
               data = 0,
               forwarded = 0;

    public int getForwarded() { return forwarded; }
    public int getData() { return data; }
}

public class DataMover2 {
    public static AtomicInteger arrivalCount = new AtomicInteger(0),
                                totalSent = new AtomicInteger(0),
                                totalArrived = new AtomicInteger(0);

    public static ExecutorService pool;

    public static List<BlockingQueue<Integer>> queues = new ArrayList<>();

    public static List<Future<DataMover2Result>> moverResults = new ArrayList<>();

    public static List<Integer> discards = new ArrayList<>();

    public static void main (String [] args) {
        final int [] arguments;
        final int MAXSIZE = 100;

        if (args.length == 0) { arguments = new int[] {123, 111, 256, 404}; }
        else {
            arguments = new int[args.length];
            IntStream.range(0, args.length).forEach(x -> arguments[x] = Integer.parseInt(args[x]));
        }

        final int numOfThreads = arguments.length;

        IntStream.range(0, numOfThreads).forEach(x -> {
            queues.add(new ArrayBlockingQueue<>(MAXSIZE));
        });

        pool = Executors.newFixedThreadPool(MAXSIZE);

        IntStream.range(0, numOfThreads).forEach(i -> {
            moverResults.add (
                pool.submit(
                    () -> {
                        DataMover2Result innerResult = new DataMover2Result();
                        int count = 0;
                        while (count < 5*numOfThreads) {
                            String output = "total   " + count + "/" + 5*numOfThreads + " | #" + i + " ";
                            BlockingQueue<Integer> outputQ = queues.get(i);
                            BlockingQueue<Integer> inputQ = queues.get(i - 1 < 0 ? queues.size() - 1 : i - 1);

                            int x = ThreadLocalRandom.current().nextInt(0, 10_000);

                            totalSent.addAndGet(x);
                            outputQ.put(x); count++;
                            System.out.println(output + "sends " + x);

                            Integer received = null;
                            try {
                                received = inputQ.poll(ThreadLocalRandom.current().nextInt(300, 1000), TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {}

                            if (received == null) {
                                output += "got nothing...";
                                System.out.println(output);
                                continue;
                            } else if (received % numOfThreads == i) {
                                arrivalCount.incrementAndGet();
                                innerResult.count++;
                                innerResult.data += received;
                                output += "got " + received;
                            } else {
                                innerResult.forwarded++;
                                outputQ.put(received-1);
                                output += "forwarded " + (received-1) + " [" + (i+1 >= numOfThreads ? 0 : i+1)  + "]";
                            }
                            System.out.println(output);
                            Thread.sleep(arguments[i]);
                        }
                        return innerResult;
                    }
                )
            );
        });

        pool.shutdown();

        try { pool.awaitTermination(30, TimeUnit.SECONDS); }
        catch (InterruptedException e) {}

        totalArrived.addAndGet(
            moverResults.stream().map(
                    x -> {
                        try { return x.get(10, TimeUnit.SECONDS); }
                        catch (InterruptedException | TimeoutException | ExecutionException e) { return null; }
                    }
                ).mapToInt(
                    DataMover2Result::getData
                ).sum());

        totalArrived.addAndGet(
            moverResults.stream().map(
                    x -> {
                        try { return x.get(10, TimeUnit.SECONDS);}
                        catch (InterruptedException | TimeoutException | ExecutionException e) { return null; }
                    }
                ).mapToInt(
                    DataMover2Result::getForwarded
                ).sum());

        queues.stream().forEach(q -> { q.drainTo(discards); });

        int discarded = discards.stream().mapToInt(Integer::valueOf).sum();
        int combined = totalArrived.get() + discarded;

        System.out.println(discards + " = " + discarded);
        System.out.println(
            "sent " + totalSent.get() +
            (totalSent.get() == combined ? " === got " : " !== got ") +
            combined +
            " = " + totalArrived.get() +
            " + discarded " + discarded);
    }
}

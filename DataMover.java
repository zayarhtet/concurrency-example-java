import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataMover {
    public static int [] data = new int[3];
    public static List<Thread> movers = new ArrayList<>();

    public static void main (String [] args) {
        final int [] arguments;

        if (args.length == 0) { arguments = new int[] {123, 111, 256, 404}; }
        else {
            arguments = new int[args.length];
            IntStream.range(0, args.length).forEach(x -> arguments[x] = Integer.parseInt(args[x]));
        }

        final int moveTime = arguments[0];

        data = new int[arguments.length-1];

        IntStream.range(0, data.length).forEach(i -> {
            data[i] = i*1000;

            final int sleepTime = arguments[i+1];
            final int finalI = i;

            movers.add(new Thread(() -> {
                IntStream.range(0,10).forEach(x -> {
                    try { Thread.sleep(sleepTime); } catch (InterruptedException e) {}

                    synchronized(data) {
                        data[finalI] = data[finalI] - finalI;

                        System.out.println("#"+ finalI + ": data " + finalI + " == " + data[finalI]);
                    
                    }

                    try { Thread.sleep(moveTime); } catch (InterruptedException e) {}

                    synchronized(data) {
                        int nextI = finalI + 1 >= data.length ? 0 : finalI + 1;

                        data[nextI] += finalI;
                        System.out.println("#"+ finalI + ": data " + nextI + " == " + data[nextI]);
                    }
                });
            }, Integer.toString(i)));
        });

        IntStream.range(0, movers.size()).forEach(x -> movers.get(x).start());

        IntStream.range(0, movers.size()).forEach(x -> {
                try { movers.get(x).join(); } catch (InterruptedException e) {}
            }
        );

        System.out.println(Arrays.stream(data)
                                    .mapToObj(Integer::valueOf)
                                    .collect(Collectors.toList()));
    }
}

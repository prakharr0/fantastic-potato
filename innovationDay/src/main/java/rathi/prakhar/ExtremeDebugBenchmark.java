package rathi.prakhar;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtremeDebugBenchmark {

    public static void main(String[] args) {

        ExecutorService pool = Executors.newFixedThreadPool(4);

        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < 50; i++) {

            tasks.add(
                    CompletableFuture.runAsync(() -> {
                        try {

                            int userId = new Random().nextInt(10);

                            User user = CacheLayer.getUser(userId);

                            byte[] data = Serializer.serialize(user);

                            User restored = Serializer.deserialize(data);

                            ScoreService.computeScore(restored);

                        } catch (Exception e) {
                            throw new RuntimeException("Worker failure", e);
                        }

                    }, pool)
            );
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

        pool.shutdown();

        System.out.println("Completed");
    }
}

class CacheLayer {

    private static final Map<Integer, User> CACHE = new HashMap<>();

    public static User getUser(int id) {

        User user = CACHE.get(id);

        if (user == null) {

            user = Database.loadUser(id);

            CACHE.put(id, user);
        }

        return user;
    }
}

class Database {

    public static User loadUser(int id) {

        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) {}

        return new User(id, "User-" + id, new Random().nextInt(100));
    }
}

class ScoreService {

    private static final AtomicInteger GLOBAL_COUNTER = new AtomicInteger();

    public static int computeScore(User user) {

        int base = user.getScore();

        int modifier = GLOBAL_COUNTER.incrementAndGet();

        return MathEngine.heavyScore(base, modifier);
    }
}

class MathEngine {

    public static int heavyScore(int base, int modifier) {

        try {

            int value = recursive(base, modifier, 3);

            return value;

        } catch (Exception e) {

            RuntimeException wrapper = new RuntimeException("Score computation failed");

            wrapper.addSuppressed(e);

            throw wrapper;
        }
    }

    private static int recursive(int a, int b, int depth) {

        if (depth == 0) {

            Map<Integer,Integer> lookup = new HashMap<>();

            lookup.put(100,2);

            Integer divisor = lookup.get(b);

            return a / divisor;
        }

        return recursive(a + b, b, depth - 1);
    }
}

class Serializer {

    public static byte[] serialize(User user) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        ObjectOutputStream out = new ObjectOutputStream(bos);

        out.writeObject(user);

        return bos.toByteArray();
    }

    public static User deserialize(byte[] data)
            throws IOException, ClassNotFoundException {

        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        ObjectInputStream in = new ObjectInputStream(bis);

        return (User) in.readObject();
    }
}

class User implements Serializable {

    private int id;
    private String name;
    private int score;

    public User(int id, String name, int score) {
        this.id = id;
        this.name = name;
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
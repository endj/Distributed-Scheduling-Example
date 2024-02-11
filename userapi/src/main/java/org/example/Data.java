package org.example;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.example.Config.MAPPER;

public class Data {
    public static final Random RNG = new Random(1337);
    public static final List<Main.User> USERS = userList();
    public static final Map<Long, Main.UserDetailed> USER_PROFILE = userProfiles(USERS);
    public static final String USERS_RESPONSE;

    static {
        try {
            USERS_RESPONSE = MAPPER.writeValueAsString(USERS);
        } catch (IOException e) {
            System.err.print(e.getMessage());
            System.exit(1);
            throw null;
        }
    }


    @SuppressWarnings("SameParameterValue")
    private static Map<Long, Main.UserDetailed> userProfiles(List<Main.User> users) {
        return users.stream().map(user -> new Main.UserDetailed(user.id(), user.userName(),
                randomString("email"),
                randomString("title"),
                randomString("role")
        )).collect(toMap(Main.UserDetailed::id, identity()));
    }

    private static List<Main.User> userList() {
        return IntStream.range(0, 2_000)
                .mapToObj(i -> new Main.User(i, randomString("username"))).toList();
    }

    private static String randomString(String prefix) {
        byte[] bytes = new byte[30];
        RNG.nextBytes(bytes);
        var encoded = Base64.getEncoder().encode(bytes);
        return prefix + "-" + new String(encoded, UTF_8);
    }
}

package io.github.charliechaplim.minefables.client;

import io.github.charliechaplim.minefables.network.WilliamStatusPayload;

/** Cache de HUD; dados nao sao utilizados para autorizar habilidades. */
public final class ClientWilliamState {
    private static int level = 1;
    private static boolean ultimate;
    private static int tier;
    private static boolean prepared;
    private static int seconds;

    private ClientWilliamState() {}

    public static void accept(WilliamStatusPayload payload) {
        level = Math.max(1, Math.min(5, payload.level()));
        ultimate = payload.ultimate();
        tier = Math.max(0, Math.min(3, payload.resonanceTier()));
        prepared = payload.impetuoPrepared();
        seconds = Math.max(0, payload.remainingSeconds());
    }

    public static void clear() {
        level = 1;
        ultimate = false;
        tier = 0;
        prepared = false;
        seconds = 0;
    }

    public static int level() { return level; }
    public static boolean ultimate() { return ultimate; }
    public static int tier() { return tier; }
    public static boolean prepared() { return prepared; }
    public static int seconds() { return seconds; }
}

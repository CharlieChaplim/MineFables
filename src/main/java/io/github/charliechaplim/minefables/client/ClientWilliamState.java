package io.github.charliechaplim.minefables.client;

import io.github.charliechaplim.minefables.network.WilliamStatusPayload;

/** Estado da HUD; o servidor valida habilidades. */
public final class ClientWilliamState {
    private static int level=1,tier,seconds,temperature;
    private static boolean ultimate,prepared;
    private static int[] normal=new int[6], over=new int[4];
    private ClientWilliamState(){}
    public static void accept(WilliamStatusPayload p) {
        level=Math.max(1,Math.min(6,p.level()));tier=Math.max(0,Math.min(3,p.resonanceTier()));
        ultimate=p.ultimate();prepared=p.impetuoPrepared();seconds=Math.max(0,p.remainingSeconds());
        temperature=Math.max(0,Math.min(4,p.temperature()));
        normal=p.normalCooldowns().clone();over=p.ultimateCooldowns().clone();
    }
    public static void clear(){level=1;tier=seconds=temperature=0;ultimate=prepared=false;normal=new int[6];over=new int[4];}
    public static int level(){return level;}
    public static boolean ultimate(){return ultimate;}
    public static boolean prepared(){return prepared;}
    public static int tier(){return tier;}
    public static int seconds(){return seconds;}
    public static int temperature(){return temperature;}
    public static int cooldownTicks(int index,boolean ultimateMode){
        int[] values=ultimateMode?over:normal;
        return index>=0&&index<values.length?values[index]:0;
    }
}

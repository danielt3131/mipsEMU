package io.github.danielt3131.mipsemu;

public class Reference {
    public static final int HEX_MODE = 1;
    public static final int BINARY_MODE = 0;
    public static final int DECIMIAL_MODE = 2;
    public static final int CREATE_OUTPUTSTREAM = 24;

    //  Registers
    public static final int REGISTER_ZERO = 0;
    public static final int REGISTER_V0 = 1;
    public static final int REGISTER_V1 = 2;
    public static final int REGISTER_A0 = 3;
    public static final int REGISTER_A1 = 4;
    public static final int REGISTER_A2 = 5;
    public static final int REGISTER_A3 = 6;
    public static final int REGISTER_T0 = 7;
    public static final int REGISTER_T1 = 8;
    public static final int REGISTER_T2 = 9;
    public static final int REGISTER_T3 = 10;
    public static final int REGISTER_T4 = 11;
    public static final int REGISTER_T5 = 12;
    public static final int REGISTER_T6 = 13;
    public static final int REGISTER_T7 = 14;
    public static final int REGISTER_T8 = 15;
    public static final int REGISTER_T9 = 16;

    public static final int REGISTER_S0 = 17;
    public static final int REGISTER_S1 = 18;
    public static final int REGISTER_S2 = 19;
    public static final int REGISTER_S3 = 20;
    public static final int REGISTER_S4 = 21;
    public static final int REGISTER_S5 = 22;
    public static final int REGISTER_S6 = 23;
    public static final int REGISTER_S7 = 24;
    public static final int REGISTER_K0 = 25;
    public static final int REGISTER_K1 = 26;
    public static final int REGISTER_GP = 27;
    public static final int REGISTER_SP = 28;
    public static final int REGISTER_FP = 29;
    public static final int REGISTER_RA = 30;
    public static final int REGISTER_AT = 31;

    // The name of each register index to the corresponding spot
    public static final String[] registerNames = {"$zero", "$v0", "$v1", "$a0", "$a1", "$a2", "$t0", "$t1", "$t2",
            "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9", "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
            "$k0", "$k1", "$gp", "$sp", "$fp", "$ra", "$at"};

}

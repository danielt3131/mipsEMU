package io.github.danielt3131.mipsemu.machine;

public class MipsMachine {

    //Register Variables
    private int zero = 0;
    private int hi, lo;
    private int pc, ra;
    private int t0, t1, t2, t3, t4, t5, t6, t7, t8, t9;
    private int a0, a1, a2, a3;
    private int v0, v1;
    private int s0, s1, s2, s3, s4, s5, s6, s7;
    private int at, k0, k1, gp, fp, sp;

    private byte[] memory;

    public MipsMachine(int memorySize)
    {
        memory = new byte[memorySize];
    }

}

package io.github.danielt3131.mipsemu.machine;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import io.github.danielt3131.mipsemu.R;

public class StateManager
{

    private static ArrayList<Byte> byteArrayList = new ArrayList<>();

    /*
    .mst file breakdown
    State (Header)
    first 128 bytes determine what is in register
    pc (4 bytes)
    hi (4 bytes)
    lo (4 bytes)
    amount of memory (4 bytes)
    size of stack (4 bytes)
    ...
    the stack
    ...
    size of text (4 bytes)
    ...
    the text
    ...
     */
    public static void toFile(OutputStream outputStream, int[] register, int pc, int hi, int lo, byte[] memory) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("StateManager", "Saving state");
                try {
                    createByteArray(register, pc, hi, lo, memory);
                    byte[] b = new byte[byteArrayList.size()];
                    for (int i = 0; i < b.length; i++) {
                        b[i] = byteArrayList.get(i);
                    }
                    outputStream.write(b, 6, b.length);
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Log.e("StateManager", e.getMessage());
                }
            }
        });
        thread.start();
    }


    private static byte[] intToBytes(int i)
    {
        byte b1 = (byte)grabLeftBits(i , 8);
        byte b2 = (byte)grabRightBits(grabLeftBits(i, 16),8);
        byte b3 = (byte)grabRightBits(grabLeftBits(i, 24),8);
        byte b4 = (byte)grabRightBits(i,8);

        return new byte[]{b1, b2, b3, b4};
    }

    /**
     * grabs right bits from an int
     * @param data the integer to grab bits from
     * @param n the amount of bits to grab
     * @return the grabbed bits
     */
    private static int grabRightBits(int data, int n)
    {
        int mask = (int)Math.pow(2,n) - 1;
        return data & mask;
    }

    /**
     * grabs left bits from an int
     * @param data the integer to grab bits from
     * @param n the amount of bits to grab
     * @return the grabbed bits
     */
    private static int grabLeftBits(int data, int n)
    {
        return data >>> 32-n;
    }

    private static void addToByteArray(byte b)
    {
        byteArrayList.add(b);
        Log.d("Writing File", String.format("added %d to file", b));
    }

    private static void addToByteArray(byte[] b)
    {
        for(byte c : b)
        {
            addToByteArray(c);
        }
    }

    private static void createByteArray(int[] register, int pc, int hi, int lo, byte[] memory)
    {
        //Creating file

        //Creating registers section
        for(int i : register)
        {
            addToByteArray(intToBytes(i));
        }

        addToByteArray(intToBytes(pc));
        addToByteArray(intToBytes(hi));
        addToByteArray(intToBytes(lo));

        //Amount of memory section

        addToByteArray(intToBytes(memory.length));

        //Calculating length of text and stack
        int sizeOfText, sizeOfStack;

        int mid = memory[memory.length/2];

        //text

        while(memory[mid] == 0)
        {
            mid--;
        }
        sizeOfText = mid + 1;

        //Stack

        mid = memory[memory.length / 2];

        while(memory[mid] == 0)
        {
            mid++;
        }
        sizeOfStack = memory.length - mid + 1;

        //Now we have sized partitions of the memory

        //adding memory partitions to bytearray
        addToByteArray(intToBytes(sizeOfText));
        for(int i = 0; i < sizeOfText; i++)
        {
            addToByteArray(memory[i]);
        }

        addToByteArray(intToBytes(sizeOfStack));
        for(int i = memory.length - 1; i > memory.length - sizeOfStack; i--)
        {
            addToByteArray(memory[i]);
        }


    }
}

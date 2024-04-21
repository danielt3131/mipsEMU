/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 or later.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.danielt3131.mipsemu.machine;

import android.util.Log;

import androidx.annotation.LongDef;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import io.github.danielt3131.mipsemu.R;

public class StateManager
{

    private static ArrayList<Byte> byteArrayList = new ArrayList<>();

    /*
    file breakdown
    "State" (Header)
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
                createByteArray(register, pc, hi, lo, memory);
                PrintWriter write = new PrintWriter(outputStream);
                write.println("State");
                for (int i = 0; i < byteArrayList.size(); i++) {
                    write.println(byteArrayList.get(i));
                }
                write.flush();
                write.close();
                Log.d("StateManager", "State saved");
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
        //Log.d("Writing File", String.format("added %d to file", b));
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

        //text

        int pointer = memory.length/2;// start at mid point
        Log.d("mid point", String.valueOf(pointer));

        try {
            while (memory[pointer] == 0) {
                pointer--;
            }
            sizeOfText = pointer + 1;
        } catch (ArrayIndexOutOfBoundsException e)
        {
            Log.d("Text Empty", "Text Empty");
            sizeOfText = 0;
        }
        Log.d("sizeOfText", "size: " + sizeOfText);

        //Stack

        pointer = memory.length/2; // start at mid point
        Log.d("mid point", String.valueOf(pointer));

        try {
            while (memory[pointer] == 0) {
                pointer++;
            }
            sizeOfStack = memory.length - pointer;
        } catch(ArrayIndexOutOfBoundsException e)
        {
            Log.d("Stack Empty", "Stack Empty");
            sizeOfStack = 0;
        }
        Log.d("sizeOfStack", "size: " + sizeOfStack);

        //Now we have sized partitions of the memory

        //adding memory partitions to bytearray
        Log.d("Writing Text", "Starting Text Writing");
        Log.d("Writing Text", "Starting Text Size");
        addToByteArray(intToBytes(sizeOfText));
        Log.d("Writing Text", "Writing Text");
        for(int i = 0; i < sizeOfText; i++)
        {
            Log.d("Text Partition", ( i + 1) + " of " + sizeOfText);
            addToByteArray(memory[i]);
        }

        Log.d("Writing Stack", "Starting Stack Writing");
        Log.d("Writing Stack", "Starting Stack Size");
        addToByteArray(intToBytes(sizeOfStack));
        Log.d("Writing Stack", "Writing Stack");
        for(int i = memory.length - 1; i >= memory.length - sizeOfStack; i--)
        {
            Log.d("Stack Partition", i + " of " + (memory.length - sizeOfStack));
            addToByteArray(memory[i]);
        }
        Log.d("Writing File", "Byte Array Completed");


    }
}

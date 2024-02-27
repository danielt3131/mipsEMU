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

/**
 * A representation of a 4 byte mips word
 */
public class Code extends Word
{

    //Protected so that the MipsMachine class can access but nothing else
    protected String opCode;
    protected String val1;
    protected String val2;
    protected String val3;

    /**
     * Creates a word
     * @param opCode the word's opcode
     * @param val1 the first argument of the word
     * @param val2 the second argument
     * @param val3 the third argument
     */
    protected Code(String opCode, String val1, String val2, String val3)
    {
        this.opCode = opCode;
        this.val1 = val1;
        this.val2 = val2;
        this.val3 = val3;
    }

    /**
     * Creates a word
     * @param opCode the word's opcode
     * @param val1 the first argument of the word
     * @param val2 the second argument
     */
    protected Code(String opCode, String val1, String val2)
    {
        this.opCode = opCode;
        this.val1 = val1;
        this.val2 = val2;
    }

    /**
     * Creates a word
     * @param opCode the word's opcode
     * @param val1 the argument of the word
     */
    protected Code(String opCode, String val1)
    {
        this.opCode = opCode;
        this.val1 = val1;
    }

    /**
     * Creates empty word
     */
    protected  Code()
    {

    }



}

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
 * Essentially an integer wrapper
 */

public class Word
{

    int value;

    /**
     * Creates empty word
     */
    protected Word()
    {

    }

    /**
     * Creates word with value
     * @param value the value to make the word
     */
    protected Word(int value)
    {
        this.value = value;
    }

}

package io.github.danielt3131.mipsemu.machine;

public class CacheBlock {

    int tag;
    byte data;
    private boolean valid;

    public CacheBlock()
    {
        tag = 0;
        data = 0;
        valid = false;
    }

    public void setValid()
    {
        valid = true;
    }

    public void setInvalid()
    {
        valid = false;
    }

    public boolean isValid()
    {
        return valid;
    }
}

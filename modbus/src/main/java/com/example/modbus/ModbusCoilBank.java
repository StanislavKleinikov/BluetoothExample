package com.example.modbus;

public class ModbusCoilBank
{
    private int coils_size;
    private boolean[] coils;

    public ModbusCoilBank(int paramInt)
    {
        if (paramInt < 1) {
            this.coils_size = 1;
        } else if (paramInt > 65535) {
            this.coils_size = 65535;
        } else {
            this.coils_size = paramInt;
        }
        this.coils = new boolean[this.coils_size];
        for (int i = 0; i < this.coils_size; i++) {
            this.coils[i] = false;
        }
    }

    public int getNumberCoils()
    {
        return this.coils_size;
    }

    public boolean getCoil(int paramInt)
    {
        return this.coils[paramInt];
    }

    public void setCoil(int paramInt, boolean paramBoolean)
    {
        this.coils[paramInt] = paramBoolean;
    }

    public boolean[] getCoils(int paramInt1, int paramInt2)
    {
        boolean[] arrayOfBoolean = new boolean[paramInt2];
        for (int i = 0; i < paramInt2; i++) {
            arrayOfBoolean[i] = this.coils[(i + paramInt1)];
        }
        return arrayOfBoolean;
    }

    public void setCoils(int paramInt, boolean[] paramArrayOfBoolean)
    {
        for (int i = 0; i < paramArrayOfBoolean.length; i++) {
            this.coils[(paramInt + i)] = paramArrayOfBoolean[i];
        }
    }
}

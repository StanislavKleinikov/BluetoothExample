package com.example.modbus;

public class ModbusRegisterBank
{
    private int register_size;
    private int[] registers;

    public ModbusRegisterBank(int paramInt)
    {
        if (paramInt < 1) {
            this.register_size = 1;
        } else if (paramInt > 65535) {
            this.register_size = 65535;
        } else {
            this.register_size = paramInt;
        }
        this.registers = new int[this.register_size];
        for (int i = 0; i < this.register_size; i++) {
            this.registers[i] = 0;
        }
    }

    public int getNumberRegisters()
    {
        return this.register_size;
    }

    public int getRegister(int paramInt)
    {
        return this.registers[paramInt];
    }

    public void setRegister(int paramInt1, int paramInt2)
    {
        this.registers[paramInt1] = paramInt2;
    }

    public int[] getRegisters(int paramInt1, int paramInt2)
    {
        int[] arrayOfInt = new int[paramInt2];
        for (int i = 0; i < paramInt2; i++) {
            arrayOfInt[i] = this.registers[(i + paramInt1)];
        }
        return arrayOfInt;
    }

    public void setRegister(int paramInt, int[] paramArrayOfInt)
    {
        for (int i = 0; i < paramArrayOfInt.length; i++) {
            this.registers[(paramInt + i)] = paramArrayOfInt[i];
        }
    }
}

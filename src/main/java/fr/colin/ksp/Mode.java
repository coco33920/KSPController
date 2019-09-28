package fr.colin.ksp;

public enum Mode {

    NO_DECODE(0x00),
    DECODE_0(0x01),
    DECODE_30(0x0F),
    DECODE(0xFF);

    int i;

    Mode(int i) {
       this.i = i;
    }

    public int getI() {
        return i;
    }
}

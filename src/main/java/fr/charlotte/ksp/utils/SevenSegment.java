package fr.charlotte.ksp.utils;

import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;

/*
hugely inspired from https://github.com/sharetop/max7219-java
 */
public class SevenSegment {

    private byte[] data;

    public static class Constants {
        public static byte MAX7219_REG_NOOP = 0x0;
        public static byte MAX7219_REG_DIGIT0 = 0x1;
        public static byte MAX7219_REG_DIGIT1 = 0x2;
        public static byte MAX7219_REG_DIGIT2 = 0x3;
        public static byte MAX7219_REG_DIGIT3 = 0x4;
        public static byte MAX7219_REG_DIGIT4 = 0x5;
        public static byte MAX7219_REG_DIGIT5 = 0x6;
        public static byte MAX7219_REG_DIGIT6 = 0x7;
        public static byte MAX7219_REG_DIGIT7 = 0x8;
        public static byte MAX7219_REG_DECODEMODE = 0x9;
        public static byte MAX7219_REG_INTENSITY = 0xA;
        public static byte MAX7219_REG_SCANLIMIT = 0xB;
        public static byte MAX7219_REG_SHUTDOWN = 0xC;
        public static byte MAX7219_REG_DISPLAYTEST = 0xF;
    }

    private static HashMap<Character, Byte> code = new HashMap<>();

    static {
        code.put('0', (byte) 0b00000000);
        code.put('1', (byte) 0b00000001);
        code.put('2', (byte) 0b00000010);
        code.put('3', (byte) 0b00000011);
        code.put('4', (byte) 0b00000100);
        code.put('5', (byte) 0b00000101);
        code.put('6', (byte) 0b00000110);
        code.put('7', (byte) 0b00000111);
        code.put('8', (byte) 0b00001000);
        code.put('9', (byte) 0b00001001);
        code.put('-', (byte) 0b00001010);
        code.put('E', (byte) 0b00001011);
        code.put('H', (byte) 0b00001100);
        code.put('L', (byte) 0b00001101);
        code.put('P', (byte) 0b00001110);
        code.put(' ', (byte) 0b00001111);
    }

    public static byte getByteFromCaracter(Character c, boolean decimal) {
        byte b = code.get(c);
        if (decimal)
            b = (byte) (b + 0b10000000);
        return b;
    }

    private SpiDevice ss;

    private short daisychaining;

    public SevenSegment(short daisychaining) throws IOException {
        this.daisychaining = daisychaining;
        ss = SpiFactory.getInstance(SpiChannel.CS0, SpiDevice.DEFAULT_SPI_SPEED, SpiDevice.DEFAULT_SPI_MODE);
        clear();
        write(Constants.MAX7219_REG_SCANLIMIT, (byte) 0x7);
        write(Constants.MAX7219_REG_DECODEMODE, (byte) 0xFF);
        write(Constants.MAX7219_REG_DISPLAYTEST, (byte) 0x0);
        write(Constants.MAX7219_REG_SHUTDOWN, (byte) 0x1);

        //0 is 0x0
    }

    public static boolean isInteger(Double doble) {
        return (doble - doble.intValue()) == 0;
    }


    public void showNumber(double number) throws IOException {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat decimalFormat = new DecimalFormat("##0.###E0", symbols);
        String string = decimalFormat.format(number);
        /*if (isInteger(number)) {
           while (string.endsWith("0")){
               string = string.substring(0, string.length()-1);
           }
        }*/
        String onlyDigit = string.replace(".", "");
        byte start = (byte) (Constants.MAX7219_REG_DIGIT0 + (onlyDigit.length()-1));
        for (int i = 0; i < string.toCharArray().length; i++) {
            if (string.charAt(i) == '.') {
                start++;
                continue;
            }
            boolean decimal = false;
            if ((i + 1) < string.toCharArray().length) {
                if (string.charAt(i + 1) == '.') {
                    decimal = true;
                }
            }
            write((byte) (start - i), getByteFromCaracter(string.charAt(i), decimal));
        }
    }

    public void write(byte[] data) throws IOException {
        this.data = data;
        this.ss.write(data);
    }

    public void clear() throws IOException {
        for (int i = 0; i < 8; i++) {
            write((byte) (Constants.MAX7219_REG_DIGIT0 + i), (byte) 0x00);
        }
    }

    public void setDecodeMode(Mode mode) throws IOException {
        write(Constants.MAX7219_REG_DECODEMODE, (byte) mode.getI());
    }

    public void setIntensity(byte intensity) throws IOException {
        if (intensity < 0x0)
            intensity = 0x0;
        if (intensity > 0xF)
            intensity = 0xF;
        write(Constants.MAX7219_REG_INTENSITY, intensity);
    }

    public void write(byte register, byte data) throws IOException {
        byte[] buffer = new byte[2 * this.daisychaining];

        for (int i = 0; i < buffer.length; i += 2) {
            buffer[i] = register;
            buffer[i + 1] = data;
        }

        write(buffer);
    }

}

package com.example.wilsontsang.test2;


public final class CommandCodes {
    public static final class Arduino {
        public static final int CUR_TEMPERATURE = 10;
        public static final int CUR_HUMIDITY = 11;
        public static final int CUR_SPEED = 12;
        public static final int CUR_TEMPMODE = 13;
        public static final int CUR_OPMODE = 14;
    }

    public static final class Application {
        public static final int SET_OPMODE = 50;
        public static final int SET_TEMPMODE = 51;
        public static final int SET_TEMP = 52;
        public static final int SET_FANSPEED = 53;
        public static final int MOVE_FAN = 54;
    }
}

////List of CommandCodes
//public class CommandCodes{
//
//    //Op Codes sent by Arduino
//    public enum ArduinoCodes {
//
//        //Arduino Op Codes
//        CURTEMPERATURE(10),
//        CURHUMIDITY(11),
//        CURSPEED(12),
//        CURTEMPMODE(13),
//        CUROPMODE(14);
//
//        private int numVal;
//
//        ArduinoCodes(int numVal) {
//            this.numVal = numVal;
//        }
//
//        public int getNumVal() {
//            return numVal;
//        }
//    }
//
//    //Op Codes sent by Application
//    public enum ApplicationCodes {
//
//        //Application Op Codes
//        SETOPMODE(50),
//        SETTEMPMODE(51),
//        SETTEMP(52),
//        SETFANSPEED(53),
//        MOVEFAN(54);
//
//
//        //Arduino Op Codes
//        private int numVal;
//
//        ApplicationCodes(int numVal) {
//            this.numVal = numVal;
//        }
//
//        public int getNumVal() {
//            return numVal;
//        }
//    }
//
//}





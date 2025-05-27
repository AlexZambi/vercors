package vct.parsers.transform.systemctocol.util;

public enum SimpleOperation {
    ADD {
        @Override
        public String toString() {return "+";}
    },
    SUB {
        @Override
        public String toString() {return "-";}
    },
    MUL {
        @Override
        public String toString() {return "*";}
    },
    DIV {
        @Override
        public String toString() {return "/";}
    },
    MIN {
        @Override
        public String toString() {return "min";}
    },
    MAX {
        @Override
        public String toString() {return "max";}
    }    
}

package vct.parsers.transform.systemctocol.util;

/**
 * Enum representing types of comparison operators.
 */
public enum Comparison {
    EQ {
        @Override
        public String toString() {return "=";}
    },
    NEQ {
        @Override
        public String toString() {return "!=";}
    },
    LESSER {
        @Override
        public String toString() {return "<";}
    },
    LESSER_EQ {
        @Override
        public String toString() {return "<=";}
    },
    GREATER {
        @Override
        public String toString() {return ">";}
    },
    GREATER_EQ {
        @Override
        public String toString() {return ">=";}
    },
}
package org.netbeans.lib.profiler.results.cpu;

import java.util.logging.Logger;

public abstract class MethodInfoMapper {
    final static protected Logger LOGGER = Logger.getLogger(MethodInfoMapper.class.getName());
    
    public static final MethodInfoMapper DEFAULT = new MethodInfoMapper() {

        @Override
        public String getInstrMethodClass(int methodId) {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return "<UNKNOWN>";
        }

        @Override
        public String getInstrMethodName(int methodId) {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return "<UNKNOWN>";
        }

        @Override
        public String getInstrMethodSignature(int methodId) {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return "<UNKNOWN>";
        }

        @Override
        public int getMaxMethodId() {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return 0;
        }

        @Override
        public int getMinMethodId() {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return 0;
        }
    };

    public abstract String getInstrMethodClass(int methodId);

    public abstract String getInstrMethodName(int methodId);

    public abstract String getInstrMethodSignature(int methodId);

    public abstract int getMinMethodId();

    public abstract int getMaxMethodId();

    public void lock(boolean mutable) {
        // default no-op
    }

    public void unlock() {
        // default no-op
    }
}

package de.ownmods.cropreplant.core;

/** Single-crop transaction. Called only on the integrated server thread. */
public final class ReplantTransaction {
    private ReplantTransaction() { }
    public enum Result { PLANTED, BLOCKED, BAD_SOIL, NO_SEED, FAILED }
    public interface Access {
        boolean emptyAndLoaded();
        boolean suitableSoil();
        boolean hasSeed();
        boolean plant();
        boolean consumeSeed();
        /** Must only remove our just-planted crop; never overwrite an unrelated block. */
        boolean rollback();
    }
    public static Result run(boolean creative, Access a) {
        if(!a.emptyAndLoaded())return Result.BLOCKED;
        if(!a.suitableSoil())return Result.BAD_SOIL;
        if(!creative && !a.hasSeed())return Result.NO_SEED;
        try {
            if(!a.plant()) { a.rollback(); return Result.FAILED; }
            if(!creative && !a.consumeSeed()) { a.rollback(); return Result.FAILED; }
            return Result.PLANTED;
        } catch(RuntimeException failure) {
            // A placement callback can throw after writing the young crop.
            try { a.rollback(); } catch(RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            throw failure;
        }
    }
}

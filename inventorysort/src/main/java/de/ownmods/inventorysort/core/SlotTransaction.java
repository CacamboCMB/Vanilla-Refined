package de.ownmods.inventorysort.core;
import java.util.*;
public final class SlotTransaction {
    private SlotTransaction() { }
    public enum Result { APPLIED, STALE, REJECTED, ROLLED_BACK }
    public interface Access<T> {
        T get(int index);
        boolean equal(T a,T b);
        boolean maySet(int index,T item);
        void set(int index,T item);
    }
    /** Caller owns the server thread. Full preflight before the first write. */
    public static <T> Result commit(List<T> before,List<T> after,Access<T> access) {
        if(before.size()!=after.size())throw new IllegalArgumentException("Size mismatch");
        for(int i=0;i<before.size();i++)if(!access.equal(before.get(i),access.get(i)))return Result.STALE;
        for(int i=0;i<after.size();i++)if(!access.maySet(i,after.get(i)))return Result.REJECTED;
        try {
            for(int i=0;i<after.size();i++)access.set(i,after.get(i));
            for(int i=0;i<after.size();i++)if(!access.equal(after.get(i),access.get(i)))throw new IllegalStateException("Write not retained");
            return Result.APPLIED;
        } catch(RuntimeException failure) {
            RuntimeException rollbackFailure=null;
            for(int i=0;i<before.size();i++)try { access.set(i,before.get(i)); }
                catch(RuntimeException e) { if(rollbackFailure==null)rollbackFailure=e;else rollbackFailure.addSuppressed(e); }
            for(int i=0;i<before.size();i++)if(!access.equal(before.get(i),access.get(i))) {
                var error=new IllegalStateException("Rollback did not restore inventory",failure);
                if(rollbackFailure!=null)error.addSuppressed(rollbackFailure);throw error;
            }
            return Result.ROLLED_BACK;
        }
    }
}

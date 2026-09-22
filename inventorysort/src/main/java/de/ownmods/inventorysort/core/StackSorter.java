package de.ownmods.inventorysort.core;
import java.util.*;

/** Pure bounded sorting/packing. Inputs are never mutated. Equal item IDs alone NEVER merge. */
public final class StackSorter {
    private StackSorter() { }
    public interface Ops<T> {
        boolean empty(T value);
        int count(T value);
        int maximum(T value);
        boolean sameKind(T a,T b);
        String orderKey(T value);
        T withCount(T value,int count);
        T emptyValue();
    }
    private static final class Group<T> {
        final T prototype; long count;
        Group(T p,int count){prototype=p;this.count=count;}
    }
    public static <T> List<T> sort(List<T> input,boolean merge,Ops<T> ops) {
        return sort(input, merge, ops, ops::orderKey);
    }
    public static <T> List<T> sort(List<T> input, boolean merge, Ops<T> ops, java.util.function.Function<T, String> key) {
        Objects.requireNonNull(key);
        if(input.size()>108)throw new IllegalArgumentException("Oversized inventory");
        var groups=new ArrayList<Group<T>>();
        for(T value:input) {
            Objects.requireNonNull(value);
            if(ops.empty(value))continue;
            int count=ops.count(value),max=ops.maximum(value);
            if(count<=0 || max<=0 || count>max)throw new IllegalArgumentException("Invalid/overstacked item");
            Group<T> match=null;
            if(merge)for(var g:groups)if(ops.sameKind(g.prototype,value) && ops.maximum(g.prototype)==max){match=g;break;}
            if(match==null)groups.add(new Group<>(value,count));else match.count=Math.addExact(match.count,count);
        }
        // Stable order for distinct component variants of the same named item.
        var keys = new IdentityHashMap<Group<T>, String>();
        for (var group : groups) keys.put(group, Objects.requireNonNull(key.apply(group.prototype)));
        groups.sort(Comparator.comparing(keys::get));
        var result=new ArrayList<T>();
        for(var g:groups) {
            long left=g.count;int max=ops.maximum(g.prototype);
            while(left>0) {
                int count=(int)Math.min(left,max);result.add(ops.withCount(g.prototype,count));left-=count;
                if(result.size()>input.size())throw new IllegalStateException("Insufficient capacity");
            }
        }
        while(result.size()<input.size())result.add(ops.emptyValue());
        return List.copyOf(result);
    }
}

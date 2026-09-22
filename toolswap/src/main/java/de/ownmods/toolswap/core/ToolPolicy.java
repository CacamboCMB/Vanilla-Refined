package de.ownmods.toolswap.core;

/** Pure quality policy shared by runtime selection and regression tests. */
public final class ToolPolicy {
    public enum Mode { SAME("same"), SAME_OR_WORSE("same_or_worse"), BEST("best");
        private final String token; Mode(String token){this.token=token;} public String token(){return token;}
        public static Mode parse(String s){for(var v:values())if(v.token.equals(s))return v;return SAME;}
    }
    private ToolPolicy() { }
    public static boolean qualityAllowed(int candidate,int reference,Mode mode){
        if(candidate<0)return false;
        if(mode==Mode.BEST||reference<0)return true;
        return mode==Mode.SAME?candidate==reference:candidate<=reference;
    }
    public static int betterScore(int quality,int durability){return quality*1_000_000+Math.max(0,durability);}
}

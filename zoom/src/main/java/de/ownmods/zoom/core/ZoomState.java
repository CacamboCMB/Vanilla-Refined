package de.ownmods.zoom.core;
/** Pure state machine. Never writes options.txt or permanently changes the player's base FOV. */
public final class ZoomState {
    private boolean latched;
    private boolean toggleMode;
    private double magnification=1;
    private long lastNanos;
    public void reset(){latched=false;magnification=1;lastNanos=0;}
    public double update(boolean enabled,boolean contextValid,boolean toggle,boolean held,int clicks,
                         boolean strong,boolean smooth,long now) {
        if(!enabled || !contextValid){reset();toggleMode=toggle;return 1;}
        if(toggle!=toggleMode){latched=false;toggleMode=toggle;}
        if(toggle && (clicks&1)!=0)latched=!latched;
        boolean active=toggle?latched:held;
        double target=active?(strong?8:4):1;
        double dt=lastNanos==0?1.0/60:Math.max(0,Math.min(0.1,(now-lastNanos)*1e-9));
        lastNanos=now;
        magnification=smooth?magnification+(target-magnification)*(1-Math.exp(-20*dt)):target;
        if(Math.abs(magnification-target)<0.001)magnification=target;
        return magnification;
    }
    /** Perspective-correct optical magnification, rather than simply dividing degrees. */
    public static float fov(float original,double magnification) {
        if(!Float.isFinite(original) || original<=0 || original>=180 || !Double.isFinite(magnification) || magnification<=1)return original;
        return (float)Math.toDegrees(2*Math.atan(Math.tan(Math.toRadians(original)*0.5)/magnification));
    }
}

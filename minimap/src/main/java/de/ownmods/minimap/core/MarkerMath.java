package de.ownmods.minimap.core;
public final class MarkerMath {private MarkerMath(){}
    /** Pixel offset for an off-map marker clipped to a circular edge. */
    public static double[] edge(double dx,double dz,double radius){double len=Math.hypot(dx,dz);if(!Double.isFinite(len)||len<1e-9)return new double[]{0,0};double f=radius/len;return new double[]{dx*f,dz*f};}
    public static boolean inside(double dx,double dz,double radius){return dx*dx+dz*dz<=radius*radius;}
    public static int color(String name){return switch(name){case "red"->0xFFD8584C;case "green"->0xFF63B45E;case "yellow"->0xFFE7C34B;case "purple"->0xFFA66DD1;case "blue"->0xFF5AA7E8;default->0xFFFFFFFF;};}
}

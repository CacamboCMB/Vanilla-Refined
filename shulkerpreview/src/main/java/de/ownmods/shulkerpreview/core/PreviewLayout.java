package de.ownmods.shulkerpreview.core;
public record PreviewLayout(int x,int y,float scale) {
    public static final int WIDTH=174,HEIGHT=80;
    public static PreviewLayout place(int width,int height,int mouseX,int mouseY) {
        float scale=Math.min(1f,Math.min(Math.max(0,width-8)/(float)WIDTH,Math.max(0,height-8)/(float)HEIGHT));
        int w=(int)Math.ceil(WIDTH*scale),h=(int)Math.ceil(HEIGHT*scale);
        int x=mouseX+12;if(x+w>width-4)x=mouseX-12-w;
        int y=mouseY-12;
        x=Math.max(4,Math.min(x,width-4-w));y=Math.max(4,Math.min(y,height-4-h));
        return new PreviewLayout(x,y,scale);
    }
}

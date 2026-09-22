package de.ownmods.uicheck;
import de.ownmods.settings.layout.RefinedLayout;
/** Geometry only, no Minecraft, font metrics, rendering or event-delivery simulation. */
public final class LayoutChecks {
    static int cases;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void test(int width,int height){
        var d=RefinedLayout.dashboard(width,height);
        check(d.x()>=0&&d.y()>=0&&d.x()+d.w()<=width&&d.y()+d.h()<=height,"dashboard bounds");
        check(d.perPage()>0&&d.tileW()>=120,"readable dashboard tile width");
        check(d.contentX()+d.columns()*d.tileW()+(d.columns()-1)*8<=d.x()+d.w()-3,"tile right");
        check(d.contentY()+26+d.rows()*d.tileH()+(d.rows()-1)*8<=d.footerY()-3,"tile/footer separation");
        check(d.footerY()+24<d.y()+d.h(),"dashboard footer");
        for(boolean details:new boolean[]{false,true}){
            var a=RefinedLayout.atlas(width,height,details);
            check(a.x()>=0&&a.y()>=0&&a.x()+a.w()<=width&&a.y()+a.h()<=height,"atlas bounds");
            check(a.mapW()>100&&a.bodyH()>=151,"minimum map dimensions");
            check(a.mapX()+a.mapW()<=a.x()+a.w()-3,"map right");
            check(a.infoX()+a.infoW()<=a.x()+a.w()-3,"info right");
            check(a.bodyY()+a.bodyH()<=a.y()+a.h()-3,"body bottom");
            int rows=RefinedLayout.categoryRows(a.bodyH());
            int lastRowBottom=a.bodyY()+6+17+(rows-1)*24+23;
            int controlsY=a.bodyY()+a.bodyH()-99;
            check(lastRowBottom<controlsY,"category list/control separation");
            check(controlsY+49+22<a.bodyY()+a.bodyH()-25,"players/footer separation");
            if(a.dockedInfo())check(a.mapX()+a.mapW()<a.infoX(),"docked map/info separation");
            else if(details)check(a.infoX()==a.mapX()&&a.infoW()==a.mapW(),"drawer replaces map");
        }
        cases++;
    }
    public static void main(String[]args){
        for(int w=320;w<=4096;w+=13)for(int h=240;h<=2160;h+=11)test(w,h);
        int[] physicalWidths={1280,1366,1600,1920,2560,3440,3840};int[] physicalHeights={720,768,900,1080,1440,1600,2160};
        for(int w:physicalWidths)for(int h:physicalHeights)for(int scale=1;scale<=8;scale++){
            int gw=(w+scale-1)/scale,gh=(h+scale-1)/scale;if(gw>=320&&gh>=240)test(gw,gh);
        }
        for(int w:new int[]{320,321,493,494,593,594,713,714,823,824})for(int h:new int[]{240,241,299,300,454,455,480,720})test(w,h);
        System.out.println("LAYOUT: "+cases+" GUI-size cases passed (pure geometry; no game test).");
    }
}

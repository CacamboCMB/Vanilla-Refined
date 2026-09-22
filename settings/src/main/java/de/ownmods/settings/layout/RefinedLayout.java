package de.ownmods.settings.layout;

/** Pure geometry, in Minecraft GUI units. No physical pixels or GUI-scale guesses. */
public final class RefinedLayout {
    private RefinedLayout() {}
    public record Dashboard(int x, int y, int w, int h, int sidebar,
                            int contentX, int contentY, int contentW, int contentH,
                            int columns, int rows, int tileW, int tileH, int footerY) {
        public int perPage() { return columns * rows; }
    }
    public static Dashboard dashboard(int width, int height) {
        int w = Math.min(800, Math.max(1, width - 16));
        int h = Math.min(490, Math.max(1, height - 16));
        int x = (width - w) / 2, y = (height - h) / 2;
        int side = w >= 520 ? 136 : 100;
        int cx = x + side + 9, cy = y + 48;
        int cw = Math.max(1, w - side - 18), ch = Math.max(1, h - 94);
        int cols = Math.max(1, Math.min(4, (cw + 8) / 150));
        int rows = Math.max(1, (ch - 26 + 8) / 84);
        return new Dashboard(x,y,w,h,side,cx,cy,cw,ch,cols,rows,
                Math.max(1,(cw - (cols-1)*8)/cols),76,y+h-34);
    }
    public record Atlas(int x, int y, int w, int h, int sideW,
                        int bodyY, int bodyH, int mapX, int mapW,
                        int infoX, int infoW, boolean dockedInfo) {}
    public static Atlas atlas(int width, int height, boolean showDetails) {
        int x = 7, y = 7, w = Math.max(1,width-14), h = Math.max(1,height-14);
        int side = w >= 680 ? 154 : 132;
        boolean docked = w >= 700;
        int mapX = x + 3 + side;
        int mapW = Math.max(1,w-side-6-(docked?214:0));
        int iw = docked ? 210 : (showDetails ? mapW : 0);
        int ix = docked ? mapX+mapW+4 : mapX;
        return new Atlas(x,y,w,h,side,y+72,h-75,mapX,mapW,ix,iw,docked);
    }
    /** Footer and filter-list pagination always have separate space. */
    public static int categoryRows(int height) {
        return Math.max(1,Math.min(10,(height-126)/24));
    }
}

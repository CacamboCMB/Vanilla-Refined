package de.ownmods.settings.layout;
public record ModGridLayout(int columns,int rows,int page,int pages,int perPage) {
    public static final int SIZE=44, STEP=52, TOP=56;
    public static ModGridLayout create(int width,int height,int count,int requestedPage) {
        if(width<0 || height<0 || count<0) throw new IllegalArgumentException();
        int columns=Math.max(1,Math.min(6,(Math.min(380,Math.max(44,width-24))+8)/STEP));
        int rows=Math.max(1,(height-120)/STEP);
        int perPage=columns*rows;
        int pages=Math.max(1,(count+perPage-1)/perPage);
        return new ModGridLayout(columns,rows,Math.max(0,Math.min(requestedPage,pages-1)),pages,perPage);
    }
}

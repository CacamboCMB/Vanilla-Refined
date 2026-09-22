package de.ownmods.inventorysort.core;
import java.util.Optional;
public record SortButtonLayout(int x,int y) {
    /** Outside the inventory area. If not enough space exists, expose keyboard-only sorting. */
    public static Optional<SortButtonLayout> place(int screenWidth,int screenHeight,int left,int top,int imageWidth) {
        int y=Math.max(4,top),x=left+imageWidth+4;
        if(x+20>screenWidth-4)x=left-24;
        if(x<4 || x+20>screenWidth-4 || y+44>screenHeight-4)return Optional.empty();
        return Optional.of(new SortButtonLayout(x,y));
    }
}

package com.lqr.emoji;


public class StickerItem {
    private String category;//类别名
    private String name;

    public StickerItem(String category, String name) {
        this.category = category;
        this.name = name;
    }

    public String getIdentifier() {
        return category + "/" + name;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof StickerItem) {
            StickerItem item = (StickerItem) obj;
            return item.getCategory().equals(category) && item.getName().equals(name);
        }
        return false;
    }
}

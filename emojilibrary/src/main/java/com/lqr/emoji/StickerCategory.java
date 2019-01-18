package com.lqr.emoji;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StickerCategory {
    private static final long serialVersionUID = -81692490861539040L;

    private String name;//贴图包名
    private String title;//显示的标题
    private boolean system;//是否是系统内置表情
    private int order = 0;//默认顺序

    private transient List<StickerItem> stickers;

    public StickerCategory(String name, String title, boolean system, int order) {
        this.name = name;
        this.title = title;
        this.system = system;
        this.order = order;
        loadStickerData();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<StickerItem> getStickers() {
        return stickers;
    }

    public void setStickers(List<StickerItem> stickers) {
        this.stickers = stickers;
    }

    public boolean hasStickers() {
        return stickers != null && stickers.size() > 0;
    }

    public int getCount() {
        if (stickers == null || stickers.isEmpty()) {
            return 0;
        }

        return stickers.size();
    }

    public String getCoverImgPath() {
        for (File file : new File(LQREmotionKit.getStickerPath()).listFiles()) {
            if (file.isFile() && file.getName().startsWith(name)) {
                return "file://" + file.getAbsolutePath();
            }
        }
        return null;
    }

    public List<StickerItem> loadStickerData() {
        List<StickerItem> stickers = new ArrayList<>();
        File stickerDir = new File(LQREmotionKit.getStickerPath(), name);
        if (stickerDir.exists()) {
            File[] files = stickerDir.listFiles();
            for (File file : files) {
                //比如：tsj ---> tsj/tsj_001.gif
//                stickers.add(new StickerItem(name, name + File.separator + file.getName()));
                stickers.add(new StickerItem(name, file.getName()));
            }
        }

        //补充最后一页缺少的贴图
        int tmp = stickers.size() % EmotionLayout.STICKER_PER_PAGE;
        if (tmp != 0) {
            int tmp2 = EmotionLayout.STICKER_PER_PAGE - (stickers.size() - (stickers.size() / EmotionLayout.STICKER_PER_PAGE) * EmotionLayout.STICKER_PER_PAGE);
            for (int i = 0; i < tmp2; i++) {
                stickers.add(new StickerItem("", ""));
            }
        }

        this.setStickers(stickers);
        return stickers;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof StickerCategory)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        StickerCategory r = (StickerCategory) obj;
        return r.getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

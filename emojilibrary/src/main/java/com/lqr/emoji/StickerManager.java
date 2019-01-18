package com.lqr.emoji;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSDN_LQR
 * 贴图管理类
 */
public class StickerManager {

    private static StickerManager instance;

    //数据源
    private List<StickerCategory> stickerCategories = new ArrayList<>();
    private Map<String, StickerCategory> stickerCategoryMap = new HashMap<>();
//    private Map<String, Integer> stickerOrder = new HashMap<>();

    public static StickerManager getInstance() {
        if (instance == null) {
            synchronized (StickerManager.class) {
                if (instance == null) {
                    instance = new StickerManager();
                }
            }
        }
        return instance;
    }

    public StickerManager() {
//        initStickerOrder();
        loadStickerCategory();
    }

    private void loadStickerCategory() {
        File stickerDir = new File(LQREmotionKit.getStickerPath());
        if (stickerDir.exists()) {
            File[] files = stickerDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                //当前的目录下同名的有文件和文件夹，只需要其中的一个取其名
                if (file.isDirectory()) {
                    String name = file.getName();
                    StickerCategory category = new StickerCategory(name, name, true, i);
                    stickerCategories.add(category);
                    stickerCategoryMap.put(name, category);
                }
            }

            //排序
            Collections.sort(stickerCategories, new Comparator<StickerCategory>() {
                @Override
                public int compare(StickerCategory o1, StickerCategory o2) {
                    return o1.getOrder() - o2.getOrder();
                }
            });
        }
    }

    public synchronized List<StickerCategory> getStickerCategories() {
        return stickerCategories;
    }

    public synchronized StickerCategory getCategory(String name) {
        return stickerCategoryMap.get(name);
    }

    public String getStickerBitmapUri(String categoryName, String stickerName) {
        String path = getStickerBitmapPath(categoryName, stickerName);
        return "file://" + path;
    }

    @Nullable
    public String getStickerBitmapPath(String categoryName, String stickerName) {
        StickerManager manager = StickerManager.getInstance();
        StickerCategory category = manager.getCategory(categoryName);
        if (category == null) {
            return null;
        }
        return LQREmotionKit.getStickerPath() + File.separator + category.getName() + File.separator + stickerName;
    }


}

package com.lqr.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

/**
 * CSDN_LQR
 * 表情布局
 */
public class EmotionLayout extends LinearLayout implements View.OnClickListener {

    public static final int EMOJI_COLUMNS = 7;

    public static final int STICKER_COLUMNS = 4;

    private int mTabPosi = 0;
    private Context mContext;
    private LinearLayout emotionLayout;
    private ViewPager2 mViewPagerEmotions;
    private LinearLayout mLlTabContainer;
    private RelativeLayout mRlEmotionAdd;
    private FrameLayout mEmotionContainer; // 容器，包含表情ViewPager和删除按钮
    private ImageView mDeleteButton; // 删除按钮

    private int mTabCount;
    private SparseArray<View> mTabViewArray = new SparseArray<>();
    private EmotionTab mSettingTab;
    private IEmotionSelectedListener mEmotionSelectedListener;
    private IEmotionExtClickListener mEmotionExtClickListener;
    private boolean mEmotionAddVisiable = false;
    private boolean mEmotionSettingVisiable = false;
    private boolean stickerVisible = true;

    private EmotionViewPager2Adapter viewPagerAdapter;

    public EmotionLayout(Context context) {
        this(context, null);
    }

    public EmotionLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmotionLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    public void setEmotionSelectedListener(IEmotionSelectedListener emotionSelectedListener) {
        if (emotionSelectedListener != null) {
            this.mEmotionSelectedListener = emotionSelectedListener;
        } else {
            Log.i("CSDN_LQR", "IEmotionSelectedListener is null");
        }
    }

    public void setEmotionExtClickListener(IEmotionExtClickListener emotionExtClickListener) {
        if (emotionExtClickListener != null) {
            this.mEmotionExtClickListener = emotionExtClickListener;
        } else {
            Log.i("CSDN_LQR", "IEmotionSettingTabClickListener is null");
        }
    }

    /**
     * 设置表情添加按钮的显隐
     *
     * @param visiable
     */
    public void setEmotionAddVisiable(boolean visiable) {
        mEmotionAddVisiable = visiable;
        if (mRlEmotionAdd != null) {
            mRlEmotionAdd.setVisibility(mEmotionAddVisiable ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 设置表情设置按钮的显隐
     *
     * @param visiable
     */
    public void setEmotionSettingVisiable(boolean visiable) {
        mEmotionSettingVisiable = visiable;
        if (mSettingTab != null) {
            mSettingTab.setVisibility(mEmotionSettingVisiable ? View.VISIBLE : View.GONE);
        }
    }

    public void setStickerVisible(boolean visible) {
        stickerVisible = visible;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }


    private void init() {
        if (emotionLayout != null) {
            ((ViewGroup) emotionLayout).removeAllViews();
            removeView(emotionLayout);
        }
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        emotionLayout = (LinearLayout) inflater.inflate(R.layout.emotion_layout, this);
        setOrientation(LinearLayout.VERTICAL);

        mEmotionContainer = findViewById(R.id.emotion_container);
        mViewPagerEmotions = findViewById(R.id.viewPager_emotions);

        // 创建并添加删除按钮到容器中
        addDeleteButton();

        mLlTabContainer = findViewById(R.id.llTabContainer);
        mRlEmotionAdd = findViewById(R.id.rlEmotionAdd);

        setEmotionAddVisiable(mEmotionAddVisiable);
        emotionLayout.post(new Runnable() {
            @Override
            public void run() {
                initTabs();
                initListener();
                initViewPager();
            }
        });
    }

    /**
     * 初始化ViewPager2
     */
    private void initViewPager() {
        // 创建ViewPager2适配器
        viewPagerAdapter = new EmotionViewPager2Adapter(mContext, stickerVisible, mEmotionSelectedListener);

        // 设置适配器
        mViewPagerEmotions.setAdapter(viewPagerAdapter);

        // 注册页面变化监听器
        mViewPagerEmotions.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int tabIndex = viewPagerAdapter.getTabIndex(position);
                if (tabIndex != mTabPosi) {
                    mTabPosi = tabIndex;
                    selectTab(mTabPosi);
                    updateDeleteButtonVisibility();
                }
            }
        });
    }

    /**
     * 添加悬浮在右下角的删除按钮
     */
    private void addDeleteButton() {
        if (mDeleteButton == null) {
            mDeleteButton = new ImageView(mContext);
            mDeleteButton.setImageResource(R.drawable.ic_emoji_del);

            mDeleteButton.setBackgroundResource(R.drawable.bg_emotion_delete);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

            params.width = LQREmotionKit.dip2px(60);
            params.height = LQREmotionKit.dip2px(40);
            params.rightMargin = LQREmotionKit.dip2px(12);
            params.bottomMargin = LQREmotionKit.dip2px(12);
            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.RIGHT;

            mDeleteButton.setPadding(20, 10, 20, 10);

            mDeleteButton.setLayoutParams(params);

            mDeleteButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mEmotionSelectedListener != null) {
                        mEmotionSelectedListener.onEmojiSelected("/DEL");
                    }
                }
            });

            mEmotionContainer.addView(mDeleteButton);
        }

        updateDeleteButtonVisibility();
    }

    /**
     * 更新删除按钮的可见性
     * 只有在emoji表情面板时才显示删除按钮
     */
    private void updateDeleteButtonVisibility() {
        if (mDeleteButton != null) {
            mDeleteButton.setVisibility(mTabPosi == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void initTabs() {
        //默认添加一个表情tab
        EmotionTab emojiTab = new EmotionTab(mContext, R.drawable.ic_tab_emoji);
        mTabCount = 1;
        mLlTabContainer.addView(emojiTab);
        mTabViewArray.put(0, emojiTab);

        //添加所有的贴图tab
        if (stickerVisible) {
            List<StickerCategory> stickerCategories = StickerManager.getInstance().getStickerCategories();
            for (int i = 0; i < stickerCategories.size(); i++) {
                StickerCategory category = stickerCategories.get(i);
                EmotionTab tab = new EmotionTab(mContext, category.getCoverImgPath());
                mLlTabContainer.addView(tab);
                mTabViewArray.put(i + 1, tab);
                mTabCount++;
            }
        }

        //最后添加一个表情设置Tab
        if (mEmotionSettingVisiable) {
            mSettingTab = new EmotionTab(mContext, R.drawable.ic_emotion_setting);
            StateListDrawable drawable = new StateListDrawable();
            Drawable unSelected = mContext.getResources().getDrawable(R.color.white);
            drawable.addState(new int[]{-android.R.attr.state_pressed}, unSelected);
            Drawable selected = mContext.getResources().getDrawable(R.color.gray);
            drawable.addState(new int[]{android.R.attr.state_pressed}, selected);
            mSettingTab.setBackground(drawable);
            mLlTabContainer.addView(mSettingTab);
            mTabViewArray.put(mTabViewArray.size(), mSettingTab);
        }

        selectTab(0);
    }

    private void initListener() {
        if (mLlTabContainer != null) {
            for (int position = 0; position < mTabCount; position++) {
                View tab = mLlTabContainer.getChildAt(position);
                tab.setTag(position);
                tab.setOnClickListener(this);
            }
        }

        if (mRlEmotionAdd != null) {
            mRlEmotionAdd.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mEmotionExtClickListener != null) {
                        mEmotionExtClickListener.onEmotionAddClick(v);
                    }
                }
            });
        }
        if (mSettingTab != null) {
            mSettingTab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mEmotionExtClickListener != null) {
                        mEmotionExtClickListener.onEmotionSettingClick(v);
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        int targetTabIndex = (int) v.getTag();
        if (targetTabIndex == mTabPosi) {
            return;
        }

        int viewPagerPosition = viewPagerAdapter.findPositionByTabIndex(targetTabIndex);
        mViewPagerEmotions.setCurrentItem(viewPagerPosition, true);
    }

    private void selectTab(int tabPosi) {
        if (mEmotionAddVisiable && tabPosi == mTabViewArray.size() - 1)
            return;

        EmotionTab tab;
        for (int i = 0; i < mTabCount; i++) {
            tab = (EmotionTab) mTabViewArray.get(i);
            tab.mIvIcon.setBackground(null);
        }
        tab = (EmotionTab) mTabViewArray.get(tabPosi);
        tab.mIvIcon.setBackgroundResource(R.drawable.shape_tab_press);
    }
}

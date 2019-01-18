package cn.wildfire.chat.setting;

import cn.wildfirechat.chat.R;
import cn.wildfire.chat.third.location.ui.base.BaseActivity;
import cn.wildfire.chat.third.location.ui.base.BasePresenter;

/**
 * @创建者 CSDN_LQR
 * @描述 关于界面
 */
public class AboutActivity extends BaseActivity {

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_about;
    }
}

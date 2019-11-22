package cn.wildfire.chat.kit.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.app.main.SplashActivity;
import cn.wildfire.chat.app.setting.AboutActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.settings.blacklist.BlacklistListActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;

public class PrivacySettingActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.privacy_setting_activity;
    }

    @OnClick(R.id.blacklistOptionItemView)
    void blacklistSettings() {
        Intent intent = new Intent(this, BlacklistListActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.momentsPrivacyOptionItemView)
    void mementsSettings() {

    }
}

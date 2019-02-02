package cn.wildfire.chat.kit.setting;

import butterknife.OnClick;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfirechat.chat.R;

public class AboutActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.activity_about;
    }

    @OnClick(R.id.introOptionItemView)
    public void intro() {
        WfcWebViewActivity.loadUrl(this, "野火IM用户协议", "http://docs.wildfirechat.cn/");
    }

    @OnClick(R.id.agreementOptionItemView)
    public void agreement() {
        WfcWebViewActivity.loadUrl(this, "野火IM用户协议", "http://www.wildfirechat.cn/firechat_user_agreement.html");
    }

    @OnClick(R.id.privacyOptionItemView)
    public void privacy() {
        WfcWebViewActivity.loadUrl(this, "野火IM个人信息保护政策", "http://www.wildfirechat.cn/firechat_user_privacy.html");
    }
}

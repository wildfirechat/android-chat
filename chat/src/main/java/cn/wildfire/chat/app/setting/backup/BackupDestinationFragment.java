package cn.wildfire.chat.app.setting.backup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 备份目标选择界面 Fragment
 */
public class BackupDestinationFragment extends Fragment {

    private CardView backupToPCCard;
    private TextView backupToPCTitleTextView;
    private TextView backupToPCDescTextView;

    private SettingViewModel settingViewModel;
    private PickBackupConversationViewModel pickBackupConversationViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_backup_destination, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pickBackupConversationViewModel = new ViewModelProvider(requireActivity()).get(PickBackupConversationViewModel.class);
        initView(view);
        observeSettingChanges();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePCOnlineStatus();
    }

    private void initView(View view) {
        view.findViewById(R.id.backupToLocalCard).setOnClickListener(v -> backupToLocal());

        backupToPCCard = view.findViewById(R.id.backupToPCCard);
        backupToPCTitleTextView = backupToPCCard.findViewById(R.id.titleTextView);
        backupToPCDescTextView = backupToPCCard.findViewById(R.id.descTextView);

        // 初始状态，稍后在 onResume 中更新
        backupToPCCard.setOnClickListener(v -> {
            if (isPCOnline()) {
                backupToPC();
            } else {
                Toast.makeText(requireContext(), R.string.pc_is_not_online, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeSettingChanges() {
        // 获取 SettingViewModel
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);

        // 监听设置变更
        settingViewModel.settingUpdatedLiveData().observe(getViewLifecycleOwner(), new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                // 当设置变更时，重新检查PC在线状态
                updatePCOnlineStatus();
            }
        });
    }

    private void updatePCOnlineStatus() {
        boolean isOnline = isPCOnline();

        if (isOnline) {
            // PC 在线 - 启用按钮
            backupToPCCard.setEnabled(true);
            backupToPCCard.setAlpha(1.0f);
            backupToPCCard.setClickable(true);
            backupToPCTitleTextView.setText(R.string.backup_to_pc);
            backupToPCTitleTextView.setTextColor(getResources().getColor(R.color.black0));
            backupToPCDescTextView.setText(R.string.backup_to_pc_desc);
            backupToPCDescTextView.setTextColor(getResources().getColor(R.color.gray1));
        } else {
            // PC 离线 - 禁用按钮
            backupToPCCard.setEnabled(false);
            backupToPCCard.setAlpha(0.5f);
            backupToPCCard.setClickable(false);
            backupToPCTitleTextView.setText(R.string.backup_to_pc_offline);
            backupToPCTitleTextView.setTextColor(getResources().getColor(R.color.gray11));
            backupToPCDescTextView.setText(R.string.pc_client_not_online);
            backupToPCDescTextView.setTextColor(getResources().getColor(R.color.gray11));
        }
    }

    private boolean isPCOnline() {
        try {
            List<PCOnlineInfo> pcOnlineInfos = ChatManager.Instance().getPCOnlineInfos();
            return pcOnlineInfos != null && !pcOnlineInfos.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void backupToLocal() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new BackupProgressFragment())
                .addToBackStack(null)
                .commit();
    }

    private void backupToPC() {
        // 跳转到备份请求进度界面 Fragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new BackupRequestProgressFragment())
                .addToBackStack(null)
                .commit();
    }
}

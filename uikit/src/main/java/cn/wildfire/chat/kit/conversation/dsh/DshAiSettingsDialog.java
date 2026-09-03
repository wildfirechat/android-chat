/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.dsh;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.utils.DshState;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.widget.WfcSheetDialogCompat;
import cn.wildfirechat.message.dsh.AgentCommandMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnSettingUpdateListener;

/**
 * AI 会话设置面板（手机端 BottomSheet / 底部弹窗，宽屏自动变居中对话框，见
 * {@link WfcSheetDialogCompat}；内容区可滚动，适配小屏）。
 * <p>
 * 静默通道：所有交互不落消息流（不显示在界面上）。
 * 打开面板发 207 DSH_Command（op=query）组合查询 → 插件聚合面板数据
 * （model 当前值+目录 / effort / sandbox / plan / cwd / sessionId / dirs）写入
 * scope=31 type=3（键 convType-line-target_3，不回复消息）→ 本面板读 type=3 渲染：
 * 模型/推理等级为下拉（model.options / effort.options + current）、沙箱为单选、
 * 计划为开关、工作目录为 cwd + dirs 列表选择弹窗。
 * 所有操作发 207 DSH_Command（op=set，cmd=命令文本，如 "/model deepseek-official/xxx"）；
 * 插件执行后写 type=1 状态 lastChange（如 "模型 → deepseek-official/deepseek-v4-pro"，变更可见）
 * 并刷新 type=3，本面板监听本端已有的用户设置更新事件（{@link OnSettingUpdateListener}）重读 type=3。
 * 不再发送 /model /effort /sandbox /plan /ls 等文本命令、不再解析机器人回复文本
 * （parseModelReply / parseEffortReply / parseSandboxReply / parsePlanReply / parseLsReply 已移除）。
 * 207 为透明消息（PersistFlag.Transparent，digest 空）：不持久化、不显示。
 * </p>
 */
public class DshAiSettingsDialog {

    /** 发送指令后控件禁用的时长（防连点），与 PC 端一致 */
    private static final long FLASH_MILLIS = 1500L;
    /** 查询兜底超时：type=3 未及时刷新也先渲染面板 */
    private static final long LOADING_TIMEOUT_MILLIS = 6000L;
    /** 目录列表（type=3 dirs 为空时补发 query）兜底超时 */
    private static final long CWD_LIST_TIMEOUT_MILLIS = 8000L;

    /** 模型候选：value=provider/id（发送用），label=value（name）（展示用） */
    private static class ModelOption {
        final String value;
        final String label;

        ModelOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }

    private final Dialog dialog;
    private final Conversation conversation;
    private final MessageViewModel messageViewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView loadingView;
    private Spinner modelSpinner;
    private Spinner effortSpinner;
    private RadioGroup sandboxGroup;
    private TextView cwdCurrentView;
    private TextView cwdListLoadingView;
    private LinearLayout cwdListContainer;
    private TextView cwdListEmptyView;
    private Button cwdSwitchBtn;
    private Switch planSwitch;
    private TextView planText;
    private Button compactBtn;
    private Button resetBtn;
    private Button destroyBtn;
    private TextView applyingView;

    /** type=3 面板数据当前值（模型/推理等级/沙箱/计划/工作目录） */
    private final List<ModelOption> modelOptions = new ArrayList<>();
    private String currentModel = "";
    private final List<String> effortOptions = new ArrayList<>();
    private String currentEffort = "";
    private String currentSandbox = "";
    private boolean planOn = false;
    private String currentCwd = "";
    /** type=3 dirs 根目录子目录候选（工作目录选择式切换） */
    private final List<String> cwdCandidates = new ArrayList<>();
    /** 下拉实际展示项（= 候选 + 当前值不在候选时前置追加），与 Spinner 位置一一对应 */
    private final List<ModelOption> displayModelOptions = new ArrayList<>();
    private final List<String> displayEffortOptions = new ArrayList<>();

    /** 程序化重建下拉/单选时的防回环开关 */
    private boolean rendering = false;
    private boolean applying = false;
    private boolean destroyed = false;
    /** 目录列表是否已打开（未打开时收到 type=3 刷新只更新候选，不展示） */
    private boolean cwdListOpen = false;
    /** 207 seq 递增序号（防重复/幂等） */
    private long commandSeq = 0;

    /** 设置更新事件：插件执行更新/查询后写 type=3，重读刷新（show 注册、dismiss 移除） */
    private final OnSettingUpdateListener settingUpdateListener = this::refreshPanelData;

    private final Runnable loadingTimeoutRunnable = () -> {
        if (!destroyed) {
            loadingView.setVisibility(View.GONE);
        }
    };
    private final Runnable flashRunnable = () -> {
        applying = false;
        updateEnabledState();
        applyingView.setVisibility(View.GONE);
    };
    /** 目录列表兜底超时：隐藏 loading，提示超时 */
    private final Runnable cwdListTimeoutRunnable = () -> {
        if (destroyed || !cwdListOpen) {
            return;
        }
        cwdListLoadingView.setVisibility(View.GONE);
        cwdListEmptyView.setText(R.string.dsh_ai_cwd_list_timeout);
        cwdListEmptyView.setVisibility(View.VISIBLE);
        updateEnabledState();
    };

    public DshAiSettingsDialog(Context context, Conversation conversation, MessageViewModel messageViewModel) {
        this.dialog = WfcSheetDialogCompat.create(context);
        this.conversation = conversation;
        this.messageViewModel = messageViewModel;
        View view = LayoutInflater.from(context).inflate(R.layout.dsh_ai_settings_dialog, null);
        dialog.setContentView(view);
        bindViews(view);
        dialog.setOnDismissListener(d -> destroy());
    }

    public void show() {
        dialog.show();
        // 监听设置更新事件：插件写 type=3（query 结果 / set 后刷新）触发重读
        ChatManager.Instance().addSettingUpdateListener(settingUpdateListener);
        // 先读已有 type=3 面板数据（若有）渲染，再发 207 query 组合查询刷新
        refreshPanelData();
        loadingView.setVisibility(View.VISIBLE);
        sendCommand("query", null);
        // 兜底：type=3 未及时刷新也先渲染面板
        handler.postDelayed(loadingTimeoutRunnable, LOADING_TIMEOUT_MILLIS);
    }

    private void bindViews(View view) {
        loadingView = view.findViewById(R.id.dshAiLoading);
        modelSpinner = view.findViewById(R.id.dshAiModelSpinner);
        effortSpinner = view.findViewById(R.id.dshAiEffortSpinner);
        sandboxGroup = view.findViewById(R.id.dshAiSandboxGroup);
        cwdCurrentView = view.findViewById(R.id.dshAiCwdCurrent);
        cwdListLoadingView = view.findViewById(R.id.dshAiCwdListLoading);
        cwdListContainer = view.findViewById(R.id.dshAiCwdList);
        cwdListEmptyView = view.findViewById(R.id.dshAiCwdListEmpty);
        cwdSwitchBtn = view.findViewById(R.id.dshAiCwdSwitch);
        planSwitch = view.findViewById(R.id.dshAiPlanSwitch);
        planText = view.findViewById(R.id.dshAiPlanText);
        compactBtn = view.findViewById(R.id.dshAiCompact);
        resetBtn = view.findViewById(R.id.dshAiReset);
        destroyBtn = view.findViewById(R.id.dshAiDestroy);
        applyingView = view.findViewById(R.id.dshAiApplying);

        TextView closeView = view.findViewById(R.id.dshAiClose);
        closeView.setText("×");
        closeView.setOnClickListener(v -> dialog.dismiss());

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (rendering || applying || position < 0 || position >= displayModelOptions.size()) {
                    return;
                }
                ModelOption option = displayModelOptions.get(position);
                if (option == null || option.value.equals(currentModel)) {
                    return;
                }
                currentModel = option.value;
                sendCommand("set", "/model " + option.value);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        effortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (rendering || applying || position < 0 || position >= displayEffortOptions.size()) {
                    return;
                }
                String value = displayEffortOptions.get(position);
                if (value == null || value.equals(currentEffort)) {
                    return;
                }
                currentEffort = value;
                sendCommand("set", "/effort " + value);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        sandboxGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (rendering || applying || checkedId == View.NO_ID) {
                return;
            }
            RadioButton radioButton = group.findViewById(checkedId);
            if (radioButton == null || radioButton.getTag() == null) {
                return;
            }
            String value = radioButton.getTag().toString();
            if (value.equals(currentSandbox)) {
                return;
            }
            currentSandbox = value;
            sendCommand("set", "/sandbox " + value);
        });

        // Switch 的点击只来自用户操作（程序化 setChecked 不触发），天然防回环
        planSwitch.setOnClickListener(v -> {
            if (applying) {
                return;
            }
            planOn = planSwitch.isChecked();
            renderPlan();
            sendCommand("set", "/plan " + (planOn ? "on" : "off"));
        });

        cwdSwitchBtn.setOnClickListener(v -> {
            if (applying) {
                return;
            }
            openCwdList();
        });

        compactBtn.setOnClickListener(v -> confirmAndSend(R.string.dsh_ai_compact_confirm, "/compact"));
        resetBtn.setOnClickListener(v -> confirmAndSend(R.string.dsh_ai_reset_confirm, "/reset"));
        // 销毁会话：毁灭性操作，不受操作冷却禁用，始终可点（点击弹强警告，确认后才发送）
        destroyBtn.setOnClickListener(v -> confirmAndSend(R.string.dsh_ai_destroy_confirm, "/destroy"));
    }

    /**
     * 重读 scope=31 type=3 面板数据并渲染（设置更新事件驱动；
     * 插件执行 query 组合查询 / set 更新后写 type=3 触发）。
     */
    private void refreshPanelData() {
        if (destroyed || conversation == null) {
            return;
        }
        try {
            JSONObject data = DshState.getDshPanelData(conversation);
            if (data != null) {
                applyPanelData(data);
            }
        } catch (Exception ignored) {
            // 读取失败保持旧数据，下次设置更新会重读
        }
    }

    /**
     * 把 type=3 面板数据应用到当前值并渲染：
     * model.options/current、effort.options/current、sandbox.current、plan.on、
     * cwd、dirs 根目录子目录。
     */
    private void applyPanelData(JSONObject data) {
        // 模型：current + options（value=provider/id，label=value（名））
        JSONObject model = data.optJSONObject("model");
        if (model != null) {
            currentModel = model.optString("current", "");
            modelOptions.clear();
            JSONArray options = model.optJSONArray("options");
            if (options != null) {
                for (int i = 0; i < options.length(); i++) {
                    JSONObject option = options.optJSONObject(i);
                    if (option == null) {
                        continue;
                    }
                    String value = option.optString("value");
                    if (TextUtils.isEmpty(value)) {
                        continue;
                    }
                    String label = option.optString("label");
                    if (TextUtils.isEmpty(label)) {
                        label = value;
                    }
                    modelOptions.add(new ModelOption(value, label));
                }
            }
        }
        // 推理等级：current + options（字符串数组）
        JSONObject effort = data.optJSONObject("effort");
        if (effort != null) {
            currentEffort = effort.optString("current", "");
            effortOptions.clear();
            JSONArray options = effort.optJSONArray("options");
            if (options != null) {
                for (int i = 0; i < options.length(); i++) {
                    String value = options.optString(i);
                    if (!TextUtils.isEmpty(value) && !effortOptions.contains(value)) {
                        effortOptions.add(value);
                    }
                }
            }
        }
        // 沙箱：current
        JSONObject sandbox = data.optJSONObject("sandbox");
        if (sandbox != null) {
            currentSandbox = sandbox.optString("current", "");
        }
        // 计划：on
        JSONObject plan = data.optJSONObject("plan");
        if (plan != null) {
            planOn = plan.optBoolean("on", false);
        }
        // 工作目录 + 根目录子目录
        currentCwd = data.optString("cwd", "");
        cwdCandidates.clear();
        JSONArray dirs = data.optJSONArray("dirs");
        if (dirs != null) {
            for (int i = 0; i < dirs.length(); i++) {
                String dir = dirs.optString(i);
                if (!TextUtils.isEmpty(dir)) {
                    cwdCandidates.add(dir);
                }
            }
        }

        renderModel();
        renderEffort();
        renderSandbox();
        renderPlan();
        renderCwd();
        // type=3 就绪：隐藏加载中提示
        loadingView.setVisibility(View.GONE);
        handler.removeCallbacks(loadingTimeoutRunnable);
    }

    /**
     * 发送 207 DSH_Command 面板指令（透明消息，不显示在消息流）。
     * op=query 组合查询（cmd 空）；op=set 更新（cmd=命令文本，如 "/model deepseek-official/xxx"）。
     * set 发送后控件短暂禁用（防连点）。
     */
    private void sendCommand(String op, String cmd) {
        if (destroyed || conversation == null || messageViewModel == null) {
            return;
        }
        AgentCommandMessageContent content = new AgentCommandMessageContent(op, cmd, ++commandSeq);
        messageViewModel.sendMessage(conversation, content);
        if ("set".equals(op)) {
            applying = true;
            updateEnabledState();
            applyingView.setVisibility(View.VISIBLE);
            handler.removeCallbacks(flashRunnable);
            handler.postDelayed(flashRunnable, FLASH_MILLIS);
        }
    }

    /** 重建模型下拉：候选来自 type=3 model.options；当前值不在候选里也追加显示并选中 */
    private void renderModel() {
        rendering = true;
        displayModelOptions.clear();
        displayModelOptions.addAll(modelOptions);
        boolean hasCurrent = false;
        for (ModelOption option : displayModelOptions) {
            if (option.value.equals(currentModel)) {
                hasCurrent = true;
                break;
            }
        }
        if (!hasCurrent && !TextUtils.isEmpty(currentModel)) {
            displayModelOptions.add(0, new ModelOption(currentModel, currentModel));
        }
        List<String> labels = new ArrayList<>();
        for (ModelOption option : displayModelOptions) {
            labels.add(option.label);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(dialog.getContext(),
            android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        int index = 0;
        for (int i = 0; i < displayModelOptions.size(); i++) {
            if (displayModelOptions.get(i).value.equals(currentModel)) {
                index = i;
                break;
            }
        }
        modelSpinner.setSelection(index);
        // 下拉重建的 onItemSelected 回调在布局后异步触发，延后清除防回环开关
        handler.post(() -> rendering = false);
    }

    /** 重建推理等级下拉：候选来自 type=3 effort.options；当前值不在候选里也追加显示并选中 */
    private void renderEffort() {
        rendering = true;
        displayEffortOptions.clear();
        displayEffortOptions.addAll(effortOptions);
        if (!displayEffortOptions.contains(currentEffort) && !TextUtils.isEmpty(currentEffort)) {
            displayEffortOptions.add(0, currentEffort);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(dialog.getContext(),
            android.R.layout.simple_spinner_item, displayEffortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        effortSpinner.setAdapter(adapter);
        int index = displayEffortOptions.indexOf(currentEffort);
        effortSpinner.setSelection(Math.max(0, index));
        handler.post(() -> rendering = false);
    }

    /** 沙箱模式固定三项单选，按当前值高亮 */
    private void renderSandbox() {
        rendering = true;
        sandboxGroup.clearCheck();
        if (!TextUtils.isEmpty(currentSandbox)) {
            for (int i = 0; i < sandboxGroup.getChildCount(); i++) {
                View child = sandboxGroup.getChildAt(i);
                if (child instanceof RadioButton && currentSandbox.equals(child.getTag())) {
                    sandboxGroup.check(child.getId());
                    break;
                }
            }
        }
        rendering = false;
    }

    private void renderPlan() {
        planSwitch.setChecked(planOn);
        planText.setText(planOn ? R.string.dsh_ai_plan_on : R.string.dsh_ai_plan_off);
    }

    /** 工作目录：当前值只读展示 */
    private void renderCwd() {
        if (TextUtils.isEmpty(currentCwd)) {
            cwdCurrentView.setText(dialog.getContext().getString(R.string.dsh_ai_cwd_current, "未设置"));
        } else {
            cwdCurrentView.setText(dialog.getContext().getString(R.string.dsh_ai_cwd_current, currentCwd));
        }
        cwdCurrentView.setVisibility(View.VISIBLE);
        // 目录列表打开中：用最新 dirs 重建（type=3 刷新可能带新目录）
        if (cwdListOpen) {
            renderCwdList();
            cwdListLoadingView.setVisibility(View.GONE);
            handler.removeCallbacks(cwdListTimeoutRunnable);
            if (cwdCandidates.isEmpty()) {
                cwdListContainer.setVisibility(View.GONE);
                cwdListEmptyView.setText(R.string.dsh_ai_cwd_list_empty);
                cwdListEmptyView.setVisibility(View.VISIBLE);
            } else {
                cwdListEmptyView.setVisibility(View.GONE);
                cwdListContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    /** 点“切换”：目录候选来自 type=3 dirs（面板数据已含）；为空时补发 query 刷新 */
    private void openCwdList() {
        cwdListOpen = true;
        if (cwdCandidates.isEmpty()) {
            cwdListContainer.setVisibility(View.GONE);
            cwdListEmptyView.setVisibility(View.GONE);
            cwdListLoadingView.setVisibility(View.VISIBLE);
            sendCommand("query", null);
            handler.removeCallbacks(cwdListTimeoutRunnable);
            handler.postDelayed(cwdListTimeoutRunnable, CWD_LIST_TIMEOUT_MILLIS);
        } else {
            cwdListLoadingView.setVisibility(View.GONE);
            cwdListEmptyView.setVisibility(View.GONE);
            renderCwdList();
            cwdListContainer.setVisibility(View.VISIBLE);
        }
        updateEnabledState();
    }

    /** 重建目录候选列表（点某个目录发 207 set /cwd 目录名并关闭列表） */
    private void renderCwdList() {
        cwdListContainer.removeAllViews();
        for (final String dir : cwdCandidates) {
            TextView item = new TextView(dialog.getContext());
            item.setText("📂 " + dir);
            item.setTextSize(14);
            item.setTextColor(dialog.getContext().getResources().getColor(R.color.gray0));
            item.setPadding(dp(12), dp(8), dp(12), dp(8));
            item.setSingleLine(true);
            item.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            item.setOnClickListener(v -> switchCwd(dir));
            cwdListContainer.addView(item);
        }
    }

    /** 点某个目录：发 207 set /cwd 目录名，关闭列表；当前值随后由 type=3 刷新 */
    private void switchCwd(String dir) {
        if (TextUtils.isEmpty(dir)) {
            return;
        }
        sendCommand("set", "/cwd " + dir);
        closeCwdList();
    }

    /** 关闭目录列表（选中后 / 超时前） */
    private void closeCwdList() {
        cwdListOpen = false;
        handler.removeCallbacks(cwdListTimeoutRunnable);
        cwdListLoadingView.setVisibility(View.GONE);
        cwdListContainer.setVisibility(View.GONE);
        cwdListEmptyView.setVisibility(View.GONE);
        updateEnabledState();
    }

    private int dp(float value) {
        return (int) (value * dialog.getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    private void confirmAndSend(int contentRes, String command) {
        new MaterialDialog.Builder(dialog.getContext())
            .content(contentRes)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .onPositive((dialog1, which) -> sendCommand("set", command))
            .show();
    }

    private void updateEnabledState() {
        boolean enabled = !applying;
        modelSpinner.setEnabled(enabled);
        effortSpinner.setEnabled(enabled);
        setRadioGroupEnabled(sandboxGroup, enabled);
        planSwitch.setEnabled(enabled);
        cwdSwitchBtn.setEnabled(enabled);
        compactBtn.setEnabled(enabled);
        resetBtn.setEnabled(enabled);
        // 销毁按钮不随操作冷却禁用：危险操作始终可点（每次点击都会再弹确认）
    }

    private void setRadioGroupEnabled(RadioGroup group, boolean enabled) {
        if (group == null) {
            return;
        }
        group.setEnabled(enabled);
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
    }

    private void destroy() {
        destroyed = true;
        ChatManager.Instance().removeSettingUpdateListener(settingUpdateListener);
        handler.removeCallbacksAndMessages(null);
    }
}

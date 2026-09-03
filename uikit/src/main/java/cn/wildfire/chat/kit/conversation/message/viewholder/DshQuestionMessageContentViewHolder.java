/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.DshPlanDetailActivity;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.dsh.AgentAnswerMessageContent;
import cn.wildfirechat.message.dsh.AgentQuestionMessageContent;

/**
 * DSH 提问卡片（200）。
 * <p>
 * 选项垂直排列、整行可点（≥40dp）；单选点击即答并立即本地置灰；多选勾选 + 底部「提交」；
 * 「自定义回答」点击后聚焦会话主输入框并弹键盘（卡片内不嵌输入框，卡片期间用户直接发的
 * 文本会被服务端当作该卡片的自定义回答）；plan-review 的 detail 不内联展开，走
 * {@link DshPlanDetailActivity} 全屏计划详情页；锁定态显示 已作答/已过期，
 * answered 时附用户选择（"已作答（选择内容）"）。
 * </p>
 */
@MessageContentType(AgentQuestionMessageContent.class)
@EnableContextMenu
public class DshQuestionMessageContentViewHolder extends NormalMessageContentViewHolder {
    // 本地已作答的消息 id：点击后立即置灰，不依赖服务端 updateMessage 推送的实时性。
    // 重新进入会话时以 content.state 为准（服务端会 updateMessage）。
    private static final Set<Long> locallyAnsweredMessageIds = new HashSet<>();

    TextView headerTextView;
    LinearLayout questionsContainer;
    TextView customAnswerTextView;
    TextView stateTextView;

    private AgentQuestionMessageContent questionContent;
    // questionId -> 已选中的 label 列表（多选）
    private final Map<String, List<String>> localSelected = new HashMap<>();

    public DshQuestionMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindViews(View itemView) {
        headerTextView = itemView.findViewById(R.id.dshHeaderTextView);
        questionsContainer = itemView.findViewById(R.id.dshQuestionsContainer);
        customAnswerTextView = itemView.findViewById(R.id.dshCustomAnswerTextView);
        stateTextView = itemView.findViewById(R.id.dshStateTextView);
    }

    private void bindEvents(View itemView) {
        customAnswerTextView.setOnClickListener(v -> {
            // 卡片内不嵌输入框：聚焦会话主输入框并弹键盘
            fragment.getConversationInputPanel().focusInput();
        });
    }

    @Override
    protected void onBind(UiMessage message) {
        questionContent = (AgentQuestionMessageContent) message.message.content;
        localSelected.clear();

        boolean locked = isLocked();
        JSONArray questions = questionContent.getQuestions();

        JSONObject first = questions != null ? questions.optJSONObject(0) : null;
        String header = first != null ? first.optString("header") : null;
        if (!TextUtils.isEmpty(header)) {
            headerTextView.setVisibility(View.VISIBLE);
            headerTextView.setText("【" + header + "】");
        } else {
            headerTextView.setVisibility(View.GONE);
        }

        questionsContainer.removeAllViews();
        if (questions != null) {
            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.optJSONObject(i);
                if (question != null) {
                    bindQuestion(questionsContainer, i, question, locked);
                }
            }
        }

        customAnswerTextView.setVisibility(locked ? View.GONE : View.VISIBLE);
        if (locked) {
            stateTextView.setVisibility(View.VISIBLE);
            stateTextView.setText(stateText());
        } else {
            stateTextView.setVisibility(View.GONE);
        }
    }

    private String stateText() {
        if ("expired".equals(questionContent.getState())) {
            return "已过期";
        }
        // answered（或本地已作答）：附上服务端 updateMessage 写入的用户选择
        return "已作答" + mySelectionText();
    }

    /**
     * 服务端更新后的用户选择（插件 updateMessage 写入 content.answers）：
     * answers[].selected 以「、」连接、或 answers[].custom 自定义文本；多题答案以「；」分隔。
     * 仅 content.state=answered 时展示，与 PC 端 DshQuestionContentView 保持一致。
     */
    private String mySelectionText() {
        if (!"answered".equals(questionContent.getState())) {
            return "";
        }
        JSONArray answers = questionContent.getContentJson().optJSONArray("answers");
        if (answers == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < answers.length(); i++) {
            JSONObject answer = answers.optJSONObject(i);
            if (answer == null) {
                continue;
            }
            JSONArray selected = answer.optJSONArray("selected");
            List<String> labels = new ArrayList<>();
            if (selected != null) {
                for (int j = 0; j < selected.length(); j++) {
                    String label = selected.optString(j);
                    if (!TextUtils.isEmpty(label)) {
                        labels.add(label);
                    }
                }
            }
            if (!labels.isEmpty()) {
                parts.add(TextUtils.join("、", labels));
            } else {
                String custom = answer.optString("custom");
                if (!TextUtils.isEmpty(custom)) {
                    parts.add(custom);
                }
            }
        }
        return parts.isEmpty() ? "" : "（" + TextUtils.join("；", parts) + "）";
    }

    private boolean isLocked() {
        String state = questionContent.getState();
        return locallyAnsweredMessageIds.contains(message.message.messageId)
            || "answered".equals(state) || "expired".equals(state);
    }

    private void bindQuestion(LinearLayout container, int index, JSONObject question, boolean locked) {
        boolean planReview = isPlanReview(question);

        TextView titleTextView = new TextView(fragment.getContext());
        titleTextView.setText((index + 1) + ". " + question.optString("question"));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleTextView.setTextColor(0xFF000000);
        container.addView(titleTextView);

        String detail = question.optString("detail");
        if (!TextUtils.isEmpty(detail) && !planReview) {
            TextView detailTextView = new TextView(fragment.getContext());
            detailTextView.setText(detail);
            detailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            detailTextView.setTextColor(0xFFB3B3B3);
            detailTextView.setPadding(0, dp(4), 0, 0);
            container.addView(detailTextView);
        }

        JSONArray options = question.optJSONArray("options");
        if (planReview) {
            // plan-review：detail 是计划全文，卡片上不内联展开，显示「查看计划」打开全屏计划详情页
            if (!TextUtils.isEmpty(detail)) {
                TextView viewPlanButton = buildButton("查看计划", false);
                viewPlanButton.setOnClickListener(v -> {
                    if (isLocked()) {
                        return;
                    }
                    DshPlanDetailActivity.showPlan(fragment, message.message.conversation, detail,
                        questionContent.getQid(), question.optString("id"),
                        approveLabel(question, options), rejectLabel(question, options));
                });
                container.addView(viewPlanButton);
            }
            // intent.approve 命中的选项渲染为主色主按钮（点击即答），其余为次按钮
            if (options != null) {
                LinearLayout buttonsLayout = new LinearLayout(fragment.getContext());
                buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(6);
                buttonsLayout.setLayoutParams(lp);
                for (int i = 0; i < options.length(); i++) {
                    JSONObject option = options.optJSONObject(i);
                    if (option == null) {
                        continue;
                    }
                    String label = option.optString("label");
                    TextView button = buildButton(label, isApproveOption(question, label));
                    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, dp(40), 1);
                    if (i > 0) {
                        btnLp.setMarginStart(dp(8));
                    }
                    button.setLayoutParams(btnLp);
                    button.setEnabled(!locked);
                    button.setAlpha(locked ? 0.5f : 1f);
                    button.setOnClickListener(v -> sendSingleAnswer(question.optString("id"), label));
                    buttonsLayout.addView(button);
                }
                container.addView(buttonsLayout);
            }
        } else if (options != null && options.length() > 0) {
            if (question.optBoolean("multiSelect")) {
                bindMultiSelectOptions(container, question, options, locked);
            } else {
                for (int i = 0; i < options.length(); i++) {
                    JSONObject option = options.optJSONObject(i);
                    if (option == null) {
                        continue;
                    }
                    container.addView(buildSingleSelectOption(question.optString("id"), option.optString("label"), locked));
                }
            }
        }
    }

    /**
     * 单选选项：整行可点（≥40dp），点击即答。
     */
    private View buildSingleSelectOption(String questionId, String label, boolean locked) {
        TextView optionView = new TextView(fragment.getContext());
        optionView.setText(label);
        optionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        optionView.setTextColor(0xFF000000);
        optionView.setMinHeight(dp(40));
        optionView.setGravity(android.view.Gravity.CENTER_VERTICAL);
        optionView.setPadding(dp(12), 0, dp(12), 0);
        optionView.setBackgroundResource(R.drawable.shape_dsh_option_row);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        optionView.setLayoutParams(lp);
        optionView.setEnabled(!locked);
        optionView.setAlpha(locked ? 0.5f : 1f);
        optionView.setOnClickListener(v -> sendSingleAnswer(questionId, label));
        return optionView;
    }

    /**
     * 多选选项：勾选 + 底部「提交」按钮（未选禁用）。
     */
    private void bindMultiSelectOptions(LinearLayout container, JSONObject question, JSONArray options, boolean locked) {
        String questionId = question.optString("id");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option == null) {
                continue;
            }
            String label = option.optString("label");

            LinearLayout row = new LinearLayout(fragment.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(40));
            row.setPadding(dp(12), 0, dp(12), 0);
            row.setBackgroundResource(R.drawable.shape_dsh_option_row);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            row.setLayoutParams(lp);

            CheckBox checkBox = new CheckBox(fragment.getContext());
            checkBox.setClickable(false);
            TextView labelView = new TextView(fragment.getContext());
            labelView.setText(label);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            labelView.setTextColor(0xFF000000);
            row.addView(checkBox);
            row.addView(labelView);

            row.setEnabled(!locked);
            row.setAlpha(locked ? 0.5f : 1f);
            row.setOnClickListener(v -> {
                if (isLocked()) {
                    return;
                }
                List<String> selected = localSelected.computeIfAbsent(questionId, k -> new ArrayList<>());
                if (selected.contains(label)) {
                    selected.remove(label);
                    checkBox.setChecked(false);
                } else {
                    selected.add(label);
                    checkBox.setChecked(true);
                }
                updateSubmitButton(container);
            });
            container.addView(row);
        }

        TextView submitButton = buildButton("提交", true);
        submitButton.setTag("dshSubmitButton");
        submitButton.setEnabled(false);
        submitButton.setAlpha(0.5f);
        submitButton.setOnClickListener(v -> {
            if (isLocked()) {
                return;
            }
            try {
                JSONArray answers = new JSONArray();
                for (Map.Entry<String, List<String>> entry : localSelected.entrySet()) {
                    if (entry.getValue().isEmpty()) {
                        continue;
                    }
                    JSONObject answer = new JSONObject();
                    answer.put("id", entry.getKey());
                    answer.put("selected", new JSONArray(entry.getValue()));
                    answers.put(answer);
                }
                if (answers.length() > 0) {
                    sendAnswer(answers);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        container.addView(submitButton);
    }

    private void updateSubmitButton(LinearLayout container) {
        View submitButton = container.findViewWithTag("dshSubmitButton");
        if (submitButton == null) {
            return;
        }
        int selectedCount = 0;
        for (List<String> labels : localSelected.values()) {
            selectedCount += labels.size();
        }
        boolean enabled = selectedCount > 0;
        submitButton.setEnabled(enabled);
        submitButton.setAlpha(enabled ? 1f : 0.5f);
    }

    private TextView buildButton(String text, boolean primary) {
        TextView button = new TextView(fragment.getContext());
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setGravity(android.view.Gravity.CENTER);
        button.setMinHeight(dp(40));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackgroundResource(primary ? R.drawable.shape_dsh_btn_primary : R.drawable.shape_dsh_btn_secondary);
        button.setTextColor(primary ? 0xFFFFFFFF : 0xFF000000);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        button.setLayoutParams(lp);
        return button;
    }

    private boolean isPlanReview(JSONObject question) {
        JSONObject intent = question.optJSONObject("intent");
        return intent != null && "plan-review".equals(intent.optString("kind"));
    }

    private boolean isApproveOption(JSONObject question, String label) {
        JSONObject intent = question.optJSONObject("intent");
        return intent != null && TextUtils.equals(intent.optString("approve"), label);
    }

    private String approveLabel(JSONObject question, JSONArray options) {
        JSONObject intent = question.optJSONObject("intent");
        String approve = intent != null ? intent.optString("approve") : null;
        if (!TextUtils.isEmpty(approve)) {
            return approve;
        }
        JSONObject first = options != null ? options.optJSONObject(0) : null;
        return first != null ? first.optString("label") : null;
    }

    private String rejectLabel(JSONObject question, JSONArray options) {
        if (options == null) {
            return null;
        }
        String approve = approveLabel(question, options);
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null && !TextUtils.equals(option.optString("label"), approve)) {
                return option.optString("label");
            }
        }
        return null;
    }

    private void sendSingleAnswer(String questionId, String label) {
        if (isLocked()) {
            return;
        }
        try {
            JSONObject answer = new JSONObject();
            answer.put("id", questionId);
            answer.put("selected", new JSONArray().put(label));
            sendAnswer(new JSONArray().put(answer));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendAnswer(JSONArray answers) {
        if (isLocked()) {
            return;
        }
        AgentAnswerMessageContent content = new AgentAnswerMessageContent(questionContent.getQid(), answers);
        messageViewModel.sendMessage(message.message.conversation, content);
        locallyAnsweredMessageIds.add(message.message.messageId);
        onBind(message);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
            fragment.getResources().getDisplayMetrics());
    }
}

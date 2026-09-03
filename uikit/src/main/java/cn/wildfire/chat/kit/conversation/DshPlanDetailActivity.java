/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.message.dsh.AgentAnswerMessageContent;
import cn.wildfirechat.model.Conversation;

/**
 * DSH plan-review 全屏计划详情页：等宽字体、可滚动，底部固定 批准/拒绝 按钮。
 */
public class DshPlanDetailActivity extends WfcBaseActivity {

    private Conversation conversation;
    private String qid;
    private String questionId;
    private String approveLabel;
    private String rejectLabel;

    public static void showPlan(Fragment from, Conversation conversation, String planText,
                                String qid, String questionId, String approveLabel, String rejectLabel) {
        Intent intent = new Intent(from.requireContext(), DshPlanDetailActivity.class);
        intent.putExtra("conversation", conversation);
        intent.putExtra("planText", planText);
        intent.putExtra("qid", qid);
        intent.putExtra("questionId", questionId);
        intent.putExtra("approveLabel", approveLabel);
        intent.putExtra("rejectLabel", rejectLabel);
        WfcPageCompat.startPage(from, intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.dsh_plan_detail_activity;
    }

    @Override
    protected void afterViews() {
        setTitle("计划详情");

        Intent intent = getIntent();
        conversation = intent.getParcelableExtra("conversation");
        String planText = intent.getStringExtra("planText");
        qid = intent.getStringExtra("qid");
        questionId = intent.getStringExtra("questionId");
        approveLabel = intent.getStringExtra("approveLabel");
        rejectLabel = intent.getStringExtra("rejectLabel");

        TextView planTextView = findViewById(R.id.dshPlanTextView);
        planTextView.setTypeface(Typeface.MONOSPACE);
        planTextView.setText(planText);
        planTextView.setMovementMethod(new ScrollingMovementMethod());

        TextView approveButton = findViewById(R.id.dshPlanApproveButton);
        TextView rejectButton = findViewById(R.id.dshPlanRejectButton);
        approveButton.setText(approveLabel != null ? approveLabel : "批准");
        approveButton.setOnClickListener(v -> decide(approveLabel != null ? approveLabel : "批准"));
        if (rejectLabel != null) {
            rejectButton.setText(rejectLabel);
            rejectButton.setOnClickListener(v -> decide(rejectLabel));
        } else {
            rejectButton.setVisibility(View.GONE);
        }
    }

    private void decide(String label) {
        if (conversation == null) {
            finish();
            return;
        }
        try {
            JSONObject answer = new JSONObject();
            answer.put("id", questionId);
            answer.put("selected", new JSONArray().put(label));
            JSONArray answers = new JSONArray().put(answer);
            AgentAnswerMessageContent content = new AgentAnswerMessageContent(qid, answers);
            new ViewModelProvider(this).get(MessageViewModel.class).sendMessage(conversation, content);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "已作答", Toast.LENGTH_SHORT).show();
        finish();
    }
}

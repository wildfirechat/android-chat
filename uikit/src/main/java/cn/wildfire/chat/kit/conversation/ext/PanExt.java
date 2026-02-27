/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.pan.PanFilePickerActivity;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;

/**
 * 网盘扩展
 */
public class PanExt extends ConversationExt {
    
    @ExtContextMenuItem(tag = "10")
    public void pickFileFromPan(View containerView, Conversation conversation) {
        // 检查网盘服务是否可用
        try {
            Class<?> clazz = Class.forName("cn.wildfire.chat.app.pan.PanServiceProvider");
            Object service = clazz.getMethod("getPanService").invoke(null);
            if (service == null) {
                Toast.makeText(activity, "网盘服务不可用", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(activity, "网盘服务未配置", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(activity, PanFilePickerActivity.class);
        startActivityForResult(intent, 100);
        
        // 发送正在输入提示
        TypingMessageContent content = new TypingMessageContent(TypingMessageContent.TYPING_FILE);
        messageViewModel.sendMessage(conversation, toUsers(), content);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            // 获取选中的文件列表
            ArrayList<PanFile> selectedFiles = data.getParcelableArrayListExtra("selectedFiles");
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                // 发送选中的文件
                for (PanFile panFile : selectedFiles) {
                    sendPanFile(panFile);
                }
            }
        }
    }
    
    /**
     * 发送网盘文件
     */
    private void sendPanFile(PanFile panFile) {
        if (panFile == null) return;
        
        // 创建文件消息内容
        FileMessageContent content = new FileMessageContent();
        content.setName(panFile.getName());
        content.setSize(panFile.getSize() != null ? panFile.getSize().intValue() : 0);
        content.remoteUrl = panFile.getStorageUrl();
        
        // 发送消息
        messageViewModel.sendMessage(conversation, toUsers(), content);
    }
    
    @Override
    public int priority() {
        return 10;
    }
    
    @Override
    public int iconResId() {
        return R.mipmap.ic_func_file;
    }
    
    @Override
    public String title(Context context) {
        return "网盘";
    }
    
    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }
    
    @Override
    public boolean filter(Conversation conversation) {
        // 检查网盘服务是否配置
        try {
            Class<?> clazz = Class.forName("cn.wildfire.chat.kit.Config");
            java.lang.reflect.Field field = clazz.getField("PAN_SERVER_ADDRESS");
            String address = (String) field.get(null);
            return address == null || address.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }
}

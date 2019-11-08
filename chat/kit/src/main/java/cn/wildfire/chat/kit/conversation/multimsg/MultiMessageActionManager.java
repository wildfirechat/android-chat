package cn.wildfire.chat.kit.conversation.multimsg;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.model.Conversation;

public class MultiMessageActionManager {
    private static MultiMessageActionManager instance;
    private List<MultiMessageAction> conversationMultiMessageActions;

    private MultiMessageActionManager() {
        conversationMultiMessageActions = new ArrayList<>();
        init();
    }

    public static synchronized MultiMessageActionManager getInstance() {
        if (instance == null) {
            instance = new MultiMessageActionManager();
        }
        return instance;
    }

    private void init() {
        registerAction(DeleteMultiMessageAction.class);
    }

    public void registerAction(Class<? extends MultiMessageAction> clazz) {
        Constructor constructor;
        try {
            constructor = clazz.getConstructor();
            MultiMessageAction action = (MultiMessageAction) constructor.newInstance();
            conversationMultiMessageActions.add(action);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterAction(Class<? extends MultiMessageAction> clazz) {
        // TODO
    }

    public List<MultiMessageAction> getConversationActions(Conversation conversation) {
        List<MultiMessageAction> currentActions = new ArrayList<>();
        for (MultiMessageAction ext : this.conversationMultiMessageActions) {
            if (!ext.filter(conversation)) {
                currentActions.add(ext);
            }
        }
        return currentActions;
    }
}

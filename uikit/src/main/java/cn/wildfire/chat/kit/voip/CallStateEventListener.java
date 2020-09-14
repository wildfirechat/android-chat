/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import cn.wildfirechat.avenginekit.AVEngineKit;

/**
 * Created by heavyrainlee on 19/02/2018.
 */

public interface CallStateEventListener {
    void onCallState(AVEngineKit.CallState state, AVEngineKit.CallSession session);
}

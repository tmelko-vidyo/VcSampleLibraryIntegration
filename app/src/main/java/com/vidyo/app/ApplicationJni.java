package com.vidyo.app;

import android.util.Log;

import com.vidyo.VidyoClientLib.LmiAndroidAppJni;
import com.vidyo.VidyoClientLib.LmiAndroidJniChatCallbacks;
import com.vidyo.VidyoClientLib.LmiAndroidJniConferenceCallbacks;
import com.vidyo.app.utils.CallStatusEvent;
import com.vidyo.app.utils.ChatMessageEvent;

import org.greenrobot.eventbus.EventBus;

public class ApplicationJni extends LmiAndroidAppJni {

    private static final String TAG = ApplicationJni.class.getCanonicalName();

    private static final String CALLBACK_CLASS_PATH = "com/vidyo/app/ApplicationJni";

    private static final String METHOD_FOR_JNI_CALLBACK = "applicationJniConferenceStatusCallback";
    private static final String METHOD_FOR_JNI_GROUP_CHAT_CALLBACK = "applicationJniChatCallback";

    private LmiAndroidJniConferenceCallbacks conferenceCallbacks;
    private LmiAndroidJniChatCallbacks chatCallbacks;

    @Override
    public void onCreate() {
        super.onCreate();
        conferenceCallbacks = new LmiAndroidJniConferenceCallbacks(CALLBACK_CLASS_PATH, METHOD_FOR_JNI_CALLBACK,
                null, null, null, null, null);
        chatCallbacks = new LmiAndroidJniChatCallbacks(CALLBACK_CLASS_PATH, METHOD_FOR_JNI_GROUP_CHAT_CALLBACK);
    }

    @SuppressWarnings("JniCallback")
    public void applicationJniConferenceStatusCallback(int status, int error, String message) {
        Log.d(TAG, "applicationJniConferenceStatusCallback: status=" + status + ", error=" + error + ", message=" + message);

        EventBus.getDefault().post(new CallStatusEvent(status, error, message));
    }

    /**
     * This method is called from the JNI when a chat message is received.
     * Handle both:
     * VIDYO_CLIENT_OUT_EVENT_PRIVATE_CHAT
     * VIDYO_CLIENT_OUT_EVENT_GROUP_CHAT
     *
     * @param groupChat whenever it's a group chat message (we have private/group chats)
     * @param uri       in case of PRIVATE chat this would be populated with sender participant's URI
     * @param name      name of sender both for private and group chats
     * @param message   message body
     */
    @SuppressWarnings("JniCallback")
    private void applicationJniChatCallback(boolean groupChat, String uri, String name, String message) {
        Log.d(TAG, "Got chat message from: " + name + ", Msg: " + message);

        EventBus.getDefault().post(new ChatMessageEvent(name, message, groupChat, uri));
    }

    @Override
    protected void onLibraryStarted() {
        Log.i(ApplicationJni.class.getCanonicalName(), "Library has been started successfully!");
    }

    public void registerCallback() {
        LmiAndroidJniConferenceSetCallbacks(conferenceCallbacks);
        LmiAndroidJniChatSetCallbacks(chatCallbacks);
    }
}
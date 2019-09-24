package com.vidyo.app;

import android.util.Log;

import com.vidyo.VidyoClientLib.LmiAndroidAppJni;
import com.vidyo.VidyoClientLib.LmiAndroidJniChatCallbacks;
import com.vidyo.VidyoClientLib.LmiAndroidJniConferenceCallbacks;
import com.vidyo.app.utils.CallStatusEvent;
import com.vidyo.app.utils.ChatMessageEvent;
import com.vidyo.app.utils.ParticipantsChangeEvent;

import org.greenrobot.eventbus.EventBus;

public class ApplicationJni extends LmiAndroidAppJni {

    private static final String TAG = ApplicationJni.class.getCanonicalName();

    private static final String CALLBACK_CLASS_PATH = "com/vidyo/app/ApplicationJni";

    private static final String METHOD_FOR_JNI_CALLBACK = "applicationJniConferenceStatusCallback";
    private static final String METHOD_FOR_JNI_GROUP_CHAT_CALLBACK = "applicationJniChatCallback";

    private static final String METHOD_FOR_JNI_PARTICIPANTS_CALLBACK = "applicationJniParticipantChangeCallback";

    private LmiAndroidJniConferenceCallbacks conferenceCallbacks;
    private LmiAndroidJniChatCallbacks chatCallbacks;

    @Override
    public void onCreate() {
        super.onCreate();
        conferenceCallbacks = new LmiAndroidJniConferenceCallbacks(CALLBACK_CLASS_PATH, METHOD_FOR_JNI_CALLBACK,
                null, null, null, null,
                METHOD_FOR_JNI_PARTICIPANTS_CALLBACK);
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
    public void applicationJniChatCallback(boolean groupChat, String uri, String name, String message) {
        Log.d(TAG, "Got chat message from: " + name + ", Msg: " + message);

        EventBus.getDefault().post(new ChatMessageEvent(name, message, groupChat, uri));
    }

    /**
     * Called from JNI layer when the participants in a conference has changed.
     *
     * @param numOfParticipants the number of participants currently in the conference
     */
    @SuppressWarnings("JniCallback")
    public void applicationJniParticipantChangeCallback(int numOfParticipants) {
        Log.d(TAG, "Got participants change event. Actual count: " + numOfParticipants);

        EventBus.getDefault().post(new ParticipantsChangeEvent(numOfParticipants));
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
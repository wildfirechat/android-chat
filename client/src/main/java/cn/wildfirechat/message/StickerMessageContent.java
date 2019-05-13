package cn.wildfirechat.message;

import android.graphics.BitmapFactory;
import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Sticker, flag = PersistFlag.Persist_And_Count)
public class StickerMessageContent extends MediaMessageContent {
    public int width;
    public int height;

    public StickerMessageContent() {
    }

    public StickerMessageContent(String localPath) {
        this.localPath = localPath;
        this.mediaType = MessageContentMediaType.FILE;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(localPath, options);
        height = options.outHeight;
        width = options.outWidth;
    }


    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = "[动态表情]";

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("x", width);
            objWrite.put("y", height);
            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);

        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                width = jsonObject.optInt("x");
                height = jsonObject.optInt("y");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[动态表情]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeString(this.localPath);
        dest.writeString(this.remoteUrl);
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
        dest.writeInt(this.mentionedType);
        dest.writeStringList(this.mentionedTargets);
    }

    protected StickerMessageContent(Parcel in) {
        this.width = in.readInt();
        this.height = in.readInt();
        this.localPath = in.readString();
        this.remoteUrl = in.readString();
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : MessageContentMediaType.values()[tmpMediaType];
        this.mentionedType = in.readInt();
        this.mentionedTargets = in.createStringArrayList();
    }

    public static final Creator<StickerMessageContent> CREATOR = new Creator<StickerMessageContent>() {
        @Override
        public StickerMessageContent createFromParcel(Parcel source) {
            return new StickerMessageContent(source);
        }

        @Override
        public StickerMessageContent[] newArray(int size) {
            return new StickerMessageContent[size];
        }
    };
}

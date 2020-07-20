package cn.wildfire.chat.kit.conversation.mention;

public class Mention {
    private int start;
    private int end;
    private int flags;
    private boolean isMentionAll;
    private String uid;

    public Mention(int start, int end, int flags) {
        this.start = start;
        this.end = end;
        this.flags = flags;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean isMentionAll() {
        return isMentionAll;
    }

    public void setMentionAll(boolean mentionAll) {
        isMentionAll = mentionAll;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}

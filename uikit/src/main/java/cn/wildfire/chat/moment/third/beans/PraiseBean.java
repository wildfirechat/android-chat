package cn.wildfire.chat.moment.third.beans;

/**
 * 点赞bean
 */
public class PraiseBean {
    private long id;
    private String praiseUserName;
    private String praiseUserId;

    public String getPraiseUserName() {
        return praiseUserName;
    }

    public void setPraiseUserName(String praiseUserName) {
        this.praiseUserName = praiseUserName;
    }

    public String getPraiseUserId() {
        return praiseUserId;
    }

    public void setPraiseUserId(String praiseUserId) {
        this.praiseUserId = praiseUserId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PraiseBean that = (PraiseBean) o;

        return praiseUserId != null ? praiseUserId.equals(that.praiseUserId) : that.praiseUserId == null;
    }

    @Override
    public int hashCode() {
        return praiseUserId != null ? praiseUserId.hashCode() : 0;
    }
}

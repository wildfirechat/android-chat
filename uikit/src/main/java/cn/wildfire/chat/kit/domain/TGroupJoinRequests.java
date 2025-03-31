package cn.wildfire.chat.kit.domain;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * 加群申请对象 t_group_join_requests
 *
 * @author ruoyi
 * @date 2025-03-25
 */
public class TGroupJoinRequests implements Parcelable {
    @SerializedName("requestId")
    private long requestId;

    @SerializedName("groupId")
    private String groupId;

    @SerializedName("applicantId")
    private String applicantId;

    @SerializedName("operatorId")
    private String operatorId;

    @SerializedName("status")
    private long status;

    @SerializedName("applyTime")
    private Date applyTime;

    @SerializedName("handleTime")
    private Date handleTime;

    @SerializedName("remark")
    private String remark;

    public TGroupJoinRequests() {
    }

    public TGroupJoinRequests(Parcel in) {
        requestId = in.readLong();
        groupId = in.readString();
        applicantId = in.readString();
        operatorId = in.readString();
        status = in.readLong();
        remark = in.readString();
    }

    public static final Creator<TGroupJoinRequests> CREATOR = new Creator<TGroupJoinRequests>() {
        @Override
        public TGroupJoinRequests createFromParcel(Parcel in) {
            return new TGroupJoinRequests(in);
        }

        @Override
        public TGroupJoinRequests[] newArray(int size) {
            return new TGroupJoinRequests[size];
        }
    };


    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setStatus(long status) {
        this.status = status;
    }

    public long getStatus() {
        return status;
    }

    public void setApplyTime(Date applyTime) {
        this.applyTime = applyTime;
    }

    public Date getApplyTime() {
        return applyTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    @NonNull
    @Override
    public String toString() {
        return "TGroupJoinRequests{" +
                "requestId=" + requestId +
                ", groupId='" + groupId + '\'' +
                ", applicantId='" + applicantId + '\'' +
                ", operatorId='" + operatorId + '\'' +
                ", status=" + status +
                ", applyTime=" + applyTime +
                ", handleTime=" + handleTime +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(requestId);
        dest.writeString(groupId);
        dest.writeString(applicantId);
        dest.writeString(operatorId);
        dest.writeLong(status);
        dest.writeLong(applyTime != null ? applyTime.getTime() : -1);
        dest.writeLong(handleTime != null ? handleTime.getTime() : -1);
        dest.writeString(remark);
    }
}

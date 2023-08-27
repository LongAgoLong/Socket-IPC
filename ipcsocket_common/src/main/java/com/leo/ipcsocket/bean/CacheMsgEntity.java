package com.leo.ipcsocket.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

public class CacheMsgEntity implements Parcelable {
    public long creationTime;
    public String msg;

    public CacheMsgEntity(String msg) {
        this.msg = msg;
        this.creationTime = SystemClock.elapsedRealtime();
    }

    @Override
    public String toString() {
        return "{\"creationTime\":" + creationTime + ",\"msg\":" + msg + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.creationTime);
        dest.writeString(this.msg);
    }

    public void readFromParcel(Parcel source) {
        this.creationTime = source.readLong();
        this.msg = source.readString();
    }

    protected CacheMsgEntity(Parcel in) {
        this.creationTime = in.readLong();
        this.msg = in.readString();
    }

    public static final Parcelable.Creator<CacheMsgEntity> CREATOR = new Parcelable.Creator<CacheMsgEntity>() {
        @Override
        public CacheMsgEntity createFromParcel(Parcel source) {
            return new CacheMsgEntity(source);
        }

        @Override
        public CacheMsgEntity[] newArray(int size) {
            return new CacheMsgEntity[size];
        }
    };
}

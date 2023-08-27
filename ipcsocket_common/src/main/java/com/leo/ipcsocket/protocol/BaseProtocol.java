package com.leo.ipcsocket.protocol;

import android.os.Parcel;
import android.os.Parcelable;

public class BaseProtocol implements Parcelable {
    private String nameSpace;
    private String name;
    private String data;

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.nameSpace);
        dest.writeString(this.name);
        dest.writeString(this.data);
    }

    public void readFromParcel(Parcel source) {
        this.nameSpace = source.readString();
        this.name = source.readString();
        this.data = source.readString();
    }

    public BaseProtocol() {
    }

    protected BaseProtocol(Parcel in) {
        this.nameSpace = in.readString();
        this.name = in.readString();
        this.data = in.readString();
    }

    public static final Parcelable.Creator<BaseProtocol> CREATOR = new Parcelable.Creator<BaseProtocol>() {
        @Override
        public BaseProtocol createFromParcel(Parcel source) {
            return new BaseProtocol(source);
        }

        @Override
        public BaseProtocol[] newArray(int size) {
            return new BaseProtocol[size];
        }
    };
}

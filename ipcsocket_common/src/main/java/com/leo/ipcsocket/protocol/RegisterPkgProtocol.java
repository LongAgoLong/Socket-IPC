package com.leo.ipcsocket.protocol;

import com.alibaba.fastjson.JSONObject;

public class RegisterPkgProtocol extends BaseProtocol {
    public static final String NAME_SPACE = "com.socket.ipc.communication_link.info";
    public static final String NAME = "RegisterPkg";

    public RegisterPkgProtocol(String pkgName) {
        setNameSpace("com.socket.ipc.communication_link.info");
        setName("RegisterPkg");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("pkgName", pkgName);
        setData(jsonObject.toJSONString());
    }
}

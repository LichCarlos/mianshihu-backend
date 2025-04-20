package com.carlos.mianshihu.model.dto.user;

import lombok .Data;
import java.io.Serializable;
@Data
public class UserEditRequest implements  Serializable{
    private String phoneNumber;
    private  String email;
    private String grade;
    private String workExperience;

    /**
     * 擅长方向
     */
    private String expertiseDirection;
    private  String userName;

    private  String  userAvatar;
    private  String userProfile;
}

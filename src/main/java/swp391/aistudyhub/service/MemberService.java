package swp391.aistudyhub.service;

import swp391.aistudyhub.entity.UserMemberSubscription;

public interface MemberService {

    void registerMember();

    UserMemberSubscription getMemberDetail();
}
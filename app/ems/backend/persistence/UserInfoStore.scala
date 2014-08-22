package ems.backend.persistence

import ems.models.UserInfo

import scala.concurrent.Future


trait UserInfoStore {
  def findUserInfoByUserId(userId: String): Future[UserInfo]
  def findUserInfoByPhoneNumber(phoneNumber: String): Future[UserInfo]
  def createUserInfo(userInfo: UserInfo): Future[UserInfo]
  def savePhoneNumber(userId: String, phoneNumber: String): Future[UserInfo]
}

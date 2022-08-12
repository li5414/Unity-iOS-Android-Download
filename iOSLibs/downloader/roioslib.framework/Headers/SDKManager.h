//
// Created by 董宸 on 2018/9/22.
// Copyright (c) 2018 ___JOYYOU___. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIApplication.h>
#import "ISDK.h"
#import "IUnity.h"

@interface SDKManager : NSObject

//加载SDK列表
+ (void) Load : (NSString *) sdklist;

//加载SDK列表（可选是否重新加载）
+ (void) Load : (NSString *) sdklist forceReload : (Boolean) force;

//初始化原生通知unity的object
+ (void) InitCallBackObj : (NSString *) objName;

//获取SDK对象列表
+ (NSArray *) SDKs;

//移除所有SDK
+ (void) RemoveAllSDKs;

//注册SDK
+ (void) RegisterSDK : (NSString *) name;

//注册SDK
+ (void) RegisterSDKWithName:(NSString *)name andSDK:(ISDK *) sdk;

//获取SDK
+ (ISDK *) GetSDK : (NSString *) name;

//获取应用
+ (UIApplication *) GetApplication;

//获取Unity
+ (NSObject<IUnity> *) GetUnity;

//设置应用
+ (void) SetApplication : (UIApplication *) app;

//设置Unity
+ (void) SetUnity : (NSObject<IUnity> *) unity;

//获取应用配置
+ (NSString *) GetMetaData : (NSString *) name;

//向Unity发送消息
+ (void) SendUnityMessage : (NSString *) name andArgs : (NSString *) args;

//向Unity发送消息
+ (void) SendUnityMessageWithObject : (NSString *) obj andName : (NSString *) name andArgs : (NSString *) args;
@end

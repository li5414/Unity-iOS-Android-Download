//
// Created by 董宸 on 2018/9/22.
// Copyright (c) 2018 ___JOYYOU___. All rights reserved.
//

#import <UIKit/UIApplication.h>
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0
#import <UserNotifications/UserNotifications.h>
#endif

#import "IUnity.h"

@interface ISDK : NSObject{
    @public
    NSString* SDKName;
}

//获取APP
+ (UIApplication *) GetApplication;

//获取Unity
+ (NSObject<IUnity> *) GetUnity;

//获取配置
+ (NSString *) GetMetaData : (NSString *) name;

//向Unity发送消息
+ (void) SendUnityMessage : (NSString *) name withArgs : (NSString *) args;

//向Unity发送消息
+ (void) SendUnityMessageWithObject : (NSString *) obj andName : (NSString *) name andArgs : (NSString *) args;

//应用创建
- (BOOL) onApplicationFinishLaunching : (UIApplication *) application withOptions : (NSDictionary *) options;

//应用将要变成非激活状态
- (void) onApplicationWillResignActive : (UIApplication *) application;

//应用将要进入后台
- (void) onApplicationDidEnterBackground : (UIApplication *) application;

//应用处于后台
- (void) onHandleEventsForBackgroundURLSession : (UIApplication *)application identifier:(NSString *)identifier completionHandler:(void (^)(void))completionHandler;

//应用将要回到前台
- (void) onApplicationWillEnterforeground : (UIApplication *) application;

//应用将要变成激活状态
- (void) onApplicationDidBecomeActive : (UIApplication *) application;

//应用收到内存警告
- (void) onApplicationDidReceiveMemoryWarning : (UIApplication *) application;

//应用将要关闭
- (void) onApplicationWillTerminate : (UIApplication *) application;

//打开Url
- (BOOL) onApplicationOpenUrl:(UIApplication*)application url:(NSURL*)url sourceApplication:(NSString*)sourceApplication annotation:(id)annotation;

//IOS 9.0新版打开Url
- (BOOL) onApplicationOpenUrl:(UIApplication *)application url:(NSURL *)url options:(NSDictionary<NSString *,id> *)options;

//处理Url
- (BOOL) onApplicationHandleUrl:(UIApplication *)application url:(NSURL *)url;

//应用处理后台刷新数据
- (void) onApplication:(UIApplication *)application performFetchWithCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler;

//注册远程通知
- (void) onApplication:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(nonnull NSData *)deviceToken;

//注册远程通知失败
- (void) onApplication:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(nonnull NSError *)error;

// 应用在前台收到远端推送
- (void) onApplication:(UIApplication *)application didReceiveRemoteNotification:(nonnull NSDictionary *)userInfo;

// 应用在后台收到远端推送
- (void)onApplication:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult result))completionHandler;

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0
//  iOS 10: App在前台获取到通知
- (void)onUserNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler;

//  iOS 10: 点击通知进入App时触发
- (void)onUserNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void (^)(void))completionHandler;
#endif

//处理Unity发来的消息
- (void) handleUnityMessage : (NSString *) name withArgs : (NSString *) args;

//处理Unity发来的数据
- (void) handleUnityData : (NSString *) name withArgs : (NSString *) args andData : (NSData *) data;

//处理Unity发来的消息并带返回值
- (NSString*) handleUnityGetArgs : (NSString *) name withArgs : (NSString *) args;

@end
